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

CREATE TABLE USER
(
    ID       BIGINT DEFAULT hibernate_sequence.nextval PRIMARY KEY,
    USERNAME VARCHAR(255),
    ENABLED  BOOLEAN,
    NAME     VARCHAR(1024),
    PASSWORD VARCHAR(255),
    ROLE     VARCHAR(255)
);

create unique index unique_username_index on USER (USERNAME);

INSERT INTO USER (ENABLED, NAME, PASSWORD, ROLE, USERNAME)
VALUES (true, 'Metis User', '$2a$10$J.f32FDikfPGahRlBKzfx.S16yRyHMiwp2U8l52y/zUsOy5AC3KBm', 'ADMIN', 'metis');

INSERT INTO USER (ENABLED, NAME, PASSWORD, ROLE, USERNAME)
VALUES (true, 'Dimitrios Amaxilatis', '$2a$10$g3ibM.L6Hm4e.dQqR3ofW.IAnGX7bWOPuyc78dJX/0rCl0.YMJGui', 'ADMIN',
        'd.amaxilatis');


