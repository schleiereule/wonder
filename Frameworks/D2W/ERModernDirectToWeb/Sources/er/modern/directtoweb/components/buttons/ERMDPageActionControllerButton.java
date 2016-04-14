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
	 * Do I need to update serialVersionUID?
	 * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

    /** logging support */
    private static final Logger log = Logger.getLogger(ERMDPageActionControllerButton.class);
	
    public ERMDPageActionControllerButton(WOContext context) {
        super(context);
    }
    
    //---------------- CSS Support --------------------//
	public String buttonCssClass() {
		String cssClass = "Button PageButton";
		String branchName = branchName();
		if (branchName != null) {
			if (branchName.startsWith("_"))
				branchName = branchName.substring(1);
			cssClass = cssClass + " " + ERXStringUtilities.capitalize(branchName) + "PageButton";
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
	
	 //---------------- ALT Support --------------------//
	public String branchButtonAlt() {
		return branchButtonLabel() + ".Alt";
	}
	
	public ERDBranchDelegateInterface branchDelegate() {
		if (branchDelegate == null) {
			WOComponent current = parent();
			while (current != null) {
				if (current instanceof D2WPage) {
					Object pageActionsDelegateClass = d2wContext().valueForKey("pageActionController");
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
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets
	 * the user selected branch name.
	 * 
	 * @return user selected branch name.
	 */
	public String branchName() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_NAME);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets
	 * the user selected branch prefix.
	 * 
	 * @return user selected branch prefix.
	 */
	public String branchPrefix() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_PREFIX);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets
	 * the user selected branch label.
	 * 
	 * @return user selected branch label.
	 */
	public String branchButtonLabel() {
		return (String) branch().valueForKey(ERDBranchDelegate.BRANCH_LABEL);
	}

	/**
	 * Implementation of the {@link ERDBranchDelegate ERDBranchDelegate}. Gets
	 * the user selected branch button id.
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
	
	public Boolean dontSubmitForm() {
		if (branchRequiresFormSubmit())
			return false;
		else
			return true;
	}

	/**
	 * Calculates the branch choices for the current page. This method is just a
	 * cover for calling the method <code>branchChoicesForContext</code> on the
	 * current {@link ERDBranchDelegate ERDBranchDelegate}.
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