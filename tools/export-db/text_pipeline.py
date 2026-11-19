"""Normalización y estructuración del texto.

Lo que hace este módulo, en orden de importancia:

1. `split_comment` — los comentarios NO son texto corrido. Cada uno concatena
   N bloques con la forma `Lema citado del versículo. Explicación…`, sin ningún
   separador. Mateo 5:3 encadena "Bienaventurados." + "Pobres." + "De ellos." +
   "El reino de los cielos." en 3.900 caracteres seguidos. Segmentarlos es la
   mayor ganancia de legibilidad de la app.

   El filtro que evita los falsos positivos (del tipo "Gr. makários.") es exigir
   que la forma plegada del lema aparezca en el texto del versículo.

2. `strip_verse_number` — el texto bíblico trae el número del versículo
   embebido ("1 EN EL principio creó Dios…"), a veces sin espacio y a veces
   partido por el OCR ("2 1 Y abrieron otro pozo…" = versículo 21).

3. `clean` — NFC, arreglo de la transliteración griega rota por el OCR y
   colapso de espacios.
"""

from __future__ import annotations

import re
import unicodedata

# El OCR del PDF sustituyó los macrones del griego transliterado.
_OCR_FIXES = str.maketrans({
    "Ç": "ō",   # 976 comentarios: agapáÇ -> agapáō
    "•": "ē",   # 21 comentarios:  agáp•  -> agápē
    "‚": ",",   # 4 comentarios
})

_CONTROL = re.compile(r"[\x00-\x08\x0b\x0c\x0e-\x1f]")
_SPACES = re.compile(r"[ \t ]{2,}")

# La cola de referencias de Elena G. de White. Nunca aparece al inicio.
# El encabezado tiene variantes de OCR: "ELENA G. DE WHITE", "ELENA G. WHITE",
# "ELENA DE G. WHITE" y hasta "ELENA C. DE WHITE" (C por G).
_EGW = re.compile(
    r"\s*COMENTARIOS?\s+DE\s+ELENA\b.{0,24}?\bWHITE\b.*$",
    re.IGNORECASE | re.DOTALL,
)

# Candidato a lema: frase breve al principio del texto o justo después de un
# signo de puntuación fuerte.
#
# Termina en ".", "?" o "!", no solo en punto: buena parte de los lemas son
# preguntas citadas del versículo ("¿Dónde estás tú?", "¿Qué has hecho?") y
# exigir punto dejaba 600 comentarios sin encabezado.
#
# El tramo "(?:\.{3}...)?" permite los puntos suspensivos con que la obra
# abrevia una cita larga ("Del fruto... podemos comer.").
_LEMMA = re.compile(
    r'(?:\A|(?<=[.!?]\s))'
    r'([¿¡"]?[A-Za-zÁÉÍÓÚÜÑáéíóúüñ][^.!?]{0,90}?(?:\.{3}[^.!?]{0,40}?)?)'
    r'[.?!]\s'
)

_CROSSREF = re.compile(r"\bver\s+com\.", re.IGNORECASE)

MAX_LEMMA_WORDS = 10
CROSSREF_MAX_LEN = 60

# Proporción de palabras del candidato que deben aparecer en el versículo
# cuando no coincide literalmente. Medido sobre el corpus: por debajo de 0,75
# empiezan a colarse candidatos que se tragan la frase siguiente porque al
# original le falta el punto tras el lema.
MIN_WORD_OVERLAP = 0.75
MIN_CONTENT_WORD = 4


def clean(text: str | None) -> str:
    """NFC + arreglo de OCR + colapso de espacios."""
    if not text:
        return ""
    # NFC recompone los 64 acentos combinantes sueltos (U+0301) del corpus;
    # sin esto tokenizarían distinto según el orden de los combinantes.
    out = unicodedata.normalize("NFC", text)
    out = out.translate(_OCR_FIXES)
    out = _CONTROL.sub(" ", out)
    return _SPACES.sub(" ", out).strip()


def fold(text: str) -> str:
    """Minúsculas, sin diacríticos, solo alfanumérico. Para comparar lemas."""
    decomposed = unicodedata.normalize("NFD", text.lower())
    stripped = "".join(c for c in decomposed if unicodedata.category(c) != "Mn")
    return " ".join(re.sub(r"[^0-9a-z ]+", " ", stripped).split())


