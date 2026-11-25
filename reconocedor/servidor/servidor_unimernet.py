"""Proceso local de reconocimiento de fórmulas para MathNova (UniMERNet Small, solo CPU, sin red).

Habla con MathNova por entrada/salida estándar, un objeto JSON por línea (UTF-8):

    MathNova -> servidor   {"id": 7, "imagen": "<PNG en base64>"}
    servidor -> MathNova   {"id": 7, "latex": "\\int 2 x \\, d x", "ms": 812}
                           {"id": 7, "error": "mensaje"}
    al arrancar            {"listo": true, "carga_ms": 5400, "version": "..."}   o   {"listo": false, "error": "..."}

Las peticiones se atienden de a una, en orden. El proceso termina cuando se cierra su entrada estándar
(MathNova se cerró o lo detuvo). Todo lo que las bibliotecas escriban en la salida estándar se manda a
la salida de errores, para que la salida estándar solo lleve el protocolo.

Carga el modelo con el código oficial de UniMERNet (unimernet 0.2.3, commit 5a2c80d), igual que demo.py:
Config -> tasks.setup_task -> task.build_model -> load_processor("formula_image_eval").

Uso:  python servidor_unimernet.py --modelo <carpeta con config.json, tokenizer.json y unimernet_small.pth>
      python servidor_unimernet.py --modelo <carpeta> --imagen formula.png      (una sola lectura, para probar)
"""
import argparse
import base64
import io
import json
import os
import sys
import tempfile
import time

os.environ["HF_HUB_OFFLINE"] = "1"
os.environ["TRANSFORMERS_OFFLINE"] = "1"
os.environ["HF_DATASETS_OFFLINE"] = "1"
os.environ["HF_HUB_DISABLE_TELEMETRY"] = "1"
os.environ["NO_ALBUMENTATIONS_UPDATE"] = "1"
os.environ.setdefault("TOKENIZERS_PARALLELISM", "false")
os.environ.setdefault("KMP_DUPLICATE_LIB_OK", "TRUE")
_CACHE = os.path.join(tempfile.gettempdir(), "mathnova-unimernet-cache")
os.environ["HF_HOME"] = _CACHE
os.environ["TORCH_HOME"] = _CACHE
os.environ["XDG_CACHE_HOME"] = _CACHE
os.environ["MPLCONFIGDIR"] = _CACHE

VERSION = "mathnova-unimernet 1 (unimernet 0.2.3 @5a2c80d, wanderkid/unimernet_small @82cb854)"

_PROTOCOLO = os.fdopen(os.dup(sys.stdout.fileno()), "w", encoding="utf-8", newline="\n", buffering=1)
sys.stdout = sys.stderr

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

PLANTILLA = """
model:
  arch: unimernet
  model_type: unimernet
  model_config:
    model_name: '{modelo}'
    max_seq_len: 1536
  load_pretrained: False
  load_finetuned: True
  finetuned: '{pesos}'
  tokenizer_config:
    path: '{modelo}'

datasets:
  formula_rec_eval:
    vis_processor:
      eval:
        name: "formula_image_eval"
        image_size:
          - 192
          - 672

run:
  runner: runner_iter
  task: unimernet_train
  device: "cpu"
  distributed: False
  evaluate: True
  test_splits: [ "eval" ]
  generate_cfg:
    temperature: 0.0
"""

def enviar(obj):
    _PROTOCOLO.write(json.dumps(obj, ensure_ascii=False) + "\n")
    _PROTOCOLO.flush()

class Reconocedor:
    def __init__(self, carpeta_modelo, hilos=None):
        import torch

        if hilos:
            torch.set_num_threads(hilos)
        carpeta = os.path.abspath(carpeta_modelo)
        pesos = os.path.join(carpeta, "unimernet_small.pth")
        for nombre in ("config.json", "tokenizer.json", "tokenizer_config.json", "unimernet_small.pth"):
            ruta = os.path.join(carpeta, nombre)
            if not os.path.isfile(ruta):
                raise FileNotFoundError(f"Falta {ruta}")
        with open(pesos, "rb") as f:
            if f.read(40).startswith(b"version https://git-lfs"):
                raise RuntimeError(f"{pesos} es un puntero de Git LFS, no los pesos")

        import argparse as _ap
        from unimernet.common.config import Config
        import unimernet.tasks as tasks
        from unimernet.processors import load_processor

        yaml = PLANTILLA.format(modelo=carpeta.replace("'", "''"), pesos=pesos.replace("'", "''"))
        fd, ruta_cfg = tempfile.mkstemp(prefix="mathnova-unimernet-", suffix=".yaml")
        with os.fdopen(fd, "w", encoding="utf-8") as f:
            f.write(yaml)
        try:
            cfg = Config(_ap.Namespace(cfg_path=ruta_cfg, options=None))
            task = tasks.setup_task(cfg)
            self.modelo = task.build_model(cfg).to(torch.device("cpu"))
            self.modelo.eval()
            self.procesador = load_processor(
                "formula_image_eval", cfg.config.datasets.formula_rec_eval.vis_processor.eval)
        finally:
            os.remove(ruta_cfg)
        self.torch = torch

    def reconocer(self, imagen):
        """imagen: PIL.Image. Devuelve el LaTeX tal como lo da el modelo."""
        with self.torch.inference_mode():
            x = self.procesador(imagen).unsqueeze(0)
            salida = self.modelo.generate({"image": x})
        return salida["pred_str"][0]

def main():
    p = argparse.ArgumentParser()
    p.add_argument("--modelo", required=True)
    p.add_argument("--imagen", help="Lee una imagen, imprime el LaTeX y termina")
    p.add_argument("--hilos", type=int, default=int(os.environ.get("MATHNOVA_HILOS", "0")) or None)
    a = p.parse_args()

    from PIL import Image

    inicio = time.perf_counter()
    try:
        r = Reconocedor(a.modelo, a.hilos)
    except Exception as e:
        enviar({"listo": False, "error": f"{type(e).__name__}: {e}"})
        return 2
    carga_ms = int((time.perf_counter() - inicio) * 1000)

    if a.imagen:
        img = Image.open(a.imagen).convert("RGB")
        t = time.perf_counter()
        latex = r.reconocer(img)
        enviar({"latex": latex, "carga_ms": carga_ms, "ms": int((time.perf_counter() - t) * 1000)})
        return 0

    enviar({"listo": True, "carga_ms": carga_ms, "version": VERSION})
    for linea in sys.stdin:
        linea = linea.strip()
        if not linea:
            continue
        try:
            pedido = json.loads(linea)
        except ValueError:
            enviar({"error": "JSON no válido"})
            continue
        ident = pedido.get("id")
        if pedido.get("accion") == "salir":
            break
        if pedido.get("accion") == "ping":
            enviar({"id": ident, "pong": True})
            continue
        try:
            t = time.perf_counter()
            img = Image.open(io.BytesIO(base64.b64decode(pedido["imagen"]))).convert("RGB")
            latex = r.reconocer(img)
            enviar({"id": ident, "latex": latex, "ms": int((time.perf_counter() - t) * 1000)})
        except Exception as e:
            enviar({"id": ident, "error": f"{type(e).__name__}: {e}"})
    return 0

if __name__ == "__main__":
    sys.exit(main())
