package prog2int.Services;

import prog2int.Models.Paciente;

import java.util.List;
import prog2int.Dao.PacienteDAO;
/**
 * Implementación del servicio de negocio para la entidad Persona.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio complejas.
 *
 * Responsabilidades:
 * - Validar datos de persona ANTES de persistir (RN-035: nombre, apellido, DNI obligatorios)
 * - Garantizar unicidad del DNI en el sistema (RN-001)
 * - COORDINAR operaciones entre Persona y Domicilio (transaccionales)
 * - Proporcionar métodos de búsqueda especializados (por DNI, nombre/apellido)
 * - Implementar eliminación SEGURA de domicilios (evita FKs huérfanas)
 *
 * Patrón: Service Layer con inyección de dependencias y coordinación de servicios
 */
public class PacienteServiceImpl implements GenericService<Paciente> {
    /**
     * DAO para acceso a datos de personas.
     * Inyectado en el constructor (Dependency Injection).
     */
    private final PacienteDAO pacienteDAO;

    /**
     * Servicio de domicilios para coordinar operaciones transaccionales.
     * IMPORTANTE: PersonaServiceImpl necesita DomicilioService porque:
     * - Una persona puede crear/actualizar su domicilio al insertarse/actualizarse
     * - El servicio coordina la secuencia: insertar domicilio → insertar persona
     * - Implementa eliminación segura: actualizar FK persona → eliminar domicilio
     */
    private final HistoriaClinicaServiceImpl historiaClinicaService;

    /**
     * Constructor con inyección de dependencias.
     * Valida que ambas dependencias no sean null (fail-fast).
     *
     * @param pacienteDAO DAO de personas (normalmente PersonaDAO)
     * @param historiaClinicaService Servicio de domicilios para operaciones coordinadas
     * @throws IllegalArgumentException si alguna dependencia es null
     */
    public PacienteServiceImpl(PacienteDAO pacienteDAO, HistoriaClinicaServiceImpl historiaClinicaService) {
        if (pacienteDAO == null) {
            throw new IllegalArgumentException("PersonaDAO no puede ser null");
        }
        if (historiaClinicaService == null) {
            throw new IllegalArgumentException("DomicilioServiceImpl no puede ser null");
        }
        this.pacienteDAO = pacienteDAO;
        this.historiaClinicaService = historiaClinicaService;
    }

    /**
     * Inserta una nueva persona en la base de datos.
     *
     * Flujo transaccional complejo:
     * 1. Valida que los datos de la persona sean correctos (nombre, apellido, DNI)
     * 2. Valida que el DNI sea único en el sistema (RN-001)
     * 3. Si la persona tiene domicilio asociado:
     *    a. Si domicilio.id == 0 → Es nuevo, lo inserta en la BD
     *    b. Si domicilio.id > 0 → Ya existe, lo actualiza
     * 4. Inserta la persona con la FK domicilio_id correcta
     *
     * IMPORTANTE: La coordinación con DomicilioService permite que el domicilio
     * obtenga su ID autogenerado ANTES de insertar la persona (necesario para la FK).
     *
     * @param paciente Persona a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla, el DNI está duplicado, o hay error de BD
     */
    @Override
    public void insertar(Paciente paciente) throws Exception {
        validatePaciente(paciente);
        validateDniUnique(paciente.getDni(), null);

        // Coordinación con HistoriaClinicaService (transaccional)
        if (paciente.getHistoriaClinica() != null) {
            if (paciente.getHistoriaClinica().getId() == 0) {
                // Historia clinica nueva: insertar primero para obtener ID autogenerado
                historiaClinicaService.insertar(paciente.getHistoriaClinica());
            } else {
                // Domicilio existente: actualizar datos
                historiaClinicaService.actualizar(paciente.getHistoriaClinica());
            }
        }

        pacienteDAO.insertar(paciente);
    }

