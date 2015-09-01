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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Sct2_IdLookUp;
import gov.vha.isaac.rf2.convert.Sct2_IdRecord;

/**
 *
 * Read a file of SCTID with corresponding UUIDs and determines if the UUIDs
 * need to be re-mapped.
 * Creates an ARF file of primordial UUIDs<br>
 *
 * @author Marc E. Campbell
 */

@Mojo(name = "sct-rf2-uuid-cache-arf-gen", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Rf2IdUuidCacheArfGenMojo extends BaseRF2Mojo
{
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		try
		{
			List<Rf2File> filesIn;
			getLog().info("::: BEGIN Rf2UuidXmapGenMojo");
			super.execute();

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
				Sct2_IdRecord.parseIdsToArf(filesIn, bwIdArf, idLookup, pathUUID, uuidAuthor);
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
