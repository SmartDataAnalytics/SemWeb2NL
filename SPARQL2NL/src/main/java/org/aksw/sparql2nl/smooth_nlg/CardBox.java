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
package org.aksw.sparql2nl.smooth_nlg;

import org.apache.jena.sparql.syntax.Element;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author christina
 */
public class CardBox {
    
    List<Entity> primaries;
    List<Entity> secondaries;
    List<Element> filters;
    List<Element> optionals;
    List<OrderBy> orderBys;
    
    public CardBox(List<Entity> primes,List<Entity> secs,List<Element> fils,List<Element> opts,List<OrderBy> obs) {
        primaries = primes;
        secondaries = secs;
        filters = fils;
        optionals = opts;
        orderBys = obs;
    }
    
    public String[][] getMatrix(Entity e) {
        
        String[][] m = new String[e.properties.size()][3];
        int row = 0;
        for (Predicate p : e.properties) {
            m[row][0] = p.subject;
            m[row][1] = p.predicate;
            m[row][2] = p.object;
            row++;
        }       
        return m;
    }
    
    public Set<String> getSecondaryVars() {
        
        Set<String> out = new HashSet<>();
        for (Entity e : secondaries) {
            out.add(e.var);
        }
        return out;
    }
    
    public void print() {
        
        System.out.println("\nPRIMARY ENTITIES:\n");
        for (Entity entity : primaries) {
            System.out.println(entity.toString());
        }
        System.out.println("\nSECONDARY ENTITIES:\n");
        for (Entity entity : secondaries) {
            System.out.println(entity.toString());
        }
        System.out.println("...");
    }
    
}
