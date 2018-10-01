package er.modern.look.pages;

import org.apache.commons.lang3.ObjectUtils;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSSelector;

import er.directtoweb.pages.templates.ERD2WTabInspectPageTemplate;
import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXConstant;
import er.modern.directtoweb.ERMDNotificationNameRegistry;

/**
 * A modernized tab inspect/edit template.
 * 
 * @d2wKey cancelButtonLabel
 * @d2wKey printerButtonComponentName
 * @d2wKey editButtonLabel
 * @d2wKey formEncoding
 * @d2wKey hasForm
 * @d2wKey headerComponentName
 * @d2wKey repetitionComponentName
 * @d2wKey actionBarComponentName
 * @d2wKey controllerButtonComponentName
 * @d2wKey pageWrapperName
 * @d2wKey returnButtonLabel
 * @d2wKey saveButtonLabel
 * @d2wKey useFocus
 * @d2wKey useAjaxControlsWhenEmbedded
 * 
 * @author davidleber
 * @author schleiereule
 */
public class ERMODTabInspectPage extends ERD2WTabInspectPageTemplate {
	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
	 * Affecting Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
	 * Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public interface Keys extends ERD2WTabInspectPageTemplate.Keys {
		public static final String task = "task";
		public static final String inlineTask = "inlineTask";
		public static final String objectBeingEdited = "objectBeingEdited";
		public static final String useAjaxControlsWhenEmbedded = "useAjaxControlsWhenEmbedded";
		public static final String inlinePageConfiguration = "inlinePageConfiguration";
	}

	private String _previousPageConfig;
	private String _previousTask;

	public ERMODTabInspectPage(WOContext wocontext) {
		super(wocontext);
	}
	
	@Override
	public void awake() {
	    ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleSaveNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
	    ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleCancelEditNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.awake();
	}

	@Override
	public void sleep() {
	    ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
	    ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.sleep();
	}

	public void handleSaveNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			resetTask();
		}
	}

	public void handleCancelEditNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			resetTask();
		}
	}

	/**
	 * Prevent the update if the notification targets a different pageConfiguration
	 * @param notification
	 * @return
	 */
	private Boolean shouldHandleNotification(NSNotification notification) {
		if (notification.userInfo() != null) {
			Object no = notification.userInfo().valueForKey("pageConfiguration");
			if ((no != null) && (no instanceof String)) {
				if (((String) no).equalsIgnoreCase((String) d2wContext().valueForKey("pageConfiguration"))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Reset the behaviour of the page to it's original one (i.e: if it was
	 * inspect that was switched to edit).
	 */
	private void resetTask() {
		if (_previousPageConfig != null)
			d2wContext().takeValueForKey(_previousPageConfig, Keys.pageConfiguration);
		if (_previousTask != null)
			d2wContext().takeValueForKey(_previousTask, Keys.task);
		_previousPageConfig = null;
		_previousTask = null;
	}

	/**
	 * Set the page's object. Overridden to reset the tab if the eo changes.
	 * Needed to support in-line ajax usage.
	 */
	@Override
	public void setObject(EOEnterpriseObject eoenterpriseobject) {
		clearTabSectionsContents();
		super.setObject(eoenterpriseobject);
	}

	// Force the tabSectionsContents to regenerate
	// if the task changes (i.e: ajax inline inspect -> edit)
	private String _previousTaskContext;

	@Override
	public D2WContext d2wContext() {
		D2WContext result = super.d2wContext();
		if (_previousTaskContext == null) {
			_previousTaskContext = result.task();
		} else if (ObjectUtils.notEqual(_previousTaskContext, result.task())) {
			clearTabSectionsContents();
			_previousTaskContext = result.task();
		}
		return super.d2wContext();
	}
}
