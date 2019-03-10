package database_classes;

/*
This class is a firebase adapted class so variable names are matching those in firebase database.
*/

import java.io.Serializable;
import java.util.Map;

public class GroupChat implements Serializable {

    private String groupId;
    private String adminId;
    private String adminName;
    private String adminDeviceToken;
    private String groupName;
    private String lastMsg;
    private String groupPhoto;
    private long timeStamp;
    private Map<String, Object> groupUsers;

    public GroupChat(String groupId, String adminId, String adminName, String adminDeviceToken, String groupName, String lastMsg, String groupPhoto, long timeStamp, Map<String, Object> groupUsers) {
        this.groupId = groupId;
        this.adminId = adminId;
        this.adminName = adminName;
        this.adminDeviceToken = adminDeviceToken;
        this.groupName = groupName;
        this.lastMsg = lastMsg;
        this.groupPhoto = groupPhoto;
        this.timeStamp = timeStamp;
        this.groupUsers = groupUsers;
    }

    public GroupChat() {}

    public String getGroupId() {
        return groupId;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getAdminName() {
        return adminName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getLastMsg() {
        return lastMsg;
    }

    public String getGroupPhoto() {
        return groupPhoto;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Map<String, Object> getGroupUsers() {
        return groupUsers;
    }

    public String getAdminDeviceToken() {
        return adminDeviceToken;
    }
}
