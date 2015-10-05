/**
 * 
 */
package org.aksw.avatar.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.math3.exception.OutOfRangeException;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;
import org.apache.commons.math3.stat.regression.ModelSpecificationException;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.apache.commons.math3.util.Pair;
import org.dllearner.kb.SparqlEndpointKS;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.dllearner.reasoning.SPARQLReasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.OWLDataPropertyImpl;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.query.ParameterizedSparqlString;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * @author Lorenz Buehmann
 *
 */
public class DBpediaFilter {
	
	private QueryExecutionFactory qef;
	
	private IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();

	public DBpediaFilter(QueryExecutionFactory qef) {
		this.qef = qef;
	}
	
	public void filterRedundantProperties(List<OWLDataProperty> properties) {
		
		Set<Pair<String, String>> suffixPairs = Sets.<Pair<String, String>>newHashSet(new Pair<String, String>("Date", "Year"));
		
		ArrayList<OWLDataProperty> toRemove = new ArrayList<OWLDataProperty>();
		
		// properties ending with "date" usually subsume properties that end with "year"
		for(int i = 0; i < properties.size(); i++) {
			OWLDataProperty p1 = properties.get(i);
			String shortForm1 = sfp.getShortForm(p1.getIRI());
			
			for(int j = i+1; j < properties.size(); j++) {
				OWLDataProperty p2 = properties.get(j);
				String shortForm2 = sfp.getShortForm(p2.getIRI());

				for (Pair<String, String> suffixPair : suffixPairs) {
					String specificSuffix = suffixPair.getFirst();
					String generalSuffix = suffixPair.getSecond();

					String prefix1 = null;
					String prefix2 = null;
					OWLDataProperty specificPropertyCandidate = null;
					OWLDataProperty generalPropertyCandidate = null;
					if(shortForm1.endsWith(specificSuffix) && shortForm2.endsWith(generalSuffix)) {
						prefix1 = shortForm1.substring(0, shortForm1.lastIndexOf(specificSuffix));
						prefix2 = shortForm2.substring(0, shortForm2.lastIndexOf(generalSuffix));
						
						specificPropertyCandidate = p1;
						generalPropertyCandidate = p2;
					} else if(shortForm1.endsWith(generalSuffix) && shortForm2.endsWith(specificSuffix)) {
						prefix1 = shortForm1.substring(0, shortForm1.lastIndexOf(generalSuffix));
						prefix2 = shortForm2.substring(0, shortForm2.lastIndexOf(specificSuffix));
						
						specificPropertyCandidate = p2;
						generalPropertyCandidate = p1;
					}

					if(prefix1 != null && prefix2 != null && generalPropertyCandidate != null) {
						String commonPrefix = Strings.commonPrefix(prefix1, prefix2);

						int minPrefixLength = Math.max(prefix1.length(), prefix2.length());
						if(commonPrefix.length() == minPrefixLength) {
							toRemove.add(generalPropertyCandidate);
						}
					}
				}
			}
		}
		
		properties.removeAll(toRemove);
	}
	
	public void getCorrelatedProperties(List<OWLDataProperty> properties) {
		// pre-filter candidates by suffix match
		
		List<List<OWLDataProperty>> candidatePairs = new ArrayList<List<OWLDataProperty>>();
		
		for(int i = 0; i < properties.size(); i++) {
			OWLDataProperty p1 = properties.get(i);
			String shortForm1 = sfp.getShortForm(p1.getIRI());
			
			for(int j = i+1; j < properties.size(); j++) {
				OWLDataProperty p2 = properties.get(j);
				String shortForm2 = sfp.getShortForm(p2.getIRI());
				
				if(shortForm1.equals(shortForm2)) {
					candidatePairs.add(Lists.newArrayList(p1, p2));
				}
			}
		}
		
		// analyze data of candidate pairs
		for (List<OWLDataProperty> pair : candidatePairs) {
			try {
				OWLDataProperty p1 = pair.get(0);
				OWLDataProperty p2 = pair.get(1);
				
				System.out.println(p1 + "----" + p2);
				
				// get random sample matrix of values
				RealMatrix sampleMatrix = getValuesSample(p1, p2);
				
				// compute correlation coefficient
				PearsonsCorrelation rValue = new PearsonsCorrelation(sampleMatrix); 
				double correlationCoefficient = rValue.getCorrelationMatrix().getEntry(0, 1);
				System.out.println("Pearsons coefficient of correlation: " + correlationCoefficient);
				
				// get p-value
				double pValue = rValue.getCorrelationPValues().getEntry(0, 1);
				System.out.println("p-value: " + pValue);
				
				// apply linear regression
				SimpleRegression regression = new SimpleRegression();
				regression.addData(sampleMatrix.getData());
				
				// get the slope
				double slope = regression.getSlope();
				System.out.println("slope estimate(b1): " + slope);
				System.out.println("s(b1): " + regression.getSlopeStdErr());
			} catch (Exception e) {
				System.out.println(e.getMessage());
			} 
		}
	}
	
