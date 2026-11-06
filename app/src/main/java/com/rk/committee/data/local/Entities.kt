package com.rk.committee.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** One committee (BC). Rounds are monthly, starting from startYear/startMonth. */
@Entity(tableName = "committees")
data class CommitteeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amountPerMember: Long,
    val startYear: Int,
    val startMonth: Int, // 1..12
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

/** A member. turnOrder 1 gets the pot in month 1, turnOrder 2 in month 2, and so on. */
@Entity(
    tableName = "members",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("committeeId")],
)
data class MemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val committeeId: Long,
    val name: String,
    val phone: String = "",
    val turnOrder: Int,
)

/** A member paid for a round (round is 0-based: 0 = first month). */
@Entity(
    tableName = "payments",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("committeeId"), Index(value = ["memberId", "round"], unique = true)],
)
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val committeeId: Long,
    val memberId: Long,
    val round: Int,
    val amount: Long,
    val paidAt: Long = System.currentTimeMillis(),
)

/** The pot for a round was handed to its member. */
@Entity(
    tableName = "payouts",
    foreignKeys = [
        ForeignKey(
            entity = CommitteeEntity::class,
            parentColumns = ["id"],
            childColumns = ["committeeId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["committeeId", "round"], unique = true), Index("memberId")],
)
data class PayoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val committeeId: Long,
    val memberId: Long,
    val round: Int,
    val amount: Long,
    val paidAt: Long = System.currentTimeMillis(),
)
