package org.ktlib.db.ktorm

import org.ktorm.schema.uuid
import org.ktorm.schema.varchar

interface SomethingElseKtorm : EntityKtorm<SomethingElseKtorm>, SomethingElse

object SomethingElseTable : Table<SomethingElseKtorm, SomethingElse>("something_else"), SomethingElseRepo {
    val name = varchar("name").bindTo { it.name }
    val somethingId = uuid("something_id").bindTo { it.somethingId }
}