"""Descarga UniMERNet Small en una revisión fija y verifica cada archivo (SHA-256).

Uso (máquina de desarrollo, con red):
    python reconocedor/preparar/descargar_modelo.py reconocedor/modelo/unimernet_small

Solo usa la biblioteca estándar. Si un archivo ya existe y su suma coincide, no lo vuelve a bajar.
Rechaza punteros de Git LFS (archivos de texto que empiezan con "version https://git-lfs").
"""
import hashlib
import json
import os
import sys
import urllib.request

REPO = "wanderkid/unimernet_small"
REVISION = "82cb85440138c2038680fca410c8f70bb7343d6d"
# Sumas SHA-256 verificadas al preparar el paquete (ver reconocedor/MODELO.md).
ARCHIVOS = json.load(open(os.path.join(os.path.dirname(__file__), "modelo.sha256.json"), encoding="utf-8"))


def sha256(ruta):
    h = hashlib.sha256()
    with open(ruta, "rb") as f:
        for bloque in iter(lambda: f.read(1 << 20), b""):
            h.update(bloque)
    return h.hexdigest()


def main(destino):
    os.makedirs(destino, exist_ok=True)
    for nombre, esperado in ARCHIVOS.items():
        ruta = os.path.join(destino, nombre)
        if os.path.exists(ruta) and sha256(ruta) == esperado:
            print(f"ok   {nombre}")
            continue
        url = f"https://huggingface.co/{REPO}/resolve/{REVISION}/{nombre}"
        print(f"baja {url}", flush=True)
        tmp = ruta + ".part"
        urllib.request.urlretrieve(url, tmp)
        with open(tmp, "rb") as f:
            if f.read(40).startswith(b"version https://git-lfs"):
                raise SystemExit(f"{nombre} es un puntero de Git LFS, no el archivo real")
        suma = sha256(tmp)
        if suma != esperado:
            raise SystemExit(f"{nombre}: SHA-256 {suma} != {esperado}")
        os.replace(tmp, ruta)
        print(f"ok   {nombre} {os.path.getsize(ruta)} bytes")
    with open(os.path.join(destino, "REVISION"), "w", encoding="utf-8") as f:
        f.write(f"{REPO}@{REVISION}\n")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "reconocedor/modelo/unimernet_small")
