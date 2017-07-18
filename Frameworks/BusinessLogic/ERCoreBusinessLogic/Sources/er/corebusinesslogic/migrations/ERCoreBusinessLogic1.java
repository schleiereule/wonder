package er.corebusinesslogic.migrations;

import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;

import er.extensions.foundation.ERXProperties;
import er.extensions.migration.ERXMigrationDatabase;
import er.extensions.migration.ERXMigrationTable;
import er.extensions.migration.ERXModelVersion;

/**
 * Add last entry relation to ERCAuditTrail
 */
public class ERCoreBusinessLogic1 extends ERXMigrationDatabase.Migration {
    
    public ERCoreBusinessLogic1() {
       super(ERXProperties.arrayForKey("ERCoreBusinessLogic0.languages"));
    }
    
    @Override
    public NSArray<ERXModelVersion> modelDependencies() {
        return null;
    }

    @Override
    public void downgrade(EOEditingContext editingContext, ERXMigrationDatabase database) throws Throwable {
        // DO NOTHING
    }

    @Override
    public void upgrade(EOEditingContext editingContext, ERXMigrationDatabase database) throws Throwable {
        ERXMigrationTable ercAuditTrailTable = database.existingTableNamed("ERCAuditTrail");
        ercAuditTrailTable.newIntegerColumn("lastEntryID", ALLOWS_NULL);
        ercAuditTrailTable.addForeignKey("lastEntryID", "ERCAuditTrailEntry", "id");
    }
}
