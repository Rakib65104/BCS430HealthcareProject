package rakib.bcs430healthcareproject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Model class representing an appointment between a patient and doctor.
 */
public class Appointment {

    private static final DateTimeFormatter SLOT_TIME_FORMAT =
            DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private String appointmentId;

    private String patientUid;
    private String doctorUid;

    private String patientName;
    private String doctorName;

    private String hospitalUid;
    private String hospitalName;
    private String hospitalDepartment;

    // Timestamp for sorting
    private Long appointmentDateTime; // Unix timestamp

    // Human readable date/time
    private String appointmentTime; // e.g., "2026-03-15 10:30 AM"

    /**
     * Fields used for availability checking
     */
    private String appointmentDate; // e.g., "2026-03-15"
    private String appointmentSlot; // e.g., "10:30 AM"

    private String status; // SCHEDULED, COMPLETED

    private Boolean newPatient;

    private String reason;

    private String notes;

    private String referralType;
    private String referralAuthorizedByDoctorUid;
    private String referralAuthorizedByDoctorName;
    private String referralNotes;

    private String visitSummary;

    private String hospitalFindings;
    private String diagnosticResults;
    private Long diagnosticResultsUploadedAt;

    private String prescribedMedications;

    private String prescribedPrescriptionId;

    private Long completedAt;

    private Long createdAt;

    public Appointment() {
    }

    public Appointment(String patientUid,
                       String doctorUid,
                       String patientName,
                       String doctorName,
                       Long appointmentDateTime) {

        this.patientUid = patientUid;
        this.doctorUid = doctorUid;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.appointmentDateTime = appointmentDateTime;
        this.status = "SCHEDULED";
        this.createdAt = System.currentTimeMillis();
    }

    // ===== Getters / Setters =====

    public String getAppointmentId() {
        return appointmentId;
    }

    public void setAppointmentId(String appointmentId) {
        this.appointmentId = appointmentId;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getHospitalUid() {
        return hospitalUid;
    }

    public void setHospitalUid(String hospitalUid) {
        this.hospitalUid = hospitalUid;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getHospitalDepartment() {
        return hospitalDepartment;
    }

    public void setHospitalDepartment(String hospitalDepartment) {
        this.hospitalDepartment = hospitalDepartment;
    }

    public Long getAppointmentDateTime() {
        return appointmentDateTime;
    }

    public void setAppointmentDateTime(Long appointmentDateTime) {
        this.appointmentDateTime = appointmentDateTime;
    }

    public String getAppointmentTime() {
        return appointmentTime;
    }

    /**
     * Also extracts appointmentDate and appointmentSlot automatically.
     * Expected format: "YYYY-MM-DD HH:MM AM"
     */
    public void setAppointmentTime(String appointmentTime) {
        this.appointmentTime = appointmentTime;

        if (appointmentTime != null && appointmentTime.contains(" ")) {
            String[] parts = appointmentTime.split(" ", 2);

            if (parts.length == 2) {
                this.appointmentDate = parts[0];
                this.appointmentSlot = parts[1];
            }
        }
    }

    public String getAppointmentDate() {
        return appointmentDate;
    }

    public void setAppointmentDate(String appointmentDate) {
        this.appointmentDate = appointmentDate;
    }

    public String getAppointmentSlot() {
        return appointmentSlot;
    }

    public void setAppointmentSlot(String appointmentSlot) {
        this.appointmentSlot = appointmentSlot;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getNewPatient() {
        return newPatient;
    }

    public void setNewPatient(Boolean newPatient) {
        this.newPatient = newPatient;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getReferralType() {
        return referralType;
    }

    public void setReferralType(String referralType) {
        this.referralType = referralType;
    }

    public String getReferralAuthorizedByDoctorUid() {
        return referralAuthorizedByDoctorUid;
    }

    public void setReferralAuthorizedByDoctorUid(String referralAuthorizedByDoctorUid) {
        this.referralAuthorizedByDoctorUid = referralAuthorizedByDoctorUid;
    }

    public String getReferralAuthorizedByDoctorName() {
        return referralAuthorizedByDoctorName;
    }

    public void setReferralAuthorizedByDoctorName(String referralAuthorizedByDoctorName) {
        this.referralAuthorizedByDoctorName = referralAuthorizedByDoctorName;
    }

    public String getReferralNotes() {
        return referralNotes;
    }

    public void setReferralNotes(String referralNotes) {
        this.referralNotes = referralNotes;
    }

    public String getVisitSummary() {
        return visitSummary;
    }

    public void setVisitSummary(String visitSummary) {
        this.visitSummary = visitSummary;
    }

    public String getHospitalFindings() {
        return hospitalFindings;
    }

    public void setHospitalFindings(String hospitalFindings) {
        this.hospitalFindings = hospitalFindings;
    }

    public String getDiagnosticResults() {
        return diagnosticResults;
    }

    public void setDiagnosticResults(String diagnosticResults) {
        this.diagnosticResults = diagnosticResults;
    }

    public Long getDiagnosticResultsUploadedAt() {
        return diagnosticResultsUploadedAt;
    }

    public void setDiagnosticResultsUploadedAt(Long diagnosticResultsUploadedAt) {
        this.diagnosticResultsUploadedAt = diagnosticResultsUploadedAt;
    }

    public String getPrescribedMedications() {
        return prescribedMedications;
    }

    public void setPrescribedMedications(String prescribedMedications) {
        this.prescribedMedications = prescribedMedications;
    }

    public String getPrescribedPrescriptionId() {
        return prescribedPrescriptionId;
    }

    public void setPrescribedPrescriptionId(String prescribedPrescriptionId) {
        this.prescribedPrescriptionId = prescribedPrescriptionId;
    }

    public Long getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Long completedAt) {
        this.completedAt = completedAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Returns a best-effort epoch millis for the appointment's scheduled time.
     */
    public Long resolveAppointmentEpochMillis() {
        if (appointmentDateTime != null) {
            return appointmentDateTime;
        }

        if (appointmentDate == null || appointmentDate.isBlank()
                || appointmentSlot == null || appointmentSlot.isBlank()) {
            return null;
        }

        try {
            LocalDate date = LocalDate.parse(appointmentDate.trim());
            LocalTime time = LocalTime.parse(appointmentSlot.trim().toUpperCase(Locale.ENGLISH), SLOT_TIME_FORMAT);
            LocalDateTime dateTime = LocalDateTime.of(date, time);

            return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        } catch (Exception e) {
            return null;
        }
    }

    public boolean hasPassed(long nowEpochMillis) {
        Long appointmentEpoch = resolveAppointmentEpochMillis();
        return appointmentEpoch != null && appointmentEpoch < nowEpochMillis;
    }
}
