Steps to deploy new source content:

	1) Place the source file(s) into the native-source folder
	2) Update the version number as appropriate in pom.xml
	3) Run a command like this to deploy - (maestro is a server name that must be defined with credentials in your maven configuration):
		mvn deploy -DaltDeploymentRepository=maestro::default::https://va.maestrodev.com/archiva/repository/data-files/
		
Note - new source content should not be checked into GIT.  When finished, simply empty the native-source folder.

For the SNOMED CT release, the only required file follows the naming pattern of:

SnomedCT_RF2Release_INT_20150731.zip

The zip file should be placed in the native-source folder, and should not be unzipped.