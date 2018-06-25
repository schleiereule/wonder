package er.modern.directtoweb.components;

import com.webobjects.appserver.WOContext;
import com.webobjects.directtoweb.D2WContext;

import er.ajax.AjaxFlickrBatchNavigation;
import er.ajax.AjaxUpdateContainer;
import er.modern.directtoweb.components.ERMDBatchSizeControl.Keys;

/**
 * D2W Batch navigation bar based on AjaxFlickrBatchNavigation
 * 
 * @author davidleber
 *
 */
public class ERMD2WBatchNavigationBar extends AjaxFlickrBatchNavigation {

	private D2WContext _d2wContext;

	public ERMD2WBatchNavigationBar(WOContext context) {
		super(context);
	}

	// ACCESSORS

	public D2WContext d2wContext() {
		if (_d2wContext == null) {
			_d2wContext = (D2WContext) valueForBinding("d2wContext");
		}
		return _d2wContext;
	}

	public void setD2wContext(D2WContext c) {
		_d2wContext = c;
	}

	public boolean hasHotkeys() {
		return d2wContext().valueForKey("batchNavNextHotkey") != null || d2wContext().valueForKey("batchNavPreviousHotkey") != null;
	}

	private String _updateContainerID;
	private String _batchIndexFieldID;

	/**
	 * Update container id for the displayGroup's list.
	 * <p>
	 * Defaults to the first parent update container id.
	 */
	public String updateContainerID() {
		if (_updateContainerID == null) {
			_updateContainerID = (String) valueForBinding(Keys.updateContainerID);
			if (_updateContainerID == null) {
				_updateContainerID = AjaxUpdateContainer.currentUpdateContainerID();
			}
		}
		return _updateContainerID;
	}

	/**
	 * Returns a unique id for this batch index control
	 */
	public String batchIndexFieldID() {
		if (_batchIndexFieldID == null) {
			_batchIndexFieldID = updateContainerID() + "_BIFID";
		}
		return _batchIndexFieldID;
	}

	public void setBatchIndexFieldID(String fieldID) {
		_batchIndexFieldID = fieldID;
	}

	public String updateFunction() {
		return "function(e) { " + updateContainerID() + "Update() }";
	}

	public void setCurrentBatchIndex(int batchNumber) {
		if (displayGroup().currentBatchIndex() != batchNumber) {
			displayGroup().setCurrentBatchIndex(batchNumber);
			AjaxUpdateContainer.updateContainerWithID(updateContainerID(), context());
		}
	}

}