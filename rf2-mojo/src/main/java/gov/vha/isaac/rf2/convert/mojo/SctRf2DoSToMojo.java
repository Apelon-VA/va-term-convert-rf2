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
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Rf2_RefsetCRecord;
import gov.vha.isaac.rf2.convert.Rf2_RefsetId;


/**
 *
 * @author marc
 */
@Mojo(name = "sct-rf2-dos-to-arf", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SctRf2DoSToMojo extends BaseRF2Mojo {

	@Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Rf2File> filesIn;
        getLog().info("::: BEGIN Rf2_RefsetCreateConceptMojo");

        super.execute();

        try {
  
            // CONCEPT REFSET FILES
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath,  "concept_refsetDoS_rf2.refset")), "UTF-8"));
            getLog().info("::: DoS REFSET FILE: " + new File(arfOutPath, "concept_refsetDoS_rf2.refset"));
            filesIn = Rf2File.getFiles(inputSctDir, "AttributeValue", ".txt");
            for (Rf2File rf2File : filesIn) {
                Rf2_RefsetCRecord[] members = Rf2_RefsetCRecord.parseRefset(rf2File, null);
                for (Rf2_RefsetCRecord m : members) {
                    m.setPath(pathUUID.toString());
                    m.writeArf(bw);
                }
            }
            bw.flush();
            bw.close();

            // WRITE PARENT REFSET CONCEPT :!!!:INTERIM:
            ArrayList<Rf2_RefsetId> refsetIdList = new ArrayList<>();
            refsetIdList.add(new Rf2_RefsetId(449613003L, /* refsetSctIdOriginal */
                    "2002.01.31", /* refsetDate */
                    "8c230474-9f11-30ce-9cad-185a96fd03a2", /* refsetPathUuidStr */
                    "Degree of Synonymy Refset (RF2)", /* refsetPrefTerm */
                    "Degree of Synonymy Refset (RF2)", /* refsetFsName */
                    "3e0cd740-2cc6-3d68-ace7-bad2eb2621da")); /* refsetParentUuid */
            Rf2_RefsetId.saveRefsetConcept(arfOutPath.getAbsolutePath(), refsetIdList);

            getLog().info("::: END Rf2_RefsetCreateConceptMojo");
        } catch (IOException | ParseException | NoSuchAlgorithmException ex) {
            Logger.getLogger(SctRf2DoSToMojo.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
