package er.modern.look.pages;

import org.apache.commons.lang3.ObjectUtils;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSNotificationCenter;
import com.webobjects.foundation.NSSelector;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.pages.templates.ERD2WTabInspectPageTemplate;
import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.modern.directtoweb.ERMDNotificationNameRegistry;
import er.modern.look.pages.ERMODInspectPage.Keys;

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
		NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector<Void>("handleSaveNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
		NSNotificationCenter.defaultCenter().addObserver(this, new NSSelector<Void>("handleCancelEditNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		super.awake();
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
	 * Perform the edit action. Overridden to support ajax behaviour. When
	 * useAjaxControlsWhenEmbedded is true, then we will switch the behaviour of
	 * this page to edit and update ajax update the form.
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
