package er.modern.directtoweb.components.query;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOContext;
import com.webobjects.appserver.WOResponse;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eocontrol.EODataSource;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;

import er.ajax.AjaxUtils;
import er.directtoweb.components.ERDCustomQueryComponent;
import er.extensions.eof.ERXQ;
import er.extensions.foundation.ERXArrayUtilities;
import er.extensions.foundation.ERXStringUtilities;
import er.modern.directtoweb.delegates.ERMD2WAttributeQueryDelegate;
import er.modern.directtoweb.delegates.ERMD2WAttributeQueryDelegate.ERMD2WQueryComponent;

/**
 * An ajax search field for ad-hoc filtering of lists. Similar to
 * ERDAjaxSearchDisplayGroup, but enables filtering over multiple attributes.
 * 
 * Gets displayed when the searchKey D2W key is not null.
 * 
 * @d2wKey searchKey
 * @d2wKey minimumCharacterCount
 * 
 */
public class ERMD2WListFilter extends ERDCustomQueryComponent implements
        ERMD2WQueryComponent {

    private static final long serialVersionUID = 1L;
    
    public interface Keys extends ERDCustomQueryComponent.Keys {
        public static final String searchChoices = "searchChoices";
        public static final String searchChoicesDisplayKey = "searchChoicesDisplayKey";
        public static final String searchChoicesKey = "searchChoicesKey";
        public static final String searchChoicesRecursionKey = "searchChoicesRecursionKey";
        public static final String searchKey = "searchKey";
        public static final String typeAheadMinimumCharacterCount = "typeAheadMinimumCharacterCount";
    }

    private Object _searchChoice;
    private String _searchValue;

    public Object searchChoiceItem;

    public ERMD2WListFilter(WOContext context) {
        super(context);
    }

    @Override
    public boolean synchronizesVariablesWithBindings() {
        return false;
    }

    // actions
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WOActionResults search() {
        EODataSource dataSource = displayGroup().dataSource();
        EOQualifier _qualifier = ERMD2WAttributeQueryDelegate.instance
                .buildQualifier(this);
        
        String searchChoicesKey = (String) d2wContext().valueForKey(Keys.searchChoicesKey);
        if (!ERXStringUtilities.stringIsNullOrEmpty(searchChoicesKey)
                && searchChoice() != null) {
            if (d2wContext().valueForKey(Keys.searchChoicesRecursionKey) != null) {
                String recursionKey = (String) d2wContext()
                        .valueForKey(Keys.searchChoicesRecursionKey);
                NSMutableArray deepChoices = new NSMutableArray(searchChoice());
                deepChoices.addObjects(NSKeyValueCoding.Utility
                        .valueForKey(searchChoice(), recursionKey));
                _qualifier = ERXQ.and(_qualifier, ERXQ.in(searchChoicesKey, ERXArrayUtilities.flatten(deepChoices)));
            } else {
                _qualifier = ERXQ.and(_qualifier,
                        ERXQ.equals(searchChoicesKey, searchChoice()));
            }
        }
        
        ((EODatabaseDataSource) dataSource).setAuxiliaryQualifier(_qualifier);
        ((EODatabaseDataSource) displayGroup().dataSource()).fetchSpecification()
                .setUsesDistinct(true);
        displayGroup().fetch();
        displayGroup().setCurrentBatchIndex(1);
        return null;
    }

    public void appendToResponse(WOResponse response, WOContext context) {
        AjaxUtils.addScriptResourceInHead(context, response, "prototype.js");
        super.appendToResponse(response, context);
    }

    public void setSearchValue(String searchValue) {
        _searchValue = searchValue;
    }

    @Override
    public String searchValue() {
        return _searchValue;
    }

    public void setSearchChoice(Object searchChoice) {
        _searchChoice = searchChoice;
    }

    public Object searchChoice() {
        return _searchChoice;
    }
    
    public String searchChoicesDisplayString() {
        String displayKey  = (String) d2wContext().valueForKey(Keys.searchChoicesDisplayKey);
        return (String) NSKeyValueCoding.Utility.valueForKey(searchChoiceItem, displayKey);
    }

    @SuppressWarnings({ "rawtypes" })
    public NSArray searchChoices() {
        NSArray searchChoices = NSArray.emptyArray();
        if (d2wContext().valueForKey(Keys.searchChoices) != null) {
            String searchChoicesKey = (String) d2wContext()
                    .valueForKey(Keys.searchChoices);
            searchChoices = (NSArray) valueForKeyPath(searchChoicesKey);
        }
        return searchChoices;
    }

    @Override
    public EODataSource dataSource() {
        return displayGroup().dataSource();
    }

}