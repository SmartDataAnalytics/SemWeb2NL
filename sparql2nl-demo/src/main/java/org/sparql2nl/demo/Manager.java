package org.sparql2nl.demo;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.ini4j.Ini;
import org.ini4j.InvalidFileFormatException;
import org.ini4j.Profile.Section;
import org.sparql2nl.demo.model.Knowledgebase;

public class Manager {
	
	private static final Logger logger = Logger.getLogger(Manager.class);
	
	private static Manager instance;
	
	private boolean useBOA;
	private String cacheDir;
	private String wordnetDir;
	
	private List<Knowledgebase> knowledgebases;
	
	private Manager() {
        knowledgebases = new ArrayList<Knowledgebase>();
	}
	
	public void setCacheDir(String cacheDir) {
		this.cacheDir = cacheDir;
	}
	
	public static synchronized Manager getInstance() {
		if(instance == null){
			instance = new Manager();
		}
		return instance;
	}
	
	public List<Knowledgebase> getKnowledgebases() {
		return knowledgebases;
	}
	
	public void init(){
		 knowledgebases = KnowledgebaseLoader.loadDatasets(this.getClass().getClassLoader().getResource("knowledgebases.xml").getPath());
	}
	
	
	public void loadSettings(){
		InputStream is;
		
		try {
			is = this.getClass().getClassLoader().getResourceAsStream("settings.ini");
			Ini ini = new Ini(is);
			//base section
			Section baseSection = ini.get("base");
			useBOA = baseSection.get("useBOA", Boolean.class);
			if(baseSection.get("cacheDir", String.class) != null){
				cacheDir = baseSection.get("cacheDir", String.class);
			}
			wordnetDir = baseSection.get("wordnetDir", String.class);
		} catch (InvalidFileFormatException e2) {
			e2.printStackTrace();
		} catch (IOException e2) {
			e2.printStackTrace();
		}
	}
	
	public boolean isUseBOA() {
		return useBOA;
	}
	
	public String getWordnetDir() {
		return wordnetDir;
	}
	
	public String getCacheDir() {
		return cacheDir;
	}
}
