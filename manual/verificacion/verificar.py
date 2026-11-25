#!/usr/bin/env python3
"""
verificar.py — FASE 4: verifica con SymPy, de forma independiente, cada resultado que aparece en las capturas.

    python manual/verificacion/verificar.py

Lee lo que MathNova mostró de verdad (capturas/limpias/<id>.texto.txt, que el arnés saca de los mismos
objetos que dibujan la Pizarra), calcula el resultado esperado con SymPy y compara (dos expresiones
equivalentes cuentan como iguales). Además revisa cada igualdad "A = B" de los pasos intermedios.
Escribe manual/verificacion/REPORTE.md (solo para el equipo; no va en el manual).
Código de salida 1 si algo no coincide.
"""
import re
import sys
from pathlib import Path

import sympy as sp
from sympy.parsing.latex import parse_latex

for _f in (sys.stdout, sys.stderr):
    _f.reconfigure(encoding="utf-8", errors="replace")

AQUI = Path(__file__).resolve().parent
LIMPIAS = AQUI.parent / "capturas" / "limpias"
x, y = sp.symbols("x y")


def texto(cid):
    return (LIMPIAS / f"{cid}.texto.txt").read_text(encoding="utf-8").splitlines()


def latex_de(cid):
    return [l[8:] for l in texto(cid) if l.startswith("[latex] ")]


def limpiar(t):
    t = re.sub(r"\\phantom\{[^}]*\}", " ", t)
    t = re.sub(r"\\(left|right|,|;|!|quad|qquad)", " ", t)
    t = t.replace("\\ ", " ").replace("−", "-").replace("·", "\\cdot ")
    return t.strip()


def expr(t):
    e = parse_latex(limpiar(t))
    e = e.subs({sp.Symbol("e"): sp.E, sp.Symbol("i"): sp.I})
    return sp.sympify(str(e))


def iguales(a, b):
    try:
        d = sp.simplify(sp.expand(a - b))
        return d == 0
    except Exception:
        return False


def igualdades(lat):
    """Devuelve pares (izquierda, derecha) de cada renglón con '=' (incluye aligned)."""
    pares = []
    cuerpo = re.sub(r"\\(begin|end)\{aligned\}", "", lat)
    for renglon in re.split(r"\\\\", cuerpo):
        if ":" in renglon or "\\text" in renglon or "\\to" in renglon or "\\lim" in renglon or "\\int" in renglon:
            continue
        partes = [p.replace("&", "") for p in renglon.split("=")]
        for a, b in zip(partes, partes[1:]):
            if a.strip() and b.strip():
                pares.append((a, b))
    return pares


filas = []
fallos = 0


NO_USADAS = {"5-6_qrc_02": "captura NO usada en el manual (decisión del equipo, opción 2): paso con igualdad falsa en MathNova"}


def anotar(cid, que, esperado, mostrado, bien, nota=""):
    global fallos
    if cid in NO_USADAS and not bien:
        nota = (nota + "; " if nota else "") + NO_USADAS[cid]
    else:
        fallos += 0 if bien else 1
    filas.append(f"| {cid} | {que} | `{esperado}` | `{mostrado}` | {'✅' if bien else '❌'} | {nota} |")


def resultado_pizarra(cid):
    """La fórmula del bloque Resultado es la que sigue a la línea 'Resultado'."""
    lineas = texto(cid)
    for i, l in enumerate(lineas):
        if l == "Resultado":
            for m in lineas[i + 1:]:
                if m.startswith("[latex] "):
                    return m[8:]
    return None


