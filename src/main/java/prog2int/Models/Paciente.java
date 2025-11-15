package prog2int.Models;


import java.time.LocalDate;

public class Paciente extends Base {

    private String nombre;

    private String apellido;

    private String dni;

    private LocalDate fechaNacimiento;

    private HistoriaClinica historiaClinica;

    // Constructores
    public Paciente() {
        super();
    }
    public Paciente(int id, String nombre, String apellido, String dni, LocalDate fechaNacimiento, Boolean eliminado, HistoriaClinica historiaClinica) {
       super( id, eliminado);
        this.nombre = nombre;
        this.apellido = apellido;
        this.dni = dni;
        this.fechaNacimiento = fechaNacimiento;
        this.historiaClinica = historiaClinica;
    }

    // Getters y Setters

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }


    public HistoriaClinica getHistoriaClinica() {
        return historiaClinica;
    }

    public void setHistoriaClinica(HistoriaClinica historiaClinica) {
        this.historiaClinica = historiaClinica;
    }
 public String detalleHistoriaClinica() {
        if (historiaClinica == null) {
            return "El paciente no tiene historia clínica asociada.";
        }
        
        return String.format(
            "=== DETALLE DE HISTORIA CLÍNICA ===%n" +
            "Paciente: %s%n" +
            "DNI: %s%n" +
            "Número de Historia: %s%n" +
            "Grupo Sanguíneo: %s%n" +
            "Antecedentes: %s%n" +
            "Medicación Actual: %s%n" +
            "Observaciones: %s%n" +
            "Estado Historia: %s",
            getNombre() + " " + getApellido(),
            dni,
            historiaClinica.getNroHistoria(),
            historiaClinica.getGrupoSanguineoSymbol(),
            historiaClinica.getAntecedentes() != null ? historiaClinica.getAntecedentes() : "No registrado",
            historiaClinica.getMedicacionActual() != null ? historiaClinica.getMedicacionActual() : "No registrada",
            historiaClinica.getObservaciones() != null ? historiaClinica.getObservaciones() : "No registradas",
            historiaClinica.isEliminado() ? "ELIMINADA" : "ACTIVA"
        );
    }




    @Override
    public String toString() {
        return "Paciente{" +
                "id=" + getId() +
                ", nombre='" + nombre + '\'' +
                ", apellido='" + apellido + '\'' +
                ", dni='" + dni + '\'' +
                ", fechaNacimiento=" + fechaNacimiento +
                ", eliminado=" + isEliminado() +
                '}';
    }
}