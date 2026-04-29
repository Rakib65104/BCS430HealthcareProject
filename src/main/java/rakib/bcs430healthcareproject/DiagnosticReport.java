package rakib.bcs430healthcareproject;

public class DiagnosticReport {

    private String reportId;
    private String appointmentId;

    private String patientUid;
    private String patientName;

    private String doctorUid;
    private String doctorName;

    private String hospitalUid;
    private String hospitalName;

    private String reportTitle;
    private String reportType;

    private String hospitalFindings;
    private String diagnosticResults;

    private String uploadedByUid;
    private String uploadedByRole;

    private Long uploadedAt;
    private String status;

    public DiagnosticReport() {
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

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

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
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

    public String getReportTitle() {
        return reportTitle;
    }

    public void setReportTitle(String reportTitle) {
        this.reportTitle = reportTitle;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
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

    public String getUploadedByUid() {
        return uploadedByUid;
    }

    public void setUploadedByUid(String uploadedByUid) {
        this.uploadedByUid = uploadedByUid;
    }

    public String getUploadedByRole() {
        return uploadedByRole;
    }

    public void setUploadedByRole(String uploadedByRole) {
        this.uploadedByRole = uploadedByRole;
    }

    public Long getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(Long uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}