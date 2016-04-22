package er.modern.look.pages;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.pages.templates.ERD2WInspectPageTemplate;
import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXValueUtilities;
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
 */
public class ERMODInspectPage extends ERD2WInspectPageTemplate {
	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
	 * Affecting Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
	 * Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public interface Keys extends ERD2WInspectPageTemplate.Keys {
		public static final String task = "task";
		public static final String inlineTask = "inlineTask";
		public static final String objectBeingEdited = "objectBeingEdited";
		public static final String useAjaxControlsWhenEmbedded = "useAjaxControlsWhenEmbedded";
		public static final String inlinePageConfiguration = "inlinePageConfiguration";
	}

	private String _previousPageConfig;
	private String _previousTask;

	public ERMODInspectPage(WOContext wocontext) {
		super(wocontext);
	}

	@Override
	public void awake() {
		NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector<Void>("handleSaveNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
		NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector<Void>("handleCancelEditNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.awake();
		clearValidationFailed();
	}

	@Override
	public void sleep() {
		NSNotificationCenter.defaultCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
		NSNotificationCenter.defaultCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.sleep();
	}

	public void handleSaveNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			resetTask();
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForParentMainContainer"), context());
		}
	}

	public void handleCancelEditNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			resetTask();
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForParentMainContainer"), context());
		}
	}

	private Boolean shouldHandleNotification(NSNotification notification) {
		if (notification.userInfo() != null) {
			Object no = notification.userInfo().valueForKey("pageConfiguration");
			if ((no != null) && (no instanceof String)) {
				if (((String) d2wContext().valueForKey("pageConfiguration")).equalsIgnoreCase((String) no)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Perform the edit action. Overridden to use ajax behaviour if
	 * useAjaxControlsWhenEmbedded is true
	 */
	@Override
	public WOComponent editAction() {
		boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
		if (useAjax) {
			EOEditingContext ec = ERXEC.newEditingContext(object().editingContext());
			EOEnterpriseObject localObj = ERXEOControlUtilities.localInstanceOfObject(ec, object());
			d2wContext().takeValueForKey(localObj, Keys.objectBeingEdited);
			_previousPageConfig = (String) d2wContext().valueForKey(Keys.pageConfiguration);
			_previousTask = (String) d2wContext().valueForKey(Keys.task);
			d2wContext().takeValueForKey("edit", Keys.inlineTask);
			String newConfig = (String) d2wContext().valueForKey(Keys.inlinePageConfiguration);
			d2wContext().takeValueForKey(newConfig, Keys.pageConfiguration);
			d2wContext().takeValueForKey("edit", Keys.task);
			return null;
		} else {
			return super.editAction();
		}
	}

	private void resetTask() {
		if (_previousPageConfig != null)
			d2wContext().takeValueForKey(_previousPageConfig, Keys.pageConfiguration);
		if (_previousTask != null)
			d2wContext().takeValueForKey(_previousTask, Keys.task);
		_previousPageConfig = null;
		_previousTask = null;
	}

}
