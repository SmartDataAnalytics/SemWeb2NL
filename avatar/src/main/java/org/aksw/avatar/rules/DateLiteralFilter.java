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
/**
 * 
 */
package org.aksw.avatar.rules;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.aksw.triple2nl.converter.LiteralConverter;
import org.apache.log4j.Logger;

import com.google.common.collect.Lists;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.graph.impl.LiteralLabel;

/**
 * Returns only canonical forms of numeric literals, e.g. for 800 cm and 8.00m the digits are the same.
 * @author Lorenz Buehmann
 *
 */
public class DateLiteralFilter {
	
	
	private static final Logger logger = Logger.getLogger(DateLiteralFilter.class.getName());
	
	private static final List<XSDDatatype> dateTypes = Lists.newArrayList(
			XSDDatatype.XSDdateTime, 
			XSDDatatype.XSDdate, 
			XSDDatatype.XSDgYearMonth, 
			XSDDatatype.XSDgYear,
			XSDDatatype.XSDgMonth,
			XSDDatatype.XSDgMonthDay);
	
	private static final List<XSDDatatype> ordering1 = Lists.newArrayList(
			XSDDatatype.XSDdateTime, 
			XSDDatatype.XSDdate, 
			XSDDatatype.XSDgYearMonth, 
			XSDDatatype.XSDgYear);
	
	private static final List<XSDDatatype> ordering2 = Lists.newArrayList(
			XSDDatatype.XSDdateTime, 
			XSDDatatype.XSDdate, 
			XSDDatatype.XSDgMonthDay, 
			XSDDatatype.XSDgMonth);
	
	LiteralConverter literalConverter = new LiteralConverter(null);
	
	public void filter(Set<Triple> triples){
		Set<Triple> dateTriples = new HashSet<>(triples.size());
		for (Triple triple : triples) {
			if(isDateDatatype(triple.getObject())){
				dateTriples.add(triple);
			}
		}
		
		Set<Triple> ommitted = new HashSet<>();
		for (Triple triple1 : dateTriples) {
			for (Triple triple2 : dateTriples) {
				if(!triple1.equals(triple2)){
					if(isMoreGeneralThan(triple1.getObject().getLiteral(), triple2.getObject().getLiteral())){
						ommitted.add(triple2);
						logger.warn("Omitting triple " + triple2 + " because of triple " + triple1 + ".");
					}
				}
			}
		}
		triples.removeAll(ommitted);
	}
	
	private boolean isMoreGeneralThan(LiteralLabel lit1, LiteralLabel lit2){
		RDFDatatype dt1 = lit1.getDatatype();
		RDFDatatype dt2 = lit2.getDatatype();
		if(ordering1.contains(dt1) && ordering1.contains(dt2)){
			if(ordering1.indexOf(dt1) < ordering1.indexOf(dt2)){
				String s1 = literalConverter.convert(lit1);
				String s2 = literalConverter.convert(lit2);
				if(s1.contains(s2)){
					return true;
				}
			}
		}
		if(ordering2.contains(dt1) && ordering2.contains(dt2)){
			if(ordering2.indexOf(dt1) < ordering2.indexOf(dt2)){
				String s1 = literalConverter.convert(lit1);
				String s2 = literalConverter.convert(lit2);
				if(s1.contains(s2)){
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean isDateDatatype(Node node){
		if(node.isLiteral()){
			LiteralLabel literal = node.getLiteral();
			
			if(literal.getDatatype() != null && dateTypes.contains(literal.getDatatype())){
				return true;
			}
		}
		return false;
	}

}
