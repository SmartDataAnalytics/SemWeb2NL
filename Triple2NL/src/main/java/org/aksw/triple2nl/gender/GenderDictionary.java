package org.aksw.triple2nl.gender;

import java.util.HashSet;
import java.util.Set;

/**
 * A dictionary that comprises two sets of names for male and female gender.
 *
 * @author Lorenz Buehmann
 */
public class GenderDictionary {

	protected Set<String> male = new HashSet<>();
	protected Set<String> female = new HashSet<>();

	private boolean caseSensitive = true;

	protected GenderDictionary(){}

	/**
	 * @param male   the set of male gender names
	 * @param female the set of female gender names
	 */
	public GenderDictionary(Set<String> male, Set<String> female) {
		this.male = male;
		this.female = female;
	}

	/**
	 * Checks whether the name is contained in the list of male gender names.
	 *
	 * @param name the name
	 * @return whether the name is contained in the list of male gender names
	 */
	public boolean isMale(String name) {
		return male.contains(caseSensitive ? name : name.toLowerCase());
	}

	/**
	 * Checks whether the name is contained in the list of female gender names.
	 *
	 * @param name the name
	 * @return whether the name is contained in the list of female gender names
	 */
	public boolean isFemale(String name) {
		return female.contains(caseSensitive ? name : name.toLowerCase());
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}
}
