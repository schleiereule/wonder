package er.extensions.concurrency;

import java.util.UUID;
import java.util.concurrent.Callable;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSNotification;

import er.extensions.appserver.ERXLabelledNotification;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.IERXStatus;

public abstract class ERXAbstractTask extends ERXTask<ERXTaskResult> implements Callable<ERXTaskResult>, IERXStatusTemplate, IERXPercentComplete, IERXThreadDetails {

	public static final String StatusListener = "statusListener";

	private String _notificationLabel;

	// key for the localizable strings
	protected String _statusTemplateKey;

	// key value pairs for the template
	protected NSDictionary<String, String> _statusTemplateFields;

	// Value between 0.0 and 1.0 indicating the task's percentage complete
	private double _percentComplete;

	// A dictionary containing details from subtasks
	private NSMutableDictionary<String, NSMutableDictionary<String, Object>> _threadDetails = new NSMutableDictionary<String, NSMutableDictionary<String, Object>>();

	public String notificationLabel() {
		if (ERXStringUtilities.isBlank(_notificationLabel))
			_notificationLabel = "NotificationLabel-" + UUID.randomUUID().toString();
		return _notificationLabel;
	}

	public void setNotificationLabel(String value) {
		if (ERXStringUtilities.isBlank(value))
			_notificationLabel = "NotificationLabel-" + UUID.randomUUID().toString();
		else
			_notificationLabel = value;
	}

	@Override
	public String statusTemplateKey() {
		return _statusTemplateKey;
	}

	@Override
	public NSDictionary<String, String> statusTemplateFields() {
		return _statusTemplateFields;
	}

	@Override
	public Double percentComplete() {
		return _percentComplete;
	}

	@Override
	public NSArray<NSDictionary<String, Object>> threadDetails() {
		NSMutableArray<NSDictionary<String, Object>> result = new NSMutableArray<NSDictionary<String, Object>>();
		for (String key : _threadDetails.allKeys()) {
			result.add(_threadDetails.get(key).immutableClone());
		}
		return result.immutableClone();
	}

	protected void setProgress(String templateKey) {
		_statusTemplateKey = templateKey;
		_statusTemplateFields = null;
	}

	protected void setProgress(String templateKey, Double percentComplete) {
		_statusTemplateKey = templateKey;
		_statusTemplateFields = null;
		_percentComplete = percentComplete;
	}

	protected void setProgress(String templateKey, NSDictionary<String, String> statusTemplateFields, Double percentComplete) {
		_statusTemplateKey = templateKey;
		_statusTemplateFields = statusTemplateFields;
		_percentComplete = percentComplete;
	}

	protected void setDetail(String key, String status, Double percentComplete) {
		if (_threadDetails.containsKey(key)) {
			NSMutableDictionary<String, Object> detail = _threadDetails.get(key);
			detail.put("status", status);
			detail.put("percentComplete", percentComplete);
		} else {
			_threadDetails.put(key, new NSMutableDictionary<String, Object>(new Object[] { status, percentComplete }, new String[] { "status", "percentComplete" }));
		}
	}

	protected void setDetail(String key, String statusTemplate, NSDictionary<String, String> statusTemplateFields, Double percentComplete) {
		if (_threadDetails.containsKey(key)) {
			NSMutableDictionary<String, Object> detail = _threadDetails.get(key);
			detail.put("statusTemplate", statusTemplate);
			detail.put("statusTemplateFields", statusTemplateFields);
			detail.put("percentComplete", percentComplete);
		} else {
			_threadDetails.put(key, new NSMutableDictionary<String, Object>(new Object[] { statusTemplate, statusTemplateFields, percentComplete }, new String[] { "statusTemplate", "statusTemplateFields", "percentComplete" }));
		}
	}

	protected void resetDetails() {
		_threadDetails = new NSMutableDictionary<String, NSMutableDictionary<String, Object>>();
	}

	public void statusListener(final NSNotification notification) {
		if (notification instanceof ERXLabelledNotification) {
			ERXLabelledNotification l = (ERXLabelledNotification) notification;
			if (l.label().equals(_notificationLabel)) {
				String status = ((IERXStatus) notification.object()).status();
				Double percentComplete = ((IERXPercentComplete) notification.object()).percentComplete();
				setProgress(status, percentComplete);
			}
		}
	}

	protected void statusListener(String key, NSNotification notification) {
		if (notification instanceof ERXLabelledNotification) {
			ERXLabelledNotification l = (ERXLabelledNotification) notification;
			if (l.label().equals(_notificationLabel)) {
				if (notification.object() instanceof IERXStatus) {
					setDetail(key, ((IERXStatus) notification.object()).status(), ((IERXPercentComplete) notification.object()).percentComplete());
				}
				if (notification.object() instanceof IERXStatusTemplate) {
					IERXStatusTemplate o = (IERXStatusTemplate) notification.object();
					setDetail(key, o.statusTemplateKey(), o.statusTemplateFields(), ((IERXPercentComplete) notification.object()).percentComplete());
				}
			}
		}
	}

	protected String localizationKey(String value) {
		return this.getClass().getName() + "." + value;
	}

}
