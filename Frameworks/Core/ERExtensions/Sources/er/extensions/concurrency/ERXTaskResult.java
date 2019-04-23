package er.extensions.concurrency;

import com.webobjects.foundation.NSArray;

public class ERXTaskResult {

	private ERXTaskOutcome _finalOutcome;
	private NSArray<?> _resultSet;

	public ERXTaskOutcome finalOutcome() {
		return _finalOutcome;
	}

	public void setFinalOutcome(ERXTaskOutcome _finalOutcome) {
		this._finalOutcome = _finalOutcome;
	}

	public NSArray<?> resultSet() {
		return _resultSet;
	}

	public void setResultSet(NSArray<?> _resultSet) {
		this._resultSet = _resultSet;
	}

	private ERXTaskResult() {
	}

	public static ERXTaskResult getInstance() {
		return new ERXTaskResult();
	}

}
