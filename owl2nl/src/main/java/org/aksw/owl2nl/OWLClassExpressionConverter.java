/**
 * 
 */
package org.aksw.owl2nl;

import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitorEx;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLIndividual;
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
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

/**
 * @author Lorenz Buehmann
 *
 */
public class OWLClassExpressionConverter implements OWLClassExpressionVisitorEx<NLGElement>{

	NLGFactory nlgFactory;
	Realiser realiser;
	
	IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	
	public OWLClassExpressionConverter(Lexicon lexicon) {
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public OWLClassExpressionConverter() {
		this(Lexicon.getDefaultLexicon());
	}
	
	public void convert(OWLClassExpression ce){
		ce.accept(this);
	}
	
	private String convert(OWLEntity entity){
		return sfp.getShortForm(entity.getIRI());
	}
	
	private OWLClassExpression rewrite(OWLClassExpression ce){
		return ce;
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
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectIntersectionOf ce) {
		for (OWLClassExpression op : getOperandsByPriority(ce)) {
			
		}
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectUnionOf ce) {
		for (OWLClassExpression op : getOperandsByPriority(ce)) {
			
		}
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectComplementOf ce) {
		OWLClassExpression op = ce.getOperand();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectSomeValuesFrom ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectAllValuesFrom ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectHasValue ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLIndividual value = ce.getValue();
		
		return null;
	}

	@Override
	public NLGElement visit(OWLObjectMinCardinality ce) {
		OWLObjectPropertyExpression property = ce.getProperty();
		OWLClassExpression filler = ce.getFiller();
		int cardinality = ce.getCardinality();
		
		return null;
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
		return null;
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
	
	public static void main(String[] args) throws Exception {
		OWLDataFactoryImpl df = new OWLDataFactoryImpl(false, false);
		PrefixManager pm = new DefaultPrefixManager("http://dbpedia.org/ontology/");
		OWLObjectProperty p1 = df.getOWLObjectProperty("birthPlace", pm);
		OWLObjectProperty p2 = df.getOWLObjectProperty("worksFor", pm);
		OWLClass cls1 = df.getOWLClass("Place", pm);
		OWLClassExpression ce = df.getOWLObjectAllValuesFrom(p1, cls1);
	}
}
