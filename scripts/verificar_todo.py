"""Verificacion completa de la serie MVC (ejercicios 0 a 8).

Compila, ejecuta las pruebas, compara el comportamiento contra el codigo original, revisa los
git diff exigidos, prueba el cambio de persistencia de una sola linea, regenera y valida los
diagramas .mdj y guarda toda la salida en evidencias/ para el documento de respuestas.

Uso: python scripts/verificar_todo.py
"""
import difflib
import json
import pathlib
import re
import shutil
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
EVID = RAIZ / "evidencias"
CP_SEP = ";" if sys.platform.startswith("win") else ":"
LIB = str(RAIZ / "lib" / "*")
resumen = []


def correr(cmd, cwd=RAIZ, entrada=None, check=True):
    r = subprocess.run(cmd, cwd=cwd, input=entrada, capture_output=True, text=True, encoding="utf-8",
                       errors="replace")
    if check and r.returncode != 0:
        print(r.stdout, r.stderr)
        raise SystemExit(f"Fallo: {' '.join(map(str, cmd))}")
    return r


def registrar(ej, esperado, obtenido, ok):
    resumen.append({"ejercicio": ej, "esperado": esperado, "obtenido": obtenido, "ok": bool(ok)})
    print(f"[{'OK' if ok else 'FALLA'}] Ej{ej}: {obtenido}")


def compilar(src, test, salida):
    shutil.rmtree(salida, ignore_errors=True)
    fuentes = [str(p) for p in (RAIZ / src).rglob("*.java")]
    correr(["javac", "-encoding", "UTF-8", "-d", str(salida / "app")] + fuentes)
    pruebas = [str(p) for p in (RAIZ / test).rglob("*.java")]
    correr(["javac", "-encoding", "UTF-8", "-d", str(salida / "test"), "-cp",
            CP_SEP.join([str(salida / "app"), LIB])] + pruebas)


def pruebas(salida, paquetes, archivo):
    cp = CP_SEP.join([str(salida / "app"), str(salida / "test"), LIB])
    r = correr(["java", "-cp", cp, "RunTests"] + paquetes, entrada="", check=False)
    texto = "\n".join(l for l in r.stdout.splitlines() if not l.startswith("Inventario exportado"))
    (EVID / archivo).write_text(texto + "\n", encoding="utf-8", newline="\n")
    m = re.search(r"encontradas: (\d+) \| exitosas: (\d+) \| fallidas: (\d+)", texto)
    return r.returncode == 0, m.groups() if m else ("?", "?", "?")


