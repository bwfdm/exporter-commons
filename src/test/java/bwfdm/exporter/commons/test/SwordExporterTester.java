package bwfdm.exporter.commons.test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.swordapp.client.SWORDClientException;
import org.swordapp.client.ServiceDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import bwfdm.exporter.commons.SwordExporter;

public class SwordExporterTester {

	protected static final Logger log = LoggerFactory.getLogger(SwordExporterTester.class);
	
	public static void main(String[] args) {
		
		Scanner scanner = new Scanner(System.in); //for password input (ATTENTION: typo is NOT hidden!)
		
		try {
			
			File fXML = new File(SwordExporterTester.class.getClassLoader().getResource("repositories.xml").getFile());
			File fXSD = new File(SwordExporterTester.class.getClassLoader().getResource("repositories_schema.xsd").getFile());
			
			// Validate xml schema
			if(!validateXMLSchema(fXSD, fXML)) {
				log.error("XML schema is not valid! Exit.");
				System.exit(1);
			}
			
			// Parse xml file
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(fXML);
			doc.getDocumentElement().normalize(); //normalization
			
			NodeList nList = doc.getElementsByTagName("repoConfig"); //Config for every repository
			
			// Iterate through all repositories
			for(int i=0; i<nList.getLength(); i++) {
				
				Node nNode = nList.item(i);
				if (nNode.getNodeType() == Node.ELEMENT_NODE) {

					System.out.println("==== TEST of SwordExporter, Repository Nr." + (i+1) + " ====\n");
					
					Element eElement = (Element) nNode;
					
					String serviceDocumentUrl = eElement.getElementsByTagName("serviceDocumentUrl").item(0).getTextContent();
					String metadataReplacementEntryUrl = eElement.getElementsByTagName("metadataReplacementEntryUrl").item(0).getTextContent();
					String adminUser = eElement.getElementsByTagName("adminUser").item(0).getTextContent();
					String userLogin = eElement.getElementsByTagName("normalUser").item(0).getTextContent();
					
					System.out.println("[serviceDocumentUrl] -- " + serviceDocumentUrl);
					System.out.println("[metadataReplacementEntryUrl] -- " + metadataReplacementEntryUrl);
					System.out.println("[adminUser] -- " + adminUser);
					System.out.println("[normalUser] -- " + userLogin);
					
				
				    
				    // Main test method
					CommonSwordRepository commonSwordRepository = null;
					if(serviceDocumentUrl.equals("") || metadataReplacementEntryUrl.equals("")) {
						log.error("Error: not defined serviceDocumentUrl or metadataReplacementEntryUrl. Please check the repositories.xml file.");
					} else {
						if(!adminUser.equals("") && !userLogin.equals("")) {
							// Enter admin-user password
							System.out.println("\nPassword for \"" + adminUser + "\" [admin]:");
							System.out.println("--> ATTENTION: typo is NOT hidden!");
							char[] adminPassword = scanner.nextLine().toCharArray();
							commonSwordRepository = new CommonSwordRepository(serviceDocumentUrl, adminUser, userLogin, adminPassword);
						} else {
							if (!userLogin.equals("")) {
								// Enter user password
								System.out.println("\nPassword for \"" + userLogin + "\" [standard user]:");
								System.out.println("--> ATTENTION: typo is NOT hidden!");
								char[] userPassword = scanner.nextLine().toCharArray();
								commonSwordRepository = new CommonSwordRepository(serviceDocumentUrl, userLogin, userPassword);
							} else {
								//ask about API-Token
								System.out.println("\nPlease enter the API-Token [will be used as credentials]:");
								System.out.println("--> ATTENTION: typo is NOT hidden!");
								char[] apiToken = scanner.nextLine().toCharArray();
								commonSwordRepository = new CommonSwordRepository(serviceDocumentUrl, apiToken);
							}
						}
						
					}
				    testSwordRepository(commonSwordRepository, serviceDocumentUrl, metadataReplacementEntryUrl);	
				}
			}
		
		} catch (Exception e) {
			log.error("Exception by testing: {}: {}: {}", e.getClass().getSimpleName(), e.getMessage(), e.getStackTrace());
		}
		
		scanner.close();
		
	}

	
	/**
	 * Publication repository test
	 * 
	 * @param serviceDocumentURL
	 * @param restURL
	 * @param publicationCollectionURL
	 * @param adminUser
	 * @param adminPassword
	 * @param userLogin
	 */
	private static void testSwordRepository(CommonSwordRepository swordRepository, String serviceDocumentUrl, String metadataReplacementEntryUrl) {
		
		String output = "";

		// Files for export
		File zipPackageFilesOnly = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/package_files_only.zip").getFile());
		File zipPackageFilesWithMetadata = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/package_files_with_metadata.zip").getFile());
		File zipPackageFilesMetadataOnly = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/package_metadata_only.zip").getFile());
		File zipPackageWithSubfolder = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/package_with_subfolder.zip").getFile());
		File xmlMetadataFile = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/entry.xml").getFile());
		File txtFile = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/test-file.txt").getFile());
		File otherFile = new File(SwordExporterTester.class.getClassLoader().getResource("testfiles/test-file.with.dots.txt.t").getFile());
		
		// Metadata
		Map<String, List<String>> metadataMap = new HashMap<String, List<String>>();
		metadataMap.put("title", Arrays.asList("TEST: My title !!!")); 			//OK, accepted
		metadataMap.put("not-real-field", Arrays.asList("TEST: unreal-name")); 	//will not be accepted
		metadataMap.put("publisher", Arrays.asList("TEST: some publisher")); 	//OK, accepted
		metadataMap.put("author", Arrays.asList("author-1")); 				//will not be accepted
		metadataMap.put("creator", Arrays.asList("creator-1", "creator-2", "creator-3")); 				//OK
		
		
		// Help functions testing
		output += "\n" + "== Test of helping functions:\n\n";
		output += "File: " + zipPackageFilesOnly.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(zipPackageFilesOnly.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(zipPackageFilesOnly.getName()) + "\n";
		output += "File: " + zipPackageFilesWithMetadata.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(zipPackageFilesWithMetadata.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(zipPackageFilesWithMetadata.getName()) + "\n";		
		output += "File: " + zipPackageFilesMetadataOnly.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(zipPackageFilesMetadataOnly.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(zipPackageFilesMetadataOnly.getName()) + "\n";
		output += "File: " + zipPackageWithSubfolder.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(zipPackageWithSubfolder.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(zipPackageWithSubfolder.getName()) + "\n";
		output += "File: " + xmlMetadataFile.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(xmlMetadataFile.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(xmlMetadataFile.getName()) + "\n";
		output += "File: " + txtFile.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(txtFile.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(txtFile.getName()) + "\n";
		output += "File: " + otherFile.getName() 
					+ " | Extension: " + SwordExporter.getFileExtension(otherFile.getName()) 
					+ " | Package type: " + SwordExporter.getPackageFormat(otherFile.getName()) + "\n";


		// Basic checks
		boolean isSwordAccessible = swordRepository.isSwordAccessible(serviceDocumentUrl);
		output += "\n" + "== Is SWORD interface accessible: " + isSwordAccessible +"\n";
		
		
		// Stop testing if SWORD is not accessible
		if(!isSwordAccessible) {
			output += "Error! SWORD API is not accessible, stop testing... \n";
			return;
		}
		
		
		// User available collections
		output += "\n" + "== User available collections:\n";
		ServiceDocument serviceDocument = swordRepository.getServiceDocument(serviceDocumentUrl);
		Map<String, String> collectionHierarchy = swordRepository.getCollectionsAsHierarchy(serviceDocument, "->");
		if(serviceDocument != null) {
			int i = 0;
			for(Map.Entry<String, String> collection: swordRepository.getCollections(serviceDocument).entrySet()) {
				i++;
				output += i + ": " + collection.getValue() + "\n";
				output += "-- URL:  " + collection.getKey() + "\n";
				output += "-- hierarchy:  " + collectionHierarchy.get(collection.getKey()) + "\n";
			}
		} else {
			output += "Error, service document is null. Can not get collections." + "\n";
		}
				
		
		// Replace metadata entry
		output += "\n" + "== Replace metadata for the entry: \n";
		output += "Entry: " + metadataReplacementEntryUrl + "\n";
		try {
			swordRepository.replaceMetadataEntry(metadataReplacementEntryUrl, metadataMap, true);
			output += "Replacement result: successful! \n";
		} catch (SWORDClientException e) {
			output += "Replacement result: not successful! Exception: " + e.getMessage() + "\n";
		}
		
		
		// Print results
		System.out.println(output);
	}
	
	
	/**
	 * Validate a XML-schema
	 * 
	 * @param fileXSD file with the XML schema
	 * @param fileXML file with the data in XML
	 * @return {@code true} if schema is correct and {@code false} otherwise
	 */
	public static boolean validateXMLSchema(File fileXSD, File fileXML) {
		
		SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		try {
			Schema schema = schemaFactory.newSchema(fileXSD);
			Validator validator = schema.newValidator();
			validator.validate(new StreamSource(fileXML));
			return true;
		
		} catch (SAXException | IOException ex) {
			log.error("Exception by XML schema validation: {}: {}", ex.getClass().getSimpleName(), ex.getMessage());
			return false;
		}
	}
	
}
