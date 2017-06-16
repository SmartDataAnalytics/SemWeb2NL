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

import java.util.List;

import org.aksw.owl2nl.exception.OWLAxiomConversionException;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.ToStringRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotationAssertionAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLAnnotationPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLAsymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLAxiomVisitor;
import org.semanticweb.owlapi.model.OWLClassAssertionAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDatatypeDefinitionAxiom;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDifferentIndividualsAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLDisjointUnionAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentDataPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalDataPropertyAxiom;
import org.semanticweb.owlapi.model.OWLFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLHasKeyAxiom;
import org.semanticweb.owlapi.model.OWLInverseFunctionalObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLInverseObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLIrreflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLNegativeDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLNegativeObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyDomainAxiom;
import org.semanticweb.owlapi.model.OWLObjectPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSameIndividualAxiom;
import org.semanticweb.owlapi.model.OWLSubAnnotationPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubDataPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLSymmetricObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.SWRLRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simplenlg.features.Feature;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owlapi.dlsyntax.DLSyntaxObjectRenderer;

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
	
	private OWLDataFactory df = new OWLDataFactoryImpl();
	
	private String nl;
	
	public OWLAxiomConverter(Lexicon lexicon) {
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
		
		ceConverter = new OWLClassExpressionConverter(lexicon);
	}
	
	public OWLAxiomConverter() {
		this(Lexicon.getDefaultLexicon());
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
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLAxiomVisitor#visit(org.semanticweb.owlapi.model.OWLSubClassOfAxiom)
	 */
	@Override
	public void visit(OWLSubClassOfAxiom axiom) {
		logger.debug("Converting SubClassOf axiom {}", axiom);
		// convert the subclass
		OWLClassExpression subClass = axiom.getSubClass();
		NLGElement subClassElement = ceConverter.asNLGElement(subClass, true);
		logger.debug("SubClass: " + realiser.realise(subClassElement));
//		((PhraseElement)subClassElement).setPreModifier("every");
		
		// convert the superclass
		OWLClassExpression superClass = axiom.getSuperClass();
		NLGElement superClassElement = ceConverter.asNLGElement(superClass);
		logger.debug("SuperClass: " + realiser.realise(superClassElement));
		
		SPhraseSpec clause = nlgFactory.createClause(subClassElement, "be", superClassElement);
		superClassElement.setFeature(Feature.COMPLEMENTISER, null);

		nl = realiser.realise(clause).toString();
		logger.debug(axiom + " = " + nl);
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
	}
	
	@Override
	public void visit(OWLObjectPropertyAssertionAxiom axiom) {
	}
	
	@Override
	public void visit(OWLDataPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLNegativeObjectPropertyAssertionAxiom axiom) {
	}

	@Override
	public void visit(OWLNegativeDataPropertyAssertionAxiom axiom) {
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
		String ontologyURL = "http://130.88.198.11/2008/iswc-modtut/materials/koala.owl";
		ontologyURL = "http://rpc295.cs.man.ac.uk:8080/repository/download?ontology=http://reliant.teknowledge.com/DAML/Transportation.owl&format=RDF/XML";
		ontologyURL = "http://protege.cim3.net/file/pub/ontologies/travel/travel.owl";
		//ontologyURL = "https://raw.githubusercontent.com/pezra/pretty-printer/master/Jenna-2.6.3/testing/ontology/bugs/koala.owl";
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));
		
		OWLAxiomConverter converter = new OWLAxiomConverter();
		for (OWLAxiom axiom : ontology.getAxioms()) {
			converter.convert(axiom);
		}
	}

}
