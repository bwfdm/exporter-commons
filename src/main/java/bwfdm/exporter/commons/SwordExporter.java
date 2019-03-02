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
package bwfdm.exporter.commons;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.AuthCredentials;
import org.swordapp.client.Content;
import org.swordapp.client.Deposit;
import org.swordapp.client.DepositReceipt;
import org.swordapp.client.EntryPart;
import org.swordapp.client.ProtocolViolationException;
import org.swordapp.client.SWORDClient;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.SWORDCollection;
import org.swordapp.client.SWORDError;
import org.swordapp.client.SWORDWorkspace;
import org.swordapp.client.ServiceDocument;
import org.swordapp.client.SwordResponse;
import org.swordapp.client.UriRegistry;

import bwfdm.exporter.commons.utils.IOUtils;


/**
 * General exporting methods for SWORD-based repositories (e.g. DSpace, Dataverse).
 *
 * @author Markus Gärtner
 * @author Volodymyr Kushnarenko
 * @author Florian Fritze
 *
 */
public abstract class SwordExporter {

	protected static final Logger log = LoggerFactory.getLogger(SwordExporter.class);

	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE_HEADER = "Content-Type";
	public static final String ACCEPT_HEADER = "Accept";
	public static final String MIME_FORMAT_ZIP = "application/zip";
	public static final String MIME_FORMAT_ATOM_XML = "application/atom+xml";

	public static enum SwordRequestType {
		DEPOSIT("DEPOSIT"), //"POST" request
		REPLACE("REPLACE"), //"PUT" request
		DELETE("DELETE")	//reserved for the future
		;

		private final String label;

		private SwordRequestType(String label) {
			this.label = label;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public String toString() {
			return label;
		}
	}


	/**
	 * Get a file extension (without a dot) from the file name
	 * (e.g. "txt", "zip", * ...)
	 *
	 * @param fileName {@link String} with the file name
	 * @return {@link String}
	 */
	public static String getFileExtension(String fileName) {
		String extension = "";
		int i = fileName.lastIndexOf('.');
		if(i>0) {
			extension = fileName.substring(i+1);
		}
		return extension;
	}


	/**
	 * Get package format basing on the file name.
	 * E.g. {@code UriRegistry.PACKAGE_SIMPLE_ZIP} or {@code UriRegistry.PACKAGE_BINARY}
	 *
	 * @param fileName {@link String} with the file name (not a full path)
	 * @return {@link String} with the package format
	 */
	public static String getPackageFormat(String fileName) {
		String extension = getFileExtension(fileName);

		if(extension.toLowerCase().equals("zip")) {
			return UriRegistry.PACKAGE_SIMPLE_ZIP;
		}
		return UriRegistry.PACKAGE_BINARY;
	}
	
	
	/**
	 * Get package format basing on the file name and "unpackZip" flag.
	 * E.g. {@code UriRegistry.PACKAGE_SIMPLE_ZIP} or {@code UriRegistry.PACKAGE_BINARY}
	 *
	 * @param fileName {@link String} with the file name (not a full path)
	 * @param unpackZip a flag, if the package should be unpacked ({@code true}) 
	 * 			in the repository or not ({@code false}) 
	 * @return {@link String} with the package format
	 */
	public static String getPackageFormat(String fileName, boolean unpackZip) {
		
		String packageFormat = getPackageFormat(fileName);
		if (packageFormat.equals(UriRegistry.PACKAGE_SIMPLE_ZIP) && !unpackZip) {
			return UriRegistry.PACKAGE_BINARY;
		}
		return packageFormat; 
	}
	
	
	/**
	 * Create new authentication credentials based on the user login and password.
	 * 
	 * @param userLogin login name of the user, usually is an E-mail address
	 * @param userPassword password for the user login
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(String userLogin, char[] userPassword) {

		requireNonNull(userLogin);
		requireNonNull(userPassword);
		return new AuthCredentials(userLogin, String.valueOf(userPassword)); // without "on-behalf-of" option
	}
	
	
	/**
	 * Create new authentication credentials with activated "on-behalf-of" option, what allows to make an export
	 * for some user (onBehalfOfUser) based only on its login name. For that case credentials of some privileged user
	 * are needed (login name, password), who could play e.g. an administrator role or just could be only allowed 
	 * to make an export into the repository and whose credentials are known. The privileged user in this case 
	 * will make an export on behalf of other user.
	 * <p>
	 * This type of authentication credentials could be used, if only administrator credentials are available 
	 * (adminUser, adminPassword) and from credentials of the current export's owner only login name is known 
	 * (onBehalfOfUser) and the password must not be used.      
	 * <p>
	 * <b>IMPORTNANT:</b> if "adminUser" and "onBehalfOfUser" are <b>identical</b>, authentication credentials 
	 * will be created <b>without the "on-behalf-of" option</b>.
	 * 
	 * @param adminUser login name of the privileged user, usually is an E-mail address
	 * @param adminPassword password of the privileged user
	 * @param onBehalfOfUser login name of the current owner of the export, usually is an E-mail address   
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(String adminUser, char[] adminPassword, String onBehalfOfUser) {

		requireNonNull(adminUser);
		requireNonNull(adminPassword);
		requireNonNull(onBehalfOfUser);
		
		if (adminUser.equals(onBehalfOfUser)) {
			return createAuthCredentials(onBehalfOfUser, adminPassword); // without "on-behalf-of" 
		} else {
			return new AuthCredentials(adminUser, String.valueOf(adminPassword), onBehalfOfUser); // with "on-behalf-of"
		}
	}
	
	
	/**
	 * Create new authentication credentials based on the API token. Could be used for the Dataverse repositories.
	 * 
	 * @param apiToken - API token, which could be usually found in the export repository GUI in the account settings. 
	 * 		Password in this case is not needed.
	 * 
	 * @return {@link AuthCredentials} object
	 */
	public static AuthCredentials createAuthCredentials(char[] apiToken) {

		requireNonNull(apiToken);
		return new AuthCredentials(String.valueOf(apiToken), ""); // use an empty string instead of password
	}


