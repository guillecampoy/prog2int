package prog2int.Models;



public class HistoriaClinica extends Base {
 

    private String nroHistoria;

    private GrupoSanguineo grupoSanguineo;


    private String antecedentes;


    private String medicacionActual;

 
    private String observaciones;


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

    public HistoriaClinica(String nroHistoria, GrupoSanguineo grupoSanguineo, String antecedentes, 
                          String medicacionActual, String observaciones, int id, boolean eliminado) {
        super( id, eliminado);
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