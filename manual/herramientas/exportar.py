#!/usr/bin/env python3
"""
exportar.py — Deja el manual listo para entregar: actualiza los índices y genera el PDF.

    python exportar.py manual.docx salida.pdf

Qué resuelve (problemas reales que aparecen al generar Word por código):
  1. El índice general y el índice de figuras quedan VACÍOS hasta que se actualizan los campos.
  2. "Página X de Y" usa el campo SECTIONPAGES (total de páginas del cuerpo). Word lo calcula,
     pero LibreOffice NO: en el PDF la "Y" saldría vacía.

Rutas (elige sola la primera que funcione):
  A) Windows + Microsoft Word + pywin32  (pip install pywin32)   ← la mejor
     Actualiza índices y campos, guarda el .docx y exporta el PDF con marcadores.
  B) LibreOffice (cualquier sistema). Debe ejecutarse con el Python que trae LibreOffice
     para poder importar "uno". En Windows:
        "C:\\Program Files\\LibreOffice\\program\\python.exe" exportar.py manual.docx salida.pdf
     Actualiza índices, y para el PDF escribe el total de páginas del cuerpo en una copia
     temporal (porque LibreOffice no calcula SECTIONPAGES). El .docx entregado conserva el campo.

Al final imprime una comprobación: páginas del PDF y si el pie "Página X de Y" quedó completo.
"""
import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile
from pathlib import Path


# ---------------------------------------------------------------- utilidades PDF
def _textos_pdf(pdf):
    try:
        from pypdf import PdfReader
        return [(p.extract_text() or "") for p in PdfReader(str(pdf)).pages]
    except ImportError:
        if shutil.which("pdftotext"):
            out = subprocess.run(["pdftotext", "-layout", str(pdf), "-"], capture_output=True, text=True).stdout
            return out.split("\f")
        return None


def _comprobar(pdf):
    pags = _textos_pdf(pdf)
    if pags is None:
        print("PDF generado. (Para comprobarlo aquí instala pypdf; si no, usa verificar_manual.py.)")
        return True
    vacios = [i + 1 for i, t in enumerate(pags) if re.search(r"P[áa]gina\s+\d+\s+de\s*$", t, re.M)]
    print(f"PDF: {len(pags)} páginas.", "Pie 'Página X de Y' completo." if not vacios
          else f"ATENCIÓN: pie sin total en las páginas {vacios}.")
    return not vacios


# ---------------------------------------------------------------- ruta A: Word
def _con_word(docx, pdf):
    import win32com.client  # noqa: solo existe en Windows con pywin32
    word = win32com.client.DispatchEx("Word.Application")
    word.Visible = False
    word.DisplayAlerts = 0
    try:
        doc = word.Documents.Open(str(Path(docx).resolve()))
        for _ in range(2):  # dos pasadas: los índices cambian el número de páginas
            for toc in doc.TablesOfContents:
                toc.Update()
            for tof in doc.TablesOfFigures:
                tof.Update()
            for story in doc.StoryRanges:
                story.Fields.Update()
                nxt = story.NextStoryRange
                while nxt is not None:
                    nxt.Fields.Update(); nxt = nxt.NextStoryRange
        doc.Save()
        # 17 = PDF; CreateBookmarks 1 = marcadores desde los títulos
        doc.ExportAsFixedFormat(str(Path(pdf).resolve()), 17, CreateBookmarks=1)
        doc.Close(False)
    finally:
        word.Quit()


# ---------------------------------------------------------------- ruta B: LibreOffice
def _soffice():
    for c in ("soffice", "libreoffice", r"C:\Program Files\LibreOffice\program\soffice.exe",
              "/Applications/LibreOffice.app/Contents/MacOS/soffice"):
        if shutil.which(c) or Path(c).exists():
            return shutil.which(c) or c
    raise RuntimeError("No encuentro LibreOffice (soffice).")


