# INVENTARIO de MathNova (FASE 1)

Fuente: lectura del código (rama `main`, 25/11/2026) y una ejecución real. Rutas relativas a `src/main/`.
Textos entre comillas = literales del código.

---

## 1.1 Tecnología

| Ítem | Valor |
|---|---|
| Lenguaje | Java 21 |
| Interfaz | JavaFX 21.0.5 (FXML + CSS); editor de fórmulas = MathLive dentro de un WebView |
| Framework | Spring Boot 4.1.2 (sin servidor web: `spring.main.web-application-type=none`) |
| Datos | SQLite en `~/.mathnova/mathnova.db` (Flyway V1–V4) |
| Otras dependencias | jlatexmath 1.0.7 (fórmulas), PDFBox (PDF), reconocedor de escritura UniMERNet (Python embebido en `reconocedor/`) |
| Punto de entrada | `java/pe/mathnova/Launcher.java` (configurado en `spring-boot-maven-plugin`) |
| Comando para ejecutar (desarrollo) | `.\mvnw.cmd spring-boot:run` desde `D:\MathNovaa` |
| Resultado de la ejecución | ✅ Arranca: `Started application in 10.165 seconds`; `Reconocedor UniMERNet en D:\MathNovaa\reconocedor`. Ventana "MathNova" abre en **Resolver**. |
| Ventana | Título "MathNova", 1280×800 por defecto, mínimo 860×600 (`Navegador.java:47-51`) |
| Pantalla del equipo de pruebas | 1536×864 lógicos (escala de Windows 125 %) → para capturas hay que hacer el proceso DPI-aware |

**Cómo lo obtiene un usuario final:** no hay instalador. `scripts/empaquetar-windows.ps1` genera con `jpackage` una **carpeta portable** `dist/MathNova/` con `MathNova.exe` y la subcarpeta `reconocedor/` (≈ varios GB por el modelo). El usuario copia la carpeta y abre `MathNova.exe` (ver `reconocedor/LEEME-ALUMNO.txt`). La carpeta `dist/` **todavía no está generada**. No crea acceso directo ni hay desinstalador: se desinstala borrando la carpeta (y, si se quiere, `%USERPROFILE%\.mathnova`).
Requisitos citados en el LEEME: Windows 10/11 de 64 bits; el reconocedor usa ≈2,5 GB de RAM (recomienda 8 GB). No necesita internet.

**Versión de MathNova:** inconsistente.
- Configuración › Acerca de muestra `mathnova.version=@project.version@` → **0.0.1-SNAPSHOT** (pom.xml).
- El .exe empaquetado se marca `--app-version 0.2.0`; "Motor de resolución" = 0.2.0.
→ Pendiente de decisión (ver resumen).

---

## 1.2 Mapa de pantallas

```
Ventana MathNova
├── Menú lateral (contraído por defecto: solo íconos; botón ☰ "Expandir menú"/"Contraer menú")
│   ├── Resolver        (se abre al iniciar)
│   ├── Historial
│   ├── Práctica
│   ├── Ejemplos
│   └── Configuración
│
├── Menú › Resolver  ("Resolver ejercicio")
│   ├── Barra del ejercicio: editor · [✎ Escrito a mano] · Q R C · Resolver
│   ├── Aviso bajo la barra (errores) · "Pulsa Resolver para actualizar"
│   ├── Panel izquierdo – pestañas
│   │   ├── Teclado   (píldoras 123 · f(x) · ABC · #&¬; αβγ desde ABC; botón ••• → Configuración)
│   │   ├── Escribir  (escritura a mano: Lápiz, Borrador, Deshacer, Borrar todo, Reintentar)
│   │   ├── Gráfica   (se abre sola al resolver; +, −, Restablecer vista; curvas de nivel en 2 variables)
│   │   ├── Análisis  (tabla de valores, signo, conjunto solución, asíntotas)
│   │   └── Exploración (tarjetas de métodos Usado/Probar/No aplica + "Comparación Q / R / C")
│   └── Panel derecho – Pizarra
│       ├── Cabecera: ‹ · "Todos los pasos"/"Paso N de M" · › · "Paso a paso"/"Ver todo" · Explicaciones · Exportar PDF
│       ├── Rótulo (FACTORIZA, DERIVA…) · enunciado · pasos numerados (botón ? = explicación)
│       ├── Resultado (+ notas, "También se puede resolver con:")
│       ├── Comprobación (solo factorizaciones) "Ver comprobación"
│       ├── "Qué más puedes hacer" (sugerencias) · "Propiedades de la función" (plegable)
│       └── Diálogo Exportar PDF → "Guardar PDF"
│
├── Menú › Historial  (filtros, tabla, hoja lateral con Abrir en Resolver / Practicar / Exportar PDF / Cerrar, diálogo Eliminar)
├── Menú › Práctica   (Elige un ejercicio → preguntas por paso → resultado final)
├── Menú › Ejemplos   (pestañas por curso › Temas › tarjetas con Resolver / Practicar)
└── Menú › Configuración (Resolución · Apariencia · Datos · Acerca de)
```

