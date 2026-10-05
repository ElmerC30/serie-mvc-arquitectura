import java.util.*;

public class SistemaInventario {
    static Scanner sc = new Scanner(System.in);
    static List<String>  nombres    = new ArrayList<>();
    static List<Integer> cantidades = new ArrayList<>();
    static List<Double>  precios    = new ArrayList<>();

    public static void main(String[] args) {
        int op;
        do {
            System.out.println("\n1) Agregar  2) Vender  3) Listar  4) Valor total  0) Salir");
            System.out.print("Opcion: ");
            op = Integer.parseInt(sc.nextLine());

            if (op == 1) {
                System.out.print("Nombre: ");
                String n = sc.nextLine();
                System.out.print("Cantidad: ");
                int c = Integer.parseInt(sc.nextLine());
                System.out.print("Precio: ");
                double pr = Double.parseDouble(sc.nextLine());
                if (n.isEmpty() || c < 0 || pr <= 0) {
                    System.out.println("Datos invalidos");
                } else {
                    nombres.add(n); cantidades.add(c); precios.add(pr);
                    System.out.println("Agregado.");
                }
            } else if (op == 2) {
                System.out.print("Producto: ");
                String n = sc.nextLine();
                int i = nombres.indexOf(n);
                if (i < 0) {
                    System.out.println("No existe");
                } else {
                    System.out.print("Cantidad a vender: ");
                    int c = Integer.parseInt(sc.nextLine());
                    if (c > cantidades.get(i)) {
                        System.out.println("Stock insuficiente");
                    } else {
                        cantidades.set(i, cantidades.get(i) - c);
                        System.out.println("Venta ok. Quedan " + cantidades.get(i));
                    }
                }
            } else if (op == 3) {
                for (int i = 0; i < nombres.size(); i++)
                    System.out.printf("%-15s %4d  $%.2f%n",
                                      nombres.get(i), cantidades.get(i), precios.get(i));
            } else if (op == 4) {
                double t = 0;
                for (int i = 0; i < nombres.size(); i++)
                    t += cantidades.get(i) * precios.get(i);
                System.out.println("Valor del inventario: $" + t);
            }
        } while (op != 0);
    }
}