	private final AuthCredentials authCredentials;
	private final SWORDClient swordClient;


	/**
	 * Constructor, creates private final {@link SWORDClient} object and sets the authentication credentials (as private final object).
	 * To change the authentication credentials, please always create a new object.
	 *
	 * @param authCredentials {@link AuthCredentials} object. To create it please use the following methods: ...
	 *
	 */
	protected SwordExporter(AuthCredentials authCredentials) {
		swordClient = new SWORDClient();
		this.authCredentials = requireNonNull(authCredentials);
	}


	/**
	 * Get available collections via SWORD v2 protocol based on the {@link ServiceDocument}.
	 *
	 * @param serviceDocument can be created via {@link #getServiceDocument(String) getServiceDocument(serviceDocumentURL)}
	 * 			<p>
	 * 			IMPORTANT: serviceDocument must be NON-{@code null}!
	 * @return {@code Map<String, String>} where key = collection URL, value = collection title
	 */
	public Map<String, String> getCollections(ServiceDocument serviceDocument){
		requireNonNull(serviceDocument);
		Map<String, String> collectionsMap = new HashMap<String, String>();
		HierarchyObject hierarchy = this.getHierarchy(serviceDocument); //get complete hierarchy of collections and services
		if(hierarchy != null) {
			collectionsMap.putAll(hierarchy.getCollections());
		}
		return collectionsMap;
	}

	
	/**
	 * Get available collections including possible hierarchical structures via SWORD v2 protocol 
	 * based on the {@link ServiceDocument}. Separator will be used to show different structure parts. 
	 * <p>
	 * E.g. for DSpace it means: collection with related communities 
	 * (e.g. "community/subcommunity/subcommunity/collection", where separator is "/")  
	 * <p>
	 * <b>IMPORTANT:</b> please check before, if service document supports the "service" tag 
	 * inside the "collection" tags - please use {@link #isServiceDocumentWithSubservices(ServiceDocument)} for that. 
	 * If "service" tag is not supported, the return value of this method should be the same
	 * as for {@link #getCollections(ServiceDocument)} method.
	 * 
	 * @param serviceDocument service document (request for it must be done before)
	 * @param hierarchySeparator {@link String} separator between hierarchical elements  
	 * @return {@code Map<String, String>} where key = collection URL, value = collection with complete hierarchy
	 */
	public Map<String, String> getCollectionsAsHierarchy(ServiceDocument serviceDocument, String hierarchySeparator){
		requireNonNull(serviceDocument);
		requireNonNull(hierarchySeparator);
		
		Map<String, String> collectionsHierarchyMap = new HashMap<String, String>();
		Map<String, String> collectionsMap = new HashMap<String, String>();
		
		// Get complete hierarchy for collections and services
		HierarchyObject hierachy = getHierarchy(serviceDocument);
		if(hierachy != null) {
			collectionsMap.putAll(hierachy.getCollections()); //get all collections (without hierarchy)
		}
		
		// Create complete hierarchy
		for(String collectionUrl : collectionsMap.keySet()) {
			String serviceHierarchyString = "";
			List<String> collectionServices = hierachy.getServiceHierarchyForCollection(collectionUrl); //get hierarchy only for one collection 
			if(collectionServices != null) {
				for (String serviceTitle : collectionServices) {
					serviceHierarchyString += serviceTitle + hierarchySeparator; //String with complete hierarchy 
				}				
			}
			collectionsHierarchyMap.put(collectionUrl, serviceHierarchyString + collectionsMap.get(collectionUrl));
		}
		
		return collectionsHierarchyMap;
	}
	
