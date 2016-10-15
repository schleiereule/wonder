package er.modern.directtoweb.assignments.defaults;

import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.D2WModel;
import com.webobjects.eocontrol.EOKeyValueArchiver;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCodingAdditions;

import er.directtoweb.assignments.ERDAssignment;
import er.directtoweb.assignments.ERDLocalizableAssignmentInterface;
import er.extensions.localization.ERXLocalizer;

public class ERMDDefaultUIStringAssignment extends ERDAssignment implements ERDLocalizableAssignmentInterface {

	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
	 * Affecting Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
	 * Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public static final String ALT = "Alt";

	private static final NSArray<String> altDependentKeys = new NSArray<String>(new String[] { D2WModel.PropertyKeyKey, D2WModel.EntityKey, "branch" });

	private static final NSArray<NSArray<String>> allKeys = new NSArray<NSArray<String>>(altDependentKeys);

	private static final NSDictionary<String, NSArray<String>> dependentKeyDict = new NSDictionary<String, NSArray<String>>(allKeys, new NSArray<String>(new String[] { "alternativeText" }));

	public ERMDDefaultUIStringAssignment(EOKeyValueUnarchiver u) {
		super(u);
	}

	public ERMDDefaultUIStringAssignment(String key, Object value) {
		super(key, value);
	}

	public void encodeWithKeyValueArchiver(EOKeyValueArchiver archiver) {
		super.encodeWithKeyValueArchiver(archiver);
	}

	public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver unarchiver) {
		return new ERMDDefaultUIStringAssignment(unarchiver);
	}

	public NSArray<String> dependentKeys(String keyPath) {
		return dependentKeyDict.get(keyPath);
	}

	public String alternativeText(D2WContext c) {
		String result = null;
		String branchName = (String) c.valueForKeyPath("branch.branchName");
		if (branchName != null) {
			StringBuilder sb = new StringBuilder(50);
			String key = sb.append("Button.").append(branchName).append(NSKeyValueCodingAdditions._KeyPathSeparatorChar).append(ALT).toString();
			result = localizedStringForKey(key, c);
		} else if (c.propertyKey() != null && c.entity() != null) {
			StringBuilder sb = new StringBuilder(100);
			String key = sb.append(c.entity().name()).append(NSKeyValueCodingAdditions._KeyPathSeparatorChar).append(c.task()).append(NSKeyValueCodingAdditions._KeyPathSeparatorChar).append(c.propertyKey())
					.append(NSKeyValueCodingAdditions._KeyPathSeparatorChar).append(ALT).toString();
			result = localizedStringForKey(key, c);
		}
		return result;
	}

	public String localizedStringForKey(String key, D2WContext c) {
		String template = ERXLocalizer.currentLocalizer().localizedStringForKey(key);
		if (template == null) {
			template = "";
			ERXLocalizer.currentLocalizer().takeValueForKey(template, key);
		}
		if (template.length() == 0) {
			return null;
		}
		return ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject(template, c);
	}
}
