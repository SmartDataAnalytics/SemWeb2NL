package org.aksw.triple2nl.gender;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lorenz Buehmann
 */
public class GeneralGenderDictionary extends GenderDictionary {

	public static String MALE_GENDER_FILE_LOCATION = "gender/male.txt";
	public static String FEMALE_GENDER_FILE_LOCATION = "gender/female.txt";

	public GeneralGenderDictionary() {
		try {
			male = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(MALE_GENDER_FILE_LOCATION).getPath()))
					.stream().map(name -> name.toLowerCase()).collect(Collectors.toSet());
			female = Files.readAllLines(Paths.get(getClass().getClassLoader().getResource(FEMALE_GENDER_FILE_LOCATION).getPath()))
					.stream().map(name -> name.toLowerCase()).collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}

		setCaseSensitive(false);
	}
}
