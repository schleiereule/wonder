package er.modern.look.pages;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WPage;

import er.directtoweb.pages.templates.ERD2WInspectPageTemplate;
import er.extensions.appserver.ERXNextPageForResultWOAction;
import er.extensions.appserver.IERXPerformWOActionForResult;
import er.modern.directtoweb.interfaces.ERMListThreadPageInterface;

public class ERMODListThreadPage extends ERD2WInspectPageTemplate implements ERMListThreadPageInterface {

	private static final long serialVersionUID = 1L;

	// The page that instantiated this list thread page
	private final WOComponent _referringPage;

	private IERXPerformWOActionForResult _nextPageForResultController;

	public ERMODListThreadPage(WOContext context) {
		super(context);
		// Grab the referring page when this list thread page is created
		_referringPage = context.page();
	}

	@Override
	public WOComponent nextPage() {
		return (WOComponent) nextPageForResultController().performAction();
	}

	public IERXPerformWOActionForResult nextPageForResultController() {
		if (_nextPageForResultController == null) {
			if (_referringPage instanceof D2WPage) {
				_nextPageForResultController = (IERXPerformWOActionForResult) ((D2WPage) _referringPage).d2wContext().valueForKey("listThreadPageNextPageForResultController");
			}
			if (_nextPageForResultController == null)
				_nextPageForResultController = new ERXNextPageForResultWOAction(_referringPage);
		} // ~ if (_nextPageForResultController == null)
		return _nextPageForResultController;
	}

	public void setNextPageForResultController(IERXPerformWOActionForResult nextPageForResultController) {
		_nextPageForResultController = nextPageForResultController;
	}

}