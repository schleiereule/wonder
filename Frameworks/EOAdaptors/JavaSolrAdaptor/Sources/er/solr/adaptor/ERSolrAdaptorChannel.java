package er.solr.adaptor;

import java.math.BigInteger;
import java.net.MalformedURLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.FacetParams;

import com.ibm.icu.math.BigDecimal;
import com.webobjects.eoaccess.EOAdaptorChannel;
import com.webobjects.eoaccess.EOAdaptorContext;
import com.webobjects.eoaccess.EOAttribute;
import com.webobjects.eoaccess.EOEntity;
import com.webobjects.eoaccess.EOGeneralAdaptorException;
import com.webobjects.eoaccess.EOModel;
import com.webobjects.eoaccess.EOSQLExpression;
import com.webobjects.eoaccess.EOStoredProcedure;
import com.webobjects.eocontrol.EOFetchSpecification;
import com.webobjects.eocontrol.EOQualifier;
import com.webobjects.eocontrol.EOSortOrdering;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSForwardException;
import com.webobjects.foundation.NSKeyValueCoding;
import com.webobjects.foundation.NSMutableArray;
import com.webobjects.foundation.NSMutableDictionary;
import com.webobjects.foundation.NSPropertyListSerialization;
import com.webobjects.foundation.NSSelector;
import com.webobjects.foundation.NSTimestamp;

import er.extensions.eof.ERXEOControlUtilities;
import er.extensions.foundation.ERXMutableURL;
import er.extensions.foundation.ERXStringUtilities;
import er.solr.ERXSolrFetchSpecification;
import er.solr.SolrFacet;
import er.solr.SolrFacet.FacetItem;

public class ERSolrAdaptorChannel extends EOAdaptorChannel {

	private NSArray<EOAttribute> _attributes;
	private NSMutableArray<NSMutableDictionary<String, Object>> _fetchedRows;
	private int _fetchIndex;
	private boolean _open;

	private EOEntity _fetchedEntity;

	private static final Logger log = Logger.getLogger(ERSolrAdaptorChannel.class);

	public ERSolrAdaptorChannel(EOAdaptorContext context) {
		super(context);
		_fetchIndex = -1;
	}

	public ERSolrAdaptorContext context() {
		return (ERSolrAdaptorContext) _context;
	}

	@Override
	public void cancelFetch() {
		_fetchedRows = null;
		_fetchIndex = -1;
	}

	@Override
	public void closeChannel() {
		_open = false;
	}

	@Override
	public int deleteRowsDescribedByQualifier(EOQualifier qualifier, EOEntity entity) {
		if (qualifier == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.deleteRowsDescribedByQualifier: null qualifier.");
		}
		if (entity == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.deleteRowsDescribedByQualifier: null entity.");
		}

		int counter = 0;

		try (SolrClient solrServer = getSolrClient(entity)) {

			String idField = entity.primaryKeyAttributeNames().get(0);

			ERSolrExpression solrExpression = ERSolrExpression.newERSolrExpression(entity);
			String solrQueryString = solrExpression.solrStringForQualifier(qualifier);
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery(solrQueryString);
			solrQuery.setRows(Integer.MAX_VALUE);

			QueryResponse queryResponse = solrServer.query(solrQuery);
			for (SolrDocument solrDoc : queryResponse.getResults()) {
				solrServer.deleteById((String) solrDoc.getFirstValue(idField));
				solrServer.commit();
				counter++;
			}

		} catch (Exception e) {
			log.error(e);
			throw new EOGeneralAdaptorException("Failed to delete '" + entity.name() + "': " + e.getMessage());
		}

		return counter;

	}

	@Override
	public NSArray describeTableNames() {
		return NSArray.EmptyArray;
	}

	@Override
	public EOModel describeModelWithTableNames(NSArray anArray) {
		return null;
	}

	@Override
	public void evaluateExpression(EOSQLExpression arg0) {
		throw new UnsupportedOperationException("ERSolrAdaptorChannel.evaluateExpression");
	}

	@Override
	public void executeStoredProcedure(EOStoredProcedure arg0, NSDictionary arg1) {
		throw new UnsupportedOperationException("ERSolrAdaptorChannel.executeStoredProcedure");
	}

