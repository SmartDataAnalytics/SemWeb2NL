package org.sparql2nl.demo;

import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.ServiceException;
import com.vaadin.server.SessionInitEvent;
import com.vaadin.server.SessionInitListener;
import com.vaadin.server.VaadinServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

/**
 * @author Lorenz Buehmann
 */
@WebServlet(value = "/*",
		asyncSupported = true)
@VaadinServletConfiguration(
		productionMode = true,
		ui = SPARQL2NLDemoUI.class,
		widgetset = "org.sparql2nl.demo.CustomWidgetset")
public class SPARQL2NLServlet extends VaadinServlet implements SessionInitListener {
	@Override
	protected final void servletInitialized() throws ServletException {
		super.servletInitialized();
	}

	@Override
	public void sessionInit(SessionInitEvent event) throws ServiceException {
//		Manager.getInstance().loadSettings();
//		Manager.getInstance().init();
	}
}
