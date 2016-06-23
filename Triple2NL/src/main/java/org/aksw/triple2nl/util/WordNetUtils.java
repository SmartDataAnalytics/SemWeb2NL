package org.aksw.triple2nl.util;

import net.sf.extjwnl.JWNLException;
import net.sf.extjwnl.data.*;
import net.sf.extjwnl.dictionary.Dictionary;

import java.util.List;

/**
 * @author Lorenz Buehmann
 */
public class WordNetUtils {

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

	public static void main(String[] args) throws Exception {
		String[] nouns = {"female", "male", "person", "book", "actor"};
		WordNetUtils utils = new WordNetUtils();

		for (String noun : nouns) {
			System.out.println(noun + ":");
			System.out.println(utils.getDerivedAdjective(noun));
		}
	}
}
