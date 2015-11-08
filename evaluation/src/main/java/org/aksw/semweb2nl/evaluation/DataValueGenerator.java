/*
 * #%L
 * Evaluation
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