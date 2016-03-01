package er.corebusinesslogic;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eocontrol.EOEnterpriseObject;

import er.directtoweb.components.ERDCustomComponent;
import er.extensions.appserver.ERXApplication;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.foundation.ERXProperties;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.localization.ERXLocalizer;

/**
 * A flexible property-level help text component, adapted from
 * @ERCDisplayHelpText. To enable it, set
 * 
 * <pre>
 * er.corebusinesslogic.ERCoreBusinessLogic.enablePropertyLevelHelpText = true
 * </pre>
 * 
 * There are two ways to specify a help text for a given property:
 * <ul>
 * <li>put keys of the form "HelpText.default.Entity.property" in your
 * Localizable.strings
 * <li>use the runtime editing to store keys in the DB
 * </ul>
 * 
 * Values in the DB will override the defaults from Localizable.strings.
 * 
 * @author fpeters
 */
public class ERCPropertyHelpText extends ERDCustomComponent {

    private static final long serialVersionUID = 1L;

    public ERCPropertyHelpText(WOContext aContext) {
        super(aContext);
    }

    /**
     * Identifies the correct help text key for the current propertyKey. Given a
     * propertyKey of "role.roleName.talent.firstName", the help text key would
     * be "Talent.firstName".
     * 
     * @return the key to retrieve a help text for
     */
    public String helpTextKey() {
        String helpTextKey = null;
        if (canGetValueForBinding("helpTextKey")) {
            helpTextKey = stringValueForBinding("helpTextKey");
        } else if (d2wContext() != null) {
            if (d2wContext().propertyKey() == null) {
                // we're outside of an attribute repetition, so
                // this is a page-level help text
                helpTextKey = (String) d2wContext().valueForKey("pageConfiguration");
            } else {
                if (d2wContext().propertyKey().indexOf('.') == -1) {
                    // it's a key on the current object
                    helpTextKey = d2wContext().entity().name() + "."
                            + d2wContext().propertyKey();
                } else if (d2wContext().valueForKey("object") != null) {
                    // it's a key path, let's find the target entity
                    EOEntity entity = ERD2WUtilities
                            .entityForPropertyKeyPath(d2wContext());
                    if (entity != null) {
                        helpTextKey = entity.name() + "." + ERXStringUtilities
                                .lastPropertyKeyInKeyPath(d2wContext().propertyKey());
                    }
                } else if (d2wContext().valueForKey("object") == null) {
                    // we're on a query page
                    helpTextKey = d2wContext().entity().name() + "."
                            + d2wContext().propertyKey();
                }
            }
        }
        return helpTextKey;
    }

    /**
     * Retrieves a prefix that will be applied to the help text key. The prefix
     * can be specified via a binding or computed at runtime, using a
     * prefixKeyPath set in the application properties. This can be used to show
     * different help texts, depending on user group, tenant etc. E.g.:
     * "session.user.tenant.id"
     * 
     * @return a prefix to apply to the standard help text key
     */
    public String helpTextPrefix() {
        String helpTextPrefix = null;
        if (canGetValueForBinding("helpTextPrefix")) {
            helpTextPrefix = stringValueForBinding("helpTextPrefix");
        } else {
            String prefixKeyPath = ERXProperties
                    .stringForKey("er.corebusinesslogic.ERCoreBusinessLogic.helpText.prefixKeyPath");
            if (!ERXStringUtilities.stringIsNullOrEmpty(prefixKeyPath)) {
                boolean keyPathRequiresSession = prefixKeyPath.toLowerCase().contains(
                        "session");
                if (!keyPathRequiresSession || context().session() != null) {
                    try {
                        Object keyPathValue = valueForKeyPath(prefixKeyPath);
                        if (keyPathValue != null) {
                            helpTextPrefix = keyPathValue.toString();
                        }
                    } catch (NullPointerException npe) {
                        if (log.isDebugEnabled()) {
                            log.debug("Failed to retrieve help text prefix keypath: "
                                    + prefixKeyPath);
                        }
                    }
                } else {
                    log.warn("Help text prefix keypath is '" + prefixKeyPath
                            + "', but session is null!");
                }
            }
        }
        return helpTextPrefix;
    }

    /**
     * @return the default help text key, as specified in Localizable.strings
     */
    public String defaultValue() {
        String helpTextDefaultValue = "";
        if (canGetValueForBinding("helpTextDefaultValue")) {
            helpTextDefaultValue = stringValueForBinding("helpTextDefaultValue");
        } else if (ERXLocalizer.currentLocalizer()
                .localizedStringForKey("HelpText.default." + helpTextKey()) != null) {
            helpTextDefaultValue = ERXLocalizer.currentLocalizer()
                    .localizedStringForKey("HelpText.default." + helpTextKey());
        }
        return helpTextDefaultValue;
    }

    public boolean hasHelpText() {
        return (helpText() != null && helpText().value() != null)
                || defaultValue() != null;
    }

    public boolean isStateless() {
        return true;
    }

    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    public ERCHelpText helpText() {
        ERCHelpText text = null;
        if (!ERXStringUtilities.stringIsNullOrEmpty(key())) {
            text = ERCHelpText.clazz.helpTextForKey(session().defaultEditingContext(),
                    key());
        }
        return text;
    }

    public boolean showCreate() {
        return showActions() && helpText() == null;
    }

    public boolean showEdit() {
        return showActions() && helpText() != null;
    }

    public boolean isHelpTextEnabled() {
        return ERXProperties.booleanForKeyWithDefault(
                "er.corebusinesslogic.ERCoreBusinessLogic.enablePropertyLevelHelpText",
                false);
    }

    /**
     * Enable or disable editing actions. Defaults to enabled in development
     * mode.
     * 
     * @return true if DB help texts should be editable
     */
    public boolean showActions() {
        boolean showActions = ERXApplication.isDevelopmentModeSafe();
        if (canGetValueForBinding("isHelpTextEditable")) {
            showActions = booleanValueForBinding("isHelpTextEditable");
        } else if (d2wContext() != null
                && ERXValueUtilities.booleanValue(d2wContext().valueForKey(
                        "isHelpTextEditable"))) {
            showActions = ERXValueUtilities.booleanValue(d2wContext().valueForKey(
                    "isHelpTextEditable"));
        }
        return showActions;
    }

    public String key() {
        String prefix = helpTextPrefix();
        String key = helpTextKey();
        if (prefix != null) {
            key = prefix + "." + key;
        }
        return key;
    }

    public WOComponent createHelpText() {
        EditPageInterface page = D2W.factory().editPageForNewObjectWithEntityNamed(
                ERCHelpText.ENTITY, session());
        ((WOComponent) page).takeValueForKeyPath(key(), "object." + ERCHelpText.Key.KEY);
        ((WOComponent) page).takeValueForKeyPath(defaultValue(), "object."
                + ERCHelpText.Key.VALUE);
        page.setNextPage(context().page());
        return (WOComponent) page;
    }

    public WOComponent editHelpText() {
        EditPageInterface page = D2W.factory().editPageForEntityNamed(ERCHelpText.ENTITY,
                session());
        EOEnterpriseObject eo = ERXEOControlUtilities.editableInstanceOfObject(
                helpText(), false);
        page.setObject(eo);
        page.setNextPage(context().page());
        return (WOComponent) page;
    }

    public String classForTooltipTrigger() {
        String classForTooltipTrigger = "TooltipTrigger";
        if (hasHelpText()) {
            classForTooltipTrigger = classForTooltipTrigger + " TooltipTriggerWithText";
        }
        return classForTooltipTrigger;
    }

}
