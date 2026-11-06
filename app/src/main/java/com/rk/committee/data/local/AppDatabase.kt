package com.rk.committee.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [CommitteeEntity::class, MemberEntity::class, PaymentEntity::class, PayoutEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun committeeDao(): CommitteeDao
    abstract fun memberDao(): MemberDao
    abstract fun paymentDao(): PaymentDao
    abstract fun payoutDao(): PayoutDao
}
