package er.extensions.eof;

import java.io.Serializable;

import er.extensions.eof.ERXConstant.StringConstant;

public abstract class ERXStringType extends ERXConstant.StringConstant implements Serializable {
	
	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page
	 * 51 of the <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public ERXStringType(String value, String name) {
		super(value, name);
	}
	
	public static String typeToString(ERXStringType stringType) {
		return stringType.getClass().getName() + ":" + stringType.value();
	}

	// do not forget to initialize the shared data of the StringType to make this feature work
	public static ERXStringType typeFromString(String typeAsString) {
		String[] parts = typeAsString.split(":");
		return (ERXStringType) StringConstant.constantForClassNamed(parts[1], parts[0]);
	}

}
