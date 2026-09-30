-- Esquema de la base empaquetada en la app. SOLO LECTURA en el cliente.
--
-- IDs derivables aritméticamente, sin UUID:
--   book_id    = number                                (1..66)
--   chapter_id = book*1000    + chapter
--   verse_id   = book*1000000 + chapter*1000 + verse   (máx. 66.150.176 < 2^31)
-- Cada uno es INTEGER PRIMARY KEY, es decir el rowid: lookup O(1), sin índice
-- extra, y enlaza directo con el content_rowid de FTS5.

PRAGMA page_size = 4096;
PRAGMA encoding  = 'UTF-8';

CREATE TABLE meta (
  key   TEXT PRIMARY KEY,
  value TEXT NOT NULL
) WITHOUT ROWID;

CREATE TABLE book (
  id            INTEGER PRIMARY KEY,           -- 1..66
  testament     INTEGER NOT NULL,              -- 0 = Antiguo, 1 = Nuevo
  title         TEXT    NOT NULL,              -- "Génesis", "1 Samuel"
  title_norm    TEXT    NOT NULL,              -- "genesis", "1 samuel" (sin tildes)
  short_title   TEXT    NOT NULL,              -- "Gn", "1 S"
  chapter_count INTEGER NOT NULL,
  verse_count   INTEGER NOT NULL,              -- solo versículos reales
  comment_count INTEGER NOT NULL,
  description   TEXT    NOT NULL DEFAULT '',
  author        TEXT    NOT NULL DEFAULT '',
  scenery       TEXT    NOT NULL DEFAULT '',
  theme         TEXT    NOT NULL DEFAULT '',
  outline       TEXT    NOT NULL DEFAULT '',
  has_intro     INTEGER NOT NULL DEFAULT 0     -- 0 en los libros sin metadatos
);
CREATE INDEX idx_book_testament ON book(testament, id);
CREATE INDEX idx_book_norm      ON book(title_norm);

CREATE TABLE chapter (
  id            INTEGER PRIMARY KEY,           -- book*1000 + number
  book_id       INTEGER NOT NULL REFERENCES book(id),
  number        INTEGER NOT NULL,
  verse_count   INTEGER NOT NULL,              -- versículos que existen de verdad
  first_verse   INTEGER NOT NULL,
  last_verse    INTEGER NOT NULL,              -- != verse_count donde hay huecos
  comment_count INTEGER NOT NULL,
  summary       TEXT    NOT NULL DEFAULT ''    -- el antiguo verse=0 (argumento)
);
CREATE INDEX idx_chapter_book ON chapter(book_id, number);

CREATE TABLE verse (
  id          INTEGER PRIMARY KEY,             -- book*1e6 + chapter*1e3 + number
  book_id     INTEGER NOT NULL REFERENCES book(id),
  chapter_id  INTEGER NOT NULL REFERENCES chapter(id),
  chapter     INTEGER NOT NULL,
  number      INTEGER NOT NULL,                -- SIEMPRE > 0
  ref         TEXT    NOT NULL,                -- "Génesis 1:1"
  text        TEXT    NOT NULL,                -- sin el prefijo numérico
  has_text    INTEGER NOT NULL,
  has_comment INTEGER NOT NULL,
  prev_id     INTEGER,                         -- navegación global: cruza
  next_id     INTEGER                          -- capítulo y libro
);
CREATE INDEX idx_verse_chapter   ON verse(chapter_id, number);
CREATE INDEX idx_verse_commented ON verse(chapter_id, number) WHERE has_comment = 1;

CREATE TABLE comment (
  verse_id    INTEGER PRIMARY KEY REFERENCES verse(id),
  body        TEXT    NOT NULL,                -- bloques separados por "\n\n"
  lemma_spans TEXT    NOT NULL DEFAULT '[]',   -- JSON [[start,len],...] sobre body
  lemma_count INTEGER NOT NULL DEFAULT 0,
  egw_refs    TEXT    NOT NULL DEFAULT '',     -- cola "COMENTARIOS DE ELENA G. DE WHITE"
  char_len    INTEGER NOT NULL,
  is_crossref INTEGER NOT NULL DEFAULT 0       -- remisiones cortas "Ver com. …"
);

-- FTS5 con contenido externo: indexa sin duplicar el texto.
-- remove_diacritics 2 hace la búsqueda insensible a tildes Y a mayúsculas,
-- y snippet()/highlight() siguen devolviendo el texto original acentuado.
--
-- Sin `prefix=`: medido sobre este corpus, el índice de prefijos costaba
-- 11,2 MB y la consulta de prefijo más lenta sin él tarda 0,6 ms en vez de
-- 0,1 ms. FTS5 resuelve `espir*` igual de bien sin el índice.
CREATE VIRTUAL TABLE comment_fts USING fts5(
  body,
  content       = 'comment',
  content_rowid = 'verse_id',
  tokenize      = "unicode61 remove_diacritics 2"
);

CREATE VIRTUAL TABLE verse_fts USING fts5(
  text,
  content       = 'verse',
  content_rowid = 'id',
  tokenize      = "unicode61 remove_diacritics 2"
);

CREATE VIEW v_verse_full AS
SELECT v.id, v.book_id, b.title AS book_title, b.short_title,
       v.chapter, v.number, v.ref, v.text, v.has_text, v.has_comment,
       v.prev_id, v.next_id,
       c.body, c.lemma_spans, c.egw_refs, c.is_crossref
FROM verse v
JOIN book b         ON b.id = v.book_id
LEFT JOIN comment c ON c.verse_id = v.id;
