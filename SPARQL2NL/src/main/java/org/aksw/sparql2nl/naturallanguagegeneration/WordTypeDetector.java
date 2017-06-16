/*
 * #%L
 * SPARQL2NL
 * %%
 * Copyright (C) 2015 Agile Knowledge Engineering and Semantic Web (AKSW)
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.aksw.sparql2nl.naturallanguagegeneration;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.IndexWord;
import net.sf.extjwnl.data.POS;
import net.sf.extjwnl.data.Synset;
import net.sf.extjwnl.dictionary.Dictionary;

public class WordTypeDetector {
	
	private static final Logger logger = LoggerFactory.getLogger(WordTypeDetector.class);
	
	public Dictionary dict;	
	
	private static final double THRESHOLD = 0.5;
	private boolean stemWords = true;
	
	public WordTypeDetector() {
		try {
			dict = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			logger.error("Failed to load WordNet.", e);
		}
	}
	
	public boolean isNoun(String keyword) {
		System.out.println(keyword);
		try {
			String token = getFirstToken(keyword);
			
			int nrOfNounSenses = 0;
			int nrOfVerbSenses = 0;
			
			// get NOUN senses
			IndexWord iw;
			if(stemWords){
				iw = dict.getMorphologicalProcessor().lookupBaseForm(POS.NOUN, token);
			} else {
				iw = dict.getIndexWord(POS.NOUN, token);
			}
			if(iw != null){
				List<Synset> synsets = iw.getSenses();
				nrOfNounSenses = synsets.size();
			}
			//get VERB senses
			if(stemWords){
				iw = dict.getMorphologicalProcessor().lookupBaseForm(POS.VERB, token);
			} else {
				iw = dict.getIndexWord(POS.VERB, token);
			}
			System.out.println("#Nouns: " + nrOfNounSenses);
			System.out.println("#Verbs: " + nrOfVerbSenses);
			
			double score = Math.abs(Math.log((double)nrOfVerbSenses/nrOfNounSenses));
			System.out.println("Score: " + score);
			if(score > THRESHOLD){
				return true;
			}
			
			/*
			if(nrOfNounSenses == 0 && nrOfVerbSenses != 0){
				return false;
			} else if(nrOfNounSenses != 0 && nrOfVerbSenses == 0){
				return true;
			} else if(nrOfNounSenses == 0 && nrOfVerbSenses == 0){
				return false;
			} else {
				if((double)nrOfNounSenses/nrOfVerbSenses < THRESHOLD){
					return false;
				} else if((double)nrOfVerbSenses/nrOfNounSenses < THRESHOLD){
					return true;
				}
			}*/
			
			
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	private String getFirstToken(String phrase){
		String[] tokens = phrase.split(" ");
        return tokens[0];
	}
	
	public static void main(String[] args) {
		System.out.println(new WordTypeDetector().isNoun("is"));
		System.out.println(new WordTypeDetector().isNoun("is part of"));
		System.out.println(new WordTypeDetector().isNoun("population total"));
		System.out.println(new WordTypeDetector().isNoun("star"));
		System.out.println(new WordTypeDetector().isNoun("starring"));
		System.out.println(new WordTypeDetector().isNoun("award"));
		System.out.println(new WordTypeDetector().isNoun("key"));
		System.out.println(new WordTypeDetector().isNoun("spouse"));
	}
}
