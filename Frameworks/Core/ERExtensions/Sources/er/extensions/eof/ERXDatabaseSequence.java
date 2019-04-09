package er.extensions.eof;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.jdbcadaptor.JDBCAdaptorException;

import er.extensions.jdbc.ERXSQLHelper;

public class ERXDatabaseSequence {

	private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ERXDatabaseSequence.class);

	private final String _name;
	private EOEditingContext _editingContext;
	private String _modelName;

	private ERXDatabaseSequence() {
		throw new AssertionError();
	}

	private ERXDatabaseSequence(EOEditingContext editingContext, String modelName, String name) {
		_editingContext = editingContext;
		_modelName = modelName;
		_name = name;
	}

	public static ERXDatabaseSequence getInstance(EOEditingContext editingContext, String modelName, String name) {
		try {
			String exp = ERXSQLHelper.newSQLHelper(editingContext, modelName).sqlForCreateSequence(name);
			ERXEOAccessUtilities.evaluateSQLWithModelNamed(editingContext, modelName, exp);
			return new ERXDatabaseSequence(editingContext, modelName, name);
		}
		catch (Exception e) {
			if (e instanceof JDBCAdaptorException) {
				JDBCAdaptorException adaptorException = (JDBCAdaptorException) e;
				String sqlState = adaptorException.sqlException().getSQLState().toUpperCase();
				switch (sqlState) {
				case "42P07": // postgresql sequence already exists
					return new ERXDatabaseSequence(editingContext, modelName, name);
				case "42000": // oracle sequence already exists
					return new ERXDatabaseSequence(editingContext, modelName, name);
				default:
					log.error(e);
					break;
				}
			}
		}
		return null;
	}

	public String name() {
		return _name;
	}

	public long nextValue() {
		return nextValue(1L);
	}

	public long nextValue(long increment) {
		if (increment != 1) {
			throw new IllegalArgumentException("ERXDatabaseSequence only supports incrementing 1 at a time.");
		}
		return ERXSQLHelper.newSQLHelper(_editingContext, _modelName).getNextValFromSequenceNamed(_editingContext, _modelName, name()).longValue();
	}

}
