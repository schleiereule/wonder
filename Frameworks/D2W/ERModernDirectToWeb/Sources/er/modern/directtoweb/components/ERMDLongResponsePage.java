package er.modern.directtoweb.components;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableDictionary;

import er.directtoweb.pages.templates.ERD2WInspectPageTemplate;
import er.extensions.appserver.ERXApplication;
import er.extensions.appserver.ERXNextPageForResultWOAction;
import er.extensions.appserver.ERXSession;
import er.extensions.appserver.IERXPerformWOActionForResult;
import er.extensions.concurrency.ERXExecutorService;
import er.extensions.concurrency.ERXFutureTask;
import er.extensions.concurrency.IERXStatusTemplate;
import er.extensions.concurrency.IERXStoppable;
import er.extensions.concurrency.IERXThreadDetails;
import er.extensions.concurrency.ERXTaskResult;
import er.extensions.foundation.ERXAssert;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXRuntimeUtilities;
import er.extensions.foundation.ERXStopWatch;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.foundation.IERXStatus;
import er.extensions.localization.ERXLocalizer;
import er.modern.directtoweb.interfaces.ERMBrowserPageInterface;

public class ERMDLongResponsePage extends ERD2WInspectPageTemplate {

	private static final long serialVersionUID = 1L;

	public ERMDLongResponsePage(WOContext context) {
		super(context);
		// Grab the referring page when this long response page is created
		_referringPage = context.page();
	}

	// flag to indicate that the user stopped the task (if it was stoppable and
	// the stop control was visible)
	private boolean _wasStoppedByUser = false;

	// Constant to stop all refresh activity on long response page so that it
	// stays open indefinitely allowing the developer
	// to develop a custom CSS stylesheet
	private static final boolean CSS_STYLE_SHEET_DEVELOPMENT_MODE = ERXApplication.isDevelopmentModeSafe() && ERXProperties.booleanForKeyWithDefault("er.coolcomponents.CCAjaxLongResponsePage.stayOnLongResponsePageIndefinitely", false);

	// Constants to determine the CSS stylesheet used for the long response page
	// for this app
	private static final String STYLESHEET_FRAMEWORK = "DivasDirectToWeb";
	private static final String STYLESHEET_FILENAME = "css/LongResponsePage.css";

	// The page that instantiated this long response page
	private final WOComponent _referringPage;

	public static final String LONG_RESPONSE_PAGE_NEXT_BUTTON_ACTION = "LongResponsePageNextButtonAction";

	private IERXPerformWOActionForResult _nextPageForResultController;

	/**
	 * @return the page controller that will be given the result of the long
	 *         task and return the next page except for the case where the user
	 *         stops the task.
	 * 
	 */
	public IERXPerformWOActionForResult nextPageForResultController() {
		if (_nextPageForResultController == null) {
			if (_referringPage instanceof D2WPage) {
				_nextPageForResultController = (IERXPerformWOActionForResult) ((D2WPage) _referringPage).d2wContext().valueForKey("longRunningTaskNextPageForResultController");
			}
			if (_nextPageForResultController == null)
				_nextPageForResultController = new ERXNextPageForResultWOAction(_referringPage);
		} // ~ if (_nextPageForResultController == null)
		return _nextPageForResultController;
	}

	/**
	 * @param nextPageForResultController
	 *            the page controller that will be given the result of the long
	 *            task and return the next page except for the case where the
	 *            user stops the task.
	 * 
	 **/
	public void setNextPageForResultController(IERXPerformWOActionForResult nextPageForResultController) {
		_nextPageForResultController = nextPageForResultController;
	}

	private Boolean _autoFireNextPageAction = null;
	
	public Boolean autoFireNextPageAction() {
		if (_autoFireNextPageAction == null) {
			if (_referringPage instanceof D2WPage) {
				_autoFireNextPageAction = ERXValueUtilities.BooleanValueWithDefault(((D2WPage) _referringPage).d2wContext().valueForKey("longRunningTaskAutoFireNextPageAction"), false);
			} else {
			    _autoFireNextPageAction = true;
			}
		}
		return _autoFireNextPageAction;
	}

	public void setAutoFireNextPageAction(Boolean value) {
		_autoFireNextPageAction = value;
	}

	private Integer _refreshInterval;

	/**
	 * @return the refresh interval in seconds. Defaults to value of
	 *         er.coolcomponents.CCAjaxLongResponsePage.refreshInterval
	 */
	public Integer refreshInterval() {
		if (_refreshInterval == null) {
			_refreshInterval = Integer.valueOf(ERXProperties.intForKeyWithDefault("er.coolcomponents.CCAjaxLongResponsePage.refreshInterval", 2));
		}
		return _refreshInterval;
	}

	/**
	 * @param refreshInterval
	 *            the refresh interval in seconds. Defaults to value of
	 *            er.coolcomponents.CCAjaxLongResponsePage.refreshInterval or 2
	 *            seconds.
	 */
	public void setRefreshInterval(Integer refreshInterval) {
		_refreshInterval = refreshInterval;
	}

