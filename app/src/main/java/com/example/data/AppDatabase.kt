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
        StaffShiftEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun receiptDao(): ReceiptDao
    abstract fun staffDao(): StaffDao

    companion object {
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
                        isActive INTEGER NOT NULL DEFAULT 1,
                        createdAt INTEGER NOT NULL,
                        lastLoginAt INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_staff_accounts_username ON staff_accounts(username)"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS staff_shifts (
                        id TEXT NOT NULL PRIMARY KEY,
                        staffId TEXT NOT NULL,
                        staffDisplayName TEXT NOT NULL,
                        openedAt INTEGER NOT NULL,
                        closedAt INTEGER,
                        openingNote TEXT NOT NULL DEFAULT '',
                        closingNote TEXT NOT NULL DEFAULT ''
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "naomi_pos_database"
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
