package er.coolcomponents;

import java.util.UUID;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSTimestamp;

import er.ajax.AjaxUtils;
import er.extensions.appserver.ERXSession;
import er.extensions.appserver.ERXWOContext;
import er.extensions.components.ERXComponent;
import er.extensions.eof.ERXConstant;

/**
 * Displays one or more notifications. This is basically a thin wrapper around
 * <a href="https://github.com/alertifyjs/alertify.js">alertify</a>.
 * Notifications that are to be displayed can be sent via NSNotification or by
 * calling one of the static methods provided.
 * 
 * @author fpeters
 */
public class CCNotifications extends ERXComponent {

    private static final long serialVersionUID = 1L;

    private static final String CC_NOTIFICATION = "CCNotification";

    public static enum TYPE {
        alert, error, success
    };

    public NSDictionary<String, Object> aNotification;

    public CCNotifications(WOContext aContext) {
        super(aContext);
    }

    @Override
    public void awake() {
        super.awake();
        NSNotificationCenter.defaultCenter()
                .addObserver(this,
                        new NSSelector<Void>("publishNSNotification",
                                ERXConstant.NotificationClassArray),
                        CC_NOTIFICATION, null);
    }

    @Override
    public void sleep() {
        NSNotificationCenter.defaultCenter().removeObserver(this, CC_NOTIFICATION,
                null);
        super.sleep();
    }

    /**
     * Extracts a message from an NSNotification's object. The user info
     * dictionary may contain a "type" key, set to one of the valid notification
     * types.
     * 
     * @param notification
     */
    public void publishNSNotification(NSNotification notification) {
        TYPE notificationType = TYPE.success;
        if (notification.userInfo() != null
                && notification.userInfo().containsKey("type")) {
            notificationType = (TYPE) notification.userInfo().valueForKey("type");
        }
        notify((String) notification.object(), notificationType);
    }

    public static void notify(String message, TYPE type) {
        notify(message, type,
                new NSTimestamp().timestampByAddingGregorianUnits(0, 0, 0, 0, 0, 5));
    }

    public static void notify(String message, TYPE type, NSTimestamp expiry) {
        NSDictionary<String, Object> guiNotification = new NSDictionary<String, Object>(
                new Object[] { message, type, expiry, UUID.randomUUID() },
                new String[] { "message", "type", "expiry", "guid" });
        // for ajax requests, directly publish the notification
        if (AjaxUtils.isAjaxRequest(ERXWOContext.currentContext().request())) {
            AjaxUtils.javascriptResponse(alertifyNotification(guiNotification),
                    ERXWOContext.currentContext());
        } else {
            // for full page loads, stash the notification on the session
            @SuppressWarnings("unchecked")
            NSArray<NSDictionary<String, Object>> notifications = (NSArray<NSDictionary<String, Object>>) ERXSession
                    .session().objectStore().valueForKeyPath("CCNotifications");
            if (notifications == null) {
                notifications = new NSMutableArray<NSDictionary<String, Object>>();
            } else {
                notifications = notifications.mutableClone();
            }
            notifications.add(guiNotification);
            ERXSession.session().objectStore().takeValueForKeyPath(
                    notifications.immutableClone(), "CCNotifications");
        }
    }

    /**
     * @return a list of pending notifications, i.e. those that are of type
     *         "alert" or have not expired
     */
    public NSArray<NSDictionary<String, Object>> pendingNotifications() {
        NSMutableArray<NSDictionary<String, Object>> pendingNotifications = new NSMutableArray<>();
        @SuppressWarnings("unchecked")
        NSArray<NSDictionary<String, Object>> notifications = (NSArray<NSDictionary<String, Object>>) ERXSession
                .session().objectStore().valueForKeyPath("CCNotifications");
        if (notifications != null) {
            for (NSDictionary<String, Object> aNotification : notifications) {
                boolean isNotExpired = new NSTimestamp()
                        .before((NSTimestamp) aNotification.valueForKey("expiry"));
                boolean isAlert = TYPE.alert.equals(aNotification.valueForKey("type"));
                if (isNotExpired || isAlert) {
                    pendingNotifications.addObject(aNotification);
                }
            }
            notifications = pendingNotifications.immutableClone();
            // replace the complete notification list with the filtered one
            ERXSession.session().objectStore().takeValueForKeyPath(notifications,
                    "CCNotifications");
        }
        return notifications;
    }

    /**
     * @return the script string to trigger display of a GUI notification
     */
    public String alertifyNotification() {
        return alertifyNotification(aNotification);
    }

    /**
     * @param notification
     * @return the script string to trigger display of a GUI notification
     */
    private static String alertifyNotification(NSDictionary<String, Object> notification) {
        StringBuilder alertifyNotification = new StringBuilder("alertify.");
        if (TYPE.alert.equals(notification.valueForKey("type"))) {
            alertifyNotification.append("alert");
        } else if (TYPE.error.equals(notification.valueForKey("type"))) {
            alertifyNotification.append("error");
        } else {
            alertifyNotification.append("success");
        }
        alertifyNotification.append("('");
        alertifyNotification.append(notification.valueForKey("message"));
        alertifyNotification.append("');");
        return alertifyNotification.toString();
    }

}
