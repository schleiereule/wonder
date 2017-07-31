package er.directtoweb.components.buttons;

import com.webobjects.appserver.WOContext;

import er.directtoweb.components.ERDCustomComponent;

/**
 * Simple combo of ERDControllerButton and WOImage. If the D2W context has a
 * non-null iconName key, the image component is rendered, otherwise it's
 * omitted. To set the iconName key, implement a method "userPresentableIcon()"
 * on your EO, returning the filename of the icon to display. Then use two rules
 * like these:
 * 
 * <pre>
 * 100 : entity.name = 'MyEntityWithIcons' => iconName = "<ERDDelayedIconAssignment>" [er.directtoweb.assignments.delayed.ERDDelayedIconAssignment]
 * 100 : *true* => iconFrameworkName = "MyIconFramework" [com.webobjects.directtoweb.Assignment]
 * </pre>
 * 
 * You can of course implement your own assignment to handle this differently.
 * 
 * @author fpeters
 */
public class ERDControllerButtonIconCombo extends ERDCustomComponent {

    private static final long serialVersionUID = 1L;

    public ERDControllerButtonIconCombo(WOContext context) {
        super(context);
    }

    @Override
    public final boolean synchronizesVariablesWithBindings() {
        return false;
    }

    @Override
    public final boolean isStateless() {
        return true;
    }

}
