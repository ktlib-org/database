package org.ktlib.db.ktorm

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.ktlib.db.Migration
import org.ktlib.db.ktorm.SomethingTable.create
import org.ktlib.entities.ValidationException
import org.ktlib.entities.clearLazyValue
import org.ktlib.entities.clearLazyValues
import org.ktlib.entities.populateFrom
import org.ktlib.test.EntitySpec
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

data class Data(val num: Int, val name: String, val date: LocalDate, val dateTime: LocalDateTime, val enum: MyEnum)

class EntityTests : EntitySpec({
    beforeSpec { Migration.run() }
    val firstId = UUID.fromString("fe8e24a6-8c95-4b6d-9c19-2d4d9ca8fa7b")
    val secondId = UUID.fromString("9ac9fbaf-fbab-4a95-9e3f-459d9bc29a28")

    "can load by id" {
        val s = Somethings.findById(firstId)

        s?.name shouldBe "FirstValue"
    }

    "can save entity" {
        val s = Something {
            name = "MyName"
        }.create()

        s.name shouldBe "MyName"
    }

    "lazy loaded value" {
        val s = Something {}

        val value = s.aLazyValue

        value shouldNotBe null
        value shouldBe s.aLazyValue
        s.clearLazyValue(Something::aLazyValue)
        value shouldNotBe s.aLazyValue
    }

    "lazy load object" {
        val s = SomethingElses.findById(secondId)

        s?.something?.name shouldBe "FirstValue"
    }

    "preload loads items" {
        val items = listOf(SomethingElses.findById(secondId)!!)

        items.preloadSomething().preloadSomethings().preloadSomethingNull()

        val lazyLoads = items.first().lazyEntityValues()
        lazyLoads.size shouldBe 3
        lazyLoads[SomethingElse::something.name] shouldNotBe null
        lazyLoads[SomethingElse::somethings.name] shouldNotBe null
        lazyLoads[SomethingElse::somethingNull.name] shouldBe null
    }

    "populate from map" {
        val data = mapOf(
            "num" to 1,
            "name" to "myName",
            "date" to "2001-01-01",
            "dateTime" to "2001-01-01T01:00:00",
            "enum" to "One",
            "long" to 1
        )

        val entity = Something {}
        entity.populateFrom(data, SomethingInfo::class)

        entity.num shouldBe 1
        entity.long shouldBe 1L
        entity.name shouldBe "myName"
        entity.enum shouldBe MyEnum.One
        entity.date shouldBe LocalDate.of(2001, 1, 1)
        entity.dateTime shouldBe LocalDateTime.of(2001, 1, 1, 1, 0)
    }

    "validation throws exception when invalid" {
        val s = Something { name = "" }

        shouldThrow<ValidationException> {
            s.validate()
        }
    }

    "validation does nothing when valid" {
        val s = Something { name = "blah" }

        s.validate()
    }

    "does not load lazy value if not loaded" {
        val items = listOf(SomethingElses.findById(secondId)!!)
        items.preloadSomething()

        items.first().something.name shouldBe "FirstValue"

        items.first().something.name = "AnotherValue"
        items.preloadSomething()

        items.first().something.name shouldBe "AnotherValue"

        items.clearLazyValues().preloadSomething()
        items.first().something.name shouldBe "FirstValue"
    }
})