package er.extensions.concurrency;

import com.webobjects.foundation.NSDictionary;

public interface IERXStatusTemplate {

	public String statusTemplateKey();
	
	public NSDictionary<String, String> statusTemplateFields();

}
