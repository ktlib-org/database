package org.ktlib.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.metrics.IMetricsTracker
import com.zaxxer.hikari.metrics.MetricsTrackerFactory
import io.github.oshai.kotlinlogging.KotlinLogging
import org.ktlib.*
import org.ktlib.entities.TransactionManager
import org.ktlib.trace.Trace
import java.sql.*
import java.sql.Date
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*
import javax.sql.DataSource
import kotlin.reflect.KClass

enum class Mode { ReadWrite, ReadOnly }

interface DatabaseTransactionManager : TransactionManager {
    val connection: Connection
}

/**
 * This object initializes the DB connection and supports some basic query and update operations.
 */
object Database : Init() {
    private val logger = KotlinLogging.logger {}
    val reads: List<DataSource>
    val readWrite: DataSource
    private var readIndex = 0

    fun <T> useConnection(mode: Mode, block: (Connection) -> T) = when(mode) {
        Mode.ReadWrite -> {
            val manager = Instances.instance(TransactionManager::class)
            if(manager is DatabaseTransactionManager) {
                manager.runInTransaction {
                    block(manager.connection)
                }
            } else {
                dataSource(mode).connection.use(block)
            }
        }
        Mode.ReadOnly -> dataSource(mode).connection.use(block)
    }

    interface ParamOrResult
    data class Result(val name: String, val type: Type) : ParamOrResult
    data class Param(val value: Any?, val type: Type) : ParamOrResult

    fun result(name: String, type: Type) = Result(name, type)
    fun <T : Any> param(value: T?, type: Type) = Param(value, type)
    fun param(value: Any) = Param(value, value::class.toType())
    inline fun <reified T : Any> paramN(value: T?) = param(value, T::class.toType())

