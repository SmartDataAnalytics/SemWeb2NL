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
package org.aksw.avatar.clustering.hardening;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;

/**
 * Hardening that prefers clusters with smaller weights
 * @author ngonga
 */
public class SmallestClusterHardening extends LargestClusterHardening{
     public List<Set<Node>> harden(Set<Set<Node>> clusters, WeightedGraph wg) {
        Set<Node> nodes = new HashSet<Node>(wg.getNodes().keySet());
        double min, weight;
        Set<Node> bestCluster;
        List<Set<Node>> result = new ArrayList<Set<Node>>();
        while (!nodes.isEmpty()) {
            min = Double.MAX_VALUE;
            bestCluster = null;
            //first get weights            
            for (Set<Node> c : clusters) {
                if (!result.contains(c)) {
                    weight = getWeight(c, wg, nodes);
                    if (weight < min) {
                        min = weight;
                        bestCluster = c;
                    }
                }
            }
            // no more clusters available
            if (bestCluster == null) {
                return result;
            }
            //in all other cases       
            clusters.remove(bestCluster);
            bestCluster.retainAll(nodes);
            result.add(bestCluster);
            nodes.removeAll(bestCluster);
        }
        result = Lists.reverse(result);
        return result;
    }
}
