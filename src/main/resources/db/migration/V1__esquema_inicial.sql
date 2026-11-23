-- MathNova · esquema SQLite (Flyway V1)
-- Fechas: TEXT en ISO-8601 UTC ('2026-11-12T19:21:00Z'). Booleanos: INTEGER 0/1.
-- Valores matemáticos: siempre TEXT exacto (sintaxis MathNova) + LaTeX. Nunca REAL (RN02).
-- Requiere PRAGMA foreign_keys = ON en cada conexión.

-- ============ Catálogos (se llenan en V2 y solo crecen) ============
CREATE TABLE curso (
    id      INTEGER PRIMARY KEY,
    nombre  TEXT    NOT NULL UNIQUE,
    orden   INTEGER NOT NULL
);

CREATE TABLE tema (
    id        INTEGER PRIMARY KEY,
    curso_id  INTEGER NOT NULL REFERENCES curso(id),
    nombre    TEXT    NOT NULL,
    orden     INTEGER NOT NULL,
    UNIQUE (curso_id, nombre)
);

CREATE TABLE operacion (
    codigo  TEXT PRIMARY KEY,                -- FACTORIZAR, LIMITE, DERIVAR...
    nombre  TEXT NOT NULL,
    modulo  TEXT NOT NULL CHECK (modulo IN ('FACTORIZACION','FRACCIONES','CALCULO','VARIAS_VARIABLES'))
);

CREATE TABLE tecnica (
    codigo     TEXT PRIMARY KEY,             -- F01..F30, T01..T05, M01..M08, A1..A6, P01, L01.., D01.., I01.., V01..
    nombre     TEXT NOT NULL,
    categoria  TEXT NOT NULL CHECK (categoria IN ('FACTORIZACION','TRASCENDENTE','VARIAS_VARIABLES','INTERNO',
                                                  'FRACCIONES','LIMITE','DERIVADA','INTEGRAL','CALCULO_3')),
    campo_min  TEXT CHECK (campo_min IN ('Q','R','C'))   -- campo desde el que aplica (F21+ solo R o C)
);

-- ============ Historial ============
CREATE TABLE ejercicio (
    id                     INTEGER PRIMARY KEY,
    expresion_entrada      TEXT    NOT NULL,          -- como la escribió el estudiante
    expresion_normalizada  TEXT    NOT NULL UNIQUE,   -- forma canónica tras el preprocesamiento (2.1)
    expresion_latex        TEXT    NOT NULL,
    variables              TEXT    NOT NULL CHECK (length(variables) > 0),   -- letras separadas por coma: 'a,b' o 'x,θ' (RF57)
    tipo                   TEXT    NOT NULL CHECK (tipo IN ('POLINOMIO','RACIONAL','TRASCENDENTE')),
    es_favorito            INTEGER NOT NULL DEFAULT 0 CHECK (es_favorito IN (0,1)),
    creado_en              TEXT    NOT NULL
);

CREATE TABLE ejercicio_tema (
    ejercicio_id  INTEGER NOT NULL REFERENCES ejercicio(id) ON DELETE CASCADE,
    tema_id       INTEGER NOT NULL REFERENCES tema(id),
    PRIMARY KEY (ejercicio_id, tema_id)
);

CREATE TABLE resolucion (
    id                INTEGER PRIMARY KEY,
    ejercicio_id      INTEGER NOT NULL REFERENCES ejercicio(id) ON DELETE CASCADE,
    operacion_codigo  TEXT    NOT NULL REFERENCES operacion(codigo),
    campo             TEXT    CHECK (campo IN ('Q','R','C')),           -- solo para factorizar
    tecnica_forzada   TEXT    REFERENCES tecnica(codigo),               -- HU08, nulo si no se forzó
    clave_parametros  TEXT    NOT NULL DEFAULT '',                      -- parámetros en forma canónica, para RN25
    resultado_texto   TEXT    NOT NULL,
    resultado_latex   TEXT    NOT NULL,
    es_aproximado     INTEGER NOT NULL DEFAULT 0 CHECK (es_aproximado IN (0,1)),   -- RN30 (≈)
    version_motor     TEXT    NOT NULL,                                 -- RNF19
    creado_en         TEXT    NOT NULL,
    actualizado_en    TEXT    NOT NULL,                                 -- RN25 actualiza esta fecha
    UNIQUE (ejercicio_id, operacion_codigo, clave_parametros)
);
CREATE INDEX ix_resolucion_fecha     ON resolucion(actualizado_en DESC);
CREATE INDEX ix_resolucion_operacion ON resolucion(operacion_codigo, actualizado_en DESC);

CREATE TABLE parametro_resolucion (
    resolucion_id  INTEGER NOT NULL REFERENCES resolucion(id) ON DELETE CASCADE,
    nombre         TEXT    NOT NULL CHECK (nombre IN ('variable','punto','lado','limite_inferior','limite_superior',
                                                       'orden','direccion','restriccion','region','sistema_coordenadas')),
    valor          TEXT    NOT NULL,
    PRIMARY KEY (resolucion_id, nombre)
);

