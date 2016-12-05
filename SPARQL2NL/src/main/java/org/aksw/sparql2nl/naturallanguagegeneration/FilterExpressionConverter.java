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

import com.google.common.collect.Sets;
import org.aksw.triple2nl.converter.DefaultIRIConverter;
import org.aksw.triple2nl.converter.LiteralConverter;
import org.apache.jena.datatypes.RDFDatatype;
import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.impl.LiteralLabel;
import org.apache.jena.sparql.expr.*;
import org.apache.jena.sparql.expr.aggregate.*;
import org.apache.jena.vocabulary.XSD;
import simplenlg.features.Feature;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.LexicalCategory;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.realiser.english.Realiser;

import java.util.Locale;
import java.util.Set;
import java.util.Stack;

public class FilterExpressionConverter implements ExprVisitor{
	
	private NLGFactory nlgFactory;
	private Realiser realiser;
	
	private Stack<NLGElement> stack;
	
	private DefaultIRIConverter uriConverter;
	private LiteralConverter literalConverter;
	
	private boolean simplifyLanguageFilterConstructs = true;
	private boolean inRegex = false;
	
	private final String dateOnText = "be on";
	private final String dateAfterText = "be after";
	private final String dateAfterOrOnText = "be after or on";
	private final String dateBeforeText = "be before";
	private final String dateBeforeOrOnText = "be before or on";
	private final String periodOnText = "be in";
	private final String periodAfterText = "be after";
	private final String periodAfterOrOnText = "be after or in";
	private final String periodBeforeText = "be before";
	private final String periodBeforeOrOnText = "be before or in";
	
	public FilterExpressionConverter(DefaultIRIConverter uriConverter, LiteralConverter literalConverter) {
		this.uriConverter = uriConverter;
		this.literalConverter = literalConverter;
		
		Lexicon lexicon = Lexicon.getDefaultLexicon();
		nlgFactory = new NLGFactory(lexicon);
		realiser = new Realiser(lexicon);
	}
	
	public FilterExpressionConverter(DefaultIRIConverter uriConverter) {
		this(uriConverter, new LiteralConverter(uriConverter));
	}
	
	public NLGElement convert(Expr expr){
		startVisit();
		expr.visit(this);
		NLGElement element = stack.pop();
		finishVisit();
		return element;
	}

	@Override
	public void startVisit() {
		stack = new Stack<>();
	}

	@Override
	public void visit(ExprFunction0 func) {
		
		
	}

	@Override
	public void visit(ExprFunction1 func) {
		func.getArg().visit(this);
		NLGElement subject = stack.pop();
		
		NLGElement element;
		if(func instanceof E_LogicalNot){
			subject.setFeature(Feature.NEGATED, true);
			stack.push(subject);
		} else {
			if(func instanceof E_Bound){
				element = nlgFactory.createClause();
				((SPhraseSpec) element).setSubject(subject);
				((SPhraseSpec) element).setVerb("exist");
			} else if(func instanceof E_Str){
				element = nlgFactory.createNounPhrase("the string of " + realiser.realise(subject).getRealisation());
			} else if(func instanceof E_Lang){
				String s = null;
				if(simplifyLanguageFilterConstructs){
					s = realiser.realise(subject).getRealisation();
				} else {
					s = "the language of " + realiser.realise(subject).getRealisation();
				}
				element = nlgFactory.createNounPhrase(s);
			} else {
				throw new UnsupportedOperationException(func + " is not implemented yet.");
			}
			stack.push(element);
		} 
		
	}

