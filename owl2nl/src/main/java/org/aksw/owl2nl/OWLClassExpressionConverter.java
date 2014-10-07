/**
 * 
 */
package org.aksw.owl2nl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.triple2nl.IRIConverter;
import org.aksw.triple2nl.SimpleIRIConverter;
import org.aksw.triple2nl.nlp.stemming.PlingStemmer;
import org.aksw.triple2nl.property.PropertyVerbalization;
import org.aksw.triple2nl.property.PropertyVerbalizer;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataComplementOf;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataIntersectionOf;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataOneOf;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataRangeVisitorEx;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDataUnionOf;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDatatypeRestriction;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualVisitorEx;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simplenlg.features.Feature;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.google.common.collect.Sets;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLClassExpressionConverter implements OWLClassExpressionVisitorEx<NLGElement>, OWLIndividualVisitorEx<NLGElement>, OWLDataRangeVisitorEx<NLGElement>{
	
	private static final Logger logger = LoggerFactory.getLogger(OWLClassExpressionConverter.class);

	NLGFactory nlgFactory;
	Realiser realiser;
	
	IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	
	IRIConverter iriConverter = new SimpleIRIConverter();
	PropertyVerbalizer propertyVerbalizer = new PropertyVerbalizer(iriConverter, null);
	OWLDataFactory df = new OWLDataFactoryImpl(false, false);
	
	boolean noun;
	
	public OWLClassExpressionConverter(Lexicon lexicon) {
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public OWLClassExpressionConverter() {
		this(Lexicon.getDefaultLexicon());
	}
	
	public String convert(OWLClassExpression ce) {
		// rewrite
		ce = rewrite(ce);

		// process
		NLGElement nlgElement = ce.accept(this);

		// realise
		nlgElement = realiser.realise(nlgElement);
		
		return nlgElement.getRealisation();
	}
	
	private String getLexicalForm(OWLEntity entity){
		return sfp.getShortForm(entity.getIRI());
	}
	
	private boolean containsNamedClass(Set<OWLClassExpression> classExpressions){
		for (OWLClassExpression ce : classExpressions) {
			if(!ce.isAnonymous()){
				return true;
			}
		}
		return false;
	}
	
	private OWLClassExpression rewrite(OWLClassExpression ce){
		if(!ce.isAnonymous()){
			return ce;
		} else if(ce instanceof OWLObjectIntersectionOf){
			Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) ce).getOperands();
			
			if(containsNamedClass(operands)){
				return ce;
			}
			
			operands.add(df.getOWLThing());
			return df.getOWLObjectIntersectionOf(operands);
		} else if(ce instanceof OWLObjectUnionOf){
			Set<OWLClassExpression> operands = ((OWLObjectUnionOf) ce).getOperands();
			Set<OWLClassExpression> newOperands = Sets.newHashSet();
			
			for (OWLClassExpression operand : operands) {
				newOperands.add(rewrite(operand));
			}
			
			return df.getOWLObjectUnionOf(newOperands);
		}
		Set<OWLClassExpression> operands = Sets.<OWLClassExpression>newHashSet(ce, df.getOWLThing());
		return df.getOWLObjectIntersectionOf(operands);
	}
	
	/**
	 * Returns a list of operands ordered by class expressions types,
	 * starting with the "more easy" first.
	 * @param ce
	 * @return
	 */
	private List<OWLClassExpression> getOperandsByPriority(OWLNaryBooleanClassExpression ce){
		return ce.getOperandsAsList();
	}

	@Override
	public NLGElement visit(OWLClass ce) {
		if(ce.isOWLThing()){
			return nlgFactory.createNounPhrase("everything");
		}
		noun = true;
		return nlgFactory.createNounPhrase("a", getLexicalForm(ce).toLowerCase());
	}

	@Override
	public NLGElement visit(OWLObjectIntersectionOf ce) {
		List<OWLClassExpression> operands = getOperandsByPriority(ce);
		
		// process the classes first
		OWLClassExpression first = operands.remove(0);
		SPhraseSpec phrase = nlgFactory.createClause();
		phrase.setSubject(first.accept(this));
		
		if(operands.size() >= 2){
			CoordinatedPhraseElement cc = nlgFactory.createCoordinatedPhrase();
			
			// process the classes
			Iterator<OWLClassExpression> iterator = operands.iterator();
			List<OWLClass> classes = new ArrayList<OWLClass>();
			while(iterator.hasNext()){
				OWLClassExpression operand = iterator.next();
				if(!operand.isAnonymous()){
					classes.add(operand.asOWLClass());
					iterator.remove();
				}
			}
			for (OWLClass cls : classes) {
				SPhraseSpec clause = nlgFactory.createClause("that", "is");
				clause.setObject(cls.accept(this));
				cc.addCoordinate(clause);
			}
			
			// process the rest
			for (OWLClassExpression operand : operands) {
				SPhraseSpec clause = nlgFactory.createClause();
				NLGElement el = operand.accept(this);
				if(noun){
					clause.setSubject("whose");
					clause.setVerbPhrase(el);
				} else {
					clause.setSubject("that");
					clause.setVerbPhrase(el);
				}
				cc.addCoordinate(clause);
			}
			
			phrase.setVerbPhrase(cc);
		} else {
			OWLClassExpression operand = operands.get(0);
			SPhraseSpec clause = nlgFactory.createClause();
			NLGElement el = operand.accept(this);
			if(noun){
				clause.setFeature(Feature.COMPLEMENTISER, "whose");
			} else {
				clause.setFeature(Feature.COMPLEMENTISER, "that");
			}
			clause.setVerbPhrase(el);
			phrase.setComplement(clause);
		}
		System.out.println(realiser.realise(phrase));
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectUnionOf ce) {
		List<OWLClassExpression> operands = getOperandsByPriority(ce);

		CoordinatedPhraseElement cc = nlgFactory.createCoordinatedPhrase();
		cc.setConjunction("or");

		for (OWLClassExpression operand : operands) {
			NLGElement el = operand.accept(this);
			cc.addCoordinate(el);
		}

		return cc;
	}

	@Override
	public NLGElement visit(OWLObjectComplementOf ce) {
		OWLClassExpression op = ce.getOperand();
		
		NLGElement phrase = op.accept(this);
		if(!op.isAnonymous()){
			phrase = nlgFactory.createClause(null, "is", phrase);
		}
		
		phrase.setFeature(Feature.NEGATED, true);
		
		noun = false;
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectSomeValuesFrom ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				phrase.setVerb(propertyVerbalization.getVerbalizationText());
				
			
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectAllValuesFrom ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				VPPhraseSpec verb = nlgFactory.createVerbPhrase(propertyVerbalization.getVerbalizationText());
				verb.addModifier("only");
				phrase.setVerb(verb);
				
			
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectHasValue ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLIndividual value = ce.getValue();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = value.accept(this);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				phrase.setVerb(propertyVerbalization.getVerbalizationText());
				
			
				NLGElement fillerElement = value.accept(this);
				phrase.setObject(fillerElement);
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectMinCardinality ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NLGElement word = nlgFactory.createWord(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()), LexicalCategory.NOUN);
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(word);
				if(cardinality > 1){
					word.setPlural(true);
					propertyNounPhrase.setPlural(true);
				}
				
				phrase.setVerb("have at least " + cardinality);
				phrase.setObject(propertyNounPhrase);
				
				
				NLGElement fillerElement = filler.accept(this);
				SPhraseSpec clause = nlgFactory.createClause(null, "is", fillerElement);
				if(cardinality > 1){
					clause.setPlural(true);
				}
				phrase.setComplement(clause);
				
				noun = false;
			} else if(propertyVerbalization.isVerbType()){
				VPPhraseSpec verb = nlgFactory.createVerbPhrase(propertyVerbalization.getVerbalizationText());
				verb.addModifier("at least " + cardinality);
				phrase.setVerb(verb);
				
			
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectExactCardinality ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectMaxCardinality ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectHasSelf ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectOneOf ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLDataSomeValuesFrom ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLDataPropertyExpression property = ce.getProperty();
		OWLDataRange filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLDataProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				phrase.setVerb(propertyVerbalization.getVerbalizationText());
				
			
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLDataAllValuesFrom ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLDataHasValue ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLDataMinCardinality ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLDataExactCardinality ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLDataMaxCardinality ce) {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLIndividualVisitorEx#visit(org.semanticweb.owlapi.model.OWLNamedIndividual)
	 */
	@Override
	public NLGElement visit(OWLNamedIndividual individual) {
		return nlgFactory.createNounPhrase(getLexicalForm(individual));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLIndividualVisitorEx#visit(org.semanticweb.owlapi.model.OWLAnonymousIndividual)
	 */
	@Override
	public NLGElement visit(OWLAnonymousIndividual individual) {
		throw new UnsupportedOperationException("Conversion of anonymous individuals not support yet!");
	}
	

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatype)
	 */
	@Override
	public NLGElement visit(OWLDatatype node) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataOneOf)
	 */
	@Override
	public NLGElement visit(OWLDataOneOf node) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataComplementOf)
	 */
	@Override
	public NLGElement visit(OWLDataComplementOf node) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataIntersectionOf)
	 */
	@Override
	public NLGElement visit(OWLDataIntersectionOf node) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataUnionOf)
	 */
	@Override
	public NLGElement visit(OWLDataUnionOf node) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatypeRestriction)
	 */
	@Override
	public NLGElement visit(OWLDatatypeRestriction node) {
		return null;
	}
}
