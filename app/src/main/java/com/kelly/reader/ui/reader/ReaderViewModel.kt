package com.kelly.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.PageCache
import com.kelly.reader.data.SettingsRepository
import com.kelly.reader.domain.LineSpacing
import com.kelly.reader.domain.ProgressLabel
import com.kelly.reader.domain.ReaderFont
import com.kelly.reader.domain.ReaderSettings
import com.kelly.reader.domain.ReadingMode
import com.kelly.reader.domain.TextAlignment

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

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
            // the saved percentage shows while a big book's text is still being read; the book
            // itself goes in with its text, so nothing is paged before the text is there
            _uiState.update { it.copy(savedProgress = book.progress) }
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
        paginate()
    }

    private fun paginate() {
        val key = measurerKey ?: return
        val measurer = measurer ?: return
        val text = _uiState.value.text
        if (_uiState.value.book == null) return
        paginationJob?.cancel()
        waitingStarts = null
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
            // the pages from the start of the book arrive after them
            Paginator.paginate(text, measurer, paginationDispatcher, anchor).collect { starts ->
                last = starts
                if (!fromSpot || starts[0] != 0) _uiState.update { it.copy(pageStarts = starts) }
            }
            last?.let { starts ->
                pageCache.save(bookId, key, starts)
                cache[key] = starts
                // in a book measured from the reading spot, the pages from the start would move
                // the text being read, so they wait for the next page turn unless there is none
                if (fromSpot && _uiState.value.hasNextPage) {
                    waitingStarts = starts
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
     * Shows the pages held back since the last change, if any, and says whether there were.
     */
    private fun showWaitingPages(): Boolean {
        val starts = waitingStarts ?: return false
        waitingStarts = null
        _uiState.update { it.copy(pageStarts = starts, paginationDone = true) }
        return true
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
