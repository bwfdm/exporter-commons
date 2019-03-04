All notable changes to this project will be documented in this file.
We follow the [Semantic Versioning 2.0.0](http://semver.org/) format.




## 0.4.0 - 2019-03-04

#### Added
- static method "isServiceDocumentWithSubservices(String serviceDocumentUrl, AuthCredentials authCredentials)"
to analyze a type of the service document even before the creation of the 
repository object 

#### Deprecated
- none

#### Removed
- exportNewEntryWithFileAndMetadata

#### Fixed
- rename of "HierarchyObject" and "HierarchyCollectionObject"
  * HierarchyObjectSword
  * HierarchyCollectionObjectSword
- rename some export method:  
  * exportNewEntryWithMetadataAndFile
- reorder input parameters in:
  * exportNewEntryWithMetadataAndFile(String collectionURL, Map<String, List<String>> metadataMap, File file, boolean unpackFileIfArchive)
  * createEntryWithMetadataAndFile(String collectionURL, Map<String, List<String>> metadataMap, File file, boolean unpackZip, boolean inProgress)
      



## 0.3.0 - 2019-03-01

#### Added
- support of "service" tag inside the "collection" tag in the service document 
(e.g. in case of DSpace it means, that service document represents communities and not collections. 
To get collections in that case, further request to the service URL - URL inside the "service" tag - is needed). 
New methods:
  * isServiceDocumentWithSubservices(ServiceDocument serviceDocument)
  * getHierarchy(ServiceDocument serviceDocument)
  * getCollectionsAsHierarchy(ServiceDocument serviceDocument, String hierarchySeparator)
- new classes "HierarchyObject" and "HierarchyCollectionObject" which represent the hierarchy of collections and services  

#### Deprecated
- none

#### Removed
- none

#### Fixed
- none




## 0.2.0 - 2019-02-21

#### Added
- support of explicit usage of the "In-Progress: true/false" header for SWORDv2 requests

#### Deprecated
- none

#### Removed
- old methods, which do not have "inProgress" variable as input, are not supported now. Please use new notation:
  * exportElement(String exportURL, SwordRequestType swordRequestType, String mimeFormat, String packageFormat, File file, Map<String, List<String>> metadataMap, boolean inProgress)
  * createEntryWithMetadata(String collectionURL, Map<String, List<String>> metadataMap, boolean inProgress)
  * createEntryWithMetadataAndFile(String collectionURL, File file, boolean unpackZip, Map<String, List<String>> metadataMap, boolean inProgress)
  * replaceMetadataEntry(String entryUrl, Map<String, List<String>> metadataMap, boolean inProgress)

#### Fixed
- none




## 0.1.1 - 2019-02-19

#### Added
- prepare for support of metadata as XML file

#### Deprecated
- none

#### Removed
- none

#### Fixed
- javadoc




## 0.1.0 - 2018-12-05

#### Added
- support of common for many repositories SWORDv2 features 
- common exporting interface (for now suits for DSpace and Dataverse repositories)

#### Deprecated
- none

#### Removed
- none

#### Fixed
- none
