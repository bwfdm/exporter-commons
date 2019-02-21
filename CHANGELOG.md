All notable changes to this project will be documented in this file.
We follow the [Semantic Versioning 2.0.0](http://semver.org/) format.




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
