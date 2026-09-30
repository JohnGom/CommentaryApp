"""Nombres curados de los 66 libros.

La base Postgres guarda los títulos sin tildes y con numerales romanos
("Genesis", "I Samuel"). Aquí están los nombres de presentación con la
ortografía correcta y las abreviaturas de uso común en español.

El orden por número es el canon protestante y coincide exactamente con
`books.number` de Postgres (verificado contra la base).
"""

OLD, NEW = 0, 1

# número -> (título, abreviatura, testamento)
BOOKS: dict[int, tuple[str, str, int]] = {
    1:  ("Génesis",           "Gn",    OLD),
    2:  ("Éxodo",             "Éx",    OLD),
    3:  ("Levítico",          "Lv",    OLD),
    4:  ("Números",           "Nm",    OLD),
    5:  ("Deuteronomio",      "Dt",    OLD),
    6:  ("Josué",             "Jos",   OLD),
    7:  ("Jueces",            "Jue",   OLD),
    8:  ("Rut",               "Rt",    OLD),
    9:  ("1 Samuel",          "1 S",   OLD),
    10: ("2 Samuel",          "2 S",   OLD),
    11: ("1 Reyes",           "1 R",   OLD),
    12: ("2 Reyes",           "2 R",   OLD),
    13: ("1 Crónicas",        "1 Cr",  OLD),
    14: ("2 Crónicas",        "2 Cr",  OLD),
    15: ("Esdras",            "Esd",   OLD),
    16: ("Nehemías",          "Neh",   OLD),
    17: ("Ester",             "Est",   OLD),
    18: ("Job",               "Job",   OLD),
    19: ("Salmos",            "Sal",   OLD),
    20: ("Proverbios",        "Pr",    OLD),
    21: ("Eclesiastés",       "Ec",    OLD),
    22: ("Cantares",          "Cnt",   OLD),
    23: ("Isaías",            "Is",    OLD),
    24: ("Jeremías",          "Jer",   OLD),
    25: ("Lamentaciones",     "Lm",    OLD),
    26: ("Ezequiel",          "Ez",    OLD),
    27: ("Daniel",            "Dn",    OLD),
    28: ("Oseas",             "Os",    OLD),
    29: ("Joel",              "Jl",    OLD),
    30: ("Amós",              "Am",    OLD),
    31: ("Abdías",            "Abd",   OLD),
    32: ("Jonás",             "Jon",   OLD),
    33: ("Miqueas",           "Mi",    OLD),
    34: ("Nahúm",             "Nah",   OLD),
    35: ("Habacuc",           "Hab",   OLD),
    36: ("Sofonías",          "Sof",   OLD),
    37: ("Hageo",             "Hag",   OLD),
    38: ("Zacarías",          "Zac",   OLD),
    39: ("Malaquías",         "Mal",   OLD),
    40: ("Mateo",             "Mt",    NEW),
    41: ("Marcos",            "Mr",    NEW),
    42: ("Lucas",             "Lc",    NEW),
    43: ("Juan",              "Jn",    NEW),
    44: ("Hechos",            "Hch",   NEW),
    45: ("Romanos",           "Ro",    NEW),
    46: ("1 Corintios",       "1 Co",  NEW),
    47: ("2 Corintios",       "2 Co",  NEW),
    48: ("Gálatas",           "Gá",    NEW),
    49: ("Efesios",           "Ef",    NEW),
    50: ("Filipenses",        "Fil",   NEW),
    51: ("Colosenses",        "Col",   NEW),
    52: ("1 Tesalonicenses",  "1 Ts",  NEW),
    53: ("2 Tesalonicenses",  "2 Ts",  NEW),
    54: ("1 Timoteo",         "1 Ti",  NEW),
    55: ("2 Timoteo",         "2 Ti",  NEW),
    56: ("Tito",              "Tit",   NEW),
    57: ("Filemón",           "Flm",   NEW),
    58: ("Hebreos",           "He",    NEW),
    59: ("Santiago",          "Stg",   NEW),
    60: ("1 Pedro",           "1 P",   NEW),
    61: ("2 Pedro",           "2 P",   NEW),
    62: ("1 Juan",            "1 Jn",  NEW),
    63: ("2 Juan",            "2 Jn",  NEW),
    64: ("3 Juan",            "3 Jn",  NEW),
    65: ("Judas",             "Jud",   NEW),
    66: ("Apocalipsis",       "Ap",    NEW),
}

assert len(BOOKS) == 66
assert sorted(BOOKS) == list(range(1, 67))
