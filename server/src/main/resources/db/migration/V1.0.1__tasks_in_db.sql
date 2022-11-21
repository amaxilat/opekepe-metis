CREATE TABLE TASK
(
    ID              BIGINT NOT NULL GENERATED ALWAYS AS IDENTITY (START WITH 1, INCREMENT BY 1),
    DATE            TIMESTAMP,
    OUT_FILE_NAME  VARCHAR(500),
    FILE_NAME            VARCHAR(500),
    TASKS VARCHAR(500)
);