    /**
     * Actualiza una persona existente en la base de datos.
     *
     * Validaciones:
     * - La persona debe tener datos válidos (nombre, apellido, DNI)
     * - El ID debe ser > 0 (debe ser una persona ya persistida)
     * - El DNI debe ser único (RN-001), excepto para la misma persona
     *
     * IMPORTANTE: Esta operación NO coordina con DomicilioService.
     * Para cambiar el domicilio de una persona, usar MenuHandler que:
     * - Asignar nuevo domicilio: opción 6 (crea nuevo) o 7 (usa existente)
     * - Actualizar domicilio: opción 9 (modifica domicilio actual)
     *
     * @param paciente Paciente con los datos actualizados
     * @throws Exception Si la validación falla, el DNI está duplicado, o la persona no existe
     */
    @Override
    public void actualizar(Paciente paciente) throws Exception {
        validatePaciente(paciente);
        if (paciente.getId() <= 0) {
            throw new IllegalArgumentException("El ID de del paciente debe ser mayor a 0 para actualizar");
        }
        validateDniUnique(paciente.getDni(), paciente.getId());
        pacienteDAO.actualizar(paciente);
    }

    /**
     * Elimina lógicamente una persona (soft delete).
     * Marca la persona como eliminado=TRUE sin borrarla físicamente.
     *
     * ⚠️ IMPORTANTE: Este método NO elimina el domicilio asociado (RN-037).
     * Si la persona tiene un domicilio, este quedará activo en la BD.
     * Esto es correcto porque múltiples personas pueden compartir un domicilio.
     *
     * @param id ID de la persona a eliminar
     * @throws Exception Si id <= 0 o no existe la persona
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        pacienteDAO.eliminar(id);
    }

    /**
     * Obtiene una persona por su ID.
     * Incluye el domicilio asociado mediante LEFT JOIN (PersonaDAO).
     *
     * @param id ID de la persona a buscar
     * @return Persona encontrada (con su domicilio si tiene), o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public Paciente getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return pacienteDAO.getById(id);
    }

    /**
     * Obtiene todas las personas activas (eliminado=FALSE).
     * Incluye sus domicilios mediante LEFT JOIN (PersonaDAO).
     *
     * @return Lista de personas activas con sus domicilios (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Paciente> getAll() throws Exception {
        return pacienteDAO.getAll();
    }

    /**
     * Expone el servicio de domicilios para que MenuHandler pueda usarlo.
     * Necesario para operaciones de menú que trabajan directamente con domicilios.
     *
     * @return Instancia de DomicilioServiceImpl inyectada en este servicio
     */
    public HistoriaClinicaServiceImpl getHistoriaClinicaService() {
        return this.historiaClinicaService;
    }

    /**
     * Busca personas por nombre o apellido (búsqueda flexible con LIKE).
     * Usa PersonaDAO.buscarPorNombreApellido() que realiza:
     * - LIKE %filtro% en nombre O apellido
     * - Insensible a mayúsculas/minúsculas (LOWER())
     * - Solo personas activas (eliminado=FALSE)
     *
     * Uso típico: El usuario ingresa "juan" y encuentra "Juan Pérez", "María Juana", etc.
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de personas que coinciden con el filtro (puede estar vacía)
     * @throws IllegalArgumentException Si el filtro está vacío
     * @throws Exception Si hay error de BD
     */
    public List<Paciente> buscarPorNombreApellido(String filtro) throws Exception {
        if (filtro == null || filtro.trim().isEmpty()) {
            throw new IllegalArgumentException("El filtro de búsqueda no puede estar vacío");
        }
        return pacienteDAO.buscarPorNombreApellido(filtro);
    }

