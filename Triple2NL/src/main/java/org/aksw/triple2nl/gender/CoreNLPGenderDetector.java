package org.aksw.triple2nl.gender;

import edu.stanford.nlp.ie.machinereading.structure.MachineReadingAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.GenderAnnotator;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import org.aksw.triple2nl.nlp.StanfordCoreNLPWrapper;

import java.util.Properties;

/**
 * @author Lorenz Buehmann
 */
public class CoreNLPGenderDetector implements GenderDetector {

	private final StanfordCoreNLPWrapper pipeline;

	public CoreNLPGenderDetector(StanfordCoreNLPWrapper pipeline) {
		this.pipeline = pipeline;
		pipeline.addAnnotator(new GenderAnnotator());
	}

	public CoreNLPGenderDetector() {
		Properties props = new Properties();
		props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner,gender");
		props.put("ssplit.isOneSentence","true");

		pipeline = new StanfordCoreNLPWrapper(new StanfordCoreNLP(props));
	}



	@Override
	public Gender getGender(String name) {
		Annotation document = new Annotation(name);

		pipeline.annotate(document);

		for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
			for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
				String gender = token.get(MachineReadingAnnotations.GenderAnnotation.class);
//				System.out.println(token + ":" + gender);
				if(gender != null) {
					if(gender.equals("MALE")) {
						return Gender.MALE;
					} else if(gender.equals("FEMALE")) {
						return Gender.FEMALE;
					}
				}
			}
		}
		return Gender.UNKNOWN;
	}
}
