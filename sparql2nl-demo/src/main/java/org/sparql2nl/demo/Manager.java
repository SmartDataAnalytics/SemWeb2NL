package org.sparql2nl.demo;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sparql2nl.demo.model.Knowledgebase;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Manager {

	private static final Logger logger = LoggerFactory.getLogger(Manager.class);

	private static Manager instance;

	private boolean useBOA;
	private String cacheDir;

	private List<Knowledgebase> knowledgebases = new ArrayList<Knowledgebase>();

	private Manager() {
	}

	public static synchronized Manager getInstance() {
		if (instance == null) {
			instance = new Manager();
		}
		return instance;
	}

	public List<Knowledgebase> getKnowledgebases() {
		return knowledgebases;
	}

	public void init() {
		knowledgebases = KnowledgebaseLoader.loadDatasets(
				this.getClass().getClassLoader().getResource("knowledgebases.xml").getPath());
	}

	public void loadSettings() {
		logger.info("loading settings from {}", this.getClass().getClassLoader().getResource("settings.ini").getPath());

		try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("settings.ini")) {
			Ini ini = new Ini(is);
			//base section
			Section baseSection = ini.get("base");
			useBOA = baseSection.get("useBOA", Boolean.class);
			if (baseSection.get("cacheDir", String.class) != null) {
				cacheDir = baseSection.get("cacheDir", String.class);
			}

			logger.info(showSettings());
		} catch (Exception e) {
			logger.info("loading settings failed", e);
		}
	}

	private String showSettings() {
		return "Settings:" +
				"\ncache dir: " + cacheDir +
				"\nuse BOA: " + useBOA;
	}

	public boolean isUseBOA() {
		return useBOA;
	}

	public String getCacheDir() {
		return cacheDir;
	}

	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}
}
