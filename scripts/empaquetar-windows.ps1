param(
    [switch]$Zip,
    [switch]$SinPruebas,
    [string]$Destino
)
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'

$repo = Split-Path $PSScriptRoot -Parent
Set-Location $repo
$dist = if ($Destino) { $Destino } else { Join-Path $repo 'dist' }
$app = Join-Path $dist 'MathNova'
$rec = Join-Path $repo 'reconocedor'

foreach ($f in @('python\python.exe', 'servidor\servidor_unimernet.py', 'modelo\unimernet_small\unimernet_small.pth')) {
    if (-not (Test-Path (Join-Path $rec $f))) { throw "Falta reconocedor\${f}: corre reconocedor\preparar\preparar-reconocedor.ps1" }
}

$jpackage = 'jpackage'
if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME 'bin\jpackage.exe'))) {
    $jpackage = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
}

$mvnArgs = @('-q', 'package')
if ($SinPruebas) { $mvnArgs += '-DskipTests' }
& (Join-Path $repo 'mvnw.cmd') @mvnArgs
if ($LASTEXITCODE -ne 0) { throw 'mvnw package falló' }
$jar = Get-ChildItem (Join-Path $repo 'target') -Filter 'mathnova-*.jar' | Where-Object { $_.Name -notlike '*.original' } | Select-Object -First 1
$entrada = Join-Path $repo 'target\jpackage-entrada'
if (Test-Path $entrada) { Remove-Item -Recurse -Force $entrada }
New-Item -ItemType Directory $entrada | Out-Null
Copy-Item $jar.FullName $entrada

if (Test-Path $app) { Remove-Item -Recurse -Force $app }
& $jpackage --type app-image --name MathNova --app-version 0.2.0 --vendor 'MathNova' `
    --input $entrada --main-jar $jar.Name --dest $dist `
    --java-options '-Xmx1024m' --java-options '-Dfile.encoding=UTF-8' `
    --java-options '-Dmathnova.reconocedor=unimernet'
if ($LASTEXITCODE -ne 0) { throw 'jpackage falló' }

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
Get-ChildItem (Join-Path $destRec 'python\Lib\site-packages\transformers\models') -Recurse -File -Filter 'convert_*' |
    Remove-Item -Force
$largo = (Get-ChildItem $app -Recurse -File | ForEach-Object { $_.FullName.Length - $app.Length } | Measure-Object -Maximum).Maximum
Write-Host "Ruta relativa más larga: $largo caracteres (la carpeta de MathNova puede medir hasta $(259 - $largo))"

$lic = Join-Path $app 'LICENCIAS'
New-Item -ItemType Directory -Force $lic | Out-Null
Copy-Item (Join-Path $repo 'reconocedor\AVISOS.txt') $lic
Copy-Item (Join-Path $rec 'LICENSE-UniMERNet.txt') (Join-Path $lic 'UniMERNet-LICENSE-Apache-2.0.txt')
Copy-Item (Join-Path $rec 'python\LICENSE.txt') (Join-Path $lic 'Python-3.11-LICENSE.txt')
Copy-Item (Join-Path $repo 'src\main\resources\editor\MATHLIVE-LICENSE.txt') $lic
Copy-Item (Join-Path $repo 'src\main\resources\editor\fonts\KATEX-LICENSE.txt') $lic
$py = Join-Path $destRec 'python\python.exe'
& $py -I (Join-Path $rec 'preparar\listar_paquetes.py') (Join-Path $lic 'paquetes-python.txt')
if ($LASTEXITCODE -ne 0) { throw 'No se pudo listar los paquetes de Python' }

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
    & tar.exe -a -c -f $zipPath -C $dist MathNova
    Write-Host ("Zip: {0}  ({1:N0} MB)" -f $zipPath, ((Get-Item $zipPath).Length / 1MB))
}
