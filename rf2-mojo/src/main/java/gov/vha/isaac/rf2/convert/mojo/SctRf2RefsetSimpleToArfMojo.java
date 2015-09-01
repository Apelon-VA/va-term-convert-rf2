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
import java.io.OutputStreamWriter;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Rf2_RefsetSimpleRecord;
/**
 * @author Marc E. Campbell
 *
 */
@Mojo(name = "sct-rf2-simple-to-arf", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SctRf2RefsetSimpleToArfMojo extends BaseRF2Mojo{

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        List<Rf2File> filesIn;
        getLog().info("::: BEGIN SctRf2RefsetSimpleToArfMojo");

        super.execute();
        
        try {
            // SIMPLE REFSET FILES "der2_cRefset_Simple"
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arfOutPath, "concept_simple_rf2.refset")), "UTF-8"));
            getLog().info("::: SIMPLE REFSET FILE: " + new File(arfOutPath, "concept_simple_rf2.refset").getAbsolutePath());
            filesIn = Rf2File.getFiles(inputSctDir, "der2_cRefset", ".txt");
            for (Rf2File rf2File : filesIn) {
                if (rf2File.getFile().getName().toLowerCase().contains("simple")) {
                    Rf2_RefsetSimpleRecord[] members = Rf2_RefsetSimpleRecord.parseRefset(rf2File, pathUUID.toString());
                    for (Rf2_RefsetSimpleRecord m : members) {
                        m.setPath(pathUUID.toString());
                        m.writeArf(bw);
                        // writeSctSnomedLongId(bwIds, m.id, m.effDateStr, m.pathStr);
                    }
                }
            }
            bw.flush();
            bw.close();

            // bwIds.flush();
            // bwIds.close();
        } catch (Exception ex) {
            throw new MojoFailureException("RF2/ARF SctRf2RefsetSimpleToArfMojo file name parse error", ex);
        }
        getLog().info("::: END SctRf2RefsetSimpleToArfMojo");
    }
}
