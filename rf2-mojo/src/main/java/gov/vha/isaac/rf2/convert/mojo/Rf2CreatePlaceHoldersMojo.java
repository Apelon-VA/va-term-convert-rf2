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

import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import gov.vha.isaac.rf2.convert.Rf2Dir;
import gov.vha.isaac.rf2.convert.Rf2File;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * <b>DESCRIPTION: </b><br>
 *
 * Rf2CreatePlaceHoldersMojo is a maven mojo which creates intermediate "place
 * holder" concepts in ARF files for used by Sct1ArfToEConceptsMojo.<br> <br>
 *
 * <b>INPUTS:</b><br> The pom needs to configure the following parameters for
 * the
 * <code>sct1-create-placeholder-concepts</code> goal.
 * <pre>
 * &lt;targetSub&gt; subdirectoryname -- working sub directly under build directory
 * &lt;Rf2Dirs&gt;            -- creates list of directories to be searched
 *    &lt;Rf2Dir&gt; dir_name -- specific directory to be added to the search list
 *
 */
@Mojo(name = "sct2-create-placeholder-concepts", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Rf2CreatePlaceHoldersMojo extends BaseRF2Mojo {
    
    private static final String FILE_SEPARATOR = File.separator;

    @Parameter(required = true)
    private Rf2Dir[] rf2Dirs;

    /**
     * Effective date for "place holder" concepts
     */
    @Parameter(required = true, defaultValue="2002-01-01 00:00:00")
    private String effectiveDate;

     /**
     * Default value from ISAAC Module
     */
    @Parameter(required = false) 
    private UUID moduleUUID = IsaacMetadataAuxiliaryBinding.ISAAC_MODULE.getPrimodialUuid();
    public void setUUIDModule(String uuidStr) {
        moduleUUID = UUID.fromString(uuidStr);
    }

    ArrayList<Long> foundSctIds;
    ArrayList<Long> neededSctIds;
    List<List<Rf2File>> listOfDirs = new ArrayList<List<Rf2File>>();
    
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("::: BEGIN Rf2CreatePlaceHoldersMojo");
        super.execute();
        List<Rf2File> filesIn;
        long[] knownConcepts = null;
        long[] expectedConcepts = null;
        long[] scratchConcepts;
        try {
            // PROCESS CONCEPTS
            filesIn = getRF2Files(targetDirectory, rf2Dirs, "sct2_Concept", ".txt");
            for (Rf2File file : filesIn) {
                int CONCEPTID = 0; // Column in RF2 Concept File
                if (knownConcepts == null) {
                    knownConcepts = parseForConceptIds(file, CONCEPTID);
                } else {
                    long[] additionalConcepts = parseForConceptIds(file, CONCEPTID);

                    long[] both = new long[knownConcepts.length + additionalConcepts.length];
                    System.arraycopy(knownConcepts, 0, both, 0, knownConcepts.length);
                    System.arraycopy(additionalConcepts, 0, both, knownConcepts.length, additionalConcepts.length);

                    Arrays.sort(both);

                    // REMOVE DUPLICATES
                    int countDups = 0;
                    for (int i = 0; i < both.length - 1; i++) {
                        if (both[i] == both[i + 1]) {
                            countDups++;
                        }
                    }

                    if (countDups > 0) {
                        long[] b = new long[both.length - countDups];
                        int j = 0;
                        for (int i = 0; i < both.length - 1; i++) {
                            if (both[i] != both[i + 1]) {
                                b[j] = both[i];
                                j++;
                            }
                        }
                        if (j < b.length) { // if one remaining unique value
                            b[j] = both[both.length - 1];
                        }
                        both = b;
                    }
                    knownConcepts = both;
                }
            }
            getLog().info("    Total known concepts: " + knownConcepts.length);

            // PROCESS DESCRIPTIONS
            filesIn = getRF2Files(targetDirectory, rf2Dirs, "sct2_Description", ".txt");
            for (Rf2File file : filesIn) {
                int CONCEPTID = 4; // Column in RF2 Description File
                scratchConcepts = parseForConceptIds(file, CONCEPTID);
                scratchConcepts = findConceptsNotPresent(knownConcepts, scratchConcepts);
                expectedConcepts = mergeConceptArrays(expectedConcepts, scratchConcepts);
            }

            // PROCESS RELATIONSHIPS
            filesIn = getRF2Files(targetDirectory, rf2Dirs, "sct2_Relationship", ".txt");
            
            for (Rf2File file : filesIn) {
                // source concepts
                int CONCEPTID1 = 4;
                scratchConcepts = parseForConceptIds(file, CONCEPTID1);
                scratchConcepts = findConceptsNotPresent(knownConcepts, scratchConcepts);
                expectedConcepts = mergeConceptArrays(expectedConcepts, scratchConcepts);
                // relationships type concepts
                int RELATIONSHIPTYPE = 7;
                scratchConcepts = parseForConceptIds(file, RELATIONSHIPTYPE);
                scratchConcepts = findConceptsNotPresent(knownConcepts, scratchConcepts);
                expectedConcepts = mergeConceptArrays(expectedConcepts, scratchConcepts);
                // destination concepts
                int CONCEPTID2 = 5;
                scratchConcepts = parseForConceptIds(file, CONCEPTID2);
                scratchConcepts = findConceptsNotPresent(knownConcepts, scratchConcepts);
                expectedConcepts = mergeConceptArrays(expectedConcepts, scratchConcepts);
            }

            // PROCESS SUBSETS
            filesIn = getRF2Files(targetDirectory, rf2Dirs, "Refset", ".txt");
            for (Rf2File file : filesIn) {
                int REFSETID = 4; // Column in subset file
                scratchConcepts = parseForConceptIds(file, REFSETID);
                scratchConcepts = findConceptsNotPresent(knownConcepts, scratchConcepts);
                expectedConcepts = mergeConceptArrays(expectedConcepts, scratchConcepts);
            }
            
            if (expectedConcepts != null && expectedConcepts.length > 0) {
                writePlaceHolderConcepts(expectedConcepts);
            }
            
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        
        getLog().info("::: END Rf2CreatePlaceHoldersMojo");
    }

    /**
     * Gather RF2 concept ids from the given
     * <code>column</code> in the data file
     * <code>f</code>.
     */
    private long[] parseForConceptIds(Rf2File f, int column) throws MojoFailureException, IOException {
        long start = System.currentTimeMillis();
        long[] a = new long[Rf2File.countFileLines(f)];
        
        try {
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(
                    new FileInputStream(f.getFile()), "UTF-8"));
            int concepts = 0;

            // Header row
            br.readLine();
            
            while (br.ready()) {
                String[] line = br.readLine().split(TAB_CHARACTER);
                a[concepts] = Long.parseLong(line[column]);
                concepts++;
            }
            Arrays.sort(a);

            // REMOVE DUPLICATES
            int countDups = 0;
            for (int i = 0; i < a.length - 1; i++) {
                if (a[i] == a[i + 1]) {
                    countDups++;
                }
            }
            
            if (countDups > 0) {
                long[] b = new long[a.length - countDups];
                int j = 0;
                for (int i = 0; i < a.length - 1; i++) {
                    if (a[i] != a[i + 1]) {
                        b[j] = a[i];
                        j++;
                    }
                }
                if (j < b.length) { // if one remaining unique value
                    b[j] = a[a.length - 1];
                }
                a = b;
            }
            
            getLog().info("    Parsed File: " + f.getFile().getName()
                    + " unique concept ids: " + a.length + " concepts, "
                    + (System.currentTimeMillis() - start) + " milliseconds");
            br.close();
            
        } catch (Exception e) {
            throw new MojoFailureException("parseForConceptIds failed", e);
        }
        
        return a;
    }

    /**
     * Note:
     * <code>known</code> and
     * <code>expected</code> must have non-duplicate value in sort order.
     */
    private long[] findConceptsNotPresent(long[] known, long[] expected) {
        // HANDLE TRIVIAL CASE
        if (known == null) {
            return expected;
        }

        // count
        int e = 0;
        int k = 0;
        int count = 0;
        while (k < known.length && e < expected.length) {
            if (known[k] < expected[e]) { // no unexpected news
                k++;
            } else if (known[k] == expected[e]) { // already known
                k++;
                e++;
            } else { // known[k] < expected[e] ... found unexpected concept
                count++;
                e++;
            }
        }
        if (e < expected.length) { // check remainder for unexpected news
            count += expected.length - e;
        }
        
        if (count == 0) { // no new expected concepts found
            return null;
        }

        //
        long[] result = new long[count];
        e = 0;
        k = 0;
        count = 0;
        while (k < known.length && e < expected.length) {
            if (known[k] < expected[e]) { // no unexpected news
                k++;
            } else if (known[k] == expected[e]) { // already known
                k++;
                e++;
            } else { // known[k] < expected[e] ... keep unexpected concept
                result[count] = expected[e];
                count++;
                e++;
            }
        }
        while (e < expected.length) { // add remainder for unexpected news
            result[count] = expected[e];
            count++;
            e++;
        }
        
        getLog().info("    FOUND NOT PRESENT = " + result.length);
        
        return result;
    }

    /**
     * Note:
     * <code>dataA</code> and
     * <code>dataB</code> must have non-duplicate values in sort order.
     */
    private long[] mergeConceptArrays(long[] dataA, long[] dataB) throws MojoFailureException {
        if (dataA == null) { // everything to merge is in dataB. dataB may still be null.
            return dataB;
        } else if (dataB == null) { // nothing to merge
            return dataA;
        }

        // determine what needs to be merged
        long[] toBeMerged = findConceptsNotPresent(dataA, dataB);

        // nothing to be merged
        if (toBeMerged == null || toBeMerged.length == 0) {
            return dataA;
        }

        // add new values
        long[] mergedData = new long[dataA.length + toBeMerged.length];
        
        int a = 0;
        int tbm = 0;
        for (int m = 0; m < mergedData.length; m++) {
            if (a == dataA.length) {
                    mergedData[m] = toBeMerged[tbm];
                        tbm++;
            } else if(tbm >= toBeMerged.length){
                mergedData[m] = dataA[a];
                a++;
            }else if (dataA[a] < toBeMerged[tbm]) {
                mergedData[m] = dataA[a];
                a++;
            } else if (dataA[a] == toBeMerged[tbm]) {
                throw new MojoFailureException(
                        "dataA[a] and toBeMerged[tbm] must be mutually exclusive");
            } else {
                    mergedData[m] = toBeMerged[tbm];
                        tbm++;
                    }
                }
        if (a < dataA.length || tbm < toBeMerged.length) {
            throw new MojoFailureException("mergeConceptArrays failed completion check");
        }
        
        getLog().info("    MERGED LENGTH = " + mergedData.length);
        
        return mergedData;
    }
    
    private List<Rf2File> getRF2Files(File wDir, Rf2Dir[] inDirs,
            String prefix, String postfix) throws MojoFailureException {
        
        List<Rf2File> listOfRf2Dirs = new ArrayList<Rf2File>();
        for (Rf2Dir rf2Dir : inDirs) {
            ArrayList<Rf2File> listOfFiles = new ArrayList<Rf2File>();
            
            getLog().info(
                    String.format("%1$s (%2$s%3$s) ", prefix.toUpperCase(), wDir, rf2Dir.getDirName()));
            
            File f1 = new File(wDir, rf2Dir.getDirName());
            ArrayList<File> fv = new ArrayList<File>();
            listFilesRecursive(fv, f1, prefix, postfix);
            
            File[] files = new File[0];
            files = fv.toArray(files);
            Arrays.sort(files);
            
            for (File f2 : files) {
                // ADD Rf2File Entry
                String revDateStr = getFileRevDate(f2);
                String pattern = "yyyy-MM-dd HH:mm:ss";
                SimpleDateFormat formatter = new SimpleDateFormat(pattern);
                Date revDate = null;
                try {
                    revDate = formatter.parse(revDateStr);
                } catch (ParseException e) {
                    e.printStackTrace();
                    throw new MojoFailureException("SimpleDateFormat parse error");
                }
                Rf2File fo = new Rf2File(f2, revDate);
                listOfFiles.add(fo);
                getLog().info("    FOUND FILE : " + f2.getName() + " " + revDate);
            }
            
            for (Rf2File f : listOfFiles) {
                listOfRf2Dirs.add(f);
            }
        }
        return listOfRf2Dirs;
    }

    /**
     * Returns file date string in "yyyy-MM-dd 00:00:00" format.
     *
     * @param f
     * @return
     * @throws MojoFailureException
     */
    private String getFileRevDate(File f) throws MojoFailureException {
        int pos;
        // Check file name for date yyyyMMdd
        // EXAMPLE: ../net/nhs/uktc/ukde/sct1_relationships_uk_drug_20090401.txt
        pos = f.getName().length() - 12; // "yyyyMMdd.txt"
        String s1 = f.getName().substring(pos, pos + 8);
        // normalize date format
        s1 = s1.substring(0, 4) + "-" + s1.substring(4, 6) + "-" + s1.substring(6);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(s1);
        } catch (ParseException pe) {
            s1 = null;
        }

        // Check path for date yyyy-MM-dd
        // EXAMPLE: ../org/snomed/2003-01-31
        pos = f.getParent().length() - 10; // "yyyy-MM-dd"
        String s2 = f.getParent().substring(pos);
        try {
            dateFormat.parse(s2);
        } catch (ParseException pe) {
            s2 = null;
        }

        //
        if ((s1 != null) && (s2 != null)) {
            if (s1.equals(s2)) {
                return s1 + " 00:00:00";
            } else {
                throw new MojoFailureException("FAILED: file name date "
                        + "and directory name date do not agree. ");
            }
        } else if (s1 != null) {
            return s1 + " 00:00:00";
        } else if (s2 != null) {
            return s2 + " 00:00:00";
        } else {
            throw new MojoFailureException("FAILED: date can not be determined"
                    + " from either file name date or directory name date.");
        }
    }
    
    private static void listFilesRecursive(ArrayList<File> list, File root, String prefix,
            String postfix) {
        if (root.isFile()) {
            list.add(root);
            return;
        }
        File[] files = root.listFiles();
        Arrays.sort(files);
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName().toUpperCase();
            
            if (files[i].isFile() && name.endsWith(postfix.toUpperCase())
                    && name.contains(prefix.toUpperCase())) {
                list.add(files[i]);
            }
            if (files[i].isDirectory()) {
                listFilesRecursive(list, files[i], prefix, postfix);
            }
        }
    }
    
    private void writePlaceHolderConcepts(long[] expectedConcepts)
            throws MojoFailureException {
        
        try {
            String writeDirStr = targetDirectory + FILE_SEPARATOR + targetSubDir + FILE_SEPARATOR
                    + outputArfDir + FILE_SEPARATOR;
            boolean success = (new File(writeDirStr)).mkdirs();
            if (success) {
                getLog().info("OUTPUT DIRECTORY: " + writeDirStr);
            }
            
            Writer concepts;
            concepts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(
                    writeDirStr, "concepts_placeholders.txt")), "UTF-8"));
            Writer ids;
            ids = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(
                    writeDirStr, "ids_placeholders.txt")), "UTF-8"));
            
            for (long sid : expectedConcepts) {
                String conceptUuidStr = UuidT3Generator.fromSNOMED(sid).toString();
                concepts.append(conceptUuidStr); // concept uuid
                concepts.append("\t");
                concepts.append("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"); //status uuid
                concepts.append("\t");
                concepts.append("1"); // primitive, will be ignored later
                concepts.append("\t");
                concepts.append(effectiveDate); // effective date
                concepts.append("\t");
                concepts.append(pathUUID.toString()); //path uuid
                concepts.append("\t");
                concepts.append(uuidAuthor.toString()); //author uuid
                concepts.append("\t");
                concepts.append(moduleUUID.toString()); //module uuid
                concepts.append("\n");
                
                ids.append(conceptUuidStr); // concept uuid
                ids.append("\t");
                ids.append(IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid().toString()); //source uuid
                ids.append("\t");
                ids.append(Long.toString(sid)); //source id
                ids.append("\t");
                ids.append("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"); //status uuid
                ids.append("\t");
                ids.append(effectiveDate); // effective date
                ids.append("\t");
                ids.append(pathUUID.toString()); //path uuid
                ids.append("\t");
                ids.append(uuidAuthor.toString()); //author uuid
                ids.append("\t");
                ids.append(moduleUUID.toString()); //module uuid
                ids.append("\n");
            }
            
            concepts.close();
            ids.close();
            
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Error", e);
        }
    }
}
