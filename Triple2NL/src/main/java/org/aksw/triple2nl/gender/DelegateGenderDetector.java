package org.aksw.triple2nl.gender;

import java.util.ArrayList;
import java.util.List;

/**
 * A delegating gender detector that goes through all given detectors until a gender was found.
 *
 * @author Lorenz Buehmann
 */
public class DelegateGenderDetector implements GenderDetector{

	private List<GenderDetector> detectors = new ArrayList<>();

	/**
	 * @param detectors a list of gender detectors which are used in the given order
	 */
	public DelegateGenderDetector(List<GenderDetector> detectors) {
		this.detectors = detectors;
	}

	@Override
	public Gender getGender(String name) {
		for (GenderDetector detector : detectors) {
			Gender gender = detector.getGender(name);
			if(gender != Gender.UNKNOWN) {
				return gender;
			}
		}
		return Gender.UNKNOWN;
	}
}
