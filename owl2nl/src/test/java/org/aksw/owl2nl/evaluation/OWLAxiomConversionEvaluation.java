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
package org.aksw.owl2nl.evaluation;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.aksw.owl2nl.OWLAxiomConverter;
import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;
import org.dllearner.utilities.owl.ManchesterOWLSyntaxOWLObjectRendererImplExt;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLObjectRenderer;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Lorenz Buehmann
 *         created on 11/4/15
 */
public class OWLAxiomConversionEvaluation {

	//static String ontologyURL = "https://raw.githubusercontent.com/pezra/pretty-printer/master/Jenna-2.6.3/testing/ontology/bugs/koala.owl";
	static String ontologyURL = "http://protege.cim3.net/file/pub/ontologies/travel/travel.owl";


	public static void main(String[] args) throws Exception{
		OWLObjectRenderer renderer = new ManchesterOWLSyntaxOWLObjectRendererImplExt();

		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		OWLOntology ontology = man.loadOntology(IRI.create(ontologyURL));

		List<List<String>> data = new ArrayList<>();

		OWLAxiomConverter converter = new OWLAxiomConverter();
		int i = 1;
		for (OWLAxiom axiom : ontology.getAxioms()) {
			String s = converter.convert(axiom);

			if(s != null) {
				List<String> rowData = new ArrayList<>();
				rowData.add(String.valueOf(i++));

				String renderedAxiom = renderer.render(axiom);
				for (ManchesterOWLSyntax keyword : ManchesterOWLSyntax.values()) {
					if(keyword.isAxiomKeyword() || keyword.isClassExpressionConnectiveKeyword() || keyword.isClassExpressionQuantiferKeyword()) {
						String regex = "\\s?(" + keyword.keyword() + "|" + keyword.toString() + ")(\\s|:)";
						renderedAxiom = renderedAxiom.replaceAll(regex, " <b>" + keyword.keyword() + "</b> ");
					}
				}
				rowData.add(renderedAxiom);
				rowData.add(s);

				data.add(rowData);
			}
		}

		String htmlTable = HTMLTableGenerator.generateHTMLTable(Lists.newArrayList("ID", "Axiom", "NL"), data);
		Files.write(htmlTable, new File("/tmp/axiomConversionResults.html"), Charsets.UTF_8);
	}
}