	/**
	 * @return flag to prevent update container refresh and thus keep the long
	 *         response page displayed indefinitely for the purpose of
	 *         developing a CS stylesheet.
	 */
	public boolean stayOnLongResponsePageIndefinitely() {
		return CSS_STYLE_SHEET_DEVELOPMENT_MODE;
	}

	private Object _longRunningTask;

	/** @return the Runnable and/or Callable task */
	public Object longRunningTask() {
		if (_longRunningTask == null)
			if (_referringPage instanceof D2WPage) {
				_longRunningTask = ((D2WPage) _referringPage).d2wContext().valueForKey("longRunningTask");
			}
		return _longRunningTask;
	}

	/**
	 * @param task
	 *            the Runnable and/or Callable task
	 */
	public void setLongRunningTask(Object task) {
		if (task instanceof Runnable || task instanceof Callable) {
			_longRunningTask = task;
		} else {
			throw new IllegalArgumentException("The task must implement the Runnable or the Callable interface!");
		}
	}

	private ERXFutureTask<?> _future;

	/**
	 * @return the {@link Future} that is bound to the long running task.
	 * 
	 *         The first time this method is accessed, it is lazily initialized
	 *         and it starts the long running task.
	 * 
	 */
	@SuppressWarnings("unchecked") // Unchecked cast
	public ERXFutureTask<?> future() {
		if (_future == null) {

			Object task = longRunningTask();
			if (task instanceof Callable) {
				_future = new ERXFutureTask<Object>((Callable<Object>) task);
			} else {
				// Runnable interface only
				_future = new ERXFutureTask<Object>((Runnable) task, null);
			}

			// This is where we hand off the task to our executor service to run
			// it in a background thread
			ERXExecutorService.executorService().execute(_future);
		}
		return _future;
	}

	private Boolean _hasStatus;
	private Boolean _hasStatusTemplate;

	public boolean hasStatus() {
		if (_hasStatus == null) {
			_hasStatus = Boolean.valueOf(future().task() instanceof IERXStatus);
		}
		if (_hasStatusTemplate == null) {
			_hasStatusTemplate = Boolean.valueOf(future().task() instanceof IERXStatusTemplate);
		}
		return (_hasStatus | _hasStatusTemplate);

	}

	public String status() {
		if (_hasStatus) {
			if (ERXStringUtilities.isNotBlank(future().status())) {
				return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(future().status());
			}
		}
		if (_hasStatusTemplate) {
			IERXStatusTemplate future = (IERXStatusTemplate) future().task();
			if (ERXStringUtilities.isNotBlank(future.statusTemplateKey())) {
				if (future.statusTemplateFields() != null)
					return ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObjectOtherObject(future.statusTemplateKey(), future.statusTemplateFields(), null);
				else
					return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault(future.statusTemplateKey());
			}
		}
		return defaultStatus();
	}

	private String _defaultStatus;

	/**
	 * @return a status message that is displayed if the task does not provide a
	 *         status message
	 */
	public String defaultStatus() {
		if (_defaultStatus == null) {
			_defaultStatus = ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault("LongResponsePage.defaultStatus");
		}
		return _defaultStatus;
	}

	public boolean hasDetails() {
		return Boolean.valueOf(future().task() instanceof IERXThreadDetails);
	}