	@Override
	public NSMutableDictionary<String, Object> fetchRow() {
		NSMutableDictionary<String, Object> row = null;
		if (_fetchedRows != null && _fetchIndex < _fetchedRows.count()) {

			row = _fetchedRows.objectAtIndex(_fetchIndex++);

			NSMutableDictionary<String, Object> snapShot = new NSMutableDictionary<>();
			for (EOAttribute att : _fetchedEntity.attributes()) {
				Object val = row.get(att.name());
				snapShot.put(att.name(), val != null ? val : NSKeyValueCoding.NullValue);
			}

			return snapShot;

		}
		return row;
	}

	@Override
	public NSDictionary<String, Object> primaryKeyForNewRowWithEntity(EOEntity entity) {
		return new NSDictionary<String, Object>(UUID.randomUUID().toString(), "id");
	}

	@Override
	public void insertRow(NSDictionary<String, Object> row, EOEntity entity) {

		if (row == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.insertRow: null row.");
		}

		if (entity == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.insertRow: null entity.");
		}

		try (SolrClient solrServer = getSolrClient(entity)) {

			SolrInputDocument doc = new SolrInputDocument();

			for (Enumeration e = entity.attributes().objectEnumerator(); e.hasMoreElements();) {
				EOAttribute attribute = (EOAttribute) e.nextElement();
				Object value = row.objectForKey(attribute.name());
				if (!attribute.isDerived() && value != null && !value.equals(NSKeyValueCoding.NullValue)) {
					doc.addField(attribute.columnName(), value);
				}
			}

			solrServer.add(doc);
			solrServer.commit();

		} catch (Exception e) {
			log.error(e);
			throw new EOGeneralAdaptorException("Failed to insert '" + entity.name() + "': " + e.getMessage());
		}

	}

	@Override
	public boolean isFetchInProgress() {
		return _fetchedRows != null && _fetchIndex < _fetchedRows.count();
	}

	@Override
	public boolean isOpen() {
		return _open;
	}

	@Override
	public void openChannel() {
		if (!_open) {
			_open = true;
		}
	}

	@Override
	public NSDictionary returnValuesForLastStoredProcedureInvocation() {
		throw new UnsupportedOperationException("ERSolrAdaptorChannel.returnValuesForLastStoredProcedureInvocation");
	}

	@Override
	public NSArray<EOAttribute> describeResults() {
		return _attributes;
	}

	@Override
	public NSArray<EOAttribute> attributesToFetch() {
		return _attributes;
	}

	@Override
	public void setAttributesToFetch(NSArray<EOAttribute> attributesToFetch) {
		if (attributesToFetch == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.setAttributesToFetch: null attributes.");
		}
		_attributes = attributesToFetch;
	}

	@Override
	public int updateValuesInRowsDescribedByQualifier(NSDictionary row, EOQualifier qualifier, EOEntity entity) {

		if (row == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.updateValuesInRowsDescribedByQualifier: null row.");
		}

		if (qualifier == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.updateValuesInRowsDescribedByQualifier: null qualifier.");
		}

		if (entity == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.updateValuesInRowsDescribedByQualifier: null entity.");
		}

		int counter = 0;

		try (SolrClient solrServer = getSolrClient(entity);) {

			ERSolrExpression solrExpression = ERSolrExpression.newERSolrExpression(entity);
			String solrQueryString = solrExpression.solrStringForQualifier(qualifier);
			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery(solrQueryString);
			solrQuery.setRows(Integer.MAX_VALUE);

			QueryResponse queryResponse = solrServer.query(solrQuery);

			for (SolrDocument solrDoc : queryResponse.getResults()) {

				SolrInputDocument solrInputDoc = new SolrInputDocument();
				for (String name : solrDoc.getFieldNames()) {
					solrInputDoc.addField(name, solrDoc.getFieldValue(name), 1.0f);
				}

				NSArray someKeys = row.allKeys();
				int keyCount = someKeys.count();
				for (int keyIndex = 0; keyIndex < keyCount; keyIndex++) {
					Object aKey = someKeys.objectAtIndex(keyIndex);
					EOAttribute anAttribute = entity.attributeNamed(aKey.toString());
					if (anAttribute != null) {
						Object aValue = row.objectForKey(aKey);
						String solrFieldName = anAttribute.columnName();

						if (solrInputDoc.containsKey(solrFieldName)) {
							solrInputDoc.setField(solrFieldName, aValue);
						} else {
							solrInputDoc.addField(solrFieldName, aValue);
						}
					}
				}

				solrServer.add(solrInputDoc);
				solrServer.commit();
				counter++;
			}

		} catch (Exception e) {
			log.error(e);
			throw new EOGeneralAdaptorException("Failed to update '" + entity.name() + "': " + e.getMessage());
		}

		return counter;

	}

