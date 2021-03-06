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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ExportRepository {
	
	/**
	 * Check if export repository is accessible via API.
	 * 
	 * @return {@code true} if repository is accessible and {@code false} otherwise.
	 */
	public boolean isRepositoryAccessible();
		
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) are registered 
	 * in the export repository.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @return {@code true} if credentials are registered and {@code false} otherwise.
	 */
	public boolean hasRegisteredCredentials();
	
	
	/**
	 * Check if current authentication credentials (e.g. API token, user login and password) 
	 * are assigned to export in the repository.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *
	 * @return {@code true} if count of user available collections is great than zero, 
	 * 		   otherwise {@code false}. 
 	 */
	public boolean hasAssignedCredentials();
	
	
	/**
	 * Get collections, which are available for the current authentication credentials.
	 * Could be, that different credentials can have an access to different collections.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *  
	 * @return {@link Map} of Strings, where key = "collection full URL", value = "collection title". 
	 * 		The map can be also empty if there are not available collections. 
	 * 		In case of some error should be returned a {@code null} value.  
	 */
	public Map<String, String> getAvailableCollections();
	
	
	/**
	 * Export (create) a new entry with metadata only (without any file) in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 *  
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param metadataMap metadata as {@link Map}, where key = metadata field (e.g. "creator", "title", "year", ... ), 
	 * 		value = {@link List} with the metadata field values (e.g. {"Author-1", "Author-2", ... }).
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.  
	 */
	public String exportNewEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap);
	
	
	/**
	 * Export (create) a new entry with metadata only (without any file) in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a xml-file.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param metadataFileXml metadata as a xml-file in dublin core (DC) format.
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException in case of IO error
	 */
	//TODO: activate in future releases
	//TODO: differentiate between dublin core (DC) and METS formats
	//public String exportNewEntryWithMetadata(String collectionURL, File metadataFileXml) throws IOException;
	
	
	/**
	 * Export (create) a new entry with metadata and file in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a {@link java.util.Map}. 
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done 
	 * @param metadataMap metadata as {@link Map}, where key = metadata field (e.g. "creator", "title", "year", ... ), 
	 * 		value = {@link List} with the metadata field values (e.g. {"Author-1", "Author-2", ... }). 
	 * @param file an archive file with one or more files inside (e.g. ZIP-file as a standard) or a binary file 
	 * 			which will be exported.
	 * @param unpackFileIfArchive should be used for archive files (e.g. ZIP). A flag which decides, 
	 * 			if the exported archive will be unpacked in the repository ({@code true} value,
	 * 			new entry will include in this case all files of the archive file) or archive will be exported 
	 * 			as a binary file ({@code false} value, new entry will include only 1 file - the exported archive
	 * 			as a binary file). <b>NOTE:</b> if unpacking is not supported by the repository, 
	 * 			please use {@code false} value. 
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException in case of IO error
	 */
	public String exportNewEntryWithMetadataAndFile(String collectionURL, Map<String, List<String>> metadataMap, 
							File file, boolean unpackFileIfArchive) throws IOException;
	
	
	/**
	 * Export (create) a new entry with metadata and file in some collection, which is available 
	 * for the current authentication credentials. Metadata are described as a xml-file.
	 * <p>
	 * <b>IMPORTANT:</b> credentials are used implicitly. Definition of the credentials must be done in other place, 
	 * e.g. via class constructor.
	 * 
	 * @param collectionURL the full URL of the collection, where the export (ingest) will be done.
	 * @param metadataFileXml metadata as a xml-file in dublin core (DC) format.
	 * @param file an archive file with one or more files inside (e.g. ZIP-file as a standard) or a binary file 
	 * 			which will be exported.
	 * @param unpackFileIfArchive should be used for archive files (e.g. ZIP). A flag which decides, 
	 * 			if the exported archive will be unpacked in the repository ({@code true} value,
	 * 			new entry will include in this case all files of the archive file) or archive will be exported 
	 * 			as a binary file ({@code false} value, new entry will include only 1 file - the exported archive
	 * 			as a binary file). <b>NOTE:</b> if unpacking is not supported by the repository, 
	 * 			please use {@code false} value.
	 * 
	 * @return {@link String} with the URL of the new created entry or {@code null} in case of error.
	 * 
	 * @throws IOException in case of IO error
	 */
	//TODO: activate in future releases
	//TODO: differentiate between dublin core (DC) and METS formats
	//public String exportNewEntryWithMetadataAndFile(String collectionURL, File metadataFileXml, 
	//			File file, boolean unpackFileIfArchive, ) throws IOException;
	
}
