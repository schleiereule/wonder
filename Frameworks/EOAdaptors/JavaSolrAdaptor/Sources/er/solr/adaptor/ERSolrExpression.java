package er.solr.adaptor;

import java.util.Enumeration;

import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eocontrol.EOAndQualifier;
import com.webobjects.eocontrol.EOKeyComparisonQualifier;
import com.webobjects.eocontrol.EOKeyValueQualifier;
import com.webobjects.eocontrol.EONotQualifier;
import com.webobjects.eocontrol.EOOrQualifier;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSTimestamp;
import com.webobjects.foundation.NSTimestampFormatter;

import er.extensions.formatters.ERXTimestampFormatter;
import er.extensions.foundation.ERXStringUtilities;

public class ERSolrExpression extends EOSQLExpression {

	public ERSolrExpression(EOEntity entity) {
		super(entity);
	}

	public static final String SOLR_EMPTY_QUALIFIER = "*:*";
	public static final String PARAMETER_EXCLUSION = "ex";
	public static final String PARAMETER_TAG = "tag";
	public static final String PARAMETER_KEY = "key";

	public static ERSolrExpression newERSolrExpression(EOEntity entity) {
		ERSolrExpression solrExpression = new ERSolrExpression(entity);
		solrExpression._entity = entity;
		return solrExpression;
	}

	public static StringBuilder escapeAndAppend(Object value, StringBuilder sb) {
		sb.append("\"");
		sb.append(ERXStringUtilities.escape(new char[] { '\"' }, '\\', String.valueOf(value)));
		sb.append("\"");
		return sb;
	}

	public static StringBuilder appendLocalParams(StringBuilder sb, NSDictionary<String, String> attributes) {
		if (attributes != null) {
			sb.append("{!");
			for (Enumeration e = attributes.allKeys().objectEnumerator(); e.hasMoreElements();) {
				String attributeKey = (String) e.nextElement();
				String attributeValue = (String) attributes.valueForKey(attributeKey);
				sb.append(attributeKey).append("=");
				escapeAndAppend(attributeValue, sb);
				if (e.hasMoreElements()) {
					sb.append(" ");
				}
			}
			sb.append("}");
		}
		return sb;
	}

	public String solrStringForQualifier(EOQualifier qualifier) {
		if (qualifier == null) {
			return SOLR_EMPTY_QUALIFIER;
		}
		if (qualifier instanceof EOKeyValueQualifier) {
			return solrStringForKeyValueQualifier((EOKeyValueQualifier) qualifier);
		} else if (qualifier instanceof EOAndQualifier) {
			return solrStringForArrayOfQualifiers(((EOAndQualifier) qualifier).qualifiers(), true);
		} else if (qualifier instanceof EOOrQualifier) {
			return solrStringForArrayOfQualifiers(((EOOrQualifier) qualifier).qualifiers(), false);
		} else if (qualifier instanceof EONotQualifier) {
			return solrStringForNegatedQualifier(qualifier);
		} else if (qualifier instanceof EOKeyComparisonQualifier) {
			return solrStringForKeyComparisonQualifier((EOKeyComparisonQualifier) qualifier);
		}

		return null;
	}

	protected String solrStringForKeyValueQualifier(EOKeyValueQualifier qualifier) {

		String solrString = null;

		String key = qualifier.key();
		String solrKey = null;

		Object value = qualifier.value();
		String solrValue = null;

		NSSelector selector = qualifier.selector();
		String solrSelector = null;

		// TODO: Handle key paths/relationships?
		EOAttribute attribute = _entity.attributeNamed(key);
		if (attribute != null)
			solrKey = attribute.columnName();
		if (solrKey == null)
			solrKey = key;

		// process null value
		if ((value == null) || (value.equals(NSKeyValueCoding.NullValue))) {
			return "(*:* NOT " + solrKey + ":*)";
		}

		// remove case insensitive like qualifier characters from value
		// set qualifier mode
		String caseInsensitiveLikeMode = null;
		if (selector.equals(EOQualifier.QualifierOperatorCaseInsensitiveLike)) {
			String stringValue = value.toString();
			int length = stringValue.length();
			boolean hasPrefix = stringValue.startsWith("*");
			boolean hasSuffix = stringValue.endsWith("*");
			if ((hasPrefix) & (hasSuffix)) {
				caseInsensitiveLikeMode = "contains";
				value = stringValue.substring(1, length - 1);
			} else {
				if (hasSuffix) {
					caseInsensitiveLikeMode = "startsWith";
					value = stringValue.substring(0, length - 1);
				}
				if (hasPrefix) {
					caseInsensitiveLikeMode = "endsWith";
					value = stringValue.substring(1);
				}
			}
		}

		// process value
		solrValue = solrStringForValue(value);

		// TODO: need to put solr in case insensitive mode
		if (selector.equals(EOQualifier.QualifierOperatorCaseInsensitiveLike)) {
			// solrValue = solrValue.toLowerCase();
			switch (caseInsensitiveLikeMode) {
			case "contains":
				solrValue = new StringBuilder("*").append(solrValue).append("*").toString();
				break;
			case "startsWith":
				solrValue = new StringBuilder(solrValue).append("*").toString();
				break;
			case "endsWith":
				solrValue = new StringBuilder("*").append(solrValue).toString();
				break;
			}
		}

		if (selector.equals(EOQualifier.QualifierOperatorLike)) {
			solrValue = new StringBuilder("*").append(solrValue).append("*").toString();
		}

		if (selector.equals(EOQualifier.QualifierOperatorContains)) {
			solrValue = new StringBuilder("*").append(solrValue).append("*").toString();
		}

		// process selector
		solrSelector = solrStringForSelector(selector, solrValue);

		boolean isRange = (selector.equals(EOQualifier.QualifierOperatorGreaterThan) || selector.equals(EOQualifier.QualifierOperatorGreaterThanOrEqualTo) || selector.equals(EOQualifier.QualifierOperatorLessThan)
				|| selector.equals(EOQualifier.QualifierOperatorLessThanOrEqualTo));
		boolean isNot = EOQualifier.QualifierOperatorNotEqual.equals(selector);

		if (isRange) {
			solrString = new StringBuilder(solrKey).append(":").append(solrSelector).toString();
		} else if (isNot) {
			solrString = new StringBuilder(solrSelector).append(" ").append(solrKey).append(":").append(solrValue).toString();
		} else {
			solrString = new StringBuilder(solrKey).append(":").append(solrSelector).append(solrValue).toString();
		}

		return solrString;
	}

