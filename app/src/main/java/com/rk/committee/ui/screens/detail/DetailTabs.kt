package com.rk.committee.ui.screens.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rk.committee.R
import com.rk.committee.data.local.MemberEntity
import com.rk.committee.domain.CommitteeDetails
import com.rk.committee.ui.components.EmptyState
import com.rk.committee.ui.components.LabelValue
import com.rk.committee.ui.components.SectionCard
import com.rk.committee.ui.components.StatusPill
import com.rk.committee.ui.util.money
import com.rk.committee.ui.util.monthLabel

// ---------------------------------------------------------------- Overview

@Composable
fun OverviewTab(d: CommitteeDetails, onOpenPayments: (Int) -> Unit, onAddMembers: () -> Unit) {
    if (d.members.isEmpty()) {
        NoMembers(onAddMembers)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val round = d.currentRound
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        // Big status card
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        when {
                            d.isCompleted -> stringResource(R.string.committee_completed)
                            !d.isStarted -> stringResource(R.string.starts_on, monthLabel(d.startMonth))
                            else -> stringResource(R.string.month_x_of_y, round + 1, d.totalRounds) +
                                " · " + monthLabel(d.monthOf(round))
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        money(d.potAmount),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(stringResource(R.string.pot_per_month), style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { d.payouts.size.toFloat() / d.totalRounds },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = scheme.secondaryContainer,
                        trackColor = scheme.onPrimary.copy(alpha = 0.25f),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.payouts_done, d.payouts.size, d.totalRounds),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        // This month
        if (!d.isCompleted) {
            item {
                val receiver = d.receiverFor(round)
                val collected = d.collectedFor(round)
                val unpaid = d.unpaidMembers(round)
                SectionCard(title = stringResource(R.string.this_month_title, monthLabel(d.monthOf(round)))) {
                    LabelValue(stringResource(R.string.receiver), receiver?.name.orEmpty())
                    LabelValue(stringResource(R.string.collected), "${money(collected)} / ${money(d.potAmount)}")
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { if (d.potAmount == 0L) 0f else (collected.toFloat() / d.potAmount).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        strokeCap = StrokeCap.Round,
                    )
                    Spacer(Modifier.height(10.dp))
                    if (unpaid.isEmpty()) {
                        StatusPill(stringResource(R.string.all_paid), scheme.primaryContainer, scheme.onPrimaryContainer)
                    } else {
                        Text(
                            stringResource(R.string.not_paid_yet),
                            style = MaterialTheme.typography.labelMedium,
                            color = scheme.error,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(unpaid.joinToString(", ") { it.name }, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { onOpenPayments(round) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Payments, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.open_payments))
                    }
                }
            }
        }

        // Next turn
        d.nextTurnRound?.let { next ->
            item {
                SectionCard(
                    title = stringResource(R.string.next_turn),
                    containerColor = scheme.secondaryContainer,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OrderBadge(next + 1, scheme.secondary, scheme.onSecondary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                d.receiverFor(next)?.name.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = scheme.onSecondaryContainer,
                            )
                            Text(
                                monthLabel(d.monthOf(next)),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSecondaryContainer,
                            )
                        }
                        Text(
                            money(d.potAmount),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        // Defaulters
        item {
            val defaulters = d.defaulters
            SectionCard(title = stringResource(R.string.pending_payments)) {
                if (defaulters.isEmpty()) {
                    Text(stringResource(R.string.no_pending), style = MaterialTheme.typography.bodyMedium)
                } else {
                    defaulters.forEach { m ->
                        val months = d.pendingRounds(m.id)
                        LabelValue(
                            label = m.name,
                            value = pluralStringResource(R.plurals.months_pending, months, months) +
                                " · " + money(months * d.committee.amountPerMember),
                        )
                    }
                }
            }
        }

        // Summary
        item {
            SectionCard(title = stringResource(R.string.summary)) {
                LabelValue(stringResource(R.string.amount_per_member), money(d.committee.amountPerMember))
                LabelValue(stringResource(R.string.members), "${d.totalRounds}")
                LabelValue(stringResource(R.string.start_month), monthLabel(d.startMonth))
                d.endMonth?.let { LabelValue(stringResource(R.string.end_month), monthLabel(it)) }
                LabelValue(stringResource(R.string.total_collected), money(d.totalCollected))
                if (d.committee.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(d.committee.notes, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Members

@Composable
fun MembersTab(d: CommitteeDetails, onEdit: (MemberEntity) -> Unit, onMove: (Int, Boolean) -> Unit) {
    if (d.members.isEmpty()) {
        EmptyState(
            icon = Icons.Filled.PersonAdd,
            title = stringResource(R.string.no_members_title),
            message = stringResource(R.string.no_members_message),
        )
        return
    }
    val scheme = MaterialTheme.colorScheme
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                stringResource(R.string.order_help),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        itemsIndexed(d.members, key = { _, m -> m.id }) { index, m ->
            val received = d.payoutFor(index) != null
            val pending = d.pendingRounds(m.id)
            Card(
                onClick = { onEdit(m) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = scheme.surfaceContainerLow),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    OrderBadge(
                        index + 1,
                        if (received) scheme.primary else scheme.surfaceVariant,
                        if (received) scheme.onPrimary else scheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            m.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (m.phone.isNotBlank()) {
                            Text(m.phone, style = MaterialTheme.typography.bodySmall, color = scheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (received) {
                                StatusPill(
                                    stringResource(R.string.received_in, monthLabel(d.monthOf(index))),
                                    scheme.primaryContainer,
                                    scheme.onPrimaryContainer,
                                )
                            } else {
                                StatusPill(
                                    stringResource(R.string.turn_in, monthLabel(d.monthOf(index))),
                                    scheme.surfaceVariant,
                                    scheme.onSurfaceVariant,
                                )
                            }
                            if (pending > 0) {
                                StatusPill(
                                    pluralStringResource(R.plurals.months_pending, pending, pending),
                                    scheme.errorContainer,
                                    scheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    Column {
                        IconButton(onClick = { onMove(index, true) }, enabled = index > 0) {
                            Icon(Icons.Filled.KeyboardArrowUp, stringResource(R.string.move_up))
                        }
                        IconButton(onClick = { onMove(index, false) }, enabled = index < d.members.lastIndex) {
                            Icon(Icons.Filled.KeyboardArrowDown, stringResource(R.string.move_down))
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- Payments

@Composable
fun PaymentsTab(
    d: CommitteeDetails,
    round: Int,
    onSelectRound: (Int) -> Unit,
    onTogglePaid: (Long, Int) -> Unit,
    onMarkAllPaid: (Int) -> Unit,
    onGivePayout: (Int) -> Unit,
    onRemovePayout: (Int) -> Unit,
    onAddMembers: () -> Unit,
) {
    if (d.members.isEmpty()) {
        NoMembers(onAddMembers)
        return
    }
    val scheme = MaterialTheme.colorScheme
    val rowState = rememberLazyListState()
    LaunchedEffect(round) { rowState.animateScrollToItem((round - 1).coerceAtLeast(0)) }

    val receiver = d.receiverFor(round)
    val payout = d.payoutFor(round)
    val collected = d.collectedFor(round)
    val unpaid = d.unpaidMembers(round)

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            state = rowState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(d.totalRounds) { r ->
                FilterChip(
                    selected = r == round,
                    onClick = { onSelectRound(r) },
                    label = { Text(monthLabel(d.monthOf(r))) },
                    leadingIcon = if (d.payoutFor(r) != null) {
                        { Icon(Icons.Filled.CheckCircle, null, Modifier.size(16.dp)) }
                    } else null,
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                SectionCard(containerColor = scheme.surfaceContainer) {
                    Text(
                        stringResource(R.string.month_x_of_y, round + 1, d.totalRounds),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.primary,
                    )
                    Spacer(Modifier.height(6.dp))
                    LabelValue(stringResource(R.string.receiver), receiver?.name.orEmpty())
                    LabelValue(stringResource(R.string.collected), "${money(collected)} / ${money(d.potAmount)}")
                    LabelValue(
                        stringResource(R.string.paid_members),
                        "${d.totalRounds - unpaid.size} / ${d.totalRounds}",
                    )
                    Spacer(Modifier.height(10.dp))
                    if (payout != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = scheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.payout_given_to, receiver?.name.orEmpty()),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onRemovePayout(round) }) { Text(stringResource(R.string.undo)) }
                        }
                    } else {
                        Button(onClick = { onGivePayout(round) }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.give_payout_to, receiver?.name.orEmpty()))
                        }
                        if (unpaid.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                stringResource(R.string.payout_warning_unpaid),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Text(
                        stringResource(R.string.tap_to_mark),
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { onMarkAllPaid(round) }, enabled = unpaid.isNotEmpty()) {
                        Text(stringResource(R.string.mark_all_paid))
                    }
                }
            }

            items(d.members, key = { it.id }) { m ->
                val paid = d.isPaid(m.id, round)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTogglePaid(m.id, round) }
                            .padding(vertical = 6.dp),
                    ) {
                        Checkbox(checked = paid, onCheckedChange = { onTogglePaid(m.id, round) })
                        Column(Modifier.weight(1f)) {
                            Text(m.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                money(d.committee.amountPerMember),
                                style = MaterialTheme.typography.bodySmall,
                                color = scheme.onSurfaceVariant,
                            )
                        }
                        if (paid) {
                            StatusPill(stringResource(R.string.paid), scheme.primaryContainer, scheme.onPrimaryContainer)
                        } else {
                            StatusPill(stringResource(R.string.unpaid), scheme.errorContainer, scheme.onErrorContainer)
                        }
                    }
                    HorizontalDivider(color = scheme.outline.copy(alpha = 0.15f))
                }
            }
        }
    }
}

// ---------------------------------------------------------------- helpers

@Composable
private fun NoMembers(onAddMembers: () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f)) {
            EmptyState(
                icon = Icons.Filled.PersonAdd,
                title = stringResource(R.string.no_members_title),
                message = stringResource(R.string.no_members_message),
            )
        }
        Button(onClick = onAddMembers, modifier = Modifier.padding(24.dp).fillMaxWidth()) {
            Text(stringResource(R.string.add_member))
        }
    }
}

@Composable
private fun OrderBadge(number: Int, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Box(
        Modifier.size(40.dp).background(container, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("$number", color = content, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
    }
}
