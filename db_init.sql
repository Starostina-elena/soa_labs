BEGIN;

SET LOCAL search_path TO s414257;

CREATE TABLE
    IF NOT EXISTS organizations (
        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        created VARCHAR(64) NOT NULL,
        deleted BOOLEAN NOT NULL DEFAULT FALSE,
        name VARCHAR NOT NULL,
        x BIGINT NOT NULL,
        y BIGINT NOT NULL,
        turnover REAL NOT NULL,
        full_name VARCHAR NOT NULL,
        org_type VARCHAR(32),
        street VARCHAR NOT NULL,
        zip_code VARCHAR NOT NULL
    );

CREATE TABLE
    IF NOT EXISTS persons (
        id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
        name VARCHAR NOT NULL,
        deleted BOOLEAN NOT NULL DEFAULT FALSE
    );

CREATE TABLE
    IF NOT EXISTS person_organizations (
        person_id BIGINT NOT NULL REFERENCES persons (id),
        organization_id BIGINT NOT NULL REFERENCES organizations (id),
        PRIMARY KEY (person_id, organization_id)
    );

COMMIT;