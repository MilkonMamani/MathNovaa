#!/usr/bin/env python3
"""
generar_docx.py — Convierte el contenido del manual (texto simple) en un Word con el formato acordado.

    python generar_docx.py datos.json contenido.md salida.docx

  datos.json   → portada y control de versiones (ver referencia/datos_ejemplo.json)
  contenido.md → el cuerpo del manual, con estas convenciones (ver referencia/ejemplo_contenido.md):

    # 1. Introducción                 título de capítulo (empieza en página nueva)
    ## 1.1 Propósito del manual       subtítulo
    ### 4.1.1 …                       tercer nivel
    Texto normal con **negrita**.     párrafo
    Ruta: Menú › Resolver             línea de ruta (gris, más pequeña)
    - viñeta                          lista con viñetas
    1. paso                           lista numerada (cada lista reinicia en 1)
    ![Figura 4.1. Título](capturas/fig.png "Texto alternativo con los pasos")   figura + título
    | Col | Col |  (tabla simple; la 2.ª línea |---| se ignora)

Después: python exportar.py salida.docx salida.pdf   (llena los índices y saca el PDF)

Formato que aplica (no cambiar sin aprobación): A4, una columna, márgenes 2,5 cm izquierda y
2 cm resto, Calibri 11 pt, interlineado 1,15, títulos negros numerados, portada sin encabezado,
páginas preliminares en romanos, cuerpo en arábigos con "Página X de Y", figuras de 16 cm en
línea con el texto y título "Figura X.Y." debajo, índice general e índice de figuras.
"""
import json
import re
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

FUENTE = "Calibri"
NEGRO = RGBColor(0x1A, 0x1A, 0x1A)
GRIS = RGBColor(0x59, 0x59, 0x59)
ESTILO_FIGURA = "Titulo de figura"      # nombre propio: el índice de figuras funciona en Word en cualquier idioma
ANCHO_FIGURA = Cm(16)


# ------------------------------------------------------------------ estilos
def _estilos(doc):
    st = doc.styles
    n = st["Normal"]
    n.font.name = FUENTE; n.font.size = Pt(11); n.font.color.rgb = NEGRO
    n.element.rPr.rFonts.set(qn("w:eastAsia"), FUENTE)
    n.paragraph_format.space_after = Pt(6); n.paragraph_format.line_spacing = 1.15
    for nivel, tam, antes in ((1, 17, 0), (2, 13.5, 14), (3, 12, 12)):
        h = st[f"Heading {nivel}"]
        h.font.name = FUENTE; h.font.size = Pt(tam); h.font.bold = True; h.font.italic = False
        h.font.color.rgb = NEGRO
        rpr = h.element.get_or_add_rPr(); rf = rpr.find(qn("w:rFonts"))
        if rf is None:
            rf = OxmlElement("w:rFonts"); rpr.append(rf)
        for a in ("w:ascii", "w:hAnsi", "w:cs", "w:eastAsia"):
            rf.set(qn(a), FUENTE)
        for a in ("w:asciiTheme", "w:hAnsiTheme", "w:cstheme", "w:eastAsiaTheme"):
            if rf.get(qn(a)) is not None:
                del rf.attrib[qn(a)]
        pf = h.paragraph_format
        pf.space_before = Pt(antes); pf.space_after = Pt(8); pf.keep_with_next = True
    def nuevo(nombre, tam, color=NEGRO, italica=False, negrita=False):
        s = st.add_style(nombre, 1); s.base_style = n
        s.font.name = FUENTE; s.font.size = Pt(tam); s.font.color.rgb = color
        s.font.italic = italica; s.font.bold = negrita
        return s
    c = nuevo(ESTILO_FIGURA, 9.5, GRIS, italica=True)
    c.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.CENTER; c.paragraph_format.space_after = Pt(12)
    r = nuevo("Ruta", 10, GRIS); r.paragraph_format.space_after = Pt(8)
    t = nuevo("Titulo preliminar", 17, negrita=True); t.paragraph_format.space_after = Pt(12)
    for nombre in ("Lista MN", "Lista numerada MN"):
        s = nuevo(nombre, 11)
        s.paragraph_format.left_indent = Cm(0.9); s.paragraph_format.first_line_indent = Cm(-0.5)
        s.paragraph_format.space_after = Pt(3)
        s.paragraph_format.tab_stops.add_tab_stop(Cm(0.9))


