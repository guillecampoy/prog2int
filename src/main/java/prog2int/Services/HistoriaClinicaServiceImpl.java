package prog2int.Services;

import java.util.List;
import prog2int.Dao.GenericDAO;
import prog2int.Models.HistoriaClinica;

/**
 * Implementación del servicio de negocio para la entidad HistoriaClinica.
 * Capa intermedia entre la UI y el DAO que aplica validaciones de negocio.
 *
 * Responsabilidades:
 * - Validar que los datos de la historia clínica sean correctos ANTES de persistir
 * - Aplicar reglas de negocio (número de historia y grupo sanguíneo obligatorios)
 * - Delegar operaciones de BD al DAO
 * - Transformar excepciones técnicas en errores de negocio comprensibles
 *
 * Patrón: Service Layer con inyección de dependencias
 */
public class HistoriaClinicaServiceImpl implements GenericService<HistoriaClinica> {
    /**
     * DAO para acceso a datos de historias clínicas.
     * Inyectado en el constructor (Dependency Injection).
     * Usa GenericDAO para permitir testing con mocks.
     */
    private final GenericDAO<HistoriaClinica> historiaClinicaDao;

    /**
     * Constructor con inyección de dependencias.
     * Valida que el DAO no sea null (fail-fast).
     *
     * @param historiaClinicaDAO DAO de historias clínicas
     * @throws IllegalArgumentException si historiaClinicaDAO es null
     */
    public HistoriaClinicaServiceImpl(GenericDAO<HistoriaClinica> historiaClinicaDAO) {
        if (historiaClinicaDAO == null) {
            throw new IllegalArgumentException("HistoriaClinicaDAO no puede ser null");
        }
        this.historiaClinicaDao = historiaClinicaDAO;
    }

    /**
     * Inserta una nueva historia clínica en la base de datos.
     *
     * Flujo:
     * 1. Valida que número de historia y grupo sanguíneo no estén vacíos
     * 2. Delega al DAO para insertar
     * 3. El DAO asigna el ID autogenerado al objeto
     *
     * @param historiaClinica Historia clínica a insertar (id será ignorado y regenerado)
     * @throws Exception Si la validación falla o hay error de BD
     */
    @Override
    public void insertar(HistoriaClinica historiaClinica) throws Exception {
        validateHistoriaClinica(historiaClinica);
        historiaClinicaDao.insertar(historiaClinica);
    }

    /**
     * Actualiza una historia clínica existente en la base de datos.
     *
     * Validaciones:
     * - La historia clínica debe tener datos válidos
     * - El ID debe ser > 0 (debe ser una historia ya persistida)
     *
     * @param historiaClinica Historia clínica con los datos actualizados
     * @throws Exception Si la validación falla o la historia clínica no existe
     */
    @Override
    public void actualizar(HistoriaClinica historiaClinica) throws Exception {
        validateHistoriaClinica(historiaClinica);
        if (historiaClinica.getId() <= 0) {
            throw new IllegalArgumentException("El ID de la historia clínica debe ser mayor a 0 para actualizar");
        }
        historiaClinicaDao.actualizar(historiaClinica);
    }

    /**
     * Elimina lógicamente una historia clínica (soft delete).
     * Marca la historia clínica como eliminado=TRUE sin borrarla físicamente.
     *
     * @param id ID de la historia clínica a eliminar
     * @throws Exception Si id <= 0 o no existe la historia clínica
     */
    @Override
    public void eliminar(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        historiaClinicaDao.eliminar(id);
    }

    /**
     * Obtiene una historia clínica por su ID.
     *
     * @param id ID de la historia clínica a buscar
     * @return Historia clínica encontrada, o null si no existe o está eliminada
     * @throws Exception Si id <= 0 o hay error de BD
     */
    @Override
    public HistoriaClinica getById(int id) throws Exception {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor a 0");
        }
        return historiaClinicaDao.getById(id);
    }

    /**
     * Obtiene todas las historias clínicas activas (eliminado=FALSE).
     *
     * @return Lista de historias clínicas activas (puede estar vacía)
     * @throws Exception Si hay error de BD
     */
    @Override
    public List<HistoriaClinica> getAll() throws Exception {
        return historiaClinicaDao.getAll();
    }

    /**
     * Expone el DAO para coordinación transaccional.
     * Usado por PacienteHistoriaClinicaService.
     */
    public GenericDAO<HistoriaClinica> getHistoriaClinicaDAO() {
        return this.historiaClinicaDao;
    }

    /**
     * Valida que una historia clínica tenga datos correctos.
     *
     * Reglas de negocio aplicadas:
     * - Número de historia es obligatorio y debe tener formato válido (HC-XXXX)
     * - Grupo sanguíneo es obligatorio
     * - Se verifica trim() para evitar strings solo con espacios
     *
     * @param historiaClinica Historia clínica a validar
     * @throws IllegalArgumentException Si alguna validación falla
     */
    private void validateHistoriaClinica(HistoriaClinica historiaClinica) throws Exception {
        if (historiaClinica == null) {
            throw new IllegalArgumentException("La historia clínica no puede ser null");
        }
        if (historiaClinica.getNroHistoria() == null || historiaClinica.getNroHistoria().trim().isEmpty()) {
            throw new IllegalArgumentException("El número de historia no puede estar vacío");
        }
        if (historiaClinica.getGrupoSanguineo() == null) {
            throw new IllegalArgumentException("El grupo sanguíneo no puede ser null");
        }
        validateNroHistoriaFormat(historiaClinica.getNroHistoria());
    }

    /**
     * Valida el formato del número de historia.
     * Formato esperado: HC-XXXX donde XXXX son 4 dígitos
     * Ejemplos válidos: HC-0001, HC-1234, HC-9999
     *
     * @param nroHistoria Número de historia a validar
     * @throws IllegalArgumentException Si el formato es inválido
     */
    private void validateNroHistoriaFormat(String nroHistoria) {
        if (!nroHistoria.matches("^HC-\\d{4}$")) {
            throw new IllegalArgumentException(
                "El número de historia debe tener el formato HC-XXXX (ej: HC-0001)"
            );
        }
    }
}