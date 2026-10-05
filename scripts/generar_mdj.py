"""Genera los diagramas UML en formato nativo de StarUML (.mdj) a partir del codigo Java.

- Lee el codigo de cada ejercicio directamente del repositorio git (por tag), asi el diagrama
  corresponde exactamente al codigo entregado en ese ejercicio.
- Construye el modelo UML (paquetes, clases, interfaces, atributos, operaciones, dependencias,
  generalizaciones y realizaciones) y las vistas de diagrama con todas las sub-vistas embebidas
  que StarUML espera (compartimentos, etiquetas), para que el archivo abra sin reparaciones.
- Valida la integridad de cada archivo: _id unicos, todas las referencias $ref resuelven y
  cada _parent apunta al objeto que realmente lo contiene.

Uso: python scripts/generar_mdj.py
"""
import base64
import json
import os
import pathlib
import re
import subprocess
import sys

RAIZ = pathlib.Path(__file__).resolve().parent.parent
FUENTE = "Arial;13;0"
FUENTE_NEGRITA = "Arial;13;1"


# ============================================================================ lectura del codigo Java

def archivos_en_tag(tag, carpeta):
    """Devuelve {ruta: contenido} de los .java bajo `carpeta` en el commit `tag`."""
    salida = subprocess.run(["git", "ls-tree", "-r", "--name-only", tag, carpeta], cwd=RAIZ,
                            capture_output=True, text=True, check=True).stdout.split()
    archivos = {}
    for ruta in salida:
        if ruta.endswith(".java"):
            archivos[ruta] = subprocess.run(["git", "show", f"{tag}:{ruta}"], cwd=RAIZ, capture_output=True,
                                            text=True, encoding="utf-8", check=True).stdout
    return archivos


def archivos_en_disco(carpeta):
    base = RAIZ / carpeta
    return {str(p.relative_to(RAIZ)).replace(os.sep, "/"): p.read_text(encoding="utf-8")
            for p in base.rglob("*.java")}


def sin_comentarios(codigo):
    codigo = re.sub(r"/\*.*?\*/", "", codigo, flags=re.S)
    codigo = re.sub(r"//[^\n]*", "", codigo)
    return re.sub(r'"(?:\\.|[^"\\])*"', '""', codigo)


MODIFICADORES = r"(?:(?:public|private|protected|static|final|abstract|synchronized|volatile|transient|default)\s+)*"
RE_TIPO = re.compile(r"^(?:public\s+)?(?:abstract\s+|final\s+)*(class|interface)\s+(\w+)"
                     r"(?:\s+extends\s+([\w.<>]+))?(?:\s+implements\s+([\w.<>,\s]+?))?\s*\{", re.M)
RE_CAMPO = re.compile(r"^ {4}(?:@\w+\s+)?(" + MODIFICADORES + r")([\w<>\[\],.? ]+?)\s+(\w+)\s*(?:=|;)")
RE_METODO = re.compile(r"^ {4}(?:@\w+(?:\([^)]*\))?\s+)*(" + MODIFICADORES + r")(?:([\w<>\[\],.? ]+?)\s+)?(\w+)\s*\(([^)]*)\)?")


def visibilidad(mods, es_interfaz):
    if "private" in mods:
        return "private"
    if "protected" in mods:
        return "protected"
    if "public" in mods or es_interfaz:
        return "public"
    return "package"


