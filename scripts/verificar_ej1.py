"""Ejercicio 1: verifica que el comportamiento observable del programa refactorizado
es identico al del codigo original (stdout byte a byte, codigo de salida y tipo de excepcion)."""
import subprocess, sys, json, pathlib

RAIZ = pathlib.Path(__file__).resolve().parent.parent
ORIGINAL = ["java", "-cp", str(RAIZ / "out" / "ej0"), "SistemaInventario"]
REFACTOR = ["java", "-cp", str(RAIZ / "out" / "app"), "App"]

GUIONES = {
    "alta_y_listado": "1\nTeclado\n5\n350\n1\nMouse\n10\n199.99\n3\n4\n0\n",
    "datos_invalidos": "1\n\n5\n10\n1\nX\n-1\n10\n1\nY\n3\n0\n1\nZ\n3\n-2\n3\n0\n",
    "venta_ok_insuficiente_inexistente": "1\nTeclado\n5\n350\n2\nTeclado\n3\n2\nTeclado\n8\n2\nMonitor\n3\n4\n0\n",
    "venta_cero_y_exacta": "1\nCable\n4\n12.5\n2\nCable\n0\n2\nCable\n4\n3\n4\n0\n",
    "nombres_duplicados": "1\nLapiz\n2\n1.5\n1\nLapiz\n7\n3\n2\nLapiz\n2\n3\n4\n0\n",
    "opcion_desconocida": "9\n-3\n3\n4\n0\n",
    "decimales_largos": "1\nA\n3\n0.1\n1\nB\n7\n0.2\n4\n3\n0\n",
    "entrada_no_numerica": "1\nTeclado\nabc\n",
    "fin_de_entrada": "3\n",
}

subprocess.run(["javac", "-encoding", "UTF-8", "-d", str(RAIZ / "out" / "ej0"),
                str(RAIZ / "ejercicio0" / "SistemaInventario.java")], check=True)

def ejecutar(cmd, entrada):
    r = subprocess.run(cmd, input=entrada.encode(), capture_output=True)
    err = r.stderr.decode(errors="replace").strip().splitlines()
    excepcion = err[0].split(":")[0].replace('Exception in thread "main" ', "") if err else ""
    return r.stdout, r.returncode, excepcion

resultados, todo_ok = [], True
for nombre, entrada in GUIONES.items():
    o = ejecutar(ORIGINAL, entrada)
    n = ejecutar(REFACTOR, entrada)
    ok = o == n
    todo_ok &= ok
    resultados.append({"guion": nombre, "identico": ok, "bytes_stdout": len(o[0]),
                       "exit": o[1], "excepcion": o[2]})
    print(f"{'OK  ' if ok else 'FAIL'} {nombre:38s} stdout={len(o[0]):4d} bytes exit={o[1]} {o[2]}")
    if not ok:
        print("  ORIGINAL:", o)
        print("  REFACTOR:", n)

ejemplo = ejecutar(REFACTOR, GUIONES["venta_ok_insuficiente_inexistente"])[0].decode()
(RAIZ / "evidencias").mkdir(exist_ok=True)
(RAIZ / "evidencias" / "ej1_equivalencia.json").write_text(
    json.dumps({"todo_ok": todo_ok, "resultados": resultados, "ejemplo_salida": ejemplo}, indent=2, ensure_ascii=False),
    encoding="utf-8")
print("RESULTADO:", "IDENTICO en todos los guiones" if todo_ok else "HAY DIFERENCIAS")
sys.exit(0 if todo_ok else 1)
