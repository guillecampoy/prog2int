package prog2int.Main;

import prog2int.Models.Paciente;
import prog2int.Models.HistoriaClinica;
import prog2int.Models.HistoriaClinica.GrupoSanguineo;
import prog2int.Services.PacienteServiceImpl;
import prog2int.Services.HistoriaClinicaServiceImpl;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Controlador de las operaciones del menú (Menu Handler).
 * Gestiona toda la lógica de interacción con el usuario para operaciones CRUD.
 *
 * Responsabilidades:
 * - Capturar entrada del usuario desde consola (Scanner)
 * - Validar entrada básica (conversión de tipos, valores vacíos)
 * - Invocar servicios de negocio (PacienteService, HistoriaClinicaService)
 * - Mostrar resultados y mensajes de error al usuario
 * - Coordinar operaciones complejas (crear paciente con historia clínica, etc.)
 *
 * Patrón: Controller (MVC) - capa de presentación en arquitectura de 4 capas
 * Arquitectura: Main → Service → DAO → Models
 *
 * IMPORTANTE: Este handler NO contiene lógica de negocio.
 * Todas las validaciones de negocio están en la capa Service.
 */
public class MenuHandler {
    /**
     * Scanner compartido para leer entrada del usuario.
     * Inyectado desde AppMenu para evitar múltiples Scanners de System.in.
     */
    private final Scanner scanner;

    private final PacienteServiceImpl pacienteService;
    private final HistoriaClinicaServiceImpl historiaClinicaService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que las dependencias no sean null (fail-fast).
     *
     * @param scanner Scanner compartido para entrada de usuario
     * @param pacienteService Servicio de pacientes
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public MenuHandler(Scanner scanner, PacienteServiceImpl pacienteService, HistoriaClinicaServiceImpl historiaClinicaService) {
        if (scanner == null || pacienteService == null || historiaClinicaService == null) {
            throw new IllegalArgumentException("Dependencias no pueden ser null");
        }
        this.scanner = scanner;
        this.pacienteService = pacienteService;
        this.historiaClinicaService = historiaClinicaService;
    }

    /**
     * Opción 1: Crear nueva paciente (con historia clínica opcional).
     *
     * Flujo:
     * 1. Solicita nombre, apellido y DNI
     * 2. Pregunta si desea agregar historia clínica
     * 3. Si sí, captura calle y número
     * 4. Crea objeto paciente y opcionalmente historia clínica
     * 5. Invoca PacienteService.insertar() que:
     *    - Valida datos (nombre, apellido, DNI obligatorios)
     *    - Valida DNI único (RN-001)
     *    - Si hay historia clínica, lo inserta primero (obtiene ID)
     *    - Inserta paciente con FK historia clínica_id correcta
     *
     * Input trimming: Aplica .trim() a todas las entradas (patrón consistente).
     *
     * Manejo de errores:
     * - IllegalArgumentException: Validaciones de negocio (muestra mensaje al usuario)
     * - SQLException: Errores de BD (muestra mensaje al usuario)
     * - Todos los errores se capturan y muestran, NO se propagan al menú principal
     */
    public void crearPaciente() {
        try {
            System.out.print("Nombre: ");
            String nombre = scanner.nextLine().trim();
            System.out.print("Apellido: ");
            String apellido = scanner.nextLine().trim();
            System.out.print("DNI: ");
            String dni = scanner.nextLine().trim();
            System.out.print("Fecha de nacimiento (YYYY-MM-DD): ");
            LocalDate fechaNacimiento = LocalDate.parse(scanner.nextLine().trim());

            HistoriaClinica historiaClinica = null;
            System.out.print("¿Desea agregar historia clínica? (s/n): ");
            if (scanner.nextLine().equalsIgnoreCase("s")) {
                historiaClinica = crearHistoriaClinica();
            }

            Paciente paciente = new Paciente(0, nombre, apellido, dni, fechaNacimiento, false, historiaClinica);
            pacienteService.insertar(paciente);
            System.out.println("Paciente creado exitosamente con ID: " + paciente.getId());
        } catch (Exception e) {
            System.err.println("\nError al crear paciente: " + e.getMessage());
        }
    }

