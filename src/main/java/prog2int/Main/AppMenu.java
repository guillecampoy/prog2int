package prog2int.Main;

import java.util.Scanner;
import prog2int.Dao.PacienteDAO;
import prog2int.Dao.HistoriaClinicaDAO;
import prog2int.Services.PacienteServiceImpl;
import prog2int.Services.HistoriaClinicaServiceImpl;

/**
 * Orquestador principal del menú de la aplicación.
 * Gestiona el ciclo de vida del menú y coordina todas las dependencias.
 *
 * Responsabilidades:
 * - Crear y gestionar el Scanner único (evita múltiples instancias de System.in)
 * - Inicializar toda la cadena de dependencias (DAOs → Services → Handler)
 * - Ejecutar el loop principal del menú
 * - Manejar la selección de opciones y delegarlas a MenuHandler
 * - Cerrar recursos al salir (Scanner)
 *
 * Patrón: Application Controller + Dependency Injection manual
 * Arquitectura: Punto de entrada que ensambla las 4 capas (Main → Service → DAO → Models)
 *
 * IMPORTANTE: Esta clase NO tiene lógica de negocio ni de UI.
 * Solo coordina y delega.
 */
public class AppMenu {
    /**
     * Scanner único compartido por toda la aplicación.
     * IMPORTANTE: Solo debe haber UNA instancia de Scanner(System.in).
     * Múltiples instancias causan problemas de buffering de entrada.
     */
    private final Scanner scanner;

    /**
     * Handler que ejecuta las operaciones del menú.
     * Contiene toda la lógica de interacción con el usuario.
     */
    private final MenuHandler menuHandler;

    /**
     * Flag que controla el loop principal del menú.
     * Se setea a false cuando el usuario selecciona "0 - Salir".
     */
    private boolean running;

    /**
     * Constructor que inicializa la aplicación.
     *
     * Flujo de inicialización:
     * 1. Crea Scanner único para toda la aplicación
     * 2. Crea cadena de dependencias (DAOs → Services) mediante createPersonaService()
     * 3. Crea MenuHandler con Scanner y PersonaService
     * 4. Setea running=true para iniciar el loop
     *
     * Patrón de inyección de dependencias (DI) manual:
     * - DomicilioDAO (sin dependencias)
     * - PersonaDAO (depende de DomicilioDAO)
     * - DomicilioServiceImpl (depende de DomicilioDAO)
     * - PersonaServiceImpl (depende de PersonaDAO y DomicilioServiceImpl)
     * - MenuHandler (depende de Scanner y PersonaServiceImpl)
     *
     * Esta inicialización garantiza que todas las dependencias estén correctamente conectadas.
     */
    public AppMenu() {
        this.scanner = new Scanner(System.in);
        HistoriaClinicaDAO historiaDAO = new HistoriaClinicaDAO();
        PacienteDAO pacienteDAO = new PacienteDAO(historiaDAO);
        HistoriaClinicaServiceImpl historiaService = new HistoriaClinicaServiceImpl(historiaDAO);
        PacienteServiceImpl pacienteService = new PacienteServiceImpl(pacienteDAO, historiaService);
        this.menuHandler = new MenuHandler(scanner, pacienteService, historiaService);
        this.running = true;
    }

    /**
     * Punto de entrada de la aplicación Java.
     * Crea instancia de AppMenu y ejecuta el menú principal.
     *
     * @param args Argumentos de línea de comandos (no usados)
     */
    public static void main(String[] args) {
        AppMenu app = new AppMenu();
        app.run();
    }

    /**
     * Loop principal del menú.
     *
     * Flujo:
     * 1. Mientras running==true:
     *    a. Muestra menú con MenuDisplay.mostrarMenuPrincipal()
     *    b. Lee opción del usuario (scanner.nextLine())
     *    c. Convierte a int (puede lanzar NumberFormatException)
     *    d. Procesa opción con processOption()
     * 2. Si el usuario ingresa texto no numérico: Muestra mensaje de error y continúa
     * 3. Cuando running==false (opción 0): Sale del loop y cierra Scanner
     *
     * Manejo de errores:
     * - NumberFormatException: Captura entrada no numérica (ej: "abc")
     * - Muestra mensaje amigable y NO termina la aplicación
     * - El usuario puede volver a intentar
     *
     * IMPORTANTE: El Scanner se cierra al salir del loop.
     * Cerrar Scanner(System.in) cierra System.in para toda la aplicación.
     */
    public void run() {
        while (running) {
            try {
                MenuDisplay.mostrarMenuPrincipal();
                int opcion = Integer.parseInt(scanner.nextLine());
                processOption(opcion);
            } catch (NumberFormatException e) {
                System.out.println("Entrada invalida. Por favor, ingrese un numero.");
            }
        }
        scanner.close();
    }

