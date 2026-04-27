package rakib.bcs430healthcareproject;

public class HospitalDepartment {

    private String departmentId;
    private String hospitalUid;
    private String name;
    private String description;
    private String phoneNumber;
    private String floor;
    private boolean active;

    public HospitalDepartment() {
    }

    public HospitalDepartment(String departmentId, String hospitalUid, String name,
                              String description, String phoneNumber,
                              String floor, boolean active) {
        this.departmentId = departmentId;
        this.hospitalUid = hospitalUid;
        this.name = name;
        this.description = description;
        this.phoneNumber = phoneNumber;
        this.floor = floor;
        this.active = active;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getHospitalUid() {
        return hospitalUid;
    }

    public void setHospitalUid(String hospitalUid) {
        this.hospitalUid = hospitalUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFloor() {
        return floor;
    }

    public void setFloor(String floor) {
        this.floor = floor;
    }

    public boolean isActive() {
        return active;
    }

    public boolean getActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}