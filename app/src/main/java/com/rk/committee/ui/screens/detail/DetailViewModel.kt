package com.rk.committee.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rk.committee.R
import com.rk.committee.data.repository.CommitteeRepository
import com.rk.committee.domain.CommitteeDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: CommitteeRepository,
) : ViewModel() {
    private val committeeId: Long = checkNotNull(savedStateHandle.get<Long>("id"))

    val details: StateFlow<CommitteeDetails?> =
        repo.observeDetails(committeeId).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Round shown on the Payments tab. null = current month. */
    private val _selectedRound = MutableStateFlow<Int?>(null)
    val selectedRound: StateFlow<Int?> = _selectedRound.asStateFlow()

    /** One-time messages (string resource ids) for the snackbar. */
    private val _messages = MutableSharedFlow<Int>(extraBufferCapacity = 4)
    val messages: SharedFlow<Int> = _messages.asSharedFlow()

    fun selectRound(round: Int) {
        _selectedRound.value = round
    }

    fun saveMember(memberId: Long?, name: String, phone: String) {
        val cleanName = name.trim()
        if (cleanName.isEmpty()) {
            _messages.tryEmit(R.string.error_name_required)
            return
        }
        viewModelScope.launch {
            if (memberId == null) {
                repo.addMember(committeeId, cleanName, phone.trim())
                _messages.tryEmit(R.string.msg_member_added)
            } else {
                val m = details.value?.members?.firstOrNull { it.id == memberId } ?: return@launch
                repo.updateMember(m.copy(name = cleanName, phone = phone.trim()))
            }
        }
    }

    fun deleteMember(memberId: Long) {
        val d = details.value ?: return
        if (d.payouts.isNotEmpty()) {
            _messages.tryEmit(R.string.msg_cannot_delete_member)
            return
        }
        val m = d.members.firstOrNull { it.id == memberId } ?: return
        viewModelScope.launch { repo.deleteMember(m) }
    }

    /** Move member up/down in the turn order. Locked once their turn's pot is given. */
    fun moveMember(index: Int, up: Boolean) {
        val d = details.value ?: return
        val target = if (up) index - 1 else index + 1
        if (target !in d.members.indices) return
        if (d.payoutFor(index) != null || d.payoutFor(target) != null) {
            _messages.tryEmit(R.string.msg_cannot_reorder)
            return
        }
        viewModelScope.launch { repo.swapOrder(d.members[index], d.members[target]) }
    }

    fun togglePaid(memberId: Long, round: Int) {
        val d = details.value ?: return
        viewModelScope.launch {
            repo.setPaid(committeeId, memberId, round, d.committee.amountPerMember, paid = !d.isPaid(memberId, round))
        }
    }

    fun markAllPaid(round: Int) {
        val d = details.value ?: return
        val ids = d.unpaidMembers(round).map { it.id }
        if (ids.isEmpty()) return
        viewModelScope.launch { repo.markAllPaid(committeeId, ids, round, d.committee.amountPerMember) }
    }

    fun setPayout(round: Int, given: Boolean) {
        val d = details.value ?: return
        val receiver = d.receiverFor(round) ?: return
        viewModelScope.launch {
            if (given) {
                repo.addPayout(committeeId, receiver.id, round, d.potAmount)
                _messages.tryEmit(R.string.msg_payout_saved)
            } else {
                repo.removePayout(committeeId, round)
            }
        }
    }

    fun deleteCommittee(onDone: () -> Unit) {
        val c = details.value?.committee ?: return
        viewModelScope.launch {
            repo.deleteCommittee(c)
            onDone()
        }
    }
}
