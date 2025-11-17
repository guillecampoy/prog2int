package prog2int.Services;

import prog2int.Models.Paciente;

import java.util.List;
import prog2int.Dao.PacienteDAO;
/**
 * Servicio de negocio para Paciente.
 * Aplica validaciones y coordina operaciones transaccionales.
 */
public class PacienteServiceImpl implements GenericService<Paciente> {
    private final PacienteDAO pacienteDAO;

    private final HistoriaClinicaServiceImpl historiaClinicaService;

    public PacienteServiceImpl(PacienteDAO pacienteDAO, HistoriaClinicaServiceImpl historiaClinicaService) {
        if (pacienteDAO == null) {
            throw new IllegalArgumentException("PacienteDAO no puede ser null");
        }
        if (historiaClinicaService == null) {
            throw new IllegalArgumentException("HistoriaClinicaServiceImpl no puede ser null");
        }
        this.pacienteDAO = pacienteDAO;
        this.historiaClinicaService = historiaClinicaService;
    }

    /**
     * Inserta un nuevo paciente en la base de datos.
     *
     * @param paciente Paciente a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla, el DNI está duplicado, o hay error de BD
     */
    @Override
    public void insertar(Paciente paciente) throws Exception {
        validatePaciente(paciente);
        validateDniUnique(paciente.getDni(), null);

        if (paciente.getHistoriaClinica() != null) {
            if (paciente.getHistoriaClinica().getId() == 0) {
                PacienteHistoriaClinicaService coordinador = 
                    new PacienteHistoriaClinicaService(
                        pacienteDAO, 
                        (prog2int.Dao.HistoriaClinicaDAO) historiaClinicaService.getHistoriaClinicaDAO()
                    );
                coordinador.crearPacienteConHistoria(paciente, paciente.getHistoriaClinica());
            } else {
                historiaClinicaService.actualizar(paciente.getHistoriaClinica());
                pacienteDAO.insertar(paciente);
            }
        } else {
            pacienteDAO.insertar(paciente);
        }
    }

    /**
     * Actualiza una paciente existente en la base de datos.
     *
     * @param paciente Paciente con los datos actualizados
     * @throws Exception Si la validación falla, el DNI está duplicado, o el paciente no existe
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
     * Elimina lógicamente un paciente (soft delete).
     * Marca el paciente como eliminado=TRUE sin borrarla físicamente.
     *
     * @param id ID del paciente a eliminar
     * @throws Exception Si id <= 0 o no existe el paciente
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        pacienteDAO.eliminar(id);
    }

    /**
     * Obtiene una paciente por su ID.
     *
     * @param id ID del paciente a buscar
     * @return Paciente encontrado o null si no existe o está eliminad0
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
     * Obtiene todos los pacientes activos (eliminado=FALSE).
     * Incluye sus historias clínicas mediante LEFT JOIN.
     *
     * @return Lista de pacientes activos (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<Paciente> getAll() throws Exception {
        return pacienteDAO.getAll();
    }

    /**
     * Expone el servicio de historias clínicas para coordinación transaccional.
     *
     * @return Instancia de HistoriaClinicaServiceImpl
     */
    public HistoriaClinicaServiceImpl getHistoriaClinicaService() {
        return this.historiaClinicaService;
    }

    /**
     * Busca pacientes por nombre o apellido (búsqueda flexible con LIKE).
     * Solo retorna pacientes activos (eliminado=FALSE).
     *
     * @param filtro Texto a buscar (no puede estar vacío)
     * @return Lista de pacientes que coinciden con el filtro (puede estar vacía)
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
     * Busca un paciente por DNI exacto.
     * Usado para validar unicidad del DNI.
     *
     * @param dni DNI exacto a buscar (no puede estar vacío)
     * @return Paciente con ese DNI, o null si no existe o está eliminado
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
     * Elimina una historia clínica de forma coordinada con su paciente.
     * Usa servicio coordinador transaccional para garantizar consistencia.
     *
     * @param pacienteId ID del paciente
     * @param historiaClinicaId ID de la historia clínica a eliminar
     * @throws IllegalArgumentException Si los IDs son inválidos o no coinciden
     * @throws Exception Si hay error de BD
     */
    public void eliminarHistoricaClinica(int pacienteId, int historiaClinicaId) throws Exception {
        if (pacienteId <= 0 || historiaClinicaId <= 0) {
            throw new IllegalArgumentException("Los IDs deben ser mayores a 0");
        }

        Paciente paciente = pacienteDAO.getById(pacienteId);
        if (paciente == null) {
            throw new IllegalArgumentException("Paciente no encontrado con ID: " + pacienteId);
        }

        if (paciente.getHistoriaClinica() == null || paciente.getHistoriaClinica().getId() != historiaClinicaId) {
            throw new IllegalArgumentException("La historia clínica no pertenece a este paciente");
        }


        PacienteHistoriaClinicaService coordinador = 
            new PacienteHistoriaClinicaService(
                pacienteDAO, 
                (prog2int.Dao.HistoriaClinicaDAO) historiaClinicaService.getHistoriaClinicaDAO()
            );
        coordinador.eliminarPacienteConHistoria(pacienteId);
    }

    /**
     * Valida que un paciente tenga datos correctos.
     *
     * Reglas de negocio:
     * - Nombre, apellido y DNI son obligatorios
     * - DNI debe tener formato válido (7-8 dígitos numéricos)
     * - Fecha de nacimiento no puede ser futura
     *
     * @param paciente Paciente a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validatePaciente(Paciente paciente) {
        if (paciente == null) {
            throw new IllegalArgumentException("El paciente no puede ser null");
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
        validateDniFormat(paciente.getDni());
        validateFechaNacimiento(paciente.getFechaNacimiento());
    }

    /**
     * Valida el formato del DNI.
     * Formato esperado: 7-8 dígitos numéricos sin puntos ni espacios
     * Ejemplos válidos: 12345678, 1234567
     *
     * @param dni DNI a validar
     * @throws IllegalArgumentException Si el formato es inválido
     */
    private void validateDniFormat(String dni) {
        if (!dni.matches("^\\d{7,8}$")) {
            throw new IllegalArgumentException(
                "El DNI debe contener entre 7 y 8 dígitos numéricos sin puntos ni espacios"
            );
        }
    }

    /**
     * Valida que la fecha de nacimiento no sea futura.
     *
     * @param fechaNacimiento Fecha de nacimiento a validar
     * @throws IllegalArgumentException Si la fecha es futura o null
     */
    private void validateFechaNacimiento(java.time.LocalDate fechaNacimiento) {
        if (fechaNacimiento == null) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser null");
        }
        if (fechaNacimiento.isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("La fecha de nacimiento no puede ser futura");
        }
    }

    /**
     * Valida que un DNI sea único en el sistema.
     * Permite actualizar el mismo paciente sin error.
     *
     * @param dni DNI a validar
     * @param pacienteId ID del paciente (null para INSERT, != null para UPDATE)
     * @throws IllegalArgumentException Si el DNI ya existe y pertenece a otro paciente
     * @throws Exception Si hay error de BD
     */
    private void validateDniUnique(String dni, Integer pacienteId) throws Exception {
        Paciente existente = pacienteDAO.buscarPorDni(dni);
        if (existente != null) {
            if (pacienteId == null || existente.getId() != pacienteId) {
                throw new IllegalArgumentException("Ya existe un paciente con el DNI: " + dni);
            }
        }
    }
}