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
package org.aksw.avatar.dump;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author ngonga
 */
public class DumpStatistics {
    // 1. Get statistics on properties ergo Map<Node, Map<Set<Node>, Integer>>> 
    // maps each class to a map that contains property pairs and the frequency 
    // with which they occur
    
    public Set<Set<Node>> clusterProperties(Map<Set<Node>, Integer> properties)
    {
        return new HashSet<>();
        
//        Model m = ModelFactory.createDefaultModel();
//        Property m.createProperty(null, null);
    }
}
