/**
 * 
 */
package org.aksw.sparql2nl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.Syntax;

/**
 * @author Lorenz Buehmann
 *
 */
public class QALDBenchmark {
	
	private static final Map<Integer, Query> queries = new HashMap<Integer, Query>();
	
	static {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(QALDBenchmark.class.getClassLoader().getResourceAsStream("qald-4_multilingual_test_withanswers.xml"));
			XPathFactory xPathfactory = XPathFactory.newInstance();
			XPath xpath = xPathfactory.newXPath();
			
			XPathExpression expr = xpath.compile("/dataset/question[@answertype='resource']");
			NodeList nl = (NodeList) expr.evaluate(doc, XPathConstants.NODESET);
			for(int i= 0; i < nl.getLength(); i++){
				Node node = nl.item(i);
				Element el = (Element)node;
				int id = Integer.parseInt(el.getAttribute("id"));
				String queryString = el.getElementsByTagName("query").item(0).getTextContent().trim();
				if(!queryString.toUpperCase().equals("OUT OF SCOPE")){
					try {
						Query query = QueryFactory.create(queryString, Syntax.syntaxSPARQL_11);
						queries.put(id, query);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Query getQuery(int id){
		return queries.get(id);
	}
	
	public static Collection<Query> getQueries(){
		return queries.values();
	}
	
	public static Collection<Query> getQueries(int... ids){
		Collection<Query> queries = new ArrayList<Query>();
		for (int id : ids) {
			queries.add(getQuery(id));
		}
		return queries;
	}
	
	public static void main(String[] args) throws Exception {
		System.out.println(QALDBenchmark.getQuery(1));
	}

}
