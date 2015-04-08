/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.clustering;

import org.semanticweb.owlapi.model.OWLProperty;
/**
 *
 * @author ngonga
 */
public class PropertyNode extends Node {
    public OWLProperty property;
    
    public PropertyNode(OWLProperty p)
    {
        super(p.toStringID());
//        this.label = p.getName();
        this.property = p;
    }
}
