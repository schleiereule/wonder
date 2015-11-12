/*
 * Copyright (C) NetStruxr, Inc. All rights reserved.
 *
 * This software is published under the terms of the NetStruxr
 * Public Software License version 0.5, a copy of which has been
 * included with this distribution in the LICENSE.NPL file.  */
package com.webobjects.directtoweb;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOModelGroup;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSKeyValueCoding.UnknownKeyException;

import er.directtoweb.interfaces.ERDPickPageInterface;

// This is needed because pageFinalized is a protected method.
public class ERD2WUtilities {

    private static Logger log = Logger.getLogger(ERD2WUtilities.class);

    public static void finalizeContext(D2WContext context) {
        if (context != null)
            context.pageFinalized();
    }

    public static void resetContextCache(D2WContext context) {
        if (context != null)
            context._localValues.clear();
    }
    
    public static boolean assignmentsAreEqual(Assignment a1, Assignment a2) {
        boolean areEqual = false;
        if (a1.getClass().equals(a2.getClass()) && a1.keyPath() != null && a2.keyPath() != null && a1.value() != null && a2.value() != null) {
            areEqual = a1.keyPath().equals(a2.keyPath()) && a1.value().equals(a2.value());
        }
        return areEqual;
    }

    // This prevents the dreaded KeyValueCoding null object exception, for say key paths: object.entityName
    // Should just return null instead of throwing.
    public static Object contextValueForKeyNoInferenceNoException(D2WContext c, String keyPath) {
        Object result = null;
        int i = keyPath.indexOf(".");
        if (i == -1) {
            result = c.valueForKeyNoInference(keyPath);
        } else {
            String first = keyPath.substring(0, i);
            String second = keyPath.substring(i + 1);
            result = c.valueForKeyNoInference(first);
            if (result != null) {
                // Optimized for two paths deep
                
                try {
                    if (second.indexOf(".") == -1) {
                        result = NSKeyValueCoding.Utility.valueForKey(result, second);
                    } else {
                        NSArray parts = NSArray.componentsSeparatedByString(second, ".");
                        for (int j = 0; j < parts.count(); j++) {
                            String part = (String)parts.objectAtIndex(j);
                            result = NSKeyValueCoding.Utility.valueForKey(result, part);
                            if (result == null)
                                break;
                        }
                    }
                } catch (NSKeyValueCoding.UnknownKeyException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("keyPath "+keyPath+" is not available for context with entity: "+c.entity().name() + "; task: " + c.task());
                    }
                    return null;
                }
            }
        }
        return result;
    }
    
    /** Utility to return the next page in the enclosing page. */
    public static WOComponent nextPageInPage(D2WPage parent) {
        WOComponent result = parent.context().page();
        WOComponent old = parent.context().component();
        try {
            parent.context()._setCurrentComponent(parent);
            if(parent.nextPageDelegate() != null) {
                NextPageDelegate delegate = parent.nextPageDelegate();
                result = delegate.nextPage(parent);
            } else {
                result = parent.nextPage();
            }
        } finally {
            parent.context()._setCurrentComponent(old);
        }
        return result;
    }

    /** Utility to return the first enclosing component that matches the given class, if there is one. */
    public static WOComponent enclosingPageOfClass(WOComponent sender, Class c) {
        WOComponent p = sender.parent();
        while(p != null) {
            if(c.isAssignableFrom(p.getClass()))
                return p;
            p = p.parent();
        }
        return null;
    }
    
    /**
     * This method is similar to enclosingPageOfClass. It differs in that it is generic
     * and it inspects the sender argument as well as its parents.
     * @param <T> The class type
     * @param sender the sender component
     * @param c the class
     * @return sender or the first of sender's parents that is assignable from class c
     */
    public static <T> T enclosingComponentOfClass(WOComponent sender, Class<T> c) {
        WOComponent p = sender;
        while(p != null) {
            if(c.isAssignableFrom(p.getClass()))
                return (T)p;
            p = p.parent();
        }
        return null;
    }

    /** Utility to return the outermost page that is a D2W page. This is needed because this component might be embedded inside a plain page. */
    public static D2WPage topLevelD2WPage(WOComponent sender) {
        WOComponent p = sender.parent();
        WOComponent last = null;
        while(p != null) {
            if(p instanceof D2WPage) {
                last = p;
            }
            p = p.parent();
        }
        return (D2WPage)last;
    }

    /** Utility to return the enclosing list page, if there is one. */
    public static ListPageInterface parentListPage(WOComponent sender) {
        return (ListPageInterface)enclosingPageOfClass(sender, ListPageInterface.class);
    }
    
    /** Utility to return the enclosing edit page, if there is one. */
    public static EditPageInterface parentEditPage(WOComponent sender) {
        return (EditPageInterface)enclosingPageOfClass(sender, EditPageInterface.class);
    }
    
    /** Utility to return the enclosing select page, if there is one. */
    public static SelectPageInterface parentSelectPage(WOComponent sender) {
        return (SelectPageInterface)enclosingPageOfClass(sender, SelectPageInterface.class);
    }
    
    /** Utility to return the enclosing query page, if there is one. */
    public static QueryPageInterface parentQueryPage(WOComponent sender) {
        return (QueryPageInterface)enclosingPageOfClass(sender, QueryPageInterface.class);
    }

    /** Utility to return the enclosing pick page, if there is one. */
    public static ERDPickPageInterface parentPickPage(WOComponent sender) {
        return (ERDPickPageInterface)enclosingPageOfClass(sender, ERDPickPageInterface.class);
    }

    /** Utility to return the enclosing D2W page, if there is one. */
    public D2WPage parentD2WPage(WOComponent sender) {
        return (D2WPage)enclosingPageOfClass(sender, D2WPage.class);
    }
    
    /**
	 * For a given D2WContext, this will return the target entity of the
	 * context's propertyKey key path. Adapted from ERDDefaultModelAssignment's
	 * smartRelationship method.
	 * 
	 * @param d2wContext
	 * @return the entity carrying the last property in the property key path
	 */
    public static EOEntity entityForPropertyKeyPath(D2WContext d2wContext) {
        Object rawObject = d2wContext.valueForKey("object");
        String propertyKey = d2wContext.propertyKey();
        return entityForPropertyKeyPathOfObject(rawObject, propertyKey);
    }

    /**
     * For a given EO and a key path, retrieve the target entity of the key path.
     * 
     * @param rawObject
     * @param propertyKey
     * @return the entity carrying the last property in the property key path
     */
    public static EOEntity entityForPropertyKeyPathOfObject(Object rawObject,
                                                            String propertyKey) {
        EOEntity entityForKeyPath = null;
        if (propertyKey != null) {
            try {
                if (rawObject != null && rawObject instanceof EOEnterpriseObject) {
                    EOEnterpriseObject object = (EOEnterpriseObject) rawObject;
                    EOEnterpriseObject lastEO = object;
                    if (propertyKey.indexOf(".") != -1 && propertyKey.indexOf("@") == -1) {
                        String partialKeyPath = KeyValuePath
                                .keyPathWithoutLastProperty(propertyKey);
                        Object rawLastEO = object.valueForKeyPath(partialKeyPath);
                        lastEO = rawLastEO instanceof EOEnterpriseObject ? (EOEnterpriseObject) rawLastEO
                                : null;
                    }
                    if (lastEO != null) {
                        entityForKeyPath = EOModelGroup.defaultGroup().entityNamed(
                                lastEO.entityName());
                    }
                }
            } catch (UnknownKeyException uke) {
                log.error("Failed to get key path " + propertyKey
                        + " on object of class " + rawObject.getClass());
            }
        }
        return entityForKeyPath;
    }

}
