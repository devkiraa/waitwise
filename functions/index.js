const functions = require("firebase-functions");
const admin = require("firebase-admin");

admin.initializeApp();

/**
 * Realtime Database trigger on /orders/{orderId}
 * Listens for orderStatus state transitions (e.g. ACCEPTED, PREPARING, READY, CANCELLED)
 * and dispatches targeted FCM push notifications to the user's registered device token.
 */
exports.onOrderStatusUpdated = functions.database
  .ref("/orders/{orderId}")
  .onUpdate(async (change, context) => {
    const beforeData = change.before.val();
    const afterData = change.after.val();

    if (!beforeData || !afterData) {
      return null;
    }

    const previousStatus = beforeData.orderStatus;
    const currentStatus = afterData.orderStatus;

    // Only send notification when the order status actually changes
    if (previousStatus === currentStatus) {
      return null;
    }

    const userId = afterData.userId;
    const tokenNumber = afterData.tokenNumber || "N/A";
    const orderId = context.params.orderId;

    if (!userId) {
      console.log(`Order ${orderId} has no associated userId.`);
      return null;
    }

    // Retrieve recipient's FCM registration token from /users/{userId}/fcmToken
    const userSnapshot = await admin.database().ref(`/users/${userId}`).once("value");
    const userData = userSnapshot.val();

    if (!userData || !userData.fcmToken) {
      console.log(`User ${userId} does not have a registered fcmToken.`);
      return null;
    }

    const fcmToken = userData.fcmToken;

    let notificationTitle = "WaitWise Order Status Update";
    let notificationBody = `Your order (Token #${tokenNumber}) is now ${currentStatus}.`;

    switch (currentStatus.toUpperCase()) {
      case "ACCEPTED":
        notificationTitle = "Order Accepted!";
        notificationBody = `Your order (Token #${tokenNumber}) has been accepted by the kitchen.`;
        break;
      case "PREPARING":
        notificationTitle = "Order in Preparation";
        notificationBody = `Your order (Token #${tokenNumber}) is now being prepared.`;
        break;
      case "READY":
        notificationTitle = "Your order is ready!";
        notificationBody = `Token #${tokenNumber} is ready for pickup. Please collect your order at the counter.`;
        break;
      case "CANCELLED":
        notificationTitle = "Order Cancelled";
        notificationBody = `Your order (Token #${tokenNumber}) was cancelled. Please check details in app.`;
        break;
      case "COLLECTED":
        notificationTitle = "Order Collected";
        notificationBody = `Thank you! Token #${tokenNumber} has been collected. Enjoy your meal!`;
        break;
    }

    const message = {
      token: fcmToken,
      notification: {
        title: notificationTitle,
        body: notificationBody,
      },
      data: {
        orderId: String(orderId),
        tokenNumber: String(tokenNumber),
        orderStatus: String(currentStatus),
      },
      android: {
        priority: "high",
        notification: {
          channelId: "waitwise_order_updates",
          priority: "high",
          defaultSound: true,
        },
      },
    };

    try {
      const response = await admin.messaging().send(message);
      console.log(`Successfully sent FCM message for Order ${orderId} (Token #${tokenNumber}): ${response}`);
      return response;
    } catch (error) {
      console.error(`Error sending FCM message for Order ${orderId}:`, error);
      if (
        error.code === "messaging/invalid-registration-token" ||
        error.code === "messaging/registration-token-not-registered"
      ) {
        await admin.database().ref(`/users/${userId}/fcmToken`).remove();
      }
      return null;
    }
  });
