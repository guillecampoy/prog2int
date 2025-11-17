package prog2int.Main;

import prog2int.ui.console.ContextColor;
import prog2int.ui.console.UtilsColor;

/**
 * Clase utilitaria para mostrar el menú de la aplicación.
 * Solo contiene métodos estáticos de visualización (no tiene estado).
 *
 * Responsabilidades:
 * - Mostrar el menú principal con todas las opciones disponibles
 * - Formatear la salida de forma consistente
 *
 * Patrón: Utility class (solo métodos estáticos, no instanciable)
 *
 * IMPORTANTE: Esta clase NO lee entrada del usuario.
 * Solo muestra el menú. AppMenu es responsable de leer la opción.
 */
public class MenuDisplay {
    /**
     * Muestra el menú principal con todas las opciones CRUD.
     *
     * Opciones de Pacientes (1-4):
     * 1. Crear paciente: Permite crear paciente con historia clínica opcional
     * 2. Listar pacientes: Lista todas o busca por nombre/apellido
     * 3. Actualizar paciente: Actualiza datos de paciente y opcionalmente su historia clínica
     * 4. Eliminar paciente: Soft delete de paciente (NO elimina historia clínica asociada)
     *
     * Opciones de Historia clínica (5-10):
     * 5. Crear historia clínica: Crea historia clínica independiente (sin asociar a paciente)
     * 6. Listar historias clínicas: Lista todas las historias clínicas activas
     * 7. Actualizar historia clínica por ID: Actualiza historia clínica directamente (afecta a paciente)
     * 8. Eliminar historia clínica por ID: PELIGROSO - puede dejar FKs huérfanas (RN-029)
     * 9. Actualizar historia clínica por ID de paciente: Busca paciente primero, luego actualiza su historia clínica
     * 10. Eliminar historia clínica por ID de paciente: SEGURO - actualiza FK primero, luego elimina (RN-029)
     *
     * Opción de salida:
     * 0. Salir: Termina la aplicación
     *
     * Formato:
     * - Separador visual "========= MENU ========="
     * - Lista numerada clara
     * - Prompt "Ingrese una opcion: " sin salto de línea (espera input)
     *
     * Nota: Los números de opción corresponden al switch en AppMenu.processOption().
     */
    public static void mostrarMenuPrincipal() {
        UtilsColor.imprimirBloque(ContextColor.INFO, "\n========= MENU =========");
        UtilsColor.imprimirOpcionDefault("1. Crear paciente");
        UtilsColor.imprimirOpcionDefault("2. Listar pacientes");
        UtilsColor.imprimirOpcionDefault("3. Actualizar paciente");
        UtilsColor.imprimirOpcionDefault("4. Eliminar paciente");
        UtilsColor.imprimirOpcionDefault("5. Crear historia clínica");
        UtilsColor.imprimirOpcionDefault("6. Listar historias clínicas");
        UtilsColor.imprimirOpcionDefault("7. Actualizar historia clínica por ID");
        UtilsColor.imprimirOpcionDefault("8. Eliminar historia clínica por ID");
        UtilsColor.imprimirOpcionDefault("9. Actualizar historia clínica por ID de paciente");
        UtilsColor.imprimirOpcionDefault("10. Eliminar historia clínica por ID de paciente");
        UtilsColor.imprimirOpcionDefault("0. Salir");
        UtilsColor.imprimirBloque(ContextColor.WARNING,"Ingrese una opción: ",'n');
    }
}