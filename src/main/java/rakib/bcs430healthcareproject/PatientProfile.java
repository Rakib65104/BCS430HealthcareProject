package rakib.bcs430healthcareproject;

import com.google.cloud.firestore.annotation.DocumentId;
import java.util.HashMap;
import java.util.Map;

/**
 * Model class representing a patient's profile data.
 * This is stored in Firestore under /patients/{uid}
 */
public class PatientProfile {
    @DocumentId
    private String uid;
    private String name;
    private String email;
    private String zip;
    private String role;
    private String passwordHash;
    private String passwordSalt;
    private String dateOfBirth;
    private Integer age;
    private String gender;
    private String insuranceNumber;
    private String insuranceCompany;
    private String allergies;
    private String medicalHistory;
    private Long createdAt;
    private Long updatedAt;

    // Default constructor required for Firestore
    public PatientProfile() {
    }

    public PatientProfile(String uid, String name, String email, String zip) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.zip = zip;
        this.role = "PATIENT";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
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
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getZip() {
        return zip;
    }

    public void setZip(String zip) {
        this.zip = zip;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
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

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getInsuranceNumber() {
        return insuranceNumber;
    }

    public void setInsuranceNumber(String insuranceNumber) {
        this.insuranceNumber = insuranceNumber;
    }

    public String getInsuranceCompany() {
        return insuranceCompany;
    }

    public void setInsuranceCompany(String insuranceCompany) {
        this.insuranceCompany = insuranceCompany;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getMedicalHistory() {
        return medicalHistory;
    }

    public void setMedicalHistory(String medicalHistory) {
        this.medicalHistory = medicalHistory;
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

    /**
     * Convert PatientProfile to a Map for Firestore storage
     */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        result.put("uid", uid);
        result.put("name", name);
        result.put("email", email);
        result.put("zip", zip);
        result.put("role", role);
        result.put("passwordHash", passwordHash);
        result.put("passwordSalt", passwordSalt);
        result.put("dateOfBirth", dateOfBirth);
        result.put("age", age);
        result.put("gender", gender);
        result.put("insuranceNumber", insuranceNumber);
        result.put("insuranceCompany", insuranceCompany);
        result.put("allergies", allergies);
        result.put("medicalHistory", medicalHistory);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        return result;
    }
}
