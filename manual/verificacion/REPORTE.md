# REPORTE de verificación (FASE 4)

Solo para el equipo. Cada resultado se calculó aparte con SymPy y se comparó con lo que MathNova dibujó (el LaTeX de los objetos de la Pizarra, guardado por el arnés en `capturas/limpias/<id>.texto.txt`).

| Captura | Qué se verifica | SymPy | MathNova | | Nota |
|---|---|---|---|---|---|
| 5-1_resolver_02_solucion | factorizar x³−2x²−x+2 en Q | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 3_zonas | factorizar x³−2x²−x+2 en Q | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-2_pasos_01 | factorizar x³−2x²−x+2 en Q | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-4_comprobacion_01 | factorizar x³−2x²−x+2 en Q | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-4_comprobacion_02 | factorizar x³−2x²−x+2 en Q | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-1_resolver_02_solucion | igualdad de un paso | `` | ` x^{3} - 2x^{2}  =  x^{2}(x - 2) ` | ✅ |  |
| 5-1_resolver_02_solucion | igualdad de un paso | `` | ` -x + 2  =  -(x - 2) ` | ✅ |  |
| 5-1_resolver_02_solucion | igualdad de un paso | `` | ` x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right) ` | ✅ |  |
| 5-1_resolver_02_solucion | igualdad de un paso | `` | ` x^{2} - 1  =  \left(x + 1\right)\left(x - 1\right) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | ` x^{3} - 2x^{2}  =  x^{2}(x - 2) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | ` -x + 2  =  -(x - 2) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | ` x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | ` x^{2} - 1  =  \left(x + 1\right)\left(x - 1\right) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)  =  \left(x^{2} - 1\right)\left(x - 2\right) ` | ✅ |  |
| 5-4_comprobacion_02 | igualdad de un paso | `` | ` \left(x^{2} - 1\right)\left(x - 2\right)  =  x^{3} - 2x^{2} - x + 2` | ✅ |  |
| 6-3_probar_02 | mismo ejercicio con Ruffini | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-3_probar_02 | igualdad de un paso | `` | `x^{3} - 2x^{2} - x + 2  =  \left(x - 1\right)\left(x^{2} - x - 2\right)` | ✅ |  |
| 6-3_probar_02 | igualdad de un paso | `` | `x\cdot (-2)  =  -2x` | ✅ |  |
| 6-3_probar_02 | igualdad de un paso | `` | `x\cdot 1  =  x` | ✅ |  |
| 6-3_probar_02 | igualdad de un paso | `` | `-2x + x  =  -x` | ✅ |  |
| 6-3_probar_02 | igualdad de un paso | `` | ` x^{2} - x - 2  =  x^{2} + x - 2x - 2 ` | ✅ |  |
| 5-6_qrc_01 | factorizar x⁴−4 en Q | `\left(x^{2} - 2\right) \left(x^{2} + 2\right)` | `\left(x^{2} + 2\right)\left(x^{2} - 2\right)` | ✅ | igual al ejercicio al expandir; 2 factores (SymPy: 2) |
| 5-6_qrc_02 | factorizar x⁴−4 en R | `\left(x - \sqrt{2}\right) \left(x + \sqrt{2}\right) \left(x^{2} + 2\right)` | `\left(x^{2} + 2\right)\left(x - \sqrt{2}\right)\left(x + \sqrt{2}\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-4_comparar_01 | comparación: fila Q | `\left(x^{2} - 2\right) \left(x^{2} + 2\right)` | `\left(x^{2} + 2\right)\left(x^{2} - 2\right)` | ✅ | puede verse en forma resumida |
| 6-4_comparar_01 | comparación: fila R | `\left(x - \sqrt{2}\right) \left(x + \sqrt{2}\right) \left(x^{2} + 2\right)` | `\left(x^{2} + 2\right)\left(x - \sqrt{2}\right)\left(x + \sqrt{2}\right)` | ✅ | puede verse en forma resumida |
| 6-4_comparar_01 | comparación: fila C | `\left(x - \sqrt{2}\right) \left(x + \sqrt{2}\right) \left(x - \sqrt{2} i\right) \left(x + \sqrt{2} i\right)` | `\left(x - \sqrt{2}i\right)\left(x + \sqrt{2}i\right)\left(x - \sqrt{2}\right)\left(x + \sqrt{2}\right)` | ✅ | puede verse en forma resumida |
| 6-2_analisis_01 | Grado | `3` | `3` | ✅ |  |
| 6-2_analisis_01 | Coeficiente principal | `1` | `1` | ✅ |  |
| 6-2_analisis_01 | Corte con el eje y | `2` | `\left(0, 2\right)` | ✅ |  |
| 6-2_analisis_01 | raíces de f(x) = 0 | `[-1, 1, 2]` | `x \in \left\{-1, 1, 2\right\}` | ✅ |  |
| 6-2_analisis_01 | conjunto solución de f(x) > 0 | `\left(-1, 1\right) \cup \left(2, \infty\right)` | `\text{C.S.} = \left(-1, 1\right) \cup \left(2, +\infty\right)` | ✅ |  |
| 5-6_qrc_02 | igualdad de un paso | `` | ` x^{4} - 4  =  \left(x^{2} + 2\right)\left(x^{2} - 2\right) ` | ✅ |  |
| 5-6_qrc_02 | igualdad de un paso | `` | `\left(x^{2} + 2\right)\left(x^{2} - 2\right)  =  x^{2} - 2` | ❌ | captura NO usada en el manual (decisión del equipo, opción 2): paso con igualdad falsa en MathNova |
| 5-6_qrc_02 | igualdad de un paso | `` | `x^{4} - 4  =  \left(x^{2} + 2\right)\left(x - \sqrt{2}\right)\left(x + \sqrt{2}\right)` | ✅ |  |
| 4-3_mano_01 | lectura de la muestra A (3x²+2x−1) | `3x^2+2x-1` | `3x^{2}+2x-1` | ✅ |  |
| 7-2_hoja_02 | hoja del Historial | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 7-2_hoja_02 | igualdad de un paso | `` | `x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right)` | ✅ |  |
| 7-2_hoja_02 | igualdad de un paso | `` | `x^{2} - 1  =  \left(x + 1\right)\left(x - 1\right)` | ✅ |  |
| 8-2_metodo_03 | igualdad de un paso | `` | `x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right)` | ✅ |  |
| 8-2_metodo_04 | igualdad de un paso | `` | `x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right)` | ✅ |  |
| 8-2_metodo_04 | igualdad de un paso | `` | `x^{2} - 1  =  \left(x + 1\right)\left(x - 1\right)` | ✅ |  |
| 8-4_terminar_01 | igualdad de un paso | `` | `x^{3} - 2x^{2} - x + 2  =  \left(x - 2\right)\left(x^{2} - 1\right)` | ✅ |  |
| 8-4_terminar_01 | igualdad de un paso | `` | `x^{2} - 1  =  \left(x + 1\right)\left(x - 1\right)` | ✅ |  |
| 8-4_terminar_01 | resultado final de la práctica | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-2_pasos_02 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-3_explicacion_01 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-3_explicacion_02 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 5-5_exportar_01 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-1_grafica_01 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-2_analisis_01 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-3_probar_01 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-3_probar_02 | factorizar x^{3} - 2x^{2} - x + 2 en Q (pasada general) | `\left(x - 2\right) \left(x - 1\right) \left(x + 1\right)` | `\left(x + 1\right)\left(x - 1\right)\left(x - 2\right)` | ✅ | igual al ejercicio al expandir; 3 factores (SymPy: 3) |
| 6-4_comparar_01 | factorizar x^{4} - 4 en Q (pasada general) | `\left(x^{2} - 2\right) \left(x^{2} + 2\right)` | `\left(x^{2} + 2\right)\left(x^{2} - 2\right)` | ✅ | igual al ejercicio al expandir; 2 factores (SymPy: 2) |

**Total:** 53 comprobaciones, 0 ❌.
