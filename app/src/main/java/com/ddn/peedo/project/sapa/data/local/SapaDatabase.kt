package com.ddn.peedo.project.sapa.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        AttendanceEntity::class,
        SyncMetaEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SapaDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun schoolDao(): SchoolDao
    abstract fun slotDao(): SlotDao
    abstract fun appointedStudentDao(): AppointedStudentDao
    abstract fun settingsDao(): SettingsDao
    abstract fun attendanceQueueDao(): AttendanceQueueDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun syncMetaDao(): SyncMetaDao

    companion object {

        @Volatile
        private var INSTANCE: SapaDatabase? = null

        /**
         * Database migration:
         *
         * Version 1 → Version 2
         *
         * Adds local attendance cache.
         */
        private val MIGRATION_1_2 =
            object : Migration(1, 2) {

                override fun migrate(
                    database: SupportSQLiteDatabase
                ) {

                    database.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS attendance (
                            attID TEXT NOT NULL,
                            slotID TEXT NOT NULL,
                            userID TEXT NOT NULL,
                            status INTEGER NOT NULL,
                            dateCreated TEXT,
                            dateUpdated TEXT,
                            PRIMARY KEY(attID)
                        )
                        """.trimIndent()
                    )
                }
            }

        fun getInstance(
            context: Context
        ): SapaDatabase {

            return INSTANCE
                ?: synchronized(this) {

                    INSTANCE
                        ?: Room.databaseBuilder(
                            context.applicationContext,
                            SapaDatabase::class.java,
                            "sapa_local.db"
                        )
                            .addMigrations(
                                MIGRATION_1_2
                            )
                            .build()
                            .also {
                                INSTANCE = it
                            }
                }
        }
    }
}