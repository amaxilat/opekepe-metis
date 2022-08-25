CREATE SEQUENCE hibernate_sequence
    START WITH 1
    INCREMENT BY 1
    MINVALUE 1;

CREATE TABLE REPORT
(
    ID               BIGINT DEFAULT hibernate_sequence.nextval PRIMARY KEY,
    DATE             TIMESTAMP,
    LOCATION         TEXT,
    OUTPUT_FILE_NAME TEXT,
    FILES_LOCATION   TEXT,
    PATH             TEXT,
    REPORT_LOCATION  TEXT
);
