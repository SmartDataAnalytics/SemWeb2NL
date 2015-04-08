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
