# Manual de usuario de MathNova

Manual según ISO/IEC/IEEE 26514:2022. Todo se genera por script: no se editan capturas ni el Word a mano.

## Regenerar todo

Desde la carpeta del proyecto (`D:\MathNovaa`), con Java 21, Python 3.10+ y Microsoft Word (o LibreOffice):

```
pip install -r manual/herramientas/requirements.txt sympy "antlr4-python3-runtime==4.11" pymupdf
python manual/arnes/definir_capturas.py          # escribe capturas.json (lista de capturas y marcas)
python manual/generar_capturas.py                # abre MathNova, toma y anota las capturas (~15 min)
python manual/verificacion/verificar.py          # verifica con SymPy -> verificacion/REPORTE.md
python manual/herramientas/generar_docx.py manual/contenido/datos.json manual/contenido/manual.md manual/salida/MathNova_Manual_de_Usuario_v1.0.docx
python manual/herramientas/exportar.py manual/salida/MathNova_Manual_de_Usuario_v1.0.docx manual/salida/MathNova_Manual_de_Usuario_v1.0.pdf
python manual/herramientas/verificar_manual.py manual/salida/MathNova_Manual_de_Usuario_v1.0.docx manual/salida/MathNova_Manual_de_Usuario_v1.0.pdf
python manual/referencia_rapida.py               # salida/MathNova_Referencia_rapida.pdf (1 página)
```

Opciones útiles de `generar_capturas.py`: `--solo id1,id2`, `--desde id`, `--solo-anotar` (no abre la app).

## Carpetas

| Ruta | Qué es |
|---|---|
| `INVENTARIO.md` | Pantallas, controles, Q/R/C y mensajes de MathNova (FASE 1). |
| `PROGRESO.md` | Estado, decisiones y hallazgos. |
| `controles.json` | Nombre de cada control → selector de JavaFX. |
| `capturas.json` | Capturas: pasos para preparar la pantalla y marcas (lo escribe `arnes/definir_capturas.py`). |
| `arnes/ArnesCapturas.java` | Abre MathNova real y fotografía la ventana (1280×1080, a 2×). |
| `capturas/limpias/` | Capturas sin anotar + cajas de los controles + texto real de la pantalla. |
| `capturas/` | Capturas anotadas (2×). `capturas/word/`: copias de 1600 px que usa el Word. |
| `verificacion/` | `verificar.py` y `REPORTE.md` (solo para el equipo). |
| `contenido/` | `manual.md` (cuerpo) y `datos.json` (portada y control de versiones). |
| `herramientas/` | Herramientas del equipo (anotar, generar_docx, exportar, verificar_manual). |
| `salida/` | Word y PDF del manual, y la referencia rápida. |
