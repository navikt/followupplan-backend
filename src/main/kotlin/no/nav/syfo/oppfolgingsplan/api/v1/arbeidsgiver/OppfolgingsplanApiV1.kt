package no.nav.syfo.oppfolgingsplan.api.v1.arbeidsgiver

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import no.nav.syfo.dinesykmeldte.DineSykmeldteService
import no.nav.syfo.oppfolgingsplan.dto.CreateOppfolgingsplanRequest
import no.nav.syfo.oppfolgingsplan.service.OppfolgingsplanService
import no.nav.syfo.texas.client.TexasHttpClient
import java.util.UUID
import no.nav.syfo.oppfolgingsplan.api.v1.extractAndValidateUUIDParameter
import no.nav.syfo.util.logger

fun Route.registerArbeidsgiverOppfolgingsplanApiV1(
    dineSykmeldteService: DineSykmeldteService,
    texasHttpClient: TexasHttpClient,
    oppfolgingsplanService: OppfolgingsplanService
) {
    val logger = logger()

    route("/arbeidsgiver/{narmesteLederId}/oppfolgingsplaner") {
        install(ValidateAccessToSykmeldtPlugin) {
            this.texasHttpClient = texasHttpClient
            this.dineSykmeldteService = dineSykmeldteService
        }

        post {
            val oppfolgingsplan = try { call.receive<CreateOppfolgingsplanRequest>() } catch (e: Exception) {
                call.application.environment.log.error("Failed to parse Oppfolgingsplan from request: ${e.message}", e)
                call.respond(HttpStatusCode.BadRequest, "Invalid Oppfolgingsplan format")
                return@post
            }

            val sykmeldt = call.attributes[CALL_ATTRIBUTE_SYKMELDT]

            if (oppfolgingsplan.sykmeldtFnr != sykmeldt.fnr) {
                call.application.environment.log.warn("Sykmeldt fnr does not match for narmestelederId: ${sykmeldt.narmestelederId}")
                call.respond(HttpStatusCode.Forbidden, "Sykmeldt fnr does not match")
                return@post
            }
            oppfolgingsplanService.persistOppfolgingsplan(sykmeldt.narmestelederId, oppfolgingsplan)
            try {
                oppfolgingsplanService.produceVarsel(oppfolgingsplan)
            } catch (e: Exception) {
                logger.error("Error when producing kafka message", e)
            }
            call.respond(HttpStatusCode.Created)
        }

        /**
         * Gir et subsett av felter for alle oppfolgingsplaner arbeidsgiver har for sykmeldt identifisert via narmesteLederId.
         * Tiltenkt for oversiktsvisning.
         */
        get("/oversikt") {
            val sykmeldt = call.attributes[CALL_ATTRIBUTE_SYKMELDT]
            val oppfolgingsplaner = oppfolgingsplanService.getOppfolginsplanOverviewFor(sykmeldt.fnr, sykmeldt.orgnummer)

            call.respond(HttpStatusCode.OK, oppfolgingsplaner)
        }

        /**
         * Gir en komplett oppfolginsplan.
         */
        get("/{uuid}") {
            val sykmeldt = call.attributes[CALL_ATTRIBUTE_SYKMELDT]

            val uuid = call.parameters.extractAndValidateUUIDParameter()

            val oppfolgingsplan = oppfolgingsplanService.getOppfolgingsplanByUuid(uuid)
                ?: run {
                    call.application.environment.log.warn("Oppfolgingsplan not found for uuid: $uuid")
                    call.respond(HttpStatusCode.NotFound, "Oppfolgingsplan not found")
                    return@get
                }

            if (oppfolgingsplan.sykmeldtFnr != sykmeldt.fnr || oppfolgingsplan.orgnummer != sykmeldt.orgnummer) {
                call.application.environment.log.warn("Sykmeldt fnr or orgnummer does not match for narmestelederId: ${sykmeldt.narmestelederId}")
                call.respond(HttpStatusCode.Forbidden, "Sykmeldt fnr or orgnummer does not match")
                return@get
            }

            call.respond(HttpStatusCode.OK, oppfolgingsplan)
        }
    }
}