    /**
     * Procesa la opción seleccionada por el usuario y delega a MenuHandler.
     *
     * Switch expression (Java 14+) con operador arrow (->):
     * - Más conciso que switch tradicional
     * - No requiere break (cada caso es independiente)
     * - Permite bloques con {} para múltiples statements
     *
     * Mapeo de opciones (corresponde a MenuDisplay):
     * 1  → Crear persona (con domicilio opcional)
     * 2  → Listar personas (todas o filtradas)
     * 3  → Actualizar persona
     * 4  → Eliminar persona (soft delete)
     * 5  → Crear domicilio independiente
     * 6  → Listar domicilios
     * 7  → Actualizar domicilio por ID (afecta a todas las personas que lo comparten)
     * 8  → Eliminar domicilio por ID (PELIGROSO - puede dejar FKs huérfanas)
     * 9  → Actualizar domicilio de una persona (afecta a todas las personas que lo comparten)
     * 10 → Eliminar domicilio de una persona (SEGURO - actualiza FK primero)
     * 0  → Salir (setea running=false para terminar el loop)
     *
     * Opción inválida: Muestra mensaje y continúa el loop.
     *
     * IMPORTANTE: Todas las excepciones de MenuHandler se capturan dentro de los métodos.
     * processOption() NO propaga excepciones al caller (run()).
     *
     * @param opcion Número de opción ingresado por el usuario
     */
    private void processOption(int opcion) {
        switch (opcion) {
            case 1 -> menuHandler.crearPaciente();
            case 2 -> menuHandler.listarPacientes();
            case 3 -> menuHandler.buscarPacientePorDni();
            case 4 -> menuHandler.actualizarPaciente();
            case 5 -> menuHandler.eliminarPaciente();
            case 6 -> menuHandler.crearHistoriaClinicaIndependiente();
            case 7 -> menuHandler.listarHistoriasClinicas();
            case 8 -> menuHandler.actualizarHistoriaClinica();
            case 9 -> menuHandler.eliminarHistoriaClinica();
            case 10 -> menuHandler.buscarHistoriaPorNumero();
            case 0 -> {
                System.out.println("Saliendo...");
                running = false;
            }
            default -> System.out.println("Opcion no valida.");
        }
    }

    /**
     * Factory method que crea la cadena de dependencias de servicios.
     * Implementa inyección de dependencias manual.
     *
     * Orden de creación (bottom-up desde la capa más baja):
     * 1. DomicilioDAO: Sin dependencias, acceso directo a BD
     * 2. PersonaDAO: Depende de DomicilioDAO (inyectado en constructor)
     * 3. DomicilioServiceImpl: Depende de DomicilioDAO
     * 4. PersonaServiceImpl: Depende de PersonaDAO y DomicilioServiceImpl
     *
     * Arquitectura resultante (4 capas):
     * Main (AppMenu, MenuHandler)
     *   ↓
     * Service (PersonaServiceImpl, DomicilioServiceImpl)
     *   ↓
     * DAO (PersonaDAO, DomicilioDAO)
     *   ↓
     * Models (Persona, Domicilio, Base)
     *
     * ¿Por qué PersonaDAO necesita DomicilioDAO?
     * - Actualmente NO lo usa (inyección preparada para futuras operaciones)
     * - Podría usarse para operaciones transaccionales coordinadas
     *
     * ¿Por qué PersonaService necesita DomicilioService?
     * - Para insertar/actualizar domicilios al crear/actualizar personas
     * - Para eliminar domicilios de forma segura (eliminarDomicilioDePersona)
     *
     * Patrón: Factory Method para construcción de dependencias
     *
     * @return PersonaServiceImpl completamente inicializado con todas sus dependencias
     */

}