# ------------------------------------------------------------------ utilidades XML
def _campo(parrafo, instr, texto="", tam=None, color=None):
    def run():
        r = parrafo.add_run()
        if tam: r.font.size = Pt(tam)
        if color: r.font.color.rgb = color
        return r
    for tipo, contenido in (("begin", None), ("instr", instr), ("separate", None), ("text", texto), ("end", None)):
        r = run()
        if tipo == "instr":
            it = OxmlElement("w:instrText"); it.set(qn("xml:space"), "preserve"); it.text = f" {contenido} "; r._r.append(it)
        elif tipo == "text":
            r.text = contenido
        else:
            fc = OxmlElement("w:fldChar"); fc.set(qn("w:fldCharType"), tipo); r._r.append(fc)


def _numeracion(seccion, formato, inicio=1):
    pg = seccion._sectPr.find(qn("w:pgNumType"))
    if pg is None:
        pg = OxmlElement("w:pgNumType"); seccion._sectPr.append(pg)
    pg.set(qn("w:fmt"), formato); pg.set(qn("w:start"), str(inicio))


def _borde_inferior(parrafo):
    ppr = parrafo._p.get_or_add_pPr(); b = OxmlElement("w:pBdr"); bt = OxmlElement("w:bottom")
    for k, v in (("w:val", "single"), ("w:sz", "4"), ("w:space", "4"), ("w:color", "BFBFBF")):
        bt.set(qn(k), v)
    b.append(bt); ppr.append(b)


def _sombra(celda, color):
    tcpr = celda._tc.get_or_add_tcPr(); sh = OxmlElement("w:shd")
    sh.set(qn("w:val"), "clear"); sh.set(qn("w:color"), "auto"); sh.set(qn("w:fill"), color); tcpr.append(sh)


def _texto_rico(parrafo, texto, **fmt):
    texto = texto.replace("\\*", "\u0000")            # \* = asterisco literal
    for trozo in re.split(r"(\*\*[^*]+\*\*)", texto):
        if not trozo:
            continue
        negrita = trozo.startswith("**") and trozo.endswith("**")
        r = parrafo.add_run((trozo[2:-2] if negrita else trozo).replace("\u0000", "*"))
        r.bold = negrita or fmt.get("bold")
        if fmt.get("size"): r.font.size = Pt(fmt["size"])
        if fmt.get("color"): r.font.color.rgb = fmt["color"]


def _tabla(doc, filas):
    t = doc.add_table(rows=len(filas), cols=len(filas[0]))
    t.style = "Table Grid"; t.alignment = WD_TABLE_ALIGNMENT.CENTER
    for i, fila in enumerate(filas):
        for j, valor in enumerate(fila):
            c = t.cell(i, j); c.text = ""
            _texto_rico(c.paragraphs[0], valor, size=10, bold=(i == 0))
            c.paragraphs[0].paragraph_format.space_after = Pt(0)
            if i == 0:
                _sombra(c, "F2F2F2")
    # bordes grises finos
    tblpr = t._tbl.tblPr; bordes = OxmlElement("w:tblBorders")
    for lado in ("top", "left", "bottom", "right", "insideH", "insideV"):
        e = OxmlElement(f"w:{lado}")
        for k, v in (("w:val", "single"), ("w:sz", "4"), ("w:space", "0"), ("w:color", "A6A6A6")):
            e.set(qn(k), v)
        bordes.append(e)
    tblpr.append(bordes)
    doc.add_paragraph()