Modos de la Pizarra:
- **Todos los pasos** (por defecto): todo visible; ‹ › desactivadas; botón dice "Paso a paso".
- **Paso a paso**: etiqueta "Paso N de M"; ‹ "Paso anterior", › "Paso siguiente"; botón dice **"Ver todo"**. Resultado y Comprobación solo aparecen en el último paso.

**Q, R y C** (`resolucion.fxml:40-63`, `ResolucionController.java:428-459, 610-618, 857`):
- Tooltips: Q "Resolver en los racionales (Q)", R "Resolver en los reales (R): radicales", C "Resolver en los complejos (C): radicales e i".
- Eligen en qué conjunto de números se **factoriza un polinomio**. Q: solo coeficientes racionales; R: admite raíces (radicales); C: admite además el número i.
- Arranca en el "Campo por defecto" de Configuración (de fábrica Q). Siempre hay uno elegido.
- Se **desactivan** (tooltip "El campo no cambia este cálculo") en límites, derivadas, integrales, extremos, etc.
- Si ya hay un resultado, cambiar el campo **vuelve a resolver solo**.
- Siguen activos en simplificar fracciones y ecuaciones, pero allí no cambian el resultado (ver "Cosas raras").
- En Exploración, "Comparación Q / R / C" muestra los tres resultados a la vez.

---

## 1.3 Controles por pantalla

### Resolver – barra y cabecera

| Texto visible | Id (lookup) | Qué hace |
|---|---|---|
| "Resolver ejercicio" + subtítulo "Escribe una expresión o una ecuación con el teclado o a mano y pulsa Resolver." | `.titulo-pantalla` | Encabezado |
| (editor de fórmulas) | `.editor-matematico` / `zonaEditor` | Donde se escribe el ejercicio |
| "⚠" | `.editor-aviso-icono` | Falta completar una casilla |
| "✎ Escrito a mano" | `#etiquetaEscritoAMano` | La fórmula vino de la pestaña Escribir |
| "Q" "R" "C" | `#selectorCampo`, `#campoQ`, `#campoR`, `#campoC` | Campo numérico |
| "Resolver" | `#botonResolver` | Resuelve; desactivado hasta que la entrada sea válida |
| (mensaje de error) | `#avisoEntrada` | Avisos de escritura |
| "Pulsa Resolver para actualizar" | `#notaActualizar` | La entrada cambió tras resolver (resultado atenuado al 40 %) |
| pestañas "Teclado" "Escribir" "Gráfica" "Análisis" "Exploración" | `#pestanaTeclado` … `#pestanaExploracion` | Cambian el panel izquierdo |
| separador arrastrable | `#division` | Ajusta el ancho izquierda/derecha |

