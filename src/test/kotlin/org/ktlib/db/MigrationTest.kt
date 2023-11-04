package org.ktlib.db

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.ktlib.db.Database.Type
import org.ktlib.db.Database.param
import org.ktlib.db.Database.result
import java.io.File
import java.util.*

class MigrationTest : StringSpec({
    "create sql migration" {
        val root = File("src/test")
        val sqlMigration = Migration.create(
            rootDir = root,
            name = "setup",
            isKotlin = false,
            repeatable = false,
            baseline = false,
            undo = null
        )

        val kotlinMigration = Migration.create(
            rootDir = root,
            name = "setup",
            isKotlin = true,
            repeatable = false,
            baseline = false,
            undo = null
        )

        sqlMigration.exists() shouldBe true
        sqlMigration.delete()
        kotlinMigration.exists() shouldBe true
        kotlinMigration.delete()
    }

    "run migrations" {
        Database.execute("drop table if exists something_else")
        Database.execute("drop table if exists something")
        Database.execute("drop table if exists flyway_schema_history")

        Migration.run()

        val results =
            Database.query(
                "select name from something where id = ?",
                param(UUID.fromString("fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b")),
                result("name", Type.String)
            )

        results.size shouldBe 1
        results.first().string("name") shouldBe "FirstValue"
    }
})