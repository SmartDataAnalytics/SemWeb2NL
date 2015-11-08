/*
 * #%L
 * Triple2NL
 * %%
 * Copyright (C) 2015 Agile Knowledge Engineering and Semantic Web (AKSW)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
/**
 * 
 */
package org.aksw.triple2nl;

import static org.junit.Assert.*;

import java.io.StringReader;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.triple2nl.converter.DefaultIRIConverter;
import org.aksw.triple2nl.converter.LiteralConverter;
import org.apache.jena.riot.Lang;
import org.dllearner.kb.sparql.SparqlEndpoint;
import org.junit.BeforeClass;
import org.junit.Test;

import com.hp.hpl.jena.datatypes.BaseDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.impl.LiteralLabel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

/**
 * @author Lorenz Buehmann
 *
 */
public class LiteralConverterTest {
	
	private static LiteralConverter conv;
	
	@BeforeClass
	public static void init() {
		String kb = "<http://dbpedia.org/datatypes/squareKilometre> <http://www.w3.org/2000/01/rdf-schema#label> \"square kilometre\"@en .";
		Model model = ModelFactory.createDefaultModel();
		model.read(new StringReader(kb), null , Lang.TURTLE.getName());
		
		conv = new LiteralConverter(new DefaultIRIConverter(model));
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.converter.sparql2nl.naturallanguagegeneration.LiteralConverter#convert(com.hp.hpl.jena.rdf.model.Literal)}.
	 */
	@Test
	public void testConvertDate() {
		
        LiteralLabel lit = NodeFactory.createLiteral("1869-06-27", null, XSDDatatype.XSDdate).getLiteral();
		String convert = conv.convert(lit);
		System.out.println(lit + " --> " + convert);
		assertEquals("27 June 1869", convert); // would be with June 27, 1869 in US 

        lit = NodeFactory.createLiteral("1914-01-01T00:00:00+02:00", null, XSDDatatype.XSDgYear).getLiteral();
        System.out.println(lit + " --> " + conv.convert(lit));
        
        lit = NodeFactory.createLiteral("--04", null, XSDDatatype.XSDgMonth).getLiteral();
        System.out.println(lit + " --> " + conv.convert(lit));
        
        lit = NodeFactory.createLiteral("1989-01-01+02:00", null, XSDDatatype.XSDgYear).getLiteral();
        System.out.println(lit + " --> " + conv.convert(lit));
        
        
	}
	
	@Test
	public void testConvertUseDefinedDatatype() throws Exception {
		LiteralLabel lit = NodeFactory.createLiteral("123", null, new BaseDatatype("http://dbpedia.org/datatypes/squareKilometre")).getLiteral();
		System.out.println(lit + " --> " + conv.convert(lit));
	}

}
