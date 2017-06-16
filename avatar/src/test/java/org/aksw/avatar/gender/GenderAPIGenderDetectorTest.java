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
