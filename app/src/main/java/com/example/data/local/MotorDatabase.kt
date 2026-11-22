package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.ElectricMotorEntity
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [ElectricMotorEntity::class, SyncLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MotorDatabase : RoomDatabase() {

    abstract fun motorDao(): MotorDao

    companion object {
        @Volatile
        private var INSTANCE: MotorDatabase? = null

        fun getInstance(context: Context): MotorDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(appContext: Context): MotorDatabase {
            lateinit var db: MotorDatabase
            db = Room.databaseBuilder(
                appContext,
                MotorDatabase::class.java,
                "voltmotor_offline.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(sqliteDb: SupportSQLiteDatabase) {
                        super.onCreate(sqliteDb)
                        CoroutineScope(Dispatchers.IO).launch {
                            val now = System.currentTimeMillis()
                            db.motorDao().insertAll(SeedCatalogData.getInitialMotors(now))
                            db.motorDao().insertSyncLog(
                                SyncLogEntity(
                                    timestamp = now,
                                    status = "OFFLINE_SEED",
                                    modelsUpdated = SeedCatalogData.getInitialMotors(now).size,
                                    sourceEndpoint = "Bundled cekmotorlistrik.com Snapshot",
                                    message = "Database offline diinisialisasi otomatis untuk akses tanpa internet."
                                )
                            )
                        }
                    }
                })
                .build()
            return db
        }
    }
}
