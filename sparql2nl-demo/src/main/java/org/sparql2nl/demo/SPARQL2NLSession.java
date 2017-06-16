package org.sparql2nl.demo;

import com.vaadin.server.VaadinSession;

/**
 * @author Lorenz Buehmann
 */
public class SPARQL2NLSession {

	protected static final String MANAGER = "manager";

	public static void init() {
		VaadinSession.getCurrent().setAttribute(MANAGER, new SPARQL2NLManager());
	}

	public static SPARQL2NLManager getManager() {
		return (SPARQL2NLManager) VaadinSession.getCurrent().getAttribute(MANAGER);
	}
}
