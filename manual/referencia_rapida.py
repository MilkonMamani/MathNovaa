#!/usr/bin/env python3
"""
referencia_rapida.py — Hoja de referencia rápida de MathNova (1 página A4).

    python manual/referencia_rapida.py
    -> manual/salida/MathNova_Referencia_rapida.docx y .pdf (exporta con herramientas/exportar.py)

Toma la tabla del "Anexo. Hoja de referencia rápida" de contenido/manual.md, para que las dos digan lo mismo.
"""
import re
import subprocess
import sys
from pathlib import Path

from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

MANUAL = Path(__file__).resolve().parent
SALIDA = MANUAL / "salida"
FUENTE = "Calibri"


def filas_anexo():
    md = (MANUAL / "contenido" / "manual.md").read_text(encoding="utf-8")
    bloque = md.split("# Anexo. Hoja de referencia rápida", 1)[1]
    filas = []
    for l in bloque.splitlines():
        if l.startswith("|"):
            celdas = [c.strip() for c in l.strip().strip("|").split("|")]
            if not all(re.fullmatch(r":?-{3,}:?", c) for c in celdas):
                filas.append(celdas)
    return filas


def texto_rico(p, t, tam):
    for i, parte in enumerate(re.split(r"\*\*", t)):
        r = p.add_run(parte)
        r.bold = i % 2 == 1
        r.font.size = Pt(tam)
        r.font.name = FUENTE


def sombra(celda, color):
    tc = celda._tc.get_or_add_tcPr()
    s = OxmlElement("w:shd")
    s.set(qn("w:val"), "clear"); s.set(qn("w:color"), "auto"); s.set(qn("w:fill"), color)
    tc.append(s)


def main():
    doc = Document()
    sec = doc.sections[0]
    sec.page_height, sec.page_width = Cm(29.7), Cm(21.0)
    sec.left_margin = sec.right_margin = Cm(2)
    sec.top_margin = sec.bottom_margin = Cm(1.8)
    est = doc.styles["Normal"]
    est.font.name = FUENTE
    est.font.size = Pt(10.5)

    t = doc.add_paragraph()
    r = t.add_run("MathNova — Referencia rápida")
    r.bold = True; r.font.size = Pt(18); r.font.name = FUENTE
    s = doc.add_paragraph()
    import json
    version = json.loads((MANUAL / "contenido" / "datos.json").read_text(encoding="utf-8"))["version_software"]
    texto_rico(s, f"Versión {version}. Escribe el ejercicio y haz clic en **Resolver**: MathNova reconoce solo qué hacer.", 10.5)
    s.runs[0].font.color.rgb = RGBColor(0x40, 0x40, 0x40)

    filas = filas_anexo()
    tabla = doc.add_table(rows=len(filas), cols=3)
    tabla.style = "Table Grid"
    tabla.alignment = WD_TABLE_ALIGNMENT.CENTER
    anchos = [Cm(5.0), Cm(3.6), Cm(8.4)]
    for i, fila in enumerate(filas):
        for j, celda in enumerate(fila):
            c = tabla.cell(i, j)
            c.width = anchos[j]
            p = c.paragraphs[0]
            p.paragraph_format.space_before = Pt(3); p.paragraph_format.space_after = Pt(3)
            texto_rico(p, f"**{celda}**" if i == 0 else celda, 10.5)
            if i == 0:
                sombra(c, "E7E6E6")

    doc.add_paragraph()
    for linea in [
        "**Escribir:** potencia con ^ (x^3), decimales con punto (3.5), variables en minúscula, un solo = por ecuación.",
        "**Si Resolver está gris:** lee el mensaje rojo bajo la barra; falta algo o hay un error.",
        "**Ayuda completa:** Manual de usuario de MathNova, capítulo 11 (Solución de problemas).",
    ]:
        p = doc.add_paragraph()
        p.paragraph_format.space_after = Pt(4)
        texto_rico(p, linea, 10.5)

    SALIDA.mkdir(exist_ok=True)
    docx = SALIDA / "MathNova_Referencia_rapida.docx"
    pdf = SALIDA / "MathNova_Referencia_rapida.pdf"
    doc.save(docx)
    r = subprocess.run([sys.executable, str(MANUAL / "herramientas" / "exportar.py"), str(docx), str(pdf)])
    sys.exit(r.returncode)


if __name__ == "__main__":
    main()
