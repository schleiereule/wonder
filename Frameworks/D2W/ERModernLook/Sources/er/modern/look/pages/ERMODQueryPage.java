package er.modern.look.pages;

import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;

import er.directtoweb.pages.templates.ERD2WQueryPageTemplate;
import er.extensions.appserver.ERXResponseRewriter;

/**
 * Modernized query page.
 * 
 * @d2wKey headerComponentName
 * @d2wKey showListInSamePage
 * @d2wKey listConfigurationName
 * @d2wKey clearButtonLabel
 * @d2wKey findButtonLabel
 * @d2wKey returnButtonLabel
 * @d2wKey actionBarComponentName
 * @d2wKey controllerButtonComponentName 
 * 
 * @author davidleber
 */
public class ERMODQueryPage extends ERD2WQueryPageTemplate {
  /**
   * Do I need to update serialVersionUID?
   * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
   * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
   */
  private static final long serialVersionUID = 1L;
  
	public interface Keys extends ERD2WQueryPageTemplate.Keys {
		public static final String parentPageConfiguration = "parentPageConfiguration";
		public static final String useAjaxControlsWhenEmbedded = "useAjaxControlsWhenEmbedded";
		public static final String allowInlineEditing = "allowInlineEditing";
		public static final String shouldShowCancelButton = "shouldShowCancelButton";
	}
	
	public ERMODQueryPage(WOContext wocontext) {
		super(wocontext);
	}
	
    @Override
    public void appendToResponse(WOResponse response, WOContext context) {
        super.appendToResponse(response, context);
        // add scripts for application of hotkey bindings
        ERXResponseRewriter.addScriptResourceInHead(response, context, "ERCoolComponents",
                "hotkeys/hotkeys-min.js");
        // add script for hotkey and tab index application
        ERXResponseRewriter.addScriptResourceInHead(response, context, "ERModernDirectToWeb", "keyboard_nav.js");
    }

}