# ------------------------------------------------------------------ partes del documento
def _portada(doc, d):
    def linea(texto, tam=11, negrita=False, color=NEGRO, despues=6):
        p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(despues)
        r = p.add_run(texto); r.font.size = Pt(tam); r.bold = negrita; r.font.color.rgb = color
    logo = Path(d.get("_dir", "."), d.get("logo", ""))
    if d.get("logo") and logo.suffix.lower() in (".png", ".jpg", ".jpeg") and logo.is_file():
        # en el encabezado de la portada: no es una figura del manual (sin título ni índice)
        p = doc.sections[0].header.paragraphs[0]; p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.add_run().add_picture(str(logo), width=Cm(6))
        linea("", despues=18)
    else:
        linea(d.get("logo", "[COMPLETAR: logo de la institución]"), 10, color=GRIS, despues=18)
    linea(d["universidad"], 14, True, despues=2)
    linea(f'{d["facultad"]} · {d["escuela"]}', 11, color=GRIS, despues=90)
    linea("MANUAL DE USUARIO", 14, color=GRIS, despues=6)
    linea(d["sistema"], 36, True, despues=2)
    linea(d["subtitulo"], 14, color=GRIS, despues=2)
    linea(f'Versión del software: {d["version_software"]}', 11, color=GRIS, despues=70)
    linea(f'Curso: {d["curso"]}', 11, despues=2)
    linea(f'Docente: {d["docente"]}', 11, despues=30)
    linea("Autores", 11, True, despues=3)
    for a in d["autores"]:
        linea(a, 11, despues=1)
    linea("", despues=50)
    linea(f'Versión del documento: {d["version_documento"]}', 10, color=GRIS, despues=2)
    linea(f'{d["ciudad"]} – {d.get("pais", "Perú")}, {d["fecha"]}', 10, color=GRIS, despues=0)


def _preliminares(doc, d):
    doc.add_paragraph("Control de versiones", style="Titulo preliminar")
    filas = [["Versión", "Fecha", "Autor(es)", "Descripción del cambio"]] + [
        [v["version"], v["fecha"], v["autores"], v["cambio"]] for v in d["versiones"]]
    _tabla(doc, filas)
    doc.add_page_break()
    doc.add_paragraph("Índice general", style="Titulo preliminar")
    _campo(doc.add_paragraph(), 'TOC \\o "1-3" \\h \\z \\u', "Actualiza el índice (exportar.py lo hace solo).")
    doc.add_page_break()
    doc.add_paragraph("Índice de figuras", style="Titulo preliminar")
    _campo(doc.add_paragraph(), f'TOC \\h \\z \\t "{ESTILO_FIGURA}{_separador_lista()}1"',
           "Actualiza el índice (exportar.py lo hace solo).")


def _separador_lista():
    """Word lee el campo TOC \\t con el separador de listas de Windows (',' o ';', p. ej. ';' en es-PE)."""
    try:
        import winreg
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, r"Control Panel\International") as k:
            sep = winreg.QueryValueEx(k, "sList")[0].strip()
            return sep if sep in (",", ";") else ","
    except (ImportError, OSError):
        return ","


def _cabecera_y_pie(doc, seccion, d, total_cuerpo):
    seccion.header.is_linked_to_previous = False
    seccion.footer.is_linked_to_previous = False
    h = seccion.header.paragraphs[0]; h.text = ""
    h.style = doc.styles["Normal"]          # el estilo "Header" trae tabulaciones que desalinean "v1.0"
    h.paragraph_format.space_after = Pt(0)
    h.paragraph_format.tab_stops.add_tab_stop(seccion.page_width - seccion.left_margin - seccion.right_margin, WD_TAB_ALIGNMENT.RIGHT)
    r = h.add_run(f'{d["sistema"]} — Manual de Usuario\tv{d["version_documento"]}'); r.font.size = Pt(9); r.font.color.rgb = GRIS
    _borde_inferior(h)
    f = seccion.footer.paragraphs[0]; f.text = ""; f.style = doc.styles["Normal"]; f.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = f.add_run("Página "); r.font.size = Pt(9); r.font.color.rgb = GRIS
    _campo(f, "PAGE", "1", 9, GRIS)
    if total_cuerpo:
        r = f.add_run(" de "); r.font.size = Pt(9); r.font.color.rgb = GRIS
        _campo(f, "SECTIONPAGES", "1", 9, GRIS)


