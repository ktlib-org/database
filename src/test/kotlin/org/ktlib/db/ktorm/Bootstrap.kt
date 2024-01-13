package org.ktlib.db.ktorm

import org.ktlib.Bootstrap

object Bootstrap : Bootstrap {
    override fun init() {
        Ktorm.registerEntityTables(SomethingTable, SomethingElseTable)
    }
}