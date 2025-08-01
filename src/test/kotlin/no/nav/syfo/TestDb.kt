package no.nav.syfo

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplan.dto.CreateOppfolgingsplanRequest
import org.flywaydb.core.Flyway
import org.slf4j.LoggerFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.sql.Connection
import java.sql.Date
import java.sql.Types
import java.util.UUID
import kotlin.apply
import kotlin.run
import kotlin.to
import kotlin.use

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:17-alpine")

private val log = LoggerFactory.getLogger(TestDB::class.qualifiedName)

class TestDatabase(
    private val connectionName: String,
    private val dbUsername: String,
    private val dbPassword: String,
) : DatabaseInterface {
    private val dataSource: HikariDataSource =
        HikariDataSource(
            HikariConfig().apply {
                jdbcUrl = connectionName
                username = dbUsername
                password = dbPassword
                maximumPoolSize = 1
                minimumIdle = 1
                isAutoCommit = false
                connectionTimeout = 10_000
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
                validate()
            },
        )
    override val connection: Connection
        get() = dataSource.connection

    init {
        runFlywayMigrations()
    }

    private fun runFlywayMigrations() =
        Flyway.configure().run {
            locations("db")
            configuration(mapOf("flyway.postgresql.transactional.lock" to "false"))
            dataSource(connectionName, dbUsername, dbPassword)
            load().migrate()
        }
}

class TestDB private constructor() {
    companion object {
        val database: DatabaseInterface

        private val psqlContainer: PsqlContainer

        init {
            try {
                psqlContainer =
                    PsqlContainer()
                        .withExposedPorts(5432)
                        .withUsername("username")
                        .withPassword("password")
                        .withDatabaseName("database")

                psqlContainer.waitingFor(HostPortWaitStrategy())
                psqlContainer.start()
                val username = "username"
                val password = "password"
                val connectionName = psqlContainer.jdbcUrl

                database = TestDatabase(connectionName, username, password)
            } catch (ex: Exception) {
                log.error("Error", ex)
                throw ex
            }
        }

        fun clearAllData() =
            database.connection.use {
                it
                    .prepareStatement(
                        """
                    DELETE FROM oppfolgingsplan_utkast;
                    DELETE FROM oppfolgingsplan;
                """,
                    ).use { ps -> ps.executeUpdate() }
                it.commit()
            }
    }
}

fun DatabaseInterface.persistOppfolgingsplan(
    narmesteLederId: String,
    createOppfolgingsplanRequest: CreateOppfolgingsplanRequest,
): UUID {
    val insertStatement = """
        INSERT INTO oppfolgingsplan (
            sykmeldt_fnr,
            narmeste_leder_id,
            narmeste_leder_fnr,
            orgnummer,
            content,
            sluttdato,
            skal_deles_med_lege,
            skal_deles_med_veileder,
            created_at
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())
        RETURNING uuid
    """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(insertStatement).use {
            it.setString(1, createOppfolgingsplanRequest.sykmeldtFnr)
            it.setString(2, narmesteLederId)
            it.setString(3, createOppfolgingsplanRequest.narmesteLederFnr)
            it.setString(4, createOppfolgingsplanRequest.orgnummer)
            it.setObject(5, createOppfolgingsplanRequest.content.toString(), Types.OTHER)
            it.setDate(6, Date.valueOf(createOppfolgingsplanRequest.sluttdato.toString()))
            it.setBoolean(7, createOppfolgingsplanRequest.skalDelesMedLege)
            it.setBoolean(8, createOppfolgingsplanRequest.skalDelesMedVeileder)
            val resultSet = it.executeQuery()
            connection.commit()
            resultSet.next()
            return resultSet.getObject("uuid", UUID::class.java)
        }
    }
}
