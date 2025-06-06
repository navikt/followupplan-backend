package no.nav.syfo.application

import io.ktor.server.application.*
import no.nav.syfo.application.database.DatabaseEnvironment

const val NAIS_DATABASE_ENV_PREFIX = "OPPFOLGINGSPLAN_DB"

data class Environment(
    val database: DatabaseEnvironment = DatabaseEnvironment(
        host = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_HOST"),
        port = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PORT"),
        name = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_DATABASE"),
        username = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_USERNAME"),
        password = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_PASSWORD"),
        sslcert = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_SSLCERT"),
        sslkey = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_SSLKEY_PK8"),
        sslrootcert = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_SSLROOTCERT"),
        sslmode = getEnvVar("${NAIS_DATABASE_ENV_PREFIX}_SSLMODE"),
    ),
)

fun getEnvVar(varName: String, defaultValue: String? = null) =
    System.getenv(varName) ?: defaultValue ?: throw RuntimeException("Missing required variable \"$varName\"")

val Application.envKind get() = environment.config.property("ktor.environment").getString()

fun Application.isDev(block: () -> Unit) {
    if (envKind == "development") block()
}

fun Application.isProd(block: () -> Unit) {
    if (envKind == "production") block()
}
