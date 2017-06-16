package org.aksw.triple2nl.util;

/**
 * @author Lorenz Buehmann
 */
public class Acronyms {

	public static boolean isAcronym(String s) {
		return s.matches("\\b[A-Z]{2,4}\\b");
	}

	public static void main(String[] args) throws Exception {
		String[] tests = {"on", "BMW", "ABBA", "but"};
		for (String s : tests) {
			System.out.println(s + ":" + Acronyms.isAcronym(s));
		}

	}
}
