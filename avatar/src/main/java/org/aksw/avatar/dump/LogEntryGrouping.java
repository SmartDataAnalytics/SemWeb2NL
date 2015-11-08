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
package org.aksw.avatar.dump;

import java.util.Collection;
import java.util.Date;

import com.google.common.collect.Multimap;
import com.google.common.collect.TreeMultimap;

/**
 * @author Lorenz Buehmann
 *
 */
public class LogEntryGrouping {

	public static Multimap<String, LogEntry> groupByIPAddress(Collection<LogEntry> entries){
		Multimap<String, LogEntry> ip2Entries = TreeMultimap.create();
		for (LogEntry entry : entries) {
			ip2Entries.put(entry.getIp(), entry);
		}
		return ip2Entries;
	}
	
	public static Multimap<Date, LogEntry> groupByTime(Collection<LogEntry> entries){
		Multimap<Date, LogEntry> time2Entries = TreeMultimap.create();
		for (LogEntry entry : entries) {
			time2Entries.put(entry.getDate(), entry);
		}
		return time2Entries;
	}
	
	public static Multimap<String, LogEntry> groupByUserAgent(Collection<LogEntry> entries){
		Multimap<String, LogEntry> userAgent2Entries = TreeMultimap.create();
		for (LogEntry entry : entries) {
			userAgent2Entries.put(entry.getUserAgent(), entry);
		}
		return userAgent2Entries;
	}

}
