/**
 * 
 */
package org.aksw.triple2nl;

import static org.junit.Assert.*;

import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.impl.LiteralLabel;

/**
 * @author Lorenz Buehmann
 *
 */
public class LiteralConverterTest {
	
	private static final LiteralConverter conv = new LiteralConverter(new DefaultIRIConverter(SparqlEndpoint.getEndpointDBpedia()));

	/**
	 * Test method for {@link org.aksw.sparql2nl.naturallanguagegeneration.LiteralConverter#convert(com.hp.hpl.jena.rdf.model.Literal)}.
	 */
	@Test
	public void testConvertDate() {
		
        LiteralLabel lit = NodeFactory.createLiteral("1869-06-27", null, XSDDatatype.XSDdate).getLiteral();
		String convert = conv.convert(lit);
		System.out.println(lit + " --> " + convert);
		assertEquals("June 27, 1869", convert); // with java 1.8.0_40-b26 I get on windows: "27 June 1869"

        lit = NodeFactory.createLiteral("1914-01-01T00:00:00+02:00", null, XSDDatatype.XSDgYear).getLiteral();
        System.out.println(lit + " --> " + conv.convert(lit));
        
        lit = NodeFactory.createLiteral("--04", null, XSDDatatype.XSDgMonth).getLiteral();
        System.out.println(lit + " --> " + conv.convert(lit));
	}
	
	@Test
	public void testConvertUseDefinedDatatype() throws Exception {
		LiteralLabel lit = NodeFactory.createLiteral("123", null, new BaseDatatype("http://dbpedia.org/datatypes/squareKilometre")).getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));
	}

}
