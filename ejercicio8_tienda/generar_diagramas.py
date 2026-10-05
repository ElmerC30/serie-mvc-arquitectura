"""Genera diagramas/Ej8_TiendaInstrumentos.mdj (formato nativo de StarUML) a partir del código Java.

Contenido del archivo:
  - Diagrama de paquetes (dependencias reales entre paquetes, leídas de los imports/usos).
  - Un diagrama de clases por paquete (modelo, vista, controlador, persistencia y raíz).
  - Diagrama de secuencia "Cobrar un kit en el mostrador".

El script valida la integridad del archivo (_id únicos, todas las $ref resuelven, cada _parent apunta
a su contenedor). Con --png, y si StarUML está instalado, además lo abre con la CLI de StarUML y
exporta cada diagrama a diagramas/imagenes/: si StarUML no pudiera leer el archivo, no exportaría nada.

Uso:  python generar_diagramas.py          (sólo genera y valida)
      python generar_diagramas.py --png    (genera, valida y exporta PNG con StarUML)
"""
import base64
import json
import os
import pathlib
import re
import shutil
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parent
SRC = RAIZ / "src"
DESTINO = RAIZ / "diagramas" / "Ej8_TiendaInstrumentos.mdj"
STARUML = pathlib.Path(r"C:\Program Files\StarUML\StarUML.exe")
FUENTE = "Arial;13;0"
FUENTE_NEGRITA = "Arial;13;1"


# ============================================================================ lectura del código Java

def sin_comentarios(codigo):
    codigo = re.sub(r"/\*.*?\*/", "", codigo, flags=re.S)
    codigo = re.sub(r"//[^\n]*", "", codigo)
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', codigo)


def unir_firmas(cuerpo):
    """Une en una sola línea las firmas de miembros que ocupan varias líneas."""
    salida, pendiente = [], None
    for linea in cuerpo.splitlines():
        if pendiente is not None:
            pendiente += " " + linea.strip()
            if pendiente.count("(") <= pendiente.count(")"):
                salida.append(pendiente)
                pendiente = None
            continue
        if linea.startswith("    ") and not linea.startswith("     ") and linea.count("(") > linea.count(")"):
            pendiente = linea
        else:
            salida.append(linea)
    return "\n".join(salida)


MODIFICADORES = r"(?:(?:public|private|protected|static|final|abstract|synchronized|volatile|transient|default)\s+)*"
RE_TIPO = re.compile(r"^(?:public\s+)?(?:abstract\s+|final\s+)*(class|interface|enum|record)\s+(\w+)"
                     r"(?:\(([^)]*)\))?(?:\s+extends\s+([\w.<>]+))?(?:\s+implements\s+([\w.<>,\s]+?))?\s*\{", re.M)
RE_CAMPO = re.compile(r"^ {4}(?:@\w+\s+)?(" + MODIFICADORES + r")([\w<>\[\],.? ]+?)\s+(\w+)\s*(?:=|;)")
RE_METODO = re.compile(r"^ {4}(?:@\w+(?:\([^)]*\))?\s+)*(" + MODIFICADORES + r")(?:([\w<>\[\],.? ]+?)\s+)?(\w+)\s*\(([^)]*)\)?")
LIT = r"[A-Z][A-Z0-9_]*\s*(?:\([^)]*\))?"
RE_LITERALES = re.compile(r"^ {4}" + LIT + r"(?:\s*,\s*" + LIT + r")*\s*[,;]?\s*$")
PALABRAS = ("if", "for", "while", "switch", "return", "new", "catch", "synchronized", "super", "this")


def visibilidad(mods, es_interfaz):
    if "private" in mods:
        return "private"
    if "protected" in mods:
        return "protected"
    if "public" in mods or es_interfaz:
        return "public"
    return "package"


def parametros(texto):
    lista = []
    for p in [x.strip() for x in (texto or "").split(",") if x.strip()]:
        partes = re.sub(r"^final\s+", "", p).rsplit(" ", 1)
        lista.append({"nombre": partes[-1], "tipo": partes[0] if len(partes) > 1 else ""})
    return lista