### Teclado (`Teclas.java`, `TecladoMatematico.java`)
- Píldoras `.teclado-pildora`: "123", "f(x)", "ABC", "#&¬". Botón "•••" (tooltip "Configuración") abre Configuración.
- **123**: x y π e · □² □ⁿ √□ □/□ · < > ( ) · ans , ≤ ≥ · 0–9 × ÷ + − = · ⌫ . ← → **Enter**.
- **f(x)**: sen cos tan sen⁻¹ cos⁻¹ tan⁻¹ · ln log₁₀ log□ · e^□ ⁿ√□ |□| (desactivada "fuera del alcance") · lím d/dx ∂/∂x ∫ ∬ ∭ ∞ i lím(x,y) extr □₍□₎ ∇f Dᵤf ∂ → ("tiende a").
- **ABC**: letras, ⇧, ñ desactivada ("no es una variable de MathNova"), "αβγ", espacio.
- **αβγ**: letras griegas; vuelve con "ABC".
- **#&¬**: ∞ ≠ ≈ ≤ ≥ → ∂ ∈ [ ] ± · ∘ ° ′ ″ | ∠ { } ⟨ ⟩ ; : % ! …
- Teclas especiales: ⌫ borra · ← → mueven el cursor · Enter resuelve · ans inserta la respuesta anterior.

### Escribir (escritura a mano, `PanelEscritura.java`, `LienzoEscritura.java`)
- Indicación "Escribe con el dedo, lápiz o mouse"; lienzo vacío: "Escribe aquí tu ejercicio" / "Una sola línea, letra grande. Para borrar, tacha encima."
- Botones de ícono (tooltip): "Lápiz", "Borrador", "Deshacer", "Borrar todo"; "Reintentar" solo si algo falla.
- Reconoce solo tras ~0,7 s sin trazar y pone la fórmula en la barra. Si duda: "¿El símbolo es [x] [×] ?".

### Gráfica / Análisis / Exploración
- **Gráfica**: "Gráfica de la función"; botones "+" ("Acercar"), "−" ("Alejar"), "Restablecer vista"; ayuda "Acerca el cursor para consultar un punto · Usa +, − o Restablecer vista para ajustar el plano". Sin gráfica: "La gráfica está disponible para funciones de una variable". Dos variables: curvas de nivel (rueda = zoom, arrastrar = mover, doble clic = restablecer).
- **Análisis**: "Análisis de la función"; vacío: "Resuelve un polinomio, una fracción o una ecuación de una variable para ver su análisis."; secciones "Tabla de valores", "Signo de la función", "Conjunto solución", "Asíntotas".
- **Exploración**: "Métodos"; tarjetas con estado "Usado" / "Probar" / "No aplica" y botón "Probar" (resuelve forzando ese método); "Comparación Q / R / C" con "Ver forma exacta"/"Ocultar forma exacta". Vacío: "Resuelve un ejercicio para ver qué métodos se usaron, cuáles se pueden probar y cuáles no aplican."

### Pizarra (`PizarraResolucion.java`) — dentro de `#tarjetaProcedimiento`

| Texto | Id / clase | Qué hace |
|---|---|---|
| "Pizarra" | `.titulo-tarjeta` | Título |
| "‹" / "›" | `.pizarra-flecha-nav` | Paso anterior / siguiente (solo en Paso a paso) |
| "Todos los pasos" / "Paso N de M" | `.pizarra-etiqueta-pasos` | Indica dónde estás |
| "Paso a paso" / "Ver todo" | `.pizarra-control` (botón de modo) | Cambia de modo |
| "Explicaciones" (interruptor) | `.interruptor-explicaciones` | Muestra/oculta todas las notas (se recuerda) |
| ícono de descarga | `.pizarra-exportar` | "Exportar PDF" |
| rótulo "FACTORIZA", "SIMPLIFICA", "RESUELVE", "CALCULA EL LÍMITE", "DERIVA", "INTEGRA", "HALLA LOS EXTREMOS" | `.pizarra-rotulo` | Qué hizo MathNova |
| pasos numerados con flecha y nombre del método | `.pizarra-paso`, `.pizarra-numero`, `.pizarra-metodo` | Procedimiento |
| "?" | `.boton-nota` ("Ver explicación del paso N") | Abre la nota del paso; "Ocultar" la cierra |
| "▸ Pasos internos (N pasos)" · "Ver desarrollo"/"Ocultar desarrollo" | `.pizarra-ver-subpasos` | Detalle de un paso |
| "Resultado" | `.pizarra-resultado-bloque`, `.pizarra-resultado-formula` | Respuesta final + notas + "También se puede resolver con:" |
| "Comprobación" · "Ver comprobación"/"Ocultar comprobación" | `.pizarra-comprobacion-seccion`, `.pizarra-ver-detalle` | Verificación (solo factorizaciones) |
| texto bajo la pizarra | `#sinPasos` | Mensaje inicial / error / sin pasos |
| "Qué más puedes hacer" + "Resolver" | `#zonaSugerencias` | Ejercicios relacionados sugeridos |
| "Propiedades de la función" | `#panelPropiedades` | Plegable (dominio, etc.) |

