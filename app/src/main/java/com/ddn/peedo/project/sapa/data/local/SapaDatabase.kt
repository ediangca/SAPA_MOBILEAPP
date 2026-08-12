package com.ddn.peedo.project.sapa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ddn.peedo.project.sapa.data.local.dao.*
import com.ddn.peedo.project.sapa.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        SchoolEntity::class,
        SlotEntity::class,
        AppointedStudentEntity::class,
        SettingsEntity::class,
        AttendanceQueueEntity::class,
        SyncMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SapaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun schoolDao(): SchoolDao
    abstract fun slotDao(): SlotDao
    abstract fun appointedStudentDao(): AppointedStudentDao
    abstract fun settingsDao(): SettingsDao
    abstract fun attendanceQueueDao(): AttendanceQueueDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {
        @Volatile
        private var INSTANCE: SapaDatabase? = null

        fun getInstance(context: Context): SapaDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    SapaDatabase::class.java,
                    "sapa_local.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}