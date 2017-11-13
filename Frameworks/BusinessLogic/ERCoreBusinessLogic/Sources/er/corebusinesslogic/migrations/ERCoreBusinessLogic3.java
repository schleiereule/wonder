package er.corebusinesslogic.migrations;

import java.sql.ResultSetMetaData;

import javax.sql.rowset.CachedRowSet;

import org.apache.log4j.Logger;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.foundation.ERXProperties;
import er.extensions.jdbc.ERXJDBCUtilities;
import er.extensions.migration.ERXMigrationDatabase;
import er.extensions.migration.ERXMigrationTable;
import er.extensions.migration.ERXModelVersion;

/**
 *
 * Add last entry relation to ERCAuditTrail
 * 
 * @property ERCoreBusinessLogic1.languages
 */
public class ERCoreBusinessLogic3 extends ERXMigrationDatabase.Migration {

    /** general logging support */
    public static final Logger log = Logger.getLogger(ERCoreBusinessLogic3.class);

    public ERCoreBusinessLogic3() {
        super(ERXProperties.arrayForKey("ERCoreBusinessLogic1.languages"));
    }

    @Override
    public NSArray<ERXModelVersion> modelDependencies() {
        return null;
    }

    @Override
    public void downgrade(EOEditingContext editingContext,
                          ERXMigrationDatabase database) throws Throwable {
        // DO NOTHING
    }

    @Override
    public void upgrade(EOEditingContext editingContext,
                        ERXMigrationDatabase database) throws Throwable {
        ERXMigrationTable ercAuditTrailTable = database.existingTableNamed("ERCAuditTrail");
        // HACK: check whether lastEntryID column already exists
        boolean exists = false;
        CachedRowSet rowSet = ERXJDBCUtilities.fetchRowSet(database.adaptorChannel(),
                "SELECT * FROM ERCAuditTrail WHERE id = 1");
        // sneaky way to get the existing column names w/o triggering an exception
        ResultSetMetaData meta = rowSet.getMetaData();
        int numCol = meta.getColumnCount();
        for (int i = 1; i < numCol + 1; i++) {
            if (meta.getColumnName(i).equalsIgnoreCase("lastEntryID")) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            // these may already exist due to merged migrations
            ercAuditTrailTable.newIntegerColumn("lastEntryID", ALLOWS_NULL);
            ercAuditTrailTable.addForeignKey("lastEntryID", "ERCAuditTrailEntry", "id");
        }
    }

}