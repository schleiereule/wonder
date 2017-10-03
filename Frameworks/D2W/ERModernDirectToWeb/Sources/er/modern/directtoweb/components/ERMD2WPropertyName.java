package er.modern.directtoweb.components;

import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;

import er.directtoweb.components.ERD2WPropertyName;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * Modern property name component.
 * 
 * @binding localContext
 * @d2wKey displayRequiredMarkerCell
 * @d2wKey escapeHTML
 * @d2wKey displayNameForProperty
 * @d2wKey componentName
 * @d2wKey customComponentName
 * @d2wKey hidePropertyName
 * @d2wKey displayRequiredMarker
 * @d2wKey keyPathsWithValidationExceptions
 * @d2wKey idForPropertyContainer
 * 
 * 
 * @author davidleber
 *
 */
public class ERMD2WPropertyName extends ERD2WPropertyName {
	
	private static final long serialVersionUID = 1L;

	public ERMD2WPropertyName(WOContext context) {
        super(context);
    }

	public String fieldLabelElement() {
		return  isEditing() ? "label" : "span";
	}
	
	public String fieldLabelClass() {
	    String key = propertyKey();
        if (key != null) {
            String temp = "";
            // remove . chars from key path and capitalize parts
            if (key.indexOf(".") != -1) {
                NSArray<String> components = NSArray.componentsSeparatedByString(key, ".");
                for (String string : components) {
                    string = ERXStringUtilities.capitalize(string);
                    temp = temp + string;
                }
            } else {
                temp = ERXStringUtilities.capitalize(key);
            }
            key = temp;
            // remove @ char introduced by partial key path
            if (key.startsWith("@")) {
                key = key.substring(1);
            }
        }
		String result = hasNoErrors() ? "" : " ErrorLabel";
		if (isEditing()) {
			result = key + "Label" + result;
		} else {
			result = "Label " + key + "Label" + result;
		}
		return result;
	}
   
    public String fieldLabelForValue() {
        return isEditing() ? (String) d2wContext().valueForKey("idForPropertyContainer")
                : null;
    }

    public String localizedTitle() {
        return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(
                "Required field");
    }

	@Override
	public boolean isEditing() {
		String task = d2wContext().task();
		String subTask = (String) d2wContext().valueForKey("subTask");
		return ("edit".equals(task) && !"list".equals(subTask)) || "query".equals(task);
	}
    
}