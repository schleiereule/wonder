package er.modern.directtoweb.delegates;

import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.NextPageDelegate;

import er.extensions.appserver.IERXPerformWOActionForResult;
import er.modern.directtoweb.components.ERMDLongResponsePage;

public class ERMDLongResponsePageDelegate implements NextPageDelegate {

	public final static Logger log = Logger.getLogger(ERMDLongResponsePageDelegate.class);
	
	public static final MethodClazz clazz = new MethodClazz();
	
	public static class MethodClazz {

		public WOComponent getLongResponsePage(WOComponent sender, Object task, IERXPerformWOActionForResult nextPageForResultController) {
			ERMDLongResponsePage page = (ERMDLongResponsePage) D2W.factory().pageForConfigurationNamed("LongResponsePage", sender.session());
			page.setLongRunningTask(task);
			page.setNextPageForResultController(nextPageForResultController);
			return page;
		}

	}
	
	private Object _task;

	public Object task() {
		return _task;
	}

	public void setTask(Object task) {
		if (task instanceof Runnable || task instanceof Callable) {
			_task = task;
		} else {
			throw new IllegalArgumentException("The task must implement the Runnable or the Callable interface!");
		}
	}
	
	private IERXPerformWOActionForResult _nextPageForResultController;

	public IERXPerformWOActionForResult nextPageForResultController() {
		return _nextPageForResultController;
	}

	public void setNextPageForResultController(IERXPerformWOActionForResult nextPageForResultController) {
		_nextPageForResultController = nextPageForResultController;
	}

	@Override
	public WOComponent nextPage(WOComponent sender) {
		return ERMDLongResponsePageDelegate.clazz.getLongResponsePage(sender, _task, _nextPageForResultController);
	}
	
	
	
}
