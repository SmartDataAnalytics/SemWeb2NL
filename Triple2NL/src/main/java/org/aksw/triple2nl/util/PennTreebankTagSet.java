/*
 * #%L
 * Triple2NL
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
/**
 * 
 */
package org.aksw.triple2nl.util;

/**
 * The part-of-speech tags used in the Penn Treebank Project.
 * @author Lorenz Buehmann
 *
 */
public enum PennTreebankTagSet {
	ADJECTIVE("JJ"), ADJECTIVE_COMPARATIVE(ADJECTIVE + "R"), ADJECTIVE_SUPERLATIVE(ADJECTIVE + "S"),

	/*
	 * This category includes most words that end in -ly as well as degree
	 * words like quite, too and very, posthead modi ers like enough and
	 * indeed (as in good enough, very well indeed), and negative markers like
	 * not, n't and never.
	 */
	ADVERB("RB"),

	/*
	 * Adverbs with the comparative ending -er but without a strictly
	 * comparative
	 * meaning, like <i>later</i> in <i>We can always come by later</i>, should
	 * simply be tagged as RB.
	 */
	ADVERB_COMPARATIVE(ADVERB + "R"), ADVERB_SUPERLATIVE(ADVERB + "S"),

	/*
	 * This category includes how, where, why, etc.
	 */
	ADVERB_WH("W" + ADVERB),

	/*
	 * This category includes and, but, nor, or, yet (as in Y et it's cheap,
	 * cheap yet good), as well as the mathematical operators plus, minus, less,
	 * times (in the sense of "multiplied by") and over (in the sense of
	 * "divided
	 * by"), when they are spelled out. <i>For</i> in the sense of "because" is
	 * a coordinating conjunction (CC) rather than a subordinating conjunction.
	 */
	CONJUNCTION_COORDINATING("CC"), CONJUNCTION_SUBORDINATING("IN"), CARDINAL_NUMBER("CD"), DETERMINER("DT"),

	/*
	 * This category includes which, as well as that when it is used as a
	 * relative pronoun.
	 */
	DETERMINER_WH("W" + DETERMINER), EXISTENTIAL_THERE("EX"), FOREIGN_WORD("FW"),
	
	LIST_ITEM_MARKER("LS"),

	NOUN("NN"), NOUN_PLURAL(NOUN + "S"), NOUN_PROPER_SINGULAR(NOUN + "P"), NOUN_PROPER_PLURAL(NOUN + "PS"),

	PREDETERMINER("PDT"), POSSESSIVE_ENDING("POS"),

	PRONOUN_PERSONAL("PRP"), PRONOUN_POSSESSIVE("PRP$"),

	/*
	 * This category includes the wh-word whose.
	 */
	PRONOUN_POSSESSIVE_WH("WP$"),

	/*
	 * This category includes what, who and whom.
	 */
	PRONOUN_WH("WP"),

	PARTICLE("RP"),

	/*
	 * This tag should be used for mathematical, scientific and technical
	 * symbols
	 * or expressions that aren't English words. It should not used for any and
	 * all technical expressions. For instance, the names of chemicals, units of
	 * measurements (including abbreviations thereof) and the like should be
	 * tagged as nouns.
	 */
	SYMBOL("SYM"), TO("TO"),

	/*
	 * This category includes my (as in M y, what a gorgeous day), oh, please,
	 * see (as in See, it's like this), uh, well and yes, among others.
	 */
	INTERJECTION("UH"),

	VERB("VB"), VERB_PAST_TENSE(VERB + "D"), VERB_PARTICIPLE_PRESENT(VERB + "G"), VERB_PARTICIPLE_PAST(VERB + "N"), VERB_SINGULAR_PRESENT_NONTHIRD_PERSON(
			VERB + "P"), VERB_SINGULAR_PRESENT_THIRD_PERSON(VERB + "Z"),

	/*
	 * This category includes all verbs that don't take an -s ending in the
	 * third person singular present: can, could, (dare), may, might, must,
	 * ought, shall, should, will, would.
	 */
	VERB_MODAL("MD"),

	
	
	//clause level
	S("S"),
	SBAR("SBAR"),
	SBARQ("SBARQ"),
	SINV("SINV"),
	SQ("SQ"),
	
	
	//phrase level
	ADJECTIVE_PHRASE("ADJP"),
	ADVERB_PHRASE("ADVP"),
	NOUN_PHRASE("NP"),
	VERB_PHRASE("VP"),
	PREPOSITIONAL_PHRASE("PP"),
	FRAGMENT("FRAG"),
	
	
	
	
	//punctuation
	/*
	 * Stanford.
	 */
	SENTENCE_TERMINATOR(".");
	

	private final String tag;

	PennTreebankTagSet(String tag) {
		this.tag = tag;
	}

	/**
	 * Returns the encoding for this part-of-speech.
	 * 
	 * @return A string representing a Penn Treebank encoding for an English
	 *         part-of-speech.
	 */
	public String toString() {
		return getTag();
	}

	/**
	 * @return the part-of-speech tag
	 */
	public String getTag() {
		return this.tag;
	}

	public static PennTreebankTagSet get(String value) {
		for (PennTreebankTagSet v : values()) {
			if (value.equals(v.getTag())) {
				return v;
			}
		}

		throw new IllegalArgumentException("Unknown part of speech: '" + value + "'.");
	}
}
