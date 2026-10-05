"""Dibuja una vista previa PNG de cada diagrama de un archivo .mdj (sin StarUML).

Lee las vistas del archivo (posiciones, compartimentos, aristas, lineas de vida y mensajes) y las
pinta con Pillow. Se usa para ilustrar el documento DOCX; los .mdj se abren directamente en StarUML.
"""
import json
import math
import pathlib
import re

from PIL import Image, ImageDraw, ImageFont

RAIZ = pathlib.Path(__file__).resolve().parent.parent
FUENTE = "C:/Windows/Fonts/arial.ttf"
FUENTE_B = "C:/Windows/Fonts/arialbd.ttf"
ESCALA = 1.6


def cargar(ruta):
    datos = json.loads(pathlib.Path(ruta).read_text(encoding="utf-8"))
    ids = {}

    def rec(o):
        if isinstance(o, dict):
            if "_id" in o:
                ids[o["_id"]] = o
            for v in o.values():
                rec(v)
        elif isinstance(o, list):
            for v in o:
                rec(v)
    rec(datos)
    diagramas = [o for o in ids.values() if o.get("_type", "").endswith("Diagram")]
    return ids, diagramas


def fuente(tam, negrita=False):
    return ImageFont.truetype(FUENTE_B if negrita else FUENTE, int(tam * ESCALA))


def s(v):
    return int(v * ESCALA)


def borde(rect, hacia):
    """Punto donde la linea del centro de rect hacia `hacia` cruza el borde del rectangulo."""
    x, y, w, h = rect
    cx, cy = x + w / 2, y + h / 2
    dx, dy = hacia[0] - cx, hacia[1] - cy
    if dx == 0 and dy == 0:
        return cx, cy
    t = min((w / 2) / abs(dx) if dx else 1e9, (h / 2) / abs(dy) if dy else 1e9)
    return cx + dx * t, cy + dy * t


def flecha(d, p1, p2, tipo):
    """tipo: dep (punteada, punta abierta), gen (solida, triangulo), real (punteada, triangulo)."""
    x1, y1, x2, y2 = map(s, (*p1, *p2))
    largo = math.hypot(x2 - x1, y2 - y1) or 1
    ux, uy = (x2 - x1) / largo, (y2 - y1) / largo
    punteada = tipo in ("dep", "real", "reply")
    tam = s(11)
    fin = (x2 - ux * tam, y2 - uy * tam) if tipo in ("gen", "real") else (x2, y2)
    if punteada:
        paso, pos = s(6), 0.0
        total = math.hypot(fin[0] - x1, fin[1] - y1)
        while pos < total:
            a, b = pos, min(pos + paso, total)
            d.line([(x1 + ux * a, y1 + uy * a), (x1 + ux * b, y1 + uy * b)], fill="black", width=2)
            pos += paso * 2
    else:
        d.line([(x1, y1), fin], fill="black", width=2)
    px, py = -uy, ux
    izq = (x2 - ux * tam + px * tam * 0.55, y2 - uy * tam + py * tam * 0.55)
    der = (x2 - ux * tam - px * tam * 0.55, y2 - uy * tam - py * tam * 0.55)
    if tipo in ("gen", "real"):
        d.polygon([(x2, y2), izq, der], outline="black", fill="white", width=2)
    elif tipo == "sync":
        d.polygon([(x2, y2), izq, der], fill="black")
    else:
        d.line([izq, (x2, y2), der], fill="black", width=2)


def texto_modelo(ids, vista):
    m = ids.get(vista.get("model", {}).get("$ref", ""), {})
    return m


