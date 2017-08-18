package org.aksw.triple2nl.gender;

import org.aksw.jena_sparql_api.core.QueryExecutionFactory;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.apache.jena.web.HttpSC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Lorenz Buehmann
 */
public class PropertyBasedGenderDetector implements GenderDetector{

    private static final Logger logger = LoggerFactory.getLogger(PropertyBasedGenderDetector.class);

    private static final ParameterizedSparqlString GENDER_QUERY_TEMPLATE = new ParameterizedSparqlString(
            "SELECT ?gender WHERE {" +
                    "VALUES (?p_id ?p) {%PROPERTIES}" +
                    "?s ?p ?gender . }");

    private final QueryExecutionFactory qef;
    private final List<String> properties;

    public PropertyBasedGenderDetector(QueryExecutionFactory qef, List<String> properties) {
        this.qef = qef;
        this.properties = properties;
    }

    @Override
    public Gender getGender(String iri, String name) {
        GENDER_QUERY_TEMPLATE.clearParams();
        GENDER_QUERY_TEMPLATE.setIri("s", iri);

        String query = GENDER_QUERY_TEMPLATE.toString();
        query = query.replace("%PROPERTIES",
                properties.stream().map(p -> "(" + properties.indexOf(p) + " <" + p + ">)").collect(Collectors.joining()));
        
        logger.debug("Gender lookup query:\n" + query);
        try (QueryExecution qe = qef.createQueryExecution(query.toString())){
            ResultSet rs = qe.execSelect();

            if(rs.hasNext()){
                QuerySolution qs = rs.next();
                Literal label = qs.getLiteral("gender");
                if(label != null) {
                    return Gender.fromText(label.getLexicalForm());
                }
            }

        } catch (Exception e) {
            int code = -1;
            //cached exception is wrapped in a RuntimeException
            if(e.getCause() instanceof QueryExceptionHTTP){
                code = ((QueryExceptionHTTP)e.getCause()).getResponseCode();
            } else if(e instanceof QueryExceptionHTTP){
                code = ((QueryExceptionHTTP) e).getResponseCode();
            } else {

            }
            HttpSC.Code statusCode = HttpSC.getCode(code);
            logger.warn("Getting gender of " + iri + " from SPARQL endpoint failed. "
                    + (statusCode != null ? "Status code: " + code + " - " + statusCode.getMessage() : ExceptionUtils.getRootCauseMessage(e)));
        }
        return Gender.UNKNOWN;
    }
}
