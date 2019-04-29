package er.modern.directtoweb.components;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

import er.extensions.concurrency.ERXFutureTask;
import er.extensions.concurrency.ERXTaskInfo;
import er.extensions.concurrency.ERXTaskThread;
import er.extensions.concurrency.IERXPercentComplete;
import er.extensions.concurrency.IERXStatusTemplate;
import er.extensions.concurrency.IERXStoppable;
import er.extensions.concurrency.IERXThreadDetails;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.IERXStatus;
import er.extensions.localization.ERXLocalizer;

/**
 * This stateless component is regenerated on each refresh with fresh statistics.
 * 
 * @author kieran
 * 
 */
public class ERMDThreadMonitor extends WOComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(ERMDThreadMonitor.class);

	public ERMDThreadMonitor(WOContext context) {
		super(context);
	}

	@Override
	public void awake() {
		super.awake();
		setValueForBinding(taskInfos().count(), "runningThreadsCount");
	}

	@Override
	public boolean synchronizesVariablesWithBindings() {
		// makes this component non-synchronizing
		return false;
	}

	@Override
	public boolean isStateless() {
		// makes this component stateless
		return true;
	}

	@Override
	public void reset() {
		super.reset();
		_taskInfo = null;
		_tasks = null;
	}

	private NSArray<ERXTaskInfo> _tasks;

	/** @return the current task infos */
	public NSArray<ERXTaskInfo> taskInfos() {
		if (_tasks == null) {
			// Grab all tasks that are instances of ERXTaskThread
			_tasks = ERXTaskThread.taskInfos();
		}
		return _tasks;
	}

	private ERXTaskInfo _taskInfo;

	/** @return the loop task info item */
	public ERXTaskInfo taskInfo() {
		return _taskInfo;
	}

	/**
	 * @param taskInfo
	 *            the loop task info item
	 */
	public void setTaskInfo(ERXTaskInfo taskInfo) {
		_taskInfo = taskInfo;
	}

	private Object task() {
		if (taskInfo() != null) {
			Runnable runnable = taskInfo().task();
			if (runnable instanceof ERXFutureTask) {
				return ((ERXFutureTask) runnable).task();
			} else {
				return runnable;
			}
		} else {
			return null;
		}
	}

	public Double percentageComplete() {
		Double result = null;
		if (task() != null && task() instanceof IERXPercentComplete) {
			result = ((IERXPercentComplete) task()).percentComplete();
			if (result != null) {
				result = result * 100.0;
			}

		}
		return result;
	}

	public String status() {
		String result = null;
		if (task() != null) {
			if (task() instanceof IERXStatus) {
				result = ((IERXStatus) task()).status();
			}
			if (task() instanceof IERXStatusTemplate) {
				IERXStatusTemplate future = (IERXStatusTemplate) task();
				if (ERXStringUtilities.isNotBlank(future.statusTemplateKey())) {
					if (future.statusTemplateFields() != null)
						result = ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObjectOtherObject(future.statusTemplateKey(), future.statusTemplateFields(), null);
					else
						result = ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(future.statusTemplateKey());
				}
			}
		}
		if (ERXStringUtilities.isBlank(result))
			result = "ERMDThreadMonitor.defaultStatus";
		return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(result);
	}

	public String description() {
		String result = null;
		if (task() != null) {
			String temp = task().toString();
			String[] tempA = temp.split("@");
			result = tempA[0].substring(tempA[0].lastIndexOf(".") + 1) + "@" + tempA[1];
		}
		return result;
	}

	private NSDictionary<String, Object> _detail;

	/**
	 * @return the detail
	 */
	public NSDictionary<String, Object> detail() {
		return _detail;
	}

	/**
	 * @param detail
	 *            the detail to set
	 */
	public void setDetail(NSDictionary<String, Object> value) {
		_detail = value;
	}

	public boolean hasDetails() {
		boolean hasDetails = false;
		if (task() != null) {
			hasDetails = task() instanceof IERXThreadDetails;
		}
		return hasDetails;
	}

	public NSArray<NSDictionary<String, Object>> details() {
		return ((IERXThreadDetails) task()).threadDetails();
	}

	public Double detailPercentageComplete() {
		Double result = null;
		result = (Double) detail().valueForKey("percentComplete");
		if (result != null) {
			result = result * 100.0;
		}
		return result;
	}

	public String detailStatus() {
		String result = null;
		if ((detail().containsKey("statusTemplate")) && (ERXStringUtilities.isNotBlank((String) detail().valueForKey("statusTemplate")))) {
			if ((detail().containsKey("statusTemplateFields")) && (detail().valueForKey("statusTemplateFields") != null))
				result = ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObjectOtherObject((String) detail().valueForKey("statusTemplate"), detail().valueForKey("statusTemplateFields"), null);
			else
				result = ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault((String) detail().valueForKey("statusTemplate"));
		}
		if (result == null) {
			if ((detail().containsKey("status")) && (ERXStringUtilities.isNotBlank((String) detail().valueForKey("status")))) {
				result = ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault((String) detail().valueForKey("status"));
			}
		}
		if (result == null)
			result = "&nbsp;";
		return result;
	}

	public String rowClass() {
		Integer index = taskInfos().indexOf(taskInfo());
		if (index == null)
			return "";
		if (index % 2 == 0)
			return "ObjRow OddObjRow";
		else
			return "ObjRow EvenObjRow";
	}

	public Boolean showStop() {
		Boolean show = false;
		Object task = task();
		if ((task != null) && (task instanceof IERXStoppable) && (!((IERXStoppable) task).willStop())) {
			show = true;
		}
		return show;
	}

	public Boolean willStop() {
		Boolean show = false;
		Object task = task();
		if ((task != null) && (task instanceof IERXStoppable) && ((IERXStoppable) task).willStop()) {
			show = true;
		}
		return show;
	}

	public WOActionResults stopTask() {
		Object task = task();
		if ((task != null) && (task instanceof IERXStoppable)) {
			((IERXStoppable) task).stop();
		}
		return null;
	}

	public String tasksRunningMessage() {
		int count = taskInfos().count();
		switch (count) {
		case 0:
			return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault("ERMDThreadMonitor.noTasksRunningMessage");
		case 1:
			return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault("ERMDThreadMonitor.taskRunningMessage");
		default:
			return ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("ERMDThreadMonitor.tasksRunningMessage", taskInfos());
		}
	}

}