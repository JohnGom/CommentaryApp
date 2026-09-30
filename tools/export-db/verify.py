#!/usr/bin/env python3
"""Comprueba las invariantes de la base exportada.

Uso:
    ./.venv/bin/python verify.py ../../data/comentarios_v1.db

Los CHEQUEOS son estructurales: si uno falla, la base no debe empaquetarse.
Los AVISOS son de calidad del texto: se listan pero no bloquean, porque el
corpus trae ruido de OCR que no siempre conviene tocar.
"""

from __future__ import annotations

import sqlite3
import sys

CHECKS: list[tuple[str, str, object]] = [
    ("libros",                        "SELECT count(*) FROM book", 66),
    ("capítulos",                     "SELECT count(*) FROM chapter", 1189),
    ("versículos",                    "SELECT count(*) FROM verse", 31092),
    ("comentarios",                   "SELECT count(*) FROM comment", 24892),
    ("argumentos de capítulo",        "SELECT count(*) FROM chapter WHERE summary <> ''", 966),
    ("versículos con number = 0",     "SELECT count(*) FROM verse WHERE number = 0", 0),
    ("comentarios huérfanos",
     "SELECT count(*) FROM comment c LEFT JOIN verse v ON v.id = c.verse_id WHERE v.id IS NULL", 0),
    ("versículos huérfanos",
     "SELECT count(*) FROM verse v LEFT JOIN chapter c ON c.id = v.chapter_id WHERE c.id IS NULL", 0),
    ("has_comment coincide con comment",
     "SELECT count(*) FROM verse WHERE has_comment = 1", 24892),
    ("has_comment sin fila en comment",
     "SELECT count(*) FROM verse v LEFT JOIN comment c ON c.verse_id = v.id "
     "WHERE v.has_comment = 1 AND c.verse_id IS NULL", 0),
    ("bloque de Elena G. de White dentro del cuerpo",
     "SELECT count(*) FROM comment WHERE body LIKE '%COMENTARIOS DE ELENA%'", 0),
    ("comentarios vacíos",            "SELECT count(*) FROM comment WHERE trim(body) = ''", 0),
    ("id de versículo mal formado",
     "SELECT count(*) FROM verse WHERE id <> book_id*1000000 + chapter*1000 + number", 0),
    ("id de capítulo mal formado",
     "SELECT count(*) FROM chapter WHERE id <> book_id*1000 + number", 0),
    ("cadena prev/next rota",
     "SELECT count(*) FROM verse WHERE (prev_id IS NULL) <> (id = (SELECT min(id) FROM verse))", 0),
    ("last_verse menor que el máximo real",
     "SELECT count(*) FROM chapter c WHERE c.last_verse <> "
     "(SELECT max(v.number) FROM verse v WHERE v.chapter_id = c.id)", 0),
    ("búsqueda sin tildes (oracion -> oración)",
     "SELECT count(*) > 0 FROM comment_fts WHERE comment_fts MATCH 'oracion'", 1),
    ("búsqueda con mayúsculas y tildes (ESPÍRITU)",
     "SELECT count(*) > 0 FROM comment_fts WHERE comment_fts MATCH 'ESPÍRITU'", 1),
    ("FTS de versículos poblado",
     "SELECT count(*) > 0 FROM verse_fts WHERE verse_fts MATCH 'principio'", 1),
    ("lemas detectados (>= 92%)",
     "SELECT CASE WHEN count(*) * 100 / 24892 >= 92 THEN 1 ELSE 0 END "
     "FROM comment WHERE lemma_count > 0", 1),
    ("modo WAL desactivado",          "PRAGMA journal_mode", "delete"),
    ("integridad",                    "PRAGMA integrity_check", "ok"),
]

WARNINGS: list[tuple[str, str]] = [
    ("textos que aún empiezan por dígito",
     "SELECT count(*) FROM verse WHERE text GLOB '[0-9]*'"),
    ("versículos sin texto",
     "SELECT count(*) FROM verse WHERE has_text = 0"),
    ("versículos sin texto pero con comentario",
     "SELECT count(*) FROM verse WHERE has_text = 0 AND has_comment = 1"),
    ("comentarios sin ningún lema",
     "SELECT count(*) FROM comment WHERE lemma_count = 0"),
    ("libros sin metadatos de introducción",
     "SELECT count(*) FROM book WHERE has_intro = 0"),
    ("capítulos sin ningún comentario",
     "SELECT count(*) FROM chapter WHERE comment_count = 0"),
    ("remisiones cortas (Ver com. …)",
     "SELECT count(*) FROM comment WHERE is_crossref = 1"),
    ("comentarios con referencias de Elena G. de White",
     "SELECT count(*) FROM comment WHERE egw_refs <> ''"),
]


def main() -> int:
    if len(sys.argv) != 2:
        print(__doc__)
        return 2

    db = sqlite3.connect(f"file:{sys.argv[1]}?mode=ro", uri=True)
    failures = 0

    print("CHEQUEOS")
    for label, sql, expected in CHECKS:
        actual = db.execute(sql).fetchone()[0]
        ok = str(actual).lower() == str(expected).lower()
        failures += not ok
        mark = "ok  " if ok else "FALLA"
        detail = f"{actual}" if ok else f"{actual}  (se esperaba {expected})"
        print(f"  {mark} {label:48} {detail}")

    print("\nAVISOS")
    for label, sql in WARNINGS:
        print(f"       {label:48} {db.execute(sql).fetchone()[0]}")

    print("\nMETA")
    for key, value in db.execute("SELECT key, value FROM meta ORDER BY key"):
        print(f"       {key:48} {value}")

    db.close()
    print("\n" + ("Todo correcto." if not failures else f"{failures} chequeo(s) fallaron."))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
