package er.modern.look.pages;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSSelector;

import er.directtoweb.pages.templates.ERD2WInspectPageTemplate;
import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXStringUtilities;
import er.modern.directtoweb.ERMDNotificationNameRegistry;

/**
 * Modernized inspect page.
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
 * 
 * @author davidleber
 * @author schleiereule
 */
public class ERMODInspectPage extends ERD2WInspectPageTemplate {
	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes Affecting
	 * Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public interface Keys extends ERD2WInspectPageTemplate.Keys {
		public static final String task = "task";
		public static final String inlineTask = "inlineTask";
		public static final String objectBeingEdited = "objectBeingEdited";
		public static final String useAjaxControlsWhenEmbedded = "useAjaxControlsWhenEmbedded";
		public static final String inlinePageConfiguration = "inlinePageConfiguration";

		public static final String previousPageConfiguration = "previousPageConfiguration";
		public static final String previousTask = "previousTask";
	}

	public ERMODInspectPage(WOContext wocontext) {
		super(wocontext);
	}

	@Override
	public void awake() {
	    ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleSaveNotification", ERXConstant.NotificationClassArray), ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
	    ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleCancelEditNotification", ERXConstant.NotificationClassArray), ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.awake();
		clearValidationFailed();
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
	 * 
	 * @param notification
	 * @return
	 */
	private Boolean shouldHandleNotification(NSNotification notification) {
		if (notification.userInfo() != null) {
			Object no = notification.userInfo().valueForKey(Keys.pageConfiguration);
			if ((no != null) && (no instanceof String)) {
				if (((String) d2wContext().valueForKey(Keys.pageConfiguration)).equalsIgnoreCase((String) no)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Reset the behaviour of the page to it's original one (i.e: if it was inspect that was
	 * switched to edit).
	 */
	private void resetTask() {
		String _previousPageConfig = (String) d2wContext().valueForKey(Keys.previousPageConfiguration);
		if (ERXStringUtilities.isNotBlank(_previousPageConfig)) {
			d2wContext().takeValueForKey(_previousPageConfig, Keys.pageConfiguration);
		}
		String _previousTask = (String) d2wContext().valueForKey(Keys.previousTask);
		if (ERXStringUtilities.isNotBlank(_previousTask)) {
			d2wContext().takeValueForKey(_previousTask, Keys.task);
		}
		d2wContext().takeValueForKey(null, Keys.previousPageConfiguration);
		d2wContext().takeValueForKey(null, Keys.previousTask);
	}

}
