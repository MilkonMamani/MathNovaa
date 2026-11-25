#!/usr/bin/env python3
"""
anotar.py — Dibuja las anotaciones del manual sobre una captura limpia.

Estilo (no cambiar sin aprobación):
  - Oscurece toda la imagen menos los elementos marcados.
  - Rojo   #D92D20 = acción del usuario (número 1, 2, 3…)
  - Verde  #16A34A = resultado (✓)
  - Azul   #1F3864 = zona de la pantalla principal (número dentro de la zona, sin etiqueta)
  - Etiqueta blanca tipo "píldora", texto negro en negrita, junto al círculo.
  - Borde gris fino alrededor de toda la imagen (va dentro del PNG, no en Word).

Uso por consola:
    python anotar.py limpia.png marcas.json anotada.png [--escala 2] [--sin-oscurecer]

Uso como módulo:
    from anotar import anotar
    img, avisos = anotar(Image.open("limpia.png"), marcas, escala=2)

Formato de cada marca (coordenadas en píxeles LÓGICOS, es decir, a escala 1):
    {"caja": [x, y, ancho, alto],          # o "cajas": [[...], [...]] para unir varias
     "tipo": "accion" | "ok" | "zona",
     "n": 1,                                # número del paso (no se usa en "ok")
     "texto": "Haz clic en Resolver",       # 2 a 6 palabras; vacío en "zona"
     "lado": "derecha" | "izquierda" | "arriba" | "abajo",   # preferencia; si choca se mueve
     "dist": 70,                            # opcional: distancia del círculo al recuadro
     "pos": [0.5, 0.9]}                     # solo "zona": posición del número dentro (fracción)

El script NUNCA falla en silencio: devuelve "avisos" cuando una etiqueta no cabe sin
chocar o se sale de la imagen. Si hay avisos, corrige "lado"/"dist" en capturas.json.
Código de salida: 0 sin avisos, 2 con avisos.
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

COLORES = {"accion": (217, 45, 32), "ok": (22, 163, 74), "zona": (31, 56, 100)}
OSCURO = (15, 23, 20, 140)          # rgba(15,23,20,0.55)
BORDE_IMAGEN = (191, 191, 191)      # #BFBFBF

# Medidas en píxeles lógicos (se multiplican por la escala)
MARGEN_RECORTE = 6      # aire alrededor del elemento resaltado
RADIO_RECUADRO = 10
GROSOR_RECUADRO = 4
RADIO_CIRCULO = 22
RADIO_CIRCULO_ZONA_FINA = 13   # para zonas de menos de 40 px de alto
GROSOR_FLECHA = 4
ALTO_ETIQUETA = 46
TAM_TEXTO = 22
TAM_NUMERO = 22
DIST_DEFECTO = 70
SEPARACION = 10         # entre círculo y etiqueta
MARGEN_IMAGEN = 8       # una etiqueta nunca toca el borde

FUENTES = [
    "C:/Windows/Fonts/segoeuib.ttf", "C:/Windows/Fonts/seguisb.ttf", "C:/Windows/Fonts/arialbd.ttf",
    "/System/Library/Fonts/Supplemental/Arial Bold.ttf",
    "/usr/share/fonts/truetype/inter/Inter-Bold.ttf",
    "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
    "/usr/share/fonts/dejavu/DejaVuSans-Bold.ttf",
]


def _fuente(tam):
    for f in FUENTES:
        if Path(f).exists():
            return ImageFont.truetype(f, tam)
    return ImageFont.load_default(size=tam)


def _choca(a, b, pad=0):
    return not (a[2] + pad <= b[0] or b[2] + pad <= a[0] or a[3] + pad <= b[1] or b[3] + pad <= a[1])


def _dentro(r, W, H, m):
    return r[0] >= m and r[1] >= m and r[2] <= W - m and r[3] <= H - m


def _caja_de(marca, e):
    cajas = marca.get("cajas") or [marca["caja"]]
    x0 = min(c[0] for c in cajas); y0 = min(c[1] for c in cajas)
    x1 = max(c[0] + c[2] for c in cajas); y1 = max(c[1] + c[3] for c in cajas)
    p = MARGEN_RECORTE
    return [(x0 - p) * e, (y0 - p) * e, (x1 + p) * e, (y1 + p) * e]


def _posiciones(b, lado, d, R, pw, ph, W, sep, etiqueta_izq=None):
    """Devuelve (centro del círculo, punta de la flecha, rectángulo de la etiqueta)."""
    if lado == "derecha":
        cx, cy = b[2] + d, (b[1] + b[3]) / 2; punta = (b[2] + 2, cy)
    elif lado == "izquierda":
        cx, cy = b[0] - d, (b[1] + b[3]) / 2; punta = (b[0] - 2, cy)
    elif lado == "arriba":
        cx, cy = (b[0] + b[2]) / 2, b[1] - d; punta = (cx, b[1] - 2)
    else:
        cx, cy = (b[0] + b[2]) / 2, b[3] + d; punta = (cx, b[3] + 2)
    if etiqueta_izq is None:
        etiqueta_izq = lado == "izquierda" or (lado in ("arriba", "abajo") and cx + R + sep + pw > W - MARGEN_IMAGEN)
    if etiqueta_izq:
        lx = cx - R - sep - pw
    else:
        lx = cx + R + sep
    etiqueta = [lx, cy - ph / 2, lx + pw, cy + ph / 2]
    return (cx, cy), punta, etiqueta


def anotar(img, marcas, escala=1, oscurecer=True):
    e = escala
    img = img.convert("RGBA")
    W, H = img.size
    avisos = []
    cajas = [_caja_de(m, e) for m in marcas]

    # 1) Oscurecer todo menos lo marcado
    if oscurecer and any(m.get("tipo", "accion") != "zona" for m in marcas):
        capa = Image.new("RGBA", img.size, OSCURO)
        mascara = Image.new("L", img.size, 255)
        dm = ImageDraw.Draw(mascara)
        for b in cajas:
            dm.rounded_rectangle(b, radius=RADIO_RECUADRO * e, fill=0)
        capa.putalpha(Image.composite(Image.new("L", img.size, OSCURO[3]), Image.new("L", img.size, 0), mascara))
        img = Image.alpha_composite(img, capa)

    d = ImageDraw.Draw(img)
    f_txt = _fuente(TAM_TEXTO * e)
    f_num = _fuente(TAM_NUMERO * e)
    ocupado = [list(b) for b in cajas]      # lo que una etiqueta no debe tapar

    for i, (m, b) in enumerate(zip(marcas, cajas)):
        tipo = m.get("tipo", "accion")
        color = COLORES[tipo]
        d.rounded_rectangle(b, radius=RADIO_RECUADRO * e, outline=color, width=GROSOR_RECUADRO * e)

        # --- zona: número dentro, sin etiqueta ni flecha
        if tipo == "zona":
            fx, fy = m.get("pos", [0.95, 0.08])
            alto = (b[3] - b[1]) / e
            R = (RADIO_CIRCULO_ZONA_FINA if alto < 40 else RADIO_CIRCULO) * e
            cx = b[0] + (b[2] - b[0]) * fx; cy = b[1] + (b[3] - b[1]) * fy
            cx = min(max(cx, b[0] + R + 2 * e), b[2] - R - 2 * e)
            cy = min(max(cy, b[1] + R + 2 * e), b[3] - R - 2 * e)
            _circulo(d, cx, cy, R, color, str(m.get("n", "")), _fuente(int(R * 0.95)), e)
            continue

        texto = m.get("texto", "")
        R = RADIO_CIRCULO * e
        ph = ALTO_ETIQUETA * e
        pw = d.textlength(texto, font=f_txt) + 32 * e if texto else 0
        pref = m.get("lado", "derecha")
        orden = [pref] + [s for s in ("derecha", "abajo", "arriba", "izquierda") if s != pref]
        dist0 = m.get("dist", DIST_DEFECTO)
        elegido = None
        intentos = [(lado, dist, izq) for lado in orden
                    for dist in (dist0, dist0 * 0.6, dist0 * 1.6, dist0 * 2.4, dist0 * 3.2, dist0 * 4.2, dist0 * 5.5)
                    for izq in ((None, True, False) if lado in ("arriba", "abajo") else (None,))]
        for lado, dist, izq in intentos:
            c, punta, et = _posiciones(b, lado, dist * e, R, pw, ph, W, SEPARACION * e, izq)
            circ = [c[0] - R, c[1] - R, c[0] + R, c[1] + R]
            zonas = [circ] + ([et] if texto else [])
            if not all(_dentro(z, W, H, MARGEN_IMAGEN * e) for z in zonas):
                continue
            otros = [o for j, o in enumerate(ocupado) if j != i]
            if any(_choca(z, o, 4 * e) for z in zonas for o in otros):
                continue
            elegido = (c, punta, et, circ); break
        if not elegido:
            c, punta, et = _posiciones(b, pref, dist0 * e, R, pw, ph, W, SEPARACION * e)
            elegido = (c, punta, et, [c[0] - R, c[1] - R, c[0] + R, c[1] + R])
            avisos.append(f"marca {i + 1} ('{texto}'): la etiqueta no cabe sin chocar o salirse; "
                          f"cambia 'lado' o 'dist'")
        (cx, cy), punta, et, circ = elegido
        ocupado += [circ] + ([et] if texto else [])

        _flecha(d, (cx, cy), punta, R, color, e)
        _circulo(d, cx, cy, R, color, None if tipo == "ok" else str(m.get("n", "")), f_num, e, check=(tipo == "ok"))
        if texto:
            d.rounded_rectangle(et, radius=8 * e, fill=(255, 255, 255), outline=color, width=2 * e)
            tb = d.textbbox((0, 0), texto, font=f_txt)
            d.text((et[0] + 16 * e, (et[1] + et[3]) / 2 - (tb[1] + tb[3]) / 2), texto, font=f_txt, fill=(17, 17, 17))

    # Borde fino dentro de la imagen
    d.rectangle([0, 0, W - 1, H - 1], outline=BORDE_IMAGEN, width=max(1, int(e)))
    return img.convert("RGB"), avisos


def _circulo(d, cx, cy, R, color, numero, fuente, e, check=False):
    d.ellipse([cx - R, cy - R, cx + R, cy + R], fill=color, outline=(255, 255, 255), width=3 * e)
    if check:
        s = R * 0.45
        d.line([(cx - s, cy + s * 0.05), (cx - s * 0.25, cy + s * 0.7), (cx + s, cy - s * 0.6)],
               fill=(255, 255, 255), width=int(3.5 * e), joint="curve")
    elif numero:
        tb = d.textbbox((0, 0), numero, font=fuente)
        d.text((cx - (tb[0] + tb[2]) / 2, cy - (tb[1] + tb[3]) / 2), numero, font=fuente, fill=(255, 255, 255))


def _flecha(d, origen, punta, R, color, e):
    dx, dy = punta[0] - origen[0], punta[1] - origen[1]
    L = math.hypot(dx, dy)
    if L <= R + 4 * e:
        return
    ux, uy = dx / L, dy / L
    inicio = (origen[0] + ux * R, origen[1] + uy * R)
    largo, ancho = 14 * e, 8 * e
    base = (punta[0] - ux * largo, punta[1] - uy * largo)
    d.line([inicio, base], fill=color, width=GROSOR_FLECHA * e)
    d.polygon([punta, (base[0] - uy * ancho, base[1] + ux * ancho), (base[0] + uy * ancho, base[1] - ux * ancho)], fill=color)


if __name__ == "__main__":
    import argparse
    ap = argparse.ArgumentParser(description="Anota una captura limpia para el manual.")
    ap.add_argument("entrada"); ap.add_argument("marcas_json"); ap.add_argument("salida")
    ap.add_argument("--escala", type=float, default=1, help="2 si la captura se tomó al 200 %%")
    ap.add_argument("--sin-oscurecer", action="store_true", help="para la captura de zonas")
    a = ap.parse_args()
    esc = int(a.escala) if a.escala.is_integer() else a.escala
    marcas = json.loads(Path(a.marcas_json).read_text(encoding="utf-8"))
    img, avisos = anotar(Image.open(a.entrada), marcas, escala=esc, oscurecer=not a.sin_oscurecer)
    img.save(a.salida)
    for av in avisos:
        print("AVISO:", av)
    sys.exit(2 if avisos else 0)
