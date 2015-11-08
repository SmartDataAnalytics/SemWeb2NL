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
package org.aksw.sparql2nl.smooth_nlg;

import java.util.Set;

/**
 *
 * @author christina
 */
public class Entity {
    
    String var;
    boolean count;
    String type;
    Set<Predicate> properties;
    
    public Entity(String v,boolean c,String t,Set<Predicate> ps) {
        var = v;
        count = c;
        type = t;
        properties = ps;
    }
    
    public String toString() {
        String out = "";
        if (count) { out += " number of "; } 
        out += var + " (" + type + ") : {";
        for (Predicate p : properties) {
            out += p.toString() + ", ";
        }
        out = out.substring(0,out.length()-2);
        out += "}";
        return out;
    }
}
