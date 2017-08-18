/*
 * #%L
 * AVATAR
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
package org.aksw.triple2nl.gender;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Detects the gender based on two lists of common names for male and female.
 *
 * @author Lorenz Buehmann
 */
public class DictionaryBasedGenderDetector implements GenderDetector {

	private final GenderDictionary dictionary;

	public DictionaryBasedGenderDetector() {
		this(new GeneralGenderDictionary());
	}

	public DictionaryBasedGenderDetector(GenderDictionary dictionary) {
		this.dictionary = dictionary;
	}

	/*
	 * (non-Javadoc) @see
	 * org.aksw.sparql2nl.entitysummarizer.gender.GenderDetector#getGender(java.lang.String)
	 */
	@Override
	public Gender getGender(String iri, String name) {
		String searchName = name;
		// check if name is compound
		String[] words = name.split(" ");
		if (words.length > 1) {
			searchName = words[0];
		}

		if (dictionary.isMale(searchName)) {
			return Gender.MALE;
		} else if (dictionary.isFemale(searchName)) {
			return Gender.FEMALE;
		} else {
			return Gender.UNKNOWN;
		}
	}

	public static void main(String[] args) throws Exception {
		DictionaryBasedGenderDetector genderDetector = new DictionaryBasedGenderDetector();
		System.out.println(genderDetector.getGender(null,"Axel"));
	}
}
