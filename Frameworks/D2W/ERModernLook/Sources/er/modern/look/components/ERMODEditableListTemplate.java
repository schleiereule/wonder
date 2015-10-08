package er.modern.look.components;

import com.webobjects.appserver.WOComponent;
import com.webobjects.appserver.WOContext;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.pages.ERD2WEditableListPage;
import er.extensions.foundation.ERXStringUtilities;

/**
 * List page for editing all items in the list.<br />
 * See {@link ERD2WEditableListPage}
 * 
 * @binding backAction
 * @binding dataSource
 * @binding defaultSortKey
 * @binding isEntityInspectable
 * @binding isEntityReadOnly
 * @binding isListEmpty
 * @binding isSelecting
 * @binding listSize
 * @binding nextPage
 * @binding object
 * @binding showCancel
 * @d2wKey formEncoding
 * @d2wKey emptyListComponentName
 * @d2wKey repetitionComponentName
 * @d2wKey backgroundColorForTable
 * @d2wKey displayNameForEntity
 * @d2wKey textColor
 * @d2wKey pageWrapperName
 * @d2wKey showBanner
 */
public class ERMODEditableListTemplate extends ERD2WEditableListPage {
    /**
     * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
     * Affecting Serialization</cite> on page 51 of the <a
     * href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
     * Serialization Spec</a>
     */
    private static final long serialVersionUID = 1L;

    public ERMODEditableListTemplate(WOContext context) {
        super(context);
    }

    /* 
     * Overridden to make it do the right thing when embedded.
     */
    public WOComponent cancel(){
        super.cancel();
        return nextPage();
    }
    
    public String idForBottomActionBlock() {
        String idForBottomActionBlock = (String) d2wContext().valueForKey(
                "idForRepetitionContainer");
        if (!ERXStringUtilities.stringIsNullOrEmpty(idForBottomActionBlock)) {
            idForBottomActionBlock = "BAB"
                    + idForBottomActionBlock
                            .substring(3, idForBottomActionBlock.length());
        }
        // use data source hash to generate globally unique ID 
        idForBottomActionBlock = idForBottomActionBlock.concat("_"
                + String.valueOf(Math.abs(dataSource().hashCode())));
        return idForBottomActionBlock;
    }

    public void updateBottomActionBlock() {
        // force update of bottom action block to keep controller button
        // functional
        AjaxUpdateContainer
                .safeUpdateContainerWithID(idForBottomActionBlock(), context());
    }
   
}
