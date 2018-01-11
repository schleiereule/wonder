package er.modern.directtoweb.components.relationships;

import org.apache.log4j.Logger;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WODisplayGroup;
import com.webobjects.directtoweb.D2WComponent;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.ERD2WContext;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EORelationship;
import com.webobjects.eocontrol.EODetailDataSource;
import com.webobjects.foundation.NSDictionary;

import er.extensions.appserver.ERXDisplayGroup;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXModelGroup;

/**
 * Displays a to-one relationship using a table generated via the
 * ERMDSimpleListPageRepetition, the same as the default way to display a
 * to-many relationship. Mainly useful for complex target objects with many
 * properties. <br>
 * The implementation feels rather hackish due to the forcing of a
 * "ListEmbedded" page configuration.
 * 
 * @author fpeters
 */
public class ERMD2WDisplayToOneTable extends D2WComponent {

    private static final long serialVersionUID = 1L;
    
    /** general logging support */
    public static final Logger log = Logger.getLogger(ERMD2WDisplayToOneTable.class);

    public ERMD2WDisplayToOneTable(WOContext aContext) {
        super(aContext);
    }
    
    private WODisplayGroup _displayGroup;
    
    public WODisplayGroup displayGroup() {
        if (_displayGroup == null) {
            _displayGroup = new ERXDisplayGroup<>();
            EODetailDataSource ds = ERXEOControlUtilities.dataSourceForObjectAndKey(object(), propertyKey());
            _displayGroup.setDataSource(ds);
            _displayGroup.fetch();
        }
        return _displayGroup;
    }
    
    public D2WContext localContext() {
        D2WContext localContext = ERD2WContext.newContext(d2wContext());
        EOEntity objectEntity = ERXModelGroup.defaultGroup().entityForObject(object());
        // TODO FP handle multi-hop relationship keypaths
        EORelationship relationship = objectEntity.relationshipNamed(propertyKey());
        EOEntity destinationEntity = relationship.destinationEntity();
        if (destinationEntity != null) {
            localContext.takeValueForKey("ListEmbedded" + destinationEntity.name(), "pageConfiguration");
        } else {
            log.warn("Failed to resolve the destination entity for a source entity of " + objectEntity.name() + " with a relationship keypath of " + propertyKey());
        }
        // this is a display-only component, no actions should be allowed
        localContext.takeValueForKey(NSDictionary.EmptyDictionary, "actions");
        return localContext;
    }
    
}
