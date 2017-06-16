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

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Returns the gender of a name by using Gender API at https://gender-api.com/
 *
 * @author Lorenz Buehmann
 */
public class GenderAPIGenderDetector implements GenderDetector {

	private static final String API_URL = "https://gender-api.com/get?name=";

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.aksw.avatar.gender.GenderDetector#getGender(java.lang.String)
	 */
	@Override
	public Gender getGender(String name) {
		try {
			URL url = new URL(API_URL + name);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Error: " + conn.getResponseCode());
			}
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				Gson gson = new Gson();
				//JSON structure:{"name":"bob","gender":"male","samples":15549,"accuracy":99,"duration":"39ms"}
				JsonObject json = gson.fromJson(reader, JsonObject.class);
				System.out.println(json);
				//get the gender value
				String gender = json.get("gender").getAsString();
				//parse one of the possible values male, female, unknown
				return Gender.valueOf(gender.toUpperCase());
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				conn.disconnect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Gender.UNKNOWN;
	}
}
