

CREATE TABLE IF NOT EXISTS usuarios (
                                        identificacion  VARCHAR(30)  PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    correo          VARCHAR(120) NOT NULL,
    clave_hash      VARCHAR(128),
    rol             VARCHAR(20)  NOT NULL DEFAULT 'USUARIO',
    puntos          INTEGER      NOT NULL DEFAULT 0,
    nivel           VARCHAR(20)  NOT NULL DEFAULT 'BRONCE',
    fecha_registro  TIMESTAMP    NOT NULL DEFAULT NOW()
    );

ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS clave_hash VARCHAR(128);
ALTER TABLE usuarios ADD COLUMN IF NOT EXISTS rol VARCHAR(20) NOT NULL DEFAULT 'USUARIO';

CREATE TABLE IF NOT EXISTS billeteras (
                                          codigo                  VARCHAR(30)    PRIMARY KEY,
    identificacion_usuario  VARCHAR(30)    NOT NULL REFERENCES usuarios (identificacion),
    nombre                  VARCHAR(100)   NOT NULL,
    tipo                    VARCHAR(30)    NOT NULL,
    saldo                   NUMERIC(18, 2) NOT NULL DEFAULT 0,
    estado                  VARCHAR(20)    NOT NULL DEFAULT 'ACTIVA',
    umbral_saldo_bajo       NUMERIC(18, 2) NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS transacciones (
                                             id                VARCHAR(20)    PRIMARY KEY,
    fecha             TIMESTAMP      NOT NULL,
    tipo              VARCHAR(30)    NOT NULL,
    valor             NUMERIC(18, 2) NOT NULL,
    codigo_origen     VARCHAR(30),
    codigo_destino    VARCHAR(30),
    estado            VARCHAR(20)    NOT NULL DEFAULT 'COMPLETADA',
    puntos_generados  INTEGER        NOT NULL DEFAULT 0
    );

CREATE TABLE IF NOT EXISTS operaciones_programadas (
                                                       id               VARCHAR(20)    PRIMARY KEY,
    fecha_ejecucion  TIMESTAMP      NOT NULL,
    tipo             VARCHAR(30)    NOT NULL,
    valor            NUMERIC(18, 2) NOT NULL,
    codigo_origen    VARCHAR(30),
    codigo_destino   VARCHAR(30),
    estado           VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE'
    );

CREATE TABLE IF NOT EXISTS beneficios (
                                          id                      SERIAL       PRIMARY KEY,
                                          identificacion_usuario  VARCHAR(30)  NOT NULL,
    descripcion             VARCHAR(200) NOT NULL,
    puntos_costo            INTEGER      NOT NULL,
    fecha_canje             TIMESTAMP    NOT NULL
    );

CREATE TABLE IF NOT EXISTS auditoria (
                                         id                      SERIAL      PRIMARY KEY,
                                         fecha                   TIMESTAMP   NOT NULL,
                                         identificacion_usuario  VARCHAR(30),
    evento                  VARCHAR(60) NOT NULL,
    nivel_riesgo            VARCHAR(20) NOT NULL,
    detalle                 TEXT
    );

CREATE TABLE IF NOT EXISTS notificaciones (
                                              id                      SERIAL      PRIMARY KEY,
                                              identificacion_usuario  VARCHAR(30) NOT NULL,
    tipo                    VARCHAR(40) NOT NULL,
    mensaje                 TEXT        NOT NULL,
    fecha                   TIMESTAMP   NOT NULL,
    despachada              BOOLEAN     NOT NULL DEFAULT FALSE
    );
