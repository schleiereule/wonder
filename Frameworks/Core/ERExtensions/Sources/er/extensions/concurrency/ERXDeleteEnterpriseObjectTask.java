package er.extensions.concurrency;

import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOGlobalID;
import com.webobjects.eocontrol.EOObjectStoreCoordinator;

import er.extensions.eof.ERXEC;

public class ERXDeleteEnterpriseObjectTask extends ERXAbstractTask {

	private static final Logger log = Logger.getLogger(ERXDeleteEnterpriseObjectTask.class);

	private final EOGlobalID _hardDiskGID;

	private ERXDeleteEnterpriseObjectTask() {
		_hardDiskGID = null;
	}

	private ERXDeleteEnterpriseObjectTask(EOEnterpriseObject eo) {
		_hardDiskGID = eo.editingContext().globalIDForObject(eo);
	}

	public static ERXDeleteEnterpriseObjectTask getInstance(EOEnterpriseObject eo) {
		return new ERXDeleteEnterpriseObjectTask(eo);
	}

	@Override
	public ERXTaskResult _call() {

		setProgress(localizationKey("start"), 0.0d);

		ERXTaskResult taskResult = ERXTaskResult.getInstance();

		// get a database connection from the store and a new context
		EOObjectStoreCoordinator tosc = ERXTaskObjectStoreCoordinatorPool.objectStoreCoordinator();
		EOEditingContext ec = ERXEC.newEditingContext(tosc);
		ec.lock();

		// fetch the enterprise object
		setProgress(localizationKey("fetchEnterpriseObject"), 0.2d);
		EOEnterpriseObject enterpriseObject = ec.faultForGlobalID(_hardDiskGID, ec);

		try {

			setProgress(localizationKey("deleteEnterpriseObject"), 0.5d);
			ec.deleteObject(enterpriseObject);
			ec.saveChanges();

			setProgress(localizationKey("completed"), 1.0d);

			taskResult.setFinalOutcome(ERXTaskOutcome.SUCCESSFUL);
			return taskResult;

		} catch (Exception e) {
			log.error(e);
			setProgress(e.getLocalizedMessage(), 1.0d);
			taskResult.setFinalOutcome(ERXTaskOutcome.FAILED);
			return taskResult;

		} finally {
			ec.unlock();
			ec.dispose();
		}

	}

}
