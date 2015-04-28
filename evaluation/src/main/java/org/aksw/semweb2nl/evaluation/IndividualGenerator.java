package org.aksw.semweb2nl.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.util.IRIShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.OWLClassImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import com.google.common.collect.HashMultiset;

public class IndividualGenerator {

	private static final String NAMESPACE = "http://semweb2nl.aksw.org/";
	private static final OWLClass GENERIC_CLASS = new OWLClassImpl(IRI.create(NAMESPACE + "someInd"));

	private OWLDataFactory df = new OWLDataFactoryImpl();
	private HashMultiset<OWLClass> classUsage = HashMultiset.create();

	private final IRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	// the simple form of naming, just a,b,c, ...
	char indCharacter = 'a';

	/**
	 * Generate a synthetic individual for the given class.
	 * 
	 * @param cls
	 */
	public OWLIndividual generateIndividual(OWLClassExpression ce) {
		if (ce instanceof OWLObjectOneOf) { // {a1,...,an} -> choose one random value
			List<OWLIndividual> individuals = new ArrayList<OWLIndividual>(((OWLObjectOneOf) ce).getIndividuals());
			Collections.shuffle(individuals, new Random(123));
			return individuals.get(0);
		} else if (ce instanceof OWLObjectIntersectionOf) {
			for (OWLClassExpression operand : ((OWLObjectIntersectionOf) ce).getOperands()) {
				if (!operand.isAnonymous() && !operand.isOWLThing()) {
					return generateIndividual(operand.asOWLClass());
				}
			}
		}
		return generateIndividual(GENERIC_CLASS);
	}

	public OWLIndividual generateIndividual(OWLClass cls) {
		//			int index = classUsage.count(cls.asOWLClass());
		//			classUsage.add(cls);
		//			IRI indIRI = IRI.create(NAMESPACE + sfp.getShortForm(cls.getIRI()) + index);
		//			return df.getOWLNamedIndividual(indIRI);

		return df.getOWLNamedIndividual(IRI.create(NAMESPACE + String.valueOf(indCharacter++)));
	}

	public void reset() {
		indCharacter = 'a';
	}
}