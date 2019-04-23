package er.extensions.eof;

import java.io.Serializable;

import er.extensions.eof.ERXConstant.StringConstant;

public abstract class ERXStringType extends ERXConstant.StringConstant implements Serializable {

	private static final long serialVersionUID = 3340039759302984455L;

	public ERXStringType(String value, String name) {
		super(value, name);
	}

	public String textDescription() {
		return name();
	}

	@Override
	public String toString() {
		return getClass().getName() + ":" + value();
	}

	public static String typeToString(ERXStringType stringType) {
		return stringType.toString();
	}

	// do not forget to initialize the shared data of the StringType to make this feature work
	public static ERXStringType typeFromString(String typeAsString) {
		String[] parts = typeAsString.split(":");
		return (ERXStringType) StringConstant.constantForClassNamed(parts[1], parts[0]);
	}

}
