package com.trancong.dexworkspacemanager.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `workspace_app_assignments` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `workspaceId` INTEGER NOT NULL,
                `zoneId` TEXT NOT NULL,
                `packageName` TEXT NOT NULL,
                `activityName` TEXT NOT NULL,
                `appLabel` TEXT NOT NULL,
                FOREIGN KEY(`workspaceId`) REFERENCES `workspaces`(`id`)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_workspace_app_assignments_workspaceId` " +
                "ON `workspace_app_assignments` (`workspaceId`)"
        )
        db.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS " +
                "`index_workspace_app_assignments_workspaceId_zoneId` " +
                "ON `workspace_app_assignments` (`workspaceId`, `zoneId`)"
        )
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE `workspaces` " +
                "ADD COLUMN `launchDelayMs` INTEGER NOT NULL DEFAULT 400"
        )
        db.execSQL(
            "ALTER TABLE `workspace_app_assignments` " +
                "ADD COLUMN `launchOrder` INTEGER NOT NULL DEFAULT 0"
        )
        db.execSQL(
            "UPDATE `workspace_app_assignments` SET `launchOrder` = 0 WHERE `zoneId` = 'zone_1'"
        )
        db.execSQL(
            "UPDATE `workspace_app_assignments` SET `launchOrder` = 1 WHERE `zoneId` = 'zone_2'"
        )
        db.execSQL(
            "UPDATE `workspace_app_assignments` SET `launchOrder` = 2 WHERE `zoneId` = 'zone_3'"
        )
    }
}
