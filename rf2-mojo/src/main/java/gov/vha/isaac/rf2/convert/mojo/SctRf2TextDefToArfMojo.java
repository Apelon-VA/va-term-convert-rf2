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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Rf2_RefsetCRecord;
import gov.vha.isaac.rf2.convert.Rf2x;
import gov.vha.isaac.rf2.convert.Sct2_DesRecord;

/**
 * @author Marc E. Campbell
 */
@Mojo(name = "sct-rf2-text-definition-to-arf", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SctRf2TextDefToArfMojo extends BaseRF2Mojo
{
	
	@Override
	public void execute() throws MojoExecutionException, MojoFailureException
	{
		List<Rf2File> filesIn;
		List<Rf2File> filesInStatus;
		getLog().info("::: BEGIN SctRf2ToArf");
		super.execute();

		try
		{
			Rf2x.setupIdCache(idCacheFile, enableUUIDToSCTIDMap);

			// FILE & DIRECTORY SETUP
			// Create multiple directories

			BufferedWriter bwIds = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "ids_textdefinitions.txt")), "UTF-8"));
			getLog().info("::: IDS OUTPUT: " + new File(arfOutPath, "ids_textdefinitions.txt").getAbsolutePath());

			// :NYI: extended status implementation does not multiple version years
			filesInStatus = Rf2File.getFiles(new File(new File(inputSctDir, "Refset"), "Content"), "AttributeValue", ".txt");
			Rf2_RefsetCRecord[] statusRecords = Rf2_RefsetCRecord.parseRefset(filesInStatus.get(0), null); // hardcoded

			// TEXTDEFINITION FILES "sct2_TextDefinition"
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "descriptions_textdefinitions_rf2.txt")),
					"UTF-8"));
			getLog().info("::: TEXTDEFINITIONS FILE: " + new File(arfOutPath, "descriptions_textdefinitions_rf2.txt").getAbsolutePath());
			filesIn = Rf2File.getFiles(inputSctDir, "sct2_TextDefinition", ".txt");
			for (Rf2File rf2File : filesIn)
			{
				Sct2_DesRecord[] textdefinitions = Sct2_DesRecord.parseDescriptions(rf2File, pathUUID.toString());
				textdefinitions = Sct2_DesRecord.attachStatus(textdefinitions, statusRecords);
				for (Sct2_DesRecord d : textdefinitions)
				{
					d.writeArf(bw);
					d.setPath(pathUUID.toString());
					if (Rf2x.isSctIdInUuidCache(d.getDesSnoIdL()) == false)
					{
						writeSctSnomedLongId(bwIds, d.getDesSnoIdL(), d.getEffDateStr(), d.getPathUuidStr());
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
		writer.append(IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid() + TAB_CHARACTER);
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
