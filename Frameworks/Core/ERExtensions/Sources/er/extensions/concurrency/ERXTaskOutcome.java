package er.extensions.concurrency;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.eof.ERXConstant;
import er.extensions.eof.ERXStringType;

public class ERXTaskOutcome extends ERXStringType {

	private int _sortOrder;

	public ERXTaskOutcome(String value, String name, int sortOrder) {
		super(value, name);
		_sortOrder = sortOrder;
	}

	public int sortOrder() {
		return _sortOrder;
	}

	public String textDescription() {
		return ERXTaskOutcome.class.getSimpleName() + "." + name();
	}

	public static ERXTaskOutcome outcome(String key) {
		return (ERXTaskOutcome) constantForClassNamed(key, ERXTaskOutcome.class.getName());
	}

	public static ERXTaskOutcome SUCCESSFUL = new ERXTaskOutcome("successful", "Successful", 1);
	public static ERXTaskOutcome SUCCESSFULWITHERROR = new ERXTaskOutcome("successfulWithError", "SuccessfulWithError", 2);
	public static ERXTaskOutcome SUCCESSFULWITHWARNING = new ERXTaskOutcome("successfulWithWarning", "SuccessfulWithWarning", 3);
	public static ERXTaskOutcome FAILED = new ERXTaskOutcome("failed", "Failed", 4);

	public static final TaskOutcomeClazz<ERXTaskOutcome> clazz = new TaskOutcomeClazz<ERXTaskOutcome>();

	public static class TaskOutcomeClazz<T extends ERXTaskOutcome> {

		public NSArray<ERXTaskOutcome> allObjects(EOEditingContext ec) {
			return new NSArray<ERXTaskOutcome>((ERXTaskOutcome[]) new Object[] { SUCCESSFUL, SUCCESSFULWITHERROR, SUCCESSFULWITHWARNING, FAILED });
		}

		public ERXTaskOutcome sharedStateForKey(String key) {
			return (ERXTaskOutcome) ERXConstant.constantForClassNamed(key, ERXTaskOutcome.class.getName());
		}

		public void initializeSharedData() {
			ERXTaskOutcome.SUCCESSFUL = sharedStateForKey("successful");
			ERXTaskOutcome.SUCCESSFULWITHERROR = sharedStateForKey("successfulWithError");
			ERXTaskOutcome.SUCCESSFULWITHWARNING = sharedStateForKey("successfulWithWarning");
			ERXTaskOutcome.FAILED = sharedStateForKey("failed");
		}

	}

}
