package rakib.bcs430healthcareproject;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Schedule {

    private final StringProperty time;
    private final StringProperty patientName;
    private final StringProperty doctorName;
    private final StringProperty department;
    private final StringProperty type;
    private final StringProperty status;
    private final StringProperty notes;

    private Appointment sourceAppointment;

    public Schedule(String time, String patientName, String type, String status, String notes) {
        this(time, patientName, "", "", type, status, notes);
    }

    public Schedule(String time,
                    String patientName,
                    String doctorName,
                    String department,
                    String type,
                    String status,
                    String notes) {
        this.time = new SimpleStringProperty(time);
        this.patientName = new SimpleStringProperty(patientName);
        this.doctorName = new SimpleStringProperty(doctorName);
        this.department = new SimpleStringProperty(department);
        this.type = new SimpleStringProperty(type);
        this.status = new SimpleStringProperty(status);
        this.notes = new SimpleStringProperty(notes);
    }

    public StringProperty timeProperty() {
        return time;
    }

    public StringProperty patientNameProperty() {
        return patientName;
    }

    public StringProperty doctorNameProperty() {
        return doctorName;
    }

    public StringProperty departmentProperty() {
        return department;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public StringProperty statusProperty() {
        return status;
    }

    public StringProperty notesProperty() {
        return notes;
    }

    public String getTime() {
        return time.get();
    }

    public String getPatientName() {
        return patientName.get();
    }

    public String getDoctorName() {
        return doctorName.get();
    }

    public String getDepartment() {
        return department.get();
    }

    public String getType() {
        return type.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getNotes() {
        return notes.get();
    }

    public void setTime(String value) {
        time.set(value);
    }

    public void setPatientName(String value) {
        patientName.set(value);
    }

    public void setDoctorName(String value) {
        doctorName.set(value);
    }

    public void setDepartment(String value) {
        department.set(value);
    }

    public void setType(String value) {
        type.set(value);
    }

    public void setStatus(String value) {
        status.set(value);
    }

    public void setNotes(String value) {
        notes.set(value);
    }

    public Appointment getSourceAppointment() {
        return sourceAppointment;
    }

    public void setSourceAppointment(Appointment sourceAppointment) {
        this.sourceAppointment = sourceAppointment;
    }
}