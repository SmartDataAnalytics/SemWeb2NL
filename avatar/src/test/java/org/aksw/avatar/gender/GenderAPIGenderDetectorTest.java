/**
 * 
 */
package org.aksw.avatar.gender;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Lorenz Buehmann
 *
 */
public class GenderAPIGenderDetectorTest {

	/**
	 * Test method for {@link org.aksw.avatar.gender.GenderAPIGenderDetector#getGender(java.lang.String)}.
	 */
	@Test
	public void testGetGender() {
		GenderDetector genderDetector = new GenderAPIGenderDetector();
		
		Gender gender = genderDetector.getGender("bob");
		assertTrue(gender == Gender.MALE);
		
		gender = genderDetector.getGender("usain");
		assertTrue(gender == Gender.MALE);
		
		gender = genderDetector.getGender("alice");
		assertTrue(gender == Gender.FEMALE);
		
		gender = genderDetector.getGender("franck");
		assertTrue(gender == Gender.MALE);
	}

}