	/**
	 * Selects rows matching the specified qualifier.
	 */
	@Override
	public void selectAttributes(NSArray<EOAttribute> attributesToFetch, EOFetchSpecification fetchSpecification, boolean shouldLock, EOEntity entity) {

		if (entity == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.selectAttributes: null entity.");
		}

		if (attributesToFetch == null) {
			throw new IllegalArgumentException("ERSolrAdaptorChannel.selectAttributes: null attributes.");
		}

		ERXSolrFetchSpecification solrFetchSpecification = null;
		if (fetchSpecification instanceof ERXSolrFetchSpecification) {
			solrFetchSpecification = (ERXSolrFetchSpecification) fetchSpecification;
		}

		_fetchedEntity = entity;

		setAttributesToFetch(attributesToFetch);

		try (SolrClient solrServer = getSolrClient(entity)) {

			_fetchIndex = 0;
			_fetchedRows = new NSMutableArray<NSMutableDictionary<String, Object>>();

			EOQualifier qualifier = fetchSpecification.qualifier();

			if (entity.restrictingQualifier() != null) {
				qualifier = ERXEOControlUtilities.andQualifier(entity.restrictingQualifier(), qualifier);
			}

			ERSolrExpression solrExpression = ERSolrExpression.newERSolrExpression(entity);
			String solrQueryString = solrExpression.solrStringForQualifier(qualifier);

			SolrQuery solrQuery = new SolrQuery();
			solrQuery.setQuery(solrQueryString);
			solrQuery.setRows(Integer.MAX_VALUE);

			// Sorting
			_applySortOrderings(solrQuery, fetchSpecification.sortOrderings());

			if (solrFetchSpecification != null) {
				if (solrFetchSpecification.maxTime() != null) {
					solrQuery.setTimeAllowed(solrFetchSpecification.maxTime());
				}

				// Batching
				if (solrFetchSpecification.isBatching()) {
					Integer numberOfRowsPerBatch = solrFetchSpecification.batchSize() != null ? solrFetchSpecification.batchSize() : Integer.MAX_VALUE;
					Integer rowOffset = (solrFetchSpecification.batchNumber().intValue() * numberOfRowsPerBatch.intValue()) - numberOfRowsPerBatch;
					solrQuery.setStart(rowOffset);
					solrQuery.setRows(numberOfRowsPerBatch);
				}

				// Facets
				if (solrFetchSpecification.facets() != null && solrFetchSpecification.facets().count() > 0) {
					solrQuery.setFacet(true);

					if (solrFetchSpecification.defaultMinFacetSize() != null) {
						solrQuery.setFacetMinCount(solrFetchSpecification.defaultMinFacetSize());
					}

					if (solrFetchSpecification.defaultFacetLimit() != null) {
						solrQuery.setFacetLimit(solrFetchSpecification.defaultFacetLimit());
					}

					for (Enumeration e = solrFetchSpecification.facets().objectEnumerator(); e.hasMoreElements();) {
						SolrFacet facet = (SolrFacet) e.nextElement();

						if (facet.sort() != null && facet.sort().solrValue() != null) {
							solrQuery.setParam("f." + facet.key() + "." + (FacetParams.FACET_SORT), facet.sort().solrValue());
						}

						if (facet.minCount() != null) {
							solrQuery.setParam("f." + facet.key() + "." + (FacetParams.FACET_MINCOUNT), String.valueOf(facet.minCount()));
						}

						if (facet.limit() != null) {
							solrQuery.setParam("f." + facet.key() + "." + (FacetParams.FACET_LIMIT), String.valueOf(facet.limit()));
						}

						// A selected facet item can be excluded from the counts
						// for other facet items, this is important
						// for supporting multiple selection. The only case
						// where it should not be excluded is when the
						// facet attribute is multi-value and the face operator
						// is AND.
						boolean isExcludingFromCounts = true;
						EOAttribute attribute = entity.attributeNamed(facet.key());

						if (attribute == null) {
							throw new IllegalStateException("Can not find EOAttribute in entity " + entity.name() + " for facet key " + facet.key());
						}

						boolean isMultiValue = attribute.valueTypeClassName().endsWith("NSArray");
						if (isMultiValue && SolrFacet.Operator.AND.equals(facet.operator())) {
							isExcludingFromCounts = false;
						}

						// Facet queries
						if (facet.isQuery()) {
							String qualifierKeyPrefix = facet.key() + ".";
							for (Enumeration qualifierKeyEnumeration = facet.qualifierKeys().objectEnumerator(); qualifierKeyEnumeration.hasMoreElements();) {
								String qualifierKey = (String) qualifierKeyEnumeration.nextElement();
								String prefixedQualifierKey = qualifierKeyPrefix + qualifierKey;
								NSMutableDictionary<String, String> parameters = new NSMutableDictionary<String, String>();

								// Set parameter on qualifier with its key so it
								// can be pulled out of the results by the same
								// key:
								// facet.query={!key=facetKey.qualifierKey}some_query
								parameters.takeValueForKey(prefixedQualifierKey, ERSolrExpression.PARAMETER_KEY);

								if (isExcludingFromCounts) {
									parameters.takeValueForKey(facet.key(), ERSolrExpression.PARAMETER_EXCLUSION);
								}

								StringBuilder sb = new StringBuilder();
								ERSolrExpression.appendLocalParams(sb, parameters);
								EOQualifier facetQualifier = facet.qualifierForKey(qualifierKey);
								sb.append(solrExpression.solrStringForQualifier(facetQualifier));
								solrQuery.addFacetQuery(sb.toString());
							}
						}

						// Facet fields
						else {
							StringBuilder sb = new StringBuilder();
							if (isExcludingFromCounts) {
								ERSolrExpression.appendLocalParams(sb, new NSDictionary<String, String>(facet.key(), ERSolrExpression.PARAMETER_EXCLUSION));
							}
							sb.append(facet.key());
							solrQuery.addFacetField(sb.toString());
						}

						// Create filter query based on selected facet items.
						if (facet.selectedItems() != null && facet.selectedItems().count() > 0) {
							StringBuilder filterQuery = new StringBuilder();

							if (isExcludingFromCounts) {
								ERSolrExpression.appendLocalParams(filterQuery, new NSDictionary<String, String>(facet.key(), ERSolrExpression.PARAMETER_TAG));
							}

							filterQuery.append("(");
							for (Enumeration facetItemEnumeration = facet.selectedItems().objectEnumerator(); facetItemEnumeration.hasMoreElements();) {
								FacetItem selectedFacetItem = (FacetItem) facetItemEnumeration.nextElement();
								String operator = null;
								if (SolrFacet.Operator.NOT.equals(facet.operator())) {
									filterQuery.append(SolrFacet.Operator.NOT.toString()).append(" ");
									operator = SolrFacet.Operator.AND.toString();
								} else {
									operator = facet.operator().toString();
								}

								if (selectedFacetItem.qualifier() != null) {
									filterQuery.append(solrExpression.solrStringForQualifier(selectedFacetItem.qualifier()));
								} else {
									filterQuery.append(facet.key()).append(":");
									ERSolrExpression.escapeAndAppend(selectedFacetItem.key(), filterQuery);
								}

								if (facetItemEnumeration.hasMoreElements()) {
									filterQuery.append(" ").append(operator).append(" ");
								}
							}
							filterQuery.append(")");
							solrQuery.addFilterQuery(filterQuery.toString());
						}
					}
				}
			}

			QueryResponse queryResponse = solrServer.query(solrQuery);

			// TODO: Handle error responses from Solr

			if (log.isDebugEnabled()) {
				log.debug("Original qualifier: " + qualifier);
				log.debug("Solr query: " + ERXStringUtilities.urlDecode(solrQuery.toString()));
				log.debug("Solr response time: " + queryResponse.getElapsedTime() + "ms");
			}

			if (solrFetchSpecification != null) {
				ERXSolrFetchSpecification.Result result = ERXSolrFetchSpecification.Result.newResult(queryResponse, solrFetchSpecification);
				solrFetchSpecification.setResult(result); // FIXME
			}

			boolean isQualifierMoreRestrictiveThanSolrQuery = false;
			for (SolrDocument solrDoc : queryResponse.getResults()) {
				NSMutableDictionary<String, Object> row = new NSMutableDictionary<String, Object>();
				for (EOAttribute attribute : attributesToFetch) {
					// if (solrDoc.containsKey(attribute.name())) {
					if (solrDoc.containsKey(attribute.columnName())) {
						// Object value =
						// solrDoc.getFieldValue(attribute.name());
						Object value = solrDoc.getFieldValue(attribute.columnName());

						if (value == null) {
							value = NSKeyValueCoding.NullValue;
						} else if (value instanceof List) {
							value = new NSArray((List) value);
						} else if (value instanceof Map) {
							value = new NSDictionary((Map) value);
						}

						row.takeValueForKey(value, attribute.name());
					}
				}
                /* TODO commented out to enable working with textCollector field
                // TODO ?? if the qualifier is a collector/multi field (copy field)
                if (qualifier != null && !qualifier.evaluateWithObject(row)) { // Performance impact?
                    isQualifierMoreRestrictiveThanSolrQuery = true;
                } else {
                    _fetchedRows.addObject(row);
                }
                */
                _fetchedRows.addObject(row);
			}

			if (isQualifierMoreRestrictiveThanSolrQuery) {
				log.warn("EOQualifier is more restrictive than generated Solr query: " + ERXStringUtilities.urlDecode(solrQuery.toString()));
			}
		} catch (EOGeneralAdaptorException e) {
			log.error(e);
			throw e;
		} catch (Throwable e) {
			log.error(e);
			throw new EOGeneralAdaptorException("Failed to fetch '" + entity.name() + "' with fetch specification '" + fetchSpecification + "': " + e.getMessage());
		}
	}

