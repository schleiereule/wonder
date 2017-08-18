package er.coolcomponents;

import com.webobjects.appserver.WOContext;

import er.extensions.appserver.ERXBrowser;
import er.extensions.appserver.ERXSession;
import er.extensions.components.ERXStatelessComponent;

public class CCTooltips extends ERXStatelessComponent {

    private static final long serialVersionUID = 1L;

    public CCTooltips(WOContext aContext) {
        super(aContext);
    }

    public boolean isPolyfillRequired() {
        boolean isPolyfillRequired = false;
        ERXBrowser browser = ERXSession.session().browser();
        if (browser.isIE()) {
            if (browser.isVersion7() || browser.isVersion8() || browser.isVersion9()
                    || browser.isVersion10()) {
                isPolyfillRequired = true;
            }
        }
        return isPolyfillRequired;
    }

}
