package no.nav.syfo.oppfolgingsplan.api.v1.arbeidsgiver

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.auth.principal
import io.ktor.server.response.respond
import io.ktor.util.AttributeKey
import no.nav.syfo.application.auth.BrukerPrincipal
import no.nav.syfo.dinesykmeldte.DineSykmeldteService
import no.nav.syfo.dinesykmeldte.Sykmeldt
import no.nav.syfo.texas.client.TexasHttpClient

class ValidateAccessToSykmeldtConfiguration(
    var texasHttpClient: TexasHttpClient? = null,
    var dineSykmeldteService: DineSykmeldteService? = null,
)

val CALL_ATTRIBUTE_SYKMELDT = AttributeKey<Sykmeldt>("sykmeldt")

val ValidateAccessToSykmeldtPlugin = createRouteScopedPlugin(
    name = "ValidateAccessToSykmeldtPlugin",
    createConfiguration = ::ValidateAccessToSykmeldtConfiguration,
) {
    pluginConfig.apply {
        onCall { call ->
            val texasHttpClient = texasHttpClient
                ?: throw throw IllegalStateException("TexasHttpClient must be provided in ValidateAccessToSykmeldtPlugin configuration")
            val dineSykmeldteService = dineSykmeldteService
                ?: throw IllegalStateException("DineSykmeldteService must be provided in ValidateAccessToSykmeldtPlugin configuration")

            val narmesteLederId = call.parameters["narmesteLederId"]
                ?: run {
                    call.application.environment.log.warn("No narmesteLederId found in request parameters")
                    call.respond(HttpStatusCode.BadRequest, "Missing narmesteLederId parameter")
                    return@onCall
                }

            val innloggetBruker = call.principal<BrukerPrincipal>()
                ?: run {
                    call.application.environment.log.warn("No user principal found in request")
                    call.respond(HttpStatusCode.Unauthorized)
                    return@onCall
                }

            val texasResponse = texasHttpClient.exhangeTokenForDineSykmeldte(innloggetBruker.token)

            val sykmeldt = dineSykmeldteService.getSykmeldtForNarmesteleder(narmesteLederId, texasResponse.accessToken)
            if (sykmeldt == null) {
                call.application.environment.log.warn("Sykmeldt not found for narmestelederId: $narmesteLederId")
                call.respond(HttpStatusCode.Forbidden)
                return@onCall
            }

//          // Replace lines above with this to run locally
//            val sykmeldt = Sykmeldt(
//                narmestelederId = narmesteLederId,
//                orgnummer = "805824352",
//                fnr =  "17498233120",
//                navn = "never mind",
//                aktivSykmelding = true,
//            )

            call.attributes[CALL_ATTRIBUTE_SYKMELDT] = sykmeldt
        }
    }
}
