package org.sparql2nl.demo;


import java.util.HashMap;

import org.coode.owlapi.manchesterowlsyntax.ManchesterOWLSyntax;

/**
 * Author: drummond<br>
 * http://www.cs.man.ac.uk/~drummond/<br><br>
 * <p/>
 * The University Of Manchester<br>
 * Bio Health Informatics Group<br>
 * Date: Sep 23, 2008<br><br>
 */
public class KeywordColorMap extends HashMap<String, String> {

    /**
	 * 
	 */
	private static final long serialVersionUID = 8840286687096195602L;

	public KeywordColorMap() {
        String restrictionColor = "#0080FF";//blue";
        String logicalOpColor = "#01DF01";//green";
        String axiomColor = "#FF0000";//red";
        String typeColor = "#FFFF00";//yellow";

        for (ManchesterOWLSyntax keyword : ManchesterOWLSyntax.values()){
            if (keyword.isAxiomKeyword()){
                put(keyword.toString(), axiomColor);
                put(keyword.toString() + ":", axiomColor);
            }
            else if (keyword.isClassExpressionConnectiveKeyword()){
                put(keyword.toString(), logicalOpColor);
            }
            else if (keyword.isClassExpressionQuantiferKeyword()){
                put(keyword.toString(), restrictionColor);
            }
            else if (keyword.isSectionKeyword()){
                put(keyword.toString(), typeColor);
                put(keyword.toString() + ":", typeColor);
            }
        }

        put("o", axiomColor);
//        put("\u279E", axiomColor);
//        put("\u2192", axiomColor);
//        put("\u2227", axiomColor);
    }
}
