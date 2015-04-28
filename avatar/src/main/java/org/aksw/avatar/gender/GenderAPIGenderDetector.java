/**
 * 
 */
package org.aksw.avatar.gender;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * Returns the gender of a name by using Gender API at https://gender-api.com/ 
 * @author Lorenz Buehmann
 *
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
				//get the gender value
				String gender = json.get("gender").getAsString();
				//parse one of the possible values male, female, unknown
				return Gender.valueOf(gender.toUpperCase());
			} catch(Exception e){
				e.printStackTrace();
			} finally{
				conn.disconnect();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return Gender.UNKNOWN;
	}
}
