package com.rk.committee.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommitteeDao {
    @Query("SELECT * FROM committees ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<CommitteeEntity>>

    @Query("SELECT * FROM committees WHERE id = :id")
    fun observe(id: Long): Flow<CommitteeEntity?>

    @Query("SELECT * FROM committees WHERE id = :id")
    suspend fun get(id: Long): CommitteeEntity?

    @Insert
    suspend fun insert(committee: CommitteeEntity): Long

    @Update
    suspend fun update(committee: CommitteeEntity)

    @Delete
    suspend fun delete(committee: CommitteeEntity)
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY turnOrder")
    fun observeAll(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE committeeId = :committeeId ORDER BY turnOrder")
    fun observeFor(committeeId: Long): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE committeeId = :committeeId ORDER BY turnOrder")
    suspend fun getFor(committeeId: Long): List<MemberEntity>

    @Query("SELECT COALESCE(MAX(turnOrder), 0) FROM members WHERE committeeId = :committeeId")
    suspend fun maxOrder(committeeId: Long): Int

    @Insert
    suspend fun insert(member: MemberEntity): Long

    @Update
    suspend fun update(member: MemberEntity)

    @Update
    suspend fun updateAll(members: List<MemberEntity>)

    @Delete
    suspend fun delete(member: MemberEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments")
    fun observeAll(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE committeeId = :committeeId")
    fun observeFor(committeeId: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payment: PaymentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(payments: List<PaymentEntity>)

    @Query("DELETE FROM payments WHERE memberId = :memberId AND round = :round")
    suspend fun delete(memberId: Long, round: Int)
}

@Dao
interface PayoutDao {
    @Query("SELECT * FROM payouts")
    fun observeAll(): Flow<List<PayoutEntity>>

    @Query("SELECT * FROM payouts WHERE committeeId = :committeeId")
    fun observeFor(committeeId: Long): Flow<List<PayoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(payout: PayoutEntity)

    @Query("DELETE FROM payouts WHERE committeeId = :committeeId AND round = :round")
    suspend fun delete(committeeId: Long, round: Int)
}