def parsear(codigo):
    paquete = re.search(r"^package\s+([\w.]+);", codigo, re.M)
    paquete = paquete.group(1) if paquete else "(raiz)"
    limpio = sin_comentarios(codigo)
    m = RE_TIPO.search(limpio)
    if not m:
        return None
    tipo, nombre, componentes, extiende, implementa = m.groups()
    es_interfaz = tipo == "interface"
    cuerpo = unir_firmas(limpio[m.end():])
    atributos, operaciones = [], []
    if tipo == "record":
        for p in parametros(componentes):
            atributos.append({"nombre": p["nombre"], "tipo": p["tipo"], "vis": "private",
                              "static": False, "final": True})
    for linea in cuerpo.splitlines():
        if not linea.startswith("    ") or linea.startswith("     "):
            continue
        if tipo == "enum":
            if RE_LITERALES.match(linea):
                for nombre_lit in re.findall(r"([A-Z][A-Z0-9_]*)\s*(?:\([^)]*\))?", linea):
                    atributos.append({"nombre": nombre_lit, "tipo": "", "vis": "public", "static": True,
                                      "final": True, "literal": True})
                continue
        if "(" not in linea.split("=")[0]:
            c = RE_CAMPO.match(linea)
            if c and not re.search(r"\b(class|interface|enum|record|return|new)\b", c.group(2)):
                mods = c.group(1)
                atributos.append({"nombre": c.group(3), "tipo": c.group(2).strip(),
                                  "vis": visibilidad(mods, es_interfaz), "static": "static" in mods,
                                  "final": "final" in mods})
                continue
        mt = RE_METODO.match(linea)
        if not mt:
            continue
        mods, retorno, nom, params = mt.groups()
        if nom in PALABRAS or (retorno and retorno.strip() in ("new", "return", "throw")):
            continue
        constructor = nom == nombre
        if not constructor and not retorno:
            continue
        vis = visibilidad(mods, es_interfaz)
        if vis not in ("public", "protected"):
            continue
        operaciones.append({"nombre": nom, "params": parametros(params),
                            "retorno": None if constructor else retorno.strip(),
                            "vis": vis, "static": "static" in mods, "constructor": constructor})
    return {"nombre": nombre, "paquete": paquete, "tipo": tipo, "interfaz": es_interfaz,
            "extiende": extiende, "implementa": [x.strip() for x in implementa.split(",")] if implementa else [],
            "atributos": atributos, "operaciones": operaciones, "cuerpo": cuerpo}


def leer_proyecto():
    clases = {}
    for ruta in sorted(SRC.rglob("*.java")):
        c = parsear(ruta.read_text(encoding="utf-8"))
        if c:
            clases[c["nombre"]] = c
    for c in clases.values():
        c["usa"] = {otro for otro in clases
                    if otro != c["nombre"] and re.search(r"\b" + re.escape(otro) + r"\b", c["cuerpo"])}
    return clases


# ============================================================================ construcción del .mdj

def ref(obj):
    return {"$ref": obj["_id"]}


def centro(v):
    return v["left"] + v["width"] // 2, v["top"] + v["height"] // 2


SIMBOLO = {"public": "+", "private": "-", "protected": "#", "package": "~"}


def texto_atributo(a):
    if a.get("literal"):
        return a["nombre"]
    return f"{SIMBOLO[a['vis']]}{a['nombre']}: {a['tipo']}"


def texto_operacion(o):
    params = ", ".join(f"{p['nombre']}: {p['tipo']}" for p in o["params"])
    ret = f": {o['retorno']}" if o["retorno"] and o["retorno"] != "void" else ""
    return f"{SIMBOLO[o['vis']]}{o['nombre']}({params}){ret}"


def estereotipo(c):
    return {"interface": "«interface»", "enum": "«enumeration»", "record": "«record»"}.get(c["tipo"], "")


