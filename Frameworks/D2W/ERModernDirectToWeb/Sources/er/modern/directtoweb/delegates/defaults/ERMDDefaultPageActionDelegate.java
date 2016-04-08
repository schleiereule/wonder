package er.modern.directtoweb.delegates.defaults;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.EditRelationshipPageInterface;
import com.webobjects.directtoweb.InspectPageInterface;
import com.webobjects.directtoweb.NextPageDelegate;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eoaccess.EOGeneralAdaptorException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSValidation;

import er.ajax.AjaxUpdateContainer;
import er.directtoweb.delegates.ERDBranchDelegate;
import er.directtoweb.delegates.ERDBranchInterface;
import er.directtoweb.pages.ERD2WInspectPage;
import er.directtoweb.pages.ERD2WPage;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOAccessUtilities;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.localization.ERXLocalizer;

public class ERMDDefaultPageActionDelegate extends ERDBranchDelegate {

	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes
	 * Affecting Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object
	 * Serialization Spec</a>
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger log = Logger.getLogger(ERMDDefaultPageActionDelegate.class);

	public boolean shouldSaveChanges(D2WContext c) {
		return ERXValueUtilities.booleanValue(c.valueForKey("shouldSaveChanges"));
	}

	public boolean shouldValidateBeforeSave(D2WContext c) {
		return ERXValueUtilities.booleanValue(c.valueForKey("shouldValidateBeforeSave"));
	}

	public boolean shouldRecoverFromOptimisticLockingFailure(D2WContext c) {
		return ERXValueUtilities.booleanValueWithDefault(c.valueForKey("shouldRecoverFromOptimisticLockingFailure"), false);
	}

	public boolean shouldRevertUponSaveFailure(D2WContext c) {
		return ERXValueUtilities.booleanValueWithDefault(c.valueForKey("shouldRevertUponSaveFailure"), false);
	}

	public WOComponent _save(WOComponent sender) {
		WOComponent nextPage = sender.context().page();
		EOEnterpriseObject eo = object(sender);
		D2WContext c = d2wContext(sender);
		ERD2WPage page = (ERD2WPage) ERD2WUtilities.enclosingComponentOfClass(sender, InspectPageInterface.class);

		if (eo != null && eo.editingContext() == null) {
			page.setErrorMessage(ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("ERD2WInspect.alreadyAborted", c));
			page.clearValidationFailed();
			return nextPage;
		}

		if (!page.errorMessages().isEmpty()) {
			page.setErrorMessage(null);
			return nextPage;
		}

		if (eo == null) {
			return _nextPageFromDelegate(page);
		}

		boolean shouldRevert = false;
		EOEditingContext ec = eo.editingContext();
		ec.lock();
		try {
			if (shouldValidateBeforeSave(c)) {
				if (ec.insertedObjects().containsObject(eo)) {
					eo.validateForInsert();
				} else {
					eo.validateForUpdate();
				}
			}
			boolean hasChanges = ec.hasChanges();
			if (shouldSaveChanges(c) && hasChanges) {
				try {
					ec.saveChanges();
					nextPage = _nextPageFromDelegate(page);
					// Refresh object to update derived attributes
					ec.refreshObject(eo);
				} catch (RuntimeException e) {
					if (shouldRevertUponSaveFailure(c)) {
						shouldRevert = true;
					}
					throw e;
				}
			} else if (!hasChanges) {
				nextPage = _nextPageFromDelegate(page);
			}
		} catch (NSValidation.ValidationException e) {
			page.setErrorMessage(ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("CouldNotSave", e));
			page.validationFailedWithException(e, e.object(), "saveChangesExceptionKey");
		} catch (EOGeneralAdaptorException e) {
			if (ERXEOAccessUtilities.isOptimisticLockingFailure(e) && shouldRecoverFromOptimisticLockingFailure(c)) {
				EOEnterpriseObject obj = ERXEOAccessUtilities.refetchFailedObject(ec, e);
				page.setErrorMessage(ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("CouldNotSavePleaseReapply", c));
				page.validationFailedWithException(e, obj, "CouldNotSavePleaseReapply");
			} else {
				throw e;
			}
		} finally {
			try {
				if (shouldRevert) {
					ec.revert();
				}
			} finally {
				ec.unlock();
			}
		}
		//String id = (String) page.d2wContext().valueForKey("idForParentMainContainer");
		//AjaxUpdateContainer.updateContainerWithID(id, sender.context());
		return nextPage;
	}

