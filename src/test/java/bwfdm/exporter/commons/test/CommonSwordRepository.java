/*
 * Unless expressly otherwise stated, code from this project is licensed under the MIT license [https://opensource.org/licenses/MIT].
 * 
 * Copyright (c) <2018> <Volodymyr Kushnarenko, Florian Fritze, Markus Gärtner, Stefan Kombrink, Matthias Fratz, Daniel Scharon, Sibylle Hermann, Franziska Rapp and Uli Hahn>
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A 
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF 
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH 
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package bwfdm.exporter.commons.test;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.SWORDClientException;

import bwfdm.exporter.commons.SwordExporter;

public class CommonSwordRepository extends SwordExporter{
	
	protected static final Logger log = LoggerFactory.getLogger(CommonSwordRepository.class);
	
	protected String serviceDocumentURL;
	
	/**
	 * Constructor with supported "on-behalf-of" option
	 * 
	 * @param serviceDocumentURL {@link String} with the service document URL
	 * @param adminUser {@link String} with the admin user name
	 * @param standardUser {@link String} with the user name (the user, in whom name export will be done)
	 * @param adminPassword char array with the admin password
	 */
	public CommonSwordRepository(String serviceDocumentURL, String adminUser, String standardUser, char[] adminPassword) {

		super(SwordExporter.createAuthCredentials(adminUser, adminPassword, standardUser));
		
		requireNonNull(serviceDocumentURL);
		requireNonNull(adminUser);
		requireNonNull(standardUser);
		requireNonNull(adminPassword);
		
		this.serviceDocumentURL = serviceDocumentURL;
	}
	
	/**
	 * Constructor for some common export
	 * 
	 * @param serviceDocumentURL {@link String} with the service document URL
	 * @param userName {@link String} with the user name 
	 * @param userPassword char array with the user password
	 */
	public CommonSwordRepository(String serviceDocumentURL, String userName, char[] userPassword) {
	
		super(SwordExporter.createAuthCredentials(userName, userPassword));
		
		requireNonNull(serviceDocumentURL);
		requireNonNull(userName);
		requireNonNull(userPassword);
		
		this.serviceDocumentURL = serviceDocumentURL;
	}
	
	/**
	 * Constructor with supported API-Token as a credentials.
	 * @param serviceDocumentURL {@link String} with the service document URL
	 * @param apiToken char array with the API-Token
	 */
	public CommonSwordRepository(String serviceDocumentURL, char[] apiToken) {
		
		super(SwordExporter.createAuthCredentials(apiToken));
		
		requireNonNull(serviceDocumentURL);
		requireNonNull(apiToken);
		
		this.serviceDocumentURL = serviceDocumentURL;
	}
	
	@Override
	public Map<String, String> getCollectionEntries(String collectionUrl) {
		throw new UnsupportedOperationException("CommonSwordRepository: not implemented method.");
	}

	@Override
	public String createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap, boolean inProgress)
			throws SWORDClientException {
		throw new UnsupportedOperationException("CommonSwordRepository: not implemented method.");
	}

	@Override
	public String createEntryWithMetadataAndFile(String collectionURL, Map<String, List<String>> metadataMap, 
			File file, boolean unpackZip, boolean inProgress) throws IOException, SWORDClientException {
		throw new UnsupportedOperationException("CommonSwordRepository: not implemented method.");
	}
	
}
