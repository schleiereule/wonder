package er.coolcomponents;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;

import er.ajax.AjaxUtils;
import er.extensions.appserver.ERXWOContext;
import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXStringUtilities;

/**
 * Displays one or more notifications. This is basically a thin wrapper around
 * <a href="https://github.com/alertifyjs/alertify.js">alertify</a>.
 * Notifications that are to be displayed can be sent via NSNotification or by
 * calling one of the static methods provided.
 * 
 * @author fpeters
 */
public class CCNotifications extends WOComponent {

    private static final long serialVersionUID = 1L;

    private static final String SHOW_UI_NOTIFICATION = "ShowUiNotification";

    public CCNotifications(WOContext aContext) {
        super(aContext);
    }

    @Override
    public void awake() {
        super.awake();
        NSNotificationCenter.defaultCenter()
                .addObserver(this,
                        new NSSelector<Void>("displayNotification",
                                ERXConstant.NotificationClassArray),
                        SHOW_UI_NOTIFICATION, null);
    }

    @Override
    public void sleep() {
        NSNotificationCenter.defaultCenter().removeObserver(this, SHOW_UI_NOTIFICATION,
                null);
        super.sleep();
    }

    /**
     * Extracts a message from an NSNotification's object. The user info dictionary may
     * contain a "type" key, to set the notification type to pass to alertify
     * (e.g. "success", "error").
     * 
     * @param notification
     */
    public void displayNotification(NSNotification notification) {
        String notificationType = "success";
        if (notification.userInfo() != null
                && notification.userInfo().containsKey("type")) {
            notificationType = (String) notification.userInfo().valueForKey("type");
        }
        AjaxUtils.javascriptResponse(
                "alertify." + notificationType + "('" + notification.object() + "')",
                context());
    }

    public static void displayNotification(String message) {
        displaySuccessNotification(message);
    }
    
    public static void displaySuccessNotification(String message) {
        message = ERXStringUtilities.escapeJavascriptApostrophes(message);
        AjaxUtils.javascriptResponse("alertify.success('" + message + "')",
                ERXWOContext.currentContext());
    }
    
    public static void displayErrorNotification(String message) {
        message = ERXStringUtilities.escapeJavascriptApostrophes(message);
        AjaxUtils.javascriptResponse("alertify.error('" + message + "')",
                ERXWOContext.currentContext());
    }
    
}
