package no.nav.syfo.application.api

import io.ktor.server.application.*
import io.ktor.server.routing.*
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.metric.registerMetricApi
import registerPodApi

fun Application.apiModule(
    applicationState: ApplicationState,
    database: DatabaseInterface,
) {
    installCallId()
    installContentNegotiation()
    installStatusPages()

    routing {
        registerPodApi(
            applicationState = applicationState,
            database = database
        )
        registerMetricApi()
    }
}
