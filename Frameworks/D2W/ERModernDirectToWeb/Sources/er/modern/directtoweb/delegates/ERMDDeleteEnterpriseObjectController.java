package er.modern.directtoweb.delegates;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.foundation.NSArray;

import er.directtoweb.pages.ERD2WPickListPage;
import er.extensions.appserver.ERXNextPageForResultWOAction;
import er.extensions.concurrency.ERXTaskResult;
import er.extensions.validation.ERXValidationException;
import er.modern.directtoweb.interfaces.ERMEditRelationshipPageInterface;

public class ERMDDeleteEnterpriseObjectController extends ERXNextPageForResultWOAction {

	public ERMDDeleteEnterpriseObjectController() {
		super();
	}

	private WOComponent _objectPage;
	private WOComponent _senderPage;

	public WOComponent objectPage() {
		return _objectPage;
	}

	public void setObjectPage(WOComponent value) {
		this._objectPage = value;
	}

	public WOComponent senderPage() {
		return _senderPage;
	}

	public void setSenderPage(WOComponent value) {
		this._senderPage = value;
	}

	@Override
	public WOActionResults performAction() {
        if (_objectPage != null) {
            if (_objectPage instanceof ERMEditRelationshipPageInterface) {
                ERMEditRelationshipPageInterface p = (ERMEditRelationshipPageInterface) _objectPage;
                WODisplayGroup displayGroup = p.relationshipDisplayGroup();
                updateDisplayGroup(displayGroup);
            } else if (_objectPage instanceof ERD2WPickListPage) {
                ERD2WPickListPage pickPage = (ERD2WPickListPage) _objectPage;
                WODisplayGroup displayGroup = pickPage.displayGroup();
                updateDisplayGroup(displayGroup);
                if (_result instanceof ERXTaskResult) {
                    @SuppressWarnings("unchecked")
                    NSArray<ERXValidationException> validationExceptions = (NSArray<ERXValidationException>) ((ERXTaskResult) _result).resultSet();
                    for (ERXValidationException e : validationExceptions) {
                        pickPage.validationFailedWithException(e, e.object(), "saveChangesExceptionKey");
                    }
                }
            }
        }
		return _senderPage;

	}

    private void updateDisplayGroup(WODisplayGroup displayGroup) {
        displayGroup.fetch();
        // when the last object of the last batch gets removed, select the new last batch
        if (displayGroup.currentBatchIndex() > displayGroup.batchCount()) {
        	displayGroup.setCurrentBatchIndex(displayGroup.batchCount());
        }
    }

}