def parsear(ruta, codigo, raiz_src):
    """Extrae la estructura de una clase o interfaz de nivel superior."""
    paquete = re.search(r"^package\s+([\w.]+);", codigo, re.M)
    paquete = paquete.group(1) if paquete else "(raiz)"
    limpio = sin_comentarios(codigo)
    m = RE_TIPO.search(limpio)
    if not m:
        return None
    tipo, nombre, extiende, implementa = m.groups()
    es_interfaz = tipo == "interface"
    cuerpo = limpio[m.end():]
    atributos, operaciones = [], []
    for linea in cuerpo.splitlines():
        if not linea.startswith("    ") or linea.startswith("     "):
            continue
        if "(" not in linea.split("=")[0]:
            c = RE_CAMPO.match(linea)
            if c and not re.search(r"\b(class|interface|return|new)\b", c.group(2)):
                mods = c.group(1)
                atributos.append({"nombre": c.group(3), "tipo": c.group(2).strip(),
                                  "vis": visibilidad(mods, es_interfaz), "static": "static" in mods,
                                  "final": "final" in mods})
                continue
        mt = RE_METODO.match(linea)
        if mt:
            mods, retorno, nom, params = mt.groups()
            if nom in ("if", "for", "while", "switch", "return", "new", "catch", "synchronized"):
                continue
            if retorno and retorno.strip() in ("new", "return"):
                continue
            constructor = nom == nombre
            if not constructor and not retorno:
                continue
            vis = visibilidad(mods, es_interfaz)
            if vis not in ("public", "protected"):
                continue
            lista = []
            for p in [x.strip() for x in (params or "").split(",") if x.strip()]:
                p = re.sub(r"^final\s+", "", p)
                partes = p.rsplit(" ", 1)
                lista.append({"nombre": partes[-1], "tipo": partes[0] if len(partes) > 1 else ""})
            operaciones.append({"nombre": nom, "params": lista, "retorno": None if constructor else retorno.strip(),
                                "vis": vis, "static": "static" in mods, "constructor": constructor})
    implementa = [x.strip() for x in implementa.split(",")] if implementa else []
    return {"nombre": nombre, "paquete": paquete, "interfaz": es_interfaz, "extiende": extiende,
            "implementa": implementa, "atributos": atributos, "operaciones": operaciones,
            "cuerpo": cuerpo, "ruta": ruta}


def leer_proyecto(archivos):
    clases = {}
    for ruta, codigo in sorted(archivos.items()):
        c = parsear(ruta, codigo, None)
        if c:
            clases[c["nombre"]] = c
    # dependencias por uso de nombres de otras clases del proyecto
    for c in clases.values():
        usados = set()
        for otro in clases:
            if otro != c["nombre"] and re.search(r"\b" + re.escape(otro) + r"\b", c["cuerpo"]):
                usados.add(otro)
        c["usa"] = usados
    return clases


# ============================================================================ construccion del .mdj

