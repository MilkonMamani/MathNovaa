# Arma la carpeta portable de MathNova para Windows x64, lista para copiar por USB y usar sin internet:
#
#   dist\MathNova\
#     MathNova.exe            lanzador (jpackage) con su propio Java: no hace falta instalar Java
#     app\  runtime\          la aplicación y el runtime de Java
#     reconocedor\
#       python\               Python 3.11 embebible con torch (CPU), transformers y UniMERNet
#       servidor\             servidor_unimernet.py (proceso local, stdin/stdout)
#       modelo\unimernet_small\  pesos, tokenizador y configuración
#     LICENCIAS\              licencias y avisos de lo que se redistribuye
#     MANIFIESTO.txt          versiones, fuentes y SHA-256 del modelo
#
# Requisitos (solo en la máquina de desarrollo): JDK 21+ con jpackage en el PATH o en JAVA_HOME, y el
# reconocedor preparado con reconocedor\preparar\preparar-reconocedor.ps1.
#
# Uso (desde la raíz del repositorio):
#   powershell -ExecutionPolicy Bypass -File scripts\empaquetar-windows.ps1 [-Zip] [-SinPruebas]
param(
    [switch]$Zip,
    [switch]$SinPruebas
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$repo = Split-Path $PSScriptRoot -Parent
Set-Location $repo
$dist = Join-Path $repo 'dist'
$app = Join-Path $dist 'MathNova'
$rec = Join-Path $repo 'reconocedor'

foreach ($f in @('python\python.exe', 'servidor\servidor_unimernet.py', 'modelo\unimernet_small\unimernet_small.pth')) {
    if (-not (Test-Path (Join-Path $rec $f))) { throw "Falta reconocedor\${f}: corre reconocedor\preparar\preparar-reconocedor.ps1" }
}

$jpackage = 'jpackage'
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
    $jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
}

# 1. La aplicación (jar ejecutable de Spring Boot).
$mvnArgs = @('-q', 'package')
if ($SinPruebas) { $mvnArgs += '-DskipTests' }
& (Join-Path $repo 'mvnw.cmd') @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'mvnw package falló' }
$jar = Get-ChildItem (Join-Path $repo 'target') -Filter 'mathnova-*.jar' | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
$entrada = Join-Path $repo 'target\jpackage-entrada'
if (Test-Path $entrada) { Remove-Item -Recurse -Force $entrada }
New-Item -ItemType Directory $entrada | Out-Null
Copy-Item $jar.FullName $entrada

# 2. jpackage: carpeta de la aplicación con su runtime de Java.
if (Test-Path $app) { Remove-Item -Recurse -Force $app }
& $jpackage --type app-image --name MathNova --app-version 0.2.0 --vendor 'MathNova' `
    --input $entrada --main-jar $jar.Name --dest $dist `
    --java-options '-Xmx1024m' --java-options '-Dfile.encoding=UTF-8' `
    --java-options '-Dmathnova.reconocedor=unimernet'
if ($LASTEXITCODE -ne 0) { throw 'jpackage falló' }

# 3. El reconocedor junto al .exe (sin lo que solo sirve para compilar).
$destRec = Join-Path $app 'reconocedor'
New-Item -ItemType Directory -Force $destRec | Out-Null
robocopy (Join-Path $rec 'python') (Join-Path $destRec 'python') /E /NFL /NDL /NJH /NJS /NP `
    /XD Scripts include /XF *.lib *.pdb | Out-Null
robocopy (Join-Path $rec 'servidor') (Join-Path $destRec 'servidor') servidor_unimernet.py /NFL /NDL /NJH /NJS /NP | Out-Null
robocopy (Join-Path $rec 'modelo\unimernet_small') (Join-Path $destRec 'modelo\unimernet_small') `
    config.json preprocessor_config.json tokenizer.json tokenizer_config.json unimernet_small.pth README.md REVISION `
    /NFL /NDL /NJH /NJS /NP | Out-Null
robocopy (Join-Path $rec 'pruebas') (Join-Path $destRec 'pruebas') *.json /NFL /NDL /NJH /NJS /NP | Out-Null
if ($LASTEXITCODE -ge 8) { throw 'robocopy falló' }

# 4. Licencias y avisos.
$lic = Join-Path $app 'LICENCIAS'
New-Item -ItemType Directory -Force $lic | Out-Null
Copy-Item (Join-Path $repo 'reconocedor\AVISOS.txt') $lic
Copy-Item (Join-Path $rec 'LICENSE-UniMERNet.txt') (Join-Path $lic 'UniMERNet-LICENSE-Apache-2.0.txt')
Copy-Item (Join-Path $rec 'python\LICENSE.txt') (Join-Path $lic 'Python-3.11-LICENSE.txt')
Copy-Item (Join-Path $repo 'src\main\resources\editor\MATHLIVE-LICENSE.txt') $lic
Copy-Item (Join-Path $repo 'src\main\resources\editor\fonts\KATEX-LICENSE.txt') $lic
# Las licencias de cada paquete de Python (dist-info) quedan dentro de reconocedor\python\Lib\site-packages;
# aquí va la lista con su versión y licencia declarada.
$py = Join-Path $destRec 'python\python.exe'
& $py -I (Join-Path $rec 'preparar\listar_paquetes.py') (Join-Path $lic 'paquetes-python.txt')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo listar los paquetes de Python' }

# 5. Manifiesto: versiones, fuentes y sumas del modelo.
$m = New-Object System.Collections.Generic.List[string]
$m.Add('MathNova - paquete portable para Windows x64 (solo CPU, sin red)')
$m.Add("Generado: $(Get-Date -Format s)")
$m.Add('Modelo: wanderkid/unimernet_small @ 82cb85440138c2038680fca410c8f70bb7343d6d (Apache-2.0)')
$m.Add('  https://huggingface.co/wanderkid/unimernet_small')
$m.Add('Codigo de inferencia: opendatalab/UniMERNet @ 5a2c80d96b1d2dba447ff18d873e5fb73ba03c35 (unimernet 0.2.3, Apache-2.0)')
$m.Add('  https://github.com/opendatalab/UniMERNet')
$m.Add('Python: 3.11.9 embebible (python.org), dependencias exactas en reconocedor\preparar\requirements-lock.txt del repositorio')
$m.Add('')
$m.Add('SHA-256 de los archivos del modelo:')
Get-ChildItem (Join-Path $destRec 'modelo\unimernet_small') -File | ForEach-Object {
    $m.Add("  $((Get-FileHash -Algorithm SHA256 $_.FullName).Hash.ToLower())  $($_.Name)  ($($_.Length) bytes)")
}
$m | Set-Content -Encoding utf8 (Join-Path $app 'MANIFIESTO.txt')
Copy-Item (Join-Path $repo 'reconocedor\LEEME-ALUMNO.txt') (Join-Path $app 'LEEME.txt')

$bytes = (Get-ChildItem $app -Recurse -File | Measure-Object Length -Sum).Sum
Write-Host ("Carpeta portable: {0}  ({1:N0} MB)" -f $app, ($bytes / 1MB))

if ($Zip) {
    $zipPath = Join-Path $dist 'MathNova-portable-win64.zip'
    if (Test-Path $zipPath) { Remove-Item $zipPath }
    # tar de Windows 10+ maneja archivos de más de 2 GB mejor que Compress-Archive.
    & tar.exe -a -c -f $zipPath -C $dist MathNova
    Write-Host ("Zip: {0}  ({1:N0} MB)" -f $zipPath, ((Get-Item $zipPath).Length / 1MB))
}
