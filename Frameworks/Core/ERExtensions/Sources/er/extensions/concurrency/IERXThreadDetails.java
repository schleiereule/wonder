package er.extensions.concurrency;

import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;

public interface IERXThreadDetails {

	public NSArray<NSDictionary<String, Object>> threadDetails();

}
