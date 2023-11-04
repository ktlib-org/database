package org.ktlib.db.ktorm

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.ktlib.Encryption
import org.ktlib.db.Database
import org.ktlib.db.ktorm.Somethings.copy
import org.ktlib.db.ktorm.Somethings.create
import org.ktlib.db.ktorm.Somethings.delete
import org.ktlib.now
import org.ktlib.test.EntitySpec
import java.util.*

class EntitySpecTest : EntitySpec() {
    private lateinit var outerSomething: Something

    init {
        beforeEach {
            outerSomething = Something {
                name = "MySomething"
            }.create()
        }

        "can create entity and mock store" {
            val same = Somethings.findById(outerSomething.id)!!

            outerSomething.id shouldBe same.id
            outerSomething.name shouldBe same.name
        }

        "can delete entity" {
            val some = Something {
                name = "me"
            }.create()

            Somethings.findById(some.id) shouldNotBe null

            some.delete()

            Somethings.findById(some.id) shouldBe null
        }

        "can coerce data on to an entity" {
            val something = Something {
                name = "MySomething"
            }.create()
            val now = now()

            val coerced = something.set(something::createdAt to now)

            coerced.createdAt shouldBe now
        }

        "can clone" {
            val something = Something {
                name = "MySomething"
            }.create()

            val clone = something.copy()

            clone.id shouldBe something.id
            clone.name shouldBe something.name
        }

        "can call another function" {
            val something = Something {
                name = "MySomething"
            }

            something.aLazyValue shouldBe something.aLazyValue
        }

        "can get hashcode" {
            val something = Something {

                name = "MySomething"
            }

            something.hashCode() shouldBe something.hashCode()
        }

        "can get string" {
            val something = Something {

                name = "MySomething"
            }

            something.toString() shouldBe something.toString()
        }

        "can use equals" {
            val uuid = UUID.randomUUID()
            val something = Something {
                name = "MySomething"
                set(::id to uuid)
            }
            val anotherThing = Something {
                name = "MySomething"
                set(::id to uuid)
            }
            val yetAnotherThing = Something {
                name = "MyOther"
                set(::id to uuid)
            }

            (something == anotherThing) shouldBe true
            (something == yetAnotherThing) shouldBe false
            (something as Any == "blah") shouldBe false
        }

        "uses same connection for entity transactions an Database queries" {
            val something = Something { name = Encryption.generateKey(20) }.create()

            val count = Database.queryInt("select count(*) from something where name = '${something.name}'")
            val updated = Database.execute("update something set name = 'blah' where id = '${something.id}'")

            count shouldBe 1
            updated shouldBe 1
        }

        "can create instance of entity" {
            val a = SomethingElse {
                name = "hello"
            }

            a.id shouldNotBe null
        }
    }
}
