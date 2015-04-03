package org.aksw.semweb2nl.evaluation;

import java.util.Random;

import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLLiteral;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class DataValueGenerator {

	private static final OWLDataFactory df = new OWLDataFactoryImpl();
	private static final Random rnd = new Random(123);
	private static final int rangeMax = -100;
	private static final int rangeMin = 100;

	public static OWLLiteral generateValue(OWLDataRange dataRange) {
		if (dataRange.isDatatype()) {
			OWLDatatype datatype = dataRange.asOWLDatatype();

			if (datatype.isInteger()) {
				return df.getOWLLiteral(rnd.nextInt());
			} else if (datatype.isDouble()) {
				return df.getOWLLiteral(rangeMin + (rangeMax - rangeMin) * rnd.nextDouble());
			} else if (datatype.isBoolean()) {
				return df.getOWLLiteral(rnd.nextBoolean());
			}
		}

		throw new UnsupportedOperationException("Data range " + dataRange + " not supported yet.");
	}
}