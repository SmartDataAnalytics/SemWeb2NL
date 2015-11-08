/*
 * #%L
 * OWL2NL
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
package org.aksw.owl2nl.evaluation;

import java.util.List;

/**
 * A generator for HTML tables.
 * @author Lorenz Buehmann
 *
 */
public class HTMLTableGenerator {
	
	static final String style =
			"<head>\n" + 
			"<style>\n" + 
			"table.sortable thead {\n" + 
			"    background-color:#eee;\n" + 
			"    color:#666666;\n" + 
			"    font-weight: bold;\n" + 
			"    cursor: pointer;\n" + 
			"}\n" + 
			"table.sortable tbody tr:nth-child(2n) td {\n" + 
			"    color: #000000;\n" + 
			"    background-color: #EAF2D3;\n" + 
			"}\n" + 
			"table.sortable tbody tr:nth-child(2n+1) td {\n" + 
			"  background: #ccfffff;\n" + 
			"}" + 
			"#benchmark {\n" + 
			"    font-family: \"Trebuchet MS\", Arial, Helvetica, sans-serif;\n" + 
			"    width: 100%;\n" + 
			"    border-collapse: collapse;\n" + 
			"}\n" + 
			"\n" + 
			"#benchmark td, #benchmark th {\n" + 
			"    font-size: 1em;\n" + 
			"    border: 1px solid #98bf21;\n" + 
			"    padding: 3px 7px 2px 7px;\n" + 
			"}\n" + 
			"\n" + 
			"#benchmark th {\n" + 
			"    font-size: 1.1em;\n" + 
			"    text-align: left;\n" + 
			"    padding-top: 5px;\n" + 
			"    padding-bottom: 4px;\n" + 
			"    background-color: #A7C942;\n" + 
			"    color: #ffffff;\n" + 
			"}\n"
			+ "#benchmark td.number {\n" + 
			"  text-align: right;\n" + 
			"}\n" + 
			"</style>\n"
			+ "<script type='text/javascript' src='http://www.kryogenix.org/code/browser/sorttable/sorttable.js'></script>" + 
			"</head>\n" + 
			"<body>";
	
	static final String style2 =
			"<head>\n" + 
			"<link rel=\"stylesheet\" href=\"https://rawgit.com/twbs/bootstrap/master/dist/css/bootstrap.min.css\">\n" + 
			"<link rel=\"stylesheet\" href=\"https://rawgit.com/wenzhixin/bootstrap-table/1.8.0/dist/bootstrap-table.css\">\n" +
			"<style type=\"text/css\">\n" + 
			"   pre {\n" + 
			"	border: 0; \n" + 
			"	background-color: transparent\n"
			+ "font-family: monospace;" + 
			"	}\n"
			+ "table {\n" + 
			"    border-collapse: separate;\n" + 
			"    border-spacing: 0 5px;\n" + 
			"}\n" + 
			"\n" + 
			"thead th {\n" + 
			"    background-color: #006DCC;\n" + 
			"    color: white;\n" + 
			"}\n" + 
			"\n" + 
			"tbody td {\n" + 
			"    background-color: #EEEEEE;\n" + 
			"}\n" + 
			"\n" + 
			"tr td:first-child,\n" + 
			"tr th:first-child {\n" + 
			"    border-top-left-radius: 6px;\n" + 
			"    border-bottom-left-radius: 6px;\n" + 
			"}\n" + 
			"\n" + 
			"tr td:last-child,\n" + 
			"tr th:last-child {\n" + 
			"    border-top-right-radius: 6px;\n" + 
			"    border-bottom-right-radius: 6px;\n" + 
			"}\n" + 
			".fixed-table-container tbody td {\n" + 
			"    border: none;\n" + 
			"}\n" + 
			".fixed-table-container thead th {\n" + 
			"    border: none;\n" + 
			"}\n" + 
			"\n" + 
			".bootstrap-table .table {\n" + 
			"	border-collapse: inherit !important;\n" + 
			"}" + 
			"</style>\n" +
			"<script src=\"http://code.jquery.com/jquery-1.11.3.min.js\"></script>\n" + 
			"<script src=\"https://rawgit.com/twbs/bootstrap/master/dist/js/bootstrap.min.js\"></script>\n" + 
			"<script src=\"https://rawgit.com/wenzhixin/bootstrap-table/1.8.0/dist/bootstrap-table-all.min.js\"></script>\n" +
			"</head>\n";


	/**
	 * Generates an HTML table based on the given column names and data.
	 * @param columnNames the column names
	 * @param data the data as a list of row entry values
	 * @return an HTML table
	 */
	public static String generateHTMLTable(List<String> columnNames, List<List<String>> data) {
		String html = "<html>\n";
		html += style2;
		html += "<body>\n";
		html += "<table data-toggle=\"table\" data-striped='true'>\n";

		// table header
		html += "<thead><tr>";
		for (String columnName : columnNames) {
			html += "<th data-align=\"left\" data-sortable=\"true\" data-valign='middle'>" + columnName + "</th>";
		}

		// the data
		html += "<tbody>\n";
		for (List<String> row : data) {
			html += "<tr>\n";
			for (String entry : row) {
				html += "<td>" + entry + "</td>\n";
				//class='number'
			}
			html += "</tr>\n";
		}
		html += "</tbody>\n";

		html += "</table>\n";
		html += "</body>\n";
		html += "</html>\n";
		
		return html;
	}
}
