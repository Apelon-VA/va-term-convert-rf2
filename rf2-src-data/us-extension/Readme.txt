Steps to deploy new source content:

	1) Place the source file(s) into the native-source folder
	2) Update the version number as appropriate in pom.xml
	3) Run a command like this to deploy - (maestro is a server name that must be defined with credentials in your maven configuration):
		mvn deploy -DaltDeploymentRepository=maestro::default::https://va.maestrodev.com/archiva/repository/data-files/
		
Note - new source content should not be checked into GIT.  When finished, simply empty the native-source folder.

For the SNOMED CT release, the only required file follows the naming pattern of:

SnomedCT_Release_US1000124_20150301_Extension.zip

The zip file should be placed in the native-source folder, and should not be unzipped.

The source content is hidden on the NLM's website - new versions can be found by poking with the appropriate pattern:


http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_YYYYMMDD_Extension.zip

Past versions have been:
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_20150301_Extension.zip
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_RF2Release_Extension_US1000124_20150301.zip
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_20140901_Extension.zip
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_20140301_Extension.zip
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_Release_US1000124_20130901_Extension.zip


Though, for a time, it followed this pattern:
http://download.nlm.nih.gov/mlb/utsauth/USExt/SnomedCT_RF2Release_Extension_US1000124_YYYYMMDD.zip