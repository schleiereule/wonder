package er.extensions.concurrency;

import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSMutableArray;

import er.extensions.eof.ERXEC;
import er.extensions.validation.ERXValidationException;

public class ERXDeleteEnterpriseObjectsTask extends ERXAbstractTask {

	private static final Logger log = Logger.getLogger(ERXDeleteEnterpriseObjectsTask.class);

	private final NSArray<EOGlobalID> _eoGlobalIDs;

	private ERXDeleteEnterpriseObjectsTask() {
		_eoGlobalIDs = null;
	}

	private ERXDeleteEnterpriseObjectsTask(NSArray<EOEnterpriseObject> eos) {
		NSMutableArray<EOGlobalID> eoGlobalIDs = new NSMutableArray<EOGlobalID>();
		for (EOEnterpriseObject anEO : eos) {
			EOGlobalID globalID = anEO.editingContext().globalIDForObject(anEO);
			if (globalID != null) {
				eoGlobalIDs.addObject(globalID);
			}
		}
		_eoGlobalIDs = eoGlobalIDs.immutableClone();
	}

	public static ERXDeleteEnterpriseObjectsTask getInstance(NSArray<EOEnterpriseObject> eos) {
		return new ERXDeleteEnterpriseObjectsTask(eos);
	}

	@Override
	public ERXTaskResult _call() {

		setProgress(localizationKey("start"), 0.0d);

		ERXTaskResult taskResult = ERXTaskResult.getInstance();
		
		NSMutableArray<ERXValidationException> validationExceptions = new NSMutableArray<ERXValidationException>();

		// get a database connection from the store and a new context
		EOObjectStoreCoordinator tosc = ERXTaskObjectStoreCoordinatorPool.objectStoreCoordinator();
		EOEditingContext ec = ERXEC.newEditingContext(tosc);
		ec.lock();

		double objectCount = _eoGlobalIDs.count();
		double counter = 0;
		double percentComplete = counter / objectCount;
		setProgress(localizationKey("fetchEnterpriseObject"), percentComplete);

		for (EOGlobalID aGlobalID : _eoGlobalIDs) {
			// fetch the enterprise object
			EOEnterpriseObject eo = ec.faultForGlobalID(aGlobalID, ec);

			try {

				ec.deleteObject(eo);
				ec.saveChanges();

				counter++;
		        percentComplete = counter / objectCount;
				setProgress(localizationKey("completed"), percentComplete);

			}
			catch (ERXValidationException ve) {
				// deletion of an EO failed, collect exception, reset the EO and move on
				validationExceptions.add(ve);
				ec.undo();
				counter++;
		        percentComplete = counter / objectCount;
				setProgress(localizationKey("completed"), percentComplete);
			}
			catch (Exception e) {
				log.error(e);
				setProgress(e.getLocalizedMessage(), 1.0d);
				taskResult.setFinalOutcome(ERXTaskOutcome.FAILED);

				// clean out the EC as we're returning now
				ec.unlock();
				ec.dispose();
				return taskResult;
			}
		}
		ec.unlock();
    	ec.dispose();

		if (validationExceptions.isEmpty()) {
			taskResult.setFinalOutcome(ERXTaskOutcome.SUCCESSFUL);
		}
		else {
			taskResult.setResultSet(validationExceptions.immutableClone());
			taskResult.setFinalOutcome(ERXTaskOutcome.SUCCESSFULWITHWARNING);
		}
		return taskResult;

	}

}
