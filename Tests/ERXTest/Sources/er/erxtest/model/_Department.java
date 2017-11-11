// DO NOT EDIT.  Make changes to Department.java instead.
package er.erxtest.model;

import com.webobjects.eoaccess.*;
import com.webobjects.eocontrol.*;
import com.webobjects.foundation.*;
import java.math.*;
import java.util.*;

import er.extensions.eof.*;
import er.extensions.eof.ERXKey.Type;
import er.extensions.foundation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public abstract class _Department extends er.extensions.eof.ERXGenericRecord {
  public static final String ENTITY_NAME = "Department";

  // Attribute Keys
  public static final ERXKey<String> NAME = new ERXKey<String>("name", Type.Attribute);

  // Relationship Keys
  public static final ERXKey<er.erxtest.model.Employee> EMPLOYEES = new ERXKey<er.erxtest.model.Employee>("employees", Type.ToManyRelationship);

  // Attributes
  public static final String NAME_KEY = NAME.key();

  // Relationships
  public static final String EMPLOYEES_KEY = EMPLOYEES.key();

  private static final Logger log = LoggerFactory.getLogger(_Department.class);

  public Department localInstanceIn(EOEditingContext editingContext) {
    Department localInstance = (Department)EOUtilities.localInstanceOfObject(editingContext, this);
    if (localInstance == null) {
      throw new IllegalStateException("You attempted to localInstance " + this + ", which has not yet committed.");
    }
    return localInstance;
  }

  public String name() {
    return (String) storedValueForKey(_Department.NAME_KEY);
  }

  public void setName(String value) {
    log.debug( "updating name from {} to {}", name(), value);
    takeStoredValueForKey(value, _Department.NAME_KEY);
  }

  public NSArray<er.erxtest.model.Employee> employees() {
    return (NSArray<er.erxtest.model.Employee>)storedValueForKey(_Department.EMPLOYEES_KEY);
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier) {
    return employees(qualifier, null, false);
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier, boolean fetch) {
    return employees(qualifier, null, fetch);
  }

  public NSArray<er.erxtest.model.Employee> employees(EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings, boolean fetch) {
    NSArray<er.erxtest.model.Employee> results;
    if (fetch) {
      EOQualifier fullQualifier;
      EOQualifier inverseQualifier = ERXQ.equals(er.erxtest.model.Employee.DEPARTMENT_KEY, this);

      if (qualifier == null) {
        fullQualifier = inverseQualifier;
      }
      else {
        fullQualifier = ERXQ.and(qualifier, inverseQualifier);
      }

      results = er.erxtest.model.Employee.fetchEmployees(editingContext(), fullQualifier, sortOrderings);
    }
    else {
      results = employees();
      if (qualifier != null) {
        results = (NSArray<er.erxtest.model.Employee>)EOQualifier.filteredArrayWithQualifier(results, qualifier);
      }
      if (sortOrderings != null) {
        results = (NSArray<er.erxtest.model.Employee>)EOSortOrdering.sortedArrayUsingKeyOrderArray(results, sortOrderings);
      }
    }
    return results;
  }

  public void addToEmployees(er.erxtest.model.Employee object) {
    includeObjectIntoPropertyWithKey(object, _Department.EMPLOYEES_KEY);
  }

  public void removeFromEmployees(er.erxtest.model.Employee object) {
    excludeObjectFromPropertyWithKey(object, _Department.EMPLOYEES_KEY);
  }

  public void addToEmployeesRelationship(er.erxtest.model.Employee object) {
    log.debug("adding {} to employees relationship", object);
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
      addToEmployees(object);
    }
    else {
      addObjectToBothSidesOfRelationshipWithKey(object, _Department.EMPLOYEES_KEY);
    }
  }

  public void removeFromEmployeesRelationship(er.erxtest.model.Employee object) {
    log.debug("removing {} from employees relationship", object);
    if (er.extensions.eof.ERXGenericRecord.InverseRelationshipUpdater.updateInverseRelationships()) {
      removeFromEmployees(object);
    }
    else {
      removeObjectFromBothSidesOfRelationshipWithKey(object, _Department.EMPLOYEES_KEY);
    }
  }

  public er.erxtest.model.Employee createEmployeesRelationship() {
    EOEnterpriseObject eo = EOUtilities.createAndInsertInstance(editingContext(),  er.erxtest.model.Employee.ENTITY_NAME );
    addObjectToBothSidesOfRelationshipWithKey(eo, _Department.EMPLOYEES_KEY);
    return (er.erxtest.model.Employee) eo;
  }

  public void deleteEmployeesRelationship(er.erxtest.model.Employee object) {
    removeObjectFromBothSidesOfRelationshipWithKey(object, _Department.EMPLOYEES_KEY);
    editingContext().deleteObject(object);
  }

  public void deleteAllEmployeesRelationships() {
    Enumeration<er.erxtest.model.Employee> objects = employees().immutableClone().objectEnumerator();
    while (objects.hasMoreElements()) {
      deleteEmployeesRelationship(objects.nextElement());
    }
  }


  public static Department createDepartment(EOEditingContext editingContext, String name
) {
    Department eo = (Department) EOUtilities.createAndInsertInstance(editingContext, _Department.ENTITY_NAME);
    eo.setName(name);
    return eo;
  }

  public static ERXFetchSpecification<Department> fetchSpec() {
    return new ERXFetchSpecification<Department>(_Department.ENTITY_NAME, null, null, false, true, null);
  }

  public static NSArray<Department> fetchAllDepartments(EOEditingContext editingContext) {
    return _Department.fetchAllDepartments(editingContext, null);
  }

  public static NSArray<Department> fetchAllDepartments(EOEditingContext editingContext, NSArray<EOSortOrdering> sortOrderings) {
    return _Department.fetchDepartments(editingContext, null, sortOrderings);
  }

  public static NSArray<Department> fetchDepartments(EOEditingContext editingContext, EOQualifier qualifier, NSArray<EOSortOrdering> sortOrderings) {
    ERXFetchSpecification<Department> fetchSpec = new ERXFetchSpecification<Department>(_Department.ENTITY_NAME, qualifier, sortOrderings);
    NSArray<Department> eoObjects = fetchSpec.fetchObjects(editingContext);
    return eoObjects;
  }

  public static Department fetchDepartment(EOEditingContext editingContext, String keyName, Object value) {
    return _Department.fetchDepartment(editingContext, ERXQ.equals(keyName, value));
  }

  public static Department fetchDepartment(EOEditingContext editingContext, EOQualifier qualifier) {
    NSArray<Department> eoObjects = _Department.fetchDepartments(editingContext, qualifier, null);
    Department eoObject;
    int count = eoObjects.count();
    if (count == 0) {
      eoObject = null;
    }
    else if (count == 1) {
      eoObject = eoObjects.objectAtIndex(0);
    }
    else {
      throw new IllegalStateException("There was more than one Department that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Department fetchRequiredDepartment(EOEditingContext editingContext, String keyName, Object value) {
    return _Department.fetchRequiredDepartment(editingContext, ERXQ.equals(keyName, value));
  }

  public static Department fetchRequiredDepartment(EOEditingContext editingContext, EOQualifier qualifier) {
    Department eoObject = _Department.fetchDepartment(editingContext, qualifier);
    if (eoObject == null) {
      throw new NoSuchElementException("There was no Department that matched the qualifier '" + qualifier + "'.");
    }
    return eoObject;
  }

  public static Department localInstanceIn(EOEditingContext editingContext, Department eo) {
    Department localInstance = (eo == null) ? null : ERXEOControlUtilities.localInstanceOfObject(editingContext, eo);
    if (localInstance == null && eo != null) {
      throw new IllegalStateException("You attempted to localInstance " + eo + ", which has not yet committed.");
    }
    return localInstance;
  }
}
