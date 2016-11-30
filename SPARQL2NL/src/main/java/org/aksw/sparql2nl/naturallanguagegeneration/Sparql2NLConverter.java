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
package org.aksw.sparql2nl.naturallanguagegeneration;

import org.apache.jena.query.Query;
import simplenlg.framework.DocumentElement;

/**
 * Interface for tools that transform SPARQL queries into natural language
 * @author ngonga
 */
public interface Sparql2NLConverter {
    
    /** Takes a query as input and returns a natural language representation of 
     * the input.
     * @param query Input query
     * @return Natural language representation as strings
     */
    public String getNLR(Query query);
    /** Converts a SPARQL query into a natural language element that can be processed 
     * further.
     * @param query Input query
     * @return Natural Language Element
     */
    public DocumentElement convert2NLE(Query query);
}
