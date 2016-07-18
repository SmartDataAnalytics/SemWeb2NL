package org.sparql2nl.demo;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener("Context listener for SPARQL2NL Demo.")
public class SPARQL2NLDemoContextListener implements ServletContextListener{
	

	@Override
	public void contextDestroyed(ServletContextEvent e) {
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {
		Manager.getInstance().loadSettings();
		Manager.getInstance().init();
	}
	
	private static String getParameter(ServletContext servletContext, String key, String defaultValue) {
        String value = servletContext.getInitParameter(key);
        return value == null ? defaultValue : value;
    }

}
