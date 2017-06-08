package er.coolcomponents;

import java.util.UUID;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
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
import er.extensions.localization.ERXLocalizer;

/**
 * Displays one or more notifications. This is basically a thin wrapper around
 * <a href="https://github.com/alertifyjs/alertify.js">alertify</a>.
 * Notifications that are to be displayed can be sent via NSNotification or by
 * calling one of the static methods provided.<br>
 * To influence the display style of the notification, choose among the three
 * types "alert", "error" and "success". The "alert" type will stay visible until
 * the user has clicked on it.<br>
 * To limit the display of an NSNotification to the current session, set the
 * sessionID key of the notification's userInfo dictionary to the session's
 * ID.<br>
 * An object implementing NSKeyValueCoding that is passed as the notification
 * object of an NSNotification or via the static methods, can be accessed from
 * localization templates.
 * 
 * @author fpeters
 */
public class CCNotifications extends ERXComponent {

    private static final long serialVersionUID = 1L;

    public static final String CC_NOTIFICATION = "CCNotification";

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
                        new NSSelector<Void>("displayNSNotification",
                                ERXConstant.NotificationClassArray),
                        CC_NOTIFICATION, null);
    }

    @Override
    public void sleep() {
        NSNotificationCenter.defaultCenter().removeObserver(this, CC_NOTIFICATION, null);
        super.sleep();
    }

    /**
     * Displays an NSNotification by retrieving the "message" key of the
     * notification's userInfo dictionary. The user info dictionary may also
     * contain a "type" key, set to one of the valid notification types and a
     * "sessionID" key that will limit display of the notification to that
     * session. If the notification's object is not null and implementing
     * NSKeyValueCoding, it is made available for access during localization.
     * 
     * @param notification
     */
    public void displayNSNotification(NSNotification notification) {
        String message = null;
        TYPE notificationType = TYPE.success;
        boolean sessionMatches = true;
        if (notification.userInfo() != null) {
            if (notification.userInfo().containsKey("message")) {
                message = (String) notification.userInfo().valueForKey("message");
            }
            if (notification.userInfo().containsKey("type")) {
                notificationType = (TYPE) notification.userInfo().valueForKey("type");
            }
            if (notification.userInfo().containsKey("sessionID") && !session().sessionID()
                    .equals(notification.userInfo().valueForKey("sessionID"))) {
                sessionMatches = false;
            }
        }
        if (sessionMatches) {
            if (notification.object() != null
                    && notification.object() instanceof NSKeyValueCoding) {
                notify((NSKeyValueCoding) notification.object(), message,
                        notificationType);
            } else {
                notify(message, notificationType);
            }
        }
    }

    public static void notify(String message, TYPE type) {
        notify(null, message, type,
                new NSTimestamp().timestampByAddingGregorianUnits(0, 0, 0, 0, 0, 5));
    }

    public static void notify(NSKeyValueCoding object, String message, TYPE type) {
        notify(object, message, type,
                new NSTimestamp().timestampByAddingGregorianUnits(0, 0, 0, 0, 0, 5));
    }

    public static void notify(NSKeyValueCoding object,
                              String message,
                              TYPE type,
                              NSTimestamp expiry) {
        NSDictionary<String, Object> guiNotification = new NSDictionary<String, Object>(
                new Object[] { object, message, type, expiry, UUID.randomUUID() },
                new String[] { "object", "message", "type", "expiry", "guid" });
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
        String rawMessage = (String) notification.valueForKey("message");
        ERXLocalizer localizer = ERXLocalizer.currentLocalizer();
        String localizedMessage = null;
        if (notification.valueForKey("object") != null) {
            NSKeyValueCoding object = (NSKeyValueCoding) notification
                    .valueForKey("object");
            localizedMessage = localizer
                    .localizedTemplateStringForKeyWithObject(rawMessage, object);
        } else {
            localizedMessage = localizer.localizedStringForKeyWithDefault(rawMessage);
        }
        alertifyNotification.append(localizedMessage);
        alertifyNotification.append("');");
        return alertifyNotification.toString();
    }

}
