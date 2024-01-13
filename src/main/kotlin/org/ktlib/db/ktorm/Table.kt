package org.ktlib.db.ktorm

import org.ktlib.db.Mode
import org.ktlib.entities.Repository
import org.ktlib.entities.ids
import org.ktlib.newUUID7
import org.ktlib.now
import org.ktlib.typeArguments
import org.ktorm.dsl.*
import org.ktorm.entity.*
import org.ktorm.schema.BaseTable
import org.ktorm.schema.ColumnDeclaring
import org.ktorm.schema.datetime
import org.ktorm.schema.uuid
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

abstract class Table<E : EntityKtorm<E>, T : org.ktlib.entities.Entity>(
    tableName: String,
    alias: String? = null
) : org.ktorm.schema.Table<E>(tableName = tableName, alias = alias), Repository<T> {
    val id = uuid("id").primaryKey().bindTo { it.id }
    val createdAt = datetime("created_at").bindTo { it.createdAt }
    val updatedAt = datetime("updated_at").bindTo { it.updatedAt }

    fun generateId(): UUID = newUUID7()

    val entityType: KClass<T>
        @Suppress("UNCHECKED_CAST")
        get() = typeArguments(Table::class)[1] as KClass<T>

    open val entityStoreType: KClass<Repository<T>>
        get() {
            val found = this::class.supertypes.find {
                it.jvmErasure.isSubclassOf(Repository::class) && it.arguments.isEmpty()
            }

            @Suppress("UNCHECKED_CAST")
            when (found) {
                null -> throw IllegalStateException("Could not automatically determine the Repository type for ${this::class}, you'll need to specify it by overriding entityStoreType")
                else -> return found.jvmErasure as KClass<Repository<T>>
            }
        }

    @Suppress("UNCHECKED_CAST")
    override fun T.copy() = (this as E).copy() as T

    override fun T.create(): T {
        @Suppress("UNCHECKED_CAST")
        val entity = this as E
        createdAt = now()
        updatedAt = createdAt
        addEntity(entity)
        return this
    }

    override fun T.delete(): T {
        @Suppress("UNCHECKED_CAST")
        (this as E).delete()
        return this
    }

    override fun T.update(): T {
        updatedAt = now()
        @Suppress("UNCHECKED_CAST")
        (this as E).flushChanges()
        return this
    }

    override fun findById(id: UUID?): T? = findById(id, Mode.ReadWrite)

    @Suppress("UNCHECKED_CAST")
    fun findById(id: UUID?, mode: Mode) =
        if (id == null) null else findOne(mode) { it.id eq id } as T?

    override fun findByIds(ids: Collection<UUID>) = findByIds(ids, Mode.ReadWrite)

    @Suppress("UNCHECKED_CAST")
    fun findByIds(ids: Collection<UUID>, mode: Mode) =
        if (ids.isEmpty()) listOf() else findList(mode) { it.id inList ids } as List<T>

    override fun List<T>.deleteAll(): List<T> {
        if (isNotEmpty()) {
            delete { it.id inList ids() }
        }
        return this
    }

    @Suppress("UNCHECKED_CAST")
    override fun all() = findAll() as List<T>
}

fun <E : Entity<E>> org.ktorm.schema.Table<E>.addEntity(entity: E): Int {
    return Ktorm.database(Mode.ReadWrite).sequenceOf(this).add(entity)
}

fun <E : Entity<E>> org.ktorm.schema.Table<E>.updateEntity(entity: E): Int {
    return Ktorm.database(Mode.ReadWrite).sequenceOf(this).update(entity)
}

inline fun <E : Any, T : BaseTable<E>> T.findOne(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): E? {
    return Ktorm.database(mode).sequenceOf(this).find(predicate)
}

fun <E : Any> BaseTable<E>.findAll(mode: Mode = Mode.ReadWrite): List<E> {
    return Ktorm.database(mode).sequenceOf(this).toList()
}

fun BaseTable<*>.crossJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Ktorm.database(Mode.ReadWrite).from(this).crossJoin(right, on)
}

fun BaseTable<*>.innerJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Ktorm.database(Mode.ReadWrite).from(this).innerJoin(right, on)
}

fun BaseTable<*>.leftJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Ktorm.database(Mode.ReadWrite).from(this).leftJoin(right, on)
}

fun BaseTable<*>.rightJoin(right: BaseTable<*>, on: ColumnDeclaring<Boolean>? = null): QuerySource {
    return Ktorm.database(Mode.ReadWrite).from(this).rightJoin(right, on)
}

fun BaseTable<*>.joinReferencesAndSelect(mode: Mode = Mode.ReadWrite): Query {
    return Ktorm.database(mode).from(this).joinReferencesAndSelect()
}

fun BaseTable<*>.select(columns: Collection<ColumnDeclaring<*>>, mode: Mode = Mode.ReadWrite): Query {
    return Ktorm.database(mode).from(this).select(columns)
}

