package com.rk.committee.domain

import com.rk.committee.data.local.CommitteeEntity
import com.rk.committee.data.local.MemberEntity
import com.rk.committee.data.local.PaymentEntity
import com.rk.committee.data.local.PayoutEntity
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * All data for one committee plus the business rules.
 * Round r (0-based) = month (start + r). Member at position r (by turnOrder) gets the pot in round r.
 */
data class CommitteeDetails(
    val committee: CommitteeEntity,
    val members: List<MemberEntity>,
    val payments: List<PaymentEntity>,
    val payouts: List<PayoutEntity>,
    val today: YearMonth = YearMonth.now(),
) {
    private val paidKeys: Set<Pair<Long, Int>> = payments.map { it.memberId to it.round }.toHashSet()

    val totalRounds: Int get() = members.size
    val potAmount: Long get() = committee.amountPerMember * members.size
    val startMonth: YearMonth get() = YearMonth.of(committee.startYear, committee.startMonth)
    val endMonth: YearMonth? get() = if (totalRounds == 0) null else monthOf(totalRounds - 1)

    /** Month index for today. Can be negative (not started) or past the end. */
    val roundForToday: Int get() = ChronoUnit.MONTHS.between(startMonth, today).toInt()
    val isStarted: Boolean get() = roundForToday >= 0

    /** Current round, clamped to a valid round. */
    val currentRound: Int
        get() = if (totalRounds == 0) 0 else roundForToday.coerceIn(0, totalRounds - 1)

    /** First round whose pot has not been given yet -> whose turn is next. */
    val nextTurnRound: Int? get() = (0 until totalRounds).firstOrNull { payoutFor(it) == null }
    val isCompleted: Boolean get() = totalRounds > 0 && nextTurnRound == null
    val totalCollected: Long get() = payments.sumOf { it.amount }

    fun monthOf(round: Int): YearMonth = startMonth.plusMonths(round.toLong())
    fun isPaid(memberId: Long, round: Int): Boolean = (memberId to round) in paidKeys
    fun payoutFor(round: Int): PayoutEntity? = payouts.firstOrNull { it.round == round }
    fun receiverFor(round: Int): MemberEntity? = members.getOrNull(round)
    fun roundOf(memberId: Long): Int = members.indexOfFirst { it.id == memberId }
    fun collectedFor(round: Int): Long = payments.filter { it.round == round }.sumOf { it.amount }
    fun unpaidMembers(round: Int): List<MemberEntity> = members.filterNot { isPaid(it.id, round) }
    fun paidCount(memberId: Long): Int = payments.count { it.memberId == memberId }

    /** Months that are due (up to today) but not paid. */
    fun pendingRounds(memberId: Long): Int {
        if (!isStarted || totalRounds == 0) return 0
        return (0..currentRound).count { !isPaid(memberId, it) }
    }

    val defaulters: List<MemberEntity> get() = members.filter { pendingRounds(it.id) > 0 }
}
