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

import java.util.Set;

import org.aksw.avatar.clustering.Node;
import org.aksw.avatar.clustering.WeightedGraph;

/**
 * Harderning that prefers clusters with higher average weight
 * @author ngonga
 */
public class AverageWeightClusterHardening extends LargestClusterHardening {
    
    /**
     * Computes the weight of a cluster w.r.t. to a given set of nodes within a
     * weighted graph
     *
     * @param cluster A cluster
     * @param wg A node- and edge-weighted graph
     * @param reference
     * @return Weight of the set of nodes
     */
    @Override
    public double getWeight(Set<Node> cluster, WeightedGraph wg, Set<Node> reference) {
        double w = 0d;

        for (Node n : cluster) {
            if (reference.contains(n)) {
                for (Node n2 : cluster) {
                    if (reference.contains(n2)) {
                        if (n.equals(n2)) {
                            w = w + wg.getNodeWeight(n);
                        } else {
                            w = w + wg.getEdgeWeight(n, n2);
                        }
                    }
                }
            }
        }
        return w/(cluster.size()*cluster.size());        
    }

}