    private fun dataSource(mode: Mode) = when (mode) {
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

    enum class Type {
        Boolean, LocalDate, LocalTime, LocalDateTime, Float, Double, Short, Int, Long, String, Uuid;
    }

    fun KClass<*>.toType(): Type =
        when (this) {
            Boolean::class -> Type.Boolean
            LocalDate::class -> Type.LocalDate
            LocalTime::class -> Type.LocalTime
            LocalDateTime::class -> Type.LocalDateTime
            Float::class -> Type.Float
            Double::class -> Type.Double
            Short::class -> Type.Short
            Int::class -> Type.Int
            Long::class -> Type.Long
            String::class -> Type.String
            UUID::class -> Type.Uuid
            else -> throw Exception("Cannot determine Sql.Type for type: $this")
        }

    class Row(private val data: Map<String, Any?>) {
        fun boolean(column: String) = data[column] as Boolean
        fun booleanOrNull(column: String) = data[column] as Boolean?
        fun short(column: String) = data[column] as Short
        fun shortOrNull(column: String) = data[column] as Short?
        fun int(column: String) = data[column] as Int
        fun intOrNull(column: String) = data[column] as Int?
        fun long(column: String) = data[column] as Long
        fun longOrNull(column: String) = data[column] as Long?
        fun string(column: String) = data[column] as String
        fun stringOrNull(column: String) = data[column] as String?
        fun float(column: String) = data[column] as Float
        fun floatOrNull(column: String) = data[column] as Float?
        fun double(column: String) = data[column] as Double
        fun doubleOrNull(column: String) = data[column] as Double?
        fun localDateTime(column: String) = data[column] as LocalDateTime
        fun localDateTimeOrNull(column: String) = data[column] as LocalDateTime?
        fun localDate(column: String) = data[column] as LocalDate
        fun localDateOrNull(column: String) = data[column] as LocalDate?
        fun localTime(column: String) = data[column] as LocalTime
        fun localTimeOrNull(column: String) = data[column] as LocalTime?
        fun uuid(column: String) = data[column] as UUID
        fun any(column: String) = data[column]
        inline fun <reified T> get(column: String) = any(column) as T
    }

    init {
        if (System.getenv("DATABASE_URL") != null) {
            val dbUrl = System.getenv("DATABASE_URL")
            val regex = Regex("([^:]+)://([^:]+):([^@]+)@(.+)")
            if (regex.matches(dbUrl)) {
                val (type, username, password, host) = regex.find(dbUrl)!!.destructured
                System.setProperty("db.jdbcUrl", "jdbc:${if (type == "mysql") type else "postgresql"}://$host")
                System.setProperty("db.username", username)
                System.setProperty("db.password", password)
            }
        }

        val driver = config<String>("db.driver")

        val createDataSource = { url: String ->
            HikariConfig().run {
                jdbcUrl = url
                maximumPoolSize = config("db.poolSize", 10)
                driverClassName = driver
                username = config("db.username")
                password = config("db.password")
                metricsTrackerFactory = MetricsTrackerFactory { _, _ ->
                    object : IMetricsTracker {
                        override fun recordConnectionUsageMillis(elapsedBorrowedMillis: Long) {
                            Trace.addDbTime(elapsedBorrowedMillis)
                        }
                    }
                }
                HikariDataSource(this)
            }
        }

        readWrite = createDataSource(config("db.jdbcUrl"))

        val urlRead: List<String>? = configListOrNull("db.jdbcUrlRead")

        reads = if (urlRead?.isNotEmpty() == true && !Environment.isLocal) {
            urlRead.map { createDataSource(it) }
        } else {
            listOf(readWrite)
        }

        Instances.registerFactory(TransactionManager::class) { DatabaseTransactionManagerImpl }
    }

    val connected: Boolean
        get() = try {
            query("select 1", result("u", Type.Boolean)).first().boolean("u")
        } catch (t: Throwable) {
            t.printStackTrace()
            false
        }

    fun execute(sql: String, vararg params: Param) = execute(sql, params.asList())

    fun execute(sql: String, params: List<Param>) = useConnection(Mode.ReadWrite) { conn ->
        debug(sql, params)

        conn.prepareStatement(sql).use { statement ->
            addParams(params, statement)
            statement.executeUpdate()
        }
    }

    private fun debug(sql: String, params: List<Param>) {
        logger.debug { mapOf("sql" to sql, "params" to params).toJson() }
    }

    fun query(sql: String, vararg args: ParamOrResult) = query(sql, args.asList())
    fun query(sql: String, args: List<ParamOrResult>, mode: Mode = Mode.ReadWrite) = executeQuery(sql, args, mode)
    fun queryInt(sql: String, vararg params: Param) = queryInt(sql, params.asList())
    fun queryIds(query: String, vararg params: Param) = queryIds(query, params.asList())

    fun queryInt(sql: String, params: List<Param>, mode: Mode = Mode.ReadWrite) =
        query(sql, params + result("value", Type.Int), mode).map {
            it.int("value")
        }.firstOrNull() ?: 0

    fun queryIds(query: String, params: List<Param>, mode: Mode = Mode.ReadWrite) =
        query(query, params + result("id", Type.Uuid), mode).map { it.uuid("id") }

    fun queryReadOnly(sql: String, vararg args: ParamOrResult) = query(sql, args.asList(), Mode.ReadOnly)
    fun queryIntReadOnly(sql: String, vararg params: Param) = queryInt(sql, params.asList(), Mode.ReadOnly)
    fun queryIdsReadOnly(query: String, vararg params: Param) = queryIds(query, params.asList(), Mode.ReadOnly)

    private fun executeQuery(sql: String, args: List<ParamOrResult>, mode: Mode): List<Row> =
        useConnection(mode) { conn ->
            val params = args.filterIsInstance<Param>()
            debug(sql, params)

            conn.prepareStatement(sql).use { statement ->
                addParams(params, statement)
                statement.executeQuery().let { rs ->
                    val results = mutableListOf<Row>()
                    val resultTypes = args.filterIsInstance<Result>()

                    while (rs.next()) {
                        val rowData = mutableMapOf<String, Any?>()
                        resultTypes.mapIndexed { i, result ->
                            rowData[result.name] = columnValue(rs, result, i + 1)
                        }
                        results.add(Row(rowData))
                    }

                    results
                }
            }
        }

    private fun columnValue(rs: ResultSet, result: Result, index: Int) =
        if (rs.wasNull()) {
            null
        } else {
            when (result.type) {
                Type.Int -> rs.getInt(index)
                Type.Long -> rs.getLong(index)
                Type.Short -> rs.getShort(index)
                Type.String -> rs.getString(index)
                Type.Boolean -> rs.getBoolean(index)
                Type.Double -> rs.getDouble(index)
                Type.Float -> rs.getFloat(index)
                Type.LocalDateTime -> rs.getTimestamp(index).toLocalDateTime()
                Type.LocalDate -> rs.getDate(index).toLocalDate()
                Type.LocalTime -> rs.getTime(index).toLocalTime()
                Type.Uuid -> UUID.fromString(rs.getString(index))
            }
        }


    private fun addParams(params: List<Param>, statement: PreparedStatement) = params.forEachIndexed { i, param ->
        setColumnValue(param.type, statement, i + 1, param.value)
    }

    private fun setColumnValue(type: Type, statement: PreparedStatement, index: Int, value: Any?) {
        if (value == null) {
            statement.setNull(index, Types.NULL)
        } else {
            when (type) {
                Type.Int -> statement.setInt(index, value as Int)
                Type.Long -> statement.setLong(index, value as Long)
                Type.Short -> statement.setShort(index, value as Short)
                Type.String -> statement.setString(index, value as String)
                Type.Boolean -> statement.setBoolean(index, value as Boolean)
                Type.Double -> statement.setDouble(index, value as Double)
                Type.Float -> statement.setFloat(index, value as Float)
                Type.LocalDateTime -> statement.setTimestamp(index, Timestamp.valueOf(value as LocalDateTime))
                Type.LocalDate -> statement.setDate(index, Date.valueOf(value as LocalDate))
                Type.LocalTime -> statement.setTime(index, Time.valueOf(value as LocalTime))
                Type.Uuid -> statement.setObject(index, value)
            }
        }
    }
}


internal object DatabaseTransactionManagerImpl : DatabaseTransactionManager {
    private val transactionHolder = ThreadLocal<DatabaseTransaction>()

