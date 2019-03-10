import * as functions from 'firebase-functions'
import * as admin from 'firebase-admin'

admin.initializeApp(functions.config().firebase)

export const onMessageNotificationCreate = functions.database
.ref('Users/{userId}/Notifications/messages/{groupId}/{notificationId}')
.onCreate(async (snapshot, context) => {
    const userId = context.params.userId
    const notificationId = context.params.notificationId
    const groupId = context.params.groupId
    console.log(`New notification ${notificationId} from userId - ${userId} to ${groupId}`)

    const notificationData = snapshot.val()
    const payload = getMessageNotificationpayload(notificationData, userId)

    await admin.messaging().sendToDevice(notificationData.deviceToken, payload)

    const countRef = snapshot.ref.root.child('Users').child(userId).child('Notifications').child('messages').child(groupId).child('notificationCount')
    return countRef.transaction(notificationCount => {
        return notificationCount + 1
    })
})

function getMessageNotificationpayload(notificationData: any, userId: any): any {
    
    let payload

    payload = {
        notification: {
            title: `New Message From ${notificationData.groupName}`,
            body:  `${notificationData.senderName}: ${notificationData.content}`,
            icon:  "default",
            sound: "default",
            tag: `${notificationData.senderName}`,
            click_action: "gis.hereim_MESSAGE_NOTIFICATION"
        },
        data: {
            group_id : notificationData.groupId,
            type : "message"
            }
        };

    return payload
}

export const onGroupRequestCreate = functions.database
.ref('/Users/{userId}/Notifications/groupRequests/{requestfromId}')
.onCreate((snapshot) => {
    const requestData = snapshot.val()
    const payload = getGroupRequestpayload(requestData)
    return admin.messaging().sendToDevice(requestData.deviceToken, payload)
})

function getGroupRequestpayload(requestData: any): any {
    
    let payload

    payload = {
        notification: {
            title: `New Group Request!`,
            body:  `${requestData.senderName} wants to join ${requestData.groupName} group chat`,
            icon:  "default",
            sound: "default",
            tag: `${requestData.senderName}`,
            click_action: "gis.hereim_GROUP_REQUEST_NOTIFICATION"
        },
        data: {
            group_id : requestData.groupId,
            type : "group_request"
            }
    };

    return payload
}

/*
export const onMessageCreate = functions.database
.ref('/Group Chats/{groupId}/messages/{messageId}')
.onCreate((snapshot, context) => {
    const groupId = context.params.groupId
    const messageId = context.params.messageId
    console.log(`New message ${messageId} in group - ${groupId}`)

    const timeStampRef = snapshot.ref.root.child(snapshot.val().senderId).child('groups').child(groupId).child('timeStamp')
    return timeStampRef.transaction(messageCount => {
        return messageCount + 1
    })
})


export const onMessageDelete = functions.database
.ref('/Group Chats/{groupId}/messages/{messageId}')
.onDelete((snapshot, context) => {
    const groupId = context.params.groupId
    const messageId = context.params.messageId
    console.log(`New message ${messageId} in group - ${groupId}`)

    const countRef = snapshot.ref.root.child('Group Chats').child(groupId).child('messageCount')
    return countRef.transaction(messageCount => {
        return messageCount - 1
    })
})*/