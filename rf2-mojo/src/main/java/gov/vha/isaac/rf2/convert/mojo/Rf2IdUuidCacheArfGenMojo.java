/*
 * Copyright 2012 International Health Terminology Standards Development Organisation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.vha.isaac.rf2.convert.mojo;

import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Sct2_IdLookUp;
import gov.vha.isaac.rf2.convert.Sct2_IdRecord;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.UUID;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 *
 * Read a file of SCTID with corresponding UUIDs and determines if the UUIDs
 * need to be re-mapped.
 * Creates an ARF file of primordial UUIDs<br>
 *
 * @author Marc E. Campbell
 */

@Mojo(name = "sct-rf2-uuid-cache-arf-gen", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Rf2IdUuidCacheArfGenMojo extends AbstractMojo
{
	/**
	 * Location of the build directory.
	 */
	@Parameter(required = true, defaultValue = "${project.build.directory}") 
	private File targetDirectory;

	/**
	 * The folder that contains the RF2 release structure... this 
	 * should typically point to a subfolder such as Delta, Full or Snapshot.
	 */
	@Parameter(required = true) 
	private File inputSctDir;

	/**
	 * Directory used for intermediate serialized sct/uuid mapping cache
	 */
	@Parameter(required = true, defaultValue = "id-cache") 
	private String idCacheDir = "";
	
	/**
	 * Applicable output sub directory under the targetDir directory.
	 */
	@Parameter(required = true, defaultValue = "input-files") 
	private String outputSubDir = "";
	
	/**
	 * Directory used to output ARF identifier files for eConcept import, under the outputSubDir
	 */
	@Parameter(required = true, defaultValue = "generated-arf") 
	private String outputArfDir = "";


	/**
	 * Default value SNOMED Core
	 */
	//TODO ask Keith how we handle path for this now... 
	@Parameter(required = false, defaultValue = "8c230474-9f11-30ce-9cad-185a96fd03a2") 
	private UUID uuidPath;
	public void setUuidPath(String uuidStr)
	{
		uuidPath = UUID.fromString(uuidStr);
	}
	/**
	 * Enable storing an in-memory map from UUIDs to SCTIDs. May not always be necessary - set to false to reduce memory usage.
	 */
	@Parameter(required = false, defaultValue = "true") 
	private boolean enableUUIDToSCTIDMap = true;

	/**
	 * Default value Workbench Auxiliary 'user'
	 */
	@Parameter(required = false) 
	private UUID uuidAuthor = IsaacMetadataAuxiliaryBinding.USER.getPrimodialUuid();

	public void setUuidAuthor(String uuidStr)
	{
		uuidAuthor = UUID.fromString(uuidStr);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			List<Rf2File> filesIn;
			getLog().info("::: BEGIN Rf2UuidXmapGenMojo");
			// SHOW DIRECTORIES
			getLog().info("  POM       Target Directory:           " + targetDirectory.getAbsolutePath());
			getLog().info("  POM Input SCT Directory:              " + inputSctDir.getAbsolutePath());
			getLog().info("  POM ID SCT/UUID Cache Directory:      " + idCacheDir);
			getLog().info("  POM Output Target/Sub Directory:      " + outputSubDir);
			getLog().info("  POM Output Target/Sub/ARF Directory:  " + outputArfDir);

			// Setup directory paths
			File idCacheFile = new File(new File(targetDirectory, idCacheDir), "idSctUuidCache.ser");
			if (idCacheFile.getParentFile().mkdirs())
			{
				getLog().info("ID Cache directory created  ...");
			}
			getLog().info("::: ID Cache File: " + idCacheFile.getAbsolutePath());
			
			File arfOutPath = new File(new File(targetDirectory, outputSubDir), outputArfDir);
			if (arfOutPath.mkdirs())
			{
				getLog().info("::: Output Arf directory created ...");
			}
			getLog().info("::: Output Arf Path: " + arfOutPath);

			// Parse IHTSDO Terminology Identifiers to Sct_CompactId cache file.
			filesIn = Rf2File.getFiles(inputSctDir, "_Identifier", ".txt");
			Sct2_IdRecord.parseToIdPreCacheFile(filesIn, idCacheFile);
			// Setup id array cache object
			// idCacheDir + FILE_SEPARATOR + "idObjectCache.jbin"
			long startTime = System.currentTimeMillis();
			Sct2_IdLookUp idLookup = new Sct2_IdLookUp(idCacheFile, enableUUIDToSCTIDMap);
			System.out.println((System.currentTimeMillis() - startTime) + " mS");

			File arfOutput = new File(arfOutPath, "ids_assigned.txt");
			// Create an ARF file of primordial UUIDs
			try (BufferedWriter bwIdArf = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(arfOutput), "UTF-8")))
			{
				getLog().info("::: Assigned SCTID/UUID ARF output: " + arfOutput);
				Sct2_IdRecord.parseIdsToArf(filesIn, bwIdArf, idLookup, uuidPath, uuidAuthor);
				bwIdArf.flush();
				bwIdArf.close();
			}

		}
		catch (Exception ex)
		{
			throw new MojoExecutionException("Failed", ex);
		}
	}
}
