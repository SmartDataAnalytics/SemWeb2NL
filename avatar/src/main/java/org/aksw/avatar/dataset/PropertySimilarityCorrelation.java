/*
 * #%L
 * AVATAR
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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.dataset;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObjectProperty;

import uk.ac.shef.wit.simmetrics.similaritymetrics.QGramsDistance;

import com.google.common.collect.Sets;

/**
 *
 * @author ngonga
 */
public class PropertySimilarityCorrelation {

    public static Map<Set<OWLObjectProperty>, Double> getCooccurrences(OWLClass cls, Set<OWLObjectProperty> properties) {
        return getCooccurrences(cls, properties, 0d);
    }

    public static Map<Set<OWLObjectProperty>, Double> getCooccurrences(OWLClass cls, Set<OWLObjectProperty> properties, double threshold) {
        Map<Set<OWLObjectProperty>, Double> pair2similarity = new HashMap<Set<OWLObjectProperty>, Double>();
       
        QGramsDistance qgrams = new QGramsDistance();
        for (OWLObjectProperty prop1 : properties) {
            for (OWLObjectProperty prop2 : properties) {
                Set<OWLObjectProperty> pair = Sets.newHashSet(prop1, prop2);
                if (!pair2similarity.containsKey(pair) && !prop1.equals(prop2)) {
                    double similarity = qgrams.getSimilarity(prop1.toStringID().substring(prop1.toStringID().lastIndexOf("/") + 1), prop2.toStringID().substring(prop2.toStringID().lastIndexOf("/") + 1));
                    if (similarity >= threshold) {
                        pair2similarity.put(pair, similarity);
                    }
                }
            }
        }
        return pair2similarity;
    }
}
