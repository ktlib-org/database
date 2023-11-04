package org.ktlib.db.ktorm

import org.ktlib.entities.Entity
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar

interface SomethingKtorm : EntityKtorm<SomethingKtorm>, Something

abstract class IntermediateTable<E : EntityKtorm<E>, T : Entity>(
    tableName: String,
    alias: String? = null
) : org.ktlib.db.ktorm.EntityTable<E, T>(tableName, alias)

object SomethingTable : IntermediateTable<SomethingKtorm, Something>("something"), SomethingStore {
    val name = varchar("name").bindTo { it.name }
    val value = varchar("value").bindTo { it.value }
    val enabled = boolean("enabled").bindTo { it.enabled }

    override fun create(name: String) = Something {
        this.name = name
    }.create()
}