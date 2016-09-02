package org.sparql2nl.demo;

import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.core.Var;
import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.dictionary.Dictionary;
import org.aksw.jena_sparql_api.cache.h2.CacheUtilsH2;
import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp;
import org.aksw.sparql2nl.naturallanguagegeneration.SimpleNLGwithPostprocessing;
import org.aksw.sparql2nl.queryprocessing.QueryPreprocessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparql2nl.demo.model.Knowledgebase;
import simplenlg.lexicon.Lexicon;
import simplenlg.realiser.english.Realiser;

import java.util.concurrent.TimeUnit;

public class SPARQL2NLManager {
	
	private static final Logger logger = LoggerFactory.getLogger(Manager.class);
	
	private Lexicon lexicon;
	private Realiser realiser;

	private QueryExecutionFactory qef;
	protected long cacheTTL = TimeUnit.DAYS.toMillis(1);

	private SimpleNLGwithPostprocessing nlg;
	private QueryPreprocessor preprocessor;
	
	private SPARQLExplain expGen;
	
	private Knowledgebase currentKnowledgebase;
	
	public SPARQL2NLManager() {
		lexicon = Lexicon.getDefaultLexicon();
        realiser = new Realiser(lexicon);
	}
	
	public void setCurrentKnowledgebase(Knowledgebase knowledgebase) {
		this.currentKnowledgebase = knowledgebase;

		qef = new org.aksw.jena_sparql_api.http.QueryExecutionFactoryHttp(
				knowledgebase.getEndpoint().getURL().toString(),
				knowledgebase.getEndpoint().getDefaultGraphURIs());

		qef = CacheUtilsH2.createQueryExecutionFactory(qef, Manager.getInstance().getCacheDir() + "/sparql", false, cacheTTL );
		qef = new QueryExecutionFactoryHttp(knowledgebase.getEndpoint().getURL().toString());
		try {
			nlg = new SimpleNLGwithPostprocessing(qef, Manager.getInstance().getCacheDir(), Dictionary.getDefaultResourceInstance());
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		nlg.setUseBOA(Manager.getInstance().isUseBOA());
		expGen = new SPARQLExplain(qef);
		
//		 preprocessor = new QueryPreprocessor(knowledgebase.getEndpoint(), Manager.getInstance().getParserModelFilePath());
	}
	
	public Query replaceVarsWithTypes(Query query){
		return preprocessor.replaceVariablesWithTypes(query);
	}
	
	public Knowledgebase getCurrentKnowledgebase() {
		return currentKnowledgebase;
	}
	
	public String getNLR(Query query){
		return nlg.getNLR(query);
	}
	
	public String getNLR(String query){
		return nlg.getNLR(QueryFactory.create(query, Syntax.syntaxARQ));
	}
	
	public String getExplanationNLR(Query query, QuerySolution qs, boolean asHtmlList){
		Model exp = expGen.getExplanation(query, qs);
		String nlr = getNLR(exp, asHtmlList);
		if(asHtmlList){
			String prefix = "<p>";
			for(Var var : query.getProjectVars()){
				prefix += var.getVarName() + "=" + qs.get(var.getVarName()) + " ";
			}
			prefix += "belongs to result set because:";
			prefix += "</p>";
			nlr = prefix + nlr;
		}
		return nlr;
	}
	
	public String getNLR(Model model){
		return getNLR(model, false);
	}
	
	public String getNLR(Model model, boolean asHtmlList){
		StringBuilder nlr = new StringBuilder();
		if(asHtmlList){
			nlr.append("<ul>");
			String tripleNlr;
			for(Statement st : model.listStatements().toList()){
				tripleNlr = realiser.realiseSentence(nlg.getNLForTriple(st.asTriple()));
				nlr.append("<li>").append(tripleNlr).append("</li>");
			}
			nlr.append("</ul>");
		} else {
			String tripleNlr;
			for(Statement st : model.listStatements().toList()){
				tripleNlr = realiser.realiseSentence(nlg.getNLForTriple(st.asTriple()));
				nlr.append(tripleNlr).append("\n");
			}
		}
		
		
		return nlr.toString();
	}
	
	public ResultSet executeSelect(Query query){
		if(query.isSelectType()){
			return qef.createQueryExecution(query).execSelect();
		} 
		throw new UnsupportedOperationException("Only SELECT queries are supported.");
	}
	
	public void reset(){
		setCurrentKnowledgebase(Manager.getInstance().getKnowledgebases().get(0));
	}

}
