# Exporter Commons
Library with common classes and interfaces for the data export (e.g. to some repository).

## Functionality
- [ExportRepository.java](src/main/java/bwfdm/exporter/commons/ExportRepository.java) - a common interface for the communication with any export repository (e.g. instance of DSpace, Dataverse etc.)

- [SwordExporter.java](src/main/java/bwfdm/exporter/commons/SwordExporter.java) - an abstract class with general exporting methods for SWORD-based repositories (e.g. DSpace, Dataverse etc.), SWORDv2 is supported for now. 

## How to add to your maven project (as dependency im pom.xml)
- ..
- TODO

## How to use
- if repository supports a SWORD protocol, the repository class should extend the [SwordExporter.java](src/main/java/bwfdm/exporter/commons/SwordExporter.java). 
- if common interface for different repositories is needed, the repository class should implement the [ExportRepository.java](src/main/java/bwfdm/exporter/commons/ExportRepository.java)
- (TODO for later) as an example could be a [DSpace connector](https://github.com/bwfdm/dspace-connector), especially the [DSpace_v6.java](https://github.com/bwfdm/dspace-connector/blob/master/src/main/java/bwfdm/connector/dspace/DSpace_v6.java) class. 

## Features
- ..
- TODO

## Tests
- ..
- TODO

## Limitations
- ..
- TODO

## Used third party libraries and their licenses
- see [license-third-party.txt](license-third-party.txt)
     
## Own license
- MIT, see [license.txt](license.txt)
