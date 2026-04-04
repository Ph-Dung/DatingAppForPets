package com.petmatch.mobile.ui.match

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmatch.mobile.data.api.RetrofitClient
import com.petmatch.mobile.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MatchViewModel : ViewModel() {

    // Suggestions (swipe deck)
    private val _suggestions = MutableStateFlow<List<PetProfileResponse>>(emptyList())
    val suggestions: StateFlow<List<PetProfileResponse>> = _suggestions

    private val _suggestionPage = MutableStateFlow(0)
    private val _hasMoreSuggestions = MutableStateFlow(true)

    private val _isLoadingSuggestions = MutableStateFlow(false)
    val isLoadingSuggestions: StateFlow<Boolean> = _isLoadingSuggestions

    // ── AI Smart mode tracking ────────────────────────────
    /** Tổng số like đã gửi từ đầu session */
    private var totalSessionLikes = 0
    /** Đủ 5 likes → bật smart mode, server sẽ sort theo AI score */
    private val _isSmartMode = MutableStateFlow(false)
    val isSmartMode: StateFlow<Boolean> = _isSmartMode

    // Super like status
    private val _superLikeStatus = MutableStateFlow<SuperLikeStatusResponse?>(null)
    val superLikeStatus: StateFlow<SuperLikeStatusResponse?> = _superLikeStatus

    // Who liked me
    private val _whoLikedMe = MutableStateFlow<List<MatchRequestResponse>>(emptyList())
    val whoLikedMe: StateFlow<List<MatchRequestResponse>> = _whoLikedMe

    // Matched list
    private val _matched = MutableStateFlow<List<MatchRequestResponse>>(emptyList())
    val matched: StateFlow<List<MatchRequestResponse>> = _matched

    // Match popup
    private val _matchPopup = MutableStateFlow<MatchRequestResponse?>(null)
    val matchPopup: StateFlow<MatchRequestResponse?> = _matchPopup

    // Filters
    private val _filterSpecies = MutableStateFlow<String?>(null)
    val filterSpecies: StateFlow<String?> = _filterSpecies
    private val _filterGender = MutableStateFlow<String?>(null)
    val filterGender: StateFlow<String?> = _filterGender
    private val _filterLookingFor = MutableStateFlow<String?>(null)
    val filterLookingFor: StateFlow<String?> = _filterLookingFor
    private val _filterMinAge = MutableStateFlow<Int?>(null)
    val filterMinAge: StateFlow<Int?> = _filterMinAge
    private val _filterMaxAge = MutableStateFlow<Int?>(null)
    val filterMaxAge: StateFlow<Int?> = _filterMaxAge
    private val _filterHealthStatus = MutableStateFlow<String?>(null)
    val filterHealthStatus: StateFlow<String?> = _filterHealthStatus

    fun loadSuggestions(ctx: Context, refresh: Boolean = false) = viewModelScope.launch {
        if (_isLoadingSuggestions.value) return@launch
        if (refresh) {
            _suggestionPage.value = 0
            _hasMoreSuggestions.value = true
            _suggestions.value = emptyList()
        }
        if (!_hasMoreSuggestions.value) return@launch
        _isLoadingSuggestions.value = true
        try {
            val hasFilters = _filterSpecies.value != null || _filterGender.value != null ||
                    _filterLookingFor.value != null || _filterMinAge.value != null ||
                    _filterMaxAge.value != null || _filterHealthStatus.value != null

            val res = if (hasFilters) {
                RetrofitClient.petApi(ctx).search(
                    species = _filterSpecies.value,
                    gender = _filterGender.value,
                    lookingFor = _filterLookingFor.value,
                    minAge = _filterMinAge.value,
                    maxAge = _filterMaxAge.value,
                    healthStatus = _filterHealthStatus.value,
                    page = _suggestionPage.value,
                    size = 5
                )
            } else {
                RetrofitClient.petApi(ctx).getSuggestions(_suggestionPage.value, 5, _isSmartMode.value)
            }
            if (res.isSuccessful) {
                val page = res.body()!!
                _suggestions.value = _suggestions.value + page.content
                _hasMoreSuggestions.value = !page.last
                _suggestionPage.value++
            }
        } catch (_: Exception) {}
        _isLoadingSuggestions.value = false
    }

    fun sendLike(ctx: Context, petId: Long, isSuperLike: Boolean) = viewModelScope.launch {
        try {
            val res = RetrofitClient.matchApi(ctx)
                .sendMatchRequest(SendMatchRequest(petId, isSuperLike))
            if (res.isSuccessful) {
                val resp = res.body()!!
                // Hiện popup nếu auto-match (cả 2 like nhau)
                if (resp.status == "ACCEPTED" && resp.canOpenConversation) {
                    _matchPopup.value = resp
                }
                if (isSuperLike) loadSuperLikeStatus(ctx)

                // ── AI preference tracking ────────────────────
                totalSessionLikes++
                // Sau 5 likes → bật smart mode
                if (totalSessionLikes >= 5 && !_isSmartMode.value) {
                    _isSmartMode.value = true
                }
            }
        } catch (_: Exception) {}

        _suggestions.value = _suggestions.value.drop(1)
        if (_suggestions.value.size <= 2) loadSuggestions(ctx)
    }

    fun sendDislike(ctx: Context) = viewModelScope.launch {
        _suggestions.value = _suggestions.value.drop(1)
        if (_suggestions.value.size <= 2) loadSuggestions(ctx)
    }

    fun dismissMatchPopup() { _matchPopup.value = null }

    fun loadSuperLikeStatus(ctx: Context) = viewModelScope.launch {
        try {
            val res = RetrofitClient.matchApi(ctx).getSuperLikeStatus()
            if (res.isSuccessful) _superLikeStatus.value = res.body()
        } catch (_: Exception) {}
    }

    fun loadWhoLikedMe(ctx: Context) = viewModelScope.launch {
        try {
            val res = RetrofitClient.matchApi(ctx).getWhoLikedMe()
            if (res.isSuccessful) _whoLikedMe.value = res.body() ?: emptyList()
        } catch (_: Exception) {}
    }

    fun loadMatched(ctx: Context) = viewModelScope.launch {
        try {
            val res = RetrofitClient.matchApi(ctx).getMatchedList()
            if (res.isSuccessful) _matched.value = res.body() ?: emptyList()
        } catch (_: Exception) {}
    }

    fun applyFilters(
        species: String?, gender: String?, lookingFor: String?,
        minAge: Int?, maxAge: Int?, healthStatus: String?,
        ctx: Context
    ) {
        _filterSpecies.value = species
        _filterGender.value = gender
        _filterLookingFor.value = lookingFor
        _filterMinAge.value = minAge
        _filterMaxAge.value = maxAge
        _filterHealthStatus.value = healthStatus
        loadSuggestions(ctx, refresh = true)
    }

    fun clearFilters(ctx: Context) = applyFilters(null, null, null, null, null, null, ctx)
}
