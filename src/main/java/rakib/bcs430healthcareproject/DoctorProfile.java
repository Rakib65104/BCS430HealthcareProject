package rakib.bcs430healthcareproject;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a doctor's profile data.
 * This is stored in Firestore under /doctors/{uid}
 */
public class DoctorProfile {

    @DocumentId
    private String uid;

    private String name;
    private String email;
    private String role;

    // Professional info
    private String specialty;
    private String clinicName;

    // Location info
    private String address;
    private String city;
    private String state;
    private String zip;

    // Practice info
    private Boolean acceptingNewPatients;

    // Security
    private String passwordHash;
    private String passwordSalt;

    // Metadata
    private Long createdAt;
    private Long updatedAt;

    // ===== Required no-arg constructor for Firestore =====
    public DoctorProfile() {
    }

    // ===== Constructor used at signup =====
    public DoctorProfile(String uid,
                         String name,
                         String email,
                         String specialty,
                         String clinicName,
                         String address,
                         String city,
                         String state,
                         String zip,
                         Boolean acceptingNewPatients) {

        this.uid = uid;
        this.name = name;
        this.email = email;
        this.specialty = specialty;
        this.clinicName = clinicName;
        this.address = address;
        this.city = city;
        this.state = state;
        this.zip = zip;
        this.acceptingNewPatients = acceptingNewPatients;

        this.role = "DOCTOR";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // ===== Getters & Setters =====

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        touch();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
        touch();
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
        touch();
    }

    public String getClinicName() {
        return clinicName;
    }

    public void setClinicName(String clinicName) {
        this.clinicName = clinicName;
        touch();
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
        touch();
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
        touch();
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
        touch();
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
        touch();
    }

    public Boolean getAcceptingNewPatients() {
        return acceptingNewPatients;
    }

    public void setAcceptingNewPatients(Boolean acceptingNewPatients) {
        this.acceptingNewPatients = acceptingNewPatients;
        touch();
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Long updatedAt) {
        this.updatedAt = updatedAt;
    }

    private void touch() {
        this.updatedAt = System.currentTimeMillis();
    }


    /**
     * Convert DoctorProfile to a Map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", name);
        result.put("email", email);
        result.put("role", role);

        result.put("specialty", specialty);
        result.put("clinicName", clinicName);
        result.put("address", address);
        result.put("city", city);
        result.put("state", state);
        result.put("zip", zip);
        result.put("acceptingNewPatients", acceptingNewPatients);

        result.put("passwordHash", passwordHash);
        result.put("passwordSalt", passwordSalt);

        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);

        return result;
    }
}