	public static Object convertValue(String value, EOAttribute attribute) {
		if (attribute != null) {
			try {
				if (attribute.valueType() != null) {
					char valueType = attribute.valueType().charAt(0);
					switch (valueType) {
					case 'i':
						return Integer.valueOf(value);
					case 'b':
						return BigInteger.valueOf(Long.valueOf(value));
					case 'l':
						return Long.valueOf(value);
					case 'd':
						return Double.valueOf(value);
					case 'B':
						return BigDecimal.valueOf(Double.valueOf(value));
					}
				}
				if (attribute.className().contains("NSTimestamp")) {
					return new NSTimestamp(SimpleDateFormat.getDateInstance().parse(value));
				} else if (attribute.className().contains("NSData")) {
					return new NSData((NSData) NSPropertyListSerialization.propertyListFromString(value));
				} else if (attribute.className().contains("NSArray")) {
					return NSArray.componentsSeparatedByString(value, " ");
				} else if (attribute.className().contains("Boolean")) {
					return Boolean.valueOf(value);
				}
			} catch (ParseException e) {
				throw NSForwardException._runtimeExceptionForThrowable(e);
			}
		}
		return value;
	}

	protected void _applySortOrderings(SolrQuery solrQuery, NSArray<EOSortOrdering> sortOrderings) {
		for (Enumeration sortOrderingEnum = sortOrderings.objectEnumerator(); sortOrderingEnum.hasMoreElements();) {
			EOSortOrdering sortOrdering = (EOSortOrdering) sortOrderingEnum.nextElement();
			SolrQuery.ORDER order;
			NSSelector sortSelector = sortOrdering.selector();
			if (sortSelector == EOSortOrdering.CompareAscending || sortSelector == EOSortOrdering.CompareCaseInsensitiveAscending) {
				order = SolrQuery.ORDER.asc;
			} else if (sortSelector == EOSortOrdering.CompareDescending || sortSelector == EOSortOrdering.CompareCaseInsensitiveDescending) {
				order = SolrQuery.ORDER.desc;
			} else {
				throw new IllegalArgumentException("Unknown sort ordering selector: " + sortSelector);
			}
			solrQuery.addSort(sortOrdering.key(), order);
		}
	}

	private SolrClient getSolrClient(EOEntity entity) throws MalformedURLException {

		NSDictionary connectionDictionary = adaptorContext().adaptor().connectionDictionary();
		String solrUrl = (String) connectionDictionary.objectForKey("serverUrl");
		String solrCore = entity.externalName();

		if (ERXStringUtilities.stringIsNullOrEmpty(solrUrl)) {
			throw new IllegalArgumentException("There is no URL specified for the connection dictionary: " + connectionDictionary);
		}

		ERXMutableURL url = new ERXMutableURL(solrUrl);
		if (solrCore != null && !solrCore.equalsIgnoreCase("solr")) {
			url.setPath(url.path() + solrCore);
		}

		return new HttpSolrClient.Builder(url.toString()).build();

	}

}
