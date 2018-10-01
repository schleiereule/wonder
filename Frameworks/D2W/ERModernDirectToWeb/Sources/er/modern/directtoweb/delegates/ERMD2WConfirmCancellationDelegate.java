package er.modern.directtoweb.delegates;

import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.directtoweb.NextPageDelegate;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSMutableDictionary;

import er.directtoweb.ERDirectToWeb;
import er.directtoweb.pages.ERD2WInspectPage;
import er.directtoweb.pages.ERD2WPage;
import er.extensions.appserver.ERXSession;
import er.extensions.foundation.ERXValueUtilities;
import er.modern.directtoweb.ERMDNotificationNameRegistry;

public class ERMD2WConfirmCancellationDelegate implements NextPageDelegate {

    private WOComponent _originalSender;
    
    public ERMD2WConfirmCancellationDelegate(WOComponent originalSender) {
        _originalSender = originalSender;
    }
    
    public WOComponent nextPage(WOComponent sender) {
        D2WContext c = d2wContext(_originalSender);
        EOEnterpriseObject eo = object(_originalSender);
        ERD2WInspectPage page = ERD2WUtilities.enclosingComponentOfClass(_originalSender, ERD2WInspectPage.class);
        EOEditingContext ec = eo != null ? eo.editingContext() : null;
        if (ec != null && ERXValueUtilities.booleanValue(c.valueForKey("shouldRevertChanges"))) {
            ec.revert();
        }
        NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
        // if the parent page configuration ID is available, add it
        if (c.valueForKey("parentPageConfigurationID") != null) {
            userInfo.put("parentPageConfigurationID", c.valueForKey("parentPageConfigurationID"));
        }
        userInfo.put("pageConfiguration", c.valueForKey("pageConfiguration"));
        ERXSession.session().notificationCenter().postNotification(ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null, userInfo);
        return page.nextPage(false);
    }

    /**
     * Gets the D2W context from the innermost enclosing D2W component of the
     * sender.
     * 
     * @param sender
     */
    protected D2WContext d2wContext(WOComponent sender) {
        if (ERDirectToWeb.D2WCONTEXT_SELECTOR.implementedByObject(sender)) {
            return (D2WContext) sender.valueForKey(ERDirectToWeb.D2WCONTEXT_SELECTOR.name());
        }
        throw new IllegalStateException("Can't figure out d2wContext from: " + sender);
    }

    /**
     * return the innermost object which might be of interest
     * 
     * @param sender
     */
    protected EOEnterpriseObject object(WOComponent sender) {
        return object(d2wContext(sender));
    }

    /**
     * Returns the current object form the d2w context
     * 
     * @param context
     */
    protected EOEnterpriseObject object(D2WContext context) {
        return (EOEnterpriseObject) context.valueForKey(ERD2WPage.Keys.object);
    }

}