def _lo_actualizar_y_exportar(docx_in, docx_out, pdf_out):
    import uno
    from com.sun.star.beans import PropertyValue

    def P(n, v):
        x = PropertyValue(); x.Name = n; x.Value = v; return x

    perfil = Path(tempfile.mkdtemp(prefix="lo_perfil_"))
    proc = subprocess.Popen([_soffice(), "--headless", "--invisible", "--norestore",
                             f"-env:UserInstallation={perfil.as_uri()}",
                             "--accept=socket,host=localhost,port=2083;urp;"])
    try:
        ctx = None
        for _ in range(60):
            try:
                lc = uno.getComponentContext()
                r = lc.ServiceManager.createInstanceWithContext("com.sun.star.bridge.UnoUrlResolver", lc)
                ctx = r.resolve("uno:socket,host=localhost,port=2083;urp;StarOffice.ComponentContext"); break
            except Exception:
                time.sleep(0.5)
        if ctx is None:
            raise RuntimeError("LibreOffice no respondió.")
        desk = ctx.ServiceManager.createInstanceWithContext("com.sun.star.frame.Desktop", ctx)
        doc = desk.loadComponentFromURL(uno.systemPathToFileUrl(str(Path(docx_in).resolve())), "_blank", 0, (P("Hidden", True),))
        doc.getTextFields().refresh()
        idx = doc.getDocumentIndexes()
        for _ in range(2):
            for i in range(idx.getCount()):
                idx.getByIndex(i).update()
        # Páginas: total y página donde empieza el cuerpo (primer título de nivel 1)
        vc = doc.getCurrentController().getViewCursor()
        vc.jumpToLastPage(); total = vc.getPage()
        inicio = None
        enum = doc.getText().createEnumeration()
        while enum.hasMoreElements():
            par = enum.nextElement()
            if par.supportsService("com.sun.star.text.Paragraph") and par.getPropertyValue("OutlineLevel") == 1 \
                    and par.getString().strip()[:1].isdigit():
                vc.gotoRange(par.getStart(), False); inicio = vc.getPage(); break
        if docx_out:
            doc.storeToURL(uno.systemPathToFileUrl(str(Path(docx_out).resolve())), (P("FilterName", "MS Word 2007 XML"),))
        fd = uno.Any("[]com.sun.star.beans.PropertyValue", (P("ExportBookmarks", True),))
        doc.storeToURL(uno.systemPathToFileUrl(str(Path(pdf_out).resolve())), (P("FilterName", "writer_pdf_Export"), P("FilterData", fd)))
        doc.close(True)
        return total, inicio
    finally:
        try:
            desk.terminate()
        except Exception:
            pass
        try:
            proc.wait(timeout=20)
        except Exception:
            proc.kill()
        shutil.rmtree(perfil, ignore_errors=True)


def _reemplazar_sectionpages(docx_in, docx_out, total):
    """Copia el .docx cambiando cada campo SECTIONPAGES por el número fijo `total`."""
    def fijar(xml):
        # campo simple
        xml = re.sub(r'<w:fldSimple[^>]*w:instr="\s*SECTIONPAGES[^"]*"[^>]*>(.*?)</w:fldSimple>',
                     lambda m: f"<w:r><w:t>{total}</w:t></w:r>", xml, flags=re.S)
        # campo complejo: begin … instrText SECTIONPAGES … end (en uno o varios runs)
        while True:
            i = xml.find("SECTIONPAGES")
            if i < 0:
                break
            b = xml.rfind('w:fldCharType="begin"', 0, i)
            rb = max(xml.rfind("<w:r>", 0, b), xml.rfind("<w:r ", 0, b))
            e = xml.find('w:fldCharType="end"', i)
            re_ = xml.find("</w:r>", e) + len("</w:r>")
            rpr = re.search(r"<w:rPr>.*?</w:rPr>", xml[rb:i], re.S)
            xml = xml[:rb] + f"<w:r>{rpr.group(0) if rpr else ''}<w:t>{total}</w:t></w:r>" + xml[re_:]
        return xml
    with zipfile.ZipFile(docx_in) as zi, zipfile.ZipFile(docx_out, "w", zipfile.ZIP_DEFLATED) as zo:
        for item in zi.infolist():
            data = zi.read(item.filename)
            if item.filename.startswith("word/") and item.filename.endswith(".xml") and b"SECTIONPAGES" in data:
                data = fijar(data.decode("utf-8")).encode("utf-8")
            zo.writestr(item, data)


def _con_libreoffice(docx, pdf):
    tmp = Path(tempfile.mkdtemp(prefix="manual_"))
    actualizado = tmp / "actualizado.docx"
    pdf1 = tmp / "pasada1.pdf"
    paginas, inicio = _lo_actualizar_y_exportar(docx, actualizado, pdf1)
    pags = _textos_pdf(pdf1)                  # si hay pypdf, se cuenta en el PDF real (más fiable)
    if pags:
        i1 = next((i for i, t in enumerate(pags) if re.search(r"P[áa]gina\s+1\s+de", t)), None)
        if i1 is not None:
            paginas, inicio = len(pags), i1 + 1
    if inicio is None:
        shutil.copy(pdf1, pdf)
    else:
        total = paginas - inicio + 1          # páginas del cuerpo
        print(f"Cuerpo: páginas {inicio} a {paginas} ({total} páginas).")
        fijo = tmp / "fijo.docx"
        _reemplazar_sectionpages(actualizado, fijo, total)
        _lo_actualizar_y_exportar(fijo, None, pdf)
    shutil.copy(actualizado, docx)   # el .docx entregado: índices llenos y campo SECTIONPAGES intacto
    shutil.rmtree(tmp, ignore_errors=True)


def exportar(docx, pdf):
    try:
        import win32com.client  # noqa
        print("Usando Microsoft Word…")
        _con_word(docx, pdf)
    except ImportError:
        print("Usando LibreOffice…")
        _con_libreoffice(docx, pdf)
    return _comprobar(pdf)


if __name__ == "__main__":
    if len(sys.argv) != 3:
        print(__doc__); sys.exit(1)
    sys.exit(0 if exportar(sys.argv[1], sys.argv[2]) else 2)
