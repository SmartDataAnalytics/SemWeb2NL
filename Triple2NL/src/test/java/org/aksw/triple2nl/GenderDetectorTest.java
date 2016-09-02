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
package org.aksw.triple2nl;

import com.google.common.collect.Lists;
import org.aksw.triple2nl.gender.*;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

/**
 * @author Lorenz Buehmann
 *
 */
public class GenderDetectorTest {

	/**
	 * Test method for {@link GenderAPIGenderDetector#getGender(String)}.
	 */
	@Ignore("needs API key")
	@Test
	public void testGetGenderGenderAPI() {
		Map<String, Gender> data = new HashMap<>();
		data.put("bob", Gender.MALE);
		data.put("usain", Gender.UNKNOWN);
		data.put("alice", Gender.FEMALE);
		data.put("frank", Gender.MALE);

		check(new GenderAPIGenderDetector(), data);
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.gender.CoreNLPGenderDetector#getGender(String)}.
	 */
	@Test
	public void testGetGenderCoreNLP() {
		Map<String, Gender> data = new HashMap<>();
		data.put("bob", Gender.MALE);
		data.put("usain", Gender.UNKNOWN);
		data.put("alice", Gender.UNKNOWN);
		data.put("frank", Gender.MALE);

		check(new CoreNLPGenderDetector(), data);
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.gender.DictionaryBasedGenderDetector#getGender(String)}.
	 */
	@Test
	public void testGetGenderDictionaryBased() {
		Map<String, Gender> data = new HashMap<>();
		data.put("bob", Gender.MALE);
		data.put("usain", Gender.UNKNOWN);
		data.put("alice", Gender.FEMALE);
		data.put("frank", Gender.MALE);

		check(new DictionaryBasedGenderDetector(), data);
	}

	/**
	 * Test method for {@link org.aksw.triple2nl.gender.DictionaryBasedGenderDetector#getGender(String)}.
	 */
	@Test
	public void testGetGenderDelegate() {
		Map<String, Gender> data = new HashMap<>();
		data.put("bob", Gender.MALE);
		data.put("usain", Gender.UNKNOWN);
		data.put("alice", Gender.FEMALE);
		data.put("frank", Gender.MALE);

		List<GenderDetector> genderDetectors = Lists.newArrayList(
				new DictionaryBasedGenderDetector(new DBpediaGenderDictionary()),
				new DictionaryBasedGenderDetector(new GeneralGenderDictionary()));

		check(new DelegateGenderDetector(genderDetectors), data);
	}

	private void check(GenderDetector genderDetector, Map<String, Gender> data) {
		data.forEach((name, gender) -> {
			Gender learnedGender = genderDetector.getGender(name);
			Assert.assertThat(learnedGender, is(gender));
		});
	}

}
