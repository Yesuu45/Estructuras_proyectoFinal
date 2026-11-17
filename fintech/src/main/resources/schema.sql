-- Esquema de la plataforma fintech. Es idempotente: Spring lo ejecuta en cada arranque
-- (spring.sql.init.mode=always), por eso todo usa IF NOT EXISTS.

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

-- Por si la tabla ya existía de antes sin la columna de la contraseña
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
    puntos_generados  INTEGER        NOT NULL DEFAULT 0,
    nivel_riesgo      VARCHAR(20)    NOT NULL DEFAULT 'BAJO'
    );

-- Por si la tabla ya existía de antes sin el nivel de riesgo
ALTER TABLE transacciones ADD COLUMN IF NOT EXISTS nivel_riesgo VARCHAR(20) NOT NULL DEFAULT 'BAJO';

CREATE TABLE IF NOT EXISTS operaciones_programadas (
                                                       id               VARCHAR(20)    PRIMARY KEY,
    fecha_ejecucion  TIMESTAMP      NOT NULL,
    tipo             VARCHAR(30)    NOT NULL,
    valor            NUMERIC(18, 2) NOT NULL,
    codigo_origen    VARCHAR(30),
    codigo_destino   VARCHAR(30),
    estado           VARCHAR(20)    NOT NULL DEFAULT 'PENDIENTE',
    periodicidad     VARCHAR(20)    NOT NULL DEFAULT 'UNICA',
    repeticiones_restantes INTEGER  NOT NULL DEFAULT 1,
    avisada          BOOLEAN        NOT NULL DEFAULT FALSE
    );

-- Por si la tabla ya existía de antes sin la periodicidad
ALTER TABLE operaciones_programadas ADD COLUMN IF NOT EXISTS periodicidad VARCHAR(20) NOT NULL DEFAULT 'UNICA';
ALTER TABLE operaciones_programadas ADD COLUMN IF NOT EXISTS repeticiones_restantes INTEGER NOT NULL DEFAULT 1;
ALTER TABLE operaciones_programadas ADD COLUMN IF NOT EXISTS avisada BOOLEAN NOT NULL DEFAULT FALSE;

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