	@Override
	public void visit(ExprFunction2 func) {
		Expr left = func.getArg1();
        Expr right = func.getArg2();

        //invert if right is variable or aggregation and left side not 
        boolean inverted = false;
        if (!left.isVariable() && (right.isVariable() || right instanceof ExprAggregator)) {
            Expr tmp = left;
            left = right;
            right = tmp;
            if (func instanceof E_GreaterThan) {
            	func = new E_LessThan(left, right);
            } else if (func instanceof E_GreaterThanOrEqual) {
            	func = new E_LessThanOrEqual(left, right);
            } else if (func instanceof E_LessThan) {
            	func = new E_GreaterThan(left, right);
            } else if (func instanceof E_LessThanOrEqual) {
            	func = new E_GreaterThanOrEqual(left, right);
            }
//            inverted = true;
        }

        //handle left side
        left.visit(this);
        NLGElement leftElement = stack.pop();
        
        //handle right side
        right.visit(this);
        NLGElement rightElement = stack.pop();
        
        if(func instanceof E_LogicalAnd || func instanceof E_LogicalOr){
        	CoordinatedPhraseElement c = nlgFactory.createCoordinatedPhrase();
        	c.addCoordinate(leftElement);
        	c.addCoordinate(rightElement);
        	if (func instanceof E_LogicalOr){
            	c.setConjunction("or");
            }
        	stack.push(c);
        } else {
        	SPhraseSpec phrase = nlgFactory.createClause();
        	 //handle verb resp. predicate
            String verb = null;
            boolean plural = false;
            if (func instanceof E_GreaterThan) {
                verb = convertLessOrGreater(right, inverted);
            } else if (func instanceof E_GreaterThanOrEqual) {
            	verb = convertLessOrGreaterEquals(right, inverted);
            } else if (func instanceof E_LessThan) {
            	verb = convertLessOrGreater(right, !inverted);
            } else if (func instanceof E_LessThanOrEqual) {
            	verb = convertLessOrGreaterEquals(right, !inverted);
            } else if(func instanceof E_Equals){
            	if(left instanceof E_Lang && simplifyLanguageFilterConstructs){
            		rightElement = nlgFactory.createNounPhrase(getLanguageForAbbreviation(realiser.realise(rightElement).getRealisation()));
                        verb = "be in";
                        plural = true;
            	} else {
                        if (realiser.realise(rightElement).toString().startsWith("?")){
                        	verb = "be the same as";
                        }
                        else {
                        	verb = "be equal to";
                        	if(right.isConstant()){
                        		if(isDateLiteral(right.getConstant().getNode().getLiteral())){
                        			if(isDatePeriodLiteral(right.getConstant().getNode().getLiteralDatatype())){
                        				verb = periodOnText;
                        			} else {
                        				verb = dateOnText;
                        			}
                        		}
                        	}
                        }
            	}
            } else if (func instanceof E_NotEquals) {
            	if(left instanceof E_Lang && simplifyLanguageFilterConstructs){
            		verb = "be in";
            		rightElement = nlgFactory.createNounPhrase(getLanguageForAbbreviation(realiser.realise(rightElement).getRealisation()));
            	} else {
            		if (realiser.realise(rightElement).toString().startsWith("?")) verb = "be the same as";
                        else verb = "be equal to";
            	}
                phrase.setFeature(Feature.NEGATED, true);
            } 
            phrase.setSubject(leftElement);
            phrase.setObject(rightElement);
            phrase.setVerb(verb);
            if(plural){
            	phrase.setPlural(true);
            }
            stack.push(phrase);
        }
        
       
	}
	
	private String convertLessOrGreater(Expr right, boolean less){
		String verb;
		if (less) {
            verb = "be less than";
            if(right.isConstant()){
        		if(isDateLiteral(right.getConstant().getNode().getLiteral())){
        			verb = dateBeforeText;
        		}
        	}
        } else {
        	verb = "be greater than";
        	if(right.isConstant()){
        		if(isDateLiteral(right.getConstant().getNode().getLiteral())){
        			verb = dateAfterText;
        		}
        	}
        }
		return verb;
	}
	
	private String convertLessOrGreaterEquals(Expr right, boolean less){
		String verb;
		if (less) {
            verb = "be less than or equal to";
            if(right.isConstant()){
        		if(isDateLiteral(right.getConstant().getNode().getLiteral())){
        			if(isDatePeriodLiteral(right.getConstant().getNode().getLiteralDatatype())){
        				verb = periodBeforeOrOnText;
        			} else {
        				verb = dateBeforeOrOnText;
        			}
        		}
        	}
        } else {
            verb = "be greater than or equal to";
            if(right.isConstant()){
        		if(isDateLiteral(right.getConstant().getNode().getLiteral())){
        			if(isDatePeriodLiteral(right.getConstant().getNode().getLiteralDatatype())){
        				verb = periodAfterOrOnText;
        			} else {
        				verb = dateAfterOrOnText;
        			}
        		}
        	}
        }
		return verb;
	}

