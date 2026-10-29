package com.kelly.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.time.ZonedDateTime
import kotlin.math.roundToInt

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.PageCache
import com.kelly.reader.data.SettingsRepository
import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ProgressLabel
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.TextAlignment

// the screen's layout usually comes a fraction of a second after the book's row
private const val LAYOUT_WAIT_MS = 500L

/**
 * Loads the book, paginates it for the measurer the screen hands over, moves between pages
 * and saves the position. The settings come from and go to the settings repository.
 */
class ReaderViewModel(
    private val bookId: String,
    private val bookRepository: BookRepository,
    private val settingsRepository: SettingsRepository,
    private val pageCache: PageCache,
    private val paginationDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var paginationJob: Job? = null
    private var measurerKey: String? = null
    private var measurer: PageMeasurer? = null
    private val cache = HashMap<String, IntArray>()

    // pages measured from the start after a change, held back until the next page turn so the
    // text being read does not move under the reader
    private var waitingStarts: IntArray? = null

    // the reading spot saved in the book's row, known before its text
    private var savedSpot: Int? = null

    // the screen's first layout, which opening waits for a moment, and whether the text is
    // being read, after which a new layout looks up its saved page on its own
    private val layoutKnown = CompletableDeferred<Unit>()
    private var readingText = false

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            // the saved percentage shows while a big book's text is still being read; the book
            // itself goes in with its text, so nothing is paged before the text is there
            _uiState.update { it.copy(savedProgress = book.progress) }
            savedSpot = book.lastOffset
            // the saved page list is read before the text: reading a big book fills the memory
            // and holds up everything else, the page number with it. The layout that names the
            // list comes a moment after the book's row
            withTimeoutOrNull(LAYOUT_WAIT_MS) { layoutKnown.await() }
            loadSavedPage()
            readingText = true
            val text = bookRepository.readText(book)
            _uiState.update {
                it.copy(
                    book = book,
                    text = text,
                    isRtl = TextDirectionDetector.isRtl(text),
                    offset = book.lastOffset.coerceIn(0, text.length),
                )
            }
            settingsRepository.setRecentBookId(bookId)
            settingsRepository.recordReadingDay(ZonedDateTime.now(clock))
            paginate()
        }
        viewModelScope.launch {
            settingsRepository.readerSettings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    /**
     * The screen calls this once it knows the page size and text style. [key] names that
     * combination: the same key reuses the pages already found, in memory or on disk, a new
     * key paginates again.
     */
    fun setPageMeasurer(key: String, measurer: PageMeasurer) {
        if (key == measurerKey) return
        measurerKey = key
        this.measurer = measurer
        layoutKnown.complete(Unit)
        if (readingText) viewModelScope.launch { loadSavedPage() }
        paginate()
    }

    /**
     * While the text is still being read, finds the page of the saved spot in the saved page
     * list of the current layout, so the page number shows at once. Needs both the spot and
     * the layout.
     */
    private suspend fun loadSavedPage() {
        val key = measurerKey ?: return
        val spot = savedSpot ?: return
        if (_uiState.value.book != null) return
        // a layout never counted keeps the number found so far: the page area changes size a
        // few times while the screen settles, and those passing sizes have no list
        val starts = cache[key] ?: pageCache.load(bookId, key) ?: return
        // the text may have come in, or the layout changed, while the list was read
        if (_uiState.value.book != null || key != measurerKey) return
        val page = SavedPage(Paginator.pageIndexFor(starts, spot) + 1, starts.size)
        _uiState.update { it.copy(savedPage = page) }
    }

    private fun paginate() {
        val key = measurerKey ?: return
        val measurer = measurer ?: return
        val text = _uiState.value.text
        if (_uiState.value.book == null) return
        paginationJob?.cancel()
        holdBack(null)
        cache[key]?.let { starts ->
            _uiState.update { it.copy(pageStarts = starts, paginationDone = true) }
            return
        }
        // the old pages stay on screen until the new ones arrive, so nothing flashes blank
        _uiState.update { it.copy(paginationDone = false) }
        paginationJob = viewModelScope.launch {
            pageCache.load(bookId, key)?.let { starts ->
                finishPagination(key, starts)
                return@launch
            }
            var last: IntArray? = null
            val anchor = _uiState.value.offset
            val fromSpot = anchor in 1 until text.length
            // every result in order, so the pages from the reading spot are complete before
            // the pages from the start of the book arrive after them; a result shows once it
            // holds the end of the page being read, as the first ones hold only its start and
            // would blank the page and drop the progress to its start
            Paginator.paginate(text, measurer, paginationDispatcher, anchor).collect { starts ->
                last = starts
                val holdsPage = starts.last() > _uiState.value.offset
                if ((!fromSpot || starts[0] != 0) && holdsPage) {
                    _uiState.update { it.copy(pageStarts = starts) }
                }
            }
            last?.let { starts ->
                pageCache.save(bookId, key, starts)
                cache[key] = starts
                // in a book measured from the reading spot, the pages from the start would move
                // the text being read, so they wait for the next page turn unless there is none
                if (fromSpot && _uiState.value.hasNextPage) {
                    holdBack(starts)
                } else {
                    finishPagination(key, starts)
                }
            }
        }
    }

    private fun finishPagination(key: String, starts: IntArray) {
        cache[key] = starts
        _uiState.update { it.copy(pageStarts = starts, paginationDone = true) }
        saveProgress()
    }

    /**
     * Goes to the next page. Pages waiting since a change come in here: the position moves to
     * the first char not shown yet, and the page that holds it shows, so no text is skipped.
     */
    fun nextPage() {
        val state = _uiState.value
        val starts = state.pageStarts ?: return
        if (!state.hasNextPage) return
        val next = starts[state.pageIndex + 1]
        showWaitingPages()
        moveTo(next)
    }

    /**
     * Goes to the previous page. Pages waiting since a change come in here, and the page that
     * holds the char just before the position shows, so no text is skipped.
     */
    fun previousPage() {
        if (showWaitingPages()) {
            val state = _uiState.value
            val starts = state.pageStarts ?: return
            if (state.offset > 0) moveTo(starts[Paginator.pageIndexFor(starts, state.offset - 1)])
            return
        }
        val state = _uiState.value
        val starts = state.pageStarts ?: return
        if (!state.hasPreviousPage) return
        moveTo(starts[state.pageIndex - 1])
    }

    /**
     * In scroll mode the screen reports the page now at the top; its start becomes the
     * position. The page that already holds the position leaves it as it is, so a spot inside
     * a page, as after the pages change, is not pulled back to the page start. Pages waiting
     * since a change come in when the top page changes.
     */
    fun moveToPage(index: Int) {
        val state = _uiState.value
        val starts = state.pageStarts ?: return
        if (index !in starts.indices) return
        if (index == state.pageIndex) return
        val target = starts[index]
        showWaitingPages()
        moveTo(target)
    }

    /**
     * Goes to the place [fraction] (0 to 1) of the way through the book, as the drawer's slider
     * asks. With every page known it goes by page, as the progress counts then, so the end of
     * the slider is the last page. While the pages are still being counted it goes by chars,
     * and a place past the pages known so far has the pages measured again from there, as when
     * a book opens at a saved spot.
     */
    fun jumpTo(fraction: Float) {
        if (_uiState.value.book == null || _uiState.value.text.isEmpty()) return
        showWaitingPages()
        val state = _uiState.value
        val share = fraction.coerceIn(0f, 1f)
        val starts = state.pageStarts
        val count = state.pageCount
        if (starts != null && count != null) {
            val index = ((share * count).roundToInt() - 1).coerceIn(0, count - 1)
            moveTo(starts[index])
            return
        }
        val target = (share * state.text.length).toInt().coerceIn(0, state.text.length - 1)
        if (starts != null && starts.first() <= target && starts.last() > target) {
            moveTo(starts[Paginator.pageIndexFor(starts, target)])
            return
        }
        moveTo(target)
        paginate()
    }

    /**
     * Shows the pages held back since the last change, if any, and says whether there were.
     */
    private fun showWaitingPages(): Boolean {
        val starts = waitingStarts ?: return false
        waitingStarts = null
        _uiState.update { it.copy(pageStarts = starts, paginationDone = true, waitingPage = null) }
        return true
    }

    /**
     * Holds [starts], the pages from the start of the book, back until the next page turn, or
     * drops the ones held with null. The page of the reading spot in them shows at once, so
     * the progress goes by page already.
     */
    private fun holdBack(starts: IntArray?) {
        waitingStarts = starts
        val page = starts?.let {
            SavedPage(Paginator.pageIndexFor(it, _uiState.value.offset) + 1, it.size)
        }
        _uiState.update { it.copy(waitingPage = page) }
    }

    private fun moveTo(offset: Int) {
        _uiState.update { it.copy(offset = offset) }
        saveProgress()
    }

    private fun saveProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        viewModelScope.launch {
            bookRepository.saveProgress(book.id, state.offset, state.progress)
        }
    }

    // ----- settings -----

    fun increaseFontSize() {
        changeFontSize(+1)
    }

    fun decreaseFontSize() {
        changeFontSize(-1)
    }

    private fun changeFontSize(delta: Int) {
        val size = (_uiState.value.settings.fontSize + delta)
            .coerceIn(ReaderSettings.MIN_FONT_SIZE, ReaderSettings.MAX_FONT_SIZE)
        viewModelScope.launch { settingsRepository.setFontSize(size) }
    }

    fun setLineSpacing(spacing: LineSpacing) {
        viewModelScope.launch { settingsRepository.setLineSpacing(spacing) }
    }

    fun setFont(font: ReaderFont) {
        viewModelScope.launch { settingsRepository.setFont(font) }
    }

    /**
     * Picks a reader theme by key, or null to follow the system again.
     */
    fun setThemeKey(key: String?) {
        viewModelScope.launch { settingsRepository.setThemeKey(key) }
    }

    fun setReadingMode(mode: ReadingMode) {
        viewModelScope.launch { settingsRepository.setReadingMode(mode) }
    }

    fun setTextAlignment(alignment: TextAlignment) {
        viewModelScope.launch { settingsRepository.setTextAlignment(alignment) }
    }

    /**
     * Picks what the lower right corner shows. Only the label changes, the pages stay the same.
     */
    fun setProgressLabel(label: ProgressLabel) {
        viewModelScope.launch { settingsRepository.setProgressLabel(label) }
    }
}
