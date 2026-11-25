#!/usr/bin/env python3
"""
generar_capturas.py — Genera las capturas del manual a partir de capturas.json.

    python manual/generar_capturas.py                  # todas
    python manual/generar_capturas.py --solo 3_zonas,5-1_resolver_02_solucion
    python manual/generar_capturas.py --solo-anotar    # no abre MathNova: re-anota las limpias

1. Compila MathNova (mvnw compile) y el arnés manual/arnes/ArnesCapturas.java.
   (Copia target/classes a arnes/build/clases-mathnova para que el compilador del IDE no la cambie a mitad.)
2. Abre MathNova real con una carpeta de usuario vacía (manual/.casa_capturas), así el
   Historial y la Configuración empiezan de fábrica y no salen datos personales.
3. Por cada captura ejecuta "preparar", guarda la imagen limpia a 2x en capturas/limpias/
   y las cajas de los controles (píxeles lógicos).
4. Convierte cada "control" en su "caja" y llama a herramientas/anotar.py.
   Si anotar.py da AVISO, se imprime y el script termina con código 2.

Cada captura debe ser independiente: su "preparar" empieza por "ir" a la pantalla.
"reiniciar": true abre MathNova de nuevo (carpeta de usuario vacía) antes de esa captura.
Acciones: ir (valor = sección del menú), escribir (valor = LaTeX), limpiar, clic (control),
esperar (control [, valor = segundos]), esperar_resultado, pausa (valor = ms).
Los "control" se traducen con manual/controles.json.
"""
import argparse
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

from PIL import Image

for _f in (sys.stdout, sys.stderr):
    _f.reconfigure(encoding="utf-8", errors="replace")

MANUAL = Path(__file__).resolve().parent
REPO = MANUAL.parent
sys.path.insert(0, str(MANUAL / "herramientas"))
from anotar import anotar  # noqa: E402

ARNES = MANUAL / "arnes"
BUILD = ARNES / "build"
LIMPIAS = MANUAL / "capturas" / "limpias"
ANOTADAS = MANUAL / "capturas"
CASA = MANUAL / ".casa_capturas"
WORD = MANUAL / "capturas" / "word"


def mvnw(*args):
    cmd = [str(REPO / ("mvnw.cmd" if os.name == "nt" else "mvnw")), "-q", "-B", *args]
    r = subprocess.run(cmd, cwd=REPO, shell=os.name == "nt")
    if r.returncode != 0:
        sys.exit(f"Falló: {' '.join(args)}")


def compilar():
    BUILD.mkdir(parents=True, exist_ok=True)
    print("Compilando MathNova…")
    mvnw("compile")
    cp_txt = BUILD / "cp.txt"
    if not cp_txt.exists() or cp_txt.stat().st_mtime < (REPO / "pom.xml").stat().st_mtime:
        mvnw("dependency:build-classpath", f"-Dmdep.outputFile={cp_txt}")
    copia = BUILD / "clases-mathnova"
    if copia.exists():
        shutil.rmtree(copia)
    shutil.copytree(REPO / "target" / "classes", copia)
    cp = os.pathsep.join([str(copia), cp_txt.read_text(encoding="utf-8").strip()])
    print("Compilando el arnés…")
    r = subprocess.run(["javac", "-proc:none", "-encoding", "UTF-8", "-cp", cp, "-d", str(BUILD),
                        str(ARNES / "ArnesCapturas.java")])
    if r.returncode != 0:
        sys.exit("No compiló el arnés")
    return os.pathsep.join([str(BUILD), cp])


def guion(capturas, controles):
    lineas = []
    for c in capturas:
        lineas.append(f"CAPTURA\t{c['id']}\t{c.get('escala', 2)}")
        for p in c.get("preparar", []):
            sel = controles[p["control"]] if "control" in p else ""
            lineas.append(f"ACCION\t{p['accion']}\t{sel}\t{p.get('valor', '')}")
        nombres = set()
        for m in c.get("marcas", []):
            nombres.update(m.get("controles", [m["control"]] if "control" in m else []))
        for n in sorted(nombres):
            lineas.append(f"CAJA\t{n}\t{controles[n]}")
        lineas.append("FIN")
    return "\n".join(lineas) + "\n"


def ejecutar_app(cp, capturas, controles):
    datos = CASA / ".mathnova"
    if datos.exists():
        shutil.rmtree(datos)
    CASA.mkdir(parents=True, exist_ok=True)
    LIMPIAS.mkdir(parents=True, exist_ok=True)
    archivo = BUILD / "guion.tsv"
    archivo.write_text(guion(capturas, controles), encoding="utf-8")
    cmd = ["java", f"-Duser.home={CASA}", "-Dprism.order=sw", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8",
           "-Dspring.main.banner-mode=off", "-Dlogging.level.root=WARN",
           "-cp", cp, "ArnesCapturas", str(archivo), str(LIMPIAS)]
    print(f"Abriendo MathNova para {len(capturas)} capturas: {capturas[0]['id']} … {capturas[-1]['id']}", flush=True)
    limite = 120 + 60 * len(capturas)
    try:
        p = subprocess.run(cmd, cwd=REPO, capture_output=True, text=True, encoding="utf-8", errors="replace",
                           timeout=limite)
    except subprocess.TimeoutExpired as e:
        hechas = [l for l in (e.stdout or b"").decode("utf-8", "replace").splitlines() if l.startswith("GUARDADA")]
        sys.exit(f"El arnés no terminó en {limite} s (se colgó después de: {hechas[-1] if hechas else 'ninguna'})")
    problemas = []
    for linea in p.stdout.splitlines():
        if linea.startswith("VOLCAR"):
            print("  " + linea)
            continue
        if linea.startswith(("CAPTURA", "GUARDADA")):
            continue
        if linea.startswith(("FALTA", "ERROR")) or "Exception" in linea:
            problemas.append(linea)
    for linea in problemas[:30]:
        print("  " + linea)
    if p.returncode != 0:
        print(p.stderr[-3000:])
        sys.exit(f"El arnés terminó con código {p.returncode}")
    if any(x.startswith("FALTA") for x in problemas):
        sys.exit("Hay controles que no se encontraron en pantalla")


