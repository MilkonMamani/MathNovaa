# Reconocimiento de escritura a mano (UniMERNet Small, local, CPU)

## Cómo funciona

```
LienzoEscritura (trazos x,y,t)  ──pausa 700 ms──▶  PanelEscritura (versión del lienzo)
   └─ ReconocedorUniMERNet: ImagenTrazos (tinta negra / fondo blanco, recortada, con margen)
        └─ ProcesoInferencia ──stdin/stdout JSON──▶ reconocedor/python/python.exe servidor/servidor_unimernet.py
                                                       (UniMERNet oficial, modelo cargado una vez)
   ◀─ LaTeX del modelo ── LatexReconocido.adaptar ──▶ editor MathLive (campo superior, "✎ Escrito a mano")
        └─ el mismo camino que el teclado: TraductorLatex → Parser → Resolver → Pizarra
```

- `mathnova.reconocedor=unimernet` (application.properties). `simulado` queda para pruebas.
- `mathnova.escritura.pausa-ms` (700): pausa sin escribir antes de reconocer.
- `mathnova.unimernet.carpeta`: carpeta del reconocedor; por defecto, junto al `.exe` o `./reconocedor`.
- Registro del proceso de inferencia: `%USERPROFILE%\.mathnova\reconocedor.log`.

## Versiones fijadas

| Componente | Versión / revisión |
|---|---|
| Modelo | `wanderkid/unimernet_small` @ `82cb85440138c2038680fca410c8f70bb7343d6d` (Apache-2.0) |
| Código | `opendatalab/UniMERNet` @ `5a2c80d96b1d2dba447ff18d873e5fb73ba03c35` (unimernet 0.2.3) |
| Python | 3.11.9 embebible (SHA-256 `009d6bf7…cfd3b`) |
| Dependencias | `preparar/requirements-lock.txt` (92 paquetes; torch 2.3.1+cpu, transformers 4.42.4) |

Sumas SHA-256 del modelo en `preparar/modelo.sha256.json` (el `.pth` coincide con el OID de Git LFS).
Los pesos y el runtime **no** se versionan (`.gitignore`).

Notas de compatibilidad encontradas al fijar versiones: `albucore` debe ser 0.0.11 (las nuevas piden
OpenCV 5); `evaluate` trae `datasets`, que exige `huggingface-hub` 0.24.x y `pyarrow` 17 con numpy 1.26;
albumentations consulta internet al importarse si no se define `NO_ALBUMENTATIONS_UPDATE=1`.

## Preparar y empaquetar (máquina de desarrollo, con internet)

```powershell
powershell -ExecutionPolicy Bypass -File reconocedor\preparar\preparar-reconocedor.ps1
powershell -ExecutionPolicy Bypass -File scripts\empaquetar-windows.ps1 [-Zip] [-SinPruebas] [-Destino <ruta corta>]
```

Resultado: `dist\MathNova\` (MathNova.exe + Java + Python + modelo + LICENCIAS + MANIFIESTO.txt).
Hay rutas internas de hasta 164 caracteres: la carpeta de MathNova debe medir ≤ 95 caracteres
(límite de 260 de Windows sin rutas largas habilitadas).

## Pruebas

- Unitarias (sin modelo): `PanelEscrituraTest`, `ResolverPizarraYEscrituraTest`, `ImagenTrazosTest`,
  `ProcesoInferenciaTest` (servidor falso), `LatexReconocidoTest` (lecturas reales copiadas),
  `ReconocedorUniMERNetContextoTest`.
- Con el modelo real (`-Dmathnova.it.unimernet=true`): `ReconocimientoRealIT` (qué lee el modelo en
  imágenes tipográficas y de trazos simulados) y `EscrituraRealIT` (pantalla real: trazos → campo →
  Resolver → Pizarra, comparado con el teclado). Dejan `target/reconocimiento/`.
- Paquete: `MathNova.exe --mathnova.autoprueba=reconocedor\pruebas --mathnova.autoprueba.salir=true`.

Las muestras de `pruebas/` y `TrazosSimulados` son trazos **simulados**, no escritura de una persona.
Para grabar muestras reales: `-Dmathnova.muestras=true` muestra la barra de muestras en "Escribir".
