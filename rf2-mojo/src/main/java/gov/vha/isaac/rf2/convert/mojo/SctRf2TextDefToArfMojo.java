/*
 * Copyright 2011 International Health Terminology Standards Development Organisation.
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

import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Rf2_RefsetCRecord;
import gov.vha.isaac.rf2.convert.Rf2x;
import gov.vha.isaac.rf2.convert.Sct2_DesRecord;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * @author Marc E. Campbell
 */
@Mojo(name = "sct-rf2-text-definition-to-arf", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SctRf2TextDefToArfMojo extends AbstractMojo
{

	private static final String FILE_SEPARATOR = File.separator;
	/**
	 * Line terminator is deliberately set to CR-LF which is DOS style
	 */
	private static final String LINE_TERMINATOR = "\r\n";
	private static final String TAB_CHARACTER = "\t";
	
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
	 * Path on which to load data. Default value SNOMED Core
	 */
	//TODO ask Keith how we handle path for this now... 
	@Parameter(required = false, defaultValue = "8c230474-9f11-30ce-9cad-185a96fd03a2") private UUID pathUUID;

	public void setUuidPath(String uuidStr)
	{
		pathUUID = UUID.fromString(uuidStr);
	}
	
	/**
	 * Enable storing an in-memory map from UUIDs to SCTIDs. May not always be necessary - set to false to reduce memory usage.
	 */
	@Parameter(required = false, defaultValue = "true") 
	private boolean enableUUIDToSCTIDMap = true;
	
	private String uuidSourceSnomedLongStr;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		List<Rf2File> filesIn;
		List<Rf2File> filesInStatus;
		getLog().info("::: BEGIN SctRf2ToArf");

		// SHOW DIRECTORIES
		String wDir = targetDirectory.getAbsolutePath();
		getLog().info("    POM Target Directory: " + targetDirectory.getAbsolutePath());
		getLog().info("    POM Target Sub Directory: " + targetSubDir);
		getLog().info("    POM Target Sub Data Directory: " + inputSctDir);

		getLog().info("    Path UUID: " + pathUUID);

		try
		{
			Rf2x.setupIdCache(targetDirectory, enableUUIDToSCTIDMap);

			// SETUP CONSTANTS
			uuidSourceSnomedLongStr = ArchitectonicAuxiliary.Concept.SNOMED_INT_ID.getPrimoridalUid().toString();

			// FILE & DIRECTORY SETUP
			// Create multiple directories
			String outDir = wDir + FILE_SEPARATOR + targetSubDir + FILE_SEPARATOR + outputArfDir + FILE_SEPARATOR;
			boolean success = (new File(outDir)).mkdirs();
			if (success)
			{
				getLog().info("::: Output Directory: " + outDir);
			}
			BufferedWriter bwIds = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outDir + "ids_textdefinitions.txt"), "UTF-8"));
			getLog().info("::: IDS OUTPUT: " + outDir + "ids.txt");

			// :NYI: extended status implementation does not multiple version years
			filesInStatus = Rf2File.getFiles(wDir, targetSubDir, "Refset/Content", "AttributeValue", ".txt");
			Rf2_RefsetCRecord[] statusRecords = Rf2_RefsetCRecord.parseRefset(filesInStatus.get(0), null); // hardcoded

			// TEXTDEFINITION FILES "sct2_TextDefinition"
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outDir + "descriptions_textdefinitions_rf2.txt"), "UTF-8"));
			getLog().info("::: TEXTDEFINITIONS FILE: " + outDir + "textdefinitions_rf2.txt");
			filesIn = Rf2File.getFiles(wDir, targetSubDir, inputSctDir, "sct2_TextDefinition", ".txt");
			for (Rf2File rf2File : filesIn)
			{
				Sct2_DesRecord[] textdefinitions = Sct2_DesRecord.parseDescriptions(rf2File, pathStr);
				textdefinitions = Sct2_DesRecord.attachStatus(textdefinitions, statusRecords);
				for (Sct2_DesRecord d : textdefinitions)
				{
					d.writeArf(bw);
					d.setPath(pathStr);
					if (Rf2x.isSctIdInUuidCache(d.desSnoIdL) == false)
					{
						writeSctSnomedLongId(bwIds, d.desSnoIdL, d.effDateStr, d.pathUuidStr);
					}
				}
			}
			bw.flush();
			bw.close();

			bwIds.flush();
			bwIds.close();

		}
		catch (Exception ex)
		{
			throw new MojoFailureException("RF2/ARF file name parse error", ex);
		}
	}

	private void writeSctSnomedLongId(BufferedWriter writer, long sctId, String date, String path) throws IOException
	{
		// PRIMARY_UUID = 0;
		writer.append(Rf2x.convertSctIdToUuidStr(sctId) + TAB_CHARACTER);
		// SOURCE_SYSTEM_UUID = 1;
		writer.append(uuidSourceSnomedLongStr + TAB_CHARACTER);
		// ID_FROM_SOURCE_SYSTEM = 2;
		writer.append(Long.toString(sctId) + TAB_CHARACTER);
		// STATUS_UUID = 3;
		writer.append(Rf2x.convertActiveToStatusUuid(true) + TAB_CHARACTER);
		// EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
		writer.append(date + TAB_CHARACTER);
		// PATH_UUID = 5;
		writer.append(path + LINE_TERMINATOR);
	}
}