    /**
     * Busca una persona por DNI exacto.
     * Usa PersonaDAO.buscarPorDni() que realiza búsqueda exacta (=).
     *
     * Uso típico:
     * - Validar unicidad del DNI (validateDniUnique)
     * - Buscar persona específica desde el menú (opción 4)
     *
     * @param dni DNI exacto a buscar (no puede estar vacío)
     * @return Persona con ese DNI, o null si no existe o está eliminada
     * @throws IllegalArgumentException Si el DNI está vacío
     * @throws Exception Si hay error de BD
     */
    public Paciente buscarPorDni(String dni) throws Exception {
        if (dni == null || dni.trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
        return pacienteDAO.buscarPorDni(dni);
    }

    /**
     * Elimina un domicilio de forma SEGURA actualizando primero la FK de la persona.
     * Este es el método RECOMENDADO para eliminar domicilios (RN-029 solucionado).
     *
     * Flujo transaccional SEGURO:
     * 1. Obtiene la persona por ID y valida que exista
     * 2. Verifica que el domicilio pertenezca a esa persona (evita eliminar domicilio ajeno)
     * 3. Desasocia el domicilio de la persona (persona.domicilio = null)
     * 4. Actualiza la persona en BD (domicilio_id = NULL)
     * 5. Elimina el domicilio (ahora no hay FKs apuntando a él)
     *
     * DIFERENCIA con DomicilioService.eliminar():
     * - DomicilioService.eliminar(): Elimina directamente (PELIGROSO, puede dejar FKs huérfanas)
     * - Este método: Primero actualiza FK, luego elimina (SEGURO)
     *
     * Usado en MenuHandler opción 10: "Eliminar domicilio de una persona"
     *
     * @param pacienteId ID de la persona dueña del domicilio
     * @param historiaClinicaId ID del domicilio a eliminar
     * @throws IllegalArgumentException Si los IDs son <= 0, la persona no existe, o el domicilio no pertenece a la persona
     * @throws Exception Si hay error de BD
     */
    public void eliminarHistoricaClinica(int pacienteId, int historiaClinicaId) throws Exception {
        if (pacienteId <= 0 || historiaClinicaId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Paciente paciente = pacienteDAO.getById(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("Persona no encontrada con ID: " + pacienteId);
        }

        if (paciente.getHistoriaClinica() == null || paciente.getHistoriaClinica().getId() != historiaClinicaId) {
            throw new IllegalArgumentException("El domicilio no pertenece a esta persona");
        }

        // Secuencia transaccional: actualizar FK → eliminar domicilio
        paciente.setHistoriaClinica(null);
        pacienteDAO.actualizar(paciente);
        historiaClinicaService.eliminar(historiaClinicaId);
    }

    /**
     * Valida que una persona tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - RN-035: Nombre, apellido y DNI son obligatorios
     * - RN-036: Se verifica trim() para evitar strings solo con espacios
     *
     * @param paciente Persona a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validatePaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("La persona no puede ser null");
        }
        if (paciente.getNombre() == null || paciente.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        }
        if (paciente.getApellido() == null || paciente.getApellido().trim().isEmpty()) {
            throw new IllegalArgumentException("El apellido no puede estar vacío");
        }
        if (paciente.getDni() == null || paciente.getDni().trim().isEmpty()) {
            throw new IllegalArgumentException("El DNI no puede estar vacío");
        }
    }

    /**
     * Valida que un DNI sea único en el sistema.
     * Implementa la regla de negocio RN-001: "El DNI debe ser único".
     *
     * Lógica:
     * 1. Busca si existe una persona con ese DNI en la BD
     * 2. Si NO existe → OK, el DNI es único
     * 3. Si existe → Verifica si es la misma persona que estamos actualizando:
     *    a. Si personaId == null (INSERT) → Error, DNI duplicado
     *    b. Si personaId != null (UPDATE) y existente.id == personaId → OK, es la misma persona
     *    c. Si personaId != null (UPDATE) y existente.id != personaId → Error, DNI duplicado
     *
     * Ejemplo de uso correcto en UPDATE:
     * - Persona ID=5 con DNI="12345678" quiere actualizar su nombre
     * - validateDniUnique("12345678", 5) → Encuentra persona con DNI="12345678" (ID=5)
     * - Como existente.id (5) == personaId (5) → OK, la persona se está actualizando a sí misma
     *
     * @param dni DNI a validar
     * @param personaId ID de la persona (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el DNI ya existe y pertenece a otra persona
     * @throws Exception Si hay error de BD al buscar
     */
    private void validateDniUnique(String dni, Integer personaId) throws Exception {
        Paciente existente = pacienteDAO.buscarPorDni(dni);
        if (existente != null) {
            // Existe una persona con ese DNI
            if (personaId == null || existente.getId() != personaId) {
                // Es INSERT (personaId == null) o es UPDATE pero el DNI pertenece a otra persona
                throw new IllegalArgumentException("Ya existe una persona con el DNI: " + dni);
            }
            // Si llegamos aquí: es UPDATE y el DNI pertenece a la misma persona → OK
        }
    }
}