    private val transactionStarted: Boolean get() = transactionHolder.get() != null

    override val connection: Connection
        get() {
            startTransaction()
            return transactionHolder.get()!!.connection
        }

    override fun close() {
        transactionHolder.get()?.close()
        transactionHolder.remove()
    }

    override fun rollback() {
        transactionHolder.get()?.rollback()
    }

    private fun commit() {
        transactionHolder.get()?.commit()
    }

    override fun startTransaction() {
        if(transactionHolder.get() == null || transactionHolder.get()?.connection?.isClosed == true) {
            transactionHolder.set(DatabaseTransaction())
        }
    }

    override fun <T> runInTransaction(func: () -> T): T {
        val alreadyStarted = transactionStarted
        try {
            startTransaction()
            val result = func()
            if(!alreadyStarted) {
                commit()
            }
            return result
        } catch (t: Throwable) {
            rollback()
            throw t
        } finally {
            if(!alreadyStarted) {
                close()
            }
        }
    }


    class DatabaseTransaction {
        private var originalIsolation = Connection.TRANSACTION_READ_COMMITTED
        private var originalAutoCommit = false

        private val lazyConnection = lazy(LazyThreadSafetyMode.NONE) {
            val conn = Database.readWrite.connection
            originalIsolation = conn.transactionIsolation
            originalAutoCommit = conn.autoCommit

            if(originalIsolation != Connection.TRANSACTION_READ_COMMITTED) {
                conn.transactionIsolation = Connection.TRANSACTION_READ_COMMITTED
            }

            if(originalAutoCommit) {
                conn.autoCommit = false
            }

            conn
        }

        val connection: Connection by lazyConnection

        fun commit() {
            if(lazyConnection.isInitialized()) {
                connection.commit()
            }
        }

        fun rollback() {
            if(lazyConnection.isInitialized()) {
                connection.rollback()
            }
        }

        fun close() {
            if(lazyConnection.isInitialized() && !connection.isClosed) {
                try {
                    if(connection.transactionIsolation == originalIsolation) {
                        connection.transactionIsolation = originalIsolation
                    }
                    if(connection.autoCommit != originalAutoCommit) {
                        connection.autoCommit = originalAutoCommit
                    }
                } catch(_: Throwable){
                } finally {
                    connection.close()
                }
            }
        }
    }
}
