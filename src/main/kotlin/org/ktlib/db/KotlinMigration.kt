package org.ktlib.db

import org.flywaydb.core.api.FlywayException
import org.flywaydb.core.api.MigrationVersion
import org.flywaydb.core.api.migration.Context
import org.flywaydb.core.api.migration.JavaMigration
import org.flywaydb.core.internal.resolver.MigrationInfoHelper

abstract class KotlinMigration() : JavaMigration {
    private val description: String
    private val version: MigrationVersion

    init {
        val shortName = javaClass.simpleName
        val prefix: String = shortName.substring(0, 1)

        if (prefix !in listOf("M", "R")) {
            throw FlywayException("Invalid migration class name: ${javaClass.name} => ensure it starts with M or R")
        }

        val repeatable = prefix == "R"
        val info = MigrationInfoHelper.extractVersionAndDescription(shortName, prefix, "_", arrayOf(""), repeatable)

        version = info.left
        description = info.right
    }

    override fun canExecuteInTransaction() = true
    override fun getDescription() = description
    override fun getVersion() = version
    override fun migrate(context: Context?) = migrate()
    abstract fun migrate()
}