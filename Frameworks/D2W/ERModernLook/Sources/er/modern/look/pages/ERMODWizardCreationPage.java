package er.modern.look.pages;

import org.apache.commons.lang3.ObjectUtils;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSNotification;
import com.webobjects.foundation.NSSelector;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.pages.templates.ERD2WWizardCreationPageTemplate;
import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXConstant;
import er.extensions.foundation.ERXValueUtilities;
import er.modern.directtoweb.ERMDNotificationNameRegistry;

/**
 * A wizard inspect/edit template. Can be used in-line, and supports ajax updates
 * 
 * @d2wKey cancelButtonLabel
 * @d2wKey bannerFileName
 * @d2wKey showBanner
 * @d2wKey headerComponentName
 * @d2wKey formEncoding
 * @d2wKey repetitionComponentName
 * @d2wKey previousButtonLabel
 * @d2wKey pageWrapperName
 * @d2wKey nextButtonLabel
 * @d2wKey saveButtonLabel
 * @d2wKey useAjaxControlsWhenEmbedded
 * 
 * @author davidleber
 * @author schleiereule
 * 
 */
public class ERMODWizardCreationPage extends ERD2WWizardCreationPageTemplate {
  /**
   * Do I need to update serialVersionUID?
   * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
   * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
   */
  private static final long serialVersionUID = 1L;

	public interface Keys extends ERD2WWizardCreationPageTemplate.Keys{
		public static final String useAjaxControlsWhenEmbedded = "useAjaxControlsWhenEmbedded";
		public static final String parentPageConfiguration = "parentPageConfiguration";
		public static final String idForParentMainContainer = "idForParentMainContainer";
		public static final String idForMainContainer = "idForMainContainer";
	}
	
	public ERMODWizardCreationPage(WOContext wocontext) {
		super(wocontext);
	}
	
	@Override
	public void awake() {
		ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleSaveNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
		ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleCancelEditNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handleNextStepNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_NEXT_STEP_ACTION, null);
		ERXSession.session().notificationCenter().addObserver(this, new NSSelector<Void>("handlePreviousStepNotification", ERXConstant.NotificationClassArray),
				ERMDNotificationNameRegistry.BUTTON_PERFORMED_PREVIOUS_STEP_ACTION, null);
		super.awake();
		clearValidationFailed();
	}
	
	@Override
	public void sleep() {
		ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null);
		ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null);
		ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_NEXT_STEP_ACTION, null);
		ERXSession.session().notificationCenter().removeObserver(this, ERMDNotificationNameRegistry.BUTTON_PERFORMED_PREVIOUS_STEP_ACTION, null);
		super.sleep();
	}

	public void handleSaveNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForParentMainContainer"), context());
		}
	}

	public void handleCancelEditNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForParentMainContainer"), context());
		}
	}
	
	public void handleNextStepNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForMainContainer"), context());
		}
	}
	
	public void handlePreviousStepNotification(NSNotification notification) {
		if (shouldHandleNotification(notification)) {
			boolean useAjax = ERXValueUtilities.booleanValue(d2wContext().valueForKey(Keys.useAjaxControlsWhenEmbedded));
			if (useAjax)
				AjaxUpdateContainer.safeUpdateContainerWithID((String) d2wContext().valueForKey("idForMainContainer"), context());
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
				if (((String) d2wContext().valueForKey("pageConfiguration")).equalsIgnoreCase((String) no)) {
					return true;
				}
			}
		}
		return false;
	}
	
	
	// previousStep() {
    // if we had an error message and are going back, we don't want the message
    // to show up on the previous page; the error message will reappear
    // when the user gets back to the initial page
    // errorMessages = new NSMutableDictionary();
	
	// nextStep() {
    // FIXME: This is no longer needed.  We now have validationKeys that will serve the same purpose.
    // NSNotificationCenter.defaultCenter().postNotification(ERD2WWizardCreationPage.WILL_GOTO_NEXT_PAGE, null);
    // if (errorMessages.count()==0 && _currentStep < tabSectionsContents().count())
	
	
	/**
	 * Sets the page object. Overridden to reset the current step if the object changes
	 */
	@Override
	public void setObject(EOEnterpriseObject eoenterpriseobject) {
		// If we are getting a new EO, then reset the current step.
		if (eoenterpriseobject != null && !eoenterpriseobject.equals(object())) {
			_currentStep = 1;
		}
		super.setObject(eoenterpriseobject);
	}
	
	// What follows is a hack.
	// I am not proud of it, but there it is.
	// This is necessary because the wizard component will blow chunks
	// if you don't clear its tabSectionContents if the d2wContext's entity
	// changes when embedded, in-line, and updated with ajax requests.
	// davidleber
	
	private EOEntity _cachedEntity;
	
	@Override
	public D2WContext d2wContext() {
		D2WContext result = super.d2wContext();
		if (_cachedEntity == null) {
			_cachedEntity = result.entity();
		} else if (ObjectUtils.notEqual(_cachedEntity, result.entity())) {
			clearTabSectionsContents();
			result.takeValueForKey(null, "tabSectionsContents");
		    _cachedEntity = result.entity();
		}
		return super.d2wContext();
	}
}
