package prog2int.Models;



/**
 * Representa la historia clínica de un paciente en el sistema.
 * <p>
 * Esta entidad modela la información médica relevante de un paciente,
 * incluyendo su grupo sanguíneo, antecedentes médicos, medicación actual y observaciones.
 * <p>
 * Relación: Cada historia clínica está asociada a un paciente (ver clase {@code Paciente}).
 * <p>
 * Mapeo de base de datos: Se espera que esta clase se mapee a una tabla de historias clínicas,
 * donde el campo {@code nroHistoria} debe ser único para cada registro.
 * <p>
 * Regla de negocio: {@code nroHistoria} es único y sirve como identificador de la historia clínica.
 */
public class HistoriaClinica extends Base {

    /**
     * Número único que identifica la historia clínica en la base de datos.
     * Regla de negocio: debe ser único.
     */
    private String nroHistoria;

    /**
     * Grupo sanguíneo del paciente.
     */
    private GrupoSanguineo grupoSanguineo;

    /**
     * Antecedentes médicos relevantes del paciente.
     */
    private String antecedentes;

    /**
     * Medicación actual que está tomando el paciente.
     */
    private String medicacionActual;

    /**
     * Observaciones adicionales sobre la historia clínica.
     */
    private String observaciones;

    /**
     * Enumeración que representa los posibles grupos sanguíneos.
     */
    // Enumeración para Grupo Sanguíneo
    public enum GrupoSanguineo {
        A_POSITIVO("A+"),
        A_NEGATIVO("A-"),
        B_POSITIVO("B+"),
        B_NEGATIVO("B-"),
        AB_POSITIVO("AB+"),
        AB_NEGATIVO("AB-"),
        O_POSITIVO("O+"),
        O_NEGATIVO("O-");

        private final String simbolo;

        GrupoSanguineo(String simbolo) {
            this.simbolo = simbolo;
        }

        public String getSimbolo() {
            return simbolo;
        }

    }

    // Constructores
    public HistoriaClinica() {
        super();
    }

    public HistoriaClinica(int id, boolean eliminado, String nroHistoria, GrupoSanguineo grupoSanguineo, String antecedentes, 
                          String medicacionActual, String observaciones) {
        super(id, eliminado);
        this.nroHistoria = nroHistoria;
        this.grupoSanguineo = grupoSanguineo;
        this.antecedentes = antecedentes;
        this.medicacionActual = medicacionActual;
        this.observaciones = observaciones;
    }


    // Getters y Setters

    public String getNroHistoria() {
        return nroHistoria;
    }

    public void setNroHistoria(String nroHistoria) {
        this.nroHistoria = nroHistoria;
    }

    public GrupoSanguineo getGrupoSanguineo() {
        return grupoSanguineo;
    }

    public void setGrupoSanguineo(GrupoSanguineo grupoSanguineo) {
        this.grupoSanguineo = grupoSanguineo;
    }

    public String getAntecedentes() {
        return antecedentes;
    }

    public void setAntecedentes(String antecedentes) {
        this.antecedentes = antecedentes;
    }

    public String getMedicacionActual() {
        return medicacionActual;
    }

    public void setMedicacionActual(String medicacionActual) {
        this.medicacionActual = medicacionActual;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }

    public String getGrupoSanguineoSymbol() {
        return grupoSanguineo != null ? grupoSanguineo.getSimbolo() : "No especificado";
    }

    @Override
    public String toString() {
        return "HISTORIA CLÍNICA [ID: " + getId() + "]\n" +
               "Número: " + nroHistoria + "\n" +
               "Grupo Sanguíneo: " + getGrupoSanguineoSymbol() + "\n" +
               "Antecedentes: " + (antecedentes != null && !antecedentes.isEmpty() ? 
                   antecedentes + "..." : "No registrados") + "\n" +
               "Medicación Actual: " + (medicacionActual != null && !medicacionActual.isEmpty() ? 
                   medicacionActual + "..." : "No registrada") + "\n" +
               "Observaciones: " + (observaciones != null && !observaciones.isEmpty() ? 
                   observaciones + "..." : "No registradas") + "\n" +
                  "Estado: " + (isEliminado() ? "ELIMINADA" : "ACTIVA");
    }
}