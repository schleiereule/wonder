package er.modern.directtoweb.delegates.defaults;

import java.util.Enumeration;
import java.util.concurrent.Callable;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.webobjects.appserver.WOActionResults;
import com.webobjects.appserver.WOComponent;
import com.webobjects.directtoweb.ConfirmPageInterface;
import com.webobjects.directtoweb.D2W;
import com.webobjects.directtoweb.D2WContext;
import com.webobjects.directtoweb.D2WPage;
import com.webobjects.directtoweb.ERD2WUtilities;
import com.webobjects.directtoweb.EditPageInterface;
import com.webobjects.directtoweb.EditRelationshipPageInterface;
import com.webobjects.directtoweb.ErrorPageInterface;
import com.webobjects.directtoweb.InspectPageInterface;
import com.webobjects.directtoweb.ListPageInterface;
import com.webobjects.directtoweb.NextPageDelegate;
import com.webobjects.eoaccess.EODatabaseContext;
import com.webobjects.eoaccess.EODatabaseDataSource;
import com.webobjects.eoaccess.EODatabaseOperation;
import com.webobjects.eoaccess.EOGeneralAdaptorException;
import com.webobjects.eoaccess.EOObjectNotAvailableException;
import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOClassDescription;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.eocontrol.EOEnterpriseObject;
import com.webobjects.eocontrol.EOSharedEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSValidation;

import er.directtoweb.ERD2WContainer;
import er.directtoweb.delegates.ERDBranchDelegate;
import er.directtoweb.delegates.ERDBranchInterface;
import er.directtoweb.delegates.ERDPageDelegate;
import er.directtoweb.delegates.ERDQueryValidationDelegate;
import er.directtoweb.interfaces.ERDErrorPageInterface;
import er.directtoweb.interfaces.ERDObjectSaverInterface;
import er.directtoweb.pages.ERD2WInspectPage;
import er.directtoweb.pages.ERD2WPage;
import er.directtoweb.pages.ERD2WPickListPage;
import er.directtoweb.pages.ERD2WQueryPage;
import er.directtoweb.pages.ERD2WWizardCreationPage;
import er.directtoweb.pages.templates.ERD2WWizardCreationPageTemplate;
import er.extensions.appserver.ERXSession;
import er.extensions.concurrency.ERXDeleteEnterpriseObjectsTask;
import er.extensions.concurrency.ERXExecutorService;
import er.extensions.concurrency.ERXTaskResult;
import er.extensions.eof.ERXEC;
import er.extensions.eof.ERXEOAccessUtilities;
import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.eof.ERXGenericRecord;
import er.extensions.foundation.ERXArrayUtilities;
import er.extensions.foundation.ERXStringUtilities;
import er.extensions.foundation.ERXValueUtilities;
import er.extensions.localization.ERXLocalizer;
import er.extensions.validation.ERXValidationException;
import er.extensions.validation.ERXValidationFactory;
import er.modern.directtoweb.ERMDNotificationNameRegistry;
import er.modern.directtoweb.components.ERMDListThreadPage;
import er.modern.directtoweb.components.ERMDLongResponsePage;
import er.modern.directtoweb.delegates.ERMD2WConfirmCancellationDelegate;
import er.modern.directtoweb.delegates.ERMDDeleteEnterpriseObjectController;

public class ERMDDefaultPageActionDelegate extends ERDBranchDelegate {