def main():
    EVID.mkdir(exist_ok=True)

    # ------------------------------------------------------------ Ejercicio 0
    correr(["javac", "-encoding", "UTF-8", "-d", str(RAIZ / "out" / "ej0"),
            str(RAIZ / "ejercicio0" / "SistemaInventario.java")])
    r = correr(["java", "-cp", str(RAIZ / "out" / "ej0"), "SistemaInventario"],
               entrada="1\nTeclado\n5\n350\n2\nTeclado\n8\n3\n4\n0\n")
    registrar(0, "El codigo original compila y se ejecuta", "compila y corre; "
              + ("muestra 'Stock insuficiente'" if "Stock insuficiente" in r.stdout else "salida inesperada"),
              "Stock insuficiente" in r.stdout)

    # ------------------------------------------------------------ Inventario (Ej 1-6)
    salida = RAIZ / "out" / "inventario"
    compilar("src", "test", salida)

    r = correr([sys.executable, str(RAIZ / "scripts" / "verificar_ej1.py")], check=False)
    guiones = len(re.findall(r"^OK ", r.stdout, re.M))
    registrar(1, "Quien lo ejecute ve exactamente lo mismo que antes",
              f"salida identica byte a byte en {guiones} guiones de prueba", r.returncode == 0)

    prohibidos = r"System\.(out|in|err)|Scanner|javax\.swing|java\.awt|import (vista|controlador|persistencia)"
    hallados = [f"{p.name}:{i}" for p in (RAIZ / "src" / "modelo").glob("*.java")
                for i, l in enumerate(p.read_text(encoding="utf-8").splitlines(), 1) if re.search(prohibidos, l)]
    registrar(1, "El paquete modelo no contiene System.out/Scanner/javax.swing ni importa vista/controlador",
              f"{len(hallados)} coincidencias prohibidas en src/modelo", not hallados)

    ok, (n, ex, fa) = pruebas(salida, ["modelo", "controlador", "persistencia"], "pruebas_inventario.txt")
    registrar(2, "Pruebas del modelo en verde sin menu ni espera de tecla",
              f"{n} pruebas, {ex} exitosas, {fa} fallidas (stdin vacio)", ok)

    diff13 = correr(["git", "diff", "ej1", "ej3", "--", "src/modelo"]).stdout
    diff23 = correr(["git", "diff", "ej2", "ej3", "--", "src/modelo"]).stdout
    registrar(3, "git diff del paquete modelo entre Ej1/Ej2 y Ej3 vacio",
              f"git diff ej1..ej3 = {len(diff13)} bytes, ej2..ej3 = {len(diff23)} bytes", not diff13 and not diff23)

    modelo4 = "\n".join(p.read_text(encoding="utf-8") for p in (RAIZ / "src" / "modelo").glob("*.java"))
    registrar(4, "El modelo notifica sin nombrar ninguna clase de vista",
              "ninguna mencion a Vista/Controlador/Swing en src/modelo",
              not re.search(r"Vista|Controlador|Swing", modelo4))

    diff56 = correr(["git", "diff", "--stat", "ej5", "ej6", "--", "src/vista", "src/controlador"]).stdout
    r6 = correr([sys.executable, str(RAIZ / "scripts" / "verificar_ej6.py")], check=False)
    registrar(6, "Alternar memoria/archivo cambiando una sola linea; vistas y controlador sin cambios",
              f"vistas/controlador sin cambios ({len(diff56)} bytes de diff); " +
              ("una linea cambiada, ambas implementaciones corren y el archivo persiste" if r6.returncode == 0
               else "falla"), r6.returncode == 0 and not diff56)

    # ------------------------------------------------------------ Ejercicio 8
    salida8 = RAIZ / "out" / "ej8"
    shutil.rmtree(salida8, ignore_errors=True)
    fuentes = [str(p) for p in (RAIZ / "ejercicio8" / "src").rglob("*.java")]
    correr(["javac", "-encoding", "UTF-8", "-d", str(salida8 / "app")] + fuentes)
    pr = [str(p) for p in (RAIZ / "ejercicio8" / "test").rglob("*.java")]
    correr(["javac", "-encoding", "UTF-8", "-d", str(salida8 / "test"), "-cp",
            CP_SEP.join([str(salida8 / "app"), LIB])] + pr)
    ok, (n, ex, fa) = pruebas(salida8, ["modelo", "persistencia", "controlador"], "pruebas_ej8.txt")
    shutil.copy(EVID / "pruebas_ej8.txt", RAIZ / "ejercicio8" / "evidencias" / "ej8_pruebas.txt")
    registrar(8, "Reglas de negocio cubiertas por pruebas; dos vistas sincronizadas",
              f"{n} pruebas, {ex} exitosas, {fa} fallidas", ok)

    # cambio de una linea en Main.java para usar archivo
    main_java = (RAIZ / "ejercicio8" / "src" / "Main.java").read_text(encoding="utf-8").splitlines()
    nuevo = [('        RepositorioBiblioteca repositorio = new RepositorioArchivo(Paths.get("datos")); // REPOSITORIO'
              if l.strip().endswith("// REPOSITORIO") else l) for l in main_java]
    cambios = [d for d in difflib.unified_diff(main_java, nuevo, lineterm="", n=0)
               if d[:1] in "+-" and not d.startswith(("+++", "---"))]
    variante = RAIZ / "out" / "ej8_archivo"
    shutil.rmtree(variante, ignore_errors=True)
    shutil.copytree(RAIZ / "ejercicio8" / "src", variante / "src")
    (variante / "src" / "Main.java").write_text("\n".join(nuevo) + "\n", encoding="utf-8")
    correr(["javac", "-encoding", "UTF-8", "-d", str(variante / "bin")] +
           [str(p) for p in (variante / "src").rglob("*.java")])
    trabajo = variante / "trabajo"
    trabajo.mkdir()
    s1 = correr(["java", "-cp", str(variante / "bin"), "Main", "--consola"], cwd=trabajo,
                entrada="3\nL02\nS01\n0\n").stdout
    s2 = correr(["java", "-cp", str(variante / "bin"), "Main", "--consola"], cwd=trabajo, entrada="6\n0\n").stdout
    persistio = "Prestado" in s2 and (trabajo / "datos" / "prestamos.csv").exists()
    (EVID / "ej8_persistencia.txt").write_text(
        "Linea cambiada en Main.java:\n" + "\n".join(cambios) + "\n\nSegunda ejecucion (solo 'Ver libros'):\n" + s2,
        encoding="utf-8", newline="\n")
    registrar(8, "Persistencia intercambiable detras de una interfaz",
              f"{len(cambios) // 2} linea cambiada; el prestamo de la 1a ejecucion aparece en la 2a: {persistio}",
              len(cambios) == 2 and persistio)

    # ------------------------------------------------------------ diagramas
    r = correr([sys.executable, str(RAIZ / "scripts" / "generar_mdj.py")], check=False)
    (EVID / "diagramas_validacion.txt").write_text(r.stdout, encoding="utf-8", newline="\n")
    registrar("1/3/6/8", "Diagramas .mdj validos; ninguna dependencia sale del modelo",
              f"{r.stdout.count('referencias OK')} archivos validos; salen del modelo: "
              f"{'ninguna' if r.stdout.count('salen del modelo: ninguna') == 4 else 'ALGUNA'}",
              r.returncode == 0 and r.stdout.count("salen del modelo: ninguna") == 4)
    correr([sys.executable, str(RAIZ / "scripts" / "render_mdj.py")])

    (EVID / "resumen_verificacion.json").write_text(json.dumps(resumen, indent=2, ensure_ascii=False),
                                                    encoding="utf-8", newline="\n")
    fallas = [x for x in resumen if not x["ok"]]
    print(f"\n{len(resumen) - len(fallas)}/{len(resumen)} verificaciones correctas")
    sys.exit(1 if fallas else 0)


if __name__ == "__main__":
    main()
