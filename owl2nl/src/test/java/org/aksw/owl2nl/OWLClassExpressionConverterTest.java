/*
 * #%L
 * OWL2NL
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
package org.aksw.owl2nl;

import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLClassExpressionConverterTest {

	private static OWLClassExpressionConverter converter;
	private static OWLObjectProperty birthPlace;
	private static OWLObjectProperty worksFor;
	private static OWLObjectProperty ledBy;
	private static OWLObjectProperty hasChildren;
	private static OWLDataProperty nrOfInhabitants;
	private static OWLClass place;
	private static OWLClass company;
	private static OWLClass person;
	private static OWLClass female;
	private static OWLDataFactoryImpl df;
	private static OWLNamedIndividual leipzig;
	private static OWLLiteral literal;
	private static OWLClassExpression oneOfFemale;
	private static OWLObjectProperty married;

	OWLClassExpression ce;
	String text;
	private static OWLDataRange dataRange;

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		converter = new OWLClassExpressionConverter();
		
		df = new OWLDataFactoryImpl();
		PrefixManager pm = new DefaultPrefixManager("http://dbpedia.org/ontology/");
		
		birthPlace = df.getOWLObjectProperty("birthPlace", pm);
		worksFor = df.getOWLObjectProperty("worksFor", pm);
		ledBy = df.getOWLObjectProperty("isLedBy", pm);
		hasChildren = df.getOWLObjectProperty("hasChildren", pm);
		married = df.getOWLObjectProperty("married", pm);

		nrOfInhabitants = df.getOWLDataProperty("nrOfInhabitants", pm);
		dataRange = df.getOWLDatatypeMinInclusiveRestriction(10000000);
		
		place = df.getOWLClass("Place", pm);
		company = df.getOWLClass("Company", pm);
		person = df.getOWLClass("Person", pm);
		female = df.getOWLClass("Female", pm);
		
		leipzig = df.getOWLNamedIndividual("Leipzig", pm);
		literal = df.getOWLLiteral(1000000);

		oneOfFemale = df.getOWLObjectOneOf(df.getOWLNamedIndividual("Female", pm));
		
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
	}
	
	@Test
	public void testNamedClass() {
		// person
		ce = person;
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testSomeValuesFrom(){
		// birth place is a place
		ce = df.getOWLObjectSomeValuesFrom(birthPlace, place);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
				
		// works for a company
		ce = df.getOWLObjectSomeValuesFrom(worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);

		// works for a company
		ce = df.getOWLObjectSomeValuesFrom(married, person);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testNested() {
		
		ce = df.getOWLObjectMinCardinality(3, birthPlace,
				df.getOWLObjectIntersectionOf(place, df.getOWLObjectSomeValuesFrom(ledBy, person)));
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		ce = df.getOWLObjectSomeValuesFrom(worksFor, df.getOWLObjectSomeValuesFrom(ledBy,person));
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testMinCardinality(){
		// has at least 3 birth places that are a place
		ce = df.getOWLObjectMinCardinality(3, birthPlace, place);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		// works for at least 3 companies
		ce = df.getOWLObjectMinCardinality(3, worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		
		ce = df.getOWLDataMinCardinality(3, nrOfInhabitants, dataRange);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testMaxCardinality(){
		// has at most 3 birth places that are a place
		ce = df.getOWLObjectMaxCardinality(3, birthPlace, place);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		// works for at most 3 companies
		ce = df.getOWLObjectMaxCardinality(3, worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		ce = df.getOWLDataMaxCardinality(3, nrOfInhabitants, dataRange);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testExactCardinality(){
		// has exactly 3 birth places that are a place
		ce = df.getOWLObjectExactCardinality(3, birthPlace, place);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		// works for exactly 3 companies
		ce = df.getOWLObjectExactCardinality(3, worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);

		ce = df.getOWLDataExactCardinality(3, nrOfInhabitants, dataRange);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);

		ce = df.getOWLObjectExactCardinality(3, hasChildren, oneOfFemale);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);

		ce = df.getOWLObjectExactCardinality(3, hasChildren, female);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testAllValuesFrom() {
		// works only for a company
		ce = df.getOWLObjectAllValuesFrom(worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		ce = df.getOWLDataAllValuesFrom(nrOfInhabitants, dataRange);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testHasValue() {
		// works for a company
		ce = df.getOWLObjectHasValue(birthPlace, leipzig);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		// nr of inhabitants is 1000000
		ce = df.getOWLDataHasValue(nrOfInhabitants, literal);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testDataHasValue() {
		// works for a company
		ce = df.getOWLObjectAllValuesFrom(worksFor, company);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testIntersection() {
		// works for a company
		ce = df.getOWLObjectIntersectionOf(
				df.getOWLObjectSomeValuesFrom(worksFor, company),
				person);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		ce = df.getOWLObjectIntersectionOf(
				place, 
				df.getOWLObjectSomeValuesFrom(ledBy, person));
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testUnion() {
		// works for a company
		ce = df.getOWLObjectUnionOf(
				df.getOWLObjectSomeValuesFrom(worksFor, company),
				person);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
	
	@Test
	public void testNegation() {
		// not a person
		ce = df.getOWLObjectComplementOf(person);
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
		
		// does not work for a company
		ce = df.getOWLObjectComplementOf(df.getOWLObjectSomeValuesFrom(worksFor, company));
		text = converter.convert(ce);
		System.out.println(ce + " = " + text);
	}
}
