package com.rk.committee.data.repository

import androidx.room.withTransaction
import com.rk.committee.data.local.AppDatabase
import com.rk.committee.data.local.CommitteeEntity
import com.rk.committee.data.local.MemberEntity
import com.rk.committee.data.local.PaymentEntity
import com.rk.committee.data.local.PayoutEntity
import com.rk.committee.domain.CommitteeDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommitteeRepository @Inject constructor(private val db: AppDatabase) {
    private val committeeDao = db.committeeDao()
    private val memberDao = db.memberDao()
    private val paymentDao = db.paymentDao()
    private val payoutDao = db.payoutDao()

    fun observeAllDetails(): Flow<List<CommitteeDetails>> = combine(
        committeeDao.observeAll(),
        memberDao.observeAll(),
        paymentDao.observeAll(),
        payoutDao.observeAll(),
    ) { committees, members, payments, payouts ->
        val m = members.groupBy { it.committeeId }
        val p = payments.groupBy { it.committeeId }
        val po = payouts.groupBy { it.committeeId }
        committees.map { c ->
            CommitteeDetails(
                committee = c,
                members = m[c.id].orEmpty().sortedBy { it.turnOrder },
                payments = p[c.id].orEmpty(),
                payouts = po[c.id].orEmpty(),
            )
        }
    }

    fun observeDetails(id: Long): Flow<CommitteeDetails?> = combine(
        committeeDao.observe(id),
        memberDao.observeFor(id),
        paymentDao.observeFor(id),
        payoutDao.observeFor(id),
    ) { c, members, payments, payouts ->
        c?.let { CommitteeDetails(it, members, payments, payouts) }
    }

    suspend fun getCommittee(id: Long): CommitteeEntity? = committeeDao.get(id)

    /** Insert or update. Returns the committee id. */
    suspend fun saveCommittee(committee: CommitteeEntity): Long =
        if (committee.id == 0L) {
            committeeDao.insert(committee)
        } else {
            committeeDao.update(committee)
            committee.id
        }

    suspend fun deleteCommittee(committee: CommitteeEntity) = committeeDao.delete(committee)

    suspend fun addMember(committeeId: Long, name: String, phone: String) = db.withTransaction {
        val order = memberDao.maxOrder(committeeId) + 1
        memberDao.insert(MemberEntity(committeeId = committeeId, name = name, phone = phone, turnOrder = order))
    }

    suspend fun updateMember(member: MemberEntity) = memberDao.update(member)

    /** Deletes a member and closes the gap in the turn order. */
    suspend fun deleteMember(member: MemberEntity) = db.withTransaction {
        memberDao.delete(member)
        val rest = memberDao.getFor(member.committeeId)
        memberDao.updateAll(rest.mapIndexed { i, m -> m.copy(turnOrder = i + 1) })
    }

    suspend fun swapOrder(a: MemberEntity, b: MemberEntity) = db.withTransaction {
        memberDao.update(a.copy(turnOrder = b.turnOrder))
        memberDao.update(b.copy(turnOrder = a.turnOrder))
    }

    suspend fun setPaid(committeeId: Long, memberId: Long, round: Int, amount: Long, paid: Boolean) {
        if (paid) {
            paymentDao.upsert(PaymentEntity(committeeId = committeeId, memberId = memberId, round = round, amount = amount))
        } else {
            paymentDao.delete(memberId, round)
        }
    }

    suspend fun markAllPaid(committeeId: Long, memberIds: List<Long>, round: Int, amount: Long) {
        paymentDao.upsertAll(
            memberIds.map { PaymentEntity(committeeId = committeeId, memberId = it, round = round, amount = amount) },
        )
    }

    suspend fun addPayout(committeeId: Long, memberId: Long, round: Int, amount: Long) =
        payoutDao.upsert(PayoutEntity(committeeId = committeeId, memberId = memberId, round = round, amount = amount))

    suspend fun removePayout(committeeId: Long, round: Int) = payoutDao.delete(committeeId, round)
}
