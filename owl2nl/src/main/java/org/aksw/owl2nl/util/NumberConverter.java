package org.aksw.owl2nl.util;

/**
 * A converter between numbers and its natural language representation.
 *
 * @author Lorenz Buehmann
 */
public class NumberConverter {

	private static final String[] words = {"zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};

	/**
	 * Returns the the token used in text for numbers between zero and ten, otherwise the number itself.
	 * @param number the number
	 * @return the token
	 */
	public static String convert(int number) {
		String token;
		if(number > words.length) {
			token = String.valueOf(number);
		} else {
			token = words[number];
		}
		return token;
	}
}