def strip_verse_number(text: str, number: int) -> str:
    """Quita el número de versículo embebido al inicio del texto bíblico.

    Se construye el patrón a partir de los dígitos del número real, con
    espacios opcionales entre ellos, porque el OCR parte algunos ("2 1" = 21).
    Solo si eso no encaja se recurre al patrón genérico.
    """
    if not text:
        return ""
    pattern = r"^\s*" + r"\s*".join(re.escape(d) for d in str(number)) + r"\s*"
    out, hits = re.subn(pattern, "", text, count=1)
    if hits:
        return out.strip()
    return re.sub(r"^\s*\d+\s*", "", text, count=1).strip()


def split_comment(body: str, verse_text: str) -> tuple[str, list[list[int]], str]:
    """Separa el comentario en bloques encabezados por su lema.

    Devuelve (body_con_bloques, spans_de_lema, referencias_egw) donde el body
    lleva los bloques unidos por "\\n\\n" y cada span es [inicio, longitud]
    sobre ese body final.
    """
    egw = ""
    match = _EGW.search(body)
    if match:
        # Solo se extrae si queda comentario de verdad: uno que fuese únicamente
        # la cola de referencias dejaría la pantalla del lector en blanco.
        remainder = body[: match.start()].strip()
        if remainder:
            egw = clean(match.group(0))
            body = remainder

    haystack = f" {fold(verse_text)} "
    verse_words = set(haystack.split())
    cuts: list[tuple[int, int]] = []
    if haystack.strip():
        for m in _LEMMA.finditer(body):
            candidate = m.group(1)
            if _is_lemma(candidate, haystack, verse_words):
                cuts.append((m.start(1), len(candidate)))

    if not cuts:
        return body.strip(), [], egw

    # Un bloque va desde su lema hasta el lema siguiente. Si el primer lema no
    # está en la posición 0, lo anterior es un bloque sin lema.
    blocks: list[tuple[str, int]] = []
    if cuts[0][0] > 0:
        head = body[: cuts[0][0]].strip()
        if head:
            blocks.append((head, 0))
    for i, (start, length) in enumerate(cuts):
        end = cuts[i + 1][0] if i + 1 < len(cuts) else len(body)
        chunk = body[start:end].strip()
        if chunk:
            blocks.append((chunk, length))

    if not blocks:
        return body.strip(), [], egw

    spans: list[list[int]] = []
    offset = 0
    for chunk, lemma_len in blocks:
        if lemma_len:
            spans.append([offset, lemma_len])
        offset += len(chunk) + 2  # el "\n\n" que los une

    return "\n\n".join(chunk for chunk, _ in blocks), spans, egw


def _is_lemma(candidate: str, haystack: str, verse_words: set[str]) -> bool:
    """¿Este candidato es de verdad una cita del versículo?

    Es el filtro que separa un lema ("Bienaventurados.") de la primera frase
    cualquiera de una explicación ("Gr. makários.").

    Dos reglas, y basta con cumplir una:

    (a) El candidato aparece literal en el versículo. Cubre los lemas de una
        sola palabra corta ("Set.", "Noé.", "Bet-el.") que la regla (b)
        descartaría por no tener ninguna palabra de peso.

    (b) Casi todas sus palabras aparecen en el versículo. Cubre lo que la
        comparación literal no puede: contracciones de la cita ("Salió Caín."
        por "Salió, pues, Caín de delante de Jehová"), singulares por plurales
        ("Estrella." por "las estrellas") y erratas del OCR en el texto bíblico
        ("Edificó" por "ediificó"). Se exige además una palabra de contenido
        para que un "Y dijo." no se cuele por estar hecho de palabras comunes.
    """
    folded = fold(candidate)
    if not folded:
        return False

    words = folded.split()
    if len(words) > MAX_LEMMA_WORDS:
        return False

    if f" {folded} " in haystack:
        return True

    matched = 0
    has_content_word = False
    for word in words:
        if _appears_in(word, verse_words):
            matched += 1
            if len(word) >= MIN_CONTENT_WORD:
                has_content_word = True

    return has_content_word and matched / len(words) >= MIN_WORD_OVERLAP


def _appears_in(word: str, verse_words: set[str]) -> bool:
    """Coincidencia por prefijo, para tolerar plurales y formas flexionadas."""
    if word in verse_words:
        return True
    return any(
        (len(word) >= MIN_CONTENT_WORD and other.startswith(word))
        or (len(other) >= MIN_CONTENT_WORD and word.startswith(other))
        for other in verse_words
    )


def is_crossreference(body: str) -> bool:
    """Remisiones puras del tipo "Desde temprano. Ver com. cap. 7: 13."."""
    return len(body) < CROSSREF_MAX_LEN and bool(_CROSSREF.search(body))
