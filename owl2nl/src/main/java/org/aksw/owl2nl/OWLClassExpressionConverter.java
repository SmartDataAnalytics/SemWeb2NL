/**
 * 
 */
package org.aksw.owl2nl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.aksw.triple2nl.converter.IRIConverter;
import org.aksw.triple2nl.converter.LiteralConverter;
import org.aksw.triple2nl.converter.SimpleIRIConverter;
import org.aksw.triple2nl.nlp.stemming.PlingStemmer;
import org.aksw.triple2nl.property.PropertyVerbalization;
import org.aksw.triple2nl.property.PropertyVerbalizer;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataCardinalityRestriction;
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
import org.semanticweb.owlapi.model.OWLFacetRestriction;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualVisitorEx;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNaryBooleanClassExpression;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
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
import org.semanticweb.owlapi.vocab.OWL2Datatype;
import org.semanticweb.owlapi.vocab.OWLFacet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import simplenlg.features.Feature;
import simplenlg.features.InternalFeature;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.framework.PhraseCategory;
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
	
	IRIConverter iriConverter = new SimpleIRIConverter();
	PropertyVerbalizer propertyVerbalizer = new PropertyVerbalizer(iriConverter, null, null);
	LiteralConverter literalConverter = new LiteralConverter(iriConverter);
	OWLDataFactory df = new OWLDataFactoryImpl(false, false);
	
	boolean noun;
	
	NLGElement object;
	NLGElement complement;
	
	int modalDepth;
	
	OWLClassExpression root;

	private boolean isSubClassExpression;

	private OWLClassExpression startClass;
	
	public OWLClassExpressionConverter(Lexicon lexicon) {
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public OWLClassExpressionConverter() {
		this(Lexicon.getDefaultLexicon());
	}
	
	public String convert(OWLClassExpression ce) {
		// process
		NLGElement nlgElement = asNLGElement(ce);

		// realise
		nlgElement = realiser.realise(nlgElement);
		
		return nlgElement.getRealisation();
	}
	
	public NLGElement asNLGElement(OWLClassExpression ce) {
		return asNLGElement(ce, false);
	}
	
	public NLGElement asNLGElement(OWLClassExpression ce, boolean isSubClassExpression) {
		this.root = ce;
		this.isSubClassExpression = isSubClassExpression;
		this.startClass = null;
		
		// reset modal depth
		modalDepth = 1;
		
		// rewrite class expression
		ce = rewrite(ce);

		// process
		NLGElement nlgElement = ce.accept(this);

		return nlgElement;
	}
	
	private String getLexicalForm(OWLEntity entity){
		return iriConverter.convert(entity.toStringID());
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
		return rewrite(ce, false);
	}
	
	private OWLClassExpression rewrite(OWLClassExpression ce, boolean inIntersection){
		if(!ce.isAnonymous()){
			return ce;
		} else if(ce instanceof OWLObjectOneOf){
			return ce;
		} else if(ce instanceof OWLObjectIntersectionOf){
			Set<OWLClassExpression> operands = ((OWLObjectIntersectionOf) ce).getOperands();
			Set<OWLClassExpression> newOperands = Sets.newHashSet();

			for (OWLClassExpression operand : operands) {
				newOperands.add(rewrite(operand, true));
			}
			
			if(!containsNamedClass(operands)){
				newOperands.add(df.getOWLThing());
			}
			
			return df.getOWLObjectIntersectionOf(newOperands);
		} else if(ce instanceof OWLObjectUnionOf){
			Set<OWLClassExpression> operands = ((OWLObjectUnionOf) ce).getOperands();
			Set<OWLClassExpression> newOperands = Sets.newHashSet();
			
			for (OWLClassExpression operand : operands) {
				newOperands.add(rewrite(operand));
			}
			
			return df.getOWLObjectUnionOf(newOperands);
		} else if(ce instanceof OWLObjectSomeValuesFrom){
			OWLClassExpression newCe = df.getOWLObjectSomeValuesFrom(((OWLObjectSomeValuesFrom) ce).getProperty(), rewrite(((OWLObjectSomeValuesFrom) ce).getFiller()));
			if(inIntersection){
				return newCe;
			}
			return df.getOWLObjectIntersectionOf(
					df.getOWLThing(),
					newCe);
		} else if(ce instanceof OWLObjectAllValuesFrom){
			OWLClassExpression newCe = df.getOWLObjectAllValuesFrom(((OWLObjectAllValuesFrom) ce).getProperty(), rewrite(((OWLObjectAllValuesFrom) ce).getFiller()));
			if(inIntersection){
				return newCe;
			}
			return df.getOWLObjectIntersectionOf(
					df.getOWLThing(),
					newCe);
		}
		if(inIntersection){
			return ce;
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
		noun = true;
		
		// get the lexical form
		String lexicalForm  = getLexicalForm(ce).toLowerCase();
		
		// we always start with the singular form and if necessary pluralize later
		lexicalForm = PlingStemmer.stem(lexicalForm);
		
		
		if(isSubClassExpression){// subclass expression
			if(ce.isOWLThing()){
				if(modalDepth == 1){
					NLGElement word = nlgFactory.createWord("everything", LexicalCategory.NOUN);
					word.setFeature(InternalFeature.NON_MORPH, true);
					return nlgFactory.createNounPhrase(word);
				} else {
					NLGElement word = nlgFactory.createWord("something", LexicalCategory.NOUN);
					word.setFeature(InternalFeature.NON_MORPH, true);
					return nlgFactory.createNounPhrase(word);
				}
			}
			NPPhraseSpec nounPhrase = nlgFactory.createNounPhrase(lexicalForm);
			if(modalDepth > 1 && !ce.equals(root)){
				nounPhrase.setDeterminer("a"); 
			} else {
				nounPhrase.setPreModifier("every");
			}
			return nounPhrase;
		} else {// superclass expression
			if(ce.isOWLThing()){
				return nlgFactory.createNounPhrase("something");
			}
			return nlgFactory.createNounPhrase("a", lexicalForm);
		}
	}

	@Override
	public NLGElement visit(OWLObjectIntersectionOf ce) {
		List<OWLClassExpression> operands = getOperandsByPriority(ce);
		
		// process first class
		OWLClassExpression first = operands.remove(0);
		SPhraseSpec phrase = nlgFactory.createClause();
		NPPhraseSpec firstElement = (NPPhraseSpec) first.accept(this);
		phrase.setSubject(firstElement);
		startClass = first;
		
		if(operands.size() >= 2){
			CoordinatedPhraseElement cc = nlgFactory.createCoordinatedPhrase();
			
			// process the classes
			Iterator<OWLClassExpression> iterator = operands.iterator();
			List<OWLClass> classes = new ArrayList<>();
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
				if(operand instanceof OWLObjectSomeValuesFrom && ((OWLObjectSomeValuesFrom) operand).getFiller().isOWLThing()){
//					clause.setFeature(Feature.COMPLEMENTISER, "that");
					clause.setVerb("have");
					clause.setObject(el);
				} else {
					clause.setFeature(Feature.COMPLEMENTISER, "whose");
					clause.setVerbPhrase(el);
				}
				
			} else {
				clause.setFeature(Feature.COMPLEMENTISER, "that");
				clause.setVerbPhrase(el);
			}
			phrase.setComplement(clause);
		}
		
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
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

		logger.debug(ce +  " = " + realiser.realise(cc));
		
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
		
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectSomeValuesFrom ce) {
		modalDepth++;
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			String verbalizationText = propertyVerbalization.getVerbalizationText();
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(verbalizationText));
				
				if(filler.isOWLThing()){
					propertyNounPhrase.setDeterminer("a");
					return propertyNounPhrase;
				}
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = filler.accept(this);
				fillerElement.setPlural(false);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
//				phrase.setVerb(propertyVerbalization.getVerbalizationText());
				
				String[] posTags = propertyVerbalization.getPOSTags().split(" ");
				String firstTag = posTags[0];
				String secondTag = posTags[1];
				
				if(firstTag.startsWith("V") && secondTag.startsWith("N")){
//				if(tokens[0].equals("has") || tokens[0].equals("have")){
					String[] tokens = verbalizationText.split(" ");
					
					verbalizationText = tokens[0];
					
					if(!filler.isOWLThing()){
						verbalizationText += " as";
					} else {
						verbalizationText += " a";
					}
					
					// stem the noun
					// TODO to absolutely correct we have to stem the noun phrase
					String nounToken = tokens[1];
					nounToken = PlingStemmer.stem(nounToken);
					verbalizationText += " " + nounToken;
					
					// append rest of the tokens
					for (int i = 2; i < tokens.length; i++) {
						verbalizationText += " " + tokens[i];
					}
					verbalizationText = verbalizationText.trim();
					
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					phrase.setVerb(verb);
					
					if(!filler.isOWLThing()){
						NLGElement fillerElement = filler.accept(this);
						phrase.setObject(fillerElement);
						fillerElement.setFeature(Feature.COMPLEMENTISER, null);
					}
				} else {
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					phrase.setVerb(verb);
					
					NLGElement fillerElement = filler.accept(this);
					phrase.setObject(fillerElement);
					fillerElement.setFeature(Feature.COMPLEMENTISER, null);
				}
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		modalDepth--;
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectAllValuesFrom ce) {
		modalDepth++;
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			String verbalizationText = propertyVerbalization.getVerbalizationText();
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				String[] posTags = propertyVerbalization.getPOSTags().split(" ");
				String firstTag = posTags[0];
				String secondTag = posTags[1];
				
				if(firstTag.startsWith("V") && secondTag.startsWith("N")){
//				if(tokens[0].equals("has") || tokens[0].equals("have")){
					String[] tokens = verbalizationText.split(" ");
					
					verbalizationText = tokens[0];
					
					if(!filler.isOWLThing()){
						verbalizationText += " as";
					} else {
						verbalizationText += " a";
					}
					
					// stem the noun
					// TODO to be absolutely correct, we have to stem the noun phrase
					String nounToken = tokens[1];
					nounToken = PlingStemmer.stem(nounToken);
					verbalizationText += " " + nounToken;
					
					// append rest of the tokens
					for (int i = 2; i < tokens.length; i++) {
						verbalizationText += " " + tokens[i];
					}
					verbalizationText += " only";
					verbalizationText = verbalizationText.trim();
					
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					phrase.setVerb(verb);
					
					if(!filler.isOWLThing()){
						NLGElement fillerElement = filler.accept(this);
						phrase.setObject(fillerElement);
						fillerElement.setFeature(Feature.COMPLEMENTISER, null);
					}
				} else {
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					verb.addModifier("only");
					phrase.setVerb(verb);

					NLGElement fillerElement = filler.accept(this);
					phrase.setObject(fillerElement);
					fillerElement.setFeature(Feature.COMPLEMENTISER, null);
				}
				
				noun = false;
			} else {
				
			}
		} else {
			
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		modalDepth--;
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
		return processObjectCardinalityRestriction(ce);
	}
	
	@Override
	public NLGElement visit(OWLObjectMaxCardinality ce) {
		return processObjectCardinalityRestriction(ce);
	}

	@Override
	public NLGElement visit(OWLObjectExactCardinality ce) {
		return processObjectCardinalityRestriction(ce);
	}
	
	private NLGElement processObjectCardinalityRestriction(OWLObjectCardinalityRestriction ce){
		SPhraseSpec phrase = nlgFactory.createClause();
		String modifier;
		if(ce instanceof OWLObjectMinCardinality){
			modifier = "at least";
		} else if(ce instanceof OWLObjectMaxCardinality){
			modifier = "at most";
		} else {
			modifier = "exactly";
		}
		
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		modifier += " " + cardinality;
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLObjectProperty().getIRI().toString());
			
			if(propertyVerbalization.isNounType()){ // if the verbalization of the property is a noun phrase
				NLGElement word = nlgFactory.createWord(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()), LexicalCategory.NOUN);
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(word);
				if(cardinality > 1){
					word.setPlural(true);
					propertyNounPhrase.setPlural(true);
				}
				VPPhraseSpec verb = nlgFactory.createVerbPhrase("have");
				verb.addModifier(modifier);
				
				phrase.setVerb(verb);
				phrase.setObject(propertyNounPhrase);
				
				
				NLGElement fillerElement = filler.accept(this);
				
				SPhraseSpec clause = nlgFactory.createClause();
				if(cardinality > 1){
					clause.setPlural(true);
				}
				clause.setVerb("be");
				clause.setObject(fillerElement);
				if(fillerElement.isA(PhraseCategory.CLAUSE)){
					fillerElement.setFeature(Feature.COMPLEMENTISER, null);
				}
				
				phrase.setComplement(clause);
				
				noun = false;
			} else if(propertyVerbalization.isVerbType()){ // if the verbalization of the property is a verb phrase
				
				String verbalizationText = propertyVerbalization.getVerbalizationText();
				
				/* here comes actually one of the most tricky parts
				   Normally, we just use the verbalization as verb, and add a modifier like 'at least n' which works
				   good for phrase like 'works for', such that we get 'works for at least n'.
				   But, if we have something like 'has gender' it would result in a strange construct
				   'has gender at least n', although it sounds more natural to have 'has at least n gender'
				   We could make use of POS tags to find such cases, although it might be more complex for longer
				   phrases.
				   It's not really clear for which verbs the rules holds, but 'has','include','contains',etc. might be a good
				   starting point.
				*/
				String[] posTags = propertyVerbalization.getPOSTags().split(" ");
				String firstTag = posTags[0];
				String secondTag = posTags[1];
				
				if(firstTag.startsWith("V") && secondTag.startsWith("N")){
//				if(tokens[0].equals("has") || tokens[0].equals("have")){
					String[] tokens = verbalizationText.split(" ");
					
					verbalizationText = tokens[0];
					verbalizationText += " " + modifier;
					
					// stem the noun if card == 1
					String nounToken = tokens[1];
					if(cardinality == 1){
						nounToken = PlingStemmer.stem(nounToken);
					}
					verbalizationText += " " + nounToken;
					for (int i = 2; i < tokens.length; i++) {
						verbalizationText += " " + tokens[i];
					}
					verbalizationText = verbalizationText.trim();
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					phrase.setVerb(verb);
				} else {
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(verbalizationText);
					verb.addModifier(modifier);
					phrase.setVerb(verb);
				}
				
				if(!filler.isOWLThing()){
					NLGElement fillerElement = filler.accept(this);
					if(cardinality > 1){
						fillerElement.setPlural(true);
					}
					phrase.setObject(fillerElement);
				}
				noun = false;
			} else {
				
			}
		} else {
			
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		
		return phrase;
	}

	@Override
	public NLGElement visit(OWLObjectHasSelf ce) {
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectOneOf ce) {
		// if it contains more than one value, i.e. oneOf(v1_,...,v_n) with n > 1, we rewrite it as unionOf(oneOf(v_1),...,oneOf(v_n)) 
		Set<OWLIndividual> individuals = ce.getIndividuals();

		if (individuals.size() > 1) {
			Set<OWLClassExpression> operands = new HashSet<>(individuals.size());
			for (OWLIndividual ind : individuals) {
				operands.add(df.getOWLObjectOneOf(ind));
			}
			return df.getOWLObjectUnionOf(operands).accept(this);
		}
		return individuals.iterator().next().accept(this);
	}

	@Override
	public NLGElement visit(OWLDataSomeValuesFrom ce) {
		modalDepth++;
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLDataPropertyExpression property = ce.getProperty();
		OWLDataRange filler = ce.getFiller();
		
		if(!property.isAnonymous()){
			
			if(!filler.isTopDatatype()){
				NLGElement fillerElement = filler.accept(this);
				phrase.setObject(fillerElement);
			}
			
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLDataProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()));
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("be");
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				phrase.setVerb(propertyVerbalization.getVerbalizationText());
				
				noun = false;
			} else {
				
			}
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		modalDepth--;
		return phrase;
	}

	@Override
	public NLGElement visit(OWLDataAllValuesFrom ce) {
		modalDepth++;
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
				
				if(filler.isDatatype() && filler.asOWLDatatype().getIRI().equals(OWL2Datatype.XSD_BOOLEAN.getIRI())){
					// "either VERB or not"
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(propertyVerbalization.getVerbalizationText());
					phrase.setVerb(verb);
					verb.addFrontModifier("either");
					verb.addPostModifier("or not");
				} else {
					VPPhraseSpec verb = nlgFactory.createVerbPhrase(propertyVerbalization.getVerbalizationText());
					verb.addModifier("only");
					phrase.setVerb(verb);
					
					NLGElement fillerElement = filler.accept(this);
					phrase.setObject(fillerElement);
				}
				
				noun = false;
			} else {
				
			}
			
			
		} else {
			//TODO handle inverse properties
		}
		logger.debug(ce +  " = " + realiser.realise(phrase));
		modalDepth--;
		return phrase;
	}

	@Override
	public NLGElement visit(OWLDataHasValue ce) {
		SPhraseSpec phrase = nlgFactory.createClause();
		
		OWLDataPropertyExpression property = ce.getProperty();
		OWLLiteral value = ce.getValue();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLDataProperty().getIRI().toString());
			String verbalizationText = propertyVerbalization.getVerbalizationText();
			if(propertyVerbalization.isNounType()){
//				verbalizationText = PlingStemmer.stem(verbalizationText);
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(verbalizationText);
				phrase.setSubject(propertyNounPhrase);
				
				phrase.setVerb("is");
				
				NLGElement valueElement = nlgFactory.createNounPhrase(literalConverter.convert(value));
				phrase.setObject(valueElement);
				
				noun = true;
			} else if(propertyVerbalization.isVerbType()){
				// if phrase starts with something like 'is' and value is a Boolean
				String[] tokens = verbalizationText.split(" ");
				if(value.getDatatype().isBoolean() && tokens[0].equals("is")){
					if(!value.parseBoolean()){
						phrase.setFeature(Feature.NEGATED, true);
					}
				} else {
					NLGElement valueElement = nlgFactory.createNounPhrase(literalConverter.convert(value));
					phrase.setObject(valueElement);
				}
				
				phrase.setVerb(verbalizationText);
				
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
	public NLGElement visit(OWLDataMinCardinality ce) {
		return processDataCardinalityRestriction(ce);
	}

	@Override
	public NLGElement visit(OWLDataExactCardinality ce) {
		return processDataCardinalityRestriction(ce);
	}

	@Override
	public NLGElement visit(OWLDataMaxCardinality ce) {
		return processDataCardinalityRestriction(ce);
	}
	
	private NLGElement processDataCardinalityRestriction(OWLDataCardinalityRestriction ce){
		SPhraseSpec phrase = nlgFactory.createClause();
		String modifier;
		if(ce instanceof OWLDataMinCardinality){
			modifier = "at least";
		} else if(ce instanceof OWLDataMaxCardinality){
			modifier = "at most";
		} else {
			modifier = "exactly";
		}
		
		OWLDataPropertyExpression property = ce.getProperty();
		OWLDataRange filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		if(!property.isAnonymous()){
			PropertyVerbalization propertyVerbalization = propertyVerbalizer.verbalize(property.asOWLDataProperty().getIRI().toString());
			if(propertyVerbalization.isNounType()){
				NLGElement word = nlgFactory.createWord(PlingStemmer.stem(propertyVerbalization.getVerbalizationText()), LexicalCategory.NOUN);
				NPPhraseSpec propertyNounPhrase = nlgFactory.createNounPhrase(word);
				if(cardinality > 1){
					word.setPlural(true);
					propertyNounPhrase.setPlural(true);
				}
				VPPhraseSpec verb = nlgFactory.createVerbPhrase("have");
				verb.addModifier(modifier + " " + cardinality);
				
				phrase.setVerb(verb);
				phrase.setObject(propertyNounPhrase);
				
				
				NLGElement fillerElement = filler.accept(this);
				
				SPhraseSpec clause = nlgFactory.createClause();
				if(cardinality > 1){
					clause.setPlural(true);
				}
				clause.setVerb("be");
				clause.setObject(fillerElement);
				if(fillerElement.isA(PhraseCategory.CLAUSE)){
					fillerElement.setFeature(Feature.COMPLEMENTISER, null);
				}
				
				phrase.setComplement(clause);
				
				noun = false;
			} else if(propertyVerbalization.isVerbType()){
				VPPhraseSpec verb = nlgFactory.createVerbPhrase(propertyVerbalization.getVerbalizationText());
				verb.addModifier(modifier + " " + cardinality);
				phrase.setVerb(verb);
				
			
				NLGElement fillerElement = filler.accept(this);
				fillerElement.setPlural(true);
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
		throw new UnsupportedOperationException("Convertion of anonymous individuals not supported yet!");
	}
	

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatype)
	 */
	@Override
	public NLGElement visit(OWLDatatype node) {
		return nlgFactory.createNounPhrase(getLexicalForm(node));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataOneOf)
	 */
	@Override
	public NLGElement visit(OWLDataOneOf node) {
		// if it contains more than one value, i.e. oneOf(v1_,...,v_n) with n > 1, we rewrite it as unionOf(oneOf(v_1),...,oneOf(v_n)) 
		Set<OWLLiteral> values = node.getValues();
		
		if(values.size() > 1){
			Set<OWLDataRange> operands = new HashSet<>(values.size());
			for (OWLLiteral value : values) {
				operands.add(df.getOWLDataOneOf(value));
			}
			return df.getOWLDataUnionOf(operands).accept(this);
		}
		return nlgFactory.createNounPhrase(literalConverter.convert(values.iterator().next()));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataComplementOf)
	 */
	@Override
	public NLGElement visit(OWLDataComplementOf node) {
		NLGElement nlgElement = node.getDataRange().accept(this);
		nlgElement.setFeature(Feature.NEGATED, true);
		return nlgElement;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataIntersectionOf)
	 */
	@Override
	public NLGElement visit(OWLDataIntersectionOf node) {
		CoordinatedPhraseElement cc = nlgFactory.createCoordinatedPhrase();
		
		for(OWLDataRange op : node.getOperands()) {
			cc.addCoordinate(op.accept(this));
		}
		
		return cc;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDataUnionOf)
	 */
	@Override
	public NLGElement visit(OWLDataUnionOf node) {
		CoordinatedPhraseElement cc = nlgFactory.createCoordinatedPhrase();
		cc.setConjunction("or");
		
		for(OWLDataRange op : node.getOperands()) {
			cc.addCoordinate(op.accept(this));
		}
		
		return cc;
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.model.OWLDataRangeVisitorEx#visit(org.semanticweb.owlapi.model.OWLDatatypeRestriction)
	 */
	@Override
	public NLGElement visit(OWLDatatypeRestriction node) {
		Set<OWLFacetRestriction> facetRestrictions = node.getFacetRestrictions();

		List<NPPhraseSpec> phrases = new ArrayList<>(facetRestrictions.size());

		for(OWLFacetRestriction facetRestriction : facetRestrictions) {
			OWLFacet facet = facetRestriction.getFacet();
			OWLLiteral value = facetRestriction.getFacetValue();
			
			String valueString = value.getLiteral();
			
			String keyword = facet.toString();
			switch(facet) {
				case LENGTH: keyword = "STRLEN(STR(%s) = %d)";
					break;
				case MIN_LENGTH: keyword = "STRLEN(STR(%s) >= %d)";
					break;
				case MAX_LENGTH: keyword = "STRLEN(STR(%s) <= %d)";
					break;
				case PATTERN: keyword = "REGEX(STR(%s), %d)";
					break;
				case LANG_RANGE:
					break;
				case MAX_EXCLUSIVE: keyword = "lower than";
					break;
				case MAX_INCLUSIVE: keyword = "lower than or equals to";
					break;
				case MIN_EXCLUSIVE: keyword = "greater than";
					break;
				case MIN_INCLUSIVE: keyword = "greater than or equals to";
					break;
				case FRACTION_DIGITS:
					break;
				case TOTAL_DIGITS:
					break;
				default:
					break;
			
			}

			phrases.add(nlgFactory.createNounPhrase(keyword + " " + valueString));
		}

		if(phrases.size() > 1) {
			CoordinatedPhraseElement coordinatedPhrase = nlgFactory.createCoordinatedPhrase();
			for (NPPhraseSpec phrase : phrases) {
				coordinatedPhrase.addCoordinate(phrase);
			}
			return coordinatedPhrase;
		}

		return phrases.get(0);
	}
}