	@Override
	public void visit(ExprFunction3 func) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ExprFunctionN func) {
		SPhraseSpec phrase = nlgFactory.createClause();
		if(func instanceof E_Regex){
			inRegex = true;
			Expr target = func.getArg(1);
			target.visit(this);
			phrase.setSubject(stack.pop());
			
			Expr pattern = func.getArg(2);
			pattern.visit(this);
			phrase.setObject(stack.pop());
			
			String adverb = "";
			Expr flags = func.getArg(3);
			if(flags != null){
				flags.visit(this);
				if(realiser.realise(stack.pop()).getRealisation().equals("\"i\"")){
					adverb += "ignorecase";
				}
				
			}
			phrase.setVerb("match " + adverb);
			stack.push(phrase);
			inRegex = false;
		} else if(func instanceof E_Function){
			ExprFunction function = func.getFunction();
			if(function.getFunctionIRI().equals(XSD.integer.getURI())){
				function.getArg(1).visit(this);
			} else {
				throw new UnsupportedOperationException(function.getFunctionIRI() + " is not implemented yet.");
			}
		} else {
			throw new UnsupportedOperationException(func + " is not implemented yet.");
		}
	}

	@Override
	public void visit(ExprFunctionOp funcOp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(NodeValue nv) {
		Node node = nv.asNode();
		String label = null;
		boolean isPlural = false;
		if(node.isVariable()){
			label = nv.toString();
		} else if(node.isURI()){
			label = uriConverter.convert(nv.asNode().getURI());
		} else if(node.isLiteral()){
			LiteralLabel lit = node.getLiteral();
			label = literalConverter.convert(lit);
			isPlural = literalConverter.isPlural(lit) && !inRegex;
			if(inRegex){
				label = "\"" + label + "\"";
			}
		}
		NLGElement element = nlgFactory.createNounPhrase(nlgFactory.createWord(label, LexicalCategory.NOUN));
		element.setPlural(isPlural);
		stack.push(element);
	}

	@Override
	public void visit(ExprVar nv) {
		NLGElement element = nlgFactory.createNounPhrase(nv.toString());
		stack.push(element);
	}

	@Override
	public void visit(ExprAggregator eAgg) {
		Aggregator aggregator = eAgg.getAggregator();
        ExprList exprList = aggregator.getExprList();
        Expr expr = exprList.get(0);
        expr.visit(this);
        String s = convertAggregator(aggregator);
		NLGElement element = nlgFactory.createNounPhrase(s + realiser.realise(stack.pop()));
		stack.push(element);
	}

	@Override
	public void finishVisit() {
		// TODO Auto-generated method stub
		
	}
	
	private boolean isDateLiteral(LiteralLabel literal){
		RDFDatatype datatype = literal.getDatatype();
		Set<RDFDatatype> dateDatatypes = Sets.newHashSet();
		dateDatatypes.addAll(Sets.newHashSet(
				XSDDatatype.XSDdate, 
				XSDDatatype.XSDdateTime, 
				XSDDatatype.XSDgYear, 
				XSDDatatype.XSDgYearMonth,
				XSDDatatype.XSDgMonth,
				XSDDatatype.XSDgMonthDay));
		if(datatype != null){
			return dateDatatypes.contains(datatype);
		}
		return false;
	}
	
	private boolean isDatePeriodLiteral(RDFDatatype datatype){
		Set<RDFDatatype> periodDatatypes = Sets.newHashSet();
		periodDatatypes.addAll(Sets.newHashSet(
				XSDDatatype.XSDgYear, 
				XSDDatatype.XSDgYearMonth,
				XSDDatatype.XSDgMonth));
		if(datatype != null){
			return periodDatatypes.contains(datatype);
		}
		return false;
	}
	
	private String getLanguageForAbbreviation(String languageTag){
		String language = Locale.forLanguageTag(languageTag.replace("\"", "").trim()).getDisplayLanguage(Locale.ROOT);
		return language;
	}
	
	public String convertAggregator(Aggregator aggregator){
        ExprList exprList = aggregator.getExprList();
        Expr expr = exprList.get(0);
        expr.visit(this);
        String s = null;
        if (aggregator instanceof AggCountVar) {
            s = "the number of ";
        } else if(aggregator instanceof AggCountVarDistinct){
        	s = "the number of distinct ";
        } else if(aggregator instanceof AggMin){
        	s = "the minimum of ";
        } else if(aggregator instanceof AggMinDistinct){
        	s = "the minimum of distinct ";
        } else if(aggregator instanceof AggMax){
        	s = "the maximum of ";
        } else if(aggregator instanceof AggMaxDistinct){
        	s = "the maximum of distinct ";
        } else if(aggregator instanceof AggAvg){
        	s = "the average of ";
        } else if(aggregator instanceof AggAvgDistinct){
        	s = "the average of distinct ";
        } else {
        	throw new UnsupportedOperationException("This aggregate function is not implemented yet." + aggregator);
        }
		return s;
	}

}
