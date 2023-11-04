package org.ktlib.db.ktorm

import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

interface SomethingElseKtorm : EntityKtorm<SomethingElseKtorm>, SomethingElse

object SomethingElseTable : EntityTable<SomethingElseKtorm, SomethingElse>("something_else"), SomethingElseStore {
    val name = varchar("name").bindTo { it.name }
    val somethingId = uuid("something_id").bindTo { it.somethingId }
}