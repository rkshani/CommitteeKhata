package com.rk.committee.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rk.committee.R
import com.rk.committee.data.repository.CommitteeRepository
import com.rk.committee.domain.CommitteeDetails
import com.rk.committee.ui.components.EmptyState
import com.rk.committee.ui.components.LoadingBox
import com.rk.committee.ui.components.StatusPill
import com.rk.committee.ui.util.money
import com.rk.committee.ui.util.monthLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(repo: CommitteeRepository) : ViewModel() {
    /** null = still loading */
    val committees: StateFlow<List<CommitteeDetails>?> =
        repo.observeAllDetails().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val list by vm.committees.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, stringResource(R.string.settings))
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Filled.Add, null) },
                text = { Text(stringResource(R.string.new_committee)) },
            )
        },
    ) { padding ->
        val items = list
        when {
            items == null -> LoadingBox(Modifier.padding(padding))
            items.isEmpty() -> EmptyState(
                icon = Icons.Filled.Groups,
                title = stringResource(R.string.empty_committees_title),
                message = stringResource(R.string.empty_committees_message),
                modifier = Modifier.padding(padding),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { SummaryHeader(items) }
                items(items, key = { it.committee.id }) { d ->
                    CommitteeCard(d, onClick = { onOpen(d.committee.id) })
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(list: List<CommitteeDetails>) {
    val active = list.count { !it.isCompleted }
    val unpaid = list.filter { it.isStarted && !it.isCompleted }.sumOf { it.unpaidMembers(it.currentRound).size }
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(20.dp)) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.active_committees), style = MaterialTheme.typography.labelLarge)
                Text("$active", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.unpaid_this_month), style = MaterialTheme.typography.labelLarge)
                Text("$unpaid", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CommitteeCard(d: CommitteeDetails, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    d.committee.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                when {
                    d.totalRounds == 0 -> StatusPill(stringResource(R.string.no_members), scheme.surfaceVariant, scheme.onSurfaceVariant)
                    d.isCompleted -> StatusPill(stringResource(R.string.completed), scheme.primaryContainer, scheme.onPrimaryContainer)
                    !d.isStarted -> StatusPill(stringResource(R.string.not_started), scheme.surfaceVariant, scheme.onSurfaceVariant)
                    else -> {
                        val unpaid = d.unpaidMembers(d.currentRound).size
                        if (unpaid == 0) {
                            StatusPill(stringResource(R.string.all_paid), scheme.primaryContainer, scheme.onPrimaryContainer)
                        } else {
                            StatusPill(
                                pluralStringResource(R.plurals.unpaid_count, unpaid, unpaid),
                                scheme.errorContainer,
                                scheme.onErrorContainer,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.amount_x_members, money(d.committee.amountPerMember), d.totalRounds),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
            )
            if (d.totalRounds > 0) {
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { d.payouts.size.toFloat() / d.totalRounds },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                Spacer(Modifier.height(10.dp))
                Row {
                    Text(
                        stringResource(R.string.month_x_of_y, d.currentRound + 1, d.totalRounds) +
                            " · " + monthLabel(d.monthOf(d.currentRound)),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        stringResource(R.string.pot_value, money(d.potAmount)),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                val next = d.nextTurnRound
                if (next != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.next_turn_value, d.receiverFor(next)?.name.orEmpty(), monthLabel(d.monthOf(next))),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.secondary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
