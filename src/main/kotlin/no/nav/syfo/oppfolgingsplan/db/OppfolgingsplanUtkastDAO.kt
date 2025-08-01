package no.nav.syfo.oppfolgingsplan.db

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.oppfolgingsplan.dto.CreateUtkastRequest
import java.sql.Date
import java.sql.ResultSet
import java.sql.Types
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class PersistedOppfolgingsplanUtkast (
    val uuid: UUID,
    val sykmeldtFnr: String,
    val narmesteLederId: String,
    val narmesteLederFnr: String,
    val orgnummer: String,
    val content: JsonNode?,
    val sluttdato: LocalDate?,
    val createdAt: Instant,
    val updatedAt: Instant,
)

fun DatabaseInterface.upsertOppfolgingsplanUtkast(
    narmesteLederId: String,
    createUtkastRequest: CreateUtkastRequest,
): UUID {
    val statement =
        """
        INSERT INTO oppfolgingsplan_utkast (
            sykmeldt_fnr,
            narmeste_leder_id,
            narmeste_leder_fnr,
            orgnummer,
            content,
            sluttdato,
            created_at,
            updated_at
        ) VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
        ON CONFLICT (narmeste_leder_id) DO UPDATE SET
            sykmeldt_fnr = EXCLUDED.sykmeldt_fnr,
            narmeste_leder_fnr = EXCLUDED.narmeste_leder_fnr,
            orgnummer = EXCLUDED.orgnummer,
            content = EXCLUDED.content,
            sluttdato = EXCLUDED.sluttdato,
            updated_at = NOW()
        RETURNING uuid
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(statement).use { preparedStatement ->
            preparedStatement.setString(1, createUtkastRequest.sykmeldtFnr)
            preparedStatement.setString(2, narmesteLederId)
            preparedStatement.setString(3, createUtkastRequest.narmesteLederFnr)
            preparedStatement.setString(4, createUtkastRequest.orgnummer)
            preparedStatement.setObject(5, createUtkastRequest.content.toString(), Types.OTHER)
            preparedStatement.setDate(6, Date.valueOf(createUtkastRequest.sluttdato.toString()))
            val resultSet = preparedStatement.executeQuery()
            connection.commit()
            resultSet.next()
            return resultSet.getObject("uuid", UUID::class.java)
        }
    }
}

fun DatabaseInterface.findOppfolgingsplanUtkastBy(
    sykmeldtFnr: String,
    orgnummer: String
): PersistedOppfolgingsplanUtkast? {
    val statement =
        """
        SELECT *
        FROM oppfolgingsplan_utkast
        WHERE sykmeldt_fnr = ?
        AND orgnummer = ?
        """.trimIndent()

    connection.use { connection ->
        connection.prepareStatement(statement).use { preparedStatement ->
            preparedStatement.setString(1, sykmeldtFnr)
            preparedStatement.setString(2, orgnummer)
            val resultSet = preparedStatement.executeQuery()
            return if (resultSet.next()) {
                resultSet.toOppfolgingsplanUtkastDTO()
            } else {
                null
            }
        }
    }
}

fun ResultSet.toOppfolgingsplanUtkastDTO(): PersistedOppfolgingsplanUtkast {
    return PersistedOppfolgingsplanUtkast(
        uuid = getObject("uuid") as UUID,
        sykmeldtFnr = getString("sykmeldt_fnr"),
        narmesteLederId = getString("narmeste_leder_id"),
        narmesteLederFnr = getString("narmeste_leder_fnr"),
        orgnummer = getString("orgnummer"),
        content = ObjectMapper().readValue(getString("content")),
        sluttdato = getDate("sluttdato")?.toLocalDate(),
        createdAt = getTimestamp("created_at").toInstant(),
        updatedAt = getTimestamp("updated_at").toInstant()
    )
}
