package er.directtoweb.components.misc;

import com.webobjects.appserver.WOContext;

import er.directtoweb.components.ERDCustomEditComponent;
import er.taggable.ERTaggable;

public class ERD2WEditTags extends ERDCustomEditComponent {

    private static final long serialVersionUID = 1L;

    public ERD2WEditTags(WOContext aContext) {
        super(aContext);
    }

    public ERTaggable<?> taggable() {
        return  (ERTaggable<?>) object().valueForKey("taggable");
    }

}
