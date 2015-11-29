package er.modern.directtoweb.delegates.defaults;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.EditRelationshipPageInterface;
import com.webobjects.directtoweb.NextPageDelegate;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSKeyValueCoding;

import er.directtoweb.delegates.ERDBranchDelegate;
import er.directtoweb.delegates.ERDBranchInterface;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXValueUtilities;

public class ERMDDefaultPageActionDelegate extends ERDBranchDelegate {
	
	/**
	 * Do I need to update serialVersionUID?
	 * See section 5.6 <cite>Type Changes Affecting Serialization</cite> on page 51 of the 
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(ERMDDefaultPageActionDelegate.class);

	
	public void _createRelated(WOComponent sender) {
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if(erpi != null) {
			EOEnterpriseObject eo = (EOEnterpriseObject)NSKeyValueCoding.Utility.valueForKey(erpi, "masterObject");
			String relationshipKey = (String)NSKeyValueCoding.Utility.valueForKey(erpi, "relationshipKey");
			if(!ERXValueUtilities.isNull(eo) && !StringUtils.isBlank(relationshipKey)) {
				EOEditingContext nestedEC = ERXEC.newEditingContext(eo.editingContext());
				EOClassDescription relatedObjectClassDescription = eo.classDescriptionForDestinationKey(relationshipKey);
				EOEnterpriseObject relatedObject = (EOEnterpriseObject)EOUtilities.createAndInsertInstance(nestedEC, relatedObjectClassDescription.entityName());
				EOEnterpriseObject localObj = EOUtilities.localInstanceOfObject(relatedObject.editingContext(), eo);
				if (localObj instanceof ERXGenericRecord) {
					((ERXGenericRecord)localObj).setValidatedWhenNested(false);
				}
				localObj.addObjectToBothSidesOfRelationshipWithKey(relatedObject, relationshipKey);
				NSKeyValueCoding.Utility.takeValueForKey(erpi, relatedObject, "selectedObject");
				NSKeyValueCoding.Utility.takeValueForKey(erpi, "create", "inlineTaskSafely");
			}
		}
	}
	
	public void _editListRelated(WOComponent sender) {
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if(erpi != null) {
			NSKeyValueCoding.Utility.takeValueForKey(erpi, "editList", "inlineTaskSafely");
		}
	}
	
	public void _queryRelated(WOComponent sender) {
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if(erpi != null) {
			NSKeyValueCoding.Utility.takeValueForKey(erpi, "query", "inlineTaskSafely");
		}
	}
	
	public WOComponent _returnRelated(WOComponent sender) {
		WOComponent nextPage = null;
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if(erpi != null) {
			EOEnterpriseObject eo = (EOEnterpriseObject)NSKeyValueCoding.Utility.valueForKey(erpi, "masterObject");
			if(eo != null) {
				//eo should be in a non-validating nested ec so no exceptions should throw
				eo.editingContext().saveChanges();
			}
			if(erpi instanceof D2WPage) {
				nextPage = _nextPageFromDelegate((D2WPage)erpi);
			}
			if (nextPage == null && eo != null) {
				nextPage = (WOComponent)D2W.factory().editPageForEntityNamed(eo.entityName(), sender.session());
				((EditPageInterface)nextPage).setObject(eo);
			}
		} else {
			nextPage = _return(sender);
		}
		return nextPage;
	}
	
	public WOComponent _return(WOComponent sender) {
		D2WPage page = ERD2WUtilities.enclosingComponentOfClass(sender, D2WPage.class);
		WOComponent nextPage = _nextPageFromDelegate(page);
		if(nextPage == null) {
			nextPage = D2W.factory().defaultPage(sender.session());
		}
		return nextPage;
	}
	
	protected WOComponent _nextPageFromDelegate(D2WPage page) {
		WOComponent nextPage = null;
		NextPageDelegate delegate = page.nextPageDelegate();
		if(delegate != null) {
			if (!((delegate instanceof ERDBranchDelegate) && (((ERDBranchInterface)page).branchName() == null))) {
				/* 
				 * we assume here, because nextPage() in ERDBranchDelegate 
				 * is final, we can't do something reasonable when none of 
				 * the branch buttons was selected. This allows us to throw 
				 * a branch delegate at any page, even when no branch was 
				 * taken
				 */
				nextPage = delegate.nextPage(page);
			}
		}
		if(nextPage == null) {
			nextPage = page.nextPage();
		}
		return nextPage;
	}

}
