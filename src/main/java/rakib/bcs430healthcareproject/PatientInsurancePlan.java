package rakib.bcs430healthcareproject;

public class PatientInsurancePlan {
    private String company;
    private String insuranceNumber;
    private String planType;
    private String groupNumber;

    public PatientInsurancePlan() {
    }

    public PatientInsurancePlan(String company, String insuranceNumber, String planType, String groupNumber) {
        this.company = company;
        this.insuranceNumber = insuranceNumber;
        this.planType = planType;
        this.groupNumber = groupNumber;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public String getGroupNumber() {
        return groupNumber;
    }

    public void setGroupNumber(String groupNumber) {
        this.groupNumber = groupNumber;
    }
}
