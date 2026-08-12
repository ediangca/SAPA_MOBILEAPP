package com.ddn.peedo.project.sapa.data.local.dao

import androidx.room.*
import com.ddn.peedo.project.sapa.data.local.entity.SyncMetaEntity

@Dao
interface SyncMetaDao {
    @Query("SELECT * FROM sync_meta WHERE moduleName = :module LIMIT 1")
    suspend fun get(module: String): SyncMetaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(meta: SyncMetaEntity)
}