	/**
	 * Do I need to update serialVersionUID? See section 5.6 <cite>Type Changes Affecting
	 * Serialization</cite> on page 51 of the
	 * <a href="http://java.sun.com/j2se/1.4/pdf/serial-spec.pdf">Java Object Serialization Spec</a>
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

	@D2WDelegate(requiresFormSubmit = true, group = "zGroup")
	public WOComponent _edit(WOComponent sender) {
		EOEnterpriseObject obj = object(sender);
		if (obj == null) {
			return sender.context().page();
		}
		D2WContext d2wContext = d2wContext(sender);
		boolean useAjax = ERXValueUtilities.booleanValue(d2wContext.valueForKey("useAjaxControlsWhenEmbedded"));
		if (useAjax) {
			EOEditingContext ec = ERXEC.newEditingContext(obj.editingContext());
			EOEnterpriseObject localObj = ERXEOControlUtilities.localInstanceOfObject(ec, obj);
			d2wContext.takeValueForKey(localObj, "objectBeingEdited");

			// for ERMODInspectPage
			d2wContext.takeValueForKey((String) d2wContext.valueForKey("pageConfiguration"), "previousPageConfiguration");
			d2wContext.takeValueForKey((String) d2wContext.valueForKey("task"), "previousTask");

			d2wContext.takeValueForKey("edit", "inlineTask");
			String newConfig = (String) d2wContext.valueForKey("inlinePageConfiguration");
			d2wContext.takeValueForKey(newConfig, "pageConfiguration");
			d2wContext.takeValueForKey("edit", "task");
			return null;
		} else {
			Object value = d2wContext.valueForKey("useNestedEditingContext");
			boolean createNestedContext = ERXValueUtilities.booleanValue(value);
			EOEnterpriseObject eo = ERXEOControlUtilities.editableInstanceOfObject(obj, createNestedContext);
			EditPageInterface epi = D2W.factory().editPageForEntityNamed(eo.entityName(), sender.session());
			epi.setObject(eo);
			epi.setNextPage(sender.context().page());
			eo.editingContext().hasChanges(); // Ensuring it survives.
			return (WOComponent) epi;
		}
	}

	@D2WDelegate(requiresFormSubmit = true, group = "yGroup")
	public void _nextStep(WOComponent sender) {
		ERXSession.session().notificationCenter().postNotification(ERD2WWizardCreationPage.WILL_GOTO_NEXT_PAGE, null);
		ERD2WWizardCreationPageTemplate page = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WWizardCreationPageTemplate.class);
		NSArray<ERD2WContainer> tabs = page.tabSectionsContents();
		ERD2WContainer tab = page.currentTab();
		int index = tabs.indexOf(tab);
		if (page.errorMessages().isEmpty() && index + 1 < tabs.count()) {
			ERD2WContainer next = tabs.objectAtIndex(index + 1);
			page.setCurrentTab(next);
			page.nextStep();
		}
		NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
		// if the parent page configuration ID is available, add it
		if (d2wContext(sender).valueForKey("parentPageConfigurationID") != null) {
			userInfo.put("parentPageConfigurationID", d2wContext(sender).valueForKey("parentPageConfigurationID"));
		}
		userInfo.put("pageConfiguration", d2wContext(sender).valueForKey("pageConfiguration"));
		ERXSession.session().notificationCenter().postNotification(ERMDNotificationNameRegistry.BUTTON_PERFORMED_NEXT_STEP_ACTION, null, userInfo);
	}

	@D2WDelegate(requiresFormSubmit = true, group = "xGroup")
	public void _prevStep(WOComponent sender) {
		ERD2WWizardCreationPageTemplate page = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WWizardCreationPageTemplate.class);
		NSArray<ERD2WContainer> tabs = page.tabSectionsContents();
		ERD2WContainer tab = page.currentTab();
		int index = tabs.indexOf(tab);
		if (index != 0) {
			page.clearValidationFailed();
			ERD2WContainer prev = tabs.objectAtIndex(index - 1);
			page.setCurrentTab(prev);
			page.previousStep();
		}
		NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
		// if the parent page configuration ID is available, add it
		if (d2wContext(sender).valueForKey("parentPageConfigurationID") != null) {
			userInfo.put("parentPageConfigurationID", d2wContext(sender).valueForKey("parentPageConfigurationID"));
		}
		userInfo.put("pageConfiguration", d2wContext(sender).valueForKey("pageConfiguration"));
		ERXSession.session().notificationCenter().postNotification(ERMDNotificationNameRegistry.BUTTON_PERFORMED_PREVIOUS_STEP_ACTION, null, userInfo);
	}

	@D2WDelegate(requiresFormSubmit = true, group = "zGroup")
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
					// play nice with the ERDObjectSaverInterface by informing
					// it that the object was saved
					ERDObjectSaverInterface osi = (ERDObjectSaverInterface) ERD2WUtilities.enclosingComponentOfClass(sender, ERDObjectSaverInterface.class);
					if (osi != null) {
						osi.setObjectWasSaved(true);
					}
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
			NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
			// if the parent page configuration ID is available, add it
			if (c.valueForKey("parentPageConfigurationID") != null) {
				userInfo.put("parentPageConfigurationID", c.valueForKey("parentPageConfigurationID"));
			}
			userInfo.put("pageConfiguration", c.valueForKey("pageConfiguration"));
			userInfo.put("newObject", eo);
			ERXSession.session().notificationCenter().postNotification(ERMDNotificationNameRegistry.BUTTON_PERFORMED_SAVE_ACTION, null, userInfo);
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
		return nextPage;
	}

	@D2WDelegate(requiresFormSubmit = false, group = "wGroup")
	public WOComponent _cancelEdit(WOComponent sender) {
		D2WContext c = d2wContext(sender);
		EOEnterpriseObject eo = object(sender);
		if (ERXValueUtilities.booleanValueWithDefault(c.valueForKey("showConfirmPageOnCancel"), false)) {
			// implement confirm page for cancellation
			ERD2WWizardCreationPage wizardPage = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WWizardCreationPage.class);
			if (wizardPage != null) {
				// only show this if we've been through more than one page
				if (wizardPage.currentStep() > 1 && ERXEOControlUtilities.isNewObject(wizardPage.object())) {
					ConfirmPageInterface cpi = (ConfirmPageInterface) D2W.factory().pageForConfigurationNamed("ConfirmCancelCreationOf" + wizardPage.entityName(), sender.session());
					cpi.setCancelDelegate(new ERDPageDelegate(sender.context().page()));
					cpi.setConfirmDelegate(new ERMD2WConfirmCancellationDelegate(sender));
					cpi.setMessage((String) c.valueForKey("cancelMessage"));
					if (cpi instanceof InspectPageInterface) {
						((InspectPageInterface) cpi).setObject(wizardPage.object());
					}
					return (WOComponent) cpi;
				}
			}
		}
		ERD2WInspectPage page = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WInspectPage.class);
		EOEditingContext ec = eo != null ? eo.editingContext() : null;
		if (ec != null && page.shouldRevertChanges()) {
			ec.revert();
		}
		NSMutableDictionary<String, Object> userInfo = new NSMutableDictionary<String, Object>();
		// if the parent page configuration ID is available, add it
		if (c.valueForKey("parentPageConfigurationID") != null) {
			userInfo.put("parentPageConfigurationID", c.valueForKey("parentPageConfigurationID"));
		}
		userInfo.put("pageConfiguration", c.valueForKey("pageConfiguration"));
		ERXSession.session().notificationCenter().postNotification(ERMDNotificationNameRegistry.BUTTON_PERFORMED_CANCEL_EDIT_ACTION, null, userInfo);
		return page.nextPage(false);
	}

	@D2WDelegate(requiresFormSubmit = false)
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

	@D2WDelegate(requiresFormSubmit = false)
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

	@D2WDelegate(requiresFormSubmit = true)
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

	@D2WDelegate(requiresFormSubmit = true)
	public WOComponent _query(WOComponent sender) {
		ERD2WQueryPage page = ERD2WUtilities.enclosingComponentOfClass(sender, ERD2WQueryPage.class);
		WOComponent nextPage = null;

		// If we have a validation delegate, validate the query values before actually performing
		// the query.
		ERDQueryValidationDelegate queryValidationDelegate = page.queryValidationDelegate();
		if (queryValidationDelegate != null) {
			// page.clearValidationFailed();
			// page.setErrorMessage(null);
			try {
				queryValidationDelegate.validateQuery(page);
			} catch (NSValidation.ValidationException ex) {
				page.setErrorMessage(ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("CouldNotQuery", ex));
				page.validationFailedWithException(ex, null, "queryExceptionKey");
			}
		}

		if (page.hasErrors()) {
			return sender.context().page();
		}

		D2WContext c = d2wContext(sender);
		if (ERXValueUtilities.booleanValue(c.valueForKey("showListInSamePage"))) {
			page.setShowResults(true);
		} else {
			nextPage = _nextPageFromDelegate(page);
			boolean allowInlineEditing = ERXValueUtilities.booleanValue(c.valueForKey("allowInlineEditing"));
			if (nextPage == null && !allowInlineEditing) {
				String listConfigurationName = (String) c.valueForKey("listConfigurationName");
				ListPageInterface listpageinterface;
				if (listConfigurationName != null) {
					listpageinterface = (ListPageInterface) D2W.factory().pageForConfigurationNamed(listConfigurationName, sender.session());
				} else {
					listpageinterface = D2W.factory().listPageForEntityNamed(c.entity().name(), sender.session());
				}
				listpageinterface.setDataSource(page.queryDataSource());
				listpageinterface.setNextPage(sender.context().page());
				nextPage = (WOComponent) listpageinterface;
			}
		}
		return nextPage;
	}

	@D2WDelegate(requiresFormSubmit = true, group = "aGroup")
	public WOComponent _return(WOComponent sender) {
		D2WPage page = ERD2WUtilities.enclosingComponentOfClass(sender, D2WPage.class);
		WOComponent nextPage = _nextPageFromDelegate(page);
		if (nextPage == null) {
			nextPage = D2W.factory().defaultPage(sender.session());
		}
		return nextPage;
	}

	@D2WDelegate(requiresFormSubmit = true)
	public WOComponent _delete(WOComponent sender) {
		return _deleteObject(sender, sender.context().page());
	}

	@D2WDelegate(requiresFormSubmit = true)
	public WOComponent _deleteReturn(WOComponent sender) {
		WOComponent nextPage = _deleteObject(sender, _return(sender));
		return nextPage;
	}

	@D2WDelegate(requiresFormSubmit = true)
	public WOComponent _clear(WOComponent sender) {
		D2WPage page = ERD2WUtilities.enclosingComponentOfClass(sender, D2WPage.class);
		if (page instanceof ERD2WQueryPage) {
			((ERD2WQueryPage) page).clearAction();
		}
		return sender.context().page();
	}

	protected WOComponent _deleteObject(WOComponent sender, WOComponent nextPage) {
		EOEnterpriseObject eo = object(sender);
		if (eo != null && eo.editingContext() != null) {
			EOEditingContext ec = eo.editingContext();
			NSValidation.ValidationException exception = null;

			try {
				if (ec instanceof EOSharedEditingContext) {
					EOEditingContext ec2 = ERXEC.newEditingContext();
					ec2.lock();
					try {
						ec2.setSharedEditingContext(null);
						eo = ERXEOControlUtilities.localInstanceOfObject(ec2, eo);
						ec2.deleteObject(eo);
						ec2.saveChanges();
					} finally {
						ec2.unlock();
						ec2.dispose();
					}
				} else {
					/*
					 * Delete the object in a nested ec first to prevent the appearance of deletion
					 * from display groups when if validation fails
					 */
					EOEnterpriseObject obj = ERXEOControlUtilities.editableInstanceOfObject(eo, true);
					EOEditingContext childEC = obj.editingContext();
					childEC.deleteObject(obj);
					childEC.saveChanges();

