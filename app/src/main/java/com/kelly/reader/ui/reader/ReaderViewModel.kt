package com.kelly.reader.ui.reader

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

import com.kelly.reader.data.BookRepository
import com.kelly.reader.data.PageCache
import com.kelly.reader.data.SettingsRepository
import com.kelly.reader.domain.LineSpacing
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

    init {
        viewModelScope.launch {
            val book = bookRepository.getBook(bookId) ?: return@launch
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
            settingsRepository.recordReadingDay(LocalDate.now(clock))
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
            Paginator.paginate(text, measurer, paginationDispatcher, anchor).conflate().collect { starts ->
                last = starts
                _uiState.update { it.copy(pageStarts = starts) }
            }
            last?.let { starts ->
                pageCache.save(bookId, key, starts)
                finishPagination(key, starts)
            }
        }
    }

    private fun finishPagination(key: String, starts: IntArray) {
        cache[key] = starts
        _uiState.update { it.copy(pageStarts = starts, paginationDone = true) }
        saveProgress()
    }

    fun nextPage() {
        val state = _uiState.value
        val starts = state.pageStarts ?: return
        if (!state.hasNextPage) return
        moveTo(starts[state.pageIndex + 1])
    }

    fun previousPage() {
        val state = _uiState.value
        val starts = state.pageStarts ?: return
        if (!state.hasPreviousPage) return
        moveTo(starts[state.pageIndex - 1])
    }

    /**
     * In scroll mode the screen reports the page now at the top; it becomes the position.
     */
    fun moveToPage(index: Int) {
        val starts = _uiState.value.pageStarts ?: return
        if (index !in starts.indices) return
        if (starts[index] == _uiState.value.offset) return
        moveTo(starts[index])
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
}
