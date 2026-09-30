#!/usr/bin/env python3
"""Exporta `biblicaldb` (Postgres) a la base SQLite que viaja dentro de la app.

Uso:
    ./.venv/bin/python export_db.py --out ../../data/comentarios_v1.db

La conexión a Postgres se abre en modo SOLO LECTURA. El archivo se escribe en
`<out>.tmp` y se publica con un rename atómico, así que un fallo a medias nunca
deja una base corrupta en su sitio.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import sqlite3
import sys
import time
from datetime import datetime, timezone

import psycopg

from book_names import BOOKS
from text_pipeline import clean, fold, is_crossreference, split_comment, strip_verse_number

SCHEMA_VERSION = 1
HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DSN = "postgresql://johng:1234@localhost/biblicaldb"


def verse_id(book: int, chapter: int, verse: int) -> int:
    return book * 1_000_000 + chapter * 1_000 + verse


def generator_fingerprint() -> str:
    """Hash de los scripts, para saber con qué código se generó la base."""
    digest = hashlib.sha256()
    for name in sorted(("export_db.py", "text_pipeline.py", "book_names.py", "schema.sql")):
        with open(os.path.join(HERE, name), "rb") as handle:
            digest.update(handle.read())
    return digest.hexdigest()[:16]


def load_books(pg: psycopg.Connection) -> dict[int, dict]:
    rows = pg.execute(
        "SELECT number, title_description, author, scenery, theme, outline "
        "FROM books ORDER BY number"
    ).fetchall()
    books: dict[int, dict] = {}
    for number, description, author, scenery, theme, outline in rows:
        if number not in BOOKS:
            raise SystemExit(f"El libro {number} no está en book_names.BOOKS")
        title, short, testament = BOOKS[number]
        fields = {
            "description": clean(description),
            "author": clean(author),
            "scenery": clean(scenery),
            "theme": clean(theme),
            "outline": clean(outline),
        }
        books[number] = {
            "id": number,
            "testament": testament,
            "title": title,
            "title_norm": fold(title),
            "short_title": short,
            "has_intro": int(any(fields.values())),
            **fields,
        }
    return books


def build(pg: psycopg.Connection, books: dict[int, dict]) -> dict:
    """Recorre los versículos en orden canónico y arma todas las filas."""
    cursor = pg.cursor(name="verses_stream")
    cursor.itersize = 5_000
    cursor.execute(
        """
        SELECT b.number, v.chapter, v.verse, v.text_data, c.comment_data
        FROM verses v
        JOIN books b         ON b.id = v.book_id
        LEFT JOIN comments c ON c.verse_id = v.id
        ORDER BY b.number, v.chapter, v.verse
        """
    )

    chapters: dict[int, dict] = {}
    verses: list[dict] = []
    comments: list[tuple] = []
    summaries = 0
    lemma_hits = 0
    source_verses = 0
    source_comments = 0

    for book_no, chapter_no, verse_no, text_data, comment_data in cursor:
        source_verses += 1
        if comment_data:
            source_comments += 1

        chapter_id = book_no * 1_000 + chapter_no
        chapter = chapters.get(chapter_id)
        if chapter is None:
            chapter = chapters[chapter_id] = {
                "id": chapter_id,
                "book_id": book_no,
                "number": chapter_no,
                "verse_count": 0,
                "first_verse": None,
                "last_verse": 0,
                "comment_count": 0,
                "summary": "",
            }

        # verse = 0 no es un versículo: es el argumento del capítulo.
        if verse_no == 0:
            chapter["summary"] = clean(text_data)
            if chapter["summary"]:
                summaries += 1
            continue

        text = strip_verse_number(clean(text_data), verse_no)
        vid = verse_id(book_no, chapter_no, verse_no)
        body = clean(comment_data) if comment_data else ""

        chapter["verse_count"] += 1
        chapter["first_verse"] = verse_no if chapter["first_verse"] is None else chapter["first_verse"]
        chapter["last_verse"] = max(chapter["last_verse"], verse_no)

        if body:
            chapter["comment_count"] += 1
            segmented, spans, egw = split_comment(body, text)
            if spans:
                lemma_hits += 1
            comments.append((
                vid,
                segmented,
                json.dumps(spans, separators=(",", ":")),
                len(spans),
                egw,
                len(segmented),
                int(is_crossreference(segmented)),
            ))

        verses.append({
            "id": vid,
            "book_id": book_no,
            "chapter_id": chapter_id,
            "chapter": chapter_no,
            "number": verse_no,
            "ref": f"{books[book_no]['title']} {chapter_no}:{verse_no}",
            "text": text,
            "has_text": int(bool(text)),
            "has_comment": int(bool(body)),
        })

    cursor.close()

    # prev/next global: cruza fronteras de capítulo y de libro, de modo que la
    # lectura continua de Génesis 1:1 a Apocalipsis 22:21 es un salto por rowid.
    for index, verse in enumerate(verses):
        verse["prev_id"] = verses[index - 1]["id"] if index > 0 else None
        verse["next_id"] = verses[index + 1]["id"] if index + 1 < len(verses) else None

    for book in books.values():
        book_chapters = [c for c in chapters.values() if c["book_id"] == book["id"]]
        book["chapter_count"] = len(book_chapters)
        book["verse_count"] = sum(c["verse_count"] for c in book_chapters)
        book["comment_count"] = sum(c["comment_count"] for c in book_chapters)

    for chapter in chapters.values():
        if chapter["first_verse"] is None:
            chapter["first_verse"] = 0

    return {
        "chapters": chapters,
        "verses": verses,
        "comments": comments,
        "summaries": summaries,
        "lemma_hits": lemma_hits,
        "source_verses": source_verses,
        "source_comments": source_comments,
    }


def write_sqlite(path: str, books: dict[int, dict], data: dict) -> None:
    db = sqlite3.connect(path)
    with open(os.path.join(HERE, "schema.sql"), encoding="utf-8") as handle:
        db.executescript(handle.read())
    db.executescript(
        "PRAGMA journal_mode = OFF;"
        "PRAGMA synchronous = OFF;"
        "PRAGMA temp_store = MEMORY;"
        "PRAGMA cache_size = -200000;"
    )

    book_cols = ("id", "testament", "title", "title_norm", "short_title", "chapter_count",
                 "verse_count", "comment_count", "description", "author", "scenery",
                 "theme", "outline", "has_intro")
    db.executemany(
        f"INSERT INTO book ({','.join(book_cols)}) VALUES ({','.join('?' * len(book_cols))})",
        [tuple(b[c] for c in book_cols) for b in books.values()],
    )

    chapter_cols = ("id", "book_id", "number", "verse_count", "first_verse",
                    "last_verse", "comment_count", "summary")
    db.executemany(
        f"INSERT INTO chapter ({','.join(chapter_cols)}) VALUES ({','.join('?' * len(chapter_cols))})",
        [tuple(c[k] for k in chapter_cols) for c in data["chapters"].values()],
    )

    verse_cols = ("id", "book_id", "chapter_id", "chapter", "number", "ref", "text",
                  "has_text", "has_comment", "prev_id", "next_id")
    db.executemany(
        f"INSERT INTO verse ({','.join(verse_cols)}) VALUES ({','.join('?' * len(verse_cols))})",
        [tuple(v[k] for k in verse_cols) for v in data["verses"]],
    )

    db.executemany(
        "INSERT INTO comment (verse_id, body, lemma_spans, lemma_count, egw_refs, "
        "char_len, is_crossref) VALUES (?,?,?,?,?,?,?)",
        data["comments"],
    )

    db.executemany(
        "INSERT INTO meta (key, value) VALUES (?,?)",
        [
            ("schema_version", str(SCHEMA_VERSION)),
            ("content_version", "1"),
            ("generated_at", datetime.now(timezone.utc).isoformat(timespec="seconds")),
            ("generator", generator_fingerprint()),
            ("source_rows_verses", str(data["source_verses"])),
            ("source_rows_comments", str(data["source_comments"])),
            ("sqlite_version", sqlite3.sqlite_version),
        ],
    )
    db.commit()

    db.executescript(
        "INSERT INTO comment_fts(comment_fts) VALUES('rebuild');"
        "INSERT INTO verse_fts(verse_fts) VALUES('rebuild');"
        "INSERT INTO comment_fts(comment_fts) VALUES('optimize');"
        "INSERT INTO verse_fts(verse_fts) VALUES('optimize');"
        "ANALYZE;"
    )
    db.commit()

    # Sin WAL: una base en modo WAL necesita escribir los ficheros -wal/-shm
    # junto al archivo y falla al abrirse desde un almacenamiento de solo lectura.
    db.execute("PRAGMA journal_mode = DELETE")
    db.execute("VACUUM")
    status = db.execute("PRAGMA integrity_check").fetchone()[0]
    if status != "ok":
        raise SystemExit(f"integrity_check falló: {status}")
    db.close()


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pg", default=os.environ.get("PG_DSN", DEFAULT_DSN))
    parser.add_argument("--out", required=True)
    args = parser.parse_args()

    out = os.path.abspath(args.out)
    tmp = out + ".tmp"
    os.makedirs(os.path.dirname(out), exist_ok=True)
    if os.path.exists(tmp):
        os.remove(tmp)

    started = time.time()
    with psycopg.connect(args.pg) as pg:
        pg.execute("SET TRANSACTION READ ONLY")
        books = load_books(pg)
        print(f"  libros            {len(books)}")
        data = build(pg, books)

    print(f"  filas leídas      {data['source_verses']} versículos, {data['source_comments']} comentarios")
    print(f"  argumentos        {data['summaries']} capítulos con sinopsis (verse=0)")
    print(f"  versículos reales {len(data['verses'])}")
    print(f"  capítulos         {len(data['chapters'])}")
    print(f"  comentarios       {len(data['comments'])}")
    hits = data["lemma_hits"]
    total = max(len(data["comments"]), 1)
    print(f"  lemas detectados  {hits} ({hits / total:.1%} de los comentarios)")

    write_sqlite(tmp, books, data)
    os.replace(tmp, out)

    digest = hashlib.sha256()
    with open(out, "rb") as handle:
        for block in iter(lambda: handle.read(1 << 20), b""):
            digest.update(block)
    checksum = digest.hexdigest()
    with open(out + ".sha256", "w", encoding="utf-8") as handle:
        handle.write(f"{checksum}  {os.path.basename(out)}\n")

    size_mb = os.path.getsize(out) / 1024 / 1024
    print(f"\n  {out}")
    print(f"  {size_mb:.1f} MB · sha256 {checksum[:16]}… · {time.time() - started:.1f}s")
    return 0


if __name__ == "__main__":
    sys.exit(main())
