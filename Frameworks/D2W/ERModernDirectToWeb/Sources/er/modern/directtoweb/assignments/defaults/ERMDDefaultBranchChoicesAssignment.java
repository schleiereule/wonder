package er.modern.directtoweb.assignments.defaults;

import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;

import er.directtoweb.assignments.ERDAssignment;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXGuardedObjectInterface;
import er.extensions.eof.ERXNonNullObjectInterface;
import er.extensions.foundation.ERXValueUtilities;
import er.modern.directtoweb.components.buttons.ERMDDeleteButton.Keys;

public class ERMDDefaultBranchChoicesAssignment extends ERDAssignment {

	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes Affecting
	 * Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	public static final NSArray<String> inspectControllerDependentKeys = new NSArray<String>("task", "entity", "frame", "isEntityDeletable", "isEntityEditable", "readOnly", "object.canDelete", "object.canUpdate");
	public static final NSArray<String> editControllerDependentKeys = new NSArray<String>("task", "subTask", "tabCount", "tabIndex");
	public static final NSArray<String> toManyRelationshipDependentKeys = new NSArray<String>("task", "entity", "parentRelationship", "frame", "isEntityEditable", "readOnly", "shouldShowQueryRelatedButton");
	public static final NSArray<String> toOneRelationshipDependentKeys = new NSArray<String>("task", "entity", "parentRelationship", "frame", "isEntityDeletable", "isEntityEditable", "isEntityInspectable", "readOnly", "object.canDelete",
			"object.canUpdate", "object.isNonNull");
	public static final NSArray<String> queryControllerDependentKeys = new NSArray<String>("task", "frame");

	public static final NSDictionary<String, NSArray<String>> dependentKeys;

	static {
		NSMutableDictionary<String, NSArray<String>> keys = new NSMutableDictionary<String, NSArray<String>>();
		keys.setObjectForKey(inspectControllerDependentKeys, "inspectControllerChoices");
		keys.setObjectForKey(editControllerDependentKeys, "editControllerChoices");
		keys.setObjectForKey(toManyRelationshipDependentKeys, "toManyControllerChoices");
		keys.setObjectForKey(toOneRelationshipDependentKeys, "toOneControllerChoices");
		keys.setObjectForKey(queryControllerDependentKeys, "queryControllerChoices");
		dependentKeys = keys.immutableClone();
	}

	public ERMDDefaultBranchChoicesAssignment(EOKeyValueUnarchiver u) {
		super(u);
	}

	public ERMDDefaultBranchChoicesAssignment(String key, Object value) {
		super(key, value);
	}