	public NSArray<NSDictionary<String, Object>> details() {
		return ((IERXThreadDetails) future().task()).threadDetails();
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

	public String detailFinishedPercentage() {
		return formatPercentage((Double) detail().valueForKey("percentComplete"));
	}

	/**
	 * @param defaultStatus
	 *            a status message that is displayed if the task does not
	 *            provide a status message
	 */
	public void setDefaultStatus(String defaultStatus) {
		_defaultStatus = defaultStatus;
	}

	/**
	 * @return the table cell width value for the finished part of the progress
	 *         bar, for example "56%". The same string can be used to display
	 *         user-friendly percentage complete value.
	 */
	public String finishedPercentage() {
		String result = formatPercentage(future().percentComplete());
		if (future().isDone())
			result = "100%";
		return result;
	}

	private String formatPercentage(Double percentComplete) {
		String result = "1%";
		if (percentComplete != null) {
			long userPercentComplete = Math.round(percentComplete.doubleValue() * 100.0d);
			if (userPercentComplete < 1) {
				userPercentComplete = 1;
			}
			if (userPercentComplete > 100) {
				userPercentComplete = 100;
			}
			result = userPercentComplete + "%";
		}
		return result;
	}

	public ERXTaskResult taskResult() {
		if ((future().isDone()) && (result() != null) && (result() instanceof ERXTaskResult))
			return (ERXTaskResult) result();
		else
			return null;
	}

	/**
	 * @return boolean to hide the unfinished table cell to avoid a tiny slice
	 *         of unfinished when we are at 100%
	 */
	public boolean hideUnfinishedProgressTableCell() {
		return future().isDone() && !wasStoppedByUser();
	}

	/**
	 * @return true if the user stopped the task while it was in progress.
	 */
	public boolean wasStoppedByUser() {
		return _wasStoppedByUser;
	}

	private boolean isStopWatchRunning = false;

	/**
	 * @return the elapsedTime since the task started running
	 */
	public String elapsedTime() {
		if (future().isDone() && isStopWatchRunning) {
			stopWatch().stop();
			isStopWatchRunning = false;
		} // ~ if (isDone())
		return stopWatch().toString();
	}

	private ERXStopWatch _stopWatch;

	/**
	 * @return a stopwatch timer, lazy initialized and started on first call of
	 *         this method
	 **/
	public ERXStopWatch stopWatch() {
		if (_stopWatch == null) {
			_stopWatch = new ERXStopWatch();
			_stopWatch.start();
			isStopWatchRunning = true;

		}
		return _stopWatch;
	}

	public WOActionResults nextPageAction() {
		ERMBrowserPageInterface cbpi = ERD2WUtilities.enclosingComponentOfClass(this, ERMBrowserPageInterface.class);
		return (WOActionResults) cbpi;
	}

	public String altForNextButton() {
		return ERXLocalizer.currentLocalizer().localizedStringForKeyWithDefault("LongResponsePage.altForNextButton");
	}

	@Override
	public WOComponent nextPage() {

		// Get the result of the task
		Object taskResult = result();

		if (log.isDebugEnabled())
			log.debug("nextPage action fired. task result is " + taskResult);

		// The response to be returned to the user after the task is done.
		WOActionResults nextPageResponse = null;

		// Invoke the expected result controller
		if (log.isDebugEnabled())
			log.debug("The task completed normally. Now setting the result, " + taskResult + ", and calling " + nextPageForResultController());
		nextPageForResultController().setResult(taskResult);
		nextPageResponse = nextPageForResultController().performAction();

		if (log.isDebugEnabled())
			log.debug("results = " + (nextPageResponse == null ? "null" : nextPageResponse.toString()));

		_result = null;
		_future = null;
		_nextPageForResultController = null;
		_longRunningTask = null;

		postNextPageButtonActionNotification();

		return (WOComponent) nextPageResponse;

	}

	private Object _result;

	/**
	 * @return the result of the task
	 * 
	 **/
	public Object result() {
		ERXAssert.POST.isTrue(future().isDone());
		if (_result == null) {
			try {
				_result = future().get();
			} catch (CancellationException cancellationException) {
				_result = cancellationException;
			} catch (InterruptedException interruptedException) {
				_result = interruptedException;
			} catch (ExecutionException executionException) {
				log.error("Long Response Error:\n" + ERXRuntimeUtilities.informationForException(executionException), executionException);
				_result = executionException;
			}
		}
		return _result;
	}

	/**
	 * @return the javascript snippet that will call the nextPage action when
	 *         the task is done.
	 */
	public String controlScriptContent() {
		String result = ";";
		if (future().isDone() && !CSS_STYLE_SHEET_DEVELOPMENT_MODE) {
			// To avoid confusion and users saying it never reaches 100% (which
			// can happen if we complete and return the result
			// before the last refresh that _would_ display 100% if we waited),
			// we will wait for a period slightly longer than the
			// refresh interval to get one more refresh and let the user
			// visually see 100%.
			// Wait one refresh interval plus 900 milliseconds as long as the
			// refresh interval is not customized to some huge value by the
			// developer
			int delay = Math.min(((refreshInterval().intValue() * 1000) + 900), 2900);
			result = "window.setTimeout(performNextPageAction, " + delay + ");";
		}
		if (log.isDebugEnabled())
			log.debug("controlScriptContent on refresh = " + result);
		return result;
	}

	/**
	 * User action to stop the task if it implements {@link IERXStoppable}. If
	 * the task is not stoppable, this action has no effect.
	 * 
	 * @return null
	 */
	public WOActionResults stopTask() {
		Object task = future().task();
		if (task instanceof IERXStoppable) {
			IERXStoppable stoppable = (IERXStoppable) task;
			stoppable.stop();
			_wasStoppedByUser = true;
		}
		return null;
	}

	/**
	 * @return the framework containing the CSS stylesheet for this page
	 */
	public String styleSheetFramework() {
		return STYLESHEET_FRAMEWORK;
	}

	/**
	 * @return the filename of the CSS stylesheet webserver resource for this
	 *         page
	 */
	public String styleSheetFilename() {
		return STYLESHEET_FILENAME;
	}

	private void postNextPageButtonActionNotification() {
		NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
		ERXSession.session().notificationCenter().postNotification(LONG_RESPONSE_PAGE_NEXT_BUTTON_ACTION, this, userInfo);
	}

}