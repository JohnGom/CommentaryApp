# Exportador Postgres → SQLite

Genera `data/comentarios_v1.db`, la base de solo lectura que viaja dentro de la app.

## Uso

```bash
cd tools/export-db
python3 -m venv .venv && ./.venv/bin/pip install -r requirements.txt   # solo la primera vez

./.venv/bin/python export_db.py --out ../../data/comentarios_v1.db
./.venv/bin/python verify.py    ../../data/comentarios_v1.db
```

El DSN por defecto es `postgresql://johng:1234@localhost/biblicaldb`; se puede cambiar con
`--pg` o con la variable de entorno `PG_DSN`. La conexión se abre en `SET TRANSACTION READ ONLY`,
así que el exportador **nunca** puede escribir en Postgres.

La base se escribe en `<out>.tmp` y se publica con un rename atómico. Junto a ella se deja un
`.sha256` que sí se versiona en git (el `.db`, de 43 MB, no).

## Qué hace, y por qué

Postgres guarda el material tal como salió del PDF. El exportador lo **estructura**; no lo
reescribe.

1. **`verse = 0` no es un versículo.** 966 filas contienen el "argumento" del capítulo (una
   sinopsis con números de versículo en línea) y ninguna tiene comentario. Van a
   `chapter.summary`. Sin esto, el 81% de los capítulos mostraría un versículo fantasma.

2. **El número del versículo viene embebido en el texto** (`"1 EN EL principio creó Dios…"`),
   298 veces sin espacio y a veces partido por el OCR (`"2 1 Y abrieron…"` = versículo 21). Se
   quita con un patrón derivado de los dígitos del número real, no con `^\d+`.

3. **Los comentarios son secuencias de lemas, no texto corrido.** Cada uno encadena bloques con
   la forma `Lema citado del versículo. Explicación…` sin ningún separador: Mateo 5:3 mete
   `Bienaventurados.` + `Pobres.` + `De ellos.` + `El reino de los cielos.` en 3.900 caracteres
   seguidos. Un candidato se acepta como lema si **cita el versículo**, y basta con una de dos
   pruebas: que aparezca literal en él (cubre los lemas de una palabra corta como `Set.` o
   `Bet-el.`), o que al menos el 75% de sus palabras estén allí (cubre contracciones como
   `Salió Caín.` por *"Salió, pues, Caín de delante de Jehová"*, plurales y erratas del OCR).
   Ese filtro es el que descarta falsos positivos como `Gr. makários.`
   El cierre del lema puede ser `.`, `?` o `!`: muchos son preguntas citadas del versículo
   (`¿Dónde estás tú?`), y exigir punto dejaba 600 comentarios sin encabezado.
   Resultado: **98,6%** de los comentarios quedan segmentados. El body se guarda con los bloques
   unidos por `\n\n` y `lemma_spans` lleva los `[inicio, longitud]` para ponerlos en negrita.

4. **Cola de Elena G. de White.** 1.063 comentarios terminan con un bloque de referencias
   (`COMENTARIOS DE ELENA G. DE WHITE 1-36 PP 297-303 …`). Se extrae a `egw_refs`. El
   encabezado tiene variantes de OCR (`ELENA G. WHITE`, `ELENA DE G. WHITE`, `ELENA C. DE WHITE`)
   que el patrón contempla.

5. **IDs aritméticos.** `verse_id = libro*1e6 + capítulo*1e3 + versículo`, siempre
   `INTEGER PRIMARY KEY` (= rowid): lookup O(1) y enlace directo con el `content_rowid` de FTS5.
   `prev_id`/`next_id` se precalculan y **cruzan capítulo y libro**, de modo que leer de corrido
   de Génesis 1:1 a Apocalipsis 22:21 es un salto por rowid.

6. **Ortografía de los libros.** Postgres los guarda sin tildes y con numerales romanos
   (`Genesis`, `I Samuel`). `book_names.py` tiene el mapa curado de nombres de presentación
   (`Génesis`, `1 Samuel`) y abreviaturas.

## Decisiones de tamaño

La base son 43,2 MB (≈24 MB comprimida dentro del AAB). Dos medidas hechas sobre este corpus:

- **Sin `prefix=` en las tablas FTS5.** El índice de prefijos costaba 11,2 MB y la consulta de
  prefijo más lenta sin él tarda 0,6 ms en vez de 0,1 ms. No compensa.
- **FTS5 con `content=` externo**, que indexa sin duplicar los 23 MB de texto.
- **`PRAGMA journal_mode = DELETE`** antes de empaquetar: una base en modo WAL necesita escribir
  los ficheros `-wal`/`-shm` junto al archivo y falla al abrirse en solo lectura.

## Defectos conocidos del material de origen

No se corrigen aquí: alterarlos sería inventar contenido. Se arreglan en Postgres y se vuelve
a exportar.

- **Job 24:1** contiene el texto de Job 24:11 (`"1 1Dentro de sus paredes…"`: el OCR partió el
  "11" y el importador leyó el primer "1" como número de versículo). Job 24:11 ya se corrigió en
  el origen, así que ahora ese texto está duplicado en los dos versículos; falta poner en 24:1
  el suyo (*"¿Por qué no son ocultados los tiempos por el Todopoderoso…"*).
- ~~Salmos 101:1~~ — corregido en el origen.

Otros rasgos del corpus que **sí** son intencionados y se conservan:

- 18 versículos sin texto que **sí tienen comentario** → la app debe mostrar el comentario igual.
- 4 libros sin metadatos de introducción (2 Samuel, 2 Reyes, 2 Crónicas, Nehemías) → `has_intro = 0`.
- 2 capítulos sin ningún comentario (2 Crónicas 35 y 36).
- 422 comentarios que son solo remisiones (`"Ver com. vers. 8."`) → `is_crossref = 1`.
- Versalitas de la RVR60 en mayúsculas (`EN EL principio`, `JEHOVA`): son tipografía, no énfasis.
- Números de página sueltos incrustados por el OCR. **No se borran**: 7.113 comentarios
  contienen números sueltos y la mayoría son legítimos (`40 años`, `1916`, `Sal. 78: 27`).