def pintar_clasificador(d, ids, v):
    x, y, w = v["left"], v["top"], v["width"]
    elem = texto_modelo(ids, v)
    sub = {ids[r["$ref"]]["_type"]: ids[r["$ref"]] for r in
           [v.get("nameCompartment"), v.get("attributeCompartment"), v.get("operationCompartment")] if r}
    nc = sub["UMLNameCompartmentView"]
    etiquetas = [ids[x_["_id"]] for x_ in nc["subViews"]]
    lineas_nombre = []
    if etiquetas[0].get("visible") and etiquetas[0].get("text"):
        lineas_nombre.append((etiquetas[0]["text"], False, 11))
    lineas_nombre.append((elem.get("name", ""), True, 13))
    if etiquetas[2].get("visible") and etiquetas[2].get("text"):
        lineas_nombre.append((etiquetas[2]["text"], False, 11))
    attrs = [a["text"] for a in sub.get("UMLAttributeCompartmentView", {}).get("subViews", [])]
    ops = [o["text"] for o in sub.get("UMLOperationCompartmentView", {}).get("subViews", [])]
    f, fb = fuente(11.5), fuente(13, True)
    ancho = max([w] + [d.textlength(t, font=fuente(tam, b)) / ESCALA + 16 for t, b, tam in lineas_nombre]
                + [d.textlength(t, font=f) / ESCALA + 14 for t in attrs + ops])
    alto_nombre = 8 + 16 * len(lineas_nombre)
    alto = alto_nombre + 8 + 15 * len(attrs) + 8 + 15 * len(ops) + 4
    d.rectangle([s(x + 4), s(y + 4), s(x + ancho + 4), s(y + alto + 4)], fill="#d9d9d9")
    d.rectangle([s(x), s(y), s(x + ancho), s(y + alto)], fill="#fffbe6" if "Interface" in v["_type"]
                else "white", outline="black", width=2)
    cy = y + 4
    for t, b, tam in lineas_nombre:
        fn = fuente(tam, b)
        d.text((s(x + ancho / 2) - d.textlength(t, font=fn) / 2, s(cy)), t, font=fn, fill="black")
        cy += 16
    cy = y + alto_nombre
    d.line([s(x), s(cy), s(x + ancho), s(cy)], fill="black", width=1)
    cy += 4
    for t in attrs:
        d.text((s(x + 6), s(cy)), t, font=f, fill="black")
        cy += 15
    cy += 4
    d.line([s(x), s(cy), s(x + ancho), s(cy)], fill="black", width=1)
    cy += 4
    for t in ops:
        d.text((s(x + 6), s(cy)), t, font=f, fill="black")
        cy += 15
    return (x, y, ancho, alto)


def pintar_paquete(d, ids, v):
    x, y, w, h = v["left"], v["top"], v["width"], v["height"]
    nombre = texto_modelo(ids, v).get("name", "")
    d.rectangle([s(x), s(y), s(x + 70), s(y + 14)], fill="#eef3fb", outline="black", width=2)
    d.rectangle([s(x), s(y + 14), s(x + w), s(y + h)], fill="#eef3fb", outline="black", width=2)
    fb = fuente(14, True)
    d.text((s(x + w / 2) - d.textlength(nombre, font=fb) / 2, s(y + 14 + (h - 14) / 2 - 9)), nombre, font=fb,
           fill="black")
    return (x, y, w, h)


def pintar_diagrama(ids, dg, destino):
    vistas = dg["ownedViews"]
    ancho = max([v.get("left", 0) + v.get("width", 0) for v in vistas] + [400]) + 600
    alto = max([v.get("top", 0) + v.get("height", 0) for v in vistas] + [300]) + 400
    img = Image.new("RGB", (s(ancho), s(alto)), "white")
    d = ImageDraw.Draw(img)
    rects = {}
    for v in vistas:
        if v["_type"] in ("UMLClassView", "UMLInterfaceView"):
            rects[v["_id"]] = pintar_clasificador(d, ids, v)
        elif v["_type"] == "UMLPackageView":
            rects[v["_id"]] = pintar_paquete(d, ids, v)
    tipos = {"UMLDependencyView": "dep", "UMLGeneralizationView": "gen", "UMLInterfaceRealizationView": "real"}
    for v in vistas:
        if v["_type"] in tipos:
            ra, rb = rects[v["tail"]["$ref"]], rects[v["head"]["$ref"]]
            ca, cb = (ra[0] + ra[2] / 2, ra[1] + ra[3] / 2), (rb[0] + rb[2] / 2, rb[1] + rb[3] / 2)
            flecha(d, borde(ra, cb), borde(rb, ca), tipos[v["_type"]])
    if dg["_type"] == "UMLSequenceDiagram":
        pintar_secuencia(d, ids, dg)
    # recorte al contenido
    caja = Image.eval(img, lambda p: 255 - p).getbbox()
    if caja:
        img = img.crop((max(0, caja[0] - 20), max(0, caja[1] - 20), caja[2] + 20, caja[3] + 20))
    destino.parent.mkdir(parents=True, exist_ok=True)
    img.save(destino)
    return destino


