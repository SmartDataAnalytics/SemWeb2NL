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
package org.aksw.owl2nl;

import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.graph.Triple;
import org.aksw.jena_sparql_api.model.QueryExecutionFactoryModel;
import org.aksw.owl2nl.exception.OWLAxiomConversionException;
import org.aksw.owl2nl.util.OWLClassExpressionUtils;
import org.aksw.triple2nl.TripleConverter;
import org.aksw.triple2nl.converter.DefaultIRIConverter;
import org.aksw.triple2nl.converter.IRIConverter;
import org.dllearner.utilities.OwlApiJenaUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simplenlg.features.Feature;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

import java.util.List;
import java.util.TreeSet;

/**
 * Converts OWL axioms into natural language.
 * @author Lorenz Buehmann
 *
 */
public class OWLAxiomConverter implements OWLAxiomVisitor{
	
	private static final Logger logger = LoggerFactory.getLogger(OWLAxiomConverter.class);
	
	private NLGFactory nlgFactory;
	private Realiser realiser;
	
	private OWLClassExpressionConverter ceConverter;
	private TripleConverter tripleConverter;
	
	private OWLDataFactory df = new OWLDataFactoryImpl();
	
	private String nl;

	private NLGElement nlgElement;
	
	public OWLAxiomConverter(OWLOntology ontology) {
		this(ontology, Lexicon.getDefaultLexicon());
	}

	public OWLAxiomConverter(OWLOntology ontology, Lexicon lexicon) {
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);

		QueryExecutionFactoryModel qef = new QueryExecutionFactoryModel(OwlApiJenaUtils.getModel(ontology));
		IRIConverter iriConverter = new DefaultIRIConverter(qef);

		ceConverter = new OWLClassExpressionConverter(lexicon, iriConverter);

		tripleConverter = new TripleConverter(qef, iriConverter, null, null);
	}

	/**
	 * Converts the OWL axiom into natural language. Only logical axioms are 
	 * supported, i.e. declaration axioms and annotation axioms are not 
	 * converted and <code>null</code> will be returned instead.
	 * @param axiom the OWL axiom
	 * @return the natural language expression
	 */
	public String convert(OWLAxiom axiom) throws OWLAxiomConversionException {
		reset();
		
		if (axiom.isLogicalAxiom()) {
			logger.debug("Converting " + axiom.getAxiomType().getName() + " axiom: " + axiom);
			try {
				axiom.accept(this);
				if(nlgElement != null) {
					nl = realiser.realise(nlgElement).getRealisation();
					logger.debug("Axiom:" + nl);
				}
				return nl;
			} catch (Exception e) {
				throw new OWLAxiomConversionException(axiom, e);
			}
		}

		logger.warn("Conversion of non-logical axioms not supported yet!");
		return null;
	}
	
	private void reset() {
		nl = null;
		nlgElement = null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi.model.OWLSubClassOfAxiom)
	 */
	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
		// convert the subclass
		OWLClassExpression subClass = axiom.getSubClass();
		NLGElement subClassElement = ceConverter.asNLGElement(subClass, true);
		logger.debug("SubClass: " + realiser.realise(subClassElement));
