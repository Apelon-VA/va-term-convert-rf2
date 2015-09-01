package gov.vha.isaac.rf2.convert.mojo;

import java.io.File;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;

public abstract class BaseRF2Mojo extends AbstractMojo
{
	/**
	 * Location of the build directory.
	 */
	@Parameter(required = true, defaultValue = "${project.build.directory}") 
	protected File targetDirectory;

	/**
	 * The folder that contains the RF2 release structure... this 
	 * should typically point to a subfolder such as Delta, Full or Snapshot.
	 */
	@Parameter(required = true) 
	protected File inputSctDir;

	/**
	 * Directory used for intermediate serialized sct/uuid mapping cache
	 */
	@Parameter(required = true, defaultValue = "id-cache") 
	protected String idCacheDir = "";
	
	/**
	 * Applicable output sub directory under the targetDir directory.
	 */
	@Parameter(required = true, defaultValue = "input-files") 
	protected String targetSubDir = "";
	
	/**
	 * Directory used to output ARF identifier files for eConcept import, under the outputSubDir
	 */
	@Parameter(required = true, defaultValue = "generated-arf") 
	protected String outputArfDir = "";


	/**
	 * Path on which to load data. Default value development
	 */
	@Parameter(required = false) 
	protected UUID pathUUID = IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid();
	public void setPathUUID(String uuidStr)
	{
		pathUUID = UUID.fromString(uuidStr);
	}
	/**
	 * Enable storing an in-memory map from UUIDs to SCTIDs. May not always be necessary - set to false to reduce memory usage.
	 */
	@Parameter(required = false, defaultValue = "true") 
	protected boolean enableUUIDToSCTIDMap = true;
	
	/**
	 * Default value Workbench Auxiliary 'user'
	 */
	@Parameter(required = false) 
	protected UUID uuidAuthor = IsaacMetadataAuxiliaryBinding.USER.getPrimodialUuid();

	public void setUuidAuthor(String uuidStr)
	{
		uuidAuthor = UUID.fromString(uuidStr);
	}

	protected File idCacheFile;
	protected File arfOutPath;
	/**
	 * Line terminator is deliberately set to CR-LF which is DOS style
	 */
	protected static final String LINE_TERMINATOR = "\r\n";
	protected static final String TAB_CHARACTER = "\t";
	
	
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		// SHOW DIRECTORIES
		getLog().info("  POM       Target Directory:           " + targetDirectory.getAbsolutePath());
		getLog().info("  POM Input SCT Directory:              " + inputSctDir.getAbsolutePath());
		getLog().info("  POM ID SCT/UUID Cache Directory:      " + idCacheDir);
		getLog().info("  POM Output Target/Sub Directory:      " + targetSubDir);
		getLog().info("  POM Output Target/Sub/ARF Directory:  " + outputArfDir);

		// Setup directory paths
		idCacheFile = new File(new File(targetDirectory, idCacheDir), "idSctUuidCache.ser");
		if (idCacheFile.getParentFile().mkdirs())
		{
			getLog().info("ID Cache directory created  ...");
		}
		getLog().info("::: ID Cache File: " + idCacheFile.getAbsolutePath());
		
		arfOutPath = new File(new File(targetDirectory, targetSubDir), outputArfDir);
		if (arfOutPath.mkdirs())
		{
			getLog().info("::: Output Arf directory created ...");
		}
		getLog().info("::: Output Arf Path: " + arfOutPath);
	}
}
