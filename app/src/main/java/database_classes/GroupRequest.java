package database_classes;

public class GroupRequest {

    private String deviceToken;
    private String groupId;
    private String groupName;
    private String senderName;
    private String senderId;
    private String senderPhoto;
    private String type;

    public GroupRequest() { }

    public String getDeviceToken() {
        return deviceToken;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getType() {
        return type;
    }

    public String getSenderPhoto() {
        return senderPhoto;
    }

    public String getSenderId() {
        return senderId;
    }
}