def anotar_todas(capturas):
    total_avisos = 0
    for c in capturas:
        cid = c["id"]
        cajas = json.loads((LIMPIAS / f"{cid}.cajas.json").read_text(encoding="utf-8"))
        marcas = []
        for m in c.get("marcas", []):
            nombres = m.get("controles", [m["control"]] if "control" in m else [])
            m2 = {k: v for k, v in m.items() if k not in ("control", "controles")}
            aj = m2.pop("ajuste", [0, 0, 0, 0])
            m2["cajas"] = [_recortar(cajas[n], aj) for n in nombres]
            m2.setdefault("texto", "")
            marcas.append(m2)
        n = c.get("numero_inicial", None)
        if n is None:
            n = _siguiente_numero(capturas, c)
        z = 1
        for m in marcas:
            if m["tipo"] == "accion" and "n" not in m:
                m["n"] = n
                n += 1
            elif m["tipo"] == "zona" and "n" not in m:
                m["n"] = z
                z += 1
        img, avisos = anotar(Image.open(LIMPIAS / f"{cid}.png"), marcas, escala=c.get("escala", 2),
                             oscurecer=not c.get("sin_oscurecer", False))
        img.save(ANOTADAS / f"{cid}.png")
        # Copia liviana para el Word: con 42 imágenes de 2560 px, Word se queda sin memoria al exportar
        # y deja figuras en blanco. 1600 px a 16 cm ≈ 250 ppp.
        WORD.mkdir(parents=True, exist_ok=True)
        img.resize((1600, round(img.height * 1600 / img.width)), Image.LANCZOS).save(WORD / f"{cid}.png", optimize=True)
        for a in avisos:
            print(f"AVISO {cid}: {a}")
        total_avisos += len(avisos)
        print(f"{'⚠' if avisos else '✔'} {cid}", flush=True)
    return total_avisos


ANCHO, ALTO = 1280, 1080   # escena de las capturas (ver ArnesCapturas)


def _recortar(c, aj):
    """Aplica 'ajuste' [izq, arriba, der, abajo] (px lógicos, + agranda) y recorta al borde de la ventana."""
    x0, y0, x1, y1 = c[0] - aj[0], c[1] - aj[1], c[0] + c[2] + aj[2], c[1] + c[3] + aj[3]
    x0, y0, x1, y1 = max(x0, 4), max(y0, 4), min(x1, ANCHO - 4), min(y1, ALTO - 4)
    return [x0, y0, x1 - x0, y1 - y0]


def _siguiente_numero(capturas, actual):
    """La numeración empieza en 1 en cada tarea y sigue entre sus capturas."""
    n = 1
    for c in TODAS:
        if c["id"] == actual["id"]:
            return n
        if c.get("tarea") == actual.get("tarea"):
            n += sum(1 for m in c.get("marcas", []) if m["tipo"] == "accion" and "n" not in m)
    return n


TODAS = []


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--solo", help="ids separados por coma")
    ap.add_argument("--solo-anotar", action="store_true")
    ap.add_argument("--desde", help="genera desde esta captura hasta el final")
    ap.add_argument("--capturas", default=str(MANUAL / "capturas.json"))
    a = ap.parse_args()
    TODAS.extend(json.loads(Path(a.capturas).read_text(encoding="utf-8")))
    controles = json.loads((MANUAL / "controles.json").read_text(encoding="utf-8"))
    capturas = TODAS
    if a.desde:
        ids = [c["id"] for c in TODAS]
        if a.desde not in ids:
            sys.exit(f"id desconocido: {a.desde}")
        capturas = TODAS[ids.index(a.desde):]
    if a.solo:
        ids = set(a.solo.split(","))
        capturas = [c for c in TODAS if c["id"] in ids]
        if len(capturas) != len(ids):
            sys.exit(f"ids desconocidos: {ids - {c['id'] for c in capturas}}")
    if not a.solo_anotar:
        cp = compilar()
        grupos = []
        for c in capturas:
            if not grupos or c.get("reiniciar"):
                grupos.append([])
            grupos[-1].append(c)
        for g in grupos:
            try:
                ejecutar_app(cp, g, controles)
            except SystemExit as e:
                print(f"Reintento del grupo {g[0]['id']} … ({e})", flush=True)
                ejecutar_app(cp, g, controles)
    avisos = anotar_todas(capturas)
    print(f"\n{len(capturas)} capturas, {avisos} avisos")
    sys.exit(2 if avisos else 0)


if __name__ == "__main__":
    main()
