package org.aksw.triple2nl.gender;

import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

/**
 * @author Lorenz Buehmann
 */
public class GeneralGenderDictionary extends GenderDictionary {

	public static String MALE_GENDER_FILE_LOCATION = "gender/male.txt";
	public static String FEMALE_GENDER_FILE_LOCATION = "gender/female.txt";

	public GeneralGenderDictionary() {
		try {
			ClassPathResource maleResource = new ClassPathResource(MALE_GENDER_FILE_LOCATION);
			ClassPathResource femaleResource = new ClassPathResource(FEMALE_GENDER_FILE_LOCATION);

			male = new BufferedReader(new InputStreamReader(
					maleResource.getInputStream(), StandardCharsets.UTF_8))
					.lines().map(name -> name.toLowerCase()).collect(Collectors.toSet());

			female = new BufferedReader(new InputStreamReader(
					femaleResource.getInputStream(), StandardCharsets.UTF_8))
					.lines().map(name -> name.toLowerCase()).collect(Collectors.toSet());
		} catch (IOException e) {
			e.printStackTrace();
		}


		setCaseSensitive(false);
	}
}
