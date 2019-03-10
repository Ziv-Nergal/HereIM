package database_classes;

public class Message {

    private String msgId;
    private String msgText;
    private String msgType;
    private String senderId;
    private String senderName;
    private String senderPhotoUri;
    private long timeStamp;
    private String senderColor;

    public Message() {}

    public Message(String msgId, String msgText, String msgType, String senderId, String senderName, String senderPhotoUri, long timeStamp, String color) {
        this.msgId = msgId;
        this.msgText = msgText;
        this.msgType = msgType;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderPhotoUri = senderPhotoUri;
        this.timeStamp = timeStamp;
        this.senderColor = color;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getMsgText()
    {
        return msgText;
    }

    public String getMsgType()
    {
        return msgType;
    }

    public String getSenderId()
    {
        return senderId;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderPhotoUri() {
        return senderPhotoUri;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public String getSenderColor() {
        return senderColor;
    }
}