    /**
     * Opción 2: Listar pacientes (todas o filtradas por nombre/apellido).
     *
     * Submenú:
     * 1. Listar todas las pacientes activas (getAll)
     * 2. Buscar por nombre o apellido con LIKE (buscarPorNombreApellido)
     *
     * Muestra:
     * - ID, Nombre, Apellido, DNI
     * - historia clínica (si tiene): Calle Número
     *
     * Manejo de casos especiales:
     * - Si no hay pacientes: Muestra "No se encontraron pacientes"
     * - Si la paciente no tiene historia clínica: Solo muestra datos de paciente
     *
     * Búsqueda por nombre/apellido:
     * - Usa pacienteDAO.buscarPorNombreApellido() que hace LIKE '%filtro%'
     * - Insensible a mayúsculas en MySQL (depende de collation)
     * - Busca en nombre O apellido
     */
    public void listarPacientes() {
        try {
            System.out.print("¿Desea (1) listar todos o (2) buscar por nombre/apellido? Ingrese opcion: ");
            int subopcion = Integer.parseInt(scanner.nextLine());

            List<Paciente> pacientes;
            if (subopcion == 1) {
                pacientes = pacienteService.getAll();
            } else if (subopcion == 2) {
                System.out.print("Ingrese texto a buscar: ");
                String filtro = scanner.nextLine().trim();
                pacientes = pacienteService.buscarPorNombreApellido(filtro);
            } else {
                System.out.println("Opcion invalida.");
                return;
            }

            if (pacientes.isEmpty()) {
                System.out.println("No se encontraron pacientes.");
                return;
            }

            for (Paciente p : pacientes) {
                System.out.println("ID: " + p.getId() + ", Nombre: " + p.getNombre() +
                        ", Apellido: " + p.getApellido() + ", DNI: " + p.getDni());
                if (p.getHistoriaClinica() != null) {
                    System.out.println("   Historia: " + p.getHistoriaClinica().getNroHistoria() +
                            ", Grupo: " + p.getHistoriaClinica().getGrupoSanguineo());
                }
            }
        } catch (Exception e) {
            System.err.println("Error al listar pacientes: " + e.getMessage());
        }
    }

