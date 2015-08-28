/*
 * Copyright 2012 International Health Terminology Standards Development Organisation.
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

import gov.vha.isaac.rf2.convert.Rf2File;
import gov.vha.isaac.rf2.convert.Sct2_IdLookUp;
import gov.vha.isaac.rf2.convert.Sct2_IdRecord;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
public class Rf2IdUuidCacheArfGenMojo extends AbstractMojo {

    /**
     * Line terminator is deliberately set to CR-LF which is DOS style
     */
    private static final String FILE_SEPARATOR = File.separator;

    /**
     * Location of the build directory.
     *
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}") 
    private File targetDirectory;
    /**
     * Applicable input sub directory under the build directory.
     */
    @Parameter(required = false) 
    private String inputSubDir = "";
    
    @Parameter(required = false) 
    private String inputSctDir = "ids";
    
    /**
     * Directory used for intermediate serialized sct/uuid mapping cache
     */
    @Parameter(required = false) 
    private String idCacheDir = "";
    
    /**
     * Applicable input sub directory under the build directory.
     */
    @Parameter(required = false, defaultValue = "generated-arf") 
    private String outputSubDir = "";
    
    /**
     * Directory used to output ARF identifier files for eConcept import
     */
    @Parameter(required = false, defaultValue = "generated-arf") 
    private String outputArfDir = "";
    
    /**
     * Default value SNOMED Core
     */
    @Parameter(required = false, defaultValue = "8c230474-9f11-30ce-9cad-185a96fd03a2") 
    private UUID uuidPath;
    
    /**
     * Enable storing an in-memory map from UUIDs to SCTIDs.  May not always be necessary - set to false to reduce memory usage.
     */
    @Parameter(required = false, defaultValue = "true") 
    private boolean enableUUIDToSCTIDMap = true;

    public void setUuidPath(String uuidStr) {
        uuidPath = UUID.fromString(uuidStr);
    }
    /**
     * Default value Workbench Auxiliary 'user'
     */
    @Parameter(required = false, defaultValue = "f7495b58-6630-3499-a44e-2052b5fcf06c") 
    private UUID uuidAuthor;

    public void setUuidAuthor(String uuidStr) {
        uuidAuthor = UUID.fromString(uuidStr);
    }

    @Override
    public void execute()
            throws MojoExecutionException, MojoFailureException {
            try {
                List<Rf2File> filesIn;
                getLog().info("::: BEGIN Rf2UuidXmapGenMojo");
                // SHOW DIRECTORIES
                String wDir = targetDirectory.getAbsolutePath();
                getLog().info("  POM       Target Directory:           "
                        + targetDirectory.getAbsolutePath());
                getLog().info("  POM Input Target/Sub Directory:       "
                        + inputSubDir);
                getLog().info("  POM Input Target/Sub/SCTID Directory: "
                        + inputSctDir);
                getLog().info("  POM ID SCT/UUID Cache Directory:      "
                        + idCacheDir);
                getLog().info("  POM Output Target/Sub Directory:      "
                        + outputSubDir);
                getLog().info("  POM Output Target/Sub/ARF Directory:  "
                        + outputArfDir);
                
                // Setup directory paths
                getLog().info("::: Input Sct Path: " + wDir + FILE_SEPARATOR
                        + inputSubDir + FILE_SEPARATOR + inputSctDir);
                String cachePath = wDir + FILE_SEPARATOR + idCacheDir + FILE_SEPARATOR;
                String idCacheFName = cachePath + "idSctUuidCache.ser";
                if ((new File(cachePath)).mkdirs()) {
                    getLog().info("ID Cache directory created ... ");
                }
                getLog().info("::: ID Cache : " + idCacheFName);
                String arfOutPath = wDir + FILE_SEPARATOR + outputSubDir
                        + FILE_SEPARATOR + outputArfDir + FILE_SEPARATOR;
                if ((new File(arfOutPath)).mkdirs()) {
                    getLog().info("::: Output Arf directory created ... ");
                }
                getLog().info("::: Output Arf Path: " + arfOutPath);

                // Parse IHTSDO Terminology Identifiers to Sct_CompactId cache file.
                filesIn = Rf2File.getFiles(wDir, inputSubDir, inputSctDir,
                        "_Identifier", ".txt");
                Sct2_IdRecord.parseToIdPreCacheFile(filesIn, idCacheFName);
                // Setup id array cache object
                // idCacheDir + FILE_SEPARATOR + "idObjectCache.jbin"
                long startTime = System.currentTimeMillis();
                Sct2_IdLookUp idLookup = new Sct2_IdLookUp(idCacheFName, enableUUIDToSCTIDMap);
                System.out.println((System.currentTimeMillis() - startTime) + " mS");
                
                // Create an ARF file of primordial UUIDs
                String idAssignedArfFName = arfOutPath + "ids_assigned.txt";
                try (BufferedWriter bwIdArf = new BufferedWriter(
                        new OutputStreamWriter(
                                new FileOutputStream(
                                        idAssignedArfFName), "UTF-8"))) {
                    getLog().info("::: Assigned SCTID/UUID ARF output: "
                            + idAssignedArfFName);
                    Sct2_IdRecord.parseIdsToArf(filesIn, bwIdArf, idLookup,
                            uuidPath, uuidAuthor);
                    bwIdArf.flush();
                    bwIdArf.close();
                }
                
            }   catch (ParseException | IOException ex) {
            Logger.getLogger(Rf2IdUuidCacheArfGenMojo.class.getName()).log(Level.SEVERE, null, ex);
        }
            
    }
}
