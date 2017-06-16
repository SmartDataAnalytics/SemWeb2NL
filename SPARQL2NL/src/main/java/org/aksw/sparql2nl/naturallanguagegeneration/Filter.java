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

import java.util.HashSet;
import java.util.Set;

public class Filter {
    
    Set<Sentence> sentences;
    String coord;
    
    public Filter(Set<Sentence> s,String c) {
        sentences = s;
        coord = c;
    }
    public Filter(Sentence s) {
        sentences = new HashSet<>();
        sentences.add(s);
        coord = null;
    }
    
}
