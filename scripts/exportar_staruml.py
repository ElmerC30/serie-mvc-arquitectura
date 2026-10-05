"""Comprueba que StarUML abre cada .mdj exportando sus diagramas a PNG con la CLI de StarUML
(ventana oculta, sin interfaz grafica). Si StarUML no puede cargar un archivo, no exporta nada
y el script lo reporta como falla.

Uso: python scripts/exportar_staruml.py
"""
import os
import pathlib
import shutil
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
STARUML = pathlib.Path(r"C:\Program Files\StarUML\StarUML.exe")
ARCHIVOS = [
    ("diagramas/Ej1_Paquetes_MVC.mdj", 2),
    ("diagramas/Ej3_Dos_Vistas.mdj", 2),
    ("diagramas/Ej6_Persistencia_DAO.mdj", 2),
    ("ejercicio8/diagramas/Ej8_Biblioteca.mdj", 8),
]


def exportar(mdj, esperados):
    origen = RAIZ / mdj
    destino = origen.parent / "imagenes" / origen.stem
    shutil.rmtree(destino, ignore_errors=True)
    destino.mkdir(parents=True)
    plantilla = str(destino).replace("\\", "/") + "/<%=filenamify(element.name)%>.png"
    r = subprocess.run([str(STARUML), "image", str(origen), "-f", "png", "-o", plantilla],
                       capture_output=True, text=True, timeout=180,
                       env={**os.environ, "MSYS_NO_PATHCONV": "1"})
    pngs = sorted(destino.glob("*.png"))
    ok = len(pngs) == esperados
    print(f"{'OK  ' if ok else 'FAIL'} {mdj}: StarUML exporto {len(pngs)}/{esperados} diagramas")
    for p in pngs:
        print(f"       {p.relative_to(RAIZ)}")
    if not ok:
        print(r.stdout[-2000:], r.stderr[-2000:])
    return ok


if __name__ == "__main__":
    if not STARUML.exists():
        print("StarUML no esta instalado; se omite la verificacion por exportacion.")
        sys.exit(0)
    resultados = [exportar(m, n) for m, n in ARCHIVOS]
    sys.exit(0 if all(resultados) else 1)