def comprobar_resultado(cid, que, esperado_expr, original, debe_ser_producto=True, campo=None):
    lat = resultado_pizarra(cid)
    if lat is None:
        anotar(cid, que, sp.latex(esperado_expr), "(no hay Resultado en pantalla)", False)
        return
    mostrado = expr(lat)
    bien = iguales(mostrado, original)
    nota = "igual al ejercicio al expandir"
    # misma factorización: mismos factores irreducibles en el campo
    f_m = sp.factor_list(sp.expand(mostrado), **(campo or {}))
    f_e = sp.factor_list(sp.expand(esperado_expr), **(campo or {}))
    bien = bien and sp.factor(mostrado, **(campo or {})) == sp.factor(esperado_expr, **(campo or {}))
    if debe_ser_producto:
        nf = len(sp.Mul.make_args(mostrado))
        ne = len(sp.Mul.make_args(esperado_expr))
        bien = bien and nf == ne
        nota += f"; {nf} factores (SymPy: {ne})"
    anotar(cid, que, sp.latex(esperado_expr), lat, bien, nota)


def unir_cadenas(lineas):
    """La Comprobación se dibuja como cadena: 'A' / '= B' / '= C' en líneas separadas."""
    r = []
    for l in lineas:
        l = re.sub(r"\\phantom\{[^}]*\}", "", l).replace("\\;", " ").strip()
        if l.startswith("=") and r:
            r[-1] += " " + l
        else:
            r.append(l)
    return r


def comprobar_pasos(cid):
    for lat in unir_cadenas(latex_de(cid)):
        for a, b in igualdades(lat):
            try:
                ea, eb = expr(a), expr(b)
            except Exception as e:
                filas.append(f"| {cid} | paso (sin leer) | | `{a} = {b}` | ⚪ | no se pudo leer el LaTeX: {type(e).__name__} |")
                continue
            anotar(cid, "igualdad de un paso", "", f"{a} = {b}", iguales(ea, eb))


# ---------------------------------------------------------------- ejercicio principal
P = x**3 - 2 * x**2 - x + 2
esperado_P = sp.factor(P)                                   # SymPy: (x - 2)*(x - 1)*(x + 1)
for cid in ["5-1_resolver_02_solucion", "3_zonas", "5-2_pasos_01", "5-4_comprobacion_01", "5-4_comprobacion_02"]:
    comprobar_resultado(cid, "factorizar x³−2x²−x+2 en Q", esperado_P, P)
comprobar_pasos("5-1_resolver_02_solucion")
comprobar_pasos("5-4_comprobacion_02")
comprobar_resultado("6-3_probar_02", "mismo ejercicio con Ruffini", esperado_P, P)
comprobar_pasos("6-3_probar_02")

# ---------------------------------------------------------------- x^4 - 4 en Q y en R
Q4 = x**4 - 4
comprobar_resultado("5-6_qrc_01", "factorizar x⁴−4 en Q", sp.factor(Q4), Q4)
comprobar_resultado("5-6_qrc_02", "factorizar x⁴−4 en R", sp.factor(Q4, extension=sp.sqrt(2)), Q4,
                    campo={"extension": sp.sqrt(2)})
comp = latex_de("6-4_comparar_01")
for nombre, esp_, ext in [("Q", sp.factor(Q4), None), ("R", sp.factor(Q4, extension=sp.sqrt(2)), sp.sqrt(2)),
                          ("C", sp.factor(Q4, extension=[sp.sqrt(2), sp.I]), [sp.sqrt(2), sp.I])]:
    hallado = None
    for lat in comp:
        try:
            e = expr(lat)
        except Exception:
            continue
        kw = {"extension": ext} if ext is not None else {}
        if iguales(e, Q4) and sp.factor(e, **kw) == esp_ and len(sp.Mul.make_args(e)) == len(sp.Mul.make_args(esp_)):
            hallado = lat
            break
    anotar("6-4_comparar_01", f"comparación: fila {nombre}", sp.latex(esp_), hallado or "(no encontrado)", hallado is not None,
           "puede verse en forma resumida")

# ---------------------------------------------------------------- análisis y gráfica
lin = texto("6-2_analisis_01")


