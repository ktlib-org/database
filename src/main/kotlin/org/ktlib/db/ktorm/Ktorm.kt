package org.ktlib.db.ktorm

import com.github.f4b6a3.uuid.UuidCreator
import org.ktlib.Init
import org.ktlib.Instances
import org.ktlib.TypeFactory
import org.ktlib.config
import org.ktlib.db.Database
import org.ktlib.db.DatabaseTransactionManager
import org.ktlib.db.Mode
import org.ktlib.entities.EntityCreator
import org.ktlib.entities.TransactionManager
import org.ktorm.entity.Entity
import org.ktorm.support.mysql.MySqlDialect
import org.ktorm.support.postgresql.PostgreSqlDialect
import java.sql.Connection
import kotlin.reflect.KClass

typealias KtormDatabase = org.ktorm.database.Database

object Ktorm : Init() {
    private val reads: List<KtormDatabase>
    private val readWrite: KtormDatabase
    private var readIndex = 0

    object KtormTransactionManager : DatabaseTransactionManager {
        override fun <T> runInTransaction(func: () -> T): T {
            return readWrite.useTransaction { func() }
        }

        override fun startTransaction() {
            if (readWrite.transactionManager.currentTransaction == null) {
                readWrite.transactionManager.newTransaction()
            }
        }

        override fun rollback() {
            readWrite.transactionManager.currentTransaction?.rollback()
        }

        override val connection: Connection
            get() {
                startTransaction()
                return readWrite.transactionManager.currentTransaction!!.connection
            }

        override fun close() {
            readWrite.transactionManager.currentTransaction?.close()
        }
    }

    class KtormTypeFactory(private val type: KClass<*>) : TypeFactory {
        override fun create(): Any {
            val entity = Entity.create(type)
            entity["id"] = UuidCreator.getTimeOrderedEpoch()
            return entity
        }
    }

    object KtormEntityCreator : EntityCreator {
        override fun <T : Any> create(type: KClass<T>): T {
            return Instances.instance(type)
        }
    }

    init {
        val dbType = config<String>("db.type")

        val dialect = when (dbType.lowercase()) {
            "mysql" -> MySqlDialect()
            "postgres" -> PostgreSqlDialect()
            else -> throw Exception("Unknown Database type $dbType. Valid values are: MySQL or Postgres")
        }

        readWrite = KtormDatabase.connect(Database.readWrite, dialect)
        reads = Database.reads.map { KtormDatabase.connect(it, dialect) }

        Instances.registerFactory(TransactionManager::class) { KtormTransactionManager }
        Instances.registerFactory(EntityCreator::class) { KtormEntityCreator }
    }

    fun registerEntityTables(vararg stores: EntityTable<*, *>) = registerEntityTables(stores.toList())

    fun registerEntityTables(stores: List<EntityTable<*, *>>) = stores.forEach { table ->
        if (table.entityClass != null) {
            Instances.registerFactory(table.entityType, KtormTypeFactory(table.entityClass!!))
            Instances.registerFactory(table.entityStoreType) { table }
        }
    }

    fun database(mode: Mode) = when (mode) {
        Mode.ReadWrite -> readWrite
        Mode.ReadOnly -> {
            if (reads.size == 1) reads[0]
            else synchronized(this) {
                val v = readIndex++
                if (readIndex == reads.size) readIndex = 0
                reads[v]
            }
        }
    }
}