    /**
     * Opción 3: Actualizar paciente existente.
     *
     * Flujo:
     * 1. Solicita ID del paciente
     * 2. Obtiene paciente actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Nombre (Enter para mantener actual)
     *    - Apellido (Enter para mantener actual)
     *    - DNI (Enter para mantener actual)
     * 4. Llama a actualizar historia clínicaDepaciente() para manejar cambios en historia clínica
     * 5. Invoca PacienteService.actualizar() que valida:
     *    - Datos obligatorios (nombre, apellido, DNI)
     *    - DNI único (RN-001), excepto para la misma paciente
     *
     * Patrón "Enter para mantener":
     * - Lee input con scanner.nextLine().trim()
     * - Si isEmpty() → NO actualiza el campo (mantiene valor actual)
     * - Si tiene valor → Actualiza el campo
     *
     * IMPORTANTE: Esta operación NO actualiza la historia clínica directamente.
     * La historia clínica se maneja en actualizar historia clínica de paciente que puede:
     * - Agregar nueva historia clínica si el paciente no tenía
     * - Dejar historia clínica sin cambios
     */
    public void actualizarPaciente() {
        try {
            System.out.print("ID del paciente a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            Paciente p = pacienteService.getById(id);

            if (p == null) {
                System.out.println("Paciente no encontrado.");
                return;
            }

            System.out.print("Nuevo nombre (actual: " + p.getNombre() + ", Enter para mantener): ");
            String nombre = scanner.nextLine().trim();
            if (!nombre.isEmpty()) p.setNombre(nombre);

            System.out.print("Nuevo apellido (actual: " + p.getApellido() + ", Enter para mantener): ");
            String apellido = scanner.nextLine().trim();
            if (!apellido.isEmpty()) p.setApellido(apellido);

            System.out.print("Nuevo DNI (actual: " + p.getDni() + ", Enter para mantener): ");
            String dni = scanner.nextLine().trim();
            if (!dni.isEmpty()) p.setDni(dni);

            pacienteService.actualizar(p);
            System.out.println("Paciente actualizado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar paciente: " + e.getMessage());
        }
    }

    /**
     * Opción 4: Eliminar paciente (soft delete).
     *
     * Flujo:
     * 1. Solicita ID del paciente
     * 2. Invoca PacienteService.eliminar() que:
     *    - Marca paciente.eliminado = TRUE
     *    - NO elimina la historia clínica asociada (RN-037)
     * Si se quiere eliminar también la historia clínica:
     * - Usar opción 10: "Eliminar historia clínica de un paciente" (eliminar historia clínica por paciente)
     * - Esa opción primero desasocia la historia clínica, luego lo elimina (seguro)
     */
    public void eliminarPaciente() {
        try {
            System.out.print("ID del paciente a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            pacienteService.eliminar(id);
            System.out.println("Paciente eliminado exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar paciente: " + e.getMessage());
        }
    }

    /**
     * Opción 5: Crear historia clínica independiente (sin asociar a paciente).
     *
     * Flujo:
     * 1. Llama a crearhistoria clínica() para capturar calle y número
     * 2. Invoca HistoriaClinicaService.insertar() que:
     *    - Valida calle y número obligatorios (RN-023)
     *    - Inserta en BD y asigna ID autogenerado
     * 3. Muestra ID generado
     *
     * Uso típico:
     * - Crear historia clínica que luego se asignará a varias pacientes (opción 7)
     * - Pre-cargar historia clínicas en la BD
     */
    public void crearHistoriaClinicaIndependiente() {
        try {
            HistoriaClinica hc = crearHistoriaClinica();
            historiaClinicaService.insertar(hc);
            System.out.println("Historia clínica creada con ID: " + hc.getId());
        } catch (Exception e) {
            System.err.println("Error al crear historia clínica: " + e.getMessage());
        }
    }

    /**
     * Opción 6: Listar todas las historias clínicas activas.
     *
     * Uso típico:
     * - Ver historia clínicas disponibles antes de asignar a paciente (opción 7)
     * - Consultar ID de historia clínica para actualizar (opción 9) o eliminar (opción 8)
     *
     * Nota: Solo muestra historias clínicas con eliminado=FALSE (soft delete).
     */
    public void listarHistoriasClinicas() {
        try {
            List<HistoriaClinica> historias = historiaClinicaService.getAll();
            if (historias.isEmpty()) {
                System.out.println("No se encontraron historias clínicas.");
                return;
            }
            for (HistoriaClinica h : historias) {
                System.out.println("ID: " + h.getId() + ", Nro: " + h.getNroHistoria() + ", Grupo: " + h.getGrupoSanguineo());
            }
        } catch (Exception e) {
            System.err.println("Error al listar historias clínicas: " + e.getMessage());
        }
    }

    /**
     * Opción 9: Actualizar historia clínica por ID.
     *
     * Flujo:
     * 1. Solicita ID de la historia clínica
     * 2. Obtiene historia clínica actual de la BD
     * 3. Muestra valores actuales y permite actualizar:
     *    - Calle (Enter para mantener actual)
     *    - Número (Enter para mantener actual)
     * 4. Invoca HistoriaClinicaService.actualizar()
     *  1. Crear nuevo historia clínica (opción 5)
     *  2. Asignar a la paciente (opción 7)
     */
    public void actualizarHistoriaClinica() {
        try {
            System.out.print("ID de la historia clínica a actualizar: ");
            int id = Integer.parseInt(scanner.nextLine());
            HistoriaClinica h = historiaClinicaService.getById(id);

            if (h == null) {
                System.out.println("Historia clínica no encontrada.");
                return;
            }

            System.out.print("Antecedentes (actual: " + h.getAntecedentes() + ", Enter para mantener): ");
            String antecedentes = scanner.nextLine().trim();
            if (!antecedentes.isEmpty()) h.setAntecedentes(antecedentes);

            historiaClinicaService.actualizar(h);
            System.out.println("Historia clínica actualizada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al actualizar historia clínica: " + e.getMessage());
        }
    }

    /**
     * Opción 8: Eliminar historia clínica por ID (PELIGROSO - soft delete directo).
     *
     * ⚠️ PELIGRO (RN-029): Este método NO verifica si hay pacientes asociadas.
     * Si hay pacientes con FK a este historia clínica, quedarán con referencia huérfana.
     *
     * Flujo:
     * 1. Solicita ID del historia clínica
     * 2. Invoca HistoriaClinicaService.eliminar() directamente
     * 3. Marca historia clínica.eliminado = TRUE
     *
     * Problemas potenciales:
     * - pacientes con historia clínica_id apuntando a historia clínica "eliminado"
     * - Datos inconsistentes en la BD
     *
     * ALTERNATIVA SEGURA: Opción 10 (eliminarhistoria clínicaPorpaciente)
     * - Primero desasocia historia clínica del paciente (historia clínica_id = NULL)
     * - Luego elimina el historia clínica
     * - Garantiza consistencia
     *
     * Uso válido:
     * - Cuando se está seguro de que el historia clínica NO tiene pacientes asociadas
     * - Limpiar historia clínicas creados por error
     */
    public void eliminarHistoriaClinica() {
        try {
            System.out.print("ID de la historia clínica a eliminar: ");
            int id = Integer.parseInt(scanner.nextLine());
            historiaClinicaService.eliminar(id);
            System.out.println("Historia clínica eliminada exitosamente.");
        } catch (Exception e) {
            System.err.println("Error al eliminar historia clínica: " + e.getMessage());
        }
    }

    /**
     * Opción 7: Actualizar historia clínica de un paciente específico.
     *
     * Flujo:
     * 1. Solicita ID del paciente
     * 2. Verifica que el paciente exista y tenga historia clínica
     * 3. Muestra valores actuales de historia clínica
     * 4. Permite actualizar
     * 5. Invoca HistoriaClinicaService.actualizar()
     *
     * ⚠️ IMPORTANTE (RN-040): Esta operación actualiza la historia clínica.
     *
     * Diferencia con opción 9 (actualizar historia clínicaPorId):
     * - Esta opción: Busca paciente primero, luego actualiza su historia clínica
     * - Opción 9: Actualiza historia clínica directamente por ID
     *
     * Ambas tienen el mismo efecto (RN-040): afectan a TODAS las pacientes
     * que comparten la historia clínica.
     */
    public void buscarPacientePorDni() {
        try {
            System.out.print("DNI del paciente: ");
            String dni = scanner.nextLine().trim();
            Paciente p = pacienteService.buscarPorDni(dni);

            if (p == null) {
                System.out.println("Paciente no encontrado.");
                return;
            }

            System.out.println("ID: " + p.getId() + ", Nombre: " + p.getNombre() +
                    ", Apellido: " + p.getApellido() + ", DNI: " + p.getDni());
            if (p.getHistoriaClinica() != null) {
                System.out.println("   Historia: " + p.getHistoriaClinica().getNroHistoria());
            }
        } catch (Exception e) {
            System.err.println("Error al buscar paciente: " + e.getMessage());
        }
    }

    /**
     * Opción 10: Eliminar historia clínica de un paciente (MÉTODO SEGURO - RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Solicita ID del paciente
     * 2. Verifica que la paciente exista y tenga historia clínica
     * 3. Invoca PacienteService.eliminar historia clínicaDepaciente() que:
     *    a. Desasocia historia clínica de paciente (paciente.historia clínica = null)
     *    b. Actualiza paciente en BD (historia clínica_id = NULL)
     *    c. Elimina el historia clínica (ahora no hay FKs apuntando a él)
     *
     * Ventaja sobre opción 8 (eliminar historia clínicaPorId):
     * - Garantiza consistencia: Primero actualiza FK, luego elimina
     * - NO deja referencias huérfanas
     * - Implementa eliminación segura recomendada en RN-029
     *
     * Este es el método RECOMENDADO para eliminar historias clínicas en producción.
     */
    public void buscarHistoriaPorNumero() {
        try {
            System.out.print("Número de historia: ");
            String nro = scanner.nextLine().trim();
            List<HistoriaClinica> historias = historiaClinicaService.getAll();
            HistoriaClinica h = historias.stream()
                .filter(hc -> hc.getNroHistoria().equals(nro))
                .findFirst()
                .orElse(null);

            if (h == null) {
                System.out.println("Historia clínica no encontrada.");
                return;
            }

            System.out.println("ID: " + h.getId() + ", Nro: " + h.getNroHistoria() +
                    ", Grupo: " + h.getGrupoSanguineoSymbol());
        } catch (Exception e) {
            System.err.println("Error al buscar historia: " + e.getMessage());
        }
    }

    /**
     * Método auxiliar privado: Crea un objeto historia clínica.
     *
     * Flujo:
     * 1. Solicita número (con trim)
     * 2. Solicita grupo sanguíneo (con trim)
     * 3. Solicita grupo antecedentes (con trim)
     * 3. Crea objeto historia clínica con ID=0 (será asignado por BD al insertar)
     *
     * Usado por:
     * - crearpaciente(): Para agregar historia clínica al crear paciente
     * - crearhistoria clínicaIndependiente(): Para crear historia clínica sin asociar
     * - actualizarhistoria clínicaDepaciente(): Para agregar historia clínica a paciente sin historia clínica
     *
     * Nota: NO persiste en BD, solo crea el objeto en memoria.
     * El caller es responsable de insertar el historia clínica.
     *
     * @return historia clínica nuevo (no persistido, ID=0)
     */
    private HistoriaClinica crearHistoriaClinica() {
        System.out.print("Número de historia (HC-XXXX): ");
        String nro = scanner.nextLine().trim();
        System.out.print("Grupo sanguíneo (A+, A-, B+, B-, AB+, AB-, O+, O-): ");
        String grupoStr = scanner.nextLine().trim();
        GrupoSanguineo grupo = parseGrupoSanguineo(grupoStr);
        System.out.print("Antecedentes: ");
        String antecedentes = scanner.nextLine().trim();
        return new HistoriaClinica(0, false, nro, grupo, antecedentes, null, null);
    }

    private GrupoSanguineo parseGrupoSanguineo(String simbolo) {
        for (GrupoSanguineo gs : GrupoSanguineo.values()) {
            if (gs.getSimbolo().equalsIgnoreCase(simbolo)) {
                return gs;
            }
        }
        return GrupoSanguineo.O_POSITIVO;
    }

    /**
     * Método auxiliar privado: Maneja actualización de historia clínica dentro de actualizar paciente.
     *
     * Casos:
     * 1. paciente TIENE historia clínica:
     *    - Pregunta si desea actualizar
     *    - Si sí, permite cambiar calle y número (Enter para mantener)
     *    - Actualiza historia clínica en BD (afecta a TODAS las pacientes que lo comparten)
     *
     * 2. paciente NO TIENE historia clínica:
     *    - Pregunta si desea agregar uno
     *    - Si sí, captura calle y número con crearhistoria clínica()
     *    - Inserta historia clínica en BD (obtiene ID)
     *    - Asocia historia clínica a la paciente
     *
     * Usado exclusivamente por actualizarpaciente() (opción 3).
     *
     * IMPORTANTE: El parámetro paciente se modifica in-place (sethistoria clínica).
     * El caller debe invocar PacienteService.actualizar() después para persistir.
     *
     * @param p paciente a la que se le actualizará/agregará historia clínica
     * @throws Exception Si hay error al insertar/actualizar historia clínica
     */

}