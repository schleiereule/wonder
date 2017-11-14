package er.modern.directtoweb.components.buttons;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import er.directtoweb.delegates.ERDBranchDelegate;
import er.directtoweb.delegates.ERDBranchDelegateInterface;
import er.directtoweb.delegates.ERDBranchInterface;
import er.extensions.foundation.ERXStringUtilities;

public class ERMDPageActionControllerButton extends ERMDActionButton implements ERDBranchInterface {

	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes Affecting
	 * Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	/** logging support */
	private static final Logger log = Logger.getLogger(ERMDPageActionControllerButton.class);

	public ERMDPageActionControllerButton(WOContext context) {
		super(context);
	}

	public static interface Keys {
		public static final String pageActionController = "pageActionController";
		public static final String pageActionConfiguration = "pageActionConfiguration";
		public static final String updateContainerKey = "updateContainerKey";
		public static final String hotkey = "hotkey";
		public static final String buttonLabel = "buttonLabel";
		public static final String auxiliaryCssClass = "auxiliaryCssClass";
	}

	// ---------------- CSS Support --------------------//
	public String buttonCssClass() {
		String cssClass = "Button PageButton";
		String branchName = branchName();
		if (branchName != null) {
			if (branchName.startsWith("_"))
				branchName = branchName.substring(1);
			cssClass = cssClass + " " + ERXStringUtilities.capitalize(branchName) + "PageButton" + auxiliaryCssClass();
		}
		return cssClass;
	}

	public String actionListCssClass() {
		String cssClass = "ActionList";
		String pageConfiguration = (String) d2wContext().valueForKey("pageConfiguration");
		if (pageConfiguration != null) {
			cssClass = cssClass + " " + pageConfiguration + "ActionList";
		}
		return cssClass;
	}

	public ERDBranchDelegateInterface branchDelegate() {
		if (branchDelegate == null) {
			WOComponent current = parent();
			while (current != null) {
				if (current instanceof D2WPage) {
					Object pageActionsDelegateClass = d2wContext().valueForKey(Keys.pageActionController);
					branchDelegate = (ERDBranchDelegateInterface) pageActionsDelegateClass;
					return branchDelegate;
				}
				current = current.parent();
			}
		}
		return branchDelegate;
	}

	public WOComponent nextPageFromParent() {
		if (branchDelegate() == null)
			return null;
		return branchDelegate().nextPage(this);
	}

	/** override this */
	public WOComponent performAction() {
		return nextPageFromParent();
	}

	@Override
	public void reset() {
		super.reset();
		branch = null;
		branchChoices = null;
		branchDelegate = null;
		branchConfiguration = null;
	}

	protected NSDictionary<String, NSDictionary<String, Object>> branchConfiguration;

	public NSDictionary<String, NSDictionary<String, Object>> branchConfiguration() {
		if (branchConfiguration == null) {
			WOComponent current = parent();
			while (current != null) {
				if (current instanceof D2WPage) {
					branchConfiguration = (NSDictionary) d2wContext().valueForKey(Keys.pageActionConfiguration);
					return branchConfiguration;
				}
				current = current.parent();
			}
		}
		return branchConfiguration;
	}

	/***
	 * gets the update container id
	 * 
	 * @return the update container id
	 */
	public String updateContainerID() {
		if (branchConfiguration() != null) {
            NSDictionary<String, Object> c = (NSDictionary<String, Object>) branchConfiguration().objectForKey(branchName());
			if (c != null) {
				String updateContainerKey = (String) d2wContext().valueForKey((String) c.objectForKey(Keys.updateContainerKey));
				if (ERXStringUtilities.isNotBlank(updateContainerKey))
					return updateContainerKey;
			}
		}
		return (String) d2wContext().valueForKey("idForMainContainer");
	}

	/**
	 * gets the button hotkey
	 * 
	 * @return the hotkey
	 */
	public String hotkey() {
		if (branchConfiguration() != null) {
            NSDictionary<String, Object> c = (NSDictionary<String, Object>) branchConfiguration().objectForKey(branchName());
			if (c != null) {
				String hotKey = (String) c.objectForKey(Keys.hotkey);
				if (ERXStringUtilities.isNotBlank(hotKey))
					return hotKey;
			}
		}
		return branchHotkey();
	}
	
	/**
     * gets the button label
     * 
     * @return the button label
     */
    public String buttonLabel() {
        String label = branchButtonLabel();
        if (branchConfiguration() != null) {
            NSDictionary<String, Object> c = (NSDictionary<String, Object>) branchConfiguration().objectForKey(branchName());
            if (c != null) {
                String buttonLabel = (String) c.objectForKey(Keys.buttonLabel);
                if (ERXStringUtilities.isNotBlank(buttonLabel))
                   label = buttonLabel;
            }
        }
        return label;
    }

    /**
     * gets the button's auxiliary CSS class
     * 
     * @return the auxiliary CSS class
     */
    public String auxiliaryCssClass() {
        if (branchConfiguration() != null) {
            NSDictionary<String, Object> c = (NSDictionary<String, Object>) branchConfiguration().objectForKey(branchName());
            if (c != null) {
                String auxiliaryCssClass = (String) c.objectForKey(Keys.auxiliaryCssClass);
                if (ERXStringUtilities.isNotBlank(auxiliaryCssClass))
                    return " " + auxiliaryCssClass;
            }
        }
        return "";
    }



	// ---------------- Branch Delegate Support --------------------//
	/** holds the chosen branch */
	protected NSDictionary branch;
	protected NSArray branchChoices;
	protected ERDBranchDelegateInterface branchDelegate;

	/**
	 * Cover method for getting the choosen branch.
	 * 
	 * @return user choosen branch.
	 */
	public NSDictionary branch() {
		return branch;
	}

	/**
	 * Sets the user chosen branch.
	 * 
	 * @param value
	 *            branch chosen by user.
	 */
	public void setBranch(NSDictionary value) {
		branch = value;
		d2wContext().takeValueForKey(value, "branch");
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets the user selected
	 * branch name.
	 * 
	 * @return user selected branch name.
	 */
	public String branchName() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_NAME);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets the user selected
	 * branch prefix.
	 * 
	 * @return user selected branch prefix.
	 */
	public String branchPrefix() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_PREFIX);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets the user selected
	 * branch label.
	 * 
	 * @return user selected branch label.
	 */
	public String branchButtonLabel() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_LABEL);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets the user selected
	 * branch button id.
	 * 
	 * @return user selected branch button id.
	 */
	public String branchButtonID() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_BUTTON_ID);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}
	 * 
	 * @return true if the selected branch requires a form submit.
	 */
	public Boolean branchRequiresFormSubmit() {
		return (Boolean) branch().valueForKey(ERDBranchDelegate.BRANCH_REQUIRESFORMSUBMIT);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}
	 * 
	 * @return a hotkey to bind to the branch action
	 */
	public String branchHotkey() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_HOTKEY);
	}

	public Boolean dontSubmitForm() {
		if (branchRequiresFormSubmit())
			return false;
		else
			return true;
	}

	// Hack for ERMODWizardCreationPage because the next/previous buttons must
	// use an AjaxSubmitButton
	public Boolean useAjaxSubmitButton() {
		if ((branchButtonID().startsWith("_nextStep")) || (branchButtonID().startsWith("_prevStep")))
			return true;
		else
			return false;
	}

	/**
	 * Calculates the branch choices for the current page. This method is just a cover for calling
	 * the method <code>branchChoicesForContext</code> on the current {@link ERDBranchDelegate
	 * ERDBranchDelegate}.
	 * 
	 * @return array of branch choices
	 */
	public NSArray branchChoices() {
		if (branchDelegate() != null) {
			branchChoices = branchDelegate().branchChoicesForContext(d2wContext());
		} else {
			branchChoices = NSArray.EmptyArray;
		}
		return branchChoices;
	}

	/**
	 * Determines if this message page should display branch choices.
	 * 
	 * @return if the current delegate supports branch choices.
	 */
	public boolean hasBranchChoices() {
		return branchDelegate() != null && branchChoices().count() > 0;
	}

	@Override
	public void validationFailedWithException(Throwable theException, Object theValue, String theKeyPath) {
		parent().validationFailedWithException(theException, theValue, theKeyPath);
		log.info("" + theException + theValue + theKeyPath);
	}

}