package rakib.bcs430healthcareproject;

/**
 * Manages the current logged-in user context across the application.
 * This is a simple singleton to store the current user's UID and profile information.
 */
public class UserContext {
    private static UserContext instance;
    private String uid;
    private PatientProfile profile;

    private UserContext() {}

    public static synchronized UserContext getInstance() {
        if (instance == null) {
            instance = new UserContext();
        }
        return instance;
    }

    public void setUserData(String uid, PatientProfile profile) {
        this.uid = uid;
        this.profile = profile;
    }

    public void clearUserData() {
        this.uid = null;
        this.profile = null;
    }

    public String getUid() {
        return uid;
    }

    public PatientProfile getProfile() {
        return profile;
    }

    public String getEmail() {
        return profile != null ? profile.getEmail() : null;
    }

    public String getName() {
        return profile != null ? profile.getName() : null;
    }

    public boolean isLoggedIn() {
        return uid != null;
    }

    public void updateProfile(PatientProfile updatedProfile) {
        this.profile = updatedProfile;
    }
}