					if (ERXEOControlUtilities.isNewObject(eo)) {
						ec.processRecentChanges();
					} else {
						ec.saveChanges();
					}
				}
			} catch (EOObjectNotAvailableException e) {
				exception = ERXValidationFactory.defaultFactory().createCustomException(eo, "EOObjectNotAvailableException");
			} catch (EOGeneralAdaptorException e) {
				NSDictionary<?, ?> userInfo = e.userInfo();
				if (userInfo != null) {
					EODatabaseOperation op = (EODatabaseOperation) userInfo.objectForKey(EODatabaseContext.FailedDatabaseOperationKey);
					if (op.databaseOperator() == EODatabaseOperation.DatabaseDeleteOperator) {
						exception = ERXValidationFactory.defaultFactory().createCustomException(eo, "EOObjectNotAvailableException");
					}
				}
				if (exception == null) {
					exception = ERXValidationFactory.defaultFactory().createCustomException(eo, "Database error: " + e.getMessage());
				}
			} catch (NSValidation.ValidationException e) {
				exception = e;
			}
			if (exception != null) {
				if (exception instanceof ERXValidationException) {
					ERXValidationException ex = (ERXValidationException) exception;
					D2WContext c = d2wContext(sender);
					ex.setContext(c);

					Object o = ex.object();
					if (o instanceof EOEnterpriseObject) {
						EOEnterpriseObject obj = (EOEnterpriseObject) o;
						c.takeValueForKey(obj.entityName(), "entityName");
						c.takeValueForKey(ex.propertyKey(), "propertyKey");
					}
				}
				if (log.isDebugEnabled()) {
					log.debug("Validation Exception: " + exception.getMessage(), exception);
				}
				ec.revert();
				String errorMessage = ERXLocalizer.currentLocalizer().localizedTemplateStringForKeyWithObject("CouldNotSave", exception);
				ErrorPageInterface epf = D2W.factory().errorPage(sender.session());
				if (epf instanceof ERDErrorPageInterface) {
					ERDErrorPageInterface err = (ERDErrorPageInterface) epf;
					err.setException(exception);
				}
				epf.setMessage(errorMessage);
				epf.setNextPage(nextPage);
				return (WOComponent) epf;
			}
		}
		return nextPage;
	}

	protected WOComponent _nextPageFromDelegate(D2WPage page) {
		WOComponent nextPage = null;
		NextPageDelegate delegate = page.nextPageDelegate();
		if (delegate != null) {
			if (!((delegate instanceof ERDBranchDelegate) && (((ERDBranchInterface) page).branchName() == null))) {
				/*
				 * we assume here, because nextPage() in ERDBranchDelegate is final, we can't do
				 * something reasonable when none of the branch buttons was selected. This allows us
				 * to throw a branch delegate at any page, even when no branch was taken
				 */
				nextPage = delegate.nextPage(page);
			}
		}
		if (nextPage == null) {
			nextPage = page.nextPage();
		}
		return nextPage;
	}
	
	@D2WDelegate(requiresFormSubmit = false, group = "lGroup")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WOComponent _selectAll(WOComponent sender) {
        WOComponent page = sender.context().page();
        if (page instanceof ERD2WPickListPage) {
            ERD2WPickListPage pickPage = (ERD2WPickListPage) page;
            NSMutableArray selectedObjects = new NSMutableArray();
            NSArray list = pickPage.filteredObjects();
            for (Enumeration e = list.objectEnumerator(); e.hasMoreElements();) {
                selectedObjects.addObject(e.nextElement());
            }
            pickPage.setSelectedObjects(selectedObjects);
        }
        return page;
    }

	@D2WDelegate(requiresFormSubmit = false, group = "lGroup")
    public WOComponent _selectNone(WOComponent sender) {
        WOComponent page = sender.context().page();
        if (page instanceof ERD2WPickListPage) {
            ERD2WPickListPage pickPage = (ERD2WPickListPage) page;
            pickPage.setSelectedObjects(NSMutableArray.EmptyArray);
        }
        return page;
    }

	@D2WDelegate(requiresFormSubmit = false, group = "lGroup")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WOComponent _selectAllOnPage(WOComponent sender) {
        WOComponent page = sender.context().page();
        if (page instanceof ERD2WPickListPage) {
            ERD2WPickListPage pickPage = (ERD2WPickListPage) page;
            NSMutableArray selectedObjects = pickPage.selectedObjects().mutableClone();
            NSArray list = pickPage.displayGroup().displayedObjects();
            for (Enumeration e = list.objectEnumerator(); e.hasMoreElements();) {
                selectedObjects.addObject(e.nextElement());
            }
            pickPage.setSelectedObjects(selectedObjects);
        }
        return page;
    }

	@D2WDelegate(requiresFormSubmit = false, group = "lGroup")
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public WOComponent _selectNoneOnPage(WOComponent sender) {
        WOComponent page = sender.context().page();
        if (page instanceof ERD2WPickListPage) {
            ERD2WPickListPage pickPage = (ERD2WPickListPage) page;
            NSArray selectedObjects = ERXArrayUtilities.arrayMinusArray(pickPage.selectedObjects(), pickPage.displayGroup().displayedObjects());
            pickPage.setSelectedObjects(selectedObjects);
        }
        return page;
    }
	

 public WOActionResults _batchDelete(WOComponent sender) {
        WOActionResults nextPage = null;
        if (sender.context().page() instanceof ERD2WPickListPage) {
            ERD2WPickListPage pickPage = (ERD2WPickListPage) sender.context().page();
            @SuppressWarnings("unchecked")
            NSArray<EOEnterpriseObject> selectedObjects = pickPage.selectedObjects();
            if (!selectedObjects.isEmpty()) {
                if (pageDelete(sender)) {
                    // delete is to be handled in the page context
                    EOEditingContext ec = selectedObjects.lastObject().editingContext();
                    try {
                        for (EOEnterpriseObject eo : selectedObjects) {
                            ec.deleteObject(eo);
                        }
                        ec.saveChanges();
                        // make sure the display group shows a batch that exists
                        // after deletions
                        if (pickPage.displayGroup().currentBatchIndex() > pickPage.displayGroup().batchCount()) {
                            pickPage.displayGroup().setCurrentBatchIndex(pickPage.displayGroup().batchCount());
                        }
                    } catch (ERXValidationException e) {
                        log.warn("Failed to delete EO: " + e.object(), e);
                    }
                    nextPage = sender.context().page();
                } else {
                    // delete is to be handled via a background task
                    ERMDDeleteEnterpriseObjectController resultController = new ERMDDeleteEnterpriseObjectController();
                    // TODO this may not work when the PickList is embedded
                    WOComponent senderPage = ERD2WUtilities.enclosingPageOfClass(sender, D2WPage.class);
                    resultController.setObjectPage(senderPage);
                    resultController.setSenderPage(resultController.objectPage());

                    Callable<ERXTaskResult> task = ERXDeleteEnterpriseObjectsTask.getInstance(selectedObjects);

                    switch (deleteMode(sender)) {
                    case "LongResponse":
                        ERMDLongResponsePage longResponsePage = (ERMDLongResponsePage) D2W.factory().pageForConfigurationNamed("LongResponsePage",
                                sender.context().session());
                        longResponsePage.setLongRunningTask(task);
                        longResponsePage.setNextPageForResultController(resultController);
                        longResponsePage.setAutoFireNextPageAction(true);
                        nextPage = longResponsePage;
                        break;
                    case "Monitor":
                        ERXExecutorService.executorService().submit(task);
                        ERMDListThreadPage listThreadPage = (ERMDListThreadPage) D2W.factory().pageForConfigurationNamed("ListThread",
                                sender.context().session());
                        listThreadPage.setNextPageForResultController(resultController);
                        nextPage = listThreadPage;
                        break;
                    }
                }
            }
        }
        return nextPage;
 }

 private String deleteMode(WOComponent sender) {
     String deleteMode = (String) d2wContext(sender).valueForKey("deleteMode");
     if (ERXStringUtilities.isBlank(deleteMode)) {
         deleteMode = "Page";
     }
     return deleteMode;
 }

 public Boolean pageDelete(WOComponent sender) {
     if (deleteMode(sender).equals("Page")) {
         return true;
     } else {
         return false;
     }
 }


}
