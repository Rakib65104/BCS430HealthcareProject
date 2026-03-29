package rakib.bcs430healthcareproject;

/**
 * Model representing a prescription sent by a doctor for a patient.
 */
public class Prescription {

    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FILLED = "FILLED";

    private String prescriptionId;
    private String doctorUid;
    private String doctorName;
    private String patientUid;
    private String patientName;
    private String pharmacyName;
    private String pharmacyAddress;
    private String pharmacyPhoneNumber;
    private String medicationInformation;
    private String instructions;
    private String status;
    private String filledBy;
    private Long filledAt;
    private Long createdAt;

    public Prescription() {
    }

    public String getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(String prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getDoctorUid() {
        return doctorUid;
    }

    public void setDoctorUid(String doctorUid) {
        this.doctorUid = doctorUid;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getPatientUid() {
        return patientUid;
    }

    public void setPatientUid(String patientUid) {
        this.patientUid = patientUid;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public String getPharmacyAddress() {
        return pharmacyAddress;
    }

    public void setPharmacyAddress(String pharmacyAddress) {
        this.pharmacyAddress = pharmacyAddress;
    }

    public String getPharmacyPhoneNumber() {
        return pharmacyPhoneNumber;
    }

    public void setPharmacyPhoneNumber(String pharmacyPhoneNumber) {
        this.pharmacyPhoneNumber = pharmacyPhoneNumber;
    }

    public String getMedicationInformation() {
        return medicationInformation;
    }

    public void setMedicationInformation(String medicationInformation) {
        this.medicationInformation = medicationInformation;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFilledBy() {
        return filledBy;
    }

    public void setFilledBy(String filledBy) {
        this.filledBy = filledBy;
    }

    public Long getFilledAt() {
        return filledAt;
    }

    public void setFilledAt(Long filledAt) {
        this.filledAt = filledAt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
