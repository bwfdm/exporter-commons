# Exporter Commons
Library with common classes and interfaces for the data export (e.g. to some repository).

## Functionality
- [ExportRepository.java](src/main/java/bwfdm/exporter/commons/ExportRepository.java) - a common interface for the communication with any export repository (e.g. instance of DSpace, Dataverse etc.)

- [SwordExporter.java](src/main/java/bwfdm/exporter/commons/SwordExporter.java) - an abstract class with general exporting methods for SWORD-based repositories (e.g. DSpace, Dataverse etc.), SWORDv2 is supported for now. 

For changes history please see [CHANGELOG.md](CHANGELOG.md)

## How to add to your maven project (as dependency in pom.xml)

```
<dependency>
    <groupId>com.github.bwfdm</groupId>
    <artifactId>exporter-commons</artifactId>
    <version>0.1.1</version>
</dependency>  
```

## How to use
- if repository supports a SWORD protocol, the repository class should extend the [SwordExporter.java](src/main/java/bwfdm/exporter/commons/SwordExporter.java). 
- if common interface for different repositories is needed, the repository class should implement the [ExportRepository.java](src/main/java/bwfdm/exporter/commons/ExportRepository.java)
<!--
- (TODO: start to use SwordExporter in the DSpace connector) as an example could be a [DSpace connector](https://github.com/bwfdm/dspace-connector), especially the [DSpace_v6.java](https://github.com/bwfdm/dspace-connector/blob/master/src/main/java/bwfdm/connector/dspace/DSpace_v6.java) class. 
-->

## Tests
- for test example see [SwordExporterTester.java](src/test/java/bwfdm/exporter/commons/test/SwordExporterTester.java)
- for credentials input please see [repositories_schema.xsd](src/test/resources/repositories_schema.xsd) and [repositories_template.xml](src/test/resources/repositories_template.xml)

## Limitations
- currently SwordExporter supports metadata in DublinCore only as Map<String, List<String>>. Metadata as XML-file is NOT supported yet.

## Used third party libraries and their licenses
- see [license-third-party.txt](license-third-party.txt)
     
## Own license
- MIT, see [license.txt](license.txt)
