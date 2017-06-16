package org.sparql2nl.demo.view;

import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.ExternalResource;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.MultiSelectMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.*;
import com.vaadin.ui.themes.ValoTheme;
import de.fatalix.vaadin.addon.codemirror.CodeMirror;
import de.fatalix.vaadin.addon.codemirror.CodeMirrorLanguage;
import de.fatalix.vaadin.addon.codemirror.CodeMirrorTheme;
import org.sparql2nl.demo.Manager;
import org.sparql2nl.demo.SPARQL2NLSession;
import org.sparql2nl.demo.model.Knowledgebase;
import org.sparql2nl.demo.model.Language;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MainView extends Panel implements View{
	
	private Label nlrLabel;
	private CodeMirror sparqlQueryInput;
//	private TextArea sparqlQueryInput;
	private VerticalLayout resultTableHolder;
	private Query currentQuery;
	private Table currentTable;
	private VerticalLayout explanationHolder;
	private Embedded centerLogo;
	private NativeSelect knowledgebaseSelector;
	private CheckBox inferVarNamesCheckBox;

	private final VerticalLayout root;
	private CssLayout dashboardPanels;

	int cnt = 1;
	
	public MainView() {
//		Design.read(this);
		addStyleName(ValoTheme.PANEL_BORDERLESS);
		setSizeFull();

		root = new VerticalLayout();
		root.setSizeFull();
		root.setMargin(true);
		root.addStyleName("main-view");
		root.setSpacing(true);
		setContent(root);
//		Responsive.makeResponsive(root);

		root.addComponent(buildHeader());
		Component content = buildContent();
		root.addComponent(content);
		root.setExpandRatio(content, 1.0f);

		root.addComponent(createFooter());

		reset();
	}
	
	private Component buildHeader(){
		HorizontalLayout header = new HorizontalLayout();
		header.setWidth("100%");
		header.setHeight(null);
		header.addStyleName("header");

	    Label label = new Label("SPARQL2NL Demo");
		label.setWidth(null);
		label.addStyleName("heading");
//		label.addStyleName("h1");
		header.addComponent(label);
		header.setComponentAlignment(label, Alignment.MIDDLE_CENTER);

		return header;
	}
	
	private Component createFooter(){
		HorizontalLayout footer = new HorizontalLayout();
		footer.setWidth("100%");
		footer.setHeight(null);
		footer.addStyleName("footer");

		Resource res = new ThemeResource("img/citec_logo.gif");

		Link link = new Link("", new ExternalResource("http://www.cit-ec.de/"));
	    link.setIcon(res);
	    link.setHeight("40px");
		link.addStyleName("logo");
	    footer.addComponent(link);

	    Label pad = new Label();
		pad.setWidth("100%");
		footer.addComponent(pad);
		footer.setExpandRatio(pad, 1f);
		
		res = new ThemeResource("img/aksw_logo.png");
		link = new Link("", new ExternalResource("http://www.aksw.org"));
	    link.setIcon(res);
	    link.setHeight("40px");
		link.addStyleName("logo");
	    footer.addComponent(link);

		return footer;
	}
	
	private Component buildContent(){
		dashboardPanels = new CssLayout();

		// use 3x3 grid as layout
		GridLayout grid = new GridLayout(3, 3);
		grid.setSizeFull();
		grid.setSpacing(true);

		// add the 4 views
		grid.addComponent(createQueryPortal(), 0, 0);
		grid.addComponent(createNLRPortal(), 2, 0);
		grid.addComponent(createQueryResultPortal(), 0, 2);
		grid.addComponent(createExplanationsPortal(), 2, 2);
		grid.setRowExpandRatio(0, 0.5f);
		grid.setRowExpandRatio(2, 0.5f);
		grid.setColumnExpandRatio(0, 0.5f);
		grid.setColumnExpandRatio(2, 0.5f);

		// add the buttons
		// translate button
        Button b = new Button("Translate");
//		b.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
//		b.setIcon(FontAwesome.CHEVRON_RIGHT);
        b.setDescription("Click to translate the SPARQL query into natural language.");
        b.addClickListener((Button.ClickListener) event -> onTranslateQuery());
        grid.addComponent(b, 1, 0);
        grid.setComponentAlignment(b, Alignment.MIDDLE_CENTER);

        // run button
		b = new Button("Execute");
//		b.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
//		b.setIcon(FontAwesome.CHEVRON_DOWN);
        b.setDescription("Click to execute the SPARQL query and show the result set in the panel below.");
        b.addClickListener((Button.ClickListener) event -> onExecuteQuery());
		grid.addComponent(b, 0, 1);
		grid.setComponentAlignment(b, Alignment.MIDDLE_CENTER);

        // explain button
		b = new Button("Explain");
//		b.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_RIGHT);
//		b.setIcon(FontAwesome.CHEVRON_RIGHT);
        b.setDescription("Click to see why selected rows belong to result set.");
        b.addClickListener((Button.ClickListener) event -> onExplain());
		grid.addComponent(b, 1, 2);
		grid.setComponentAlignment(b, Alignment.MIDDLE_CENTER);
//
        // central DBpedia image
		grid.addComponent(createKnowledgeBaseSelector(), 1, 1);

		return grid;
		
	}

	private Component createContentWrapper(final Component content) {
		final CssLayout slot = new CssLayout();
		slot.setWidth("100%");
		slot.addStyleName("dashboard-panel-slot");

		CssLayout card = new CssLayout();
		card.setWidth("100%");
		card.addStyleName(ValoTheme.LAYOUT_CARD);

		HorizontalLayout toolbar = new HorizontalLayout();
		toolbar.addStyleName("dashboard-panel-toolbar");
		toolbar.setWidth("100%");

		Label caption = new Label(content.getCaption());
		caption.addStyleName(ValoTheme.LABEL_H4);
		caption.addStyleName(ValoTheme.LABEL_COLORED);
		caption.addStyleName(ValoTheme.LABEL_NO_MARGIN);
		content.setCaption(null);

		MenuBar tools = new MenuBar();
		tools.addStyleName(ValoTheme.MENUBAR_BORDERLESS);
		MenuBar.MenuItem max = tools.addItem("", FontAwesome.EXPAND, (MenuBar.Command) selectedItem -> {
			if (!slot.getStyleName().contains("max")) {
				selectedItem.setIcon(FontAwesome.COMPRESS);
				toggleMaximized(slot, true);
			} else {
				slot.removeStyleName("max");
				selectedItem.setIcon(FontAwesome.EXPAND);
				toggleMaximized(slot, false);
			}
		});
		max.setStyleName("icon-only");
		MenuBar.MenuItem root = tools.addItem("", FontAwesome.COG, null);
		root.addItem("Configure", (MenuBar.Command) selectedItem -> Notification.show("Not implemented in this demo"));
		root.addSeparator();
		root.addItem("Close", (MenuBar.Command) selectedItem -> Notification.show("Not implemented in this demo"));

		toolbar.addComponents(caption, tools);
		toolbar.setExpandRatio(caption, 1);
		toolbar.setComponentAlignment(caption, Alignment.MIDDLE_LEFT);

//		card.addComponents(toolbar, content);
//		slot.addComponent(card);
		VerticalLayout vl = new VerticalLayout();
		vl.setSizeFull();
		vl.addStyleName(ValoTheme.LAYOUT_CARD);
		content.setSizeFull();
		vl.addComponent(content);
		return vl;
	}

	private void toggleMaximized(final Component panel, final boolean maximized) {
		for (Component aRoot : root) {
			aRoot.setVisible(!maximized);
		}
		dashboardPanels.setVisible(true);

		for (Component c : dashboardPanels) {
			c.setVisible(!maximized);
		}

		if (maximized) {
			panel.setVisible(true);
			panel.addStyleName("max");
		} else {
			panel.removeStyleName("max");
		}
	}
	
	private Component createQueryPortal(){
		sparqlQueryInput = new CodeMirror();
		sparqlQueryInput.setTheme(CodeMirrorTheme.DEFAULT);
		sparqlQueryInput.setLanguage(CodeMirrorLanguage.SPARQL);
		sparqlQueryInput.setSizeFull();


//		sparqlQueryInput = new TextArea();
//		sparqlQueryInput.addStyleName("sparqlquery-view");
//		sparqlQueryInput.setCaption("Enter your SPARQL query:");
//		sparqlQueryInput.setSizeFull();

		return createContentWrapper(sparqlQueryInput);
	}
	
	private Component createKnowledgeBaseSelector(){
		VerticalLayout l = new VerticalLayout();
		l.setSizeFull();
		
		IndexedContainer ic = new IndexedContainer();
		ic.addContainerProperty("label", String.class, null);
		for(Knowledgebase kb : Manager.getInstance().getKnowledgebases()){
			ic.addItem(kb);
	        ic.addItem(kb);
		}
        
		knowledgebaseSelector = new NativeSelect();
		knowledgebaseSelector.addStyleName("borderless");
		knowledgebaseSelector.setWidth("100%");
		knowledgebaseSelector.setHeight(null);
		knowledgebaseSelector.setNullSelectionAllowed(false);
		knowledgebaseSelector.setContainerDataSource(ic);
		knowledgebaseSelector.setImmediate(true);
		knowledgebaseSelector.addValueChangeListener((ValueChangeListener) event -> onChangeKnowledgebase());
        l.addComponent(knowledgebaseSelector);
        
		centerLogo = new Embedded("");
		centerLogo.setSizeFull();
		centerLogo.setHeight("80px");
		l.addComponent(centerLogo);
		l.setExpandRatio(centerLogo, 1f);
		
        return l;
	}
	
	private Component createNLRPortal() {
		nlrLabel = new Label();
		nlrLabel.setCaption("Translation");
		nlrLabel.addStyleName("nlr-view");
		nlrLabel.setSizeFull();

		//create option to change language in header of the portal
		HorizontalLayout header = new HorizontalLayout();
		
//		inferVarNamesCheckBox = new CheckBox("Nice var names");
//		inferVarNamesCheckBox.setImmediate(true);
//		header.addComponent(inferVarNamesCheckBox);
		
		NativeSelect languageType = new NativeSelect();
		for (Language lang : Language.values()) {
			languageType.addItem(lang);
        }
		languageType.setNullSelectionAllowed(false);
		languageType.setValue(Language.ENGLISH);
		header.setSizeUndefined();
        header.addComponent(languageType);
        header.setSpacing(true);
        header.setComponentAlignment(languageType, Alignment.MIDDLE_LEFT);

		return createContentWrapper(nlrLabel);
	}
	
	private Component createQueryResultPortal(){
		resultTableHolder = new VerticalLayout();
		resultTableHolder.setSizeFull();
		resultTableHolder.setCaption("Query result");
//		resultTableHolder.setStyleName("white");

		return createContentWrapper(resultTableHolder);
	}
	
	private Component createExplanationsPortal(){
		explanationHolder = new VerticalLayout();
		explanationHolder.setWidth("100%");
		explanationHolder.setHeight(null);
		explanationHolder.setSpacing(true);
		explanationHolder.addStyleName("explanations-view");

		Panel p = new Panel();
		p.setSizeFull();
		p.setContent(explanationHolder);

		return createContentWrapper(p);
	}

	private String getCurrentQuery() {
		return sparqlQueryInput.getCode();
	}
	
	private void onTranslateQuery(){
		String queryStr = getCurrentQuery();
		
		try {
			//check if query has correct syntax
			Query query = QueryFactory.create(queryStr, Syntax.syntaxARQ);
//			if((Boolean) inferVarNamesCheckBox.getValue()){
//				query = UserSession.getManager().replaceVarsWithTypes(query);
//			}
			//generate nlr
			nlrLabel.setValue(SPARQL2NLSession.getManager().getNLR(query));
		} catch (QueryException e) {
			Notification.show("Could not parse SPARQL query!",
							  e.getLocalizedMessage(),
							  Notification.Type.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	private void onExplain(QuerySolution qs){
		if(currentQuery != null){
			explanationHolder.removeAllComponents();
			addExplanation(qs);
		}
	}
	
	private void onExplain(){
		if(currentQuery != null){
			if(currentTable != null){
				explanationHolder.removeAllComponents();
				Set<Object> itemIds = (Set<Object>) currentTable.getValue();
				for(Object itemId : itemIds){
					addExplanation((QuerySolution) itemId);
				}
			}
		}
	}
	
	public void reset(){
		SPARQL2NLSession.getManager().reset();
		knowledgebaseSelector.setValue(SPARQL2NLSession.getManager().getCurrentKnowledgebase());
		sparqlQueryInput.setCode(QueryFactory.create(SPARQL2NLSession.getManager().getCurrentKnowledgebase().getExampleQuery(), Syntax.syntaxARQ).toString());
		resultTableHolder.removeAllComponents();
		explanationHolder.removeAllComponents();
		centerLogo.setSource(SPARQL2NLSession.getManager().getCurrentKnowledgebase().getIcon());
		centerLogo.setDescription(SPARQL2NLSession.getManager().getCurrentKnowledgebase().getDescription());
	}
	
	private void onChangeKnowledgebase(){
		Knowledgebase kb = (Knowledgebase) knowledgebaseSelector.getValue();
		if(kb.getIcon() != null){
			centerLogo.setSource(kb.getIcon());
			centerLogo.setDescription(kb.getDescription());
		}
		sparqlQueryInput.setCode(QueryFactory.create(kb.getExampleQuery(), Syntax.syntaxARQ).toString());
		SPARQL2NLSession.getManager().setCurrentKnowledgebase(kb);
		explanationHolder.removeAllComponents();
		resultTableHolder.removeAllComponents();
		nlrLabel.setValue(null);
	}
	
	private void addExplanation(QuerySolution qs){
		String exp = SPARQL2NLSession.getManager().getExplanationNLR(currentQuery, qs, true);
		Label l = new Label(exp, ContentMode.HTML);
		l.addStyleName("explanation");
		l.setWidth("90%");
		l.setHeight(null);
		explanationHolder.addComponent(l);
		explanationHolder.setComponentAlignment(l, Alignment.MIDDLE_CENTER);
	}
	
	private void onExecuteQuery(){
		String queryStr = sparqlQueryInput.getCode();
		
		try {
			//check if query has correct syntax
			currentQuery = QueryFactory.create(queryStr, Syntax.syntaxARQ);
			//table for results
			final Table table = new Table();
			table.setSelectable(true);
			table.setMultiSelect(true);
			table.setMultiSelectMode(MultiSelectMode.DEFAULT);
			table.addItemClickListener(new ItemClickEvent.ItemClickListener() {
	            private static final long serialVersionUID = 2068314108919135281L;

	            public void itemClick(ItemClickEvent event) {
	                if (event.isDoubleClick()) {
	                     onExplain((QuerySolution) event.getItemId());
	                }
	            }
	        });
			
			
			for(Var var : currentQuery.getProjectVars()){
				table.addContainerProperty(var.toString(), Label.class,  null);
			}

			ResultSet rs = SPARQL2NLSession.getManager().executeSelect(currentQuery);
			QuerySolution qs;
			List<Object> row;
			int i = 1;
			List<String> uriRows = new ArrayList<>();
			while(rs.hasNext()){
				qs = rs.next();
				row = new ArrayList<>();
				
//				Item item = table.getItem(qs);
//				
				for(Var var : currentQuery.getProjectVars()){
					String varName = var.getVarName();
					RDFNode node = qs.get(varName);
					if(node != null){
//						if(node.isURIResource() && !uriRows.contains(varName)){
//							uriRows.add(varName);
//						}
//						row.add(node.toString());
						Label l;
						if(node.isURIResource()){
							String uri = node.asResource().getURI();
							l = new Label("<a href=\"" + uri + "\" target=\"_blank\">" + uri + "</a>", ContentMode.HTML);
							l.addStyleName("table-link");
						} else {
							l = new Label(node.toString());
						}
						row.add(l);
					} else {
						row.add(null);
					}
					
				}
				
//				table.addItem(row.toArray(), new Integer(i++));
				table.addItem(row.toArray(), qs);
			}
//			for(final String uriRowVar : uriRows){
//				table.addGeneratedColumn(uriRowVar, new Table.ColumnGenerator() {
//		            public Component generateCell(Table source, Object itemId,
//		                    Object columnId) {
//		                Item item = table.getItem(itemId);
//		                String uri = (String) item.getItemProperty(uriRowVar).getValue();
//		                Label l = new Label("<a href=\"" + uri + "\">" + uri + "</a>", Label.CONTENT_XHTML);
//		             
//		                // the Link -component:
//		                Link link = new Link(uri, new ExternalResource(uri));
//		                return link;
//		            }
//
//		        });
//			}
			
			table.setSizeFull();
			resultTableHolder.removeAllComponents();
			resultTableHolder.addComponent(table);
			resultTableHolder.setExpandRatio(table, 1f);
			currentTable = table;
			
		} catch (QueryException e) {
			Notification.show("Could not execute SPARQL query!",
							  e.getLocalizedMessage(),
							  Notification.Type.ERROR_MESSAGE);
			e.printStackTrace();
		}
		
	}

	@Override
	public void enter(ViewChangeListener.ViewChangeEvent event) {

	}
}
