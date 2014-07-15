/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.triple2nl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

/**
 * Class holds a set of prepositions.
 * @author ngonga
 */
public class Preposition extends HashSet<String> {

	private static final String filename = "preposition_list.txt";

	public Preposition(InputStream is) {
		try (BufferedReader bufRdr = new BufferedReader(new InputStreamReader(is))) {
			String line = null;
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

	public boolean isPreposition(String s) {
		return contains(s);
	}
}
