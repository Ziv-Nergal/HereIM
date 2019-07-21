package database_classes;

/*
This class is a Firebase adapted class so variable names are matching those in Firebase database.
*/

public class GroupUser {

    private String uid;
    private String deviceToken;
    private String email;
    private String fullName;
    private String photoUri;
    private String status;
    private UserLocation location;
    private boolean isSharingLocation;

    private boolean online;

    public GroupUser() {}

    public GroupUser(String uid, String deviceToken, String email, String fullName,
                     boolean online, String photoUri, String status,
                     boolean isSharingLocation) {
        this.uid = uid;
        this.deviceToken = deviceToken;
        this.email = email;
        this.fullName = fullName;
        this.online = online;
        this.photoUri = photoUri;
        this.status = status;
        this.isSharingLocation = isSharingLocation;
    }

    public String getUid() {
        return uid;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isOnline() {
        return online;
    }

    public String getPhotoUri() {
        return photoUri;
    }

    public String getStatus() {
        return status;
    }

    public UserLocation getLocation() {
        return location;
    }

    public boolean getIsSharingLocation() { return isSharingLocation; }
}
