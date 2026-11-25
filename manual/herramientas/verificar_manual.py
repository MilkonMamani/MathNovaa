#!/usr/bin/env python3
"""
verificar_manual.py — Revisión automática del manual antes de entregarlo.

    python verificar_manual.py manual.docx [manual.pdf]

Comprueba lo que una persona suele olvidar y que se puede medir:
  - Cada figura tiene texto alternativo y un título "Figura X.Y." debajo.
  - Cada figura mide unos 16 cm de ancho y no lleva borde de párrafo (se ve mal en algunos visores).
  - Los índices (general y de figuras) están llenos.
  - Los títulos están numerados (1, 1.1, 1.1.1), salvo los de las páginas preliminares.
  - El texto no lleva colores, emojis ni íconos (rojo y verde solo van en las capturas).
  - No hay frases de más de 25 palabras.
  - Lista los [COMPLETAR: …] pendientes.
  - Con el PDF: número de páginas y pie "Página X de Y" completo.

Termina con código 0 si todo está bien y 2 si hay algo que corregir.
"""
import re
import sys
import zipfile

EMU_16CM = 5760000
COLORES_PERMITIDOS = {"000000", "1A1A1A", "595959", "auto", "333333", "404040"}
ICONOS = re.compile("[ℹ⚠✅❌✔❗\U0001F300-\U0001FAFF]")
PRELIMINARES = ("control de versiones", "índice", "indice")


def _texto(xml):
    return re.sub(r"<[^>]+>", "", xml)


def _estilos(z):
    try:
        st = z.read("word/styles.xml").decode("utf-8")
    except KeyError:
        return {}, set()
    titulos, caption = {}, set()
    for m in re.finditer(r'<w:style [^>]*w:styleId="([^"]+)"[^>]*>(.*?)</w:style>', st, re.S):
        sid, cuerpo = m.group(1), m.group(2)
        nombre = (re.search(r'<w:name w:val="([^"]+)"', cuerpo) or [None, ""])[1].lower()
        n = re.search(r"(heading|título|titulo)\s*([1-3])$", nombre)
        if n:
            titulos[sid] = int(n.group(2))
        if nombre in ("caption", "descripción", "descripcion", "epígrafe"):
            caption.add(sid)
    return titulos, caption


def verificar(docx, pdf=None):
    errores, avisos = [], []
    z = zipfile.ZipFile(docx)
    doc = z.read("word/document.xml").decode("utf-8")
    titulos, caption = _estilos(z)
    parrafos = re.findall(r"<w:p[ >].*?</w:p>", doc, re.S)

    # --- figuras
    n_fig, n_titulos = 0, 0
    for i, p in enumerate(parrafos):
        if "<w:drawing" in p:
            n_fig += 1
            descr = re.search(r'<wp:docPr[^>]*descr="([^"]*)"', p)
            if not descr or len(descr.group(1).strip()) < 10:
                errores.append(f"Figura {n_fig}: falta texto alternativo (descr).")
            cx = re.search(r'<wp:extent cx="(\d+)"', p)
            if cx and abs(int(cx.group(1)) - EMU_16CM) > EMU_16CM * 0.03:
                avisos.append(f"Figura {n_fig}: mide {int(cx.group(1)) / 360000:.1f} cm de ancho (se esperaba 16 cm).")
            if "<w:pBdr>" in p:
                errores.append(f"Figura {n_fig}: tiene borde de párrafo; el borde debe ir dentro de la imagen.")
            sig = _texto(parrafos[i + 1]) if i + 1 < len(parrafos) else ""
            if re.match(r"\s*Figura\s+\d+(\.\d+)*\.", sig):
                n_titulos += 1
            else:
                errores.append(f"Figura {n_fig}: no tiene debajo un título 'Figura X.Y.'.")
    # --- títulos
    h1 = []
    for p in parrafos:
        sid = re.search(r'<w:pStyle w:val="([^"]+)"', p)
        if sid and sid.group(1) in titulos:
            t = _texto(p).strip()
            if titulos[sid.group(1)] == 1:
                h1.append(t)
            if t.lower().startswith(PRELIMINARES) or t.lower().startswith("anexo"):
                continue
            if not re.match(r"\d+(\.\d+)*\.?\s", t):
                avisos.append(f"Título sin numerar: '{t}'.")
    # --- índices llenos
    todo = _texto(doc)
    for t in h1:
        limpio = re.sub(r"^\d+(\.\d+)*\.?\s*", "", t)
        if limpio and todo.count(limpio) < 2 and not t.lower().startswith(PRELIMINARES):
            errores.append(f"El índice general no incluye '{t}' (¿se actualizaron los índices?).")
            break
    if n_fig and len(re.findall(r"Figura\s+\d+(\.\d+)*\.", todo)) < 2 * n_fig:
        errores.append("El índice de figuras parece vacío o incompleto.")
    # --- estilo sobrio
    colores = {c for c in re.findall(r'<w:color w:val="([0-9A-Fa-f]{6}|auto)"', doc)} - COLORES_PERMITIDOS
    if colores:
        avisos.append(f"Texto con colores no permitidos: {sorted(colores)} (solo negro y grises).")
    if ICONOS.search(todo):
        avisos.append("Hay emojis o íconos en el texto (⚠, ℹ, ✅…). Quítalos.")
    for p in parrafos:
        for frase in re.split(r"(?<=[.!?])\s+", _texto(p)):
            if len(frase.split()) > 25 and "...." not in frase:
                avisos.append(f"Frase larga ({len(frase.split())} palabras): '{frase[:70]}…'")
    # --- pendientes
    pendientes = sorted(set(re.findall(r"\[COMPLETAR[^\]]*\]", todo)))
    for xml in [n for n in z.namelist() if re.match(r"word/(header|footer)\d*\.xml", n)]:
        pendientes += re.findall(r"\[COMPLETAR[^\]]*\]", _texto(z.read(xml).decode("utf-8")))
    # --- PDF
    if pdf:
        try:
            from pypdf import PdfReader
            paginas = [(p.extract_text() or "") for p in PdfReader(pdf).pages]
            vacios = [i + 1 for i, t in enumerate(paginas) if re.search(r"P[áa]gina\s+\d+\s+de\s*$", t, re.M)]
            if vacios:
                errores.append(f"PDF: el pie 'Página X de Y' está sin total en las páginas {vacios}.")
            print(f"PDF: {len(paginas)} páginas.")
        except ImportError:
            avisos.append("Instala pypdf (pip install pypdf) para revisar el PDF.")

    print(f"Figuras: {n_fig} · con título: {n_titulos} · capítulos: {len(h1)}")
    for e in errores:
        print("❌", e)
    for a in avisos:
        print("⚠️ ", a)
    if pendientes:
        print("Pendientes por completar:", ", ".join(sorted(set(pendientes))))
    if not errores and not avisos:
        print("✅ Todo correcto.")
    return not errores and not avisos


if __name__ == "__main__":
    if len(sys.argv) not in (2, 3):
        print(__doc__); sys.exit(1)
    sys.exit(0 if verificar(*sys.argv[1:]) else 2)
