"""Lista los paquetes de Python del runtime con su versión y la licencia que declaran (para LICENCIAS/)."""
import importlib.metadata as m
import sys

filas = []
for d in m.distributions():
    md = d.metadata
    lic = (md.get("License-Expression") or md.get("License") or "").strip().splitlines()
    clasif = [c.split("::")[-1].strip() for c in (md.get_all("Classifier") or []) if c.startswith("License ::")]
    texto = (lic[0] if lic else "") or ", ".join(clasif) or "ver dist-info"
    filas.append(f"{md['Name']}=={d.version}\t{texto[:100]}")
with open(sys.argv[1], "w", encoding="utf-8") as f:
    f.write("Paquetes de Python redistribuidos en reconocedor\python (licencia declarada por cada paquete;\n"
            "el texto completo está en Lib\site-packages\<paquete>.dist-info):\n\n")
    f.write("\n".join(sorted(filas, key=str.lower)) + "\n")
