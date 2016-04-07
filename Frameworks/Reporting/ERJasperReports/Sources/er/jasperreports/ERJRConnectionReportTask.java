package er.jasperreports;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.apache.commons.lang.exception.NestableRuntimeException;
import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOFetchSpecification;

import er.extensions.appserver.ERXApplication;
import er.extensions.concurrency.IERXPercentComplete;
import er.extensions.eof.ERXEC;
import er.extensions.foundation.ERXAssert;

/**
 * A background task class that creates a JasperReports report in the context
 * of a WebObjects application. Sensible defaults are used.
 * 
 * A convenient Builder pattern inner class is provided too.
 * 
 * @author kieran
 */
public class ERJRConnectionReportTask implements Callable<File>, IERXPercentComplete {
	private static final Logger log = Logger.getLogger(ERJRConnectionReportTask.class);
	
	private File reportFile;
	private final String frameworkName;
	private final String jasperCompiledReportFileName;
	private Map<String, Object> parameters;
	private Connection connection;
	
	// iVar so we can get percentage complete
	private ERJRFoundationDataSource jrDataSource;
	
	public ERJRConnectionReportTask(Connection connection, String jasperCompiledReportFileName) {
		this(connection, jasperCompiledReportFileName, null, null);
	}
	
	public ERJRConnectionReportTask(Connection connection, String jasperCompiledReportFileName, HashMap<String, Object> parameters) {
		this(connection, jasperCompiledReportFileName, null, parameters);
	}
	
	
	public ERJRConnectionReportTask(Connection connection, String jasperCompiledReportFileName, String frameworkName, HashMap<String, Object> parameters) {
		ERXAssert.PRE.notNull(connection);
		ERXAssert.PRE.notNull(jasperCompiledReportFileName);
		
		this.jasperCompiledReportFileName = jasperCompiledReportFileName;
		this.frameworkName = frameworkName;
		this.parameters = parameters;
		this.connection = connection;
		
		if (this.parameters == null) {
			this.parameters = new HashMap<String, Object>();
		}
		
	}


	/**
	 * Callable interface implementation
	 * 
	 * @throws Exception
	 */
	public File call() throws Exception {

		ERXApplication._startRequest();
		try {
			return _call();
		} catch (Exception e) {
			log.error("Error in JR task", e);
			throw e;
		} finally {
			// Unlocks any locked editing contexts
			ERXApplication._endRequest();
		}

	}

	private File _call() {
		// If development
		if (ERXApplication.isDevelopmentModeSafe()) {
			parameters.put("_isDevelopmentMode", Boolean.TRUE);
		} else {
			parameters.put("_isDevelopmentMode", Boolean.FALSE );
		}
		
		reportFile = null;
		
		if (log.isDebugEnabled())
			log.debug("Starting JasperReportTask: " + toString());
		try {
			
			if (jasperCompiledReportFileName != null) {
				reportFile = ERJRUtilities.runCompiledReportToPDFFile(jasperCompiledReportFileName, frameworkName, parameters, connection);
			}
			
			
		} catch (Exception e) {
			throw new NestableRuntimeException(e);
		} finally {
		}

		return reportFile;
	}

	public File file() {
		return reportFile;
	}

	/* (non-Javadoc)
	 * Fake
	 */
	public Double percentComplete() {
		return Double.valueOf(0.1);
	}
	
}