def valor_tras(etiqueta):
    for k, l in enumerate(lin):
        if l == etiqueta:
            for m in lin[k + 1:]:
                if m.startswith("[latex] "):
                    return m[8:]
    return "(no encontrado)"


for etiqueta, valor in [("Grado", sp.degree(P, x)), ("Coeficiente principal", sp.LC(P, x)),
                        ("Corte con el eje y", P.subs(x, 0))]:
    mostrado = valor_tras(etiqueta)
    nums = re.findall(r"-?\d+", mostrado)
    anotar("6-2_analisis_01", etiqueta, str(valor), mostrado, bool(nums) and int(nums[-1]) == valor)
raices = sorted(sp.solve(P, x))
mostrado = valor_tras("Raíces de f(x) = 0")
nums = sorted(int(n) for n in re.findall(r"-?\d+", mostrado))
anotar("6-2_analisis_01", "raíces de f(x) = 0", str(raices), mostrado, nums == raices)
cs = sp.solve_univariate_inequality(P > 0, x, relational=False)
mostrado = next((l[8:] for l in lin if "C.S." in l), "(no encontrado)")
anotar("6-2_analisis_01", "conjunto solución de f(x) > 0", sp.latex(cs), mostrado,
       cs == sp.Union(sp.Interval.open(-1, 1), sp.Interval.open(2, sp.oo))
       and r"\left(-1, 1\right) \cup \left(2, +\infty\right)" in mostrado)

comprobar_pasos("5-6_qrc_02")

# ---------------------------------------------------------------- escritura a mano
ed = [l[9:] for l in texto("4-3_mano_01") if l.startswith("[editor] ")]
leido = ed[0] if ed else ""
anotar("4-3_mano_01", "lectura de la muestra A (3x²+2x−1)", "3x^2+2x-1", leido,
       bool(leido) and iguales(expr(leido), 3 * x**2 + 2 * x - 1))

# ---------------------------------------------------------------- historial y práctica
comprobar_resultado("7-2_hoja_02", "hoja del Historial", esperado_P, P)
comprobar_pasos("7-2_hoja_02")
for cid in ["8-2_metodo_03", "8-2_metodo_04", "8-4_terminar_01"]:
    comprobar_pasos(cid)
comprobar_resultado("8-4_terminar_01", "resultado final de la práctica", esperado_P, P)

# ---------------------------------------------------------------- pasada general: toda captura con Resultado
ya = {f.split(" | ")[0].lstrip("| ").strip() for f in filas if "factorizar" in f or "resultado" in f.lower()}
for archivo in sorted(LIMPIAS.glob("*.texto.txt")):
    cid = archivo.name[:-len(".texto.txt")]
    if cid in ya or cid in NO_USADAS or cid.startswith("_"):
        continue
    lineas = texto(cid)
    if "FACTORIZA" not in lineas or resultado_pizarra(cid) is None:
        continue
    k = lineas.index("FACTORIZA")
    enunciado = next(l[8:] for l in lineas[k:] if l.startswith("[latex] "))
    original = expr(enunciado)
    comprobar_resultado(cid, f"factorizar {enunciado} en Q (pasada general)", sp.factor(original), original)

# ---------------------------------------------------------------- reporte
cab = ["# REPORTE de verificación (FASE 4)", "",
       "Solo para el equipo. Cada resultado se calculó aparte con SymPy y se comparó con lo que MathNova dibujó "
       "(el LaTeX de los objetos de la Pizarra, guardado por el arnés en `capturas/limpias/<id>.texto.txt`).", "",
       "| Captura | Qué se verifica | SymPy | MathNova | | Nota |", "|---|---|---|---|---|---|"]
(AQUI / "REPORTE.md").write_text("\n".join(cab + filas + ["", f"**Total:** {len(filas)} comprobaciones, {fallos} ❌."]) + "\n",
                                 encoding="utf-8")
print("\n".join(filas))
print(f"\n{len(filas)} comprobaciones, {fallos} no coinciden")
sys.exit(1 if fallos else 0)