class Mdj:
    def __init__(self, nombre_proyecto, nombre_modelo):
        self.contador = 0
        self.proyecto = self.nuevo("Project", None, name=nombre_proyecto, ownedElements=[],
                                   author="Serie de ejercicios MVC")
        self.modelo = self.nuevo("UMLModel", self.proyecto, name=nombre_modelo, ownedElements=[])
        self.proyecto["ownedElements"].append(self.modelo)
        self.paquetes = {}
        self.elementos = {}       # nombre de clase -> elemento UML
        self.relaciones = []      # (tipo, origen, destino, elemento)

    def nid(self):
        self.contador += 1
        crudo = b"MVC-ITSON" + self.contador.to_bytes(6, "big")
        return base64.b64encode(crudo).decode()

    def nuevo(self, tipo, padre, **campos):
        obj = {"_type": tipo, "_id": self.nid()}
        if padre is not None:
            obj["_parent"] = ref(padre)
        obj.update(campos)
        return obj

    # ---------------------------------------------------------------- modelo
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
        for a in c["atributos"]:
            e["attributes"].append(self.nuevo("UMLAttribute", e, name=a["nombre"], visibility=a["vis"],
                                              type=a["tipo"], isStatic=a["static"], isReadOnly=a["final"]))
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
        return e

    def relacion(self, tipo, origen, destino, nombre=""):
        r = self.nuevo(tipo, origen, source=ref(origen), target=ref(destino))
        if nombre:
            r["name"] = nombre
        origen.setdefault("ownedElements", []).append(r)
        self.relaciones.append(r)
        return r

    # ---------------------------------------------------------------- diagramas
    def diagrama(self, tipo, nombre, padre=None, por_defecto=False):
        padre = padre or self.modelo
        d = self.nuevo(tipo, padre, name=nombre, ownedViews=[])
        if por_defecto:
            d["defaultDiagram"] = True
        padre.setdefault("ownedElements", []).insert(0, d)
        return d

    def etiqueta(self, vista_padre, modelo=None, texto="", visible=True, fuente=FUENTE, **extra):
        e = self.nuevo("LabelView", vista_padre, font=fuente, parentStyle=True, left=0, top=0, width=0,
                       height=13, text=texto, visible=visible)
        if modelo is not None:
            e["model"] = ref(modelo)
        e.update(extra)
        return e

    def compartimento_nombre(self, vista, modelo, nombre, estereotipo="", espacio=""):
        nc = self.nuevo("UMLNameCompartmentView", vista, model=ref(modelo), subViews=[], font=FUENTE,
                        left=vista["left"], top=vista["top"], width=vista["width"], height=25)
        est = self.etiqueta(nc, texto=estereotipo, visible=bool(estereotipo))
        nom = self.etiqueta(nc, texto=nombre, fuente=FUENTE_NEGRITA)
        ns = self.etiqueta(nc, texto=espacio, visible=bool(espacio))
        prop = self.etiqueta(nc, visible=False, horizontalAlignment=1)
        nc["subViews"] = [est, nom, ns, prop]
        nc.update(stereotypeLabel=ref(est), nameLabel=ref(nom), namespaceLabel=ref(ns), propertyLabel=ref(prop))
        return nc

    def vista_clasificador(self, diagrama, c, x, y, w, h, mostrar_paquete=False):
        elem = self.elementos[c["nombre"]]
        tipo = "UMLInterfaceView" if c["interfaz"] else "UMLClassView"
        v = self.nuevo(tipo, diagrama, model=ref(elem), subViews=[], font=FUENTE, containerChangeable=True,
                       left=x, top=y, width=w, height=h, showNamespace=mostrar_paquete)
        if c["interfaz"]:
            v["stereotypeDisplay"] = "label"
        nc = self.compartimento_nombre(v, elem, c["nombre"], "«interface»" if c["interfaz"] else "",
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
        diagrama["ownedViews"].append(v)
        return v

    def vista_paquete(self, diagrama, pkg, x, y, w=170, h=80):
        v = self.nuevo("UMLPackageView", diagrama, model=ref(pkg), subViews=[], font=FUENTE,
                       containerChangeable=True, left=x, top=y, width=w, height=h)
        nc = self.compartimento_nombre(v, pkg, pkg["name"])
        v["subViews"] = [nc]
        v["nameCompartment"] = ref(nc)
        diagrama["ownedViews"].append(v)
        return v

    def vista_relacion(self, diagrama, rel, v_origen, v_destino):
        tipo_vista = {"UMLDependency": "UMLDependencyView", "UMLGeneralization": "UMLGeneralizationView",
                      "UMLInterfaceRealization": "UMLInterfaceRealizationView"}[rel["_type"]]
        (x1, y1), (x2, y2) = centro(v_origen), centro(v_destino)
        v = self.nuevo(tipo_vista, diagrama, model=ref(rel), subViews=[], font=FUENTE,
                       head=ref(v_destino), tail=ref(v_origen), lineStyle=1,
                       points=f"{x1}:{y1};{x2}:{y2}")
        etiquetas = []
        for alpha, distancia in ((1.5707963267948966, 15), (1.5707963267948966, 30), (-1.5707963267948966, 15)):
            etiquetas.append(self.nuevo("EdgeLabelView", v, model=ref(rel), visible=False, font=FUENTE,
                                        left=(x1 + x2) // 2, top=(y1 + y2) // 2, height=13, alpha=alpha,
                                        distance=distancia, hostEdge=ref(v), edgePosition=1))
        v["subViews"] = etiquetas
        v.update(nameLabel=ref(etiquetas[0]), stereotypeLabel=ref(etiquetas[1]), propertyLabel=ref(etiquetas[2]))
        diagrama["ownedViews"].append(v)
        return v

    # ---------------------------------------------------------------- secuencia
    def secuencia(self, nombre, participantes, mensajes, tipos):
        """participantes: [(nombre_rol, nombre_tipo)]; mensajes: [(i_origen, i_destino, texto, sort)]."""
        colab = self.nuevo("UMLCollaboration", self.modelo, name="Colaboracion " + nombre, ownedElements=[],
                           attributes=[])
        inter = self.nuevo("UMLInteraction", colab, name=nombre, ownedElements=[], messages=[], participants=[])
        colab["ownedElements"].append(inter)
        self.modelo["ownedElements"].append(colab)
        lifelines = []
        for rol, tipo in participantes:
            attr = self.nuevo("UMLAttribute", colab, name=rol)
            if tipo in self.elementos:
                attr["type"] = ref(self.elementos[tipo])
            elif tipo in tipos:
                attr["type"] = ref(tipos[tipo])
            else:
                attr["type"] = tipo
            colab["attributes"].append(attr)
            ll = self.nuevo("UMLLifeline", inter, name=rol, represent=ref(attr), isMultiInstance=False)
            inter["participants"].append(ll)
            lifelines.append(ll)
        msgs = []
        for i_o, i_d, texto, sort in mensajes:
            m = self.nuevo("UMLMessage", inter, name=texto, source=ref(lifelines[i_o]),
                           target=ref(lifelines[i_d]), messageSort=sort)
            inter["messages"].append(m)
            msgs.append(m)

        d = self.diagrama("UMLSequenceDiagram", nombre, padre=inter)
        d["showSequenceNumber"] = True
        ancho_col, x0, y_cab, alto_cab = 205, 40, 60, 40
        alto_total = 140 + 42 * len(mensajes) + 60
        frame = self.nuevo("UMLFrameView", d, model=ref(d), subViews=[], font=FUENTE, left=15, top=15,
                           width=x0 + ancho_col * len(participantes) + 10, height=alto_total)
        fn = self.etiqueta(frame, texto=nombre, left=50, top=20, width=200, fuente=FUENTE_NEGRITA)
        ft = self.etiqueta(frame, texto="sd", left=20, top=20, width=25, fuente=FUENTE_NEGRITA)
        frame["subViews"] = [fn, ft]
        frame.update(nameLabel=ref(fn), frameTypeLabel=ref(ft))
        d["ownedViews"].append(frame)

        vistas_ll, lineas, xs = [], [], []
        for i, ll in enumerate(lifelines):
            x = x0 + ancho_col * i + 20
            w = ancho_col - 40
            v = self.nuevo("UMLSeqLifelineView", d, model=ref(ll), subViews=[], font=FUENTE,
                           left=x, top=y_cab, width=w, height=alto_total - y_cab, stereotypeDisplay="label")
            nc = self.compartimento_nombre(v, ll, participantes[i][0] + ": " + participantes[i][1])
            linea = self.nuevo("UMLLinePartView", v, model=ref(ll), font=FUENTE, left=x + w // 2,
                               top=y_cab + alto_cab, width=1, height=alto_total - y_cab - alto_cab)
            v["subViews"] = [nc, linea]
            v.update(nameCompartment=ref(nc), linePart=ref(linea))
            d["ownedViews"].append(v)
            vistas_ll.append(v)
            lineas.append(linea)
            xs.append(x + w // 2)

        for k, ((i_o, i_d, texto, sort), m) in enumerate(zip(mensajes, msgs)):
            y = y_cab + alto_cab + 40 + 42 * k
            if i_o == i_d:
                x = xs[i_o]
                puntos = f"{x}:{y};{x + 40}:{y};{x + 40}:{y + 20};{x + 7}:{y + 20}"
            else:
                puntos = f"{xs[i_o]}:{y};{xs[i_d]}:{y}"
            mv = self.nuevo("UMLSeqMessageView", d, model=ref(m), subViews=[], font=FUENTE,
                            head=ref(lineas[i_d]), tail=ref(lineas[i_o]), lineStyle=0, points=puntos)
            etiquetas = []
            for alpha, dist in ((1.5707963267948966, 10), (1.5707963267948966, 25), (-1.5707963267948966, 10)):
                etiquetas.append(self.nuevo("EdgeLabelView", mv, model=ref(m), font=FUENTE, height=13,
                                            left=min(xs[i_o], xs[i_d]), top=y - 15, alpha=alpha,
                                            distance=dist, hostEdge=ref(mv), edgePosition=1,
                                            visible=dist == 10 and alpha > 0))
            act = self.nuevo("UMLActivationView", mv, model=ref(m), font=FUENTE, left=xs[i_d] - 7,
                             top=y, width=14, height=29, visible=sort != "reply")
            mv["subViews"] = etiquetas + [act]
            mv.update(nameLabel=ref(etiquetas[0]), stereotypeLabel=ref(etiquetas[1]),
                      propertyLabel=ref(etiquetas[2]), activation=ref(act))
            d["ownedViews"].append(mv)
        return d, (participantes, mensajes, xs, y_cab, alto_cab, alto_total)

    def guardar(self, ruta):
        validar(self.proyecto)
        ruta.parent.mkdir(parents=True, exist_ok=True)
        ruta.write_text(json.dumps(self.proyecto, indent="\t", ensure_ascii=False), encoding="utf-8")


def ref(obj):
    return {"$ref": obj["_id"]}


def centro(v):
    return v["left"] + v["width"] // 2, v["top"] + v["height"] // 2


SIMBOLO = {"public": "+", "private": "-", "protected": "#", "package": "~"}


def texto_atributo(a):
    return f"{SIMBOLO[a['vis']]}{a['nombre']}: {a['tipo']}"


def texto_operacion(o):
    params = ", ".join(f"{p['nombre']}: {p['tipo']}" for p in o["params"])
    ret = f": {o['retorno']}" if o["retorno"] and o["retorno"] != "void" else ""
    return f"{SIMBOLO[o['vis']]}{o['nombre']}({params}){ret}"


def tamano(c, mostrar_paquete=False):
    lineas = [c["nombre"]] + [texto_atributo(a) for a in c["atributos"]] + \
             [texto_operacion(o) for o in c["operaciones"]]
    if mostrar_paquete:
        lineas.append(f"(from {c['paquete']})")
    ancho = max(150, int(max(len(l) for l in lineas) * 7.0) + 20)
    alto = 30 + (15 if c["interfaz"] else 0) + (15 if mostrar_paquete else 0) \
        + 10 + 15 * len(c["atributos"]) + 10 + 15 * len(c["operaciones"])
    return ancho, alto


# ============================================================================ validacion

def validar(proyecto):
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
                    refs.append((obj, k, v["$ref"]))
                else:
                    recorrer(v, contenedor)
        elif isinstance(obj, list):
            for x in obj:
                if isinstance(x, dict) and set(x) == {"$ref"}:
                    refs.append((None, "lista", x["$ref"]))
                else:
                    recorrer(x, contenedor)

    recorrer(proyecto, None)
    for obj, campo, r in refs:
        if r not in ids:
            raise ValueError(f"$ref sin resolver en {campo}: {r}")
    for obj, contenedor in padres:
        if obj["_parent"]["$ref"] != contenedor["_id"]:
            raise ValueError(f"_parent incorrecto en {obj['_type']} {obj.get('name', '')}")
    return len(ids), len(refs)


# ============================================================================ diagramas concretos

def diagrama_paquetes(m, clases, titulo, posiciones):
    """Diagrama de paquetes con una dependencia por cada import entre paquetes."""
    d = m.diagrama("UMLPackageDiagram", titulo, por_defecto=True)
    deps = set()
    for c in clases.values():
        for usado in c["usa"] | ({c["extiende"]} if c["extiende"] in clases else set()) | \
                {i for i in c["implementa"] if i in clases}:
            if clases[usado]["paquete"] != c["paquete"]:
                deps.add((c["paquete"], clases[usado]["paquete"]))
    vistas = {p: m.vista_paquete(d, m.paquete(p), *posiciones[p]) for p in posiciones}
    for origen, destino in sorted(deps):
        rel = m.relacion("UMLDependency", m.paquete(origen), m.paquete(destino))
        m.vista_relacion(d, rel, vistas[origen], vistas[destino])
    return d, vistas, sorted(deps)


def relaciones_de_clases(m, clases):
    """Crea en el modelo las relaciones entre clases (una sola vez) y las devuelve."""
    rels = []
    for c in clases.values():
        origen = m.elementos[c["nombre"]]
        estructurales = set()
        if c["extiende"] in clases:
            rels.append((c["nombre"], c["extiende"], m.relacion(
                "UMLGeneralization", origen, m.elementos[c["extiende"]])))
            estructurales.add(c["extiende"])
        for i in c["implementa"]:
            if i in clases:
                rels.append((c["nombre"], i, m.relacion("UMLInterfaceRealization", origen, m.elementos[i])))
                estructurales.add(i)
        for usado in sorted(c["usa"] - estructurales):
            rels.append((c["nombre"], usado, m.relacion("UMLDependency", origen, m.elementos[usado])))
    return rels


def diagrama_clases(m, clases, rels, titulo, nombres, columnas, mostrar_paquete=True, por_defecto=False):
    """Diagrama de clases con las clases indicadas, acomodadas por columnas."""
    d = m.diagrama("UMLClassDiagram", titulo, por_defecto=por_defecto)
    vistas, layout = {}, []
    x = 30
    for columna in columnas:
        y = 30
        ancho_col = 0
        for nombre in columna:
            if nombre not in nombres:
                continue
            w, h = tamano(clases[nombre], mostrar_paquete)
            vistas[nombre] = m.vista_clasificador(d, clases[nombre], x, y, w, h, mostrar_paquete)
            layout.append((nombre, x, y, w, h))
            y += h + 45
            ancho_col = max(ancho_col, w)
        x += ancho_col + 70
    aristas = []
    for o, de, rel in rels:
        if o in vistas and de in vistas:
            m.vista_relacion(d, rel, vistas[o], vistas[de])
            aristas.append((o, de, rel["_type"]))
    return d, layout, aristas


def diagrama_clases_filas(m, clases, rels, titulo, filas):
    """Diagrama de clases con una fila por paquete (de arriba hacia abajo: quien depende, de quien depende)."""
    d = m.diagrama("UMLClassDiagram", titulo)
    vistas, layout = {}, []
    y = 30
    for fila in filas:
        x, alto = 30, 0
        for nombre in fila:
            w, h = tamano(clases[nombre], True)
            vistas[nombre] = m.vista_clasificador(d, clases[nombre], x, y, w, h, True)
            layout.append((nombre, x, y, w, h))
            x += w + 50
            alto = max(alto, h)
        y += alto + 90
    aristas = []
    for o, de, rel in rels:
        if o in vistas and de in vistas:
            m.vista_relacion(d, rel, vistas[o], vistas[de])
            aristas.append((o, de, rel["_type"]))
    return d, layout, aristas


def filas_por_paquete(clases, orden_paquetes):
    filas = []
    for p in orden_paquetes:
        nombres = sorted([c for c in clases if clases[c]["paquete"] == p],
                         key=lambda n: (not clases[n]["interfaz"], "Exception" in n, n))
        if nombres:
            filas.append(nombres)
    return filas


def columnas_por_paquete(clases, orden_paquetes, max_por_columna=6):
    columnas = []
    for p in orden_paquetes:
        nombres = sorted([c for c in clases if clases[c]["paquete"] == p],
                         key=lambda n: (not clases[n]["interfaz"], "Exception" in n, n))
        for i in range(0, len(nombres), max_por_columna):
            columnas.append(nombres[i:i + max_por_columna])
    return columnas


def generar_inventario(tag, archivo, titulo, posiciones, orden):
    clases = leer_proyecto(archivos_en_tag(tag, "src"))
    for c in clases.values():
        if c["paquete"] == "(raiz)":
            c["paquete"] = "(raiz)"
    m = Mdj(f"Inventario MVC - {titulo}", "Modelo de diseno")
    for c in clases.values():
        m.clasificador(c)
    _, _, deps = diagrama_paquetes(m, clases, f"Paquetes - {titulo}", posiciones)
    rels = relaciones_de_clases(m, clases)
    _, layout, aristas = diagrama_clases_filas(m, clases, rels, f"Clases - {titulo}",
                                               filas_por_paquete(clases, orden))
    m.guardar(RAIZ / "diagramas" / archivo)
    return {"archivo": f"diagramas/{archivo}", "clases": clases, "deps_paquetes": deps,
            "layout": layout, "aristas": aristas, "posiciones": posiciones, "objetos": m.contador}


def generar_biblioteca():
    clases = leer_proyecto(archivos_en_disco("ejercicio8/src"))
    m = Mdj("Biblioteca MVC - Proyecto integrador", "Modelo de diseno")
    for c in clases.values():
        m.clasificador(c)
    posiciones = {"(raiz)": (330, 30), "vista": (60, 200), "controlador": (330, 200),
                  "persistencia": (600, 200), "soporte": (600, 380), "modelo": (330, 380)}
    _, _, deps = diagrama_paquetes(m, clases, "Paquetes - Biblioteca", posiciones)
    rels = relaciones_de_clases(m, clases)
    por_paquete = {}
    for p in ["modelo", "vista", "controlador", "persistencia", "soporte", "(raiz)"]:
        nombres = {c for c in clases if clases[c]["paquete"] == p}
        # se incluyen las interfaces externas que el paquete implementa, para mostrar la realizacion
        externos = {i for c in nombres for i in clases[c]["implementa"] if i in clases} - nombres
        if p == "modelo":
            cols = [["Biblioteca", "ObservadorBiblioteca", "RepositorioBiblioteca"],
                    ["Libro", "Socio", "Prestamo"], ["EstadoLibro", "EstadoSocio"],
                    ["ReglaNegocioException", "DatosInvalidosException", "NoEncontradoException",
                     "LimitePrestamosException", "LibroNoDisponibleException", "SocioConMultaException"]]
        else:
            cols = [sorted(externos)] + columnas_por_paquete({k: clases[k] for k in nombres}, [p], 4)
        # en el diagrama del modelo solo se dibujan las relaciones estructurales para que sea legible
        rels_p = rels if p != "modelo" else [r for r in rels if r[2]["_type"] != "UMLDependency"
                                              or r[1] in ("RepositorioBiblioteca", "ObservadorBiblioteca",
                                                          "Libro", "Socio", "Prestamo")
                                              and r[0] in ("Biblioteca", "EstadoLibro", "EstadoSocio")]
        _, layout, aristas = diagrama_clases(m, clases, rels_p, f"Clases - paquete {p}", nombres | externos,
                                             cols, mostrar_paquete=bool(externos) or p == "(raiz)",
                                             por_defecto=False)
        por_paquete[p] = (layout, aristas)

    participantes = [("bibliotecario", "Usuario"), ("ventana", "VistaSwing"), ("ctrlVentana", "ControladorSwing"),
                     ("casos", "ControladorBiblioteca"), ("biblioteca", "Biblioteca"),
                     ("repositorio", "RepositorioBiblioteca"), ("ctrlConsola", "ControladorConsola"),
                     ("consola", "VistaConsola")]
    mensajes = [
        (0, 1, "clic en Prestar (ISBN, socio)", "asynchSignal"),
        (1, 2, "actionPerformed(e)", "synchCall"),
        (2, 1, "getPrestamoIsbn(), getPrestamoSocio()", "synchCall"),
        (2, 3, "prestar(isbn, socioId)", "synchCall"),
        (3, 4, "prestar(isbn, socioId)", "synchCall"),
        (4, 4, "validar R4 multa, R2 disponible, R1 maximo 3", "synchCall"),
        (4, 5, "insertarPrestamo(prestamo)", "synchCall"),
        (4, 4, "notificar()", "synchCall"),
        (4, 2, "bibliotecaCambiada()", "synchCall"),
        (2, 3, "refrescar()", "synchCall"),
        (3, 1, "mostrarLibros(estados), mostrarSocios(estados)", "synchCall"),
        (4, 6, "bibliotecaCambiada()", "synchCall"),
        (6, 7, "mostrarAvisoCambio(), mostrarLibros(...)", "synchCall"),
        (4, 3, "prestamo", "reply"),
        (3, 1, "mostrarExito(\"Prestado ... vence dd/MM/aaaa\")", "synchCall"),
    ]
    actor = m.nuevo("UMLActor", m.modelo, name="Usuario")
    m.modelo["ownedElements"].append(actor)
    _, seq = m.secuencia("Prestar un libro desde la ventana", participantes, mensajes, {"Usuario": actor})
    destino = RAIZ / "ejercicio8" / "diagramas" / "Ej8_Biblioteca.mdj"
    m.guardar(destino)
    return {"archivo": "ejercicio8/diagramas/Ej8_Biblioteca.mdj", "clases": clases, "deps_paquetes": deps,
            "posiciones": posiciones, "por_paquete": por_paquete, "secuencia": seq, "objetos": m.contador}


POS_INVENTARIO = {"(raiz)": (290, 30), "vista": (60, 190), "controlador": (520, 190), "modelo": (290, 350)}
POS_INVENTARIO_DAO = {"(raiz)": (290, 30), "vista": (60, 190), "controlador": (520, 190), "modelo": (290, 350),
                      "persistencia": (60, 470)}


def generar_todo():
    resultados = [
        generar_inventario("ej1", "Ej1_Paquetes_MVC.mdj", "Ejercicio 1", POS_INVENTARIO,
                           ["(raiz)", "controlador", "vista", "modelo"]),
        generar_inventario("ej3", "Ej3_Dos_Vistas.mdj", "Ejercicio 3", POS_INVENTARIO,
                           ["(raiz)", "controlador", "vista", "modelo"]),
        generar_inventario("ej6", "Ej6_Persistencia_DAO.mdj", "Ejercicio 6", POS_INVENTARIO_DAO,
                           ["(raiz)", "controlador", "vista", "modelo", "persistencia"]),
        generar_biblioteca(),
    ]
    for r in resultados:
        datos = json.loads((RAIZ / r["archivo"]).read_text(encoding="utf-8"))
        n_ids, n_refs = validar(datos)
        desde_modelo = [d for d in r["deps_paquetes"] if d[0] == "modelo"]
        print(f"{r['archivo']}: {len(r['clases'])} clases, {n_ids} objetos, {n_refs} referencias OK; "
              f"dependencias entre paquetes: {r['deps_paquetes']}; salen del modelo: {desde_modelo or 'ninguna'}")
    return resultados


if __name__ == "__main__":
    generar_todo()
