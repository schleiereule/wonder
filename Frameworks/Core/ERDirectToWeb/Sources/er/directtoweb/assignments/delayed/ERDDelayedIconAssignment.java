package er.directtoweb.assignments.delayed;

import org.apache.log4j.Logger;

import com.webobjects.directtoweb.D2WContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOKeyValueUnarchiver;
import com.webobjects.foundation.NSKeyValueCoding.UnknownKeyException;

/**
 * Used to resolve an icon name by calling <code>userPresentableIcon()</code> on
 * an EO.
 * 
 * @author fpeters
 *
 */
public class ERDDelayedIconAssignment extends ERDDelayedAssignment {

    private static final long serialVersionUID = 1L;

    public static Object decodeWithKeyValueUnarchiver(EOKeyValueUnarchiver eokeyvalueunarchiver) {
        return new ERDDelayedIconAssignment(eokeyvalueunarchiver);
    }

    /** Logging support */
    public final static Logger log = Logger.getLogger(ERDDelayedIconAssignment.class);

    public ERDDelayedIconAssignment(EOKeyValueUnarchiver u) {
        super(u);
    }

    public ERDDelayedIconAssignment(String key, Object value) {
        super(key, value);
    }

    public String iconName(D2WContext c) {
        String iconName = null;
        EOEnterpriseObject o = (EOEnterpriseObject) c.valueForKey("object");
        if (o != null) {
            try {
                iconName = (String) o.valueForKey("userPresentableIcon");
            } catch (UnknownKeyException uke) {
                log.warn("Failed to get an icon path from an object of entity "
                        + c.valueForKey("entityName")
                        + ", did you forget to implement the 'userPresentableIcon' method?",
                        uke);
            }
        }
        return iconName;
    }

    @Override
    public Object fireNow(D2WContext c) {
        return iconName(c);
    }

}
