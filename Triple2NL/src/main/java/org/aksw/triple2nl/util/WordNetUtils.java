package org.aksw.triple2nl.util;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Utilities for WordNet.
 *
 * @author Lorenz Buehmann
 */
public class WordNetUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(WordNetUtils.class);

	private Dictionary dict;

	public WordNetUtils() {
		try {
			dict = Dictionary.getDefaultResourceInstance();
		} catch (JWNLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Returns the derived adjective with the same word form for the most common sense of the given noun if exists.
	 *
	 * @param noun the noun
	 */
	public String getDerivedAdjective(String noun) {
		try {
			IndexWord nounIW = dict.lookupIndexWord(POS.NOUN, noun);

			List<Synset> senses = nounIW.getSenses();

			Synset mainSense = senses.get(0);

			List<Pointer> pointers = mainSense.getPointers(PointerType.DERIVATION);

			for (Pointer pointer : pointers) {
				Synset derivedSynset = pointer.getTargetSynset();
				if(derivedSynset.getPOS() == POS.ADJECTIVE) {
//					return derivedSynset.getWords().get(0).getLemma();
				}
				if(derivedSynset.getPOS() == POS.VERB) {
					System.out.println(derivedSynset);
				}
			}
		} catch (JWNLException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * Gets all synsets for the given word as VERB and NOUN.
	 *
	 * @param word the word
	 * @return a representative word for each synset
	 */
	public List<String> getAllSynsets(String word) {
		List<String> synsets = new ArrayList<>();

		try {
			// noun synsets
			IndexWord iw = dict.getIndexWord(POS.NOUN, word);
			if(iw != null) {
				for (Synset synset : iw.getSenses()) {
					synsets.add("NOUN " + synset.getWords().get(0).getLemma());
				}
			}

			// verb synsets
			iw = dict.getIndexWord(POS.VERB, word);
			if(iw != null) {
				for (Synset synset : iw.getSenses()) {
					synsets.add("VERB " + synset.getWords().get(0).getLemma());
				}
			}
		} catch (JWNLException e) {
			LOGGER.error("WordNet lookup failed.", e);
		}

		return synsets;
	}

	public static void main(String[] args) throws Exception {
		String[] nouns = {"female", "male", "person", "book", "actor"};
		WordNetUtils utils = new WordNetUtils();

		for (String noun : nouns) {
			System.out.println(noun + ":");
			System.out.println(utils.getDerivedAdjective(noun));
		}
	}
}
