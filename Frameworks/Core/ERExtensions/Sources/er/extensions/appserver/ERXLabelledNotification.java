package er.extensions.appserver;

import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSNotification;

public class ERXLabelledNotification extends NSNotification {

	public ERXLabelledNotification(String name, Object object, String label) {
		super(name, object);
		_label = label;
	}

	public ERXLabelledNotification(String name, Object object, NSDictionary userInfo, String label) {
		super(name, object, userInfo);
		_label = label;
	}

	private String _label;

	public String label() {
		return _label;
	}

	public void setLabel(String value) {
		_label = value;
	}

}