def tamano(c, mostrar_paquete):
    lineas = [c["nombre"]] + [texto_atributo(a) for a in c["atributos"]] + \
             [texto_operacion(o) for o in c["operaciones"]]
    if mostrar_paquete:
        lineas.append(f"(from {c['paquete']})")
    ancho = max(150, int(max(len(l) for l in lineas) * 7.0) + 20)
    alto = 30 + (15 if estereotipo(c) else 0) + (15 if mostrar_paquete else 0) \
        + 10 + 15 * len(c["atributos"]) + 10 + 15 * len(c["operaciones"])
    return ancho, alto


class Mdj:
    def __init__(self, nombre_proyecto):
        self.contador = 0
        self.proyecto = self.nuevo("Project", None, name=nombre_proyecto, ownedElements=[],
                                   author="Jesús Elmer Cantúa Domínguez")
        self.modelo = self.nuevo("UMLModel", self.proyecto, name="Modelo de diseño", ownedElements=[])
        self.proyecto["ownedElements"].append(self.modelo)
        self.paquetes, self.elementos = {}, {}

    def nuevo(self, tipo, padre, **campos):
        self.contador += 1
        obj = {"_type": tipo, "_id": base64.b64encode(b"TIENDA-MVC" + self.contador.to_bytes(6, "big")).decode()}
        if padre is not None:
            obj["_parent"] = ref(padre)
        obj.update(campos)
        return obj

    # ---------------------------------------------------------------- elementos del modelo UML
    def paquete(self, nombre):
        if nombre not in self.paquetes:
            p = self.nuevo("UMLPackage", self.modelo, name=nombre, ownedElements=[])
            self.modelo["ownedElements"].append(p)
            self.paquetes[nombre] = p
        return self.paquetes[nombre]

    def clasificador(self, c):
        pkg = self.paquete(c["paquete"])
        e = self.nuevo("UMLInterface" if c["interfaz"] else "UMLClass", pkg, name=c["nombre"],
                       ownedElements=[], attributes=[], operations=[])
        if c["tipo"] in ("enum", "record"):
            e["stereotype"] = "enumeration" if c["tipo"] == "enum" else "record"
        for a in c["atributos"]:
            attr = self.nuevo("UMLAttribute", e, name=a["nombre"], visibility=a["vis"],
                              isStatic=a["static"], isReadOnly=a["final"])
            if a["tipo"]:
                attr["type"] = a["tipo"]
            e["attributes"].append(attr)
        for o in c["operaciones"]:
            op = self.nuevo("UMLOperation", e, name=o["nombre"], visibility=o["vis"], isStatic=o["static"],
                            parameters=[])
            for p in o["params"]:
                op["parameters"].append(self.nuevo("UMLParameter", op, name=p["nombre"], type=p["tipo"]))
            if o["retorno"] and o["retorno"] != "void":
                op["parameters"].append(self.nuevo("UMLParameter", op, name="", type=o["retorno"],
                                                   direction="return"))
            if o["constructor"]:
                op["stereotype"] = "create"
            e["operations"].append(op)
        pkg["ownedElements"].append(e)
        self.elementos[c["nombre"]] = e

    def relacion(self, tipo, origen, destino):
        r = self.nuevo(tipo, origen, source=ref(origen), target=ref(destino))
        origen.setdefault("ownedElements", []).append(r)
        return r

    # ---------------------------------------------------------------- vistas
    def diagrama(self, tipo, nombre, padre=None, por_defecto=False):
        padre = padre or self.modelo
        d = self.nuevo(tipo, padre, name=nombre, ownedViews=[])
        if por_defecto:
            d["defaultDiagram"] = True
        padre.setdefault("ownedElements", []).insert(0, d)
        return d

    def etiqueta(self, padre, texto="", visible=True, fuente=FUENTE, **extra):
        e = self.nuevo("LabelView", padre, font=fuente, parentStyle=True, left=0, top=0, width=0,
                       height=13, text=texto, visible=visible)
        e.update(extra)
        return e

    def compartimento_nombre(self, vista, modelo, nombre, est="", espacio=""):
        nc = self.nuevo("UMLNameCompartmentView", vista, model=ref(modelo), subViews=[], font=FUENTE,
                        left=vista["left"], top=vista["top"], width=vista["width"], height=25)
        e_est = self.etiqueta(nc, texto=est, visible=bool(est))
        e_nom = self.etiqueta(nc, texto=nombre, fuente=FUENTE_NEGRITA)
        e_ns = self.etiqueta(nc, texto=espacio, visible=bool(espacio))
        e_prop = self.etiqueta(nc, visible=False, horizontalAlignment=1)
        nc["subViews"] = [e_est, e_nom, e_ns, e_prop]
        nc.update(stereotypeLabel=ref(e_est), nameLabel=ref(e_nom), namespaceLabel=ref(e_ns),
                  propertyLabel=ref(e_prop))
        return nc

    def vista_clasificador(self, d, c, x, y, mostrar_paquete):
        w, h = tamano(c, mostrar_paquete)
        elem = self.elementos[c["nombre"]]
        v = self.nuevo("UMLInterfaceView" if c["interfaz"] else "UMLClassView", d, model=ref(elem), subViews=[],
                       font=FUENTE, containerChangeable=True, left=x, top=y, width=w, height=h,
                       showNamespace=mostrar_paquete)
        if estereotipo(c):
            v["stereotypeDisplay"] = "label"
        nc = self.compartimento_nombre(v, elem, c["nombre"], estereotipo(c),
                                       f"(from {c['paquete']})" if mostrar_paquete else "")
        attr = self.nuevo("UMLAttributeCompartmentView", v, model=ref(elem), subViews=[], font=FUENTE,
                          left=x, top=y + 25, width=w, height=10)
        for a, a_el in zip(c["atributos"], elem["attributes"]):
            attr["subViews"].append(self.nuevo("UMLAttributeView", attr, model=ref(a_el), font=FUENTE,
                                               left=x + 5, top=y + 30, width=w - 10, height=13,
                                               text=texto_atributo(a), horizontalAlignment=0))
        ops = self.nuevo("UMLOperationCompartmentView", v, model=ref(elem), subViews=[], font=FUENTE,
                         left=x, top=y + 35, width=w, height=10)
        for o, o_el in zip(c["operaciones"], elem["operations"]):
            ops["subViews"].append(self.nuevo("UMLOperationView", ops, model=ref(o_el), font=FUENTE,
                                              left=x + 5, top=y + 40, width=w - 10, height=13,
                                              text=texto_operacion(o), horizontalAlignment=0))
        rec = self.nuevo("UMLReceptionCompartmentView", v, model=ref(elem), subViews=[], font=FUENTE,
                         visible=False, left=x, top=y, width=10, height=10)
        tpl = self.nuevo("UMLTemplateParameterCompartmentView", v, model=ref(elem), subViews=[], font=FUENTE,
                         visible=False, left=x, top=y, width=10, height=10)
        v["subViews"] = [nc, attr, ops, rec, tpl]
        v.update(nameCompartment=ref(nc), attributeCompartment=ref(attr), operationCompartment=ref(ops),
                 receptionCompartment=ref(rec), templateParameterCompartment=ref(tpl))
        d["ownedViews"].append(v)
        return v, w, h

    def vista_paquete(self, d, pkg, x, y, w=170, h=80):
        v = self.nuevo("UMLPackageView", d, model=ref(pkg), subViews=[], font=FUENTE,
                       containerChangeable=True, left=x, top=y, width=w, height=h)
        nc = self.compartimento_nombre(v, pkg, pkg["name"])
        v["subViews"] = [nc]
        v["nameCompartment"] = ref(nc)
        d["ownedViews"].append(v)
        return v

    def vista_relacion(self, d, rel, v_origen, v_destino):
        tipo_vista = {"UMLDependency": "UMLDependencyView", "UMLGeneralization": "UMLGeneralizationView",
                      "UMLInterfaceRealization": "UMLInterfaceRealizationView"}[rel["_type"]]
        (x1, y1), (x2, y2) = centro(v_origen), centro(v_destino)
        v = self.nuevo(tipo_vista, d, model=ref(rel), subViews=[], font=FUENTE, head=ref(v_destino),
                       tail=ref(v_origen), lineStyle=1, points=f"{x1}:{y1};{x2}:{y2}")
        etiquetas = [self.nuevo("EdgeLabelView", v, model=ref(rel), visible=False, font=FUENTE,
                                left=(x1 + x2) // 2, top=(y1 + y2) // 2, height=13, alpha=alpha,
                                distance=dist, hostEdge=ref(v), edgePosition=1)
                     for alpha, dist in ((1.5707963267948966, 15), (1.5707963267948966, 30),
                                         (-1.5707963267948966, 15))]
        v["subViews"] = etiquetas
        v.update(nameLabel=ref(etiquetas[0]), stereotypeLabel=ref(etiquetas[1]), propertyLabel=ref(etiquetas[2]))
        d["ownedViews"].append(v)

    # ---------------------------------------------------------------- secuencia
    def secuencia(self, nombre, participantes, mensajes, actor):
        colab = self.nuevo("UMLCollaboration", self.modelo, name="Colaboración " + nombre, ownedElements=[],
                           attributes=[])
        inter = self.nuevo("UMLInteraction", colab, name=nombre, ownedElements=[], messages=[], participants=[])
        colab["ownedElements"].append(inter)
        self.modelo["ownedElements"].append(colab)
        lifelines = []
        for rol, tipo in participantes:
            attr = self.nuevo("UMLAttribute", colab, name=rol)
            attr["type"] = ref(actor) if tipo == actor["name"] else ref(self.elementos[tipo])
            colab["attributes"].append(attr)
            ll = self.nuevo("UMLLifeline", inter, name=rol, represent=ref(attr), isMultiInstance=False)
            inter["participants"].append(ll)
            lifelines.append(ll)
        msgs = []
        for i_o, i_d, texto, sort in mensajes:
            msg = self.nuevo("UMLMessage", inter, name=texto, source=ref(lifelines[i_o]),
                             target=ref(lifelines[i_d]), messageSort=sort)
            inter["messages"].append(msg)
            msgs.append(msg)

        d = self.diagrama("UMLSequenceDiagram", nombre, padre=inter)
        d["showSequenceNumber"] = True
        ancho_col, x0, y_cab, alto_cab = 215, 40, 60, 40
        alto_total = 140 + 42 * len(mensajes) + 60
        frame = self.nuevo("UMLFrameView", d, model=ref(d), subViews=[], font=FUENTE, left=15, top=15,
                           width=x0 + ancho_col * len(participantes) + 10, height=alto_total)
        fn = self.etiqueta(frame, texto=nombre, left=50, top=20, width=260, fuente=FUENTE_NEGRITA)
        ft = self.etiqueta(frame, texto="sd", left=20, top=20, width=25, fuente=FUENTE_NEGRITA)
        frame["subViews"] = [fn, ft]
        frame.update(nameLabel=ref(fn), frameTypeLabel=ref(ft))
        d["ownedViews"].append(frame)

        lineas, xs = [], []
        for i, ll in enumerate(lifelines):
            x, w = x0 + ancho_col * i + 20, ancho_col - 40
            v = self.nuevo("UMLSeqLifelineView", d, model=ref(ll), subViews=[], font=FUENTE, left=x, top=y_cab,
                           width=w, height=alto_total - y_cab, stereotypeDisplay="label")
            nc = self.compartimento_nombre(v, ll, participantes[i][0] + ": " + participantes[i][1])
            linea = self.nuevo("UMLLinePartView", v, model=ref(ll), font=FUENTE, left=x + w // 2,
                               top=y_cab + alto_cab, width=1, height=alto_total - y_cab - alto_cab)
            v["subViews"] = [nc, linea]
            v.update(nameCompartment=ref(nc), linePart=ref(linea))
            d["ownedViews"].append(v)
            lineas.append(linea)
            xs.append(x + w // 2)

        for k, ((i_o, i_d, _texto, sort), msg) in enumerate(zip(mensajes, msgs)):
            y = y_cab + alto_cab + 40 + 42 * k
            if i_o == i_d:
                x = xs[i_o]
                puntos = f"{x}:{y};{x + 40}:{y};{x + 40}:{y + 20};{x + 7}:{y + 20}"
            else:
                puntos = f"{xs[i_o]}:{y};{xs[i_d]}:{y}"
            mv = self.nuevo("UMLSeqMessageView", d, model=ref(msg), subViews=[], font=FUENTE,
                            head=ref(lineas[i_d]), tail=ref(lineas[i_o]), lineStyle=0, points=puntos)
            etiquetas = [self.nuevo("EdgeLabelView", mv, model=ref(msg), font=FUENTE, height=13,
                                    left=min(xs[i_o], xs[i_d]), top=y - 15, alpha=alpha, distance=dist,
                                    hostEdge=ref(mv), edgePosition=1, visible=dist == 10 and alpha > 0)
                         for alpha, dist in ((1.5707963267948966, 10), (1.5707963267948966, 25),
                                             (-1.5707963267948966, 10))]
            act = self.nuevo("UMLActivationView", mv, model=ref(msg), font=FUENTE, left=xs[i_d] - 7, top=y,
                             width=14, height=29, visible=sort != "reply")
            mv["subViews"] = etiquetas + [act]
            mv.update(nameLabel=ref(etiquetas[0]), stereotypeLabel=ref(etiquetas[1]),
                      propertyLabel=ref(etiquetas[2]), activation=ref(act))
            d["ownedViews"].append(mv)

    def guardar(self, ruta):
        n_ids, n_refs = validar(self.proyecto)
        ruta.parent.mkdir(parents=True, exist_ok=True)
        ruta.write_text(json.dumps(self.proyecto, indent="\t", ensure_ascii=False), encoding="utf-8")
        return n_ids, n_refs


def validar(proyecto):
    """_id únicos, todas las $ref resuelven y cada _parent apunta a su contenedor real."""
    ids, refs, padres = {}, [], []

    def recorrer(obj, contenedor):
        if isinstance(obj, dict):
            if "_id" in obj:
                if obj["_id"] in ids:
                    raise ValueError("_id duplicado: " + obj["_id"])
                ids[obj["_id"]] = obj
                if "_parent" in obj:
                    padres.append((obj, contenedor))
                contenedor = obj
            for k, v in obj.items():
                if k == "_parent":
                    continue
                if isinstance(v, dict) and set(v) == {"$ref"}:
                    refs.append(v["$ref"])
                else:
                    recorrer(v, contenedor)
        elif isinstance(obj, list):
            for x in obj:
                if isinstance(x, dict) and set(x) == {"$ref"}:
                    refs.append(x["$ref"])
                else:
                    recorrer(x, contenedor)

    recorrer(proyecto, None)
    faltantes = [r for r in refs if r not in ids]
    if faltantes:
        raise ValueError(f"$ref sin resolver: {faltantes[:5]}")
    for obj, contenedor in padres:
        if obj["_parent"]["$ref"] != contenedor["_id"]:
            raise ValueError(f"_parent incorrecto en {obj['_type']} {obj.get('name', '')}")
    return len(ids), len(refs)


# ============================================================================ diagramas de la tienda

POSICIONES_PAQUETES = {"(raiz)": (330, 30), "vista": (60, 200), "controlador": (330, 200),
                       "persistencia": (600, 200), "modelo": (330, 380)}

# Columnas de cada diagrama de clases. Las clases de otro paquete se muestran con "(from paquete)"
# para que se vea hacia dónde apuntan las dependencias.
DIAGRAMAS_CLASES = {
    "modelo": [["Tienda", "ObservadorTienda", "ArticuloDAO"],
               ["Articulo", "TipoArticulo", "Familia"],
               ["LineaPedido", "Ticket", "RenglonTicket"],
               ["ReglaNegocioException", "DatosInvalidosException", "ArticuloNoEncontradoException",
                "StockInsuficienteException", "AccesorioIncompatibleException"]],
    "vista": [["VistaAlmacen", "VentanaAlmacen"], ["VistaMostrador", "VentanaMostrador"], ["Formato"]],
    "controlador": [["ControladorAlmacen", "ControladorMostrador"], ["VistaAlmacen", "VistaMostrador"],
                    ["Tienda", "ObservadorTienda"]],
    "persistencia": [["ArticuloDAOMemoria", "ArticuloDAOArchivo"], ["ArticuloDAO", "Articulo"]],
    "(raiz)": [["Main"], ["ControladorAlmacen", "ControladorMostrador"],
               ["Tienda", "ArticuloDAOArchivo", "ArticuloDAOMemoria"]],
}

# En el diagrama del modelo sólo se dibujan las dependencias que explican la estructura.
DEPENDENCIAS_MODELO = {("Tienda", "ArticuloDAO"), ("Tienda", "ObservadorTienda"), ("Tienda", "Articulo"),
                       ("Tienda", "Ticket"), ("Tienda", "LineaPedido"), ("Ticket", "RenglonTicket"),
                       ("Articulo", "TipoArticulo"), ("Articulo", "Familia"),
                       ("Tienda", "ReglaNegocioException")}

SECUENCIA_PARTICIPANTES = [("cajero", "Cajero"), ("mostrador", "VentanaMostrador"),
                           ("ctrlMostrador", "ControladorMostrador"), ("tienda", "Tienda"), ("dao", "ArticuloDAO"),
                           ("ctrlAlmacen", "ControladorAlmacen"), ("almacen", "VentanaAlmacen")]
SECUENCIA_MENSAJES = [
    (0, 1, "clic en Cobrar", "asynchSignal"),
    (1, 2, "cobrar()", "synchCall"),
    (2, 3, "vender(pedido)", "synchCall"),
    (3, 3, "cotizar(pedido): stock, R1 compatibilidad, R3 kit", "synchCall"),
    (3, 4, "buscar(codigo) / actualizar(articulo)", "synchCall"),
    (3, 3, "notificar()", "synchCall"),
    (3, 5, "tiendaCambiada()", "synchCall"),
    (5, 3, "getArticulos(), getAlertasReabasto()  [R2]", "synchCall"),
    (5, 6, "mostrarInventario(...), mostrarAlertas(...)", "synchCall"),
    (3, 2, "tiendaCambiada()", "synchCall"),
    (2, 1, "mostrarCatalogo(articulos)", "synchCall"),
    (3, 2, "ticket", "reply"),
    (2, 1, "mostrarPedido(vacio), mostrarVentaRealizada(ticket)", "synchCall"),
]


def relaciones(m, clases):
    rels = []
    for c in clases.values():
        origen = m.elementos[c["nombre"]]
        estructurales = set()
        if c["extiende"] in clases:
            rels.append((c["nombre"], c["extiende"], m.relacion("UMLGeneralization", origen,
                                                                m.elementos[c["extiende"]])))
            estructurales.add(c["extiende"])
        for i in c["implementa"]:
            if i in clases:
                rels.append((c["nombre"], i, m.relacion("UMLInterfaceRealization", origen, m.elementos[i])))
                estructurales.add(i)
        for usado in sorted(c["usa"] - estructurales):
            rels.append((c["nombre"], usado, m.relacion("UMLDependency", origen, m.elementos[usado])))
    return rels


def diagrama_paquetes(m, clases):
    d = m.diagrama("UMLPackageDiagram", "Paquetes - Tienda de instrumentos", por_defecto=True)
    deps = set()
    for c in clases.values():
        for usado in c["usa"] | ({c["extiende"]} & set(clases)) | (set(c["implementa"]) & set(clases)):
            if clases[usado]["paquete"] != c["paquete"]:
                deps.add((c["paquete"], clases[usado]["paquete"]))
    vistas = {p: m.vista_paquete(d, m.paquete(p), *xy) for p, xy in POSICIONES_PAQUETES.items()}
    for origen, destino in sorted(deps):
        m.vista_relacion(d, m.relacion("UMLDependency", m.paquete(origen), m.paquete(destino)),
                         vistas[origen], vistas[destino])
    return sorted(deps)


def diagrama_clases(m, clases, rels, paquete, columnas):
    d = m.diagrama("UMLClassDiagram", f"Clases - paquete {paquete}")
    vistas, x = {}, 30
    for columna in columnas:
        y, ancho = 30, 0
        for nombre in columna:
            ajeno = clases[nombre]["paquete"] != paquete
            v, w, h = m.vista_clasificador(d, clases[nombre], x, y, ajeno)
            vistas[nombre] = v
            y += h + 45
            ancho = max(ancho, w)
        x += ancho + 70
    for o, de, rel in rels:
        if o in vistas and de in vistas:
            if paquete == "modelo" and rel["_type"] == "UMLDependency" and (o, de) not in DEPENDENCIAS_MODELO:
                continue
            m.vista_relacion(d, rel, vistas[o], vistas[de])


def exportar_png():
    if not STARUML.exists():
        print("StarUML no está instalado; se omite la exportación a PNG.")
        return True
    salida = DESTINO.parent / "imagenes"
    shutil.rmtree(salida, ignore_errors=True)
    salida.mkdir(parents=True)
    plantilla = str(salida).replace("\\", "/") + "/<%=filenamify(element.name)%>.png"
    try:
        subprocess.run([str(STARUML), "image", str(DESTINO), "-f", "png", "-o", plantilla],
                       capture_output=True, text=True, timeout=180, env={**os.environ, "MSYS_NO_PATHCONV": "1"})
    except subprocess.TimeoutExpired:
        print("StarUML no respondió en 180 s (suele pasar si está abierto o sin red para buscar "
              "actualizaciones). Ciérralo y vuelve a intentar, o abre el .mdj a mano.")
    pngs = sorted(salida.glob("*.png"))
    esperados = 2 + len(DIAGRAMAS_CLASES)
    print(f"StarUML abrió el archivo y exportó {len(pngs)}/{esperados} diagramas:")
    for p in pngs:
        print("   ", p.relative_to(RAIZ))
    return len(pngs) == esperados


def main():
    clases = leer_proyecto()
    m = Mdj("Tienda de instrumentos musicales - MVC")
    for c in clases.values():
        m.clasificador(c)
    deps = diagrama_paquetes(m, clases)
    rels = relaciones(m, clases)
    for paquete, columnas in DIAGRAMAS_CLASES.items():
        diagrama_clases(m, clases, rels, paquete, columnas)
    actor = m.nuevo("UMLActor", m.modelo, name="Cajero")
    m.modelo["ownedElements"].append(actor)
    m.secuencia("Cobrar un kit en el mostrador", SECUENCIA_PARTICIPANTES, SECUENCIA_MENSAJES, actor)
    n_ids, n_refs = m.guardar(DESTINO)

    print(f"{DESTINO.relative_to(RAIZ)}: {len(clases)} clases, {n_ids} objetos, {n_refs} referencias válidas")
    print("Dependencias entre paquetes:", ", ".join(f"{a} -> {b}" for a, b in deps))
    salen = [d for d in deps if d[0] == "modelo"]
    print("Dependencias que salen del modelo:", salen or "ninguna (correcto)")
    ok = not salen
    if "--png" in sys.argv:
        ok = exportar_png() and ok
    sys.exit(0 if ok else 1)


if __name__ == "__main__":
    main()