//		((PhraseElement)subClassElement).setPreModifier("every");
		
		// convert the superclass
		OWLClassExpression superClass = axiom.getSuperClass();
		NLGElement superClassElement = ceConverter.asNLGElement(superClass, false, subClass.isAnonymous());
		logger.debug("SuperClass: " + realiser.realise(superClassElement));

		SPhraseSpec clause;
		if(subClass.isAnonymous() || OWLClassExpressionUtils.hasNamedClassOnTopLevel(superClass)) { // LHS is complex CE
			clause = nlgFactory.createClause(subClassElement, "be", superClassElement);
			superClassElement.setFeature(Feature.COMPLEMENTISER, null);
		} else {// LHS is named class
			clause = nlgFactory.createClause(subClassElement, null, superClassElement);
			superClassElement.setFeature(Feature.COMPLEMENTISER, null);
		}


		nlgElement = clause;
	}
	
	@Override
	public void visit(OWLEquivalentClassesAxiom axiom) {
		List<OWLClassExpression> classExpressions = axiom.getClassExpressionsAsList();
		
		for (int i = 0; i < classExpressions.size(); i++) {
			for (int j = i + 1; j < classExpressions.size(); j++) {
				OWLSubClassOfAxiom subClassAxiom = df.getOWLSubClassOfAxiom(
						classExpressions.get(i), 
						classExpressions.get(j));
				subClassAxiom.accept(this);
			}
		}
	}

	/*
	 * We rewrite DisjointClasses(C_1,...,C_n) as SubClassOf(C_i, ObjectComplementOf(C_j)) for each subset {C_i,C_j} with i != j 
	 */
	@Override
	public void visit(OWLDisjointClassesAxiom axiom) {
		List<OWLClassExpression> classExpressions = axiom.getClassExpressionsAsList();
		
		for (int i = 0; i < classExpressions.size(); i++) {
			for (int j = i + 1; j < classExpressions.size(); j++) {
				OWLSubClassOfAxiom subClassAxiom = df.getOWLSubClassOfAxiom(
						classExpressions.get(i), 
						df.getOWLObjectComplementOf(classExpressions.get(j)));
				subClassAxiom.accept(this);
			}
		}
	}
	
	@Override
	public void visit(OWLDisjointUnionAxiom axiom) {
	}

	
	//#########################################################
	//################# object property axioms ################
	//#########################################################

	@Override
	public void visit(OWLSubObjectPropertyOfAxiom axiom) {
	}
	
	@Override
	public void visit(OWLEquivalentObjectPropertiesAxiom axiom) {
	}
	
	@Override
	public void visit(OWLDisjointObjectPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLObjectPropertyDomainAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLObjectPropertyRangeAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLInverseObjectPropertiesAxiom axiom) {
	}

	@Override
	public void visit(OWLFunctionalObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLAsymmetricObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLReflexiveObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLSymmetricObjectPropertyAxiom axiom) {
	}
	
	@Override
	public void visit(OWLTransitiveObjectPropertyAxiom axiom) {
	}

	@Override
	public void visit(OWLIrreflexiveObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLInverseFunctionalObjectPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	//#########################################################
	//################# data property axioms ##################
	//#########################################################
	
	@Override
	public void visit(OWLSubDataPropertyOfAxiom axiom) {
	}
	
	@Override
	public void visit(OWLEquivalentDataPropertiesAxiom axiom) {
	}
	
	@Override
	public void visit(OWLDisjointDataPropertiesAxiom axiom) {
	}
	
	@Override
	public void visit(OWLDataPropertyDomainAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLDataPropertyRangeAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	@Override
	public void visit(OWLFunctionalDataPropertyAxiom axiom) {
		axiom.asOWLSubClassOfAxiom().accept(this);
	}
	
	//#########################################################
	//################# individual axioms #####################
	//#########################################################
	
	@Override
	public void visit(OWLClassAssertionAxiom axiom) {
		if(axiom.getClassExpression().isOWLThing()) {
			logger.warn("Explicit assertion of an individual to owl:Thing is meaningless. Skipping conversion.");
			return;
		}
		SPhraseSpec clause = nlgFactory.createClause();

		// individual is the subject
		OWLIndividual individual = axiom.getIndividual();
		if(individual.isNamed()) {
			clause.setSubject(ceConverter.getLexicalForm(individual.asOWLNamedIndividual()));
		} else {
			clause.setSubject("something");
		}

		// 'to be' as verb
		clause.setVerb("be");

		// class is the object
		OWLClassExpression ce = axiom.getClassExpression();
		clause.setObject(ceConverter.asNLGElement(ce));

		nlgElement = clause;
	}
	
	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
		Triple triple;
		if(axiom.getProperty().isAnonymous()) {
			triple = Triple.create(
					NodeFactory.createURI(axiom.getSubject().toStringID()),
					NodeFactory.createURI(axiom.getProperty().getNamedProperty().toStringID()),
					NodeFactory.createURI(axiom.getObject().toStringID()));

		} else {
			triple = Triple.create(
					NodeFactory.createURI(axiom.getObject().toStringID()),
					NodeFactory.createURI(axiom.getProperty().getNamedProperty().toStringID()),
					NodeFactory.createURI(axiom.getSubject().toStringID()));
		}
		SPhraseSpec phrase = tripleConverter.convertToPhrase(triple, false, axiom.getProperty().isAnonymous());

		nlgElement = phrase;
	}
	
	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
		Triple triple = Triple.create(
				NodeFactory.createURI(axiom.getSubject().toStringID()),
				OwlApiJenaUtils.asNode(axiom.getProperty().asOWLDataProperty()),
				NodeFactory.createLiteral(OwlApiJenaUtils.getLiteral(axiom.getObject())));
		SPhraseSpec phrase = tripleConverter.convertToPhrase(triple, false, axiom.getProperty().isAnonymous());

		nlgElement = phrase;
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
		df.getOWLObjectPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject()).accept(this);
		nlgElement.setFeature(Feature.NEGATED, true);
	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
		df.getOWLDataPropertyAssertionAxiom(axiom.getProperty(), axiom.getSubject(), axiom.getObject()).accept(this);
		nlgElement.setFeature(Feature.NEGATED, true);
	}

	@Override
	public void visit(OWLDifferentIndividualsAxiom axiom) {
	}

	@Override
	public void visit(OWLSameIndividualAxiom axiom) {
	}

	//#########################################################
	//################# other logical axioms ##################
	//#########################################################

	@Override
	public void visit(OWLSubPropertyChainOfAxiom axiom) {
	}
	
	@Override
	public void visit(OWLHasKeyAxiom axiom) {
	}

	@Override
	public void visit(OWLDatatypeDefinitionAxiom axiom) {
	}

	@Override
	public void visit(SWRLRule axiom) {
	}
	
	//#########################################################
	//################# non-logical axioms ####################
	//#########################################################
	
	@Override
	public void visit(OWLAnnotationAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLSubAnnotationPropertyOfAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyDomainAxiom axiom) {
	}

	@Override
	public void visit(OWLAnnotationPropertyRangeAxiom axiom) {
	}

	@Override
	public void visit(OWLDeclarationAxiom axiom) {
	}
	
	public static void main(String[] args) throws Exception {
		ToStringRenderer.getInstance().setRenderer(new DLSyntaxObjectRenderer());
		ToStringRenderer.getInstance().setRenderer(new ManchesterOWLSyntaxOWLObjectRendererImpl());
		String ontologyURL = "http://130.88.198.11/2008/iswc-modtut/materials/koala.owl";
		ontologyURL = "http://rpc295.cs.man.ac.uk:8080/repository/download?ontology=http://reliant.teknowledge.com/DAML/Transportation.owl&format=RDF/XML";
		ontologyURL = "http://protege.cim3.net/file/pub/ontologies/travel/travel.owl";
		ontologyURL = "https://raw.githubusercontent.com/pezra/pretty-printer/master/Jenna-2.6.3/testing/ontology/bugs/koala.owl";
//		ontologyURL = "http://protege.stanford.edu/ontologies/pizza/pizza.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		
		OWLAxiomConverter converter = new OWLAxiomConverter(ontology);
		System.out.println("\tAxiom\tAs SubClassOf Axiom\tVerbalization");
		for (OWLAxiom axiom : new TreeSet<>(ontology.getLogicalAxioms())) {
			String axiomStr = axiom.toString();
			String axiomSubClsStr = "";
			boolean isSubClassOfAxiom = axiom.getAxiomType() == AxiomType.SUBCLASS_OF;
			if(axiom instanceof OWLSubClassOfAxiomShortCut) {
				axiomSubClsStr = ((OWLSubClassOfAxiomShortCut) axiom).asOWLSubClassOfAxiom().toString();
				isSubClassOfAxiom = true;
			}
			String nl = converter.convert(axiom);

			System.out.println((isSubClassOfAxiom ? "x\t" : "\t") + axiomStr + "\t" + axiomSubClsStr + "\t" + nl);
		}
	}

}
