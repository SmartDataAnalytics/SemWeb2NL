package org.aksw.triple2nl.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * Class holds a set of prepositions.
 * @author Axel Ngonga
 */
public class Preposition extends HashSet<String> {

	private static final String filename = "preposition_list.txt";

	public Preposition(InputStream is) {
		try (BufferedReader bufRdr = new BufferedReader(new InputStreamReader(is))) {
			String line;
			while ((line = bufRdr.readLine()) != null) {
				add(line.toLowerCase().trim());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Preposition() {
		this(Preposition.class.getClassLoader().getResourceAsStream(filename));
	}

	/**
	 * Determines whether the given token is contained in the list of prepositions.
	 * @param s the input token
	 * @return TRUE if the token is a preposition, otherwise FALSE
	 */
	public boolean isPreposition(String s) {
		return contains(s);
	}
}
