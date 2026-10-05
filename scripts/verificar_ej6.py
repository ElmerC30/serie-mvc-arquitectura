"""Ejercicio 6: la aplicacion alterna entre DAO en memoria y DAO en archivo cambiando una sola linea
de App.java, sin modificar vistas ni controladores."""
import subprocess, pathlib, shutil, difflib, sys, json

RAIZ = pathlib.Path(__file__).resolve().parent.parent
OUT = RAIZ / "out" / "ej6"
shutil.rmtree(OUT, ignore_errors=True)
ok = True
reporte = []

def log(t=""):
    print(t); reporte.append(t)

diff = subprocess.run(["git", "diff", "--stat", "ej5", "ej6", "--", "src/vista", "src/controlador"],
                      cwd=RAIZ, capture_output=True, text=True).stdout
log("$ git diff --stat ej5 ej6 -- src/vista src/controlador")
log(diff.strip() or "(vacio: vistas y controladores no cambiaron)")
ok &= diff.strip() == ""

def compilar(variante, linea_dao):
    src = OUT / variante / "src"
    shutil.copytree(RAIZ / "src", src)
    app = src / "App.java"
    original = app.read_text(encoding="utf-8").splitlines()
    nuevo = [linea_dao if l.strip().endswith("// DAO") else l for l in original]
    app.write_text("\n".join(nuevo) + "\n", encoding="utf-8")
    cambios = [d for d in difflib.unified_diff(original, nuevo, lineterm="", n=0)
               if d[:1] in "+-" and not d.startswith(("+++", "---"))]
    archivos = [str(p) for p in src.rglob("*.java")]
    subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(OUT / variante / "bin")] + archivos, check=True)
    return cambios

def ejecutar(variante, entrada, cwd):
    r = subprocess.run(["java", "-cp", str(OUT / variante / "bin"), "App"], input=entrada.encode(),
                       capture_output=True, cwd=cwd)
    return r.stdout.decode()

memoria = compilar("memoria", "        ProductoDAO dao = new ProductoDAOMemoria(); // DAO")
archivo = compilar("archivo", '        ProductoDAO dao = new ProductoDAOArchivo(Paths.get("inventario.csv")); // DAO')
log("\nLineas de App.java que cambian para usar archivo:")
for d in archivo:
    log("  " + d)
ok &= len(memoria) == 0 and len(archivo) == 2   # una linea quitada + una agregada = 1 linea cambiada

trabajo = OUT / "trabajo"; trabajo.mkdir()
alta = "1\nTeclado\n5\n350\n1\nMouse\n10\n199.99\n2\nMouse\n4\n3\n4\n0\n"
consulta = "3\n4\n0\n"

s_mem_1 = ejecutar("memoria", alta, trabajo)
s_arc_1 = ejecutar("archivo", alta, trabajo)
log("\nPrimera ejecucion (alta + venta): salida memoria == salida archivo -> " + str(s_mem_1 == s_arc_1))
ok &= s_mem_1 == s_arc_1

s_mem_2 = ejecutar("memoria", consulta, trabajo)
s_arc_2 = ejecutar("archivo", consulta, trabajo)
log("\nSegunda ejecucion solo consulta (3 y 4) con MEMORIA:")
log(s_mem_2)
log("Segunda ejecucion solo consulta (3 y 4) con ARCHIVO:")
log(s_arc_2)
contenido = (trabajo / "inventario.csv").read_text(encoding="utf-8")
log("Contenido de inventario.csv:\n" + contenido)
ok &= "Teclado" not in s_mem_2 and "Teclado" in s_arc_2 and "Mouse" in s_arc_2 and "Mouse,6,199.99" in contenido

log("RESULTADO: " + ("OK - ambas implementaciones corren cambiando una sola linea" if ok else "FALLA"))
(RAIZ / "evidencias" / "ej6_persistencia.txt").write_text("\n".join(reporte), encoding="utf-8", newline="\n")
sys.exit(0 if ok else 1)
