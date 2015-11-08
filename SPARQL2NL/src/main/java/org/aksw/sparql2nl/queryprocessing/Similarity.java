/*
 * #%L
 * SPARQL2NL
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
package org.aksw.sparql2nl.queryprocessing;

import org.aksw.sparql2nl.similarity.NormedGraphIsomorphism;
import org.aksw.sparql2nl.similarity.TypeAwareGraphIsomorphism;

import simpack.measure.external.simmetrics.JaccardSimilarity;
import simpack.measure.external.simmetrics.Levenshtein;
import simpack.measure.external.simmetrics.QGramsDistance;

/**
 * Computes the similarity between two queries
 * @author ngonga
 */
public class Similarity {

	public enum SimilarityMeasure{
		LEVENSHTEIN, QGRAMS, JACCARD, GRAPH_ISOMORPHY, TYPE_AWARE_ISOMORPHY
	}

    public static double getSimilarity(Query q1, Query q2, SimilarityMeasure measure)
    {
        //check whether queries use the same features
        //if they don't return 0
        if(q1.getUsesCount() != q2.getUsesCount() ||
                q1.getUsesGroupBy() != q2.getUsesGroupBy() ||
                q1.getUsesLimit() != q2.getUsesLimit() ||
                q1.getUsesSelect() != q2.getUsesSelect()) return 0;
        //if they do then compute similarity
        if(measure == SimilarityMeasure.LEVENSHTEIN)
            return (new Levenshtein(q1.getQueryWithOnlyVars(), q2.getQueryWithOnlyVars()).getSimilarity());
        else if(measure == SimilarityMeasure.QGRAMS)
            return (new QGramsDistance(q1.getQueryWithOnlyVars(), q2.getQueryWithOnlyVars()).getSimilarity());
        else if(measure == SimilarityMeasure.JACCARD)
            return (new JaccardSimilarity(q1.getQueryWithOnlyVars(), q2.getQueryWithOnlyVars()).getSimilarity());
        //add graph based-metric here
        else if(measure == SimilarityMeasure.GRAPH_ISOMORPHY)
            return (new NormedGraphIsomorphism().getSimilarity(q1, q2));
        else if(measure == SimilarityMeasure.TYPE_AWARE_ISOMORPHY)
            return (new TypeAwareGraphIsomorphism().getSimilarity(q1, q2));
        //default
        return (new Levenshtein(q1.getQueryWithOnlyVars(), q2.getQueryWithOnlyVars()).getSimilarity());
    }
}