CREATE TABLE paso (
    id               INTEGER PRIMARY KEY,
    resolucion_id    INTEGER NOT NULL REFERENCES resolucion(id) ON DELETE CASCADE,
    paso_padre_id    INTEGER REFERENCES paso(id) ON DELETE CASCADE,     -- pasos anidados (factor dentro de factor)
    orden            INTEGER NOT NULL,
    tecnica_codigo   TEXT    NOT NULL REFERENCES tecnica(codigo),
    justificacion    TEXT    NOT NULL,
    antes_texto      TEXT    NOT NULL,
    antes_latex      TEXT    NOT NULL,
    despues_texto    TEXT,                                               -- nulo en un método descartado
    despues_latex    TEXT,
    trabajo_tipo     TEXT    CHECK (trabajo_tipo IN ('TABLA_RUFFINI','ASPA_SIMPLE','ASPA_DOBLE','ASPA_DOBLE_ESPECIAL',
                                                     'SISTEMA_COEFICIENTES','SUSTITUCION','TRAYECTORIAS','OTRO')),
    trabajo_json     TEXT    CHECK (trabajo_json IS NULL OR json_valid(trabajo_json)),
    es_descartado    INTEGER NOT NULL DEFAULT 0 CHECK (es_descartado IN (0,1)),
    motivo_descarte  TEXT,
    UNIQUE (resolucion_id, orden),
    CHECK ((trabajo_tipo IS NULL) = (trabajo_json IS NULL)),
    CHECK (es_descartado = 1 OR despues_texto IS NOT NULL),
    CHECK (es_descartado = 0 OR motivo_descarte IS NOT NULL)
);
CREATE INDEX ix_paso_padre   ON paso(paso_padre_id);
CREATE INDEX ix_paso_tecnica ON paso(tecnica_codigo);

CREATE TABLE restriccion_dominio (
    id             INTEGER PRIMARY KEY,
    resolucion_id  INTEGER NOT NULL REFERENCES resolucion(id) ON DELETE CASCADE,
    texto          TEXT    NOT NULL,                                     -- 'x ≠ 2', 'x > 0'
    latex          TEXT    NOT NULL,
    origen         TEXT    NOT NULL CHECK (origen IN ('DENOMINADOR','LOGARITMO','RAIZ_PAR','TANGENTE','OTRA'))
);

CREATE TABLE sugerencia (
    id                INTEGER PRIMARY KEY,
    resolucion_id     INTEGER NOT NULL REFERENCES resolucion(id) ON DELETE CASCADE,
    operacion_codigo  TEXT    NOT NULL REFERENCES operacion(codigo),
    regla_codigo      TEXT    NOT NULL,                                  -- RN15..RN19, RN41
    justificacion     TEXT    NOT NULL,
    parametros_json   TEXT    CHECK (parametros_json IS NULL OR json_valid(parametros_json)),
    orden             INTEGER NOT NULL,
    UNIQUE (resolucion_id, orden)
);

-- ============ Estudio ============
CREATE TABLE ejemplo (
    id                  INTEGER PRIMARY KEY,
    tema_id             INTEGER NOT NULL REFERENCES tema(id),
    enunciado           TEXT    NOT NULL,
    expresion           TEXT    NOT NULL,
    operacion_codigo    TEXT    NOT NULL REFERENCES operacion(codigo),
    parametros_json     TEXT    CHECK (parametros_json IS NULL OR json_valid(parametros_json)),
    resultado_esperado  TEXT    NOT NULL,
    dificultad          INTEGER NOT NULL CHECK (dificultad BETWEEN 1 AND 3)
);

CREATE TABLE sesion_practica (
    id             INTEGER PRIMARY KEY,
    resolucion_id  INTEGER NOT NULL REFERENCES resolucion(id) ON DELETE CASCADE,
    iniciada_en    TEXT    NOT NULL,
    terminada_en   TEXT,
    total_pasos    INTEGER NOT NULL CHECK (total_pasos > 0),
    aciertos       INTEGER NOT NULL DEFAULT 0,
    CHECK (aciertos BETWEEN 0 AND total_pasos)
);

CREATE TABLE intento_paso (
    id                   INTEGER PRIMARY KEY,
    sesion_id            INTEGER NOT NULL REFERENCES sesion_practica(id) ON DELETE CASCADE,
    orden_paso           INTEGER NOT NULL,
    tecnica_propuesta    TEXT    REFERENCES tecnica(codigo),
    expresion_propuesta  TEXT    NOT NULL,
    es_correcto          INTEGER NOT NULL CHECK (es_correcto IN (0,1)),
    retroalimentacion    TEXT    NOT NULL,
    intentado_en         TEXT    NOT NULL
);
CREATE INDEX ix_intento_sesion ON intento_paso(sesion_id, orden_paso);

-- ============ Preferencias ============
CREATE TABLE configuracion (
    clave  TEXT PRIMARY KEY,
    valor  TEXT NOT NULL
);
