package er.modern.directtoweb.components.repetitions;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver._private.WOGenericContainer;
import com.webobjects.directtoweb.ERD2WContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSNotificationCenter;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.ERD2WKeys;
import er.directtoweb.components.repetitions.ERDInspectPageRepetition;
import er.extensions.appserver.ERXSession;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.validation.ERXValidationException;
import er.extensions.validation.ERXValidationFactory;
import er.modern.directtoweb.components.ERMDAjaxNotificationCenter;

/**
 * Modern tableless inspect/edit page repetition
 * 
 * @d2wKey componentName
 * @d2wKey propertyNameComponentName
 * @d2wKey sectionComponentName
 * @d2wKey baseClassForLine
 * @d2wKey pageType
 * @d2wKey displayNameForProperty
 * @d2wKey classForSection
 * @d2wKey classForAttributeValue
 * @d2wKey classForLabelSpan
 * @d2wKey classForEmptyLabelSpan
 * @d2wKey classForAttributeRepetitionWrapper
 * 
 * @author davidleber
 */
public class ERMDInspectPageRepetition extends ERDInspectPageRepetition {
	
    private static final long serialVersionUID = 1L;

    public int index;
	
    public ERXValidationException aValidationException;
    
    public ERMDInspectPageRepetition(WOContext context) {
        super(context);
    }
    
	// LINE
	
    /**
     * CSS class for the current line in the repetition.
     * <p>
     * Examples:
     * <p>
     * "Line OddLine FirstLine InspectLine Attribute1Line"
     * "Line EvenLine InspectLine Attribute2Line"
     * "Line OddLine InspectLine Attribute3Line"
     * "Line EvenLine LastLine InspectLine Attribute4Line"
     * <p>
     * "Line OddLine FirstLine EditLine Attribute1Line ErrorLine"
     * 
     * @return String css class derived from rules and position
     */
	public String lineDivClass() {
		String lineBase = (String)d2wContext().valueForKey(ERD2WKeys.BASE_CLASS_FOR_LINE);
		String evenessAndPosition = "Even" + lineBase;
		int lastIndex = currentSectionKeys().count() - 1;
		if (index %2 == 0) {
			evenessAndPosition = "Odd" + lineBase;
		}
		if (index == 0) {
			evenessAndPosition += " First" + lineBase;
		} else if (index == lastIndex) {
			evenessAndPosition += " Last" + lineBase;
		}
		String error = hasNoErrors() ? "" : " Error" + lineBase;
		return lineBase + " " + evenessAndPosition + " " + d2wContext().valueForKey(ERD2WKeys.PAGE_TYPE) + lineBase + " " + ERXStringUtilities.capitalize(propertyKey()) + lineBase + error;
	}

	
	// ERRORS //
	
    public boolean hasNoErrors() {
        return !validationExceptionOccurredForPropertyKey();
    }
    
    public String displayNameForProperty() {
    	return (String)d2wContext().valueForKey(ERD2WKeys.DISPLAY_NAME_FOR_PROPERTY);
    }
    
    public boolean validationExceptionOccurredForPropertyKey() {
        if (propertyKey() == null) {
            return false;
        } else {
            boolean contains = ((ERD2WContext) d2wContext()).hasValidationExceptionForPropertyKey(propertyKey());
            return contains;
        }
    }
    
    public NSArray<ERXValidationException> validationExceptions() {
        NSArray<ERXValidationException> errorMessages = NSArray.emptyArray();
        if (validationExceptionOccurredForPropertyKey()) {
            ERXValidationException primaryException = ((ERD2WContext) d2wContext()).validationExceptionForPropertyKey(propertyKey());
            if (primaryException != null && primaryException.additionalExceptions().count() > 0) {
                // collect additional exceptions
                NSMutableArray<ERXValidationException> exceptions = new NSMutableArray<>();
                exceptions.addObject(primaryException);
                // TODO this assumes that nested exceptions target the same property key
                exceptions = collectAdditionalExceptions(primaryException, exceptions);
                errorMessages = exceptions.immutableClone();
            } else {
                errorMessages = new NSArray<>(primaryException);
            }
        }
        return errorMessages;
    }

    private NSMutableArray<ERXValidationException> collectAdditionalExceptions(ERXValidationException primaryException,
                                             NSMutableArray<ERXValidationException> exceptions) {
        for (ValidationException anException : primaryException.additionalExceptions()) {
            if (!(anException instanceof ERXValidationException)) {
                anException = ERXValidationFactory.defaultFactory()
                        .convertException(anException);
            }
            exceptions.addObject((ERXValidationException) anException);
            if (anException.additionalExceptions().count() > 0) {
                collectAdditionalExceptions((ERXValidationException) anException,
                        exceptions);
            }
        }
        return exceptions;
    }
    
	@SuppressWarnings({ "unchecked", "rawtypes" })
    public NSArray<String> keyPathsWithValidationExceptions() {
        NSArray exceptions = (NSArray)d2wContext().valueForKey(ERD2WKeys.KEY_PATHS_WITH_VALIDATION_EXCEPTIONS);
        return exceptions != null ? exceptions : NSArray.EmptyArray;
    }
    
    // AJAX notification center support
	
	public boolean isDependent() {
	    return ERXValueUtilities.booleanValueWithDefault(
	            d2wContext().valueForKey(ERD2WKeys.IS_DEPENDENT), false);
	}
	
	public boolean shouldObserve() {
	    return ERXValueUtilities.booleanValueWithDefault(
	            d2wContext().valueForKey(ERD2WKeys.SHOULD_OBSERVE), false);
	}

	public String lineDivId() {
	    String lineDivId = null;
	    // only needed if this is a dependent property
	    if (isDependent()) {
	        String pageConfiguration = (String) d2wContext().valueForKey(
	                ERD2WKeys.PAGE_CONFIGURATION);
	        lineDivId = pageConfiguration
	                + ERXStringUtilities.capitalize(propertyKey()).replaceAll("\\.", "_")
	                + "LineUC";
	    }
	    return lineDivId;
	}

    /**
     * If the current property key is depending on an observed property key, we
     * surround it with an update container.
     * 
     * @return the component name to use as the line div
     */
    public String lineDivComponentName() {
        String lineDivComponentName = WOGenericContainer.class.getSimpleName();
        if (isDependent()) {
            lineDivComponentName = AjaxUpdateContainer.class.getSimpleName();
        }
        return lineDivComponentName;
    }

    /**
     * Posts a change notification when an observed property key has changed.
     */
    public void postChangeNotification() {
        ERXSession.session().notificationCenter().postNotification(
                ERMDAjaxNotificationCenter.PropertyChangedNotification, d2wContext());
    }
}