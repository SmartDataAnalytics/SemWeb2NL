/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.clustering;

import java.util.Set;

/**
 *
 * @author ngonga
 */
public interface ClusteringAlgorithm {
    Set<Set<Node>> cluster(WeightedGraph wg);
}
