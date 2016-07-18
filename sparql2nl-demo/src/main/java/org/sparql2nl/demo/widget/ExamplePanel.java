package org.sparql2nl.demo.widget;

import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.*;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.util.SimpleIRIShortFormProvider;
import org.sparql2nl.demo.model.Example;

import java.io.StringWriter;
import java.util.Iterator;

public class ExamplePanel extends Panel{
	
//	private Disclosure dis;
	private Example example;
	private boolean selected = false;
	private SimpleIRIShortFormProvider sfp = new SimpleIRIShortFormProvider();
	
	public ExamplePanel(Example example){
		this(example, false);
	}
	
	public ExamplePanel(Example example, boolean nlrMode) {
		this.example = example;
		
		setSizeFull();
		
		HorizontalLayout content = new HorizontalLayout();
		content.setWidth("100%");
		content.setHeight(null);
		setContent(content);
		
		CheckBox c = new CheckBox();
		c.addListener(new Property.ValueChangeListener() {
			
			@Override
			public void valueChange(ValueChangeEvent event) {
				selected = (Boolean)event.getProperty().getValue();
			}
		});
		content.addComponent(c);

		StringBuilder solutionString = new StringBuilder();
		if(nlrMode){
			QuerySolution qs = example.getData();
			boolean labelExists = false;
			for (Iterator<String> iterator = qs.varNames(); iterator.hasNext();) {
				String var = iterator.next();
				if(var.equals("string") && qs.get(var).isLiteral()){
					labelExists = true;
					break;
				}
				
			}
			if(labelExists){
				solutionString.append(qs.getLiteral("string").getLexicalForm());
			} else {
				int varCnt = 0;
				for (Iterator<String> iterator = qs.varNames(); iterator.hasNext();) {
					varCnt++;
					iterator.next();
				}
				
				for (Iterator<String> iterator = qs.varNames(); iterator.hasNext();) {
					String var = iterator.next();
					RDFNode node = qs.get(var);
					String label = null;
					if(node != null){
						if(node.isLiteral()){
							label = node.asLiteral().getLexicalForm();
						} else if(node.isURIResource()){
							label = sfp.getShortForm(IRI.create(node.asResource().getURI()));
						}
					}
					if(label != null){
						if(varCnt > 1){
							solutionString.append("( " + var + " = "  + label  + ")").append(" ");
						} else {
							solutionString.append(label);
						}
						
					}
					
				}
				
			}
			
		} else {
			solutionString.append(example.getData().toString());
		}
		
		VerticalLayout l = new VerticalLayout();
		l.setWidth("100%");
		l.setHeight(null);

		Panel p = new Panel(solutionString.toString());
		p.setContent(l);
		p.setWidth("100%");
		content.addComponent(p);
		content.setExpandRatio(p, 1.0f);
		
//		dis = new Disclosure(solutionString.toString());
//		dis.setContent(l);
//		dis.setWidth("100%");
//		content.addComponent(dis);
		
		String explanationString = null;
		if(nlrMode){
			explanationString = example.getExplanationNlr();
		} else {
			StringWriter sw = new StringWriter();
			example.getExplanation().write(sw, "TURTLE");
			explanationString = removePrefixes(sw.toString());
		}
		
		Label explanationLabel = new Label(explanationString);
		explanationLabel.setContentMode(Label.CONTENT_PREFORMATTED);
		l.addComponent(explanationLabel);
		
//		content.setExpandRatio(dis, 1.0f);
	}
	
	public boolean containsPositiveExample(){
		return example.isPositive();
	}
	
	public boolean isSelected(){
		return selected;
	}
	
	public void expand(boolean expand){
//		if(expand) {
//			dis.open();
//		} else {
//			dis.close();
//		}
	}
	
	private String removePrefixes(String explanationString){
		while(explanationString.indexOf("@prefix") >=0){
			explanationString = explanationString.substring(explanationString.indexOf("\n")+1);
		}
		explanationString = explanationString.trim();
		return explanationString;
	}

}
