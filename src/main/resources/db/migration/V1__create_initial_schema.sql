CREATE TABLE oppfolgingsplan_utkast
(
    "uuid"               UUID PRIMARY KEY    NOT NULL DEFAULT gen_random_uuid(),
    "sykemeldt_fnr"      VARCHAR(11)         NOT NULL,
    "narmeste_leder_id"  VARCHAR(150) UNIQUE NOT NULL,
    "narmeste_leder_fnr" VARCHAR(11)         NOT NULL,
    "orgnummer"          VARCHAR(9)          NOT NULL,
    "content"            JSONB,
    "sluttdato"          DATE,
    "created_at"         TIMESTAMPTZ         NOT NULL,
    "updated_at"         TIMESTAMPTZ         NOT NULL
);

CREATE TABLE oppfolgingsplan
(
    "uuid"                        UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    "sykemeldt_fnr"               VARCHAR(11)      NOT NULL,
    "narmeste_leder_id"           VARCHAR(150)     NOT NULL,
    "narmeste_leder_fnr"          VARCHAR(11)      NOT NULL,
    "orgnummer"                   VARCHAR(9)       NOT NULL,
    "content"                     JSONB            NOT NULL,
    "sluttdato"                   DATE             NOT NULL,
    "created_at"                  TIMESTAMPTZ      NOT NULL,
    "skal_deles_med_lege"         BOOLEAN          NOT NULL,
    "delt_med_lege_tidspunkt"     TIMESTAMPTZ,
    "skal_deles_med_veileder"     BOOLEAN          NOT NULL,
    "delt_med_veileder_tidspunkt" TIMESTAMPTZ
);

create index utkast_nl_idx on oppfolgingsplan_utkast (narmeste_leder_id);
create index oppfolgingsplan_nl_idx on oppfolgingsplan (narmeste_leder_id);
create index oppfolgingsplan_created_at_idx on oppfolgingsplan (created_at);