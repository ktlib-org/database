package org.ktlib.db

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.ktlib.db.Database.Type
import org.ktlib.db.Database.param
import org.ktlib.db.Database.paramN
import org.ktlib.db.Database.result
import org.ktlib.test.EntitySpec
import org.ktlib.toMillisUtc
import org.ktlib.toUUID
import java.sql.SQLException

class DatabaseTests : EntitySpec({
    beforeSpec {
        Migration.run()
    }

    "connected" {
        Database.connected shouldBe true
    }

    "query for int" {
        val count = Database.queryInt("select count(*) from something")

        (count > 0) shouldBe true
    }

    "cannot insert duplicate id" {
        shouldThrow<SQLException> {
            Database.execute("insert into something (id, name) values ('fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b', 'FirstValue')")
        }
    }

    "query" {
        Database.execute("insert into something (id, name) values ('9ac9fbaf-fbab-4a95-9e3f-459d9bc29a28', 'anotherName')")

        val results =
            Database.query(
                "select name from something where name = ?",
                param("anotherName"),
                result("name", Type.String)
            )

        results.size shouldBe 1
        results.first().string("name") shouldBe "anotherName"
    }

    "query by uuid" {
        val results =
            Database.query(
                "select name from something where id = ?",
                param("fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b".toUUID()),
                result("name", Type.String)
            )

        results.size shouldBe 1
        results.first().string("name") shouldBe "FirstValue"
    }

    "query for ids" {
        val ids = Database.queryIds("select id from something order by id").sorted()

        ids.isNotEmpty() shouldBe true
        ids.first() shouldBe "fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b".toUUID()
    }

    "query with null param" {
        val nullName: String? = null

        val ids = Database.queryIds("select id from something where name = ?", paramN(nullName))

        ids.isEmpty() shouldBe true
    }

    "query with null result" {
        val results = Database.query(
            "select name, value from something where id = 'fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b'",
            result("name", Type.String),
            result("value", Type.String)
        )

        results.size shouldBe 1
        results.first().string("name") shouldBe "FirstValue"
        results.first().stringOrNull("value") shouldBe null
    }

    "query for date time" {
        val results =
            Database.query(
                "select created_at from something where id = 'fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b'",
                result("createdAt", Type.LocalDateTime)
            )

        results.size shouldBe 1
        (results.first().localDateTime("createdAt").toMillisUtc() > 0) shouldBe true
    }

    "read only query" {
        val ids = Database.queryIdsReadOnly("select id from something order by id")

        ids.isNotEmpty() shouldBe true
    }
})