### Exportar PDF (`ExportarPdf.java`)
Diálogo "Exportar PDF": "Se guardan los pasos con sus explicaciones, el resultado y la comprobación, en una hoja A4." · casilla "Incluir gráfica" · "Cancelar" / "Elegir dónde guardar" → ventana "Guardar PDF" (filtro "Documento PDF (*.pdf)", carpeta Documentos, nombre `mathnova-<operación>-<AAAA-MM-DD>.pdf`) → aviso "PDF guardado en <ruta>".

### Atajos de teclado
- **Enter** en el editor (o tecla Enter del teclado en pantalla) = Resolver.
- Lista de sugerencias del editor (al escribir lim, int, der, sen, raiz, frac…): ↑/↓ elegir, Enter/Tab insertar, Esc cerrar.
- **Esc** cierra la hoja y el diálogo del Historial.
- No hay atajos Ctrl+… propios.

---

## 1.4 Escritura y mensajes

### Lo que el usuario necesita para escribir
- Potencias con `^` o □²/□ⁿ; un exponente de varias partes va entre paréntesis: `e^(x+2)`.
- Se puede omitir el signo de multiplicar: `2x`, `3sen(x)`, `x(x+1)`. También `*`, ×, ·.
- Fracciones con □/□ o `/`; raíces con √□ o escribiendo `sqrt`/`raiz`.
- Funciones: sen, cos, tan, ln, log… se convierten solas al escribirlas. Sin paréntesis solo toman algo simple (`sen 2x`); si no, usa paréntesis.
- Decimales con **punto** (3.5). La coma separa datos.
- Variables: letras minúsculas (no mayúsculas). e, i y π son constantes.
- Ecuaciones: un solo "=". Plantillas lím, d/dx, ∫ (límites opcionales), ∬, ∭, extr desde la pestaña f(x) o escribiendo su nombre.
- Toda casilla vacía (□) debe llenarse (salvo los dos límites de una ∫ simple).

### Mensajes (texto exacto → causa)

**Aviso bajo la barra (`#avisoEntrada`)** — aparece tras 0,4 s sin escribir o al pulsar Resolver.
- Casillas: "Completa la casilla resaltada", "Completa el exponente", "Completa el numerador", "Completa el denominador", "Completa el radicando", "Completa el índice de la raíz", "Completa el argumento de la función", "Completa la base del logaritmo", "Completa la variable y el punto del límite", "Completa la variable del límite", "Completa el punto del límite", "Completa la función del límite", "Completa la variable de la derivada", "Completa la función que se deriva", "Completa el integrando", "Completa el límite inferior", "Completa el límite superior", "Completa el otro límite o deja los dos vacíos", "Completa los límites de la región", "Completa la casilla vacía en la posición <n>".
- Escritura: "A la raíz le falta lo de adentro: usa la tecla √ o escribe √(…) con paréntesis"; "A la integral le falta el diferencial, por ejemplo dx"; "Al límite le falta la variable y el punto: lim_{x→a}"; "MathNova integra hasta tres veces: ∫, ∬ o ∭"; "Carácter '<c>' no reconocido en la posición <n>"; "'<c>' en la posición <n> no es una letra permitida"; "Las sumatorias, productorias, integrales de línea y el valor absoluto están fuera del alcance de MathNova"; "Sobra \"<t>\" al final"; "Falta el lado derecho de la ecuación"; "Una ecuación lleva un solo \"=\""; "Se esperaba <algo> y llegó <t>"; "∞ solo puede ir como punto de un límite"; "El índice de la raíz debe ser un entero mayor que 1"; "La base del logaritmo debe ser un entero mayor que 1"; "'<c>' es una constante; no puede ser la variable de la operación"; "El orden de arriba (<a>) y el de abajo (<b>) de la derivada no coinciden"; "Un límite al infinito no lleva lado (+ o -)"; "\"<t>\" va sola: un límite, una derivada o una integral no se combina con otra expresión"; "MathNova no reconoce \\<comando>".
- No soportado: "Por ahora se factorizan polinomios y expresiones con funciones de x, se simplifican fracciones de polinomios y se resuelven ecuaciones polinómicas de una incógnita." (+ sugerencia de derivada implícita si hay 2 variables); "Esta operación todavía no se resuelve."; "Escribe el número dentro de la derivada: d/dx(3·…)."

