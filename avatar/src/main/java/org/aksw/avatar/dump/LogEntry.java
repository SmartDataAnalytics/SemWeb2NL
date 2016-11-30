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
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.aksw.avatar.dump;

import java.util.Date;

import org.apache.commons.lang3.builder.CompareToBuilder;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;


/**
 *
 * @author ngonga
 */
public class LogEntry implements Comparable<LogEntry>{
    String query;
    Date date;
    String ip;
    Query sparqlQuery;
    String userAgent;
    
    

	public LogEntry(String q)
    {
        query = q;
        this.sparqlQuery = QueryFactory.create(q);
    }
    
    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
        this.sparqlQuery = QueryFactory.create(query);
    }

    public Query getSparqlQuery() {
        return sparqlQuery;
    }

    public void setSparqlQuery(Query sparqlQuery) {
        this.sparqlQuery = sparqlQuery;
    }
    
	public String getUserAgent() {
		return userAgent;
	}

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(LogEntry other) {
		return new CompareToBuilder()
		.append(ip, other.ip)
		.append(query, other.query)
		.append(date, other.date)
		.toComparison();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((ip == null) ? 0 : ip.hashCode());
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LogEntry other = (LogEntry) obj;
		if (ip == null) {
			if (other.ip != null)
				return false;
		} else if (!ip.equals(other.ip))
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		return true;
	}
    
   
}
