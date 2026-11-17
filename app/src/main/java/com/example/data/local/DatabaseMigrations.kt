package com.example.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Non-destructive Room schema migrations.
 * Historical financial data must never be dropped on schema updates.
 */
object DatabaseMigrations {
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Reserved non-destructive migration path for future columns/indices.
        }
    }
}
