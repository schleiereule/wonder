package er.modern.directtoweb.interfaces;

import com.webobjects.directtoweb.D2WContext;

import er.extensions.eof.ERXGenericRecord;

public interface ERMBrowserPageInterface {
	
	public D2WContext d2wContext();
	
	public ERXGenericRecord selectedObject();
	
	public void setSelectedObject(ERXGenericRecord selectedObject);

}
