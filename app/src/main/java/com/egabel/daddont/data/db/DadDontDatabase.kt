package com.egabel.daddont.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.egabel.daddont.data.model.BreachEvent
import com.egabel.daddont.data.model.DesireCheckIn
import com.egabel.daddont.data.model.DialogSession
import com.egabel.daddont.data.model.Impulse
import com.egabel.daddont.data.model.ReturnEvent

@Database(
    entities = [Impulse::class, ReturnEvent::class, DialogSession::class, DesireCheckIn::class, BreachEvent::class],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class DadDontDatabase : RoomDatabase() {
    abstract fun impulseDao(): ImpulseDao
    abstract fun returnEventDao(): ReturnEventDao
    abstract fun dialogSessionDao(): DialogSessionDao
    abstract fun desireCheckInDao(): DesireCheckInDao
    abstract fun breachEventDao(): BreachEventDao

    companion object {
        @Volatile
        private var INSTANCE: DadDontDatabase? = null

        fun getInstance(context: Context): DadDontDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DadDontDatabase::class.java,
                    "daddont.db"
                ).fallbackToDestructiveMigration(dropAllTables = true)
                 .build().also { INSTANCE = it }
            }
        }
    }
}
