package org.sparql2nl.demo;

import com.vaadin.annotations.Theme;
import com.vaadin.navigator.Navigator;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.UI;
import org.sparql2nl.demo.view.MainView;

/**
 * @author Lorenz Buehmann
 */
@Theme("sparql2nl")
public class SPARQL2NLDemoUI extends UI{

	Navigator navigator;

	protected static final String MAINVIEW = "main";

	@Override
	protected void init(VaadinRequest request) {
		getPage().setTitle("SPARQL2NL Demo");

		SPARQL2NLSession.init();
		Manager.getInstance().loadSettings();
		Manager.getInstance().init();

		// Create a navigator to control the views
		navigator = new Navigator(this, this);

		// Create and register the views
//		navigator.addView("", new StartView());
		navigator.addView("", new MainView());
	}
}