	private RealMatrix getValuesSample(OWLDataProperty p1, OWLDataProperty p2) throws Exception{
		ParameterizedSparqlString queryTemplate = new ParameterizedSparqlString("SELECT * WHERE {?s ?p1 ?o1 . ?s ?p2 ?o2 . }");
		queryTemplate.setIri("p1", p1.toStringID());
		queryTemplate.setIri("p2", p2.toStringID());
		
		Query query = queryTemplate.asQuery();
		query.setLimit(1000);
		
		QueryExecution qe = qef.createQueryExecution(query);
		ResultSet rs = qe.execSelect();
		
		List<Double> xValues = new ArrayList<Double>();
		List<Double> yValues = new ArrayList<Double>();
		while(rs.hasNext()) {
			QuerySolution qs = rs.next();
			Resource resource = qs.getResource("s");
			
			Literal lit1 = qs.getLiteral("o1");
			Literal lit2 = qs.getLiteral("o2");
			
//			System.out.println(resource);
//			System.out.println("lit1:" + lit1);
//			System.out.println("lit2:" + lit2);
			try {
				double val1 = Double.parseDouble(lit1.getLexicalForm());
				double val2 = Double.parseDouble(lit2.getLexicalForm());
				
//				System.out.println(val1);
//				System.out.println(val2);
				
				if(val1 != 0.0 && val2 != 0.0) {
					xValues.add(val1);
					yValues.add(val2);
				}
			} catch(Exception e) {
//				e.printStackTrace();
			}
		}
		
		if(xValues.size() > 3) {
			RealMatrix matrix = new Array2DRowRealMatrix(xValues.size(), 2);
			for(int row = 0; row < xValues.size(); row++) {
				matrix.addToEntry(row, 0, xValues.get(row));
				matrix.addToEntry(row, 1, yValues.get(row));
			}
			return matrix;
		}
		
		throw new Exception("Not enough data for properties " + p1 + " and " + p2);
	}
	
	public static void main(String[] args) throws Exception {
		SparqlEndpointKS ks = new SparqlEndpointKS(SparqlEndpoint.getEndpointDBpedia());
		ks.init();
		SPARQLReasoner reasoner = new SPARQLReasoner(ks);
		
		DBpediaFilter dbPediaFilter = new DBpediaFilter(ks.getQueryExecutionFactory());
		
		ArrayList<OWLDataProperty> properties = new ArrayList<OWLDataProperty>(reasoner.getDatatypeProperties());
		
		dbPediaFilter.filterRedundantProperties(properties);
		
//		properties = Lists.<OWLDataProperty>newArrayList(
//				new OWLDataPropertyImpl(IRI.create("http://dbpedia.org/ontology/height")), 
//				new OWLDataPropertyImpl(IRI.create("http://dbpedia.org/ontology/Person/height")),
//				new OWLDataPropertyImpl(IRI.create("http://dbpedia.org/ontology/weight")), 
//				new OWLDataPropertyImpl(IRI.create("http://dbpedia.org/ontology/Person/weight"))
//				);
		
		dbPediaFilter.getCorrelatedProperties(properties);
	}

}