	public WOComponent _cancelEdit(WOComponent sender) {
		EOEnterpriseObject eo = object(sender);
		ERD2WInspectPage page = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WInspectPage.class);
		//String id = (String) page.d2wContext().valueForKey("idForParentMainContainer");
		//AjaxUpdateContainer.updateContainerWithID(id, sender.context());
		EOEditingContext ec = eo != null ? eo.editingContext() : null;
		if (ec != null && page.shouldRevertChanges()) {
			ec.revert();
		}
		return page.nextPage(false);
	}

	public void _createRelated(WOComponent sender) {
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if (erpi != null) {
			EOEnterpriseObject eo = (EOEnterpriseObject) NSKeyValueCoding.Utility.valueForKey(erpi, "masterObject");
			String relationshipKey = (String) NSKeyValueCoding.Utility.valueForKey(erpi, "relationshipKey");
			if (!ERXValueUtilities.isNull(eo) && !StringUtils.isBlank(relationshipKey)) {
				EOEditingContext nestedEC = ERXEC.newEditingContext(eo.editingContext());
				EOClassDescription relatedObjectClassDescription = eo.classDescriptionForDestinationKey(relationshipKey);
				EOEnterpriseObject relatedObject = (EOEnterpriseObject) EOUtilities.createAndInsertInstance(nestedEC, relatedObjectClassDescription.entityName());
				EOEnterpriseObject localObj = EOUtilities.localInstanceOfObject(relatedObject.editingContext(), eo);
				if (localObj instanceof ERXGenericRecord) {
					((ERXGenericRecord) localObj).setValidatedWhenNested(false);
				}
				localObj.addObjectToBothSidesOfRelationshipWithKey(relatedObject, relationshipKey);
				NSKeyValueCoding.Utility.takeValueForKey(erpi, relatedObject, "selectedObject");
				NSKeyValueCoding.Utility.takeValueForKey(erpi, "create", "inlineTaskSafely");
			}
		}
	}

	public void _queryRelated(WOComponent sender) {
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if (erpi != null) {
			D2WContext c = (D2WContext) NSKeyValueCoding.Utility.valueForKey(erpi, "d2wContext");
			if (ERXValueUtilities.booleanValueWithDefault(c.valueForKey("shouldShowQueryPage"), true)) {
				NSKeyValueCoding.Utility.takeValueForKey(erpi, "query", "inlineTaskSafely");
			} else {
				// go straight to the select page
				EOEnterpriseObject eo = (EOEnterpriseObject) NSKeyValueCoding.Utility.valueForKey(erpi, "masterObject");
				EOEditingContext ec = eo.editingContext();
				String relationshipKey = (String) NSKeyValueCoding.Utility.valueForKey(erpi, "relationshipKey");
				if (!ERXValueUtilities.isNull(eo) && !StringUtils.isBlank(relationshipKey)) {
					String destinationEntityName = eo.classDescriptionForDestinationKey(relationshipKey).entityName();
					EODatabaseDataSource ds = new EODatabaseDataSource(ec, destinationEntityName);
					NSKeyValueCoding.Utility.takeValueForKey(erpi, ds, "selectDataSource");
					NSKeyValueCoding.Utility.takeValueForKey(erpi, "list", "inlineTaskSafely");
				}
			}
		}
	}

	public WOComponent _returnRelated(WOComponent sender) {
		WOComponent nextPage = null;
		EditRelationshipPageInterface erpi = ERD2WUtilities.enclosingComponentOfClass(sender, EditRelationshipPageInterface.class);
		if (erpi != null) {
			EOEnterpriseObject eo = (EOEnterpriseObject) NSKeyValueCoding.Utility.valueForKey(erpi, "masterObject");
			if (eo != null) {
				// eo should be in a non-validating nested ec so no exceptions
				// should throw
				eo.editingContext().saveChanges();
			}
			if (erpi instanceof D2WPage) {
				nextPage = _nextPageFromDelegate((D2WPage) erpi);
			}
			if (nextPage == null && eo != null) {
				nextPage = (WOComponent) D2W.factory().editPageForEntityNamed(eo.entityName(), sender.session());
				((EditPageInterface) nextPage).setObject(eo);
			}
		} else {
			nextPage = _return(sender);
		}
		return nextPage;
	}

	public WOComponent _return(WOComponent sender) {
		D2WPage page = ERD2WUtilities.enclosingComponentOfClass(sender, D2WPage.class);
		WOComponent nextPage = _nextPageFromDelegate(page);
		if (nextPage == null) {
			nextPage = D2W.factory().defaultPage(sender.session());
		}
		return nextPage;
	}

	protected WOComponent _nextPageFromDelegate(D2WPage page) {
		WOComponent nextPage = null;
		NextPageDelegate delegate = page.nextPageDelegate();
		if (delegate != null) {
			if (!((delegate instanceof ERDBranchDelegate) && (((ERDBranchInterface) page).branchName() == null))) {
				/*
				 * we assume here, because nextPage() in ERDBranchDelegate is
				 * final, we can't do something reasonable when none of the
				 * branch buttons was selected. This allows us to throw a branch
				 * delegate at any page, even when no branch was taken
				 */
				nextPage = delegate.nextPage(page);
			}
		}
		if (nextPage == null) {
			nextPage = page.nextPage();
		}
		return nextPage;
	}

}
