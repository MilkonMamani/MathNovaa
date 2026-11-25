# PROGRESO del manual de usuario de MathNova

## Estado
| Fase | Estado |
|---|---|
| 1 Reconocimiento | ✅ Aprobada — `manual/INVENTARIO.md` |
| 2 Plan | ✅ Aprobada (índice de 13 capítulos + anexo; ejercicios x³−2x²−x+2 y x⁴−4) |
| 3 Capturas | ✅ Aprobada la muestra; 42 capturas generadas, 0 avisos (`manual/capturas/`) |
| 4 Verificación | ✅ 51 comprobaciones, 0 ❌ en capturas del manual. El ❌ de x⁴−4 en R queda reportado (opción 2: esa captura no va al manual) |
| 5 Revisión visual | ✅ 2 rondas con subagente; corregidos todos los problemas (diálogos con marco, esperas, desplazamientos, etiquetas) |
| 6 Redacción y Word | ✅ Manual completo: 55 págs., 44 figuras, 35 tareas, "✅ Todo correcto"; referencia rápida de 1 página |
| 7 Usuario novato | ✅ 1.ª ronda: 7 bloqueos → corregidos (4.5 nueva, ¿Terminaste?, 2.2, 5.6, 10.4, cap. 11). 2.ª ronda: 0 bloqueos |
| 8 Entrega | ✅ Resumen entregado al equipo (25/11/2026) |

Decisión 25/11/2026: la portada declara la versión que muestra la app (0.0.1-SNAPSHOT), no 0.2.0.

## Hecho
- 25/11/2026: tests del proyecto en verde (5532, 0 fallos) tras actualizar `PruebaHumoTest` a la pantalla sin tarjeta Solución, y mover las notas del resultado a la Pizarra (cambios sin commit).
- 25/11/2026: FASE 1. App ejecutada con `.\mvnw.cmd spring-boot:run` (arranca en ~10 s). Inventario hecho con 3 subagentes.

## Decisiones (aprobadas por el usuario, 25/11/2026)
- Versión de MathNova en el manual: **0.0.1-SNAPSHOT** (la que muestra Acerca de; antes se había elegido 0.2.0).
- Fecha del documento: **25 de noviembre de 2026**.
- Instalación: **no va en este manual** (irá en otro documento). El cap. 2 solo cubre requisitos, abrir y cerrar.
- Pestaña **Escribir** (a mano) y sección **Práctica**: **se incluyen**, cada una con todas sus tareas.
- Git: todo queda **en local, sin commit** por ahora (decisión del 25/11/2026).

## Cómo se generan las capturas (FASE 3)
- `python manual/generar_capturas.py [--solo id,id] [--solo-anotar]`.
- Arnés Java `manual/arnes/ArnesCapturas.java`: abre MathNova real (Spring + `Navegador.iniciar`), ejecuta el guion y fotografía la **escena** a 2× (sin barra de título, sin cursor, sin ventanas ajenas, sin depender de la escala 125 %). Pega encima los diálogos JavaFX abiertos.
- Tamaño de escena fijo **1280×1080** (con 1280×800 el Resultado del ejercicio principal quedaba cortado y había barra de desplazamiento) → imágenes 2560×2160.
- Carpeta de usuario aislada `manual/.casa_capturas` (historial y configuración de fábrica). `"reiniciar": true` abre la app de nuevo antes de esa captura.
- Controles por nombre en `manual/controles.json`; numeración automática por `"tarea"`.

## Cambios autorizados en herramientas (25/11/2026)
- `generar_docx.py`: el campo del índice de figuras usa el separador de listas de Windows (`;` en es-PE); con `,` salía vacío.
- `generar_docx.py`: los párrafos justo antes de una figura (Ruta, Ejemplo, una línea) se marcan "conservar con el siguiente" (no más títulos solos al pie).

## Hallazgos de la FASE 6
- Word se quedaba sin memoria con 42 imágenes de 2560×2160 y dejaba figuras en blanco (marcador de 4×5 px).
  `verificar_manual.py` NO lo detecta. Solución: el Word usa `capturas/word/` (1600 px ≈ 250 ppp a 16 cm).
- Configuración › Acerca de muestra "MathNova 0.0.1-SNAPSHOT" (pom.xml), mientras el ejecutable se empaqueta como 0.2.0. El manual declara 0.0.1-SNAPSHOT.

## Comando completo
```
python manual/generar_capturas.py
python manual/verificacion/verificar.py
python manual/herramientas/generar_docx.py manual/contenido/datos.json manual/contenido/manual.md manual/salida/MathNova_Manual_de_Usuario_v1.0.docx
python manual/herramientas/exportar.py manual/salida/MathNova_Manual_de_Usuario_v1.0.docx manual/salida/MathNova_Manual_de_Usuario_v1.0.pdf
python manual/herramientas/verificar_manual.py manual/salida/MathNova_Manual_de_Usuario_v1.0.docx manual/salida/MathNova_Manual_de_Usuario_v1.0.pdf
```

## FASE 4 — hallazgos
- `python manual/verificacion/verificar.py` → `manual/verificacion/REPORTE.md`. Compara con SymPy el LaTeX real
  que el arnés guarda en `capturas/limpias/<id>.texto.txt`.
- ❌ `5-6_qrc_02` (x⁴−4 en R): el paso 2 "Pasar a R" muestra **(x²+2)(x²−2) = x²−2**, igualdad falsa.
  El resultado final (x²+2)(x−√2)(x+√2) sí es correcto. No se cambió el ejercicio (regla del prompt): se consultó.
- Además en esa captura la Comprobación queda cortada (el procedimiento en R es más largo que 1080 px).
- Lecciones del arnés: los clics no deben esperar (un diálogo modal colgó la corrida 1,5 h); ventana sin marco
  (Windows achicaba la ventana); dibujo por software (`-Dprism.order=sw`, error D3D intermitente); copia de
  `target/classes` (el IDE recompila a mitad); foto recortada a la ventana (la sombra de la hoja la agrandaba).

## Notas técnicas para las capturas
- Pantalla 1536×864 lógicos, escala 125 % → proceso DPI-aware o multiplicar cajas por 1,25.
- Harness JavaFX: se puede reutilizar el patrón de `src/test/java/.../PruebaHumoTest.java` (abre `resolucion.fxml`, escribe con el teclado, `lookup` por id/clase).
