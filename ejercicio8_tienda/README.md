# Ejercicio 8 · Tienda de instrumentos musicales (MVC)

Dos ventanas sobre el mismo modelo: **Almacén** (inventario, alertas, entrada de mercancía) y
**Mostrador** (pedido, cotización con descuento de kit, cobro). Ambas se actualizan solas mediante
`ObservadorTienda`.

| Paquete | Contenido |
|---|---|
| `modelo` | `Tienda` (reglas), `Articulo`, `TipoArticulo`, `Familia`, `Ticket`, excepciones, `ArticuloDAO`, `ObservadorTienda` |
| `vista` | `VistaAlmacen` / `VistaMostrador` (interfaces), `VentanaAlmacen` / `VentanaMostrador` (Swing), `Formato` |
| `controlador` | `ControladorAlmacen`, `ControladorMostrador` |
| `persistencia` | `ArticuloDAOMemoria`, `ArticuloDAOArchivo` |

Reglas de negocio (todas en `modelo.Tienda`):

1. **Compatibilidad**: en un pedido con saxofón, boquillas y cañas deben ser de su misma familia.
2. **Stock mínimo**: cañas < 20, boquillas < 2 o saxofón agotado generan alerta de reabastecimiento.
3. **Kit de inicio**: por cada saxofón, 1 boquilla y hasta 10 cañas de su familia llevan 15 % de descuento.

Cambiar de persistencia: editar la línea marcada `// DAO` en `src/Main.java`.

```
probar.bat      compila y ejecuta las 33 pruebas JUnit 5 (usa ..\lib)
ejecutar.bat    abre las dos ventanas (datos en datos\inventario.csv)
python generar_diagramas.py         genera y valida diagramas\Ej8_TiendaInstrumentos.mdj
python generar_diagramas.py --png   además exporta cada diagrama a PNG con la CLI de StarUML
```

El `.mdj` contiene: diagrama de paquetes, un diagrama de clases por paquete (modelo, vista,
controlador, persistencia, raíz) y el diagrama de secuencia "Cobrar un kit en el mostrador".

Evidencias en `evidencias/` (salida de pruebas y capturas generadas con `test/EvidenciasTienda.java`).
