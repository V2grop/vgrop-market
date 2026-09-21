package com.v2grop.lbankpulse.presentation
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.v2grop.lbankpulse.data.ResearchCacheRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
/** First presentation-layer migration seam; existing Java screens remain operational. */
class ResearchHistoryViewModel(repository: ResearchCacheRepository) : ViewModel() {
    val history = repository.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