fun BaseTable<*>.select(vararg columns: ColumnDeclaring<*>) = select(columns.asList())
fun BaseTable<*>.select(mode: Mode = Mode.ReadWrite, vararg columns: ColumnDeclaring<*>) =
    select(columns.asList(), mode)

fun BaseTable<*>.selectDistinct(columns: Collection<ColumnDeclaring<*>>, mode: Mode = Mode.ReadWrite): Query {
    return Ktorm.database(mode).from(this).selectDistinct(columns)
}

fun BaseTable<*>.selectDistinct(vararg columns: ColumnDeclaring<*>) = selectDistinct(columns.asList())
fun BaseTable<*>.selectDistinct(mode: Mode = Mode.ReadWrite, vararg columns: ColumnDeclaring<*>) =
    selectDistinct(columns.asList(), mode)

inline fun <E : Any, T : BaseTable<E>> T.findList(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): List<E> {
    return Ktorm.database(mode).sequenceOf(this).filter(predicate).toList()
}

fun <T : BaseTable<*>> T.update(block: UpdateStatementBuilder.(T) -> Unit): Int {
    return Ktorm.database(Mode.ReadWrite).update(this, block)
}

fun <T : BaseTable<*>> T.batchUpdate(block: BatchUpdateStatementBuilder<T>.() -> Unit): IntArray {
    return Ktorm.database(Mode.ReadWrite).batchUpdate(this, block)
}

fun <T : BaseTable<*>> T.insert(block: AssignmentsBuilder.(T) -> Unit): Int {
    return Ktorm.database(Mode.ReadWrite).insert(this, block)
}

fun <T : BaseTable<*>> T.batchInsert(block: BatchInsertStatementBuilder<T>.() -> Unit): IntArray {
    return Ktorm.database(Mode.ReadWrite).batchInsert(this, block)
}

fun <T : BaseTable<*>> T.insertAndGenerateKey(block: AssignmentsBuilder.(T) -> Unit): Any {
    return Ktorm.database(Mode.ReadWrite).insertAndGenerateKey(this, block)
}

fun <T : BaseTable<*>> T.delete(predicate: (T) -> ColumnDeclaring<Boolean>): Int {
    return Ktorm.database(Mode.ReadWrite).delete(this, predicate)
}

fun BaseTable<*>.deleteAll(): Int {
    return Ktorm.database(Mode.ReadWrite).deleteAll(this)
}

inline fun <E : Any, T : BaseTable<E>> T.all(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return Ktorm.database(mode).sequenceOf(this).all(predicate)
}

fun <E : Any, T : BaseTable<E>> T.any(mode: Mode = Mode.ReadWrite): Boolean {
    return Ktorm.database(mode).sequenceOf(this).any()
}

inline fun <E : Any, T : BaseTable<E>> T.any(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return Ktorm.database(mode).sequenceOf(this).any(predicate)
}

fun <E : Any, T : BaseTable<E>> T.none(mode: Mode = Mode.ReadWrite): Boolean {
    return Ktorm.database(mode).sequenceOf(this).none()
}

inline fun <E : Any, T : BaseTable<E>> T.none(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): Boolean {
    return Ktorm.database(mode).sequenceOf(this).none(predicate)
}

fun <E : Any, T : BaseTable<E>> T.isEmpty(mode: Mode = Mode.ReadWrite): Boolean {
    return Ktorm.database(mode).sequenceOf(this).isEmpty()
}

fun <E : Any, T : BaseTable<E>> T.isNotEmpty(mode: Mode = Mode.ReadWrite): Boolean {
    return Ktorm.database(mode).sequenceOf(this).isNotEmpty()
}

fun <E : Any, T : BaseTable<E>> T.count(mode: Mode = Mode.ReadWrite): Int {
    return Ktorm.database(mode).sequenceOf(this).count()
}

inline fun <E : Any, T : BaseTable<E>> T.count(
    mode: Mode = Mode.ReadWrite,
    predicate: (T) -> ColumnDeclaring<Boolean>
): Int {
    return Ktorm.database(mode).sequenceOf(this).count(predicate)
}

inline fun <E : Any, T : BaseTable<E>, C : Number> T.sumBy(
    mode: Mode = Mode.ReadWrite,
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return Ktorm.database(mode).sequenceOf(this).sumBy(selector)
}

inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.maxBy(
    mode: Mode = Mode.ReadWrite,
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return Ktorm.database(mode).sequenceOf(this).maxBy(selector)
}

inline fun <E : Any, T : BaseTable<E>, C : Comparable<C>> T.minBy(
    mode: Mode = Mode.ReadWrite,
    selector: (T) -> ColumnDeclaring<C>
): C? {
    return Ktorm.database(mode).sequenceOf(this).minBy(selector)
}

inline fun <E : Any, T : BaseTable<E>> T.averageBy(
    mode: Mode = Mode.ReadWrite,
    selector: (T) -> ColumnDeclaring<out Number>
): Double? {
    return Ktorm.database(mode).sequenceOf(this).averageBy(selector)
}