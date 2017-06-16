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
package org.aksw.sparql2nl.naturallanguagegeneration;

import java.util.Hashtable;
import java.util.Set;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

public class Union {
    
    Set<Set<Sentence>> sentences;
    boolean optional;
    
    public Union(Set<Set<Sentence>> s,boolean o) {
        sentences = s;
        optional = o;
        if (optional) {
            for (Set<Sentence> un : sentences) {
                for (Sentence sent : un) {
                     sent.optional = true;
                }
            }
        }
    }
    
    public Sentence removeRedundancy(Realiser realiser) {
        
        Hashtable<String,Sentence> unionsents = new Hashtable<>();
            
        for (Set<Sentence> un : sentences) {          
             for (Sentence s : un) {
                  if (!unionsents.containsKey(realiser.realise(s.sps).toString().toLowerCase())) 
                       unionsents.put(realiser.realise(s.sps).toString().toLowerCase(),s);
             }
        }
        if (unionsents.size() == 1) { 
            for (String key : unionsents.keySet()) {
                 return unionsents.get(key);
            }
        }
        return null;
    }
    
}
