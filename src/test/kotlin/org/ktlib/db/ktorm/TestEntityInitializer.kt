package org.ktlib.db.ktorm

import org.ktlib.Init
import org.ktlib.entities.EntityInitializer

class TestEntityInitializer : EntityInitializer, Init() {
    init {
        Ktorm.registerEntityTables(SomethingTable, SomethingElseTable)
    }
}