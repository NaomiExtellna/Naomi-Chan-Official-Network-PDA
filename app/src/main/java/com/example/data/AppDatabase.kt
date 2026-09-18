package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ReceiptEntity::class,
        StaffAccountEntity::class,
        StaffShiftEntity::class,
        AuditEventEntity::class,
        TramDepartureCacheEntity::class
    ],
    version = 6,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun staffDao(): StaffDao
    abstract fun auditDao(): AuditDao
    abstract fun tramCacheDao(): TramCacheDao

    companion object {
        const val DATABASE_VERSION = 6

        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS staff_accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        username TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        credentialHash TEXT NOT NULL,
                        salt TEXT NOT NULL,
                        role TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_staff_accounts_username ON staff_accounts(username)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS staff_shifts (
                        id TEXT NOT NULL PRIMARY KEY,
                        staffId TEXT NOT NULL,
                        staffDisplayName TEXT NOT NULL,
                        openedAt INTEGER NOT NULL,
                        closedAt INTEGER,
                        openingNote TEXT NOT NULL,
                        closingNote TEXT NOT NULL
                    )
                    """.trimIndent()
                )

                db.execSQL("ALTER TABLE receipts ADD COLUMN processedBy TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE receipts ADD COLUMN shiftId TEXT")
                db.execSQL("ALTER TABLE receipts ADD COLUMN receiptStatus TEXT NOT NULL DEFAULT 'ACTIVE'")
                db.execSQL("ALTER TABLE receipts ADD COLUMN voidReason TEXT")
                db.execSQL("ALTER TABLE receipts ADD COLUMN voidedAt INTEGER")
                db.execSQL("ALTER TABLE receipts ADD COLUMN voidedBy TEXT")
                db.execSQL("ALTER TABLE receipts ADD COLUMN replacesReceiptId TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN recoveryHash TEXT")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN recoverySalt TEXT")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN failedAttempts INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN lockedUntil INTEGER")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN mustChangeCredential INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN canVoid INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN canExport INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN canEditVenues INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN canChangeGateway INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE staff_accounts ADD COLUMN canViewTotals INTEGER NOT NULL DEFAULT 0")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS audit_events (
                        id TEXT NOT NULL PRIMARY KEY,
                        createdAt INTEGER NOT NULL,
                        actorId TEXT,
                        actorName TEXT NOT NULL,
                        action TEXT NOT NULL,
                        target TEXT NOT NULL,
                        details TEXT NOT NULL,
                        severity TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_createdAt ON audit_events(createdAt)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_audit_events_actorId ON audit_events(actorId)")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipts_createdAt ON receipts(createdAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_receipts_syncStatus_receiptStatus_createdAt " +
                        "ON receipts(syncStatus, receiptStatus, createdAt)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS tram_departure_cache (
                        stopName TEXT NOT NULL PRIMARY KEY,
                        payloadJson TEXT NOT NULL,
                        fetchedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_tram_departure_cache_fetchedAt " +
                        "ON tram_departure_cache(fetchedAt)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "naomi_pos_database"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