def pintar_secuencia(d, ids, dg):
    f, fb = fuente(11.5), fuente(12, True)
    lineas = {}
    for v in dg["ownedViews"]:
        if v["_type"] == "UMLFrameView":
            x, y, w, h = v["left"], v["top"], v["width"], v["height"]
            d.rectangle([s(x), s(y), s(x + w), s(y + h)], outline="black", width=2)
            titulo = "sd " + dg["name"]
            d.text((s(x + 6), s(y + 4)), titulo, font=fb, fill="black")
            d.line([s(x), s(y + 22), s(x + 12 + d.textlength(titulo, font=fb) / ESCALA), s(y + 22),
                    s(x + 22 + d.textlength(titulo, font=fb) / ESCALA), s(y + 12)], fill="black", width=1)
        if v["_type"] == "UMLSeqLifelineView":
            x, y, w, h = v["left"], v["top"], v["width"], v["height"]
            ll = texto_modelo(ids, v)
            attr = ids[ll["represent"]["$ref"]]
            tipo = attr["type"]
            tipo = ids[tipo["$ref"]]["name"] if isinstance(tipo, dict) else tipo
            nombre = f"{ll['name']}: {tipo}"
            fn = fb
            while d.textlength(nombre, font=fn) / ESCALA > w - 6 and fn.size > 10:
                fn = ImageFont.truetype(FUENTE_B, fn.size - 1)
            d.rectangle([s(x), s(y), s(x + w), s(y + 40)], fill="white", outline="black", width=2)
            d.text((s(x + w / 2) - d.textlength(nombre, font=fn) / 2, s(y + 12)), nombre, font=fn, fill="black")
            cx = x + w / 2
            yy = y + 40
            while yy < y + h:
                d.line([s(cx), s(yy), s(cx), s(min(yy + 5, y + h))], fill="black", width=1)
                yy += 10
            lineas[ids[v["linePart"]["$ref"]]["_id"]] = cx
    n = 0
    for v in dg["ownedViews"]:
        if v["_type"] != "UMLSeqMessageView":
            continue
        n += 1
        m = texto_modelo(ids, v)
        pts = [tuple(map(float, p.split(":"))) for p in v["points"].split(";")]
        y = pts[0][1]
        xa, xb = lineas[v["tail"]["$ref"]], lineas[v["head"]["$ref"]]
        etiqueta = f"{n}: {m['name']}"
        if m["messageSort"] != "reply":
            d.rectangle([s(xb - 6), s(y), s(xb + 6), s(y + 26)], fill="#f2f2f2", outline="black", width=1)
        if xa == xb:
            d.line([s(xa + 6), s(y - 8), s(xa + 40), s(y - 8), s(xa + 40), s(y + 8)], fill="black", width=2)
            flecha(d, (xa + 40, y + 8), (xa + 7, y + 8), "sync")
            d.text((s(xa + 46), s(y - 12)), etiqueta, font=f, fill="black")
        else:
            tipo = {"reply": "reply", "asynchSignal": "async"}.get(m["messageSort"], "sync")
            fin = xb - 6 if xb > xa else xb + 6
            flecha(d, (xa, y), (fin, y), tipo)
            tx = min(xa, xb) + 10
            d.text((s(tx), s(y - 17)), etiqueta, font=f, fill="black")


def renderizar(ruta_mdj, carpeta):
    ids, diagramas = cargar(ruta_mdj)
    salidas = []
    for dg in diagramas:
        nombre = re.sub(r"[^\w\- ]", "", dg["name"]).strip().replace(" ", "_")
        salidas.append(pintar_diagrama(ids, dg, carpeta / f"{nombre}.png"))
    return salidas


if __name__ == "__main__":
    for mdj in ["diagramas/Ej1_Paquetes_MVC.mdj", "diagramas/Ej3_Dos_Vistas.mdj",
                "diagramas/Ej6_Persistencia_DAO.mdj", "ejercicio8/diagramas/Ej8_Biblioteca.mdj"]:
        ruta = RAIZ / mdj
        for p in renderizar(ruta, ruta.parent / "vista_previa" / ruta.stem):
            print(p.relative_to(RAIZ))
