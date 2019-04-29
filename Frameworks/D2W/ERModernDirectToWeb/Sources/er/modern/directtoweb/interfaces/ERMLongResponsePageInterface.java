package er.modern.directtoweb.interfaces;

import er.extensions.appserver.IERXPerformWOActionForResult;

public interface ERMLongResponsePageInterface {
	
	public void setLongRunningTask(Object task);
	
	public void setNextPageForResultController(IERXPerformWOActionForResult nextPageForResultController);
	
	public void setAutoFireNextPageAction(Boolean value);

}