def _cuerpo(doc, md, base):
    lineas = md.splitlines()
    i, primer_h1, lista_num = 0, True, 0
    while i < len(lineas):
        l = lineas[i].rstrip()
        if not l.strip():
            lista_num = 0; i += 1; continue
        m = re.match(r"^(#{1,3})\s+(.*)", l)
        if m:
            nivel = len(m.group(1))
            p = doc.add_paragraph(m.group(2).strip(), style=f"Heading {nivel}")
            if nivel == 1 and not primer_h1:
                p.paragraph_format.page_break_before = True
            primer_h1 = primer_h1 and nivel != 1
            i += 1; continue
        m = re.match(r'^!\[(.+?)\]\((.+?)(?:\s+"(.+)")?\)\s*$', l)
        if m:
            # Lo que va justo antes de la figura (Ruta, Ejemplo, una línea) queda en su misma página
            for previo in reversed(doc.paragraphs[-3:]):
                if previo.style.name == ESTILO_FIGURA or previo.style.name.startswith("Heading") \
                        or previo._p.xpath(".//w:drawing"):
                    break
                previo.paragraph_format.keep_with_next = True
            titulo, ruta, alt = m.group(1), m.group(2), m.group(3) or m.group(1)
            p = doc.add_paragraph(); p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.keep_with_next = True; p.paragraph_format.space_before = Pt(6)
            p.add_run().add_picture(str((base / ruta).resolve()), width=ANCHO_FIGURA)
            for dp in p._p.iter(qn("wp:docPr")):
                dp.set("descr", alt); dp.set("title", titulo)
            c = doc.add_paragraph(style=ESTILO_FIGURA)
            mm = re.match(r"(Figura\s+[\d.]+\.)\s*(.*)", titulo)
            if mm:
                c.add_run(mm.group(1) + " ").bold = True; c.add_run(mm.group(2))
            else:
                c.add_run(titulo)
            i += 1; continue
        if l.startswith("|"):
            filas = []
            while i < len(lineas) and lineas[i].startswith("|"):
                celdas = [x.strip() for x in lineas[i].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-{3,}:?", x) for x in celdas):
                    filas.append(celdas)
                i += 1
            _tabla(doc, filas); continue
        if re.match(r"^Ruta:\s*", l):
            p = doc.add_paragraph(style="Ruta")
            r = p.add_run("Ruta: "); r.bold = True
            p.add_run(re.sub(r"^Ruta:\s*", "", l))
            i += 1; continue
        m = re.match(r"^[-*]\s+(.*)", l)
        if m:
            p = doc.add_paragraph(style="Lista MN"); p.add_run("•\t"); _texto_rico(p, m.group(1))
            i += 1; continue
        m = re.match(r"^\d+\.\s+(.*)", l)
        if m:
            lista_num += 1
            p = doc.add_paragraph(style="Lista numerada MN"); p.add_run(f"{lista_num}.\t"); _texto_rico(p, m.group(1))
            i += 1; continue
        lista_num = 0
        _texto_rico(doc.add_paragraph(), l)
        i += 1


def generar(datos_json, contenido_md, salida):
    d = json.loads(Path(datos_json).read_text(encoding="utf-8"))
    d["_dir"] = Path(datos_json).resolve().parent
    md_path = Path(contenido_md)
    doc = Document()
    _estilos(doc)
    s0 = doc.sections[0]
    s0.page_height, s0.page_width = Cm(29.7), Cm(21)
    for s in [s0]:
        s.left_margin = Cm(2.5); s.right_margin = s.top_margin = s.bottom_margin = Cm(2)
    _portada(doc, d)
    s1 = doc.add_section(WD_SECTION.NEW_PAGE); _numeracion(s1, "lowerRoman"); _cabecera_y_pie(doc, s1, d, False)
    _preliminares(doc, d)
    s2 = doc.add_section(WD_SECTION.NEW_PAGE); _numeracion(s2, "decimal"); _cabecera_y_pie(doc, s2, d, True)
    _cuerpo(doc, md_path.read_text(encoding="utf-8"), md_path.parent)
    # Word: actualizar campos al abrir (por si no se usa exportar.py)
    ajustes = doc.settings.element; uf = OxmlElement("w:updateFields"); uf.set(qn("w:val"), "true"); ajustes.append(uf)
    doc.core_properties.title = f'{d["sistema"]} — Manual de Usuario v{d["version_documento"]}'
    doc.core_properties.author = "; ".join(d["autores"])
    doc.save(salida)
    print("Generado:", salida)


if __name__ == "__main__":
    if len(sys.argv) != 4:
        print(__doc__); sys.exit(1)
    generar(*sys.argv[1:])