	protected String solrStringForArrayOfQualifiers(NSArray<EOQualifier> qualifiers, boolean isConjoined) {
		StringBuilder solrStringBuilder = null;
		String operator = isConjoined ? " AND " : " OR ";
		boolean isAddingParens = false;

		for (EOQualifier qualifier : qualifiers) {
			String solrString = solrStringForQualifier(qualifier);
			if (solrString != null) {
				if (solrStringBuilder != null) {
					solrStringBuilder.append(operator);
					solrStringBuilder.append(solrString);
					isAddingParens = true;
				} else {
					solrStringBuilder = new StringBuilder(solrString);
				}
			}
		}

		if (isAddingParens) {
			solrStringBuilder.insert(0, '(');
			solrStringBuilder.append(')');
		}

		return solrStringBuilder.toString();
	}

	protected String solrStringForNegatedQualifier(EOQualifier qualifier) {
		// TODO
		return null;
	}

	protected String solrStringForKeyComparisonQualifier(EOKeyComparisonQualifier qualifier) {
		// TODO
		return null;
	}

	protected String solrStringForValue(Object value) {
		// complex values
		if (value instanceof NSArray) {
			NSMutableArray m = new NSMutableArray();
			NSArray a = (NSArray) value;
			for (Object o : a) {
				m.add(solrStringForValue(o));
			}
			return m.toString();
		}
		// primitve values
		String result = null;
		if (value instanceof NSTimestamp) {
			NSTimestamp timeStamp = (NSTimestamp) value;
			NSTimestampFormatter formatter = ERXTimestampFormatter.dateFormatterForPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
			result = formatter.format(timeStamp);
		}
		// default
		if (result == null) {
			result = value.toString();
		}
		// escape reserved chars in value
		result = result.replace("\\", "\\\\");
		NSArray<String> reservedCharacters = new NSArray<String>("+", "-", "&&", "||", "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", "*", "?", ":", " ");
		for (String reservedCharacter : reservedCharacters) {
			result = result.replace(reservedCharacter, "\\" + reservedCharacter);
		}
		return result;
	}

	protected String solrStringForSelector(NSSelector selector, String solrValue) {
		if (selector.equals(EOQualifier.QualifierOperatorEqual) || selector.equals(EOQualifier.QualifierOperatorContains) || selector.equals(EOQualifier.QualifierOperatorLike) || selector.equals(EOQualifier.QualifierOperatorCaseInsensitiveLike)) {
			return "";
		} else if (selector.equals(EOQualifier.QualifierOperatorNotEqual)) {
			return "NOT";
		} else if (selector.equals(EOQualifier.QualifierOperatorLessThan)) {
			return new StringBuilder("{* TO ").append(solrValue).append("}").toString();
		} else if (selector.equals(EOQualifier.QualifierOperatorGreaterThan)) {
			return new StringBuilder("{").append(solrValue).append(" TO *}").toString();
		} else if (selector.equals(EOQualifier.QualifierOperatorLessThanOrEqualTo)) {
			return new StringBuilder("[* TO ").append(solrValue).append("]").toString();
		} else if (selector.equals(EOQualifier.QualifierOperatorGreaterThanOrEqualTo)) {
			return new StringBuilder("[").append(solrValue).append(" TO *]").toString();
		}

		throw new IllegalStateException("solrStringForSelector:  Unknown operator: " + selector);
	}

	@Override
	public NSMutableDictionary<String, Object> bindVariableDictionaryForAttribute(EOAttribute arg0, Object arg1) {
		return new NSMutableDictionary<String, Object>();
	}

}