**Bajo la Pizarra (`#sinPasos`)**: "Escribe un ejercicio y pulsa Resolver para verlo resuelto en la pizarra." (inicial) · "No se pudo resolver: <motivo>" · "Prueba otro método en Exploración o vuelve a resolver en automático." · "No se aplicó ningún método: …" (4 variantes).

**Escribir (a mano)**: "Preparando el reconocedor… (solo la primera vez)" · "Reconociendo…" · "Listo: revisa la expresión arriba y pulsa Resolver." · "Revisa la expresión: <aviso>" · "No pude reconocer tu escritura. Tus trazos siguen aquí." (+ Reintentar) · "No reconocí nada. Escribe un poco más grande y vuelve a intentarlo." · "Se conserva tu corrección del campo de arriba." · "Falta el reconocedor de escritura en <carpeta> (no está: …). Copia la carpeta \"reconocedor\" del paquete de MathNova o ejecuta reconocedor\\preparar\\preparar-reconocedor.ps1 con internet." · "No se pudo preparar el reconocedor. <motivo>".

**Exportar PDF**: "PDF guardado en <ruta>" · "No se pudo guardar el PDF: <motivo>" · "Esta resolución no tiene gráfica." · tooltip "Exportar PDF: resuelve un ejercicio primero".

**Historial**: "Todavía no hay ejercicios. Resuelve uno en Resolver y aparecerá aquí." · "No hay ejercicios que coincidan con la búsqueda." · "Cargando la hoja…" · "No se pudo abrir la hoja de este ejercicio." · "¿Eliminar este ejercicio?" / "Se borran también sus pasos. Esto no se puede deshacer."

**Práctica**: "Preparando los ejercicios…" · "No se pudieron preparar los ejercicios." · "No se puede practicar este ejercicio: <motivo>" · "Por ahora se practican factorizaciones en Q y límites de una variable." · "⚠ No aplica." + motivo · "No es el paso que toca aquí." · "Terminaste. Acertaste A de N pasos a la primera."

**Configuración**: "Guardado" · "¿Borrar todo el historial?" / "Se borran todos los ejercicios guardados, con sus pasos y sus prácticas. Los ejemplos no se tocan. Esto no se puede deshacer." · "Historial borrado" · "No se pudo borrar el historial".

**Ejemplos**: "Este tema todavía no tiene ejemplos." · tooltip "Por ahora se practican factorizaciones en Q y límites".

### Botón Resolver
Desactivado si: la barra está vacía, falta una casilla, hay un error de escritura, se está calculando, o el último intento con esa misma entrada falló. Enter con una casilla vacía no resuelve: lleva el cursor a la casilla y la marca en naranja.

---

## 1.5 Otras funciones

