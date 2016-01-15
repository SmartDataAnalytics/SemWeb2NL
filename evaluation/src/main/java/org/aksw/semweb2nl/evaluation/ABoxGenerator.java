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

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLIndividualAxiom;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter;

import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class ABoxGenerator extends OWLClassExpressionVisitorAdapter {
	private OWLDataFactory df = new OWLDataFactoryImpl();
	private Set<OWLIndividualAxiom> axioms = new HashSet<OWLIndividualAxiom>();
	private IndividualGenerator individualGenerator;
	// the stack of individuals which are the current subject we are talking about
	private Deque<OWLIndividual> individuals = new ArrayDeque<OWLIndividual>();

	public ABoxGenerator(IndividualGenerator individualGenerator) {
		this.individualGenerator = individualGenerator;
	}

	/**
	 * Generate the minimal number of facts such that the given individual
	 * belongs to the
	 * given class expression C. This makes it more simple to provide a
	 * description
	 * for a negative example, i.e. an individual that does not belong to C.
	 */
	public Set<OWLIndividualAxiom> generateInstanceData(OWLIndividual ind, OWLClassExpression expr) {
		reset();

		individuals.push(ind);

		expr.accept(this);

		return axioms;
	}

	private void reset() {
		axioms = new HashSet<OWLIndividualAxiom>();
		individuals.clear();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLClass)
	 */
	@Override
	public void visit(OWLClass desc) {
		if (!desc.isBuiltIn()) {
			axioms.add(df.getOWLClassAssertionAxiom(desc, individuals.peek()));
		}
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectIntersectionOf)
	 */
	@Override
	public void visit(OWLObjectIntersectionOf ce) {
		List<OWLClassExpression> operands = ce.getOperandsAsList();
		// we have to process \exists r.T before \forall r.C 
		Collections.sort(operands, new Comparator<OWLClassExpression>() {
			@Override
			public int compare(OWLClassExpression o1, OWLClassExpression o2) {
				if (o1.getClassExpressionType() == ClassExpressionType.OBJECT_ALL_VALUES_FROM
						&& o2.getClassExpressionType() != ClassExpressionType.OBJECT_ALL_VALUES_FROM) {
					return 1;
				} else if (o1.getClassExpressionType() != ClassExpressionType.OBJECT_ALL_VALUES_FROM
						&& o2.getClassExpressionType() == ClassExpressionType.OBJECT_ALL_VALUES_FROM) {
					return -11;
				}
				return o1.compareTo(o2);
			}

		});
		for (OWLClassExpression operand : operands) {
			operand.accept(this);
		}
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom)
	 */
	@Override
	public void visit(OWLObjectSomeValuesFrom ce) {
		// given \exists r.C

		OWLIndividual subject = individuals.peek();

		// create new object
		OWLIndividual object = individualGenerator.generateIndividual(ce.getFiller());

		// add r(a,b)
		axioms.add(df.getOWLObjectPropertyAssertionAxiom(ce.getProperty(), subject, object));

		// process C(b)
		individuals.push(object);
		ce.getFiller().accept(this);
		individuals.poll();
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLDataSomeValuesFrom)
	 */
	@Override
	public void visit(OWLDataSomeValuesFrom ce) {
		// given \exists r.dt
		// add r(a,val^^dt)
		axioms.add(df.getOWLDataPropertyAssertionAxiom(ce.getProperty(), individuals.peek(),
				DataValueGenerator.generateValue(ce.getFiller())));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectHasValue)
	 */
	@Override
	public void visit(OWLObjectHasValue ce) {
		// given \exists r.{x}, add r(a,x)
		axioms.add(df.getOWLObjectPropertyAssertionAxiom(ce.getProperty(), individuals.peek(), ce.getFiller()));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLDataHasValue)
	 */
	@Override
	public void visit(OWLDataHasValue ce) {
		// given \exists r.{x}, add r(a,x)
		axioms.add(df.getOWLDataPropertyAssertionAxiom(ce.getProperty(), individuals.peek(), ce.getFiller()));
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectAllValuesFrom)
	 */
	@Override
	public void visit(OWLObjectAllValuesFrom ce) {
		// \forall r.C -> if C != owl:Thing we add C(b_j) for each r(a_i, b_j)
		if (!ce.isOWLThing()) {
			for (OWLIndividualAxiom axiom : new HashSet<>(axioms)) {
				if (axiom instanceof OWLObjectPropertyAssertionAxiom) {
					if (((OWLObjectPropertyAssertionAxiom) axiom).getProperty().equals(ce.getProperty())) {
						OWLIndividual object = ((OWLObjectPropertyAssertionAxiom) axiom).getObject();
						individuals.push(object);
						ce.getFiller().accept(this);
						individuals.poll();
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectMaxCardinality)
	 */
	@Override
	public void visit(OWLObjectMaxCardinality ce) {
		processObjectCardinality(ce);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectMaxCardinality)
	 */
	@Override
	public void visit(OWLObjectMinCardinality ce) {
		processObjectCardinality(ce);
	}

	/* (non-Javadoc)
	 * @see org.semanticweb.owlapi.util.OWLClassExpressionVisitorAdapter#visit(org.semanticweb.owlapi.model.OWLObjectMaxCardinality)
	 */
	@Override
	public void visit(OWLObjectExactCardinality ce) {
		processObjectCardinality(ce);
	}

	private void processObjectCardinality(OWLObjectCardinalityRestriction ce) {
		// given \exists r.C

		OWLIndividual subject = individuals.peek();

		int cardinality = ce.getCardinality();

		for (int i = 0; i < cardinality; i++) {
			// create new object
			OWLIndividual object = individualGenerator.generateIndividual(ce.getFiller());

			// add r(a,b)
			axioms.add(df.getOWLObjectPropertyAssertionAxiom(ce.getProperty(), subject, object));

			// process C(b)
			individuals.push(object);
			ce.getFiller().accept(this);
			individuals.poll();
		}
	}
}