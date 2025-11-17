package prog2int.ui.console;

public class UtilsColor {
    private static final String RESET = "\u001B[0m";
    private static final String VERDE = "\u001B[32m";
    private static final String ROJO = "\u001B[31m";
    private static final String AMARILLO = "\u001B[33m";
    private static final String AZUL = "\u001B[34m";
    private static final String BLANCO = "\u001B[37m";

    public static void imprimirBloque(ContextColor contexto, String mensaje) {
        String color = switch (contexto) {
            case SUCCESS -> VERDE;
            case ERROR -> ROJO;
            case WARNING -> AMARILLO;
            case INFO -> AZUL;
            case DEFAULT -> BLANCO;
        };
        System.out.println(color + mensaje + RESET);
    }

    public static void imprimirOpcionDefault(String opcion) {
        UtilsColor.imprimirBloque(ContextColor.DEFAULT,opcion);
    }

    public static void imprimirBloque(ContextColor contexto, String mensaje, char salto) {
        String color = switch (contexto) {
            case SUCCESS -> VERDE;
            case ERROR -> ROJO;
            case WARNING -> AMARILLO;
            case INFO -> AZUL;
            case DEFAULT -> BLANCO;
        };

        if (salto == 'n') {
            System.out.print(color + mensaje + RESET);
        } else {
            System.out.println(color + mensaje + RESET);
        }
    }
    public static void imprimirEncabezado() {
        System.out.println();
        UtilsColor.imprimirBloque(ContextColor.INFO,"======Resultados======");
    }
}