### Historial
- Se guarda solo todo lo resuelto (y lo practicado).
- Filtros: buscador "Buscar por expresión, operación o tema"; curso ("Todos", "Fundamentos de Matemática", "Cálculo 1", "Cálculo 2", "Cálculo 3"); fecha ("Cualquier fecha", "Hoy", "Esta semana", "Este mes"); interruptor "Solo favoritos".
- Tabla: "Ejercicio · Operación · Tema · Campo · Fecha · Acciones"; acciones de ícono: ojo "Ver la hoja con todos los pasos", estrella "Marcar como favorito"/"Quitar de favoritos", papelera "Eliminar este ejercicio". Contador "N ejercicios"; "Mostrar más" (páginas de 50).
- Hoja lateral: pasos, "Métodos que no aplicaron", "Restricciones", "Resultado"; botones "Abrir en Resolver", "Practicar", "Exportar PDF", "Cerrar" (o Esc / clic fuera).
- Tareas: buscar/filtrar, ver la hoja, volver a resolver, practicar, exportar, marcar favorito, eliminar.

### Práctica
- "Elige un ejercicio": tarjetas "<Tema> · N pasos" con "Empezar" (salen de los Ejemplos practicables: factorizaciones en Q y límites de una variable).
- En cada paso: "Paso N. ¿Qué método aplicas?" (o "¿Qué haces ahora?" en límites; "¿Terminaste?" al final) con 2–4 botones.
- Error: el botón se desactiva, "⚠ No aplica." + motivo, suma a "Intentos fallidos". Acierto: aparece el paso en la pizarra.
- Panel: "Paso k de N", barra de avance, "Aciertos a la primera", "Intentos fallidos", "Ver una pista", "Elegir otro ejercicio".
- Final: "Resultado", "Terminaste. Acertaste A de N pasos a la primera.", "Practicar otro", "Repetir este".

### Ejemplos
- Pestañas por curso → lista "Temas" (con cantidad) → tarjetas (enunciado, fórmula, nota) con "Resolver" (lleva a Resolver y lo resuelve con su campo) y "Practicar" (desactivado si no se puede).
- 33 ejemplos precargados en 4 cursos.

### Configuración (se guarda sola, aviso "Guardado")
| Opción | Valores (defecto) | Qué cambia |
|---|---|---|
| "Campo por defecto" | Q / R / C (Q) | Campo con que empieza Resolver |
| "Cómo se abre la pizarra" | "Todos los pasos" / "Paso a paso" (Todos) | Modo inicial de la Pizarra |
| "Mostrar explicaciones" | sí/no (no) | Notas visibles en cada paso |
| "Mostrar códigos de técnica" | sí/no (no) | Muestra códigos como "F16 · Ruffini" |
| "Tamaño de letra de la pizarra" | "Normal" / "Grande" / "Muy grande" (Normal) | Pizarra, hoja del Historial, Práctica y PDF |
| "Tema" | "Claro" / "Oscuro" (Claro) | Colores de toda la app |
| "Datos" | "Borrar todo el historial" (con confirmación) | Borra ejercicios guardados (no los ejemplos) |
| "Acerca de" | MathNova, Motor de resolución, Java, JavaFX, SQLite | Solo informativo |

### Exportar
Desde la Pizarra (ícono) o desde la hoja del Historial. PDF A4: encabezado "MathNova" + fecha, operación, enunciado, "Pasos" con explicaciones, "Resultado", "Restricciones", "Comprobación", gráfica opcional (solo desde Resolver), "Página i de N". Exportar también guarda el ejercicio en el Historial.

---

## Cosas raras detectadas (para FASE 8; no se corrigen)
1. Versión: Acerca de mostraría "0.0.1-SNAPSHOT"; el .exe se marca 0.2.0.
2. Q/R/C siguen activos al simplificar fracciones y resolver ecuaciones, pero no cambian el resultado.
3. El teclado ofrece ⇧ (mayúsculas), <, >, ≤, ≥, ≠, ≈, ∈, °… que el parser rechaza ("no es una letra permitida" / "no reconocido").
4. No hay indicador visible mientras calcula: el de la tarjeta Solución quedó fuera de pantalla.
5. Configuración muestra códigos internos al alumno: "Q es el de siempre (RN27)"; también en la pizarra "(RN38)".
6. Errores del Historial al leer, marcar favorito o borrar solo van al log: el usuario no ve nada.
7. Clave `animaciones` sin opción visible en Configuración.
8. La coma decimal no funciona como decimal (3,5) aunque es lo habitual en Perú.
9. La Comprobación solo aparece en factorizaciones.
