/*
 * Copyright 2011 International Health Terminology Standards Development Organisation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import gov.vha.isaac.rf2.convert.Rf2x;
import gov.vha.isaac.rf2.convert.Sct2_ConRecord;
import gov.vha.isaac.rf2.convert.Sct2_DesRecord;
import gov.vha.isaac.rf2.convert.Sct2_RelRecord;


//TODO figure out what is going on with this class - almost identical to the one with status (so it should be a flag) and worse, in the pom
//it is also being executed... so it just overwrites the previous work.... sigh.

/**
 * @author Marc E. Campbell
 */
@Mojo(name = "sct-rf2-to-arf-no-status", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SctRf2ToArfNoStatusMojo extends BaseRF2Mojo{


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Rf2File> filesIn;
        getLog().info("::: BEGIN SctRf2ToArfNoStatus");

        // SHOW DIRECTORIES
        super.execute();

        try {
            Rf2x.setupIdCache(idCacheFile, enableUUIDToSCTIDMap);

            BufferedWriter bwIds = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "ids.txt")), "UTF-8"));
            getLog().info("::: IDS OUTPUT: " + new File(arfOutPath, "ids.txt").getAbsolutePath());


            // hardcoded
            // CONCEPT FILES: parse, write
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "concepts_rf2.txt")), "UTF-8"));
            getLog().info("::: CONCEPTS FILE: " + new File(arfOutPath, "concepts_rf2.txt").getAbsolutePath());
            filesIn = Rf2File.getFiles(inputSctDir, "sct2_Concept", ".txt");
            for (Rf2File rf2File : filesIn) {
                getLog().info("    ... " + rf2File.getFile().getName());
                Sct2_ConRecord[] concepts = Sct2_ConRecord.parseConcepts(rf2File, pathUUID.toString());
                for (Sct2_ConRecord c : concepts) {
                    c.setPath(pathUUID.toString());
                    c.writeArf(bw);
                    if (Rf2x.isSctIdInUuidCache(c.getConSnoIdL()) == false) {
                        writeSctSnomedLongId(bwIds, c.getConSnoIdL(), c.getEffDateStr(), c.getPathUuidStr());
                    }
                }
            }
            bw.flush();
            bw.close();

            // DESCRIPTION FILES "sct2_Description"
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "descriptions_rf2.txt")), "UTF-8"));
            getLog().info("::: DESCRIPTIONS FILE: " + new File(arfOutPath, "descriptions_rf2.txt"));
            filesIn = Rf2File.getFiles(inputSctDir, "sct2_Description", ".txt");
            for (Rf2File rf2File : filesIn) {
                getLog().info("    ... " + rf2File.getFile().getName());
                Sct2_DesRecord[] descriptions = Sct2_DesRecord.parseDescriptions(rf2File, pathUUID.toString());
                for (Sct2_DesRecord d : descriptions) {
                    d.setPath(pathUUID.toString());
                    d.writeArf(bw);
                    if (Rf2x.isSctIdInUuidCache(d.getDesSnoIdL()) == false) {
                        writeSctSnomedLongId(bwIds, d.getDesSnoIdL(), d.getEffDateStr(), d.getPathUuidStr());
                    }
                }
            }
            bw.flush();
            bw.close();

            // RELATIONSHIP FILES "sct2_StatedRelationship" "sct2_Relationship"
            bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "relationships_rf2.txt")), "UTF-8"));
            getLog().info("::: RELATIONSHIPS FILE: " + new File(arfOutPath, "relationships_rf2.txt").getAbsolutePath());
            filesIn = Rf2File.getFiles(inputSctDir, "sct2_Relationship", ".txt");
            filesIn.addAll(Rf2File.getFiles(inputSctDir, "res2_RetiredIsaRelationship", ".txt"));
            for (Rf2File rf2File : filesIn) {
                getLog().info("    ... " + rf2File.getFile().getName());
                Sct2_RelRecord[] rels = Sct2_RelRecord.parseRelationships(rf2File, true, pathUUID.toString());
                for (Sct2_RelRecord r : rels) {
                    r.setPath(pathUUID.toString());
                    r.writeArf(bw);
                    if (Rf2x.isSctIdInUuidCache(r.getRelSnoId()) == false) {
                        writeSctSnomedLongId(bwIds, r.getRelSnoId(), r.getEffDateStr(), r.getPathUuidStr());
                    }
                }
            }

            filesIn = Rf2File.getFiles(inputSctDir, "sct2_StatedRelationship", ".txt");
            filesIn.addAll(Rf2File.getFiles(inputSctDir, "res2_RetiredStatedIsaRelationship", ".txt"));
            for (Rf2File rf2File : filesIn) {
                getLog().info("    ... " + rf2File.getFile().getName());
                Sct2_RelRecord[] rels = Sct2_RelRecord.parseRelationships(rf2File, false, pathUUID.toString());
                for (Sct2_RelRecord r : rels) {
                    r.setPath(pathUUID.toString());
                    r.writeArf(bw);
                    if (Rf2x.isSctIdInUuidCache(r.getRelSnoId()) == false) {
                        writeSctSnomedLongId(bwIds, r.getRelSnoId(), r.getEffDateStr(), r.getPathUuidStr());
                    }
                }
            }
            bw.flush();
            bw.close();

            bwIds.flush();
            bwIds.close();

        } catch (Exception ex) {
            throw new MojoFailureException("RF2/ARF file name parse error", ex);
        }
    }

    private void writeSctSnomedLongId(BufferedWriter writer, long sctId, String date, String path) throws IOException {
        // PRIMARY_UUID = 0;
        writer.append(Rf2x.convertSctIdToUuidStr(sctId) + TAB_CHARACTER);
        // SOURCE_SYSTEM_UUID = 1;
        writer.append(IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid().toString() + TAB_CHARACTER);
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
