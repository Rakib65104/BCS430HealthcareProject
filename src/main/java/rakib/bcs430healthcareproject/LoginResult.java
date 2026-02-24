package rakib.bcs430healthcareproject;

public class LoginResult {
    private final String uid;
    private final String role;

    public LoginResult(String uid, String role) {
        this.uid = uid;
        this.role = role;
    }

    public String getUid() { return uid; }
    public String getRole() { return role; }
}