	public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver unarchiver) {
		return new ERMDDefaultBranchChoicesAssignment(unarchiver);
	}

	@Override
	public NSArray<String> dependentKeys(String keyPath) {
		return dependentKeys.objectForKey(keyPath);
	}
	
	public Object inspectControllerChoices(D2WContext c) {
		NSMutableArray<String> choices = new NSMutableArray<String>();
		EOEnterpriseObject eo = (EOEnterpriseObject)c.valueForKey("object");
		EOEntity e = c.entity();
		boolean unguarded = !ERXGuardedObjectInterface.class.isAssignableFrom(classForEntity(e));
		boolean isEntityEditable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityEditable"));
		boolean isEntityDeletable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityDeletable"));
		boolean canUpdate = eo instanceof ERXGuardedObjectInterface?((ERXGuardedObjectInterface)eo).canUpdate():unguarded;
		boolean canDelete = eo instanceof ERXGuardedObjectInterface?((ERXGuardedObjectInterface)eo).canDelete():unguarded;
		boolean isEntityWritable = !ERXValueUtilities.booleanValue(c.valueForKey("readOnly"));

		if(!c.frame()) {
			choices.add("_return");
		}
		if(isEntityWritable && isEntityEditable && canUpdate) {
			choices.add("_edit");
		}
		if(isEntityWritable && isEntityDeletable && canDelete) {
			choices.add("_deleteReturn");
		}
		return choices;
	}

	public Object editControllerChoices(D2WContext c) {
		String subTask = (String) c.valueForKey("subTask");
		if ("wizard".equals(subTask)) {
			Integer count = (Integer) c.valueForKey("tabCount");
			Integer index = (Integer) c.valueForKey("tabIndex");
			if (count != null && index != null && count.intValue() > 1) {
				if (index == 0) {
					return new NSArray<String>("_cancelEdit", "_nextStep");
				} else if (index.intValue() + 1 == count.intValue()) {
					return new NSArray<String>("_cancelEdit", "_prevStep", "_save");
				} else {
					return new NSArray<String>("_cancelEdit", "_prevStep", "_nextStep");
				}
			}
		}
		return new NSArray<String>("_cancelEdit", "_save");
	}

	public Object toManyControllerChoices(D2WContext c) {
		NSMutableArray<String> choices = new NSMutableArray<String>();
		EORelationship rel = (EORelationship) c.valueForKey("parentRelationship");
		EOEntity e = c.entity();
		boolean isEntityEditable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityEditable"));
		boolean isEntityWritable = !ERXValueUtilities.booleanValue(c.valueForKey("readOnly"));
		boolean isConcrete = !e.isAbstractEntity();

		boolean isEntityQueryable = ERXValueUtilities.booleanValue(c.valueForKey("shouldShowQueryRelatedButton"));

		if (!c.frame()) {
			choices.add("_returnRelated");
		}
		if ((rel.inverseRelationship() == null || isEntityWritable) && isEntityQueryable) {
			choices.add("_queryRelated");
		}
		if (isEntityWritable && isEntityEditable && isConcrete && e.subEntities().isEmpty()) {
			choices.add("_createRelated");
		}
		return choices;
	}

	public Object toOneControllerChoices(D2WContext c) {
		NSMutableArray<String> choices = new NSMutableArray<String>();
		EOEnterpriseObject eo = (EOEnterpriseObject) c.valueForKey("object");
		EORelationship rel = (EORelationship) c.valueForKey("parentRelationship");
		EOEntity e = c.entity();
		boolean nullInterface = ERXNonNullObjectInterface.class.isAssignableFrom(classForEntity(e));
		boolean isNonNull = ERXValueUtilities.booleanValue(c.valueForKeyPath("object.isNonNull"));
		boolean unguarded = !ERXGuardedObjectInterface.class.isAssignableFrom(classForEntity(e));
		boolean isEntityEditable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityEditable"));
		boolean isEntityDeletable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityDeletable"));
		boolean isEntityInspectable = ERXValueUtilities.booleanValue(c.valueForKey("isEntityInspectable"));
		boolean canUpdate = eo instanceof ERXGuardedObjectInterface ? ((ERXGuardedObjectInterface) eo).canUpdate() : unguarded;
		boolean canDelete = eo instanceof ERXGuardedObjectInterface ? ((ERXGuardedObjectInterface) eo).canDelete() : unguarded;
		boolean isPendingDeletion = ERXEOControlUtilities.eoEquals(eo, (EOEnterpriseObject) c.valueForKey(Keys.objectPendingDeletion));
		boolean isEntityWritable = !ERXValueUtilities.booleanValue(c.valueForKey("readOnly"));
		boolean isConcrete = !e.isAbstractEntity();
		boolean isEntityQueryable = ERXValueUtilities.booleanValueWithDefault(c.valueForKey("shouldShowQueryRelatedButton"), true);

		if (!c.frame()) {
			choices.add("_returnRelated");
		}
		if ((rel.inverseRelationship() == null || isEntityWritable) && isEntityQueryable) {
			choices.add("_queryRelated");
		}
		if (isEntityInspectable && !isPendingDeletion && (isNonNull || (!nullInterface))) {
			choices.add("_inspectRelated");
		}
		if (isEntityWritable && isEntityEditable && isConcrete && e.subEntities().isEmpty()) {
			choices.add("_createRelated");
		}
		if (isEntityWritable && isEntityEditable && !isPendingDeletion && canUpdate) {
			choices.add("_editRelated");
			if (!rel.ownsDestination()) {
				choices.add("_removeRelated");
			}
		}
		if (isEntityWritable && isEntityDeletable && canDelete) {
			choices.add("_delete");
		}
		return choices;
	}

	public Object editListControllerChoices(D2WContext c) {
		NSMutableArray<String> choices = new NSMutableArray<String>();
		EOEnterpriseObject eo = (EOEnterpriseObject) c.valueForKey("object");
		choices.add("_editListRelated");
		return choices;
	}

	public Object queryControllerChoices(D2WContext c) {
		if (c.frame()) {
			return new NSArray<String>("_clear", "_query");
		}
		return new NSArray<String>("_clear", "_query");
	}

	private Class<?> classForEntity(EOEntity entity) {
		try {
			return Class.forName(entity.className());
		} catch (ClassNotFoundException e) {
			throw NSForwardException._runtimeExceptionForThrowable(e);
		}
	}

}
