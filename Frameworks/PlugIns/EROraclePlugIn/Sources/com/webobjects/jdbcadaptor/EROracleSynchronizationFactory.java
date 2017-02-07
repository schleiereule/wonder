package com.webobjects.jdbcadaptor;

import com.webobjects.eoaccess.EOAdaptor;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOSynchronizationFactory;
import com.webobjects.eoaccess.synchronization.EOSchemaGeneration;
import com.webobjects.eoaccess.synchronization.EOSchemaGenerationOptions;
import com.webobjects.eoaccess.synchronization.EOSchemaSynchronization;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.jdbcadaptor.OraclePlugIn.OracleSynchronizationFactory;

/** Overrides OracleSynchronizationFactory. This class does not add any
 * additional implementation, its just there to be consistent with the
 * other EOF PlugIns
 * 
 * @author David Teran
 *
 */
    public class EROracleSynchronizationFactory extends EOSynchronizationFactory implements EOSchemaGeneration, EOSchemaSynchronization {


    public EROracleSynchronizationFactory(EOAdaptor eoadaptor) {
        super(eoadaptor);
    }
    
    @SuppressWarnings("rawtypes")
    public NSArray<EOSQLExpression> statementsToInsertColumnForAttribute(EOAttribute attribute, NSDictionary options) {
        String clause = _columnCreationClauseForAttribute(attribute);
        EOSQLExpression expression = _expressionForString("alter table "
                + formatTableName(attribute.entity().externalName()) + " add " + clause);
        return new NSArray<EOSQLExpression>(expression);
    }
   
    
    /*
     * OraclePlugIn.OracleSynchronizationFactory methods
     */

    @SuppressWarnings("rawtypes")
    public NSArray<EOSQLExpression> dropTableStatementsForEntityGroup(NSArray entityGroup) {
        return new OracleSynchronizationFactory(adaptor()).dropTableStatementsForEntityGroup(entityGroup);
    }

    @Override
    public NSArray<EOSQLExpression> _statementsToDeleteTableNamedOptions(String tableName,
                                                                         EOSchemaGenerationOptions options) {
        return new OracleSynchronizationFactory(adaptor())._statementsToDeleteTableNamedOptions(tableName, options);
    }

    @Override
    public NSArray<EOSQLExpression> primaryKeySupportStatementsForEntityGroup(NSArray<EOEntity> entityGroup) {
        return new OracleSynchronizationFactory(adaptor()).primaryKeySupportStatementsForEntityGroup(entityGroup);
    }

    @Override
    public NSArray<EOSQLExpression> dropPrimaryKeySupportStatementsForEntityGroup(NSArray<EOEntity> entityGroup) {
        return new OracleSynchronizationFactory(adaptor()).dropPrimaryKeySupportStatementsForEntityGroup(entityGroup);
    }

    @Override
    public void appendExpressionToScript(EOSQLExpression expression,
                                         StringBuffer script) {
        new OracleSynchronizationFactory(adaptor()).appendExpressionToScript(expression, script);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public NSArray<EOSQLExpression> createDatabaseStatementsForConnectionDictionary(NSDictionary connectionDictionary,
                                                                                    NSDictionary administrativeConnectionDictionary) {
        return new OracleSynchronizationFactory(adaptor()).createDatabaseStatementsForConnectionDictionary(connectionDictionary, administrativeConnectionDictionary);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public NSArray<EOSQLExpression> dropDatabaseStatementsForConnectionDictionary(NSDictionary connectionDictionary,
                                                                                  NSDictionary administrativeConnectionDictionary) {
        return new OracleSynchronizationFactory(adaptor()).dropDatabaseStatementsForConnectionDictionary(connectionDictionary, administrativeConnectionDictionary);
    }

    @Override
    public boolean supportsSchemaSynchronization() {
        return true;
    }

    @Override
    public NSArray<EOSQLExpression> foreignKeyConstraintStatementsForRelationship(EORelationship relationship) {
        return new OracleSynchronizationFactory(adaptor()).foreignKeyConstraintStatementsForRelationship(relationship);
    }
}