	/**
	 * Get complete hierarchy of collections and all related services as a {@link HierarchyObject}.
	 * <p>
	 * Service document can include pure collections, and also collections with the {@code <service>..</service>} 
	 * tag inside - e.g. for DSpace "service" tag means, that there is a community, not a collection.
	 * <p>
	 * This method represents this hierarchy of collections and related services. 
	 * The idea is similar to the DSpace v6 "hierarchy" REST-API request, 
	 * but suitable for the service document (SWORDv2 protocol):
	 * <a href="https://wiki.duraspace.org/display/DSDOC6x/REST+API#RESTAPI-Hierarchy">https://wiki.duraspace.org/display/DSDOC6x/REST+API#RESTAPI-Hierarchy</a>   
	 * <p>
	 * <b>INFO:</b> Created {@link HierarchyObject} could be stored and used later 
	 * to get all collections and services if needed (to avoid new requests to the server)
	 * 
	 * @param serviceDocument service document
	 * @return {@link HierarchyObject} object with the complete repository hierarchy
	 */
	public HierarchyObject getHierarchy(ServiceDocument serviceDocument) {
		
		requireNonNull(serviceDocument);
		HierarchyObject hierarchy = new HierarchyObject("", ""); // empty fields explicit for the main workspace
		
		for (SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {
								
				// Check, if it is a pure collection (does not have <service> tag)
				if(collection.getSubServices().isEmpty()) { // is collection
					// key = full URL, value = Title
					hierarchy.collections.add(new HierarchyCollectionObject(collection.getTitle(), collection.getHref().toString()));
				} else { 
					// It is a service. Get all collections inside the service (e.g. collections inside the community for DSpace)
					HierarchyObject newServiceObject = createHierarchyObject(collection.getTitle(), collection.getSubServices().get(0).toString());					
					hierarchy.services.add(newServiceObject);
				}			
			}
		}
		
		return hierarchy;
	}
		
	
	/**
	 * Create a new {@link HierarchyObject} based on the title and URL.
	 * Sometimes service document includes "service" tag, what means, that some extra structure is available 
	 * (not a standard "collection"). This method investigates this "service" structure 
	 * Internal private method, is used iteratively.
	 * <p>
	 * The method makes a http-request to the URL and analyses the respond to create a new {@link HierarchyObject}.
	 * The responds looks similar to the service document, 
	 * but could not be analyzed with standards methods for the {@link ServiceDocument}.
	 *    
	 * @param title title of the {@link HierarchyObject}, should be taken from the "service" tag
	 * @param url {@link String} with the URL of the hierarchy object (from the "collection" tag)
	 *   
	 * @return {@link HierarchyObject} object with the hierarchy for the concrete element ("service") 
	 * 			or {@code null} in case of error
	 */
	private HierarchyObject createHierarchyObject(String title, String url) {
		requireNonNull(url);
		HierarchyObject newHierarchyObject = new HierarchyObject(title, url);		
		
		try {
			// Get request on collectionUrl, same as via "curl" 
			// e.g.: curl -i $url --user "$USER_MAIL:$USER_PASSWORD"
			Content content = this.getSwordClient().getContent(newHierarchyObject.serviceUrl, SwordExporter.MIME_FORMAT_ATOM_XML, 
					UriRegistry.PACKAGE_SIMPLE_ZIP, this.getAuthCredentials());
			
			try {
				String response = IOUtils.readStream(content.getInputStream());
				
				Pattern collectionPattern = Pattern.compile("<collection (.+?)</collection>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<collection href="http://hdl.handle.net/123456789/1">...many-tags...</collection>"
				Matcher collectionMatcher = collectionPattern.matcher(response);
				
				// Find all collections (with and without "service" tag)
				while(collectionMatcher.find()) {
					String collectionString = collectionMatcher.group(1);
					
					// Check, if is it a service, not a collection 
					Pattern servicePattern = Pattern.compile("<service.+?>(.+?)</service>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<service xmlns="http://purl.org/net/sword/terms/">https://some_link</service>"
					Matcher servciceMatcher = servicePattern.matcher(collectionString);
					if(servciceMatcher.find()) {
						// Find service url
						String serviceUrl = servciceMatcher.group(1);
												
						// Find service title
						String serviceTitle = "";
						Pattern serviceTitlePattern = Pattern.compile("<.+?title.+?>(.+?)</.+?title>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<atom:title type="text">service-title</atom:title>"  
						Matcher serviceTitleMatcher = serviceTitlePattern.matcher(collectionString);
						if(serviceTitleMatcher.find()) {
							serviceTitle = serviceTitleMatcher.group(1);
						}	
						
						// Create new hierarchy object for this service
						HierarchyObject obj = createHierarchyObject(serviceTitle, serviceUrl);
						if(obj != null) {
							newHierarchyObject.services.add(obj);
						} else {}
						
					} else {
						// Normal collection, without "service" tag inside		
						Pattern hrefPattern = Pattern.compile("href=\"(.+?)\">", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<id>https://some_link</id>"
						Matcher hrefMatcher = hrefPattern.matcher(collectionString);
						
						Pattern titlePattern = Pattern.compile("<.+?title.+?>(.+?)</.+?title>", Pattern.DOTALL | Pattern.MULTILINE); //e.g. "<title type="text">some_title</title>" 
						Matcher titleMatcher = titlePattern.matcher(collectionString);
						
						// Find href and title, add collection to the new hierarchy object
						if(hrefMatcher.find() && titleMatcher.find()) { 
							newHierarchyObject.collections.add(new HierarchyCollectionObject(titleMatcher.group(1), hrefMatcher.group(1)));
						}
					}
				}
			} catch (IOException e) {
				log.error("Exception by converting Bitstream to String: {}: {}", e.getClass().getSimpleName(), e.getMessage());
				return null;
			}	
		} catch (SWORDClientException | ProtocolViolationException | SWORDError e) {
			log.error("Exception by getting content (request) via SWORD: {}: {}", e.getClass().getSimpleName(), e.getMessage());
			return null;
		}
		
		return newHierarchyObject;
	}
	
	
	/**
	 * Check if service document represents services and not the traditional collections.
	 * <p>
	 * Some repositories can provide a service document, which has a "service" tag (subservice) 
	 * inside the "collection" tag. It means, that to get the collection items, extra request to the service URL 
	 * will be needed.
	 * <p>
	 * E.g. in case of DSpace it means, that service document lists all top level communities of the repository, 
	 * and not collections as usually. To get the collections inside the community, extra request to the service URL
	 * will be needed.
	 * <p>
	 * <b>IMPORTANT:</b> if service document includes subservices, then to represent a full hierarchy of collections 
	 * including related services the method {@link #getCollectionsAsHierarchy(ServiceDocument, String)} could be used. 
	 *   
	 * @param serviceDocument service document
	 * 
	 * @return {@code true} if service document includes subservices ("service" tag inside the "collection" tag) 
	 *  		and {@code false} otherwise (service document includes only traditional collections)
	 */
	public boolean isServiceDocumentWithSubservices(ServiceDocument serviceDocument) {
		for (SWORDWorkspace workspace : serviceDocument.getWorkspaces()) {
			for (SWORDCollection collection : workspace.getCollections()) {							
				// Check, if collection has a <service> tag inside (i.e. is a community in case of DSpace)
				if(!collection.getSubServices().isEmpty()) {
					return true;
				}			
			}
		}		
		return false;
	}
	
	
	/**
	 * Get available entries of the provided collection based on the the current authentication credentials.
	 * E.g. for DSpace repository it means - items inside the collection. 
	 * For Dataverse repository - datasets inside the dataverse.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 * 
	 * @param collectionUrl a collection URL, must have a "/swordv2/collection/" substring inside
	 * 
	 * @return {@link Map} of entries, where key = entry URL (with "/swordv2/edit/" substring inside), 
	 * 					value = entry title. If there are not available entries, the map will be also empty.
	 * 					Returns {@code null} in case of error.
	 */
	public abstract Map<String, String> getCollectionEntries(String collectionUrl);


	public AuthCredentials getAuthCredentials() {
		return authCredentials;
	}


	protected SWORDClient getSwordClient() {
		return swordClient;
	}


	/**
	 * Request a service document based on the URL.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param serviceDocumentURL string with the service document URL
	 * @return {@link ServiceDocument} object or {@code null} if service document is not accessible via provided URL 
	 * 			and implicitly used authentication credentials or in case of error.
	 */
	public ServiceDocument getServiceDocument(String serviceDocumentURL) {
		ServiceDocument serviceDocument = null;
		try {
			serviceDocument = swordClient.getServiceDocument(serviceDocumentURL, authCredentials);
		} catch (SWORDClientException | ProtocolViolationException e) {
			log.error("Exception by accessing service document", e);
			return null;
		}
		return serviceDocument;
	}

	/**
	 * Check if SWORDv2 API is accessible with current authentication credentials.
	 * <p>
	 * IMPORTANT: credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *  
	 * @param serviceDocumentURL string with the service document URL
	 * @return {@code true} if SWORD API is accessible and {@code false} otherwise
	 */
	public boolean isSwordAccessible(String serviceDocumentURL) {
		return getServiceDocument(serviceDocumentURL) != null;
	}

	/**
	 * Export an element via SWORD - any file (also including metadata as a xml-file) or metadata as a {@link Map}.
	 * Private internal method, should be used ONLY for the internal implementation.
	 * <p>
	 * IMPORTANT: is possible to export ONLY 1 option in the same time (only file, or only a Map of metadata).
	 * "Multipart" is not supported!
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param exportURL could be link to the collection (from the service document)
	 * 		  or a link to edit the collection ("Location" field in the response)
	 * @param swordRequestType see {@link SwordRequestType}
	 * @param mimeFormat String with e.g. {@code "application/atom+xml"} or {@code "application/zip"}, see {@link SwordRequestType}}
	 * @param packageFormat {@code String} with the package format, see {@link UriRegistry#PACKAGE_SIMPLE_ZIP} or {@link UriRegistry#PACKAGE_BINARY}
	 * @param file {@link File} for export
	 * @param metadataMap {@link Map} of metadata for export
	 * @param inProgress {@code boolean} value for the "In-Progress" header 
	 * 		  <p>
	 * 	      For DSpace "In-Progress: true" means, that export will be done at first to the user's workspace, 
	 *        where further editing of the exported element is possible. And "In-Progress: false" means export directly 
	 *        to the workflow, without a possibility of further editing.
	 *        <p>
	 *        For Dataverse for some requests only "In-Progress: false" is recommended, 
	 *        see <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 *
	 * @return {@link SwordResponse} object or {@code null} in case of error.
	 * 		   <pre>
	 * 		   If request type is {@code SwordRequestType.DEPOSIT}, please cast the returned object to {@code DepositReceipt},
	 * 		   you can check it via e.g. {@code instanceof} operator.
	 *  	   If request type is {@code SwordRequestType.REPLACE}, the casting is not needed.
	 *  	
	 *  	   <b>IMPORTANT:</b> by request type {@code SwordRequestType.REPLACE} there is no warranty,
	 *  	   that all fields of the {@link SwordResponse} object are initialized, so for the current moment
	 *  	   only status code field is available, all other fields are {@code null} (e.g. "Location" field).
	 *  	   In case of {@code SwordRequestType.DEPOSIT} request type such problems were not found.
	 *  	   </pre>
	 * @throws ProtocolViolationException in case of SWORD error
	 * @throws SWORDError in case of SWORD error 
	 * @throws SWORDClientException in case of SWORD error
	 * @throws FileNotFoundException in case of wrong file name
	 *
	 */
	protected SwordResponse exportElement(String exportURL, SwordRequestType swordRequestType,
			String mimeFormat, String packageFormat, File file, Map<String, List<String>> metadataMap, boolean inProgress)
					throws SWORDClientException, SWORDError, ProtocolViolationException, FileNotFoundException {

		requireNonNull(exportURL);
		requireNonNull(swordRequestType);
		requireNonNull(inProgress);

		// Check if only 1 parameter is used (metadata OR file).
		// Multipart is not supported.
		if( ((file != null)&&(metadataMap != null)) || ((file == null)&&(metadataMap == null)) ) {
			return null;
		}

		FileInputStream fis = null;

		Deposit deposit = new Deposit();

		// Check if "metadata as a Map"
		if(metadataMap != null) {
			EntryPart ep = new EntryPart();
			for(Map.Entry<String, List<String>> metadataEntry : metadataMap.entrySet()) {
				for (String property: metadataEntry.getValue()) {
					ep.addDublinCore(metadataEntry.getKey(), property);
				}
			}
			deposit.setEntryPart(ep);
		}

		// Check if "file"
		if(file != null) {
			fis = new FileInputStream(file); // open FileInputStream
			deposit.setFile(fis);
			deposit.setFilename(file.getName()); 	// deposit works properly ONLY with a "filename" parameter
													// --> in curl: -H "Content-Disposition: filename=file.zip"
		}

		deposit.setMimeType(mimeFormat);
		deposit.setPackaging(packageFormat);
		deposit.setInProgress(inProgress);

		try {
			switch (swordRequestType) {
			case DEPOSIT:
				DepositReceipt receipt = swordClient.deposit(exportURL, deposit, authCredentials);
				return receipt; // returns Deposit Receipt instance;
			case REPLACE:
				if (deposit.getEntryPart() != null) {
					// Use "replace" method for EntryPart (metadata as Map)
					SwordResponse response = swordClient.replace(exportURL, deposit, authCredentials);
					return response;
				} else {
					// Use "replace" method for Media (metadata as XML-file)

					// TODO: create issue for SWORD-Client to consider the header "In-Progress:
					// true" for "replaceMedia()" method
					// -> https://github.com/swordapp/JavaClient2.0/issues
					//
					// Code area, file "org.swordapp.client.SWORDClient.java", lines 464-468:
					//
					// // add the headers specific to a binary only deposit
					// http.addContentDisposition(options, deposit.getFilename());
					// http.addContentMd5(options, deposit.getMd5());
					// http.addPackaging(options, deposit.getPackaging());
					// http.addMetadataRelevant(options, deposit.isMetadataRelevant());
					//
					// Add new line:
					// http.addInProgress(options, deposit.isInProgress());
					//
					SwordResponse response = swordClient.replaceMedia(exportURL, deposit, authCredentials);
					return response;
				}				
			default:
				log.error("Wrong SWORD-request type: {} : Supported here types are: {}, {}",
						swordRequestType, SwordRequestType.DEPOSIT, SwordRequestType.REPLACE);
				throw new IllegalArgumentException("Wrong SWORD-request type: "+swordRequestType);
			}
		} finally {
			if(fis!=null) {
				IOUtils.closeQuietly(fis);
			}
		}

	}

	
	/**
	 * Export the metadata only (without any file) to some collection.
	 * Metadata are described as a {@link java.util.Map}.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL holds the collection URL where the metadata will be exported to
	 * @param metadataMap holds the metadata itself
	 * @param inProgress {@code boolean} value for the "In-Progress" header 
	 * 		  <p>
	 * 	      For DSpace "In-Progress: true" means, that export will be done at first to the user's workspace, 
	 *        where further editing of the exported element is possible. And "In-Progress: false" means export directly 
	 *        to the workflow, without a possibility of further editing.
	 *        <p>
	 *        For Dataverse for some requests only "In-Progress: false" is recommended, 
	 *        see <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 * 
	 * @return {@link String} with the entry URL which includes "/swordv2/edit/" substring inside. 
	 * 		This URL could be used without changes for further update of the metadata 
	 * 		(see {@link #replaceMetadataEntry(String, Map, boolean) replaceMetadataEntry(entryURL, metadataMap, inProgress)}) 
	 * 		<p>
	 * 		<b>IMPORTANT for Dataverse repository:</b> for further update/extension of the media part 
	 * 		(e.g. uploaded files inside the dataset) please replace "/swordv2/edit/" substring inside the entry URL to 
	 * 		"/swordv2/edit-media/". 
	 * 		For more details please visit <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 * 		<p>
	 * 		<b>IMPORTANT for DSpace repository:</b> further update/extension of the media part (e.g. uploaded files)
	 * 		via SWORD is not supported, only update of the metadata is allowed.
	 * 
	 * @throws SWORDClientException in case of SWORD error
	 */
	public abstract String createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap, boolean inProgress) throws SWORDClientException;

	
	/**
	 * Export a file together with the metadata to some collection.
	 * Metadata are described as a {@link java.util.Map}.
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param collectionURL holds the collection URL where items will be exported to
	 * @param unpackZip decides whether to unpack the zipfile or places the packed zip file as uploaded data
	 * @param file holds a file which can contain one or multiple files
	 * @param metadataMap holds the metadata which is necessary for the ingest
	 * @param inProgress {@code boolean} value for the "In-Progress" header 
	 * 		  <p>
	 * 	      For DSpace "In-Progress: true" means, that export will be done at first to the user's workspace, 
	 *        where further editing of the exported element is possible. And "In-Progress: false" means export directly 
	 *        to the workflow, without a possibility of further editing.
	 *        <p>
	 *        For Dataverse for some requests only "In-Progress: false" is recommended, 
	 *        see <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 *
	 * @return {@link String} with the entry URL which includes "/swordv2/edit/" substring inside. 
	 * 		This URL could be used without changes for further update of the metadata 
	 * 		(see {@link #replaceMetadataEntry(String, Map, boolean) replaceMetadataEntry(entryURL, metadataMap, inProgress)}) 
	 * 		<p>
	 * 		<b>IMPORTANT for Dataverse repository:</b> for further update/extension of the media part 
	 * 		(e.g. uploaded files inside the dataset) please replace "/swordv2/edit/" substring inside the entry URL to 
	 * 		"/swordv2/edit-media/". 
	 * 		For more details please visit <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 * 		<p>
	 * 		<b>IMPORTANT for DSpace repository:</b> further update/extension of the media part (e.g. uploaded files)
	 * 		via SWORD is not supported, only update of the metadata is allowed.   
	 *
	 * @throws SWORDClientException in case of SWORD error
	 * @throws IOException in case of IO error
	 */
	public abstract String createEntryWithMetadataAndFile(String collectionURL, File file, boolean unpackZip, Map<String, List<String>> metadataMap, boolean inProgress) throws IOException, SWORDClientException;


	/**
	 * Export a file to some URL (e.g. URL of some collection or metadata set).
	 * <p>
	 * IMPORTANT: authentication credentials are used implicitly. Definition of the credentials is realized via the class constructor.
	 *
	 * @param url The URL where to export the zipFile to.
	 * @param file A file that should be exported.
	 * @param inProgress {@code boolean} value for the "In-Progress" header 
	 * 		  <p>
	 * 	      For DSpace "In-Progress: true" means, that export will be done at first to the user's workspace, 
	 *        where further editing of the exported element is possible. And "In-Progress: false" means export directly 
	 *        to the workflow, without a possibility of further editing.
	 *        <p>
	 *        For Dataverse for some requests only "In-Progress: false" is recommended, 
	 *        see <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 *
	 * TODO: uncomment later. Think about - return location link as String (with "edit" substring inside)
	 *
	 * @throws IOException in case of IO error
	 * @throws SWORDClientException in case of SWORD error
	 */
	//public abstract void exportFile(String url, File file, boolean inProgress) throws IOException, SWORDClientException;
	
	
	/**
	 * Replaces an existing metadata entry with new metadata in the repository
	 * 
	 * @param entryUrl The URL which points to the metadata entry, includes "/swordv2/edit/" substring inside.
	 * @param metadataMap The metadata that will replace the old metadata.
	 * @param inProgress {@code boolean} value for the "In-Progress" header 
	 * 		  <p>
	 * 	      For DSpace "In-Progress: true" means, that export will be done at first to the user's workspace, 
	 *        where further editing of the exported element is possible. And "In-Progress: false" means export directly 
	 *        to the workflow, without a possibility of further editing.
	 *        <p>
	 *        For Dataverse for some requests only "In-Progress: false" is recommended, 
	 *        see <a href="http://guides.dataverse.org/en/latest/api/sword.html">http://guides.dataverse.org/en/latest/api/sword.html</a>
	 * 
	 * @throws SWORDClientException in case of SWORD error
	 */
	public void replaceMetadataEntry(String entryUrl, Map<String, List<String>> metadataMap, boolean inProgress) throws SWORDClientException {
		try {
			exportElement(entryUrl, SwordRequestType.REPLACE, MIME_FORMAT_ATOM_XML, null, null, metadataMap, inProgress);	
		} catch (FileNotFoundException | ProtocolViolationException | SWORDError e) {
			throw new SWORDClientException("Exception by replacing of metadata via metadata Map: " 
					+ e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
	
	
	/**
	 * Class to represent a hierarchy for collections and services.
	 * The idea is taken from the DSpace v6, see "hierarchy" REST-API request:
	 * <a href="https://wiki.duraspace.org/display/DSDOC6x/REST+API#RESTAPI-Hierarchy">https://wiki.duraspace.org/display/DSDOC6x/REST+API#RESTAPI-Hierarchy</a>
	 * 
	 * @author Volodymyr Kushnarenko
	 */
	public class HierarchyObject {

		public String serviceTitle;
		public String serviceUrl;
		public List<HierarchyObject> services;
		public List<HierarchyCollectionObject> collections;
		
		public HierarchyObject() {
			serviceTitle = "";
			serviceUrl = "";
			collections = new ArrayList<HierarchyCollectionObject>();
			services = new ArrayList<HierarchyObject>();
		}
		
		public HierarchyObject(String serviceTitle, String serviceUrl) {
			this();
			this.serviceTitle = serviceTitle;
			this.serviceUrl = serviceUrl;
		}
		
		/**
		 * Get all "services" for the current collection
		 * 
		 * @param collectionUrl {@link String} with the collection URL
		 * 
		 * @return {@code List<String>} with all related services.
		 * <p>
		 * <b>IMPORTANT:</b> the first service (the main "workspace" from the service document) 
		 * will be implicitly removed from the list.   
		 */
		public List<String> getServiceHierarchyForCollection(String collectionUrl){		
			List<String> serviceList = new ArrayList<String>(0);
			serviceList = getServiceHierarchyForCollection(this, collectionUrl, serviceList);
			if(serviceList != null) {
				serviceList.remove(0); // remove "workspace", it is not a service what we need
			}
			return serviceList;
		}
				
		/**
		 * Get a hierarchy of the "services" for the current collection.
		 * The method will be used recursively.
		 *   
		 * @param currentHierarchyObject currently used hierarchy object
		 * @param collectionUrl {@link String} with the collection URL
		 * @param serviceList {@code List<String>} with the collected "services" (titles of the {@link HierarchyObject})
		 * 				which are collected during the recursive iteration
		 * @return {@code List<String>} with the collected service titles for the current collection
		 */
		private List<String> getServiceHierarchyForCollection(HierarchyObject currentHierarchyObject, String collectionUrl, List<String> serviceList) {
			
			for(HierarchyCollectionObject collectionObject: currentHierarchyObject.collections) {
				if(collectionObject.collectionHref.equals(collectionUrl)) {
					serviceList.add(0, currentHierarchyObject.serviceTitle);
					return serviceList;
				}
			}
			
			for(HierarchyObject serviceObject: currentHierarchyObject.services) {
				List<String> list = getServiceHierarchyForCollection(serviceObject, collectionUrl, serviceList);
				if(list != null) {
					list.add(0, currentHierarchyObject.serviceTitle);
					return list;
				}				
			}
			
			return null;
		}
				
		/**
		 * Get all collection titles (without hierarchy) of the hierarchy object.
		 * If hierarchy object represents a complete repository, 
		 * the method will return all stored collections (only titles) of the repository.
		 * 
		 * @return {@code Map<String, String>} where key = collection URL, value = collection title (without hierarchy)
		 */
		public Map<String, String> getCollections(){
			Map<String, String> collectionsMap = new HashMap<String, String>();
			
			for(HierarchyCollectionObject collectionObject: collections) {
				collectionsMap.put(collectionObject.collectionHref, collectionObject.collectionTitle);
			}			
			for(HierarchyObject serviceObject: services) {
				collectionsMap.putAll(serviceObject.getCollections());
			}	
			
			return collectionsMap;
		}
	}
	
	
	/**
	 * Class to represent a collection inside the {@link HierarchyObject}
	 * 
	 * @author Volodymyr Kushnarenko
	 */
	public class HierarchyCollectionObject {

		public String collectionTitle;
		public String collectionHref;
		
		public HierarchyCollectionObject() {
			this.collectionTitle = "";
			this.collectionHref = "";
		}
		
		public HierarchyCollectionObject(String collectionTitle, String collectionHref) {
			this.collectionTitle = collectionTitle;
			this.collectionHref = collectionHref;
		}
	}
		
}
