# Prepara el reconocedor de escritura en la máquina de desarrollo (con internet), en <repo>\reconocedor:
#   python\   Python 3.11.9 embebible + dependencias fijadas (requirements-lock.txt) + UniMERNet 0.2.3 @5a2c80d
#   modelo\unimernet_small\   wanderkid/unimernet_small @82cb854 (SHA-256 verificados)
# El equipo del alumno no necesita nada de esto: recibe la carpeta ya preparada dentro del paquete.
#
# Uso (PowerShell, desde la raíz del repositorio):
#   powershell -ExecutionPolicy Bypass -File reconocedor\preparar\preparar-reconocedor.ps1
param(
    [switch]$Rehacer   # borra reconocedor\python y lo vuelve a armar
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$preparar = $PSScriptRoot
$reconocedor = Split-Path $preparar -Parent
$descargas = Join-Path $preparar 'descargas'
$python = Join-Path $reconocedor 'python'
$py = Join-Path $python 'python.exe'

$PY_URL = 'https://www.python.org/ftp/python/3.11.9/python-3.11.9-embed-amd64.zip'
$PY_SHA = '009d6bf7e3b2ddca3d784fa09f90fe54336d5b60f0e0f305c37f400bf83cfd3b'
$UNIMERNET_COMMIT = '5a2c80d96b1d2dba447ff18d873e5fb73ba03c35'
$UNIMERNET_URL = "https://github.com/opendatalab/UniMERNet/archive/$UNIMERNET_COMMIT.zip"

function Bajar($url, $destino) {
    if (-not (Test-Path $destino)) {
        Write-Host "Descargando $url"
        Invoke-WebRequest -Uri $url -OutFile $destino -UseBasicParsing
    }
}

function Sha256($ruta) { (Get-FileHash -Algorithm SHA256 $ruta).Hash.ToLower() }

New-Item -ItemType Directory -Force $descargas | Out-Null

if ($Rehacer -and (Test-Path $python)) { Remove-Item -Recurse -Force $python }

if (-not (Test-Path $py)) {
    $zip = Join-Path $descargas 'python-3.11.9-embed-amd64.zip'
    Bajar $PY_URL $zip
    if ((Sha256 $zip) -ne $PY_SHA) { throw "SHA-256 distinto en $zip" }
    Expand-Archive -Path $zip -DestinationPath $python -Force
    # site-packages habilitado; nada más en sys.path (ni PYTHONPATH ni la carpeta del usuario).
    Set-Content -Path (Join-Path $python 'python311._pth') -Encoding ascii -Value "python311.zip`r`n.`r`nLib\site-packages`r`nimport site"
    $getpip = Join-Path $descargas 'get-pip.py'
    Bajar 'https://bootstrap.pypa.io/get-pip.py' $getpip
    & $py $getpip --no-warn-script-location
    if ($LASTEXITCODE -ne 0) { throw 'get-pip falló' }
}

# Dependencias exactas (sin resolver nada nuevo: --no-deps sobre el lock completo).
& $py -m pip install --no-warn-script-location --no-deps -r (Join-Path $preparar 'requirements-lock.txt')
if ($LASTEXITCODE -ne 0) { throw 'pip install del lock falló' }

# Código oficial de UniMERNet en el commit fijado (el pyproject usa poetry-core como backend).
$uz = Join-Path $descargas "UniMERNet-$UNIMERNET_COMMIT.zip"
Bajar $UNIMERNET_URL $uz
$fuente = Join-Path $descargas "UniMERNet-$UNIMERNET_COMMIT"
if (-not (Test-Path $fuente)) { Expand-Archive -Path $uz -DestinationPath $descargas -Force }
& $py -m pip install --no-warn-script-location 'poetry-core==1.9.1'
& $py -m pip install --no-warn-script-location --no-deps --no-build-isolation $fuente
if ($LASTEXITCODE -ne 0) { throw 'No se pudo instalar UniMERNet' }
& $py -m pip uninstall -y poetry-core
Copy-Item (Join-Path $fuente 'LICENSE') (Join-Path $reconocedor 'LICENSE-UniMERNet.txt') -Force

# Modelo (revisión y sumas fijas).
& $py -I (Join-Path $preparar 'descargar_modelo.py') (Join-Path $reconocedor 'modelo\unimernet_small')
if ($LASTEXITCODE -ne 0) { throw 'La descarga o verificación del modelo falló' }

# Prueba: una lectura real sin red.
$img = Join-Path $fuente 'asset\test_imgs\0000001.png'
& $py -I (Join-Path $reconocedor 'servidor\servidor_unimernet.py') --modelo (Join-Path $reconocedor 'modelo\unimernet_small') --imagen $img
if ($LASTEXITCODE -ne 0) { throw 'La inferencia de prueba falló' }
Write-Host 'Reconocedor listo.'
