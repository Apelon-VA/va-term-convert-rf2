/**
 * Copyright (c) 2009 International Health Terminology Standards Development Organisation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package gov.vha.isaac.rf2.convert.mojo;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.ihtsdo.otf.tcc.api.coordinate.Status;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.dto.TtkConceptChronicle;
import org.ihtsdo.otf.tcc.dto.component.attribute.TtkConceptAttributesChronicle;
import org.ihtsdo.otf.tcc.dto.component.attribute.TtkConceptAttributesRevision;
import org.ihtsdo.otf.tcc.dto.component.description.TtkDescriptionChronicle;
import org.ihtsdo.otf.tcc.dto.component.description.TtkDescriptionRevision;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifier;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifierLong;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifierString;
import org.ihtsdo.otf.tcc.dto.component.identifier.TtkIdentifierUuid;
import org.ihtsdo.otf.tcc.dto.component.refex.TtkRefexAbstractMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_boolean.TtkRefexBooleanMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_boolean.TtkRefexBooleanRevision;
import org.ihtsdo.otf.tcc.dto.component.refex.type_int.TtkRefexIntMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_int.TtkRefexIntRevision;
import org.ihtsdo.otf.tcc.dto.component.refex.type_string.TtkRefexStringMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_string.TtkRefexStringRevision;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid.TtkRefexUuidMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid.TtkRefexUuidRevision;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid_float.TtkRefexUuidFloatMemberChronicle;
import org.ihtsdo.otf.tcc.dto.component.refex.type_uuid_float.TtkRefexUuidFloatRevision;
import org.ihtsdo.otf.tcc.dto.component.relationship.TtkRelationshipChronicle;
import org.ihtsdo.otf.tcc.dto.component.relationship.TtkRelationshipRevision;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import gov.vha.isaac.ochre.util.UuidT5Generator;
import gov.vha.isaac.rf2.convert.Sct2_IdCompact;
import gov.vha.isaac.rf2.convert.sct1.Sct1Dir;
import gov.vha.isaac.rf2.convert.sct1.Sct1_ConRecord;
import gov.vha.isaac.rf2.convert.sct1.Sct1_DesRecord;
import gov.vha.isaac.rf2.convert.sct1.Sct1_IdRecord;
import gov.vha.isaac.rf2.convert.sct1.Sct1_RefSetRecord;
import gov.vha.isaac.rf2.convert.sct1.Sct1_RelDestRecord;
import gov.vha.isaac.rf2.convert.sct1.Sct1_RelRecord;

/**
 * <b>DESCRIPTION: </b><br>
 *
 * Sct1ArfToEConceptMojo is a maven mojo which converts SNOMED concepts, descriptions, stated
 * relationships and inferred relationships (Distribution Normal Form) RF1 release files to IHTSDO
 * Workbench versioned import eConcepts format. ARF formatted files can also be combined with the
 * SCT 1 files. <p> <b>Relationship uuids are generated based on the algorithm below. Note that
 * changing the role group in terms of relationship members or an non-mutable part any role group
 * member will cause that role group to be retired and a new role group to be created.</b>
 * <pre>
 * relGroupList = in concept1-type-concept2 sorted order
 *              {triplet_A(concept1-type-concept2),
 *               triplet_B(concept1-type-concept2),
 *               triplet_C(concept1-type-concept2), ...}
 *
 * relationship_id = createUUID_Type5(REL_ID_NAMESPACE_UUID_TYPE1,
 *                concept1_sctid_as_string +
 *                type_sctid_as_string +
 *                concept2_sctid_as_string +
 *                relGroupList_as_long_string);
 * </pre> <b>INPUTS:</b>
 * <pre>
 * &lt;targetSub&gt;       subdirname -- working sub directly under build directory
 * &lt;outputDirectory&gt; dirname    -- directory for output eConcepts files
 * &lt;dateStart&gt;       yyyy.mm.dd -- filter excludes files before startDate
 * &lt;dateStop&gt;        yyyy.mm.dd -- filter excludes files after stopDate
 * &lt;uuidModule&gt;      uuid -- moduleIdx UUID
 * &lt;uuidSnorocket&gt;   uuid -- Snorocket User UUID for defining inferred relationships
 * &lt;uuidUser&gt;        uuid -- User UUID if not a defining inferred relationship
 * &lt;rf2Mapping&gt;        true= maps preferred description type to synomym like RF2
 *                     false=retains strict RF1 description type
 * &lt;includeCTV3ID&gt;     true | false
 * &lt;includeSNOMEDRTID&gt; true | false
 *
 * &lt;sct1Dirs&gt;                  -- list of sct input directory items
 *    &lt;sct1Dir&gt;                -- detailed input directory item
 *       &lt;directoryName&gt; name  -- directory name
 *       &lt;mapSctIdInferredToStated&gt;   true | false
 *       &lt;keepHistoricalFromInferred&gt; true | false
 *       &lt;keepQualifierFromInferred&gt;  true | false
 *       &lt;keepAdditionalFromInferred&gt; true | false
 *       &lt;corePathUuid&gt;     uuid -- core pathIdx UUID
 *       &lt;inferredPathName&gt; name -- inferred pathIdx name
 *       &lt;statedPathName&gt;   name -- stated pathIdx name
 *
 * &lt;arfInputDirs&gt;
 *       &lt;param&gt;/cement/&lt;/param&gt;
 * </pre> The POM needs to specify mutually exclusive extensions in separate directories in the
 * array
 * <code>sctInputDirArray</code> parameter. Each directory entry will be parsed to locate SNOMED
 * formated text files in any sub-directories. <br> <br>
 *
 * Each SNOMED file should contain a version date in the file name
 * <code>"sct1_*yyyyMMdd.txt"</code>. If a valid date is not found in the file name then the parent
 * directory name will be checked for a date in the format
 * <code>"yyyy-MM-dd"</code>.
 *
 * Versioning is performed for the files under the SAME
 * <code>sctInputDirArray[a]</code> directory. Records of the same primary ids are compared in
 * historical sequence to other records of the same primary ids for all applicable files under
 * directory
 * <code>sctInputDirArray[a]</code>. <p>
 *
 * Set
 * <code>includeCTV3ID</code> and/or
 * <code>includeSNOMEDRTID</code> to true to have the corresponding CTV3 IDs and SNOMED RT IDs to be
 * included in
 * <code>ids.txt</code> output file. The default value is false to not include the CTV3 IDs and
 * SNOMED RT IDs. <p> <b>OUTPUTS:</b> EConcept jbin file. (default name: sctSiEConcept.jbin) <br>
 * <p> <b>REQUIRMENTS:</b><br>
 *
 * 1. RELEASE DATE must be in either the SNOMED file name. The preferred date format in
 * <code>yyyyMMdd</code>. <br> <br> 2. SNOMED EXTENSIONS must be mutually exclusive from SNOMED CORE
 * and each other; and, placed under separate
 * <code>sctInputDirArray</code> directories.<br> <br> 3. STATED & INFERRED. Stated relationship
 * files names must begin with "sct1_relationships_stated". Inferred relationship file names must
 * begin with "sct1_relationships_inferred". Relationship file names without "_stated" or
 * "_inferred" are not supported. <p> <b>PROCESSING:</b><br> Step #1. Versioning & Relationship
 * Generated IDs. Merge time series of releases into a versioned intermediate concept, description,
 * and relationship files. This step also adds an algorithmically computed relationship ids. Ids are
 * kept directly with each primary (concept, description & relationship) component. <br> <br> Step
 * #2. ARF files. Append arf files to sct binary records files.<br> <br> Step #3. Destination Rels.
 * Build file for destination rels. Non-required fields are dropped.<br> <br> Step #4. Match IDs.
 * Associate ids with each specific component.<br> <br> Step #5. Refset. Refset preparation.<br>
 * <br> Step #6. Sort. Sort into concept order for merging the prepared files to create eConcepts in
 * the next step.<br> <br> Step #7. Create EConcepts. Concurrently read pre-sorted concept,
 * description, source relationship and destination relationship files and creates eConcepts.<br>
 * <p> <b>NOTES:</b><br> <b>Records are NOT VERSIONED between files under DIFFERENT
 * <code>sctInputDirArray</code> directories. The versioned output from
 * <code>sctInputDirArray[a+1]</code> is appended to the versioned output from
 * <code>sctInputDirArray[a]</code>. </b><br>
 *
 * @userIdx Marc E. Campbell
 *
 */
@Mojo(name = "sct1-arf-to-econcepts", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class Sct1ArfToEConceptMojo extends AbstractMojo {
    
    private Logger LOG = LogManager.getLogger();

    private int countEConWritten;
    private int statCon;
    private int statDes;
    private int statRel;
    private int statRelDest;
    private int statRsByCon;
    private int statRsByRs;
    private int statRsBoolFromArf;
    private int statRsIntFromArf;
    private int statRsFloatFromArf;
    private int statRsConFromArf;
    private int statRsStrFromArf;
    private static final int IS_LESS = -1;
    private static final int IS_EQUAL = 0;
    private static final String FILE_SEPARATOR = File.separator;
    private static final int ooResetInterval = 100;
    // workaround to set stated relationship characteristic as STATED_RELATIONSHIP 
    // starts a integer 5 at beginning of import pipeline
    // integer 5 is replaced with STATED_RELATIONSHIP UUID at eConcept creation
    private static final int STATED_CHAR_WORKAROUND = 5;
    private static final String TAB_CHARACTER = "\t";
    
    
    /**
     * Location of the build directory.
     */
    @Parameter(required = true, defaultValue = "${project.build.directory}") 
    protected File targetDirectory;
    
    
    /**
     * Start date (inclusive)
     */
    @Parameter
    private String dateStart;
    private Date dateStartObj;
    /**
     * Stop date (inclusive)
     */
    @Parameter
    private String dateStop;
    private Date dateStopObj;

    /**
     * Applicable output sub directory under the targetDir directory.
     */
    @Parameter(required = true, defaultValue = "input-files") 
    protected String targetSubDir = "";
    /**
     * ARF Input Directories Array. The directory array parameter supported extensions via separate
     * directories in the array.
     */
    @Parameter(required = true, defaultValue = "generated-arf") 
    private String arfInputDir;
    /**
     * SCT Input Directories Array. The directory array parameter supported extensions via separate
     * directories in the array.
     *
     * Files under the SAME directory entry in the array will be versioned relative each other. Each
     * input directory in the array is treated as mutually exclusive to others directories in the
     * array.
     */
    @Parameter
    private Sct1Dir[] sct1Dirs;
    /**
     * If this contains anything, only convert paths which match one of the enclosed regex
     */
    @Parameter
    private String[] inputFilters;

    @Parameter(defaultValue = "false")
    private boolean useSctRelId;

    @Parameter(defaultValue = "false")
    private boolean rf2Mapping;

    /**
     * Directory used to output the econcept format files 
     */
    @Parameter(required = true, defaultValue="generated-artifacts")
    private String outputDirectory;
    
    @Parameter(required = true, defaultValue="SnomedCoreEConcepts.jbin")
    private String outputFileName;

    @Parameter(required = true, defaultValue="false")
    private boolean reportRootConcepts;

    /**
     * Module - defaults to snomed core
     */
    @Parameter
    private UUID uuidModule = IsaacMetadataAuxiliaryBinding.SNOMED_CT_CORE_MODULE.getPrimodialUuid();
    private static final int MODULE_DEFAULT_IDX = -1;

    public void setUuidModule(String uuidStr) {
        uuidModule = UUID.fromString(uuidStr);
    }

    @Parameter
    private UUID uuidUser = IsaacMetadataAuxiliaryBinding.USER.getPrimodialUuid();

    public void setUuidUser(String uuidStr) {
        uuidUser = UUID.fromString(uuidStr);
    }
    /**
     * Snorocket "User" UUID 
     */
    @Parameter
    private UUID uuidUserSnorocket = IsaacMetadataAuxiliaryBinding.IHTSDO_CLASSIFIER.getPrimodialUuid();
    
    private static final int USER_DEFAULT_IDX = 0;
    private static final int USER_SNOROCKET_IDX = 1;

    public void setUuidUserSnorocket(String uuidStr) {
        uuidUserSnorocket = UUID.fromString(uuidStr);
    }
    private String scratchDirectory = FILE_SEPARATOR + "tmp_steps";
    private static final String REL_ID_NAMESPACE_UUID_TYPE1 = "84fd0460-2270-11df-8a39-0800200c9a66";
//
    
    private boolean includeCTV3ID = false;
    private boolean includeSNOMEDRTID = false;
    
    private HashMap<UUID, Long> relUuidMap; // :yyy:
    private String fNameStep1Con;
    private String fNameStep1Desc;
    private String fNameStep1Rel;
    private String fNameStep1Ids;
    private String fNameStep2Refset;
    private String fNameStep3RelDest;
    private String fNameStep4Con;
    private String fNameStep4Desc;
    private String fNameStep4Rel;
    private String fNameStep6Con;
    private String fNameStep6Desc;
    private String fNameStep6Rel;
    private String fNameStep6RelDest;
    private String fNameStep5RsByCon; 
    private String fNameStep5RsByRs; 
    private String fNameStep7ECon;
    // UUIDs
    private static UUID uuidPathWbAux;
    private static String uuidPathWbAuxStr;
    private static UUID uuidDescPrefTerm;
    private static UUID uuidDescFullSpec;
    private static UUID uuidWbAuxIsa;
    UUID uuidStatedDescFs;
    UUID uuidStatedDescPt;
    UUID uuidStatedRel;
    UUID uuidInferredDescFs;
    UUID uuidInferredDescPt;
    UUID uuidInferredRel;
    private static UUID uuidPathSnomedInferred;
    private static String uuidPathSnomedInferredStr;
    private static UUID uuidPathSnomedStated;
    private static String uuidPathSnomedStatedStr;
    private static UUID uuidSourceSnomedLong;
    private static int uuidSourceSnomedIdx;
//    private static UUID uuidSourceCtv3;
//    private static UUID uuidSourceSnomedRt;
    private SimpleDateFormat arfSimpleDateFormatDash;
    private SimpleDateFormat arfSimpleDateFormatDot;
    private SimpleDateFormat arfSimpleDateFormat;
    private HashMap<Long, HashSet<Sct2_IdCompact>> additionalIds;

    private class ARFFile {

        File file;

        public ARFFile(File f) {
            this.file = f;
        }

        @Override
        public String toString() {
            return " :: " + file.getPath();
        }
    }

    private class SCTFile {

        File file;
        String revDate;
        Boolean isStated;
        Boolean hasStatedSctRelId;
        Boolean mapSctIdInferredToStated; // :DEPRECIATED: Cross map inferred id to stated.
        Boolean keepQualifier; // 1
        Boolean keepHistorical; // 2
        Boolean keepAdditional; // 3
        long zRevTime;
        int pathIdx;
        int pathInferredIdx;
        int pathStatedIdx;

        public SCTFile(File f, String wDir, String subDir, String d, Sct1Dir sctDir)
                throws ParseException {
            this.file = f;
            this.revDate = d; // yyyy-MM-dd 00:00:00 format

            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            this.zRevTime = df.parse(revDate).getTime();

            // PATHS
            this.pathIdx = lookupZPathIdx(sctDir.getWbPathUuidCore().toString());
            if (sctDir.getWbPathUuidInferred() != null) {
                this.pathInferredIdx = lookupZPathIdx(sctDir.getWbPathUuidInferred().toString());
            } else {
                this.pathInferredIdx = pathIdx; // DEFAULT TO CORE PATH
            }
            if (sctDir.getWbPathUuidStated() != null) {
                this.pathStatedIdx = lookupZPathIdx(sctDir.getWbPathUuidStated().toString());
            } else {
                this.pathStatedIdx = pathIdx; // DEFAULT TO CORE PATH
            }
            // NON-DEFINING RELATIONSHIPS FILTER
            this.keepQualifier = sctDir.getKeepQualifierFromInferred(); // 1
            this.keepHistorical = sctDir.getKeepHistoricalFromInferred(); // 2
            this.keepAdditional = sctDir.getKeepAdditionalFromInferred(); // 3

            // RELATIONSHIP SCT ID INFERRED TO STATED MAPPING
            boolean doCrossMap = false;
            boolean hasSnomedId = true;
            this.isStated = false;
            if (useSctRelId) {
                if (f.getName().toUpperCase().contains("STATED")) {
                    this.isStated = true;
                }
                doCrossMap = false;
                hasSnomedId = sctDir.isStatedSctRelIdPresent();
            } else {
                if (f.getName().toUpperCase().contains("STATED")) {
                    this.isStated = true;
                    if (sctDir.doMapSctIdInferredToStated()) {
                        doCrossMap = true;
                        hasSnomedId = false;
                    }
                } else if (f.getName().toUpperCase().contains("INFERRED")
                        && sctDir.doMapSctIdInferredToStated()) {
                    doCrossMap = true;
                    hasSnomedId = true;
                }
            }

            this.hasStatedSctRelId = hasSnomedId;
            this.mapSctIdInferredToStated = doCrossMap;

            // setup PATH UUID (puuid), hasStatedSctRelId, doCrossMap
            getLog().info("           " + f.getName() + " QUEUED");
        }

        @Override
        public String toString() {
            return file.getPath();
        }
    }
    // AUTHOR UUID LOOKUP
    private HashMap<String, Integer> zAuthorMap; // <UUID, index>
    private ArrayList<String> zAuthorList;
    private UUID[] zAuthorUuidArray;
    private int zAuthorIdxCounter;

    private int lookupZAuthorIdx(String authorIdStr) {
        Integer tmp = zAuthorMap.get(authorIdStr);
        if (tmp == null) {
            zAuthorIdxCounter++;
            zAuthorMap.put(authorIdStr, Integer.valueOf(zAuthorIdxCounter));
            zAuthorList.add(authorIdStr);
            return zAuthorIdxCounter;
        } else {
            return tmp.intValue();
        }
    }
    // PATH UUID LOOKUP
    private HashMap<String, Integer> zPathMap;
    private ArrayList<String> zPathList;
    private UUID[] zPathArray;
    private int zPathIdxCounter;

    private int lookupZPathIdx(String pathIdStr) {
        Integer tmp = zPathMap.get(pathIdStr);
        if (tmp == null) {
            zPathIdxCounter++;
            zPathMap.put(pathIdStr, Integer.valueOf(zPathIdxCounter));
            zPathList.add(pathIdStr);
            return zPathIdxCounter;
        } else {
            return tmp.intValue();
        }
    }
    // MODULE UUID LOOKUP
    private HashMap<String, Integer> zModuleMap;
    private ArrayList<String> zModuleList;
    private UUID[] zModuleArray;
    private int zModuleIdxCounter;

    private int lookupZModuleIdx(String moduleIdStr) {
        Integer tmp = zModuleMap.get(moduleIdStr);
        if (tmp == null) {
            zModuleIdxCounter++;
            zModuleMap.put(moduleIdStr, Integer.valueOf(zModuleIdxCounter));
            zModuleList.add(moduleIdStr);
            return zModuleIdxCounter;
        } else {
            return tmp.intValue();
        }
    }
    // SOURCE UUID LOOKUP
    private ArrayList<String> zSourceUuidList;
    private UUID[] zSourceUuidArray;

    // STATUS TYPE LOOKUP

    private int lookupZStatusUuidIdx(String statusUuidStr) {
        UUID status = UUID.fromString(statusUuidStr);
        if (status.equals(UUID.fromString("FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"))) {
            return -1; // Status for "place holder" concept
        }
        else if (status.equals(SnomedMetadataRf2.ACTIVE_VALUE_RF2.getPrimodialUuid())) {
            return 1;
        }
        else if (status.equals(SnomedMetadataRf2.INACTIVE_VALUE_RF2.getPrimodialUuid())) {
            return 0;
        }
        else {
            throw new RuntimeException("Bad assumption!");
        }
        
    }
    // DESCRIPTION TYPE LOOKUP
    private int lookupZDesTypeUuidIdx(String desTypeUuidStr) {
        UUID uuid = UUID.fromString(desTypeUuidStr);
        if (uuid.equals(IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getPrimodialUuid())) {
            return 0;
        }
        if (uuid.equals(IsaacMetadataAuxiliaryBinding.PREFERRED.getPrimodialUuid())) {
            return 1;
        }
        else if (uuid.equals(IsaacMetadataAuxiliaryBinding.SYNONYM.getPrimodialUuid())) {
            return 2;
        }
        else if (uuid.equals(IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getPrimodialUuid())) {
            return 3;
        }
        throw new RuntimeException("Unhandled desc type " + desTypeUuidStr);
    }
    // RELATIONSHIP CHARACTERISTIC LOOKUP
    private UUID[] zRelCharArray;
    private String[] zRelCharStrArray;

    private int lookupRelCharTypeIdx(String uuid) {
        int max = zRelCharArray.length;
        int idx = 0;
        while (idx < max) {
            if (uuid.equalsIgnoreCase(zRelCharStrArray[idx])) {
                return idx;
            }
            idx++;
        }

        // GROW ARRAYS
        max = max + 1;
        UUID[] tmpUuidArray = Arrays.copyOf(zRelCharArray, max);
        tmpUuidArray[idx] = UUID.fromString(uuid);
        zRelCharArray = tmpUuidArray;

        String[] tmpStrArray = Arrays.copyOf(zRelCharStrArray, max);
        tmpStrArray[idx] = uuid;
        zRelCharStrArray = tmpStrArray;
        return idx;
    }
    // RELATIONSHIP REFINIBILITY LOOKUP
    private UUID[] zRelRefArray;
    private String[] zRelRefStrArray;

    private int lookupRelRefTypeIdx(String uuid) {
        int max = zRelRefArray.length;
        int idx = 0;
        while (idx < max) {
            if (uuid.equalsIgnoreCase(zRelRefStrArray[idx])) {
                return idx;
            }
            idx++;
        }
        // GROW ARRAYS
        max = max + 1;
        UUID[] tmpUuidArray = Arrays.copyOf(zRelRefArray, max);
        tmpUuidArray[idx] = UUID.fromString(uuid);
        zRelRefArray = tmpUuidArray;

        String[] tmpStrArray = Arrays.copyOf(zRelRefStrArray, max);
        tmpStrArray[idx] = uuid;
        zRelRefStrArray = tmpStrArray;

        return idx;
    }

    // RELATIONSHIP ROLE TYPE LOOKUP
    private class RoleTypeEntry {

        long snomedId;
        String uuidStr;
        UUID uuid;

        public RoleTypeEntry(String uStr) {
            super();
            this.snomedId = Integer.MAX_VALUE;
            this.uuidStr = uStr;
            this.uuid = UUID.fromString(uStr);
        }

        public RoleTypeEntry(long snomedId) {
            super();
            this.snomedId = snomedId;
            this.uuid = UuidT3Generator.fromSNOMED(snomedId);
            this.uuidStr = uuid.toString();
        }
    }
    private List<RoleTypeEntry> zRoleTypeList;

    private int lookupRoleTypeIdxFromSnoId(long roleTypeSnoId) {
        int last = zRoleTypeList.size();
        for (int idx = 0; idx < last; idx++) {
            if (zRoleTypeList.get(idx).snomedId == roleTypeSnoId) {
                return idx;
            }
        }

        RoleTypeEntry tmp = new RoleTypeEntry(roleTypeSnoId);
        zRoleTypeList.add(tmp);
        return last;
    }

    private int lookupRoleTypeIdx(String uStr) {
        int last = zRoleTypeList.size();
        for (int idx = 0; idx < last; idx++) {
            if (zRoleTypeList.get(idx).uuidStr.equalsIgnoreCase(uStr)) {
                return idx;
            }
        }

        RoleTypeEntry tmp = new RoleTypeEntry(uStr);
        zRoleTypeList.add(tmp);
        return last;
    }

    // 
    private UUID lookupRoleType(int roleTypeIdx) {
        return zRoleTypeList.get(roleTypeIdx).uuid;
    }

    private class IdSrcSystemEntry {

        UUID srcSystemUuid;
        String srcSystemIdStr;

        public IdSrcSystemEntry(UUID srcSysUuid) {
            super();
            this.srcSystemUuid = srcSysUuid;
            this.srcSystemIdStr = srcSystemUuid.toString();
        }

        public IdSrcSystemEntry(String srcSysIdStr) {
            super();
            this.srcSystemIdStr = srcSysIdStr;
            this.srcSystemUuid = UUID.fromString(srcSystemIdStr);
        }
    }
    private List<IdSrcSystemEntry> zIdSrcSystemList;

    private int lookupSrcSystemIdx(String uuidStr) {
        int last = zIdSrcSystemList.size();
        for (int idx = 0; idx < last; idx++) {
            if (uuidStr.equalsIgnoreCase(zIdSrcSystemList.get(idx).srcSystemIdStr)) {
                return idx;
            }
        }

        // NOT FOUND IN LIST
        zIdSrcSystemList.add(new IdSrcSystemEntry(uuidStr));
        getLog().info(" ::: IMPORT DISCOVERED NOT-DECLARED ID SYSTEM = " + uuidStr);
        return last;
    }

    private UUID lookupSrcSystemUUID(int idx) {
        if (idx < zIdSrcSystemList.size()) {
            return zIdSrcSystemList.get(idx).srcSystemUuid;
        } else {
            return null;
        }
    }

    private void setupLookupPartA() throws MojoFailureException {
        // Relationship Role Types
        zRoleTypeList = new ArrayList<RoleTypeEntry>();

        try {
            // RELATIONSHIP CHARACTERISTIC
            zRelCharArray = new UUID[6];
            zRelCharArray[0] = UUID.fromString("a4c6bf72-8fb6-11db-b606-0800200c9a66");//defining
            zRelCharArray[1] = UUID.fromString("416ad0e4-b6bc-386c-900e-121c58b20f55");//qualifier
            zRelCharArray[2] = UUID.fromString("1d054ca3-2b32-3004-b7af-2701276059d5");//historical
            zRelCharArray[3] = UUID.fromString("66f4785e-92e9-3d1c-ae3b-f1632b52b111");//additional
            zRelCharArray[4] = UUID.fromString("f88e2a66-3a5b-3358-92f0-5b3f5e82b270");//characteristic type
            zRelCharArray[STATED_CHAR_WORKAROUND] = IsaacMetadataAuxiliaryBinding.STATED.getPrimodialUuid();

            // string lookup array
            zRelCharStrArray = new String[6];
            for (int idx = 0; idx < 6; idx++) {
                zRelCharStrArray[idx] = zRelCharArray[idx].toString();
            }

            // RELATIONSHIP REFINABILITY
            zRelRefArray = new UUID[3];
            zRelRefArray[0] = UUID.fromString("e4cde443-8fb6-11db-b606-0800200c9a66");  //not refinable
            zRelRefArray[1] = UUID.fromString("c3d997d3-b0a4-31f8-846f-03fa874f5479");  //optional
            zRelRefArray[2] = UUID.fromString("3f2cec85-be64-339e-ba99-4a75f53bc51c");  // mandatory
            // string lookup array
            zRelRefStrArray = new String[3];
            for (int idx = 0; idx < 3; idx++) {
                zRelRefStrArray[idx] = zRelRefArray[idx].toString();
            }

            // ID SOURCE SYSTEM
            zIdSrcSystemList = new ArrayList<IdSrcSystemEntry>();
            zIdSrcSystemList.add(new IdSrcSystemEntry(uuidSourceSnomedLong));
            uuidSourceSnomedIdx = 0;
//            zIdSrcSystemList.add(new IdSrcSystemEntry(uuidSourceCtv3, IdDataType.STRING));
//            zIdSrcSystemList.add(new IdSrcSystemEntry(uuidSourceSnomedRt, IdDataType.STRING));
//            zIdSrcSystemList.add(new IdSrcSystemEntry(ArchitectonicAuxiliary.Concept.ICD_9.getUids().iterator().next(), IdDataType.STRING));

        } catch (Exception e) {
            getLog().info(e);
            throw new MojoFailureException("FAILED: Sct1ArfToEConcept -- setupLookupPartA()");
        }

    }

    private void setupLookupPartB() throws MojoFailureException {
        zAuthorUuidArray = new UUID[zAuthorList.size()];
        int i = 0;
        for (String s : zAuthorList) {
            zAuthorUuidArray[i] = UUID.fromString(s);
            i++;
        }

        zPathArray = new UUID[zPathList.size()];
        i = 0;
        for (String s : zPathList) {
            zPathArray[i] = UUID.fromString(s);
            i++;
        }

        zModuleArray = new UUID[zModuleList.size()];
        i = 0;
        for (String s : zModuleList) {
            zModuleArray[i] = UUID.fromString(s);
            i++;
        }

        // SNOMED_INT ... :FYI: soft code in SctZ1ConRecord
        zSourceUuidArray = new UUID[zSourceUuidList.size()];
        i = 0;
        for (String s : zSourceUuidList) {
            zSourceUuidArray[i] = UUID.fromString(s);
            i++;
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        //           conceptsToWatchMap =
        //                    new HashMap<UUID, ConceptDescriptor>();
        //            if (conceptsToWatch != null) {
        //                for (ConceptDescriptor cd : conceptsToWatch) {
        //                    conceptsToWatchMap.put(UUID.fromString(cd.getUuid()), cd);
        //                }
        //            }

        getLog().info("::: BEGIN Sct1ArfToEConcept");

        // SHOW build directory from POM file
        String targetDir = targetDirectory.getAbsolutePath();
        getLog().info("    POM Target Directory: " + targetDir);

        // SHOW input sub directory from POM file
        if (!targetSubDir.equals("")) {
            targetSubDir = FILE_SEPARATOR + targetSubDir;
            getLog().info("    POM Target Sub Directory: " + targetSubDir);
        }

        // SHOW input directories from POM file
        if (sct1Dirs != null) {
            for (int i = 0; i < sct1Dirs.length; i++) {
                sct1Dirs[i].setDirectoryName(sct1Dirs[i].getDirectoryName().replace('/',
                        File.separatorChar));
                getLog().info("POM SCT Input Directory (" + i + ") = " + sct1Dirs[i].getDirectoryName());
                if (!sct1Dirs[i].getDirectoryName().startsWith(FILE_SEPARATOR)) {
                    sct1Dirs[i].setDirectoryName(FILE_SEPARATOR + sct1Dirs[i].getDirectoryName());
                }
            }
        }

        additionalIds = new HashMap<>();
        try {
            File additionalIdsFile = new File(targetDir + File.separatorChar + "input-files"+ File.separatorChar + File.separatorChar + arfInputDir + File.separatorChar + "additional.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(additionalIdsFile),"UTF-8"));
            int sctId = 0;
            int lsb = 1;
            int msb = 2;
            int time = 3;
            String line = br.readLine();
            while (line != null) {
                String[] parts = line.split("\t");
                if(additionalIds.containsKey(Long.parseLong(parts[sctId]))){
                    additionalIds.get(Long.parseLong(parts[sctId])).add(new Sct2_IdCompact(
                        Long.parseLong(parts[msb]), 
                        Long.parseLong(parts[lsb]), 
                        Long.parseLong(parts[sctId]), 
                        Long.parseLong(parts[time]))
                    );
                }else{
                    HashSet<Sct2_IdCompact> ids = new HashSet<>();
                    ids.add(new Sct2_IdCompact(
                        Long.parseLong(parts[msb]), 
                        Long.parseLong(parts[lsb]), 
                        Long.parseLong(parts[sctId]), 
                        Long.parseLong(parts[time])));
                    additionalIds.put(Long.parseLong(parts[sctId]),ids);
                }
                line = br.readLine();
            }
            br.close();
        } catch (FileNotFoundException ex) {
            LOG.error(ex);
        } catch (IOException ex) {
            LOG.error(ex);
        }
        arfInputDir = arfInputDir.replace('/', File.separatorChar);
        getLog().info("POM ARF Input Directory = " + arfInputDir);
        if (!arfInputDir.startsWith(FILE_SEPARATOR)) {
            arfInputDir = FILE_SEPARATOR + arfInputDir;
        }

        // SHOW input sub directory from POM file
        if (!outputDirectory.equals("")) {
            outputDirectory = FILE_SEPARATOR + outputDirectory;
            getLog().info("POM Output Directory: " + outputDirectory);
        }

        executeMojo(targetDir, targetSubDir, arfInputDir, sct1Dirs, outputDirectory,
                includeCTV3ID, includeSNOMEDRTID);
        getLog().info("::: END Sct1ArfToEConcept");
    }

    void executeMojo(String tDir, String tSubDir, String arfDir, Sct1Dir[] sctDirs,
            String outDir, boolean ctv3idTF, boolean snomedrtTF) throws MojoFailureException {

        // :DEBUG:TEST:
        statRsBoolFromArf = 0;
        statRsIntFromArf = 0;
        statRsFloatFromArf = 0;
        statRsConFromArf = 0;
        statRsStrFromArf = 0;

        getLog().info("::: RF2 Mapping: " + rf2Mapping);
        getLog().info("::: Target Directory: " + tDir);
        getLog().info("::: Target Sub Directory:     " + tSubDir);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.ss hh:mm:ss");
        if (dateStartObj != null) {
            getLog().info("::: Start date (inclusive) = " + sdf.format(dateStartObj));
        }
        if (dateStopObj != null) {
            getLog().info(":::  Stop date (inclusive) = " + sdf.format(dateStopObj));
        }
        if (sctDirs != null) {
            for (int i = 0; i < sctDirs.length; i++) {
                getLog().info("::: SCT Input Directory (" + i + ") = " + sctDirs[i].getDirectoryName());
                getLog().info(
                        ":::     UUID Core:     " + sctDirs[i].getWbPathUuidCore() + " : "
                        + sctDirs[i].getWbPathUuidCoreFromName());
                getLog().info(
                        ":::     UUID Stated:   " + sctDirs[i].getWbPathUuidStated() + " : "
                        + sctDirs[i].getWbPathUuidStatedFromName());
                getLog().info(
                        ":::     UUID Inferred: " + sctDirs[i].getWbPathUuidInferred() + " : "
                        + sctDirs[i].getWbPathUuidInferredFromName());
                getLog().info(
                        ":::     Keep Qualifier Rels from _inferred_ file:  "
                        + sctDirs[i].getKeepQualifierFromInferred());
                getLog().info(
                        ":::     Keep Historical Rels from _inferred_ file: "
                        + sctDirs[i].getKeepHistoricalFromInferred());
                getLog().info(
                        ":::     Keep Additional Rels from _inferred_ file: "
                        + sctDirs[i].getKeepAdditionalFromInferred());
                getLog().info(
                        ":::     Map SCT REL IDs from inferred to stated: "
                        + sctDirs[i].doMapSctIdInferredToStated());
            }
        }
        getLog().info("::: ARF Input Directory = " + arfDir);
        getLog().info("::: Output Directory:  " + outDir);

        fNameStep1Con = tDir + scratchDirectory + FILE_SEPARATOR + "step1_concepts.ser";
        fNameStep1Rel = tDir + scratchDirectory + FILE_SEPARATOR + "step1_relationships.ser";
        fNameStep1Desc = tDir + scratchDirectory + FILE_SEPARATOR + "step1_descriptions.ser";
        fNameStep1Ids = tDir + scratchDirectory + FILE_SEPARATOR + "step1_ids.ser";

        fNameStep2Refset = tDir + scratchDirectory + FILE_SEPARATOR + "step2_refset.ser";

        fNameStep3RelDest = tDir + scratchDirectory + FILE_SEPARATOR + "step3_rel_dest.ser";

        fNameStep4Con = tDir + scratchDirectory + FILE_SEPARATOR + "step4_concepts.ser";
        fNameStep4Desc = tDir + scratchDirectory + FILE_SEPARATOR + "step4_descriptions.ser";
        fNameStep4Rel = tDir + scratchDirectory + FILE_SEPARATOR + "step4_relationships.ser";

        fNameStep5RsByCon = tDir + scratchDirectory + FILE_SEPARATOR + "step5_refset_by_con.ser";
        fNameStep5RsByRs = tDir + scratchDirectory + FILE_SEPARATOR + "step5_refet_by_refset.ser";

        fNameStep6Con = tDir + scratchDirectory + FILE_SEPARATOR + "step6_concepts.ser";
        fNameStep6Desc = tDir + scratchDirectory + FILE_SEPARATOR + "step6_descriptions.ser";
        fNameStep6Rel = tDir + scratchDirectory + FILE_SEPARATOR + "step6_relationships.ser";
        fNameStep6RelDest = tDir + scratchDirectory + FILE_SEPARATOR + "step6_rel_dest.ser";

        fNameStep7ECon = tDir + outDir + FILE_SEPARATOR + outputFileName;
        getLog().info("::: Output File:  " + outputFileName);

        zAuthorMap = new HashMap<String, Integer>();
        zAuthorList = new ArrayList<String>();
        zAuthorIdxCounter = -1;

        zPathMap = new HashMap<String, Integer>();
        zPathList = new ArrayList<String>();
        zPathIdxCounter = -1;

        zModuleMap = new HashMap<String, Integer>();
        zModuleList = new ArrayList<String>();
        zModuleIdxCounter = -1;

        zSourceUuidList = new ArrayList<String>();

        setupUuids();

        // Setup target (build) directory
        getLog().info("    Target Build Directory: " + tDir);

        arfSimpleDateFormatDash = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        arfSimpleDateFormatDot = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        arfSimpleDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");

        ObjectOutputStream oosCon = null;
        ObjectOutputStream oosDes = null;
        ObjectOutputStream oosRel = null;
        ObjectOutputStream oosIds = null;
        ObjectOutputStream oosRefSet = null;
        // SETUP OUTPUT directory
        try {
            // Create multiple directories
            boolean success = (new File(tDir + outDir)).mkdirs();
            if (success) {
                getLog().info("OUTPUT DIRECTORY: " + tDir + outDir);
            }

            String tmpDir = scratchDirectory;
            success = (new File(tDir + tmpDir)).mkdirs();
            if (success) {
                getLog().info("SCRATCH DIRECTORY: " + tDir + tmpDir);
            }

            // SETUP CONCEPTS OUTPUT FILE
            oosCon = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep1Con)));
            getLog().info("Step 1 CONCEPTS OUTPUT: " + fNameStep1Con);

            // SETUP DESCRIPTIONS OUTPUT FILE
            oosDes = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep1Desc)));
            getLog().info("Step 1 DESCRIPTIONS OUTPUT: " + fNameStep1Desc);

            // SETUP RELATIONSHIPS OUTPUT FILE
            oosRel = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep1Rel)));
            getLog().info("RELATIONSHIPS Step 1 OUTPUT: " + fNameStep1Rel);

            // SETUP IDS OUTPUT FILE
            oosIds = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep1Ids)));
            getLog().info("IDS Step 1 OUTPUT: " + fNameStep1Ids);

            // SETUP REFSET OUTPUT FILE
            oosRefSet = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep2Refset)));
            getLog().info("REFSET Step 2 OUTPUT: " + fNameStep2Refset);

            setupLookupPartA();

            // STEP #1. Convert to versioned binary objects file.  
            // Also computes algorithmic relationship uuid.
            if (sctDirs != null) {
                executeMojoStep1(tDir, tSubDir, sctDirs, ctv3idTF, snomedrtTF, oosCon, oosDes, oosRel,
                        oosIds);
                System.gc();
            }

            // STEP #2. Convert arf files to versioned binary objects file.
            // Uses existing relationship uuid
            // Appends to binary stream created in Step 1.
            executeMojoStep2(tDir, tSubDir, arfDir, oosCon, oosDes, oosRel, oosIds, oosRefSet);

            // stateSave(wDir);
            oosCon.close();
            oosDes.close();
            oosRel.close();
            oosIds.close();
            oosRefSet.close();

            // stateRestore(wDir);
            setupLookupPartB();

            // STEP #3. Gather destination relationship lists
            executeMojoStep3();
            System.gc();

            // STEP #4. Add IDs to components.
            executeMojoStep4_MatchIds();
            System.gc();

            // STEP #5. Add IDs to components.
            executeMojoStep5();

            // Step #6. Sort files to concept order for next stage
            executeMojoStep6();

            // STEP #7. Convert to EConcepts
            executeMojoStep7();

        } catch (Exception e) { // Catch exception if any
            getLog().info("Sct1ArfToEConceptsMojo sct1-arf-to-econcepts Error");
            throw new MojoFailureException("Error", e);
        }
    }

    private void executeMojoStep1(String wDir, String subDir, Sct1Dir[] inDirs, boolean ctv3idTF,
            boolean snomedrtTF, ObjectOutputStream oosCon, ObjectOutputStream oosDes,
            ObjectOutputStream oosRel, ObjectOutputStream oosIds) throws MojoFailureException {
        getLog().info("*** Sct1ArfToEConcept STEP #1 BEGIN SCT1 PROCESSING ***");
        long start = System.currentTimeMillis();

        // PROCESS SNOMED FILES
        try {
            // SETUP CONCEPTS INPUT SCTFile ArrayList
            List<List<SCTFile>> listOfCDirs = getSctFiles(wDir, subDir, inDirs, "concept", ".txt");
            processConceptsFiles(wDir, listOfCDirs, ctv3idTF, snomedrtTF, oosCon);
            listOfCDirs = null;
            System.gc();
        } catch (Exception e1) {
            getLog().info("FAILED: processConceptsFiles()");
            getLog().info(e1);
            throw new MojoFailureException("FAILED: processConceptsFiles()", e1);
        }

        try {
            // SETUP DESCRIPTIONS INPUT SCTFile ArrayList
            List<List<SCTFile>> listOfDDirs = getSctFiles(wDir, subDir, inDirs, "descriptions",
                    ".txt");
            processDescriptionsFiles(wDir, listOfDDirs, oosDes);
            listOfDDirs = null;
            System.gc();
        } catch (Exception e1) {
            getLog().info("FAILED: processDescriptionsFiles()");
            getLog().info(e1);
            throw new MojoFailureException("FAILED: processDescriptionsFiles()", e1);
        }

        // 3,254,249 from 2002.07 through 2010.01 
        relUuidMap = new HashMap<UUID, Long>(); // :yyy:

        // SETUP INFERRED RELATIONSHIPS INPUT SCTFile ArrayList
        List<List<SCTFile>> listOfRiDirs = getSctFiles(wDir, subDir, inDirs,
                "relationships_inferred", ".txt");

        // SETUP STATED RELATIONSHIPS INPUT SCTFile ArrayList
        List<List<SCTFile>> listOfRsDirs = getSctFiles(wDir, subDir, inDirs,
                "relationships_stated", ".txt");
        try {
            getLog().info("START RELATIONSHIPS PROCESSING...");

            // SETUP RELATIONSHIPS EXCEPTION REPORT FILE
            String erFileName = wDir + scratchDirectory + FILE_SEPARATOR
                    + "relationships_report.txt";
            BufferedWriter erw;
            erw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(erFileName),
                    "UTF-8"));
            getLog().info("RELATIONSHIPS Exceptions Report OUTPUT: " + erFileName);

            processRelationshipsFiles(wDir, listOfRiDirs, oosRel, oosIds, erw, USER_SNOROCKET_IDX);
            processRelationshipsFiles(wDir, listOfRsDirs, oosRel, oosIds, erw, USER_DEFAULT_IDX);

            erw.close(); // Need to be sure to the close file!
        } catch (Exception e1) {
            getLog().info("FAILED: processRelationshipsFiles()");
            getLog().info(e1);
            throw new MojoFailureException("FAILED: processRelationshipsFiles()", e1);
        }

        relUuidMap = null; // memory not needed any more.
        System.gc();
        getLog().info(
                "*** VERSIONING TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept STEP #1  SCT1 PROCESSING COMPLETE ***\r\n");
    }

    private void executeMojoStep2(String wDir, String subDir, String arfDir,
            ObjectOutputStream oosCon, ObjectOutputStream oosDes, ObjectOutputStream oosRel,
            ObjectOutputStream oosIds, ObjectOutputStream oosRefSet) {
        getLog().info("*** Sct1ArfToEConcept STEP #2 BEGINNING - INGEST ARF ***");
        long start = System.currentTimeMillis();

        try {

            // PROCESS CONCEPT ARF FILES
            List<List<ARFFile>> listOfCDirs = getArfFiles(wDir, subDir, arfDir, "concepts", ".txt");
            processArfConFiles(wDir, listOfCDirs, oosCon);
            listOfCDirs = null;
            System.gc();

            // PROCESS DESCRIPTION ARF FILES
            List<List<ARFFile>> listOfDDirs = getArfFiles(wDir, subDir, arfDir, "descriptions",
                    ".txt");
            processArfDesFiles(wDir, listOfDDirs, oosDes);
            listOfDDirs = null;
            System.gc();

            // PROCESS RELATIONSHIP ARF FILES
            List<List<ARFFile>> listOfRDirs = getArfFiles(wDir, subDir, arfDir, "relationships",
                    ".txt");
            processArfRelFiles(wDir, listOfRDirs, oosRel);
            listOfRDirs = null;
            System.gc();

            // PROCESS IDS ARF FILES
            List<List<ARFFile>> listOfIDirs = getArfFiles(wDir, subDir, arfDir, "ids", ".txt");
            processArfIdsFiles(wDir, listOfIDirs, oosIds);
            listOfIDirs = null;
            System.gc();

            // PROCESS REFSET BOOLEAN FILES
            List<List<ARFFile>> listOfRsBoolDirs = getArfFiles(wDir, subDir, arfDir, "boolean",
                    ".refset");
            processArfRsBoolFiles(wDir, listOfRsBoolDirs, oosRefSet);
            listOfRsBoolDirs = null;
            System.gc();

            // PROCESS REFSET CONCEPT FILES
            List<List<ARFFile>> listOfRsConDirs = getArfFiles(wDir, subDir, arfDir, "concept",
                    ".refset");
            processArfRsConFiles(wDir, listOfRsConDirs, oosRefSet);
            listOfRsConDirs = null;
            System.gc();

            // PROCESS REFSET INTEGER FILES
            List<List<ARFFile>> listOfRsIntDirs = getArfFiles(wDir, subDir, arfDir, "integer",
                    ".refset");
            processArfRsIntFiles(wDir, listOfRsIntDirs, oosRefSet);
            listOfRsIntDirs = null;
            System.gc();
            
            // PROCESS REFSET FLOAT FILES
            List<List<ARFFile>> listOfRsFloatDirs = getArfFiles(wDir, subDir, arfDir, "float",
                    ".refset");
            processArfRsFloatFiles(wDir, listOfRsFloatDirs, oosRefSet);
            listOfRsFloatDirs = null;
            System.gc();

            // PROCESS REFSET STRING FILES
            List<List<ARFFile>> listOfRsStrDirs = getArfFiles(wDir, subDir, arfDir, "_string_",
                    ".refset");
            processArfRsStrFiles(wDir, listOfRsStrDirs, oosRefSet);
            listOfRsStrDirs = null;
            System.gc();
            
            // PROCESS REFSET STRING STRING FILES
            List<List<ARFFile>> listOfRsStrStrDirs = getArfFiles(wDir, subDir, arfDir, "stringstring",
                    ".refset");
            processArfRsStrStrFiles(wDir, listOfRsStrStrDirs, oosRefSet);
            listOfRsStrDirs = null;
            System.gc();

            getLog().info(
                    "\r\nstatRsBoolFromArf= " + statRsBoolFromArf + "\r\nstatRsIntFromArf= "
                    + statRsIntFromArf + "\r\nstatRsFloatFromArf= "
                    + statRsFloatFromArf+ "\r\nstatRsConFromArf= " + statRsConFromArf
                    + "\r\nstatRsStrFromArf= " + statRsStrFromArf);

        } catch (MojoFailureException e1) {
            getLog().info("FAILED: processArfIdFiles()");
            getLog().info(e1);
        } catch (IOException e) {
            getLog().info(e);
        }

        getLog().info(
                "*** ARF TO BINARY OBJECT TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept STEP #2 COMPLETED - INGEST ARF ***\r\n");
    }

    private void processArfConFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfConFile(f.file, oos);
            }
        }
    }

    private void parseArfConFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int CONCEPT_UUID = 0;
        int CONCEPT_STATUS = 1;
        int ISPRIMITIVE = 2; // primitive
        int EFFECTIVE_DATE = 3; // Effective Date
        int PATH_UUID = 4; // Path UUID
        int AUTHOR_UUID = 5; // Author UUID
        int MODULE_UUID = 6; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // Concept UUID
            UUID uuidCon = UUID.fromString(line[CONCEPT_UUID]);
            // Status
            int conceptStatus = lookupZStatusUuidIdx(line[CONCEPT_STATUS]);
            // Primitive
            String isPrimitiveStr = line[ISPRIMITIVE];
            int isPrimitive = 0;
            if (isPrimitiveStr.startsWith("1") || isPrimitiveStr.startsWith("t")
                    || isPrimitiveStr.startsWith("T")) {
                isPrimitive = 1;
            }
            // Effective Date
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // Path UUID
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // AUTHOR_UUID = 5;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 6;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_ConRecord tmpConRec = new Sct1_ConRecord(uuidCon, conceptStatus, isPrimitive,
                    revTime, pathIdx, authorIdx, moduleIdx);

            oos.writeUnshared(tmpConRec);
        }
        br.close();
    }

    private void processArfDesFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfDesFile(f.file, oos);
            }
        }
    }

    private void parseArfDesFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int DESCRIPTION_UUID = 0;
        int STATUS_UUID = 1;
        int CONCEPT_UUID = 2;
        int TERM_STRING = 3;
        int CAPITALIZATION_STATUS_INT = 4;
        int DESCRIPTION_TYPE_UUID = 5;
        int LANGUAGE_CODE_STR = 6;
        int EFFECTIVE_DATE = 7;
        int PATH_UUID = 8;
        int AUTHOR_UUID = 9; // Author UUID
        int MODULE_UUID = 10; // Module UUID

        int RF1_UNSPECIFIED = 0;
        int RF1_PREFERRED = 1;
        int RF1_SYNOMYM = 2;

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // DESCRIPTION_UUID = 0;
            UUID uuidDes = UUID.fromString(line[DESCRIPTION_UUID]);
            // STATUS_UUID = 1;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // CONCEPT_UUID = 2;
            UUID uuidCon = UUID.fromString(line[CONCEPT_UUID]);
            // TERM_STRING = 3;
            String termStr = line[TERM_STRING];
            // CAPITALIZATION_STATUS = 4;
            // int capitalization = Integer.parseInt(line[CAPITALIZATION_STATUS_INT]);
            String capitalizationStr = line[CAPITALIZATION_STATUS_INT];
            int capitalization = 0;
            if (capitalizationStr.startsWith("1") || capitalizationStr.startsWith("t")
                    || capitalizationStr.startsWith("T")) {
                capitalization = 1;
            }

            // DESCRIPTION_TYPE = 5;
            int descriptionType = lookupZDesTypeUuidIdx(line[DESCRIPTION_TYPE_UUID]);
            if (rf2Mapping == true
                    && (descriptionType == RF1_UNSPECIFIED || descriptionType == RF1_PREFERRED)) {
                descriptionType = RF1_SYNOMYM;
            }
            // LANGUAGE_CODE = 6;
            String langCodeStr = line[LANGUAGE_CODE_STR];
            // EFFFECTIVE_DATE = 7;
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 8;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // AUTHOR_UUID = 9;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 10;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_DesRecord tmpDesRec = new Sct1_DesRecord(uuidDes, status, uuidCon, termStr,
                    capitalization, descriptionType, langCodeStr,
                    revTime, pathIdx, authorIdx, moduleIdx);

            // :DEBUG:
            //            if (debug)
            //            if (tmpDesRec.conUuidMsb == -8120194779924901686L
            //                    && tmpDesRec.conUuidLsb == -6989461898667750587L) {
            //                getLog().info(":DEBUG: ################ " + tmpDesRec.conSnoId);
            //                getLog().info(":DEBUG: ... conSnoId   = " + tmpDesRec.conSnoId);
            //                getLog().info(":DEBUG: ... conUuidLsb = " + tmpDesRec.conUuidLsb);
            //                getLog().info(":DEBUG: ... conUuidMsb = " + tmpDesRec.conUuidMsb);
            //                getLog().info(":DEBUG: ... termText   = " + tmpDesRec.termText);
            //            }
            // :DEBUG:END 

            try {
                oos.writeUnshared(tmpDesRec);
            } catch (Exception e) {
                getLog().info(e);
            }
        }

        br.close();
    }

    private void processArfRelFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRelFile(f.file, oos);
            }
        }
    }

    private void parseArfRelFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int RELATIONSHIP_UUID = 0;
        int STATUS_UUID = 1;
        int C1_UUID = 2;
        int ROLE_TYPE_UUID = 3;
        int C2_UUID = 4;
        int CHARACTERISTIC_UUID = 5;
        int REFINABILITY_UUID = 6;
        int GROUP = 7;
        int EFFECTIVE_DATE = 8; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 9;
        int AUTHOR_UUID = 10; // Path UUID
        int MODULE_UUID = 11; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // RELATIONSHIP_UUID = 0;
            UUID uuidRelId = UUID.fromString(line[RELATIONSHIP_UUID]);
            // STATUS_UUID = 1;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // C1_UUID = 2;
            UUID uuidC1 = UUID.fromString(line[C1_UUID]);
            // ROLE_TYPE_UUID = 3;
            int roleTypeIdx = lookupRoleTypeIdx(line[ROLE_TYPE_UUID]);
            // C2_UUID = 4;
            UUID uuidC2 = UUID.fromString(line[C2_UUID]);
            // CHARACTERISTIC_UUID = 5;
            int characteristic = lookupRelCharTypeIdx(line[CHARACTERISTIC_UUID]);
            // REFINABILITY_UUID = 6;
            int refinability = lookupRelRefTypeIdx(line[REFINABILITY_UUID]);
            // GROUP = 7;
            int group = Integer.parseInt(line[GROUP]);
            // EFFECTIVE_DATE = 8;  // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 9;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // AUTHOR_UUID = 10;
            int userIdx = USER_DEFAULT_IDX;
            if (line.length > AUTHOR_UUID) {
                if (line[AUTHOR_UUID].equalsIgnoreCase(uuidUserSnorocket.toString())) {
                    userIdx = USER_SNOROCKET_IDX;
                }
            }
            // MODULE_UUID = 11;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RelRecord tmpRelRec = new Sct1_RelRecord(uuidRelId, status, uuidC1, roleTypeIdx,
                    uuidC2, characteristic, refinability, group,
                    revTime, pathIdx, userIdx, moduleIdx);

            oos.writeUnshared(tmpRelRec);
        }

        br.close();
    }

    private void processArfIdsFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfIdsFile(f.file, oos);
            }
        }
    }

    private void parseArfIdsFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int PRIMARY_UUID = 0;
        int SOURCE_SYSTEM_UUID = 1;
        int ID_FROM_SOURCE_SYSTEM = 2;
        int STATUS_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int AUTHOR_UUID = 6; // Author UUID
        int MODULE_UUID = 7; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // PRIMARY_UUID = 0;
            UUID uuidPrimaryId = UUID.fromString(line[PRIMARY_UUID]);
            // SOURCE_SYSTEM_UUID = 1;
            int sourceSystemIdx = lookupSrcSystemIdx(line[SOURCE_SYSTEM_UUID]);
            // ID_FROM_SOURCE_SYSTEM = 2;
            String idFromSourceSystem = line[ID_FROM_SOURCE_SYSTEM];
            // STATUS_UUID = 3;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // AUTHOR_UUID = 6;
            int authorIdx = USER_DEFAULT_IDX;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 7;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_IdRecord tmpIdRec = new Sct1_IdRecord(uuidPrimaryId, sourceSystemIdx,
                    idFromSourceSystem, status,
                    revTime, pathIdx, authorIdx, moduleIdx);

            oos.writeUnshared(tmpIdRec);
        }

        br.close();
    }

    private void processArfRsBoolFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsBoolFile(f.file, oos);
            }
        }
    }

    private void parseArfRsBoolFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int EXT_VALUE_UUID = 6;
        int AUTHOR_UUID = 7; // Author UUID
        int MODULE_UUID = 8; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // EXT_VALUE_UUID = 6;
            boolean vBool = false;
            if (line[EXT_VALUE_UUID].charAt(0) == 't' || line[EXT_VALUE_UUID].charAt(0) == 'T') {
                vBool = true;
            }
            // AUTHOR_UUID = 7;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 8;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            // :DEBUG:
            //            if (uuidComponent.equals(UUID.fromString("7c57f6b4-4a63-52ad-b762-73acc15f23de"))) 
            //                getLog().info("FOUND IT");

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    vBool);

            statRsBoolFromArf++;
            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }

    private void processArfRsConFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos) throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsConFile(f.file, oos);
            }
        }
    }

    private void parseArfRsConFile(File f, ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int EXT_VALUE_UUID = 6;
        int AUTHOR_UUID = 7; // Author UUID
        int MODULE_UUID = 8; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // EXT_VALUE_UUID = 6;
            UUID uuidConExt = UUID.fromString(line[EXT_VALUE_UUID]);
            // AUTHOR_UUID = 7;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 8;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    uuidConExt);

            statRsConFromArf++;
            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }

    private void processArfRsIntFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsIntFile(f.file, oos);
            }
        }
    }

    private void parseArfRsIntFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int EXT_VALUE_UUID = 6;
        int AUTHOR_UUID = 7; // Author UUID
        int MODULE_UUID = 8; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // CONCEPT_EXT_VALUE_UUID = 6;
            int vInt = Integer.valueOf(line[EXT_VALUE_UUID]);
            // AUTHOR_UUID = 7;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 8;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    vInt);

            statRsIntFromArf++;
            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }
    private void processArfRsFloatFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsFloatFile(f.file, oos);
            }
        }
    }

    private void parseArfRsFloatFile(File f, ObjectOutputStream oos) throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int CONCEPT_VALUE = 6;
        int EXT_VALUE = 7;
        int AUTHOR_UUID = 8; // Author UUID
        int MODULE_UUID = 9; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // CONCEPT_VALUE = 6
            UUID vConcept = UUID.fromString(line[CONCEPT_VALUE]);
            // CONCEPT_EXT_VALUE_UUID = 7;
            float vFloat = Float.valueOf(line[EXT_VALUE]);
            // AUTHOR_UUID = 8;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 9;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    vConcept, vFloat);

            statRsFloatFromArf++;
            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }

    private void processArfRsStrFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsStrFile(f.file, oos);
            }
        }
    }

    private void parseArfRsStrFile(File f, ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int EXT_VALUE_UUID = 6;
        int AUTHOR_UUID = 7; // Author UUID
        int MODULE_UUID = 8; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // CONCEPT_EXT_VALUE_UUID = 6;
            String vStr = line[EXT_VALUE_UUID];
            // AUTHOR_UUID = 7;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 8;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    vStr);

            statRsStrFromArf++;
            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }
    
    private void processArfRsStrStrFiles(String wDir, List<List<ARFFile>> listOfDirs,
            ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        for (List<ARFFile> laf : listOfDirs) {
            for (ARFFile f : laf) {
                parseArfRsStrStrFile(f.file, oos);
            }
        }
    }

    private void parseArfRsStrStrFile(File f, ObjectOutputStream oos)
            throws IOException, MojoFailureException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(f),
                "UTF-8"));

        int REFSEST_UUID = 0;
        int MEMBER_UUID = 1;
        int STATUS_UUID = 2;
        int REFERENCED_COMPONENT_UUID = 3;
        int EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        int PATH_UUID = 5;
        int STRING1 = 6;
        int STRING2 = 7;
        int AUTHOR_UUID = 8; // Author UUID
        int MODULE_UUID = 9; // Module UUID

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // REFSEST_UUID = 0;
            UUID uuidRefset = UUID.fromString(line[REFSEST_UUID]);
            // MEMBER_UUID = 1;
            UUID uuidMember = UUID.fromString(line[MEMBER_UUID]);
            // STATUS_UUID = 2;
            int status = lookupZStatusUuidIdx(line[STATUS_UUID]);
            // REFERENCED_COMPONENT_UUID = 3;
            UUID uuidComponent = UUID.fromString(line[REFERENCED_COMPONENT_UUID]);
            // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
            long revTime = convertDateStrToTime(line[EFFECTIVE_DATE]);
            // PATH_UUID = 5;
            int pathIdx = lookupZPathIdx(line[PATH_UUID]);
            // STRING1 = 6;
            String vStr1 = line[STRING1];
            // STRING2 = 7;
            String vStr2 = line[STRING2];
            // AUTHOR_UUID = 7;
            int authorIdx = -1;
            if (line.length > AUTHOR_UUID) {
                authorIdx = lookupZAuthorIdx(line[AUTHOR_UUID]);
            }
            // MODULE_UUID = 8;
            int moduleIdx = -1;
            if (line.length > MODULE_UUID) {
                moduleIdx = lookupZModuleIdx(line[MODULE_UUID]);
            }

            Sct1_RefSetRecord tmpRsRec = new Sct1_RefSetRecord(uuidRefset, uuidMember,
                    uuidComponent, status,
                    revTime, pathIdx, authorIdx, moduleIdx,
                    vStr1,vStr2);

            oos.writeUnshared(tmpRsRec);
        }

        br.close();
    }

    private void executeMojoStep3()
            throws MojoFailureException {
        getLog().info("*** Sct1ArfToEConcept STEP #3 BEGINNING -- GATHER DESTINATION RELs ***");
        long start = System.currentTimeMillis();

        try {
            // read in relationships, sort by C2-ROLETYPE
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(fNameStep1Rel)));
            ArrayList<Sct1_RelRecord> aRel = new ArrayList<Sct1_RelRecord>();

            int count = 0;
            Object obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_RelRecord) {
                        aRel.add((Sct1_RelRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" relationship count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" relationship count = " + count + " @EOF\r\n");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException -- Step 2 reading file");
            }
            ois.close();
            getLog().info(" relationship count = " + count + "\r\n");

            // SORT BY [C2-RoleType]
            Comparator<Sct1_RelRecord> compRelDest = new Comparator<Sct1_RelRecord>() {
                @Override
                public int compare(Sct1_RelRecord o1, Sct1_RelRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // C2
                    if (o1.getC2UuidMsb() > o2.getC2UuidMsb()) {
                        return thisMore;
                    } else if (o1.getC2UuidMsb() < o2.getC2UuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getC2UuidLsb() > o2.getC2UuidLsb()) {
                            return thisMore;
                        } else if (o1.getC2UuidLsb() < o2.getC2UuidLsb()) {
                            return thisLess;
                        } else {
                            // ROLE TYPE
                            if (o1.getRelUuidMsb() > o2.getRelUuidMsb()) {
                                return thisMore;
                            } else if (o1.getRelUuidMsb() < o2.getRelUuidMsb()) {
                                return thisLess;
                            } else {
                                if (o1.getRelUuidLsb() > o2.getRelUuidLsb()) {
                                    return thisMore;
                                } else if (o1.getRelUuidLsb() < o2.getRelUuidLsb()) {
                                    return thisLess;
                                } else {
                                    return 0; // EQUAL
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aRel, compRelDest);

            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fNameStep3RelDest)));
            long lastRelMsb = Long.MIN_VALUE;
            long lastRelLsb = Long.MIN_VALUE;
            for (Sct1_RelRecord r : aRel) {
                if (r.getRelUuidMsb() != lastRelMsb || r.getRelUuidLsb() != lastRelLsb) {
                    oos.writeUnshared(new Sct1_RelDestRecord(r.getRelUuidMsb(), r.getRelUuidLsb(),
                            r.getC2UuidMsb(), r.getC2UuidLsb(), r.getRoleTypeIdx()));
                }
                lastRelMsb = r.getRelUuidMsb();
                lastRelLsb = r.getRelUuidLsb();
            }
            oos.flush();
            oos.close();
            aRel = null;
            System.gc();

        } catch (FileNotFoundException e) {
            getLog().info(e);
        } catch (IOException e) {
            getLog().info(e);
        }

        getLog().info(
                "*** DESTINATION RELs: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept STEP #3 COMPLETED -- GATHER DESTINATION RELs ***\r\n");
    }

    private void executeMojoStep4_MatchIds()
            throws MojoFailureException {
        getLog().info("*** Sct1ArfToEConcept STEP #4 BEGINNING -- MATCH IDs ***");
        long start = System.currentTimeMillis();
        int nWrite = 0; // counter for memory optimization for object files writing

        try {
            // Read in IDs. Sort by primary uuid
            // *** IDs ***
            ObjectInputStream ois;
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep1Ids)));
            ArrayList<Sct1_IdRecord> aId = new ArrayList<Sct1_IdRecord>();

            int count = 0;
            Object obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_IdRecord) {
                        aId.add((Sct1_IdRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" id count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" id count = " + count + "\r\n");
            }
            ois.close();

            // SORT BY [PRIMARYID, Path, Revision]
            Collections.sort(aId);

            // Read in con.  Sort by con uuid.
            // *** CONCEPTS ***
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep1Con)));
            ArrayList<Sct1_ConRecord> aCon = new ArrayList<Sct1_ConRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_ConRecord) {
                        aCon.add((Sct1_ConRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" concept count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" concept count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" concept count = " + count + "\r\n");

            // SORT BY [CONCEPTID, Path, Revision]
            Collections.sort(aCon);

            // MATCH & ADD ID TO CONCEPT
            // PLACE IDs ON FIRST UUID INSTANCE OF CONCEPT
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fNameStep4Con)));

            int lastIdIdx = aId.size();
            int lastConIdx = aCon.size();
            int theIdIdx = 0;
            int theConIdx = 0;
            while (theIdIdx < lastIdIdx && theConIdx < lastConIdx) {
                Sct1_ConRecord tmpCon = aCon.get(theConIdx);
                int match = checkIdConMatched(aId.get(theIdIdx), tmpCon);

                if (match == 0) {
                    // MATCH
                    if (aId.get(theIdIdx).getSrcSystemIdx() == 0) {
                        tmpCon.setConSnoId(aId.get(theIdIdx).getDenotationLong());
                    } else {
                        if (tmpCon.getAddedIds() == null) {
                            tmpCon.setAddedIds(new ArrayList<Sct1_IdRecord>());
                        }
                        tmpCon.getAddedIds().add(aId.get(theIdIdx));
                    }
                    theIdIdx++; // Get next id.
                } else if (match == 1) {
                    // Ids are ahead of the concepts.
                    oos.writeUnshared(tmpCon); // Save this concept.
                    theConIdx++; // Get next concept.

                    // PERIODIC RESET IMPROVES MEMORY USE
                    nWrite++;
                    if (nWrite % ooResetInterval == 0) {
                        oos.reset();
                    }
                } else {
                    // Concepts are ahead of the ids.
                    theIdIdx++; // Get the next id.
                }
            }
            while (theConIdx < lastConIdx) {
                oos.writeUnshared(aCon.get(theConIdx)); // Save this concept.
                theConIdx++;

                // PERIODIC RESET IMPROVES MEMORY USE
                nWrite++;
                if (nWrite % ooResetInterval == 0) {
                    oos.reset();
                }
            }
            oos.flush();
            oos.close();
            aCon = null;

            // Read in des.  Sort by des uuid.
            // *** DESCRIPTIONS ***
            ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(fNameStep1Desc)));
            ArrayList<Sct1_DesRecord> aDes = new ArrayList<Sct1_DesRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_DesRecord) {
                        aDes.add((Sct1_DesRecord) obj);
                        count++;

                        if (count % 100000 == 0) {
                            getLog().info(" description count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" description count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" description count = " + count + "\r\n");

            Collections.sort(aDes);

            // MATCH & ADD ID TO DESCRIPTION
            // PLACE IDs ON FIRST UUID INSTANCE OF DESCRIPTIONS
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep4Desc)));

            lastIdIdx = aId.size();
            theIdIdx = 0;
            int lastDesIdx = aDes.size();
            int theDesIdx = 0;
            while (theIdIdx < lastIdIdx && theDesIdx < lastDesIdx) {
                Sct1_DesRecord tmpDes = aDes.get(theDesIdx);
                int match = checkIdDesMatched(aId.get(theIdIdx), tmpDes);
                if (match == 0) { // MATCH
                    if (aId.get(theIdIdx).getSrcSystemIdx() == 0) {
                        tmpDes.setDesSnoId(aId.get(theIdIdx).getDenotationLong());
                    } else {
                        if (tmpDes.getAddedIds() == null) {
                            tmpDes.setAddedIds(new ArrayList<Sct1_IdRecord>());
                        }
                        tmpDes.getAddedIds().add(aId.get(theIdIdx));
                    }
                    theIdIdx++; // Get next id.
                } else if (match == 1) { // Ids are ahead of the descriptions.
                    oos.writeUnshared(tmpDes); // Save this description.
                    theDesIdx++; // Get next description.

                    // PERIODIC RESET IMPROVES MEMORY USE
                    nWrite++;
                    if (nWrite % ooResetInterval == 0) {
                        oos.reset();
                    }
                } else { // Descriptions are ahead of the ids.
                    theIdIdx++; // Get the next id.
                }
            }
            while (theDesIdx < lastDesIdx) {
                oos.writeUnshared(aDes.get(theDesIdx)); // Save this concept.
                theDesIdx++;

                // PERIODIC RESET IMPROVES MEMORY USE
                nWrite++;
                if (nWrite % ooResetInterval == 0) {
                    oos.reset();
                }
            }
            oos.flush();
            oos.close();
            aDes = null;

            // Read in rel. Sort by rel uuid.
            // *** RELATIONSHIPS ***
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep1Rel)));
            ArrayList<Sct1_RelRecord> aRel = new ArrayList<Sct1_RelRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_RelRecord) {
                        aRel.add((Sct1_RelRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" relationships count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" relationships count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" relationships count = " + count + "\r\n");

            Collections.sort(aRel);

            // MATCH & ADD ID TO RELATIONSHIP
            // PLACE IDs ON FIRST UUID INSTANCE OF RELATIONSHIP
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep4Rel)));

            theIdIdx = 0;
            lastIdIdx = aId.size();
            int lastRelIdx = aRel.size();
            int theRelIdx = 0;
            while (theIdIdx < lastIdIdx && theRelIdx < lastRelIdx) {
                Sct1_RelRecord tmpRel = aRel.get(theRelIdx);
                int match = checkIdRelMatched(aId.get(theIdIdx), tmpRel);

                if (match == 0) { // MATCH
                    if (aId.get(theIdIdx).getSrcSystemIdx() == 0) {
                        tmpRel.setRelSnoId(aId.get(theIdIdx).getDenotationLong());
                    } else {
                        if (tmpRel.getAddedIds() == null) {
                            tmpRel.setAddedIds(new ArrayList<Sct1_IdRecord>(1));
                        }
                        tmpRel.getAddedIds().add(aId.get(theIdIdx));
                    }
                    theIdIdx++; // Get next id.
                } else if (match == 1) { // Ids are ahead of the relationships.
                    oos.writeUnshared(tmpRel); // Save this relationship.
                    theRelIdx++; // Get next relationship.

                    // PERIODIC RESET IMPROVES MEMORY USE
                    nWrite++;
                    if (nWrite % ooResetInterval == 0) {
                        oos.reset();
                    }
                } else { // Relationships are ahead of the ids.
                    theIdIdx++; // Get the next id.
                }
            }
            while (theRelIdx < lastRelIdx) {
                oos.writeUnshared(aRel.get(theRelIdx)); // Save this concept.
                theRelIdx++;

                // PERIODIC RESET IMPROVES MEMORY USE
                nWrite++;
                if (nWrite % ooResetInterval == 0) {
                    oos.reset();
                }
            }
            oos.flush();
            oos.close();
            aRel = null;

        } catch (FileNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("FileNotFoundException");
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IOException");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException");
        }

        getLog().info(
                "*** ATTACH IDs TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept STEP #4 COMPLETED - MATCH IDs ***\r\n");
    }

    private TtkIdentifier createEIdentifier(Sct1_IdRecord id) {
        if (id.getDenotation() != null) {
            return createTtkIdentifierString(id);
        } else {
            return createTtkIdentifierLong(id);
        }
    }

    private TtkIdentifier createTtkIdentifierString(Sct1_IdRecord id) {
        TtkIdentifierString eId = new TtkIdentifierString();
        eId.setAuthorityUuid(lookupSrcSystemUUID(id.getSrcSystemIdx()));

        eId.setDenotation(id.getDenotation());

        // PATH
        long msb = zPathArray[id.getPathIdx()].getMostSignificantBits();
        long lsb = zPathArray[id.getPathIdx()].getLeastSignificantBits();
        eId.setPathUuid(new UUID(msb, lsb));

        // STATUS
        eId.setStatus(getStatus(id.getStatus()));

        // VERSION (REVISION TIME)
        eId.setTime(id.getRevTime());

        // USER
        if (id.getUserIdx() == USER_SNOROCKET_IDX) {
            eId.setAuthorUuid(uuidUserSnorocket);
        } else {
            eId.setAuthorUuid(uuidUser);
        }

        // MODULE UUID
        if (id.getModuleIdx() == MODULE_DEFAULT_IDX) {
            eId.setModuleUuid(uuidModule);
        } else {
            eId.setModuleUuid(zModuleArray[id.getModuleIdx()]);
        }

        return eId;
    }

    private TtkIdentifier createTtkIdentifierLong(Sct1_IdRecord id) {
        TtkIdentifierLong eId = new TtkIdentifierLong();
        eId.setAuthorityUuid(lookupSrcSystemUUID(id.getSrcSystemIdx()));

        eId.setDenotation(id.getDenotationLong());

        // PATH
        long msb = zPathArray[id.getPathIdx()].getMostSignificantBits();
        long lsb = zPathArray[id.getPathIdx()].getLeastSignificantBits();
        eId.setPathUuid(new UUID(msb, lsb));

        // STATUS
        eId.setStatus(getStatus(id.getStatus()));

        // VERSION (REVISION TIME)
        eId.setTime(id.getRevTime());

        // USER
        if (id.getUserIdx() == USER_SNOROCKET_IDX) {
            eId.setAuthorUuid(uuidUserSnorocket);
        } else {
            eId.setAuthorUuid(uuidUser);
        }

        // MODULE UUID
        if (id.getModuleIdx() == MODULE_DEFAULT_IDX) {
            eId.setModuleUuid(uuidModule);
        } else {
            eId.setModuleUuid(zModuleArray[id.getModuleIdx()]);
        }

        return eId;
    }

    private int checkIdConMatched(Sct1_IdRecord id, Sct1_ConRecord con) {
        final int BEFORE = -1;
        final int MATCH = 0;
        final int AFTER = 1;

        if (id.getPrimaryUuidMsb() > con.getConUuidMsb()) {
            return AFTER;
        } else if (id.getPrimaryUuidMsb() == con.getConUuidMsb() && id.getPrimaryUuidLsb() > con.getConUuidLsb()) {
            return AFTER;
        }

        if (id.getPrimaryUuidMsb() == con.getConUuidMsb() && id.getPrimaryUuidLsb() == con.getConUuidLsb()) {
            return MATCH;
        }

        return BEFORE;
    }

    private int checkIdDesMatched(Sct1_IdRecord id, Sct1_DesRecord des) {
        final int BEFORE = -1;
        final int MATCH = 0;
        final int AFTER = 1;

        if (id.getPrimaryUuidMsb() > des.getDesUuidMsb()) {
            return AFTER;
        } else if (id.getPrimaryUuidMsb() == des.getDesUuidMsb() && id.getPrimaryUuidLsb() > des.getDesUuidLsb()) {
            return AFTER;
        }

        if (id.getPrimaryUuidMsb() == des.getDesUuidMsb() && id.getPrimaryUuidLsb() == des.getDesUuidLsb()) {
            return MATCH;
        }

        return BEFORE;
    }

    private int checkIdRelMatched(Sct1_IdRecord id, Sct1_RelRecord rel) {
        final int BEFORE = -1;
        final int MATCH = 0;
        final int AFTER = 1;

        if (id.getPrimaryUuidMsb() > rel.getRelUuidMsb()) {
            return AFTER;
        } else if (id.getPrimaryUuidMsb() == rel.getRelUuidMsb() && id.getPrimaryUuidLsb() > rel.getRelUuidLsb()) {
            return AFTER;
        }

        if (id.getPrimaryUuidMsb() == rel.getRelUuidMsb() && id.getPrimaryUuidLsb() == rel.getRelUuidLsb()) {
            return MATCH;
        }

        return BEFORE;
    }

    private void executeMojoStep5() {
        getLog().info("*** Sct1ArfToEConcept Step #5 BEGINNING -- REFSET ATTACHMENT ***");
        long start = System.currentTimeMillis();

        try {
            // *** READ IN REFSET ***
            int numObj = countFileObjects(fNameStep2Refset);

            ObjectInputStream ois;
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep2Refset)));
            ArrayList<Sct1_RefSetRecord> aRs = new ArrayList<Sct1_RefSetRecord>(numObj);

            int count = 0;
            Object obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_RefSetRecord) {
                        aRs.add((Sct1_RefSetRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" refset member in = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" refset member in = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" refset member in = " + count + "\r\n");

            // Sort by [COMPONENTID]
            Collections.sort(aRs);
            int aRsMax = aRs.size();

            // ATTACH ENVELOPE CONCEPTS (3 PASS)
            // *** CONCEPTS ***
            int idxRsA = 0;
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep4Con)));
            try {
                count = 0;
                obj = ois.readUnshared();
                while (obj != null && idxRsA < aRsMax) {
                    Sct1_RefSetRecord rsRec = aRs.get(idxRsA);
                    Sct1_ConRecord conRec = (Sct1_ConRecord) obj;
                    int rsVin = compareMsbLsb(rsRec.getReferencedComponentUuidMsb(),
                            rsRec.getReferencedComponentUuidLsb(), conRec.getConUuidMsb(), conRec.getConUuidLsb());

                    if (rsVin == 0) {
                        rsRec.setConUuidMsb(conRec.getConUuidMsb());
                        rsRec.setConUuidLsb(conRec.getConUuidLsb());
                        rsRec.setComponentType(Sct1_RefSetRecord.ComponentType.CONCEPT);
                        idxRsA++;
                    } else if (rsVin > 0) {
                        obj = ois.readUnshared();
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" concept count = " + count);
                        }
                    } else {
                        idxRsA++;
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" concept count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" concept count = " + count + "\r\n");

            // *** DESCRIPTIONS ***
            ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(fNameStep4Desc)));
            try {
                count = 0;
                idxRsA = 0;
                obj = ois.readUnshared();
                while (obj != null && idxRsA < aRsMax) {
                    Sct1_RefSetRecord rsRec = aRs.get(idxRsA);
                    Sct1_DesRecord desRec = (Sct1_DesRecord) obj;
                    
                    int rsVin = compareMsbLsb(rsRec.getReferencedComponentUuidMsb(),
                            rsRec.getReferencedComponentUuidLsb(), desRec.getDesUuidMsb(), desRec.getDesUuidLsb());

                    if (rsVin == 0) {
                        if (rsRec.getConUuidMsb() != Long.MAX_VALUE) {
                            getLog().info(
                                    "ERROR: Refset Envelop UUID Concept/Description conflict"
                                    + "\r\nExisting UUID:"
                                    + new UUID(rsRec.getConUuidMsb(), rsRec.getConUuidLsb())
                                    + "\r\nDescription UUID:"
                                    + new UUID(desRec.getDesUuidMsb(), desRec.getDesUuidLsb()));
                        }

                        rsRec.setConUuidMsb(desRec.getConUuidMsb());
                        rsRec.setConUuidLsb(desRec.getConUuidLsb());
                        rsRec.setComponentType(Sct1_RefSetRecord.ComponentType.DESCRIPTION);
                        idxRsA++;
                    } else if (rsVin > 0) {
                        obj = ois.readUnshared();
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" description count = " + count);
                        }
                    } else {
                        idxRsA++;
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" description count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" description count = " + count + "\r\n");

            // *** RELATIONSHIPS ***
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep4Rel)));
            try {
                count = 0;
                idxRsA = 0;
                obj = ois.readUnshared();
                while (obj != null && idxRsA < aRsMax) {
                    Sct1_RefSetRecord rsRec = aRs.get(idxRsA);
                    Sct1_RelRecord relRec = (Sct1_RelRecord) obj;
                    int rsVin = compareMsbLsb(rsRec.getReferencedComponentUuidMsb(),
                            rsRec.getReferencedComponentUuidLsb(), relRec.getRelUuidMsb(), relRec.getRelUuidLsb());

                    if (rsVin == 0) {
                        if (rsRec.getConUuidMsb() != Long.MAX_VALUE) {
                            getLog().info(
                                    "ERROR: Refset Envelop UUID Concept/Relationship conflict"
                                    + "\r\nExisting UUID:"
                                    + new UUID(rsRec.getConUuidMsb(), rsRec.getConUuidLsb())
                                    + "\r\nRelationship UUID:"
                                    + new UUID(relRec.getRelUuidMsb(), relRec.getRelUuidLsb()));
                        }

                        rsRec.setConUuidMsb(relRec.getC1UuidMsb());
                        rsRec.setConUuidLsb(relRec.getC1UuidLsb());
                        rsRec.setComponentType(Sct1_RefSetRecord.ComponentType.RELATIONSHIP);
                        idxRsA++;
                    } else if (rsVin > 0) {
                        obj = ois.readUnshared();
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" relationship count = " + count);
                        }
                    } else {
                        idxRsA++;
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" relationships count = " + count + " @ eof\r\n");
            }
            ois.close();
            getLog().info(" relationships count = " + count + "\r\n");

            // *** MEMBERS WHICH ARE REFSET CONCEPTS ***
            ArrayList<Sct1_RefSetRecord> bRs = new ArrayList<Sct1_RefSetRecord>(aRs);
            Comparator<Sct1_RefSetRecord> compRsByRs = new Comparator<Sct1_RefSetRecord>() {
                @Override
                public int compare(Sct1_RefSetRecord o1, Sct1_RefSetRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // CONCEPTID
                    if (o1.getRefsetUuidMsb() > o2.getRefsetUuidMsb()) {
                        return thisMore;
                    } else if (o1.getRefsetUuidMsb() < o2.getRefsetUuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getRefsetUuidLsb() > o2.getRefsetUuidLsb()) {
                            return thisMore;
                        } else if (o1.getRefsetUuidLsb() < o2.getRefsetUuidLsb()) {
                            return thisLess;
                        } else {
                            // Path
                            if (o1.getPathIdx() > o2.getPathIdx()) {
                                return thisMore;
                            } else if (o1.getPathIdx() < o2.getPathIdx()) {
                                return thisLess;
                            } else {
                                // Revision
                                if (o1.getRevTime() > o2.getRevTime()) {
                                    return thisMore;
                                } else if (o1.getRevTime() < o2.getRevTime()) {
                                    return thisLess;
                                } else {
                                    return 0; // EQUAL
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(bRs, compRsByRs);

            count = 0;
            idxRsA = 0;
            int idxRsB = 0;
            while (idxRsA < aRsMax && idxRsB < aRsMax) {
                Sct1_RefSetRecord rsRecA = aRs.get(idxRsA);
                Sct1_RefSetRecord rsRecB = bRs.get(idxRsB);
                int rsVin = compareMsbLsb(rsRecA.getReferencedComponentUuidMsb(),
                        rsRecA.getReferencedComponentUuidLsb(), rsRecB.getRefsetUuidMsb(),
                        rsRecB.getRefsetUuidLsb());

                if (rsVin == 0) {
                    if (rsRecA.getConUuidMsb() != Long.MAX_VALUE) {
                        getLog().info(
                                "ERROR: Refset Envelop UUID Concept/Refset conflict"
                                + "\r\nExisting UUID:"
                                + new UUID(rsRecA.getConUuidMsb(), rsRecA.getConUuidLsb())
                                + "\r\nRefset UUID:"
                                + new UUID(rsRecB.getRefsetUuidMsb(), rsRecB.getRefsetUuidLsb()));
                    }

                    rsRecA.setConUuidMsb(rsRecB.getRefsetUuidMsb());
                    rsRecA.setConUuidLsb(rsRecB.getRefsetUuidLsb());
                    rsRecA.setComponentType(Sct1_RefSetRecord.ComponentType.MEMBER);
                    idxRsA++;
                } else if (rsVin > 0) {
                    idxRsB++;
                    count++;
                    if (count % 100000 == 0) {
                        getLog().info(" refset count = " + count);
                    }
                } else {
                    idxRsA++;
                }
            }

            // SAVE FILE SORTED BY "ENVELOP eConcept" UUID, pathIdx, revision
            Comparator<Sct1_RefSetRecord> compRsByCon = new Comparator<Sct1_RefSetRecord>() {
                @Override
                public int compare(Sct1_RefSetRecord o1, Sct1_RefSetRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // CONCEPTID
                    if (o1.getConUuidMsb() > o2.getConUuidMsb()) {
                        return thisMore;
                    } else if (o1.getConUuidMsb() < o2.getConUuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getConUuidLsb() > o2.getConUuidLsb()) {
                            return thisMore;
                        } else if (o1.getConUuidLsb() < o2.getConUuidLsb()) {
                            return thisLess;
                        } else {
                            // Path
                            if (o1.getPathIdx() > o2.getPathIdx()) {
                                return thisMore;
                            } else if (o1.getPathIdx() < o2.getPathIdx()) {
                                return thisLess;
                            } else {
                                // Revision
                                if (o1.getRevTime() > o2.getRevTime()) {
                                    return thisMore;
                                } else if (o1.getRevTime() < o2.getRevTime()) {
                                    return thisLess;
                                } else {
                                    return 0; // EQUAL
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aRs, compRsByCon);
            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fNameStep5RsByCon)));
            for (Sct1_RefSetRecord r : aRs) {
                oos.writeUnshared(r);
            }
            oos.flush();
            oos.close();

            // SAVE FILE SORTED BY REFSET UUID
            Collections.sort(aRs, compRsByRs);
            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep5RsByRs)));
            for (Sct1_RefSetRecord r : aRs) {
                oos.writeUnshared(r);
            }
            oos.flush();
            oos.close();

            // :NYI: check to see if any refset member remained unassigned. 

            aRs = null;
            System.gc();

        } catch (FileNotFoundException e) {
            getLog().info(e);
        } catch (IOException e) {
            getLog().info(e);
        } catch (ClassNotFoundException e) {
            getLog().info(e);
        }

        getLog().info(
                "*** MASTER SORT TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept Step #5 COMPLETED -- REFSET ATTACHMENT ***\r\n");
    }

    private int compareMsbLsb(long aMsb, long aLsb, long bMsb, long bLsb) {
        int thisMore = 1;
        int thisLess = -1;
        if (aMsb < bMsb) {
            return thisLess; // instance less than received
        } else if (aMsb > bMsb) {
            return thisMore; // instance greater than received
        } else {
            if (aLsb < bLsb) {
                return thisLess;
            } else if (aLsb > bLsb) {
                return thisMore;
            } else {
                return 0; // instance == received
            }
        }
    }

    // :NYI: concepts may not need to be sorted again after previous step.
    private void executeMojoStep6()
            throws MojoFailureException {
        getLog().info("*** Sct1ArfToEConcept Step #6 BEGINNING -- SORT BY CONCEPT ***");
        long start = System.currentTimeMillis();
        try {

            // *** CONCEPTS ***
            ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(
                    new FileInputStream(fNameStep4Con)));
            ArrayList<Sct1_ConRecord> aCon = new ArrayList<Sct1_ConRecord>();

            int count = 0;
            Object obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_ConRecord) {
                        aCon.add((Sct1_ConRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" concept count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" concept count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" concept count = " + count + "\r\n");

            // SORT BY [CONCEPTID, Path, Revision]
            Comparator<Sct1_ConRecord> compCon = new Comparator<Sct1_ConRecord>() {
                @Override
                public int compare(Sct1_ConRecord o1, Sct1_ConRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // CONCEPTID
                    if (o1.getConUuidMsb() > o2.getConUuidMsb()) {
                        return thisMore;
                    } else if (o1.getConUuidMsb() < o2.getConUuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getConUuidLsb() > o2.getConUuidLsb()) {
                            return thisMore;
                        } else if (o1.getConUuidLsb() < o2.getConUuidLsb()) {
                            return thisLess;
                        } else {
                            // Path
                            if (o1.getPathIdx() > o2.getPathIdx()) {
                                return thisMore;
                            } else if (o1.getPathIdx() < o2.getPathIdx()) {
                                return thisLess;
                            } else {
                                // Revision
                                if (o1.getRevTime() > o2.getRevTime()) {
                                    return thisMore;
                                } else if (o1.getRevTime() < o2.getRevTime()) {
                                    return thisLess;
                                } else {
                                    return 0; // EQUAL
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aCon, compCon);

            ObjectOutputStream oos = new ObjectOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fNameStep6Con)));
            for (Sct1_ConRecord r : aCon) {
                oos.writeUnshared(r);
            }
            oos.flush();
            oos.close();
            aCon = null;
            System.gc();

            // *** DESCRIPTIONS ***
            ois = new ObjectInputStream(
                    new BufferedInputStream(new FileInputStream(fNameStep4Desc)));
            ArrayList<Sct1_DesRecord> aDes = new ArrayList<Sct1_DesRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_DesRecord) {
                        aDes.add((Sct1_DesRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" description count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" description count = " + count + " @EOF\r\n");
            }
            ois.close();
            getLog().info(" description count = " + count + "\r\n");

            // SORT BY [CONCEPTID, DESCRIPTIONID, Path, Revision]
            Comparator<Sct1_DesRecord> compDes = new Comparator<Sct1_DesRecord>() {
                @Override
                public int compare(Sct1_DesRecord o1, Sct1_DesRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // CONCEPTID
                    if (o1.getConUuidMsb() > o2.getConUuidMsb()) {
                        return thisMore;
                    } else if (o1.getConUuidMsb() < o2.getConUuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getConUuidLsb() > o2.getConUuidLsb()) {
                            return thisMore;
                        } else if (o1.getConUuidLsb() < o2.getConUuidLsb()) {
                            return thisLess;
                        } else {
                            // DESCRIPTIONID
                            if (o1.getDesUuidMsb() > o2.getDesUuidMsb()) {
                                return thisMore;
                            } else if (o1.getDesUuidMsb() < o2.getDesUuidMsb()) {
                                return thisLess;
                            } else {
                                if (o1.getDesUuidLsb() > o2.getDesUuidLsb()) {
                                    return thisMore;
                                } else if (o1.getDesUuidLsb() < o2.getDesUuidLsb()) {
                                    return thisLess;
                                } else {
                                    // Path
                                    if (o1.getPathIdx() > o2.getPathIdx()) {
                                        return thisMore;
                                    } else if (o1.getPathIdx() < o2.getPathIdx()) {
                                        return thisLess;
                                    } else {
                                        // Revision
                                        if (o1.getRevTime() > o2.getRevTime()) {
                                            return thisMore;
                                        } else if (o1.getRevTime() < o2.getRevTime()) {
                                            return thisLess;
                                        } else {
                                            return 0; // EQUAL
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aDes, compDes);

            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep6Desc)));
            for (Sct1_DesRecord r : aDes) {
                oos.writeUnshared(r);
            }

            oos.flush();
            oos.close();
            aDes = null;
            System.gc();

            // *** RELATIONSHIPS ***
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fNameStep4Rel)));
            ArrayList<Sct1_RelRecord> aRel = new ArrayList<Sct1_RelRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_RelRecord) {
                        aRel.add((Sct1_RelRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" relationships count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" relationships count = " + count + "\r\n");
            }
            ois.close();
            getLog().info(" relationships count = " + count + " @EOF\r\n");

            // SORT BY [C1-Group-RoleType-Path-RevisionVersion]
            Comparator<Sct1_RelRecord> compRel = new Comparator<Sct1_RelRecord>() {
                @Override
                public int compare(Sct1_RelRecord o1, Sct1_RelRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // C1
                    if (o1.getC1UuidMsb() > o2.getC1UuidMsb()) {
                        return thisMore;
                    } else if (o1.getC1UuidMsb() < o2.getC1UuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getC1UuidLsb() > o2.getC1UuidLsb()) {
                            return thisMore;
                        } else if (o1.getC1UuidLsb() < o2.getC1UuidLsb()) {
                            return thisLess;
                        } else {
                            // GROUP
                            if (o1.getGroup() > o2.getGroup()) {
                                return thisMore;
                            } else if (o1.getGroup() < o2.getGroup()) {
                                return thisLess;
                            } else {
                                // ROLE TYPE
                                if (o1.getRoleTypeIdx() > o2.getRoleTypeIdx()) {
                                    return thisMore;
                                } else if (o1.getRoleTypeIdx() < o2.getRoleTypeIdx()) {
                                    return thisLess;
                                } else {
                                    // C2
                                    if (o1.getC2UuidMsb() > o2.getC2UuidMsb()) {
                                        return thisMore;
                                    } else if (o1.getC2UuidMsb() < o2.getC2UuidMsb()) {
                                        return thisLess;
                                    } else {
                                        if (o1.getC2UuidLsb() > o2.getC2UuidLsb()) {
                                            return thisMore;
                                        } else if (o1.getC2UuidLsb() < o2.getC2UuidLsb()) {
                                            return thisLess;
                                        } else {
                                            // PATH
                                            if (o1.getPathIdx() > o2.getPathIdx()) {
                                                return thisMore;
                                            } else if (o1.getPathIdx() < o2.getPathIdx()) {
                                                return thisLess;
                                            } else {
                                                // VERSION
                                                if (o1.getRevTime() > o2.getRevTime()) {
                                                    return thisMore;
                                                } else if (o1.getRevTime() < o2.getRevTime()) {
                                                    return thisLess;
                                                } else {
                                                    return 0; // EQUAL
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aRel, compRel);

            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep6Rel)));
            for (Sct1_RelRecord r : aRel) {
                oos.writeUnshared(r);
            }
            oos.flush();
            oos.close();
            aRel = null;

            // ** DESTINATION RELATIONSHIPS **
            ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep3RelDest)));
            ArrayList<Sct1_RelDestRecord> aRelDest = new ArrayList<Sct1_RelDestRecord>();

            count = 0;
            obj = null;
            try {
                while ((obj = ois.readObject()) != null) {
                    if (obj instanceof Sct1_RelDestRecord) {
                        aRelDest.add((Sct1_RelDestRecord) obj);
                        count++;
                        if (count % 100000 == 0) {
                            getLog().info(" destination relationships count = " + count);
                        }
                    }
                }
            } catch (EOFException ex) {
                getLog().info(" destination relationships count = " + count + "\r\n");
            }
            ois.close();
            getLog().info(" destination relationships count = " + count + " @EOF\r\n");

            // SORT BY [C2-RoleType]
            Comparator<Sct1_RelDestRecord> compRelDest = new Comparator<Sct1_RelDestRecord>() {
                @Override
                public int compare(Sct1_RelDestRecord o1, Sct1_RelDestRecord o2) {
                    int thisMore = 1;
                    int thisLess = -1;
                    // C2
                    if (o1.getC2UuidMsb() > o2.getC2UuidMsb()) {
                        return thisMore;
                    } else if (o1.getC2UuidMsb() < o2.getC2UuidMsb()) {
                        return thisLess;
                    } else {
                        if (o1.getC2UuidLsb() > o2.getC2UuidLsb()) {
                            return thisMore;
                        } else if (o1.getC2UuidLsb() < o2.getC2UuidLsb()) {
                            return thisLess;
                        } else {
                            // ROLE TYPE
                            if (o1.getRoleTypeIdx() > o2.getRoleTypeIdx()) {
                                return thisMore;
                            } else if (o1.getRoleTypeIdx() < o2.getRoleTypeIdx()) {
                                return thisLess;
                            } else {
                                return 0; // EQUAL
                            }
                        }
                    }
                } // compare()
            };
            Collections.sort(aRelDest, compRelDest);

            oos = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(
                    fNameStep6RelDest)));
            for (Sct1_RelDestRecord r : aRelDest) {
                oos.writeUnshared(r);
            }
            oos.flush();
            oos.close();
            aRelDest = null;

            System.gc();

        } catch (FileNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("File Not Found -- Step 6 Sort");
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- Step 6 Sort");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- Step 6 Sort");
        }
        getLog().info(
                "*** MASTER SORT TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** Sct1ArfToEConcept Step #6 COMPLETED -- SORT BY CONCEPT ***\r\n");
    }

    /**
     * executeMojoStep6() reads concepts, descriptions, relationship, destination relationships, &
     * ids files in concept order.
     *
     * @throws MojoFailureException
     * @throws IOException
     */
    private void executeMojoStep7()
            throws MojoFailureException, IOException {
        statCon = 0;
        statDes = 0;
        statRel = 0;
        statRelDest = 0;
        statRsByCon = 0;
        statRsByRs = 0;

        getLog().info("*** Sct1ArfToEConcept STEP #7 BEGINNING -- CREATE eCONCEPTS ***");
        long start = System.currentTimeMillis();
        countEConWritten = 0;

        // Lists hold records for the immediate operations 
        ArrayList<Sct1_ConRecord> conList = new ArrayList<Sct1_ConRecord>();
        ArrayList<Sct1_DesRecord> desList = new ArrayList<Sct1_DesRecord>();
        ArrayList<Sct1_RelRecord> relList = new ArrayList<Sct1_RelRecord>();
        ArrayList<Sct1_RelDestRecord> relDestList = new ArrayList<Sct1_RelDestRecord>();
        ArrayList<Sct1_RefSetRecord> rsByConList = new ArrayList<Sct1_RefSetRecord>();
        ArrayList<Sct1_RefSetRecord> rsByRsList = new ArrayList<Sct1_RefSetRecord>();

        // Since readObject must look one record ahead,
        // the look ahead record is stored as "Next"
        Sct1_ConRecord conNext = null;
        Sct1_DesRecord desNext = null;
        Sct1_RelRecord relNext = null;
        Sct1_RelDestRecord relDestNext = null;
        Sct1_RefSetRecord rsByConNext = null;
        Sct1_RefSetRecord rsByRsNext = null;

        // Open Input and Output Streams
        ObjectInputStream oisCon = null;
        ObjectInputStream oisDes = null;
        ObjectInputStream oisRel = null;
        ObjectInputStream oisRelDest = null;
        ObjectInputStream oisRsByCon = null;
        ObjectInputStream oisRsByRs = null;
        DataOutputStream dos = null;
        try {
            oisCon = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep6Con)));
            oisDes = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep6Desc)));
            oisRel = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep6Rel)));
            oisRelDest = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep6RelDest)));
            oisRsByCon = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep5RsByCon)));
            oisRsByRs = new ObjectInputStream(new BufferedInputStream(new FileInputStream(
                    fNameStep5RsByRs)));
            dos = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(fNameStep7ECon)));
        } catch (FileNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("File Not Found -- Step #7");
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- Step #7");
        }

        // :DEBUG:
        //        boolean readMoreBug = true;
        //        boolean nextBug = false;
        //        int bugCount = 0;
        //        while (readMoreBug) {
        //            Object bugO;
        //            try {
        //                bugO = oisDes.readUnshared()
        //                if (bugO instanceof Sct1_DesRecord || nextBug == true) {
        //                    Sct1_DesRecord bugDes = (Sct1_DesRecord) bugO;
        //                    if (bugDes.conUuidMsb == -8120194779924901686L
        //                            && bugDes.conUuidLsb == -6989461898667750587L) {
        //                        getLog().info(":DEBUG: ...  ## count ## " + bugCount);
        //                        getLog().info(":DEBUG: ... conSnoId   = " + bugDes.conSnoId);
        //                        getLog().info(":DEBUG: ... conUuidLsb = " + bugDes.conUuidLsb);
        //                        getLog().info(":DEBUG: ... conUuidMsb = " + bugDes.conUuidMsb);
        //                        getLog().info(":DEBUG: ... termText   = " + bugDes.termText);
        //                        nextBug = !nextBug;
        //                    }
        //                } else 
        //                    readMoreBug = false;
        //                    
        //            } catch (IOException e) {
        //                getLog().info(e);
        //                readMoreBug = false;
        //            } catch (ClassNotFoundException e) {
        //                getLog().info(e);
        //                readMoreBug = false;
        //            }
        //            bugCount++;
        //        } 
        // :DEBUG:END 

        int countCon = 0;
        int countDes = 0;
        int countRel = 0;
        int countRsByCon = 0;
        int countRsByRs = 0;
        boolean notDone = true;
        UUID theCon;
        UUID theDes = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID theRel = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID theRelDest = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID theRsByCon = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID theRsByRs = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID prevCon = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID prevDes = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID prevRel = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        UUID prevRelDest = new UUID(Long.MIN_VALUE, Long.MIN_VALUE);
        while (notDone) {
            // Get next Concept record(s) for 1 id.
            conNext = readNextCon(oisCon, conList, conNext);
            Sct1_ConRecord tmpConRec = conList.get(0);
            theCon = new UUID(tmpConRec.getConUuidMsb(), tmpConRec.getConUuidLsb());
            countCon++;

            while (theDes.compareTo(theCon) == IS_LESS) {
                desNext = readNextDes(oisDes, desList, desNext);
                if (desNext == null && desList.isEmpty()) {
                    theDes = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
                    Sct1_DesRecord tmpDes = desList.get(0);
                    theDes = new UUID(tmpDes.getConUuidMsb(), tmpDes.getConUuidLsb());
                    countDes++;
                    if (theDes.compareTo(theCon) == IS_LESS) {
                        getLog().info("ORPHAN DESCRIPTION :: " + desList.get(0).getTermText());
                    }
                }
            }

            while (theRel.compareTo(theCon) == IS_LESS) {
                relNext = readNextRel(oisRel, relList, relNext);
                if (relNext == null && relList.isEmpty()) {
                    theRel = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
                    // theRel = relList.get(0).c1SnoId;
                    Sct1_RelRecord tmpRel = relList.get(0);
                    theRel = new UUID(tmpRel.getC1UuidMsb(), tmpRel.getC1UuidLsb());
                    countRel++;
                    if (theRel.compareTo(theCon) == IS_LESS) {
                        getLog().info(
                                "ORPHAN RELATIONSHIP :: relid=" + relList.get(0).getRelSnoId() + " c1=="
                                + relList.get(0).getC1SnoId());
                    }
                }
            }

            while (theRelDest.compareTo(theCon) == IS_LESS) {
                relDestNext = readNextRelDest(oisRelDest, relDestList, relDestNext);
                if (relDestNext == null && relDestList.isEmpty()) {
                    theRelDest = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
                    // theRelDest = relDestList.get(0).c2SnoId;
                    Sct1_RelDestRecord tmpRelDest = relDestList.get(0);
                    theRelDest = new UUID(tmpRelDest.getC2UuidMsb(), tmpRelDest.getC2UuidLsb());
                    if (theRelDest.compareTo(theCon) == IS_LESS) {
                        getLog().info(
                                "ORPHAN DEST. RELATIONSHIP :: relid="
                                + relList.get(0).getRelSnoId()
                                + " c2=="
                                + new UUID(relDestList.get(0).getC2UuidMsb(),
                                relDestList.get(0).getC2UuidLsb()));
                    }
                }
            }

            while (theRsByCon.compareTo(theCon) == IS_LESS) {
                rsByConNext = readNextRsByCon(oisRsByCon, rsByConList, rsByConNext);

                if (rsByConNext == null && rsByConList.isEmpty()) {
                    theRsByCon = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
                    Sct1_RefSetRecord tmpRsByCon = rsByConList.get(0);
                    theRsByCon = new UUID(tmpRsByCon.getConUuidMsb(), tmpRsByCon.getConUuidLsb());
                    countRsByCon++;
                    if (theRsByCon.compareTo(theCon) == IS_LESS) {
                        getLog().info(
                                "ORPHAN REFSET MEMBER RECORD_A :: "
                                + new UUID(rsByConList.get(0).getRefsetMemberUuidMsb(),
                                rsByConList.get(0).getRefsetMemberUuidLsb()));
                    }
                }
            }

            while (theRsByRs.compareTo(theCon) == IS_LESS) {
                rsByRsNext = readNextRsByRs(oisRsByRs, rsByRsList, rsByRsNext);
                // :DEBUG:
                //                UUID debugUuidRsByCon111 = UUID.fromString("ccbd4a65-9b1a-5df3-94d1-4a1085f3c758");

                if (rsByRsNext == null && rsByRsList.isEmpty()) {
                    theRsByRs = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
                } else {
                    Sct1_RefSetRecord tmpRsByRs = rsByRsList.get(0);
                    theRsByRs = new UUID(tmpRsByRs.getRefsetUuidMsb(), tmpRsByRs.getRefsetUuidLsb());
                    countRsByRs++;
                    if (theRsByRs.compareTo(theCon) == IS_LESS) {
                        getLog().info(
                                "ORPHAN REFSET MEMBER RECORD_B :: "
                                + new UUID(rsByRsList.get(0).getRefsetMemberUuidMsb(),
                                rsByRsList.get(0).getRefsetMemberUuidLsb()));
                    }
                }
            }

            // Check for next sync
            if (theCon.compareTo(theDes) != IS_EQUAL
                    || theCon.compareTo(theRel) != IS_EQUAL /*
                     * || theCon != theRelDest
                     */) {
                if (reportRootConcepts) {
                    getLog().info("CONFIRM: ROOT CONCEPT ");
                    UUID uuid = new UUID(conList.get(0).getConUuidMsb(), conList.get(0).getConUuidLsb());
                    getLog().info(" -is- concept SNOMED UUID =" + uuid.toString());
                    getLog().info(" -is- concept SNOMED id =" + conList.get(0).getConSnoId());
                    getLog().info(" -is- concept counter #" + countCon);
                    getLog().info(" -is- description \"" + desList.get(0).getTermText() + "\"\r\n");
                    getLog().info(
                            " ...prev... " + prevCon + " " + prevDes + " " + prevRel + " "
                            + prevRelDest);
                    getLog().info(
                            " ...-is-... " + theCon + " " + theDes + " " + theRel + " " + theRelDest);
                    String cnStr = "*null*";
                    if (conNext != null) {
                        uuid = new UUID(conNext.getConUuidMsb(), conNext.getConUuidLsb());
                        cnStr = uuid.toString();
                    }
                    String dnStr = "*null*";
                    if (desNext != null) {
                        uuid = new UUID(desNext.getConUuidMsb(), desNext.getConUuidLsb());
                        dnStr = uuid.toString();
                    }
                    String rnStr = "*null*";
                    if (relNext != null) {
                        uuid = new UUID(relNext.getC1UuidMsb(), relNext.getC1UuidLsb());
                        rnStr = uuid.toString();
                    }
                    String rdnStr = "*null*";
                    if (relDestNext != null) {
                        uuid = new UUID(relDestNext.getC2UuidMsb(), relDestNext.getC2UuidLsb());
                        rdnStr = uuid.toString();
                    }
                    getLog().info(
                            " ..\"next\".. " + cnStr + " " + dnStr + " " + rnStr + " " + rdnStr
                            + "\r\n");
                }
            }

            ArrayList<Sct1_RefSetRecord> addRsByCon = null;
            if (theCon.compareTo(theRsByCon) == IS_EQUAL) {
                addRsByCon = rsByConList;
            }
            ArrayList<Sct1_RefSetRecord> addRsByRs = null;
            if (theCon.compareTo(theRsByRs) == IS_EQUAL) {
                addRsByRs = rsByRsList;
            }

            if (theCon.compareTo(theDes) != IS_EQUAL && theCon.compareTo(theRel) == IS_EQUAL && 
                ((theCon.compareTo(theRelDest) != IS_EQUAL) || (theCon.compareTo(theRelDest) == IS_EQUAL))) {
                // MISSING CASE(s)  theCon !=theDes ==theRel ==theRelDest/!=theRelDest
                createEConcept(conList, null, relList, null, addRsByCon, addRsByRs, dos);
            } else if (theCon.compareTo(theDes) == IS_EQUAL && theCon.compareTo(theRel) == IS_EQUAL
                    && theCon.compareTo(theRelDest) == IS_EQUAL) {
                // MIDDLE CASE theCon ==theDes ==theRel ==theRelDest
                createEConcept(conList, desList, relList, relDestList, addRsByCon, addRsByRs, dos);
            } else if (theCon.compareTo(theDes) == IS_EQUAL && theCon.compareTo(theRel) != IS_EQUAL
                    && theCon.compareTo(theRelDest) == IS_EQUAL) {
                // TOP CASE  theCon ==theDes !=theRel ==theRelDest
                createEConcept(conList, desList, null, relDestList, addRsByCon, addRsByRs, dos);
            } else if (theCon.compareTo(theDes) == IS_EQUAL && theCon.compareTo(theRel) == IS_EQUAL
                    && theCon.compareTo(theRelDest) != IS_EQUAL) {
                // BOTTOM CASE theCon ==theDes ==theRel !=theRelDest
                createEConcept(conList, desList, relList, null, addRsByCon, addRsByRs, dos);
            } else if (theCon.compareTo(theDes) == IS_EQUAL && theCon.compareTo(theRel) != IS_EQUAL
                    && theCon.compareTo(theRelDest) != IS_EQUAL) {
                // UNCONNECTED CONCEPT theCon ==theDes !=theRel !=theRelDest
                createEConcept(conList, desList, null, null, addRsByCon, addRsByRs, dos);
            } else if (theCon.compareTo(theDes) != IS_EQUAL && theCon.compareTo(theRel) != IS_EQUAL
                    && theCon.compareTo(theRelDest) != IS_EQUAL) {
                // UNCONNECTED REFSET CONCEPT theCon !=theDes !=theRel !=theRelDest
                createEConcept(conList, null, null, null, addRsByCon, addRsByRs, dos);
            }else {
                getLog().info("!!! Note: the following can occur if the placeholder concepts mojo is used. "
                        + "Placeholders are created for where the concept attach to existing data, potentially resulting in empty placeholders.");
                getLog().info(
                        "!!! Case what case is this??? -- Step 4" + " theCon=\t" + theCon
                        + "\ttheDes=\t" + theDes + "\ttheRel=\t" + theRel
                        + "\ttheRelDest\t" + theRelDest);
                getLog().info("!!! --- concept UUID id   =" + theCon);
                getLog().info("!!! --- concept SNOMED id =" + conList.get(0).getConSnoId());

                getLog().info("!!! --- concept counter   #" + countCon);
                if(!desList.isEmpty()){
                    getLog().info("!!! --- description       \"" + desList.get(0).getTermText() + "\"");
                }
                getLog().info("!!! \r\n");
//                throw new MojoFailureException("Case not implemented -- executeMojoStep7()");
            }

            if (conNext == null && desNext == null && relNext == null) {
                notDone = false;
            }

            prevCon = theCon;
            prevDes = theDes;
            prevRel = theRel;
            prevRelDest = theRelDest;

            if ((addRsByRs != null) && (addRsByRs.size() > 4096)) {
                System.gc();
            }
            if (countCon % 500000 == 0) {
                System.gc();
            }

        }
        getLog().info(
                "RECORD COUNT = " + countCon + "(Con) " + countDes + "(Des) " + countRel + "(Rel)");
        getLog().info(
                "COMPONENT COUNT = " + statCon + "(statCon) " + statDes + "(statDes) " + statRel
                + "(statRel)");
        getLog().info(
                "INDEX COUNT = " + statRelDest + "(statRelDest) " + statRsByCon + "(statRsByCon) "
                + statRsByRs + "(statRsByRs)");
        getLog().info(
                "REFSET COUNT = " + countRsByCon + "(countRsByCon) " + countRsByRs
                + "(countRsByRs) ");

        // CLOSE FILES
        try {
            oisCon.close();
            oisDes.close();
            oisRel.close();
            oisRelDest.close();
            oisRsByCon.close();
            oisRsByRs.close();
            dos.close();
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- Step 4, closing files");
        }
        getLog().info(
                "*** ECONCEPT CREATION TIME: " + ((System.currentTimeMillis() - start) / 1000)
                + " seconds");
        getLog().info("*** ECONCEPTS WRITTEN TO FILE = " + countEConWritten);
        getLog().info("*** Sct1ArfToEConcept STEP #7 COMPLETED -- CREATE eCONCEPTS ***\r\n");
    }

    // UUID OF INTEREST
    // ICD-0-3 == cff53f1a-1d11-5ae7-801e-d3301cfdbea0
//    private static final UUID debugUuid01 = UUID.fromString("445d932f-f552-3b4b-a323-3a06f1d26d98");
    //private static final UUID debugUuid02 = UUID.fromString("daa9598a-2ddb-5527-beda-ee4303a7656c");
    //private static final UUID debugUuid03 = UUID.fromString("3ca0d065-06b8-596c-8ca0-e4d2a605701c");
    private void createEConcept(ArrayList<Sct1_ConRecord> conList,
            ArrayList<Sct1_DesRecord> desList, ArrayList<Sct1_RelRecord> relList,
            ArrayList<Sct1_RelDestRecord> relDestList, ArrayList<Sct1_RefSetRecord> rsByConList,
            ArrayList<Sct1_RefSetRecord> rsByRsList, DataOutputStream dos)
            throws MojoFailureException {
        if (conList.size() < 1) {
            throw new MojoFailureException("createEConcept(), empty conList");
        }

        statCon++;
        if (desList != null) {
            statDes += desList.size();
        }
        if (relList != null) {
            statRel += relList.size();
        }
        if (relDestList != null) {
            statRelDest += relDestList.size();
        }
        if (rsByConList != null) {
            statRsByCon += rsByConList.size();
        }
        if (rsByRsList != null) {
            statRsByRs += rsByRsList.size();
        }

        Collections.sort(conList);
        Sct1_ConRecord cRec0 = conList.get(0);
        UUID theConUUID = new UUID(cRec0.getConUuidMsb(), cRec0.getConUuidLsb());

//        if (theConUUID.compareTo(debugUuid01) == 0) {
//            System.out.println(":!!!:DEBUG:");
//        }

        TtkConceptChronicle ec = new TtkConceptChronicle();
        ec.setPrimordialUuid(theConUUID);

        if (cRec0.getStatus() == -1) {
            ec.setConceptAttributes(null);
        } else {
            // ADD CONCEPT ATTRIBUTES
            TtkConceptAttributesChronicle ca = new TtkConceptAttributesChronicle();
            ca.primordialUuid = theConUUID;
            ca.setDefined(cRec0.getIsprimitive() == 0 ? true : false);
            ca.setAuthorUuid(uuidUser);
            if (cRec0.getModuleIdx() == MODULE_DEFAULT_IDX) {
                ca.setModuleUuid(uuidModule);
            } else {
                ca.setModuleUuid(zModuleArray[cRec0.getModuleIdx()]);
            }

            ArrayList<TtkIdentifier> tmpAdditionalIds = new ArrayList<TtkIdentifier>();

            // SNOMED ID, if present
            if (cRec0.getConSnoId() < Long.MAX_VALUE) {
                TtkIdentifierLong cid = new TtkIdentifierLong();
                cid.setAuthorityUuid(uuidSourceSnomedLong);
                cid.setDenotation(cRec0.getConSnoId());
                cid.setPathUuid(zPathArray[cRec0.getPathIdx()]);
                cid.setStatus(Status.ACTIVE);
                cid.setTime(cRec0.getRevTime());
                cid.authorUuid = uuidUser;
                if (cRec0.getModuleIdx() == MODULE_DEFAULT_IDX) {
                    cid.setModuleUuid(uuidModule);
                } else {
                    cid.setModuleUuid(zModuleArray[cRec0.getModuleIdx()]);
                }
                tmpAdditionalIds.add(cid);
            }
            // CTV 3 ID, if present
//            if (cRec0.getCtv3id() != null) {
//                TtkIdentifierString cids = new TtkIdentifierString();
//                cids.setAuthorityUuid(uuidSourceCtv3);
//                cids.setDenotation(cRec0.getCtv3id());
//                cids.setPathUuid(zPathArray[cRec0.getPathIdx()]);
//                cids.setStatus(Status.ACTIVE);
//                cids.setTime(cRec0.getRevTime());
//                cids.authorUuid = uuidUser;
//                if (cRec0.getModuleIdx() == MODULE_DEFAULT_IDX) {
//                    cids.setModuleUuid(uuidModule);
//                } else {
//                    cids.setModuleUuid(zModuleArray[cRec0.getModuleIdx()]);
//                }
//                tmpAdditionalIds.add(cids);
//            }
//            // SNOMED RT ID, if present
//            if (cRec0.getSnomedrtid() != null) {
//                TtkIdentifierString cids = new TtkIdentifierString();
//                cids.setAuthorityUuid(uuidSourceSnomedRt);
//                cids.setDenotation(cRec0.getSnomedrtid());
//                cids.setPathUuid(zPathArray[cRec0.getPathIdx()]);
//                cids.setStatus(Status.ACTIVE);
//                cids.setTime(cRec0.getRevTime());
//                cids.authorUuid = uuidUser;
//                if (cRec0.getModuleIdx() == MODULE_DEFAULT_IDX) {
//                    cids.setModuleUuid(uuidModule);
//                } else {
//                    cids.setModuleUuid(zModuleArray[cRec0.getModuleIdx()]);
//                }
//                tmpAdditionalIds.add(cids);
//            }
            if (cRec0.getAddedIds() != null) {
                for (Sct1_IdRecord eId : cRec0.getAddedIds()) {
                    tmpAdditionalIds.add(createEIdentifier(eId));
                }
            }
            
            if (additionalIds.containsKey(cRec0.getConSnoId())) {
                for(Sct2_IdCompact sct : additionalIds.get(cRec0.getConSnoId())){
                TtkIdentifierUuid uuid = new TtkIdentifierUuid();
                uuid.setAuthorityUuid(UUID.fromString("2faa9262-8fb2-11db-b606-0800200c9a66")); //GENERATED UUID
                uuid.setDenotation(new UUID(sct.getUuidMsb(), sct.getUuidLsb()));
                uuid.setPathUuid(zPathArray[cRec0.getPathIdx()]);
                uuid.setStatus(Status.ACTIVE);
                uuid.setTime(sct.getTime());
                uuid.setAuthorUuid(uuidUser);
                if (cRec0.getModuleIdx() == MODULE_DEFAULT_IDX) {
                    uuid.setModuleUuid(uuidModule);
                } else {
                    uuid.setModuleUuid(zModuleArray[cRec0.getModuleIdx()]);
                }
                tmpAdditionalIds.add(uuid);
                }
            }
            
            if (tmpAdditionalIds.size() > 0) {
                ca.additionalIds = tmpAdditionalIds;
            } else {
                ca.additionalIds = null;
            }

            ca.setStatus(getStatus(cRec0.getStatus()));
            ca.setPathUuid(zPathArray[cRec0.getPathIdx()]);
            ca.setTime(cRec0.getRevTime()); // long

            int max = conList.size();
            List<TtkConceptAttributesRevision> caRevisions = new ArrayList<TtkConceptAttributesRevision>();
            for (int i = 1; i < max; i++) {
                TtkConceptAttributesRevision rev = new TtkConceptAttributesRevision();
                Sct1_ConRecord cRec = conList.get(i);
                rev.setDefined(cRec.getIsprimitive() == 0 ? true : false);
                rev.setStatus(getStatus(cRec.getStatus()));
                rev.setPathUuid(zPathArray[cRec.getPathIdx()]);
                rev.setTime(cRec.getRevTime());
                rev.authorUuid = uuidUser;
                if (cRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                    rev.setModuleUuid(uuidModule);
                } else {
                    rev.setModuleUuid(zModuleArray[cRec.getModuleIdx()]);
                }
                caRevisions.add(rev);
            }

            if (caRevisions.size() > 0) {
                ca.revisions = caRevisions;
            } else {
                ca.revisions = null;
            }
            ec.setConceptAttributes(ca);
        }

        // ADD DESCRIPTIONS
        if (desList != null) {
            Collections.sort(desList);
            List<TtkDescriptionChronicle> eDesList = new ArrayList<TtkDescriptionChronicle>();
            // long theDesId = Long.MIN_VALUE;
            long theDesMsb = Long.MIN_VALUE;
            long theDesLsb = Long.MIN_VALUE;
            TtkDescriptionChronicle des = null;
            List<TtkDescriptionRevision> revisions = new ArrayList<>();
            for (Sct1_DesRecord dRec : desList) {
                if (dRec.getDesUuidMsb() != theDesMsb || dRec.getDesUuidLsb() != theDesLsb) {
                    // CLOSE OUT OLD RELATIONSHIP
                    if (des != null) {
                        if (revisions.size() > 0) {
                            des.revisions = revisions;
                            revisions = new ArrayList<>();
                        }
                        eDesList.add(des);
                    }

                    // CREATE NEW DESCRIPTION
                    des = new TtkDescriptionChronicle();

                    ArrayList<TtkIdentifier> tmpDesAdditionalIds = new ArrayList<TtkIdentifier>();
                    if (dRec.getDesSnoId() < Long.MAX_VALUE) {
                        TtkIdentifierLong did = new TtkIdentifierLong();
                        did.setAuthorityUuid(uuidSourceSnomedLong);
                        did.setDenotation(dRec.getDesSnoId());
                        did.setPathUuid(zPathArray[dRec.getPathIdx()]);
                        did.setStatus(Status.ACTIVE);
                        did.setTime(dRec.getRevTime());
                        did.authorUuid = uuidUser;
                        if (dRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                            did.setModuleUuid(uuidModule);
                        } else {
                            did.setModuleUuid(zModuleArray[dRec.getModuleIdx()]);
                        }
                        tmpDesAdditionalIds.add(did);
                    }
                    if (dRec.getAddedIds() != null) {
                        for (Sct1_IdRecord eId : dRec.getAddedIds()) {
                            tmpDesAdditionalIds.add(createEIdentifier(eId));
                        }
                    }
                    if (additionalIds.containsKey(dRec.getDesSnoId())) {
                        for(Sct2_IdCompact sct : additionalIds.get(dRec.getDesSnoId())){
                        TtkIdentifierUuid uuid = new TtkIdentifierUuid();
                        uuid.setAuthorityUuid(UUID.fromString("2faa9262-8fb2-11db-b606-0800200c9a66")); //GENERATED UUID
                        uuid.setDenotation(new UUID(sct.getUuidMsb(), sct.getUuidLsb()));
                        uuid.setPathUuid(zPathArray[dRec.getPathIdx()]);
                        uuid.setStatus(Status.ACTIVE);
                        uuid.setTime(sct.getTime());
                        uuid.setAuthorUuid(uuidUser);
                        if (dRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                            uuid.setModuleUuid(uuidModule);
                        } else {
                            uuid.setModuleUuid(zModuleArray[dRec.getModuleIdx()]);
                        }
                        tmpDesAdditionalIds.add(uuid);
                    }
                    }
                    if (tmpDesAdditionalIds.size() > 0) {
                        des.additionalIds = tmpDesAdditionalIds;
                    } else {
                        des.additionalIds = null;
                    }

                    theDesMsb = dRec.getDesUuidMsb();
                    theDesLsb = dRec.getDesUuidLsb();
                    des.setPrimordialComponentUuid(new UUID(theDesMsb, theDesLsb));
                    des.setConceptUuid(theConUUID);
                    des.setText(dRec.getTermText());
                    des.setInitialCaseSignificant(dRec.getCapStatus() == 1 ? true : false);
                    des.setLang(dRec.getLanguageCode());
                    des.setTypeUuid(getSnomedDescriptionType(dRec.getDescriptionType()));
                    des.setStatus(getStatus(dRec.getStatus()));
                    des.setPathUuid(zPathArray[dRec.getPathIdx()]);
                    des.setTime(dRec.getRevTime());
                    des.authorUuid = uuidUser;
                    if (dRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        des.setModuleUuid(uuidModule);
                    } else {
                        des.setModuleUuid(zModuleArray[dRec.getModuleIdx()]);
                    }
                    des.revisions = null;
                } else {
                    TtkDescriptionRevision edv = new TtkDescriptionRevision();
                    edv.setText(dRec.getTermText());
                    edv.setTypeUuid(getSnomedDescriptionType(dRec.getDescriptionType()));
                    edv.setInitialCaseSignificant(dRec.getCapStatus() == 1 ? true : false);
                    edv.setLang(dRec.getLanguageCode());
                    edv.setStatus(getStatus(dRec.getStatus()));
                    edv.setPathUuid(zPathArray[dRec.getPathIdx()]);
                    edv.setTime(dRec.getRevTime());
                    edv.authorUuid = uuidUser;
                    if (dRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        edv.setModuleUuid(uuidModule);
                    } else {
                        edv.setModuleUuid(zModuleArray[dRec.getModuleIdx()]);
                    }
                    revisions.add(edv);
                }
            }
            if (des != null && revisions.size() > 0) {
                des.revisions = revisions;
            }
            eDesList.add(des);
            ec.setDescriptions(eDesList);
        }

        // ADD RELATIONSHIPS
        if (relList != null) {
            Collections.sort(relList);
            List<TtkRelationshipChronicle> eRelList = new ArrayList<>();
            long theRelMsb = Long.MIN_VALUE;
            long theRelLsb = Long.MIN_VALUE;
            TtkRelationshipChronicle rel = null;
            List<TtkRelationshipRevision> revisions = new ArrayList<TtkRelationshipRevision>();
            for (Sct1_RelRecord rRec : relList) {
                if (rRec.getRelUuidMsb() != theRelMsb || rRec.getRelUuidLsb() != theRelLsb) {
                    // CLOSE OUT OLD RELATIONSHIP
                    if (rel != null) {
                        if (revisions.size() > 0) {
                            rel.revisions = revisions;
                            revisions = new ArrayList<TtkRelationshipRevision>();
                        }
                        eRelList.add(rel);
                    }

                    // CREATE NEW RELATIONSHIP
                    rel = new TtkRelationshipChronicle();

                    ArrayList<TtkIdentifier> tmpRelAdditionalIds = new ArrayList<TtkIdentifier>(1);
                    if (rRec.getAddedIds() != null) {
                        for (Sct1_IdRecord eId : rRec.getAddedIds()) {
                            tmpRelAdditionalIds.add(createEIdentifier(eId));
                        }
                    } else if (rRec.getRelSnoId() < Long.MAX_VALUE) {
                        TtkIdentifierLong rid = new TtkIdentifierLong();
                        rid.setAuthorityUuid(uuidSourceSnomedLong);
                        rid.setDenotation(rRec.getRelSnoId());
                        rid.setPathUuid(zPathArray[rRec.getPathIdx()]);
                        rid.setStatus(Status.ACTIVE);
                        rid.setTime(rRec.getRevTime());
                        rid.authorUuid = uuidUser;
                        if (rRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                            rid.setModuleUuid(uuidModule);
                        } else {
                            rid.setModuleUuid(zModuleArray[rRec.getModuleIdx()]);
                        }
                        tmpRelAdditionalIds.add(rid);
                    }
                    if (additionalIds.containsKey(rRec.getRelSnoId())) {
                        for (Sct2_IdCompact sct : additionalIds.get(rRec.getRelSnoId())) {
                            TtkIdentifierUuid uuid = new TtkIdentifierUuid();
                            uuid.setAuthorityUuid(UUID.fromString("2faa9262-8fb2-11db-b606-0800200c9a66")); //GENERATED UUID
                            uuid.setDenotation(new UUID(sct.getUuidMsb(), sct.getUuidLsb()));
                            uuid.setPathUuid(zPathArray[rRec.getPathIdx()]);
                            uuid.setStatus(Status.ACTIVE);
                            uuid.setTime(sct.getTime());
                            uuid.setAuthorUuid(uuidUser);
                            if (rRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                uuid.setModuleUuid(uuidModule);
                            } else {
                                uuid.setModuleUuid(zModuleArray[rRec.getModuleIdx()]);
                            }
                            tmpRelAdditionalIds.add(uuid);
                        }
                    }
                    if (tmpRelAdditionalIds.size() > 0) {
                        rel.additionalIds = tmpRelAdditionalIds;
                    } else {
                        rel.additionalIds = null;
                    }

                    theRelMsb = rRec.getRelUuidMsb();
                    theRelLsb = rRec.getRelUuidLsb();
                    rel.setPrimordialComponentUuid(new UUID(theRelMsb, theRelLsb));
                    rel.setC1Uuid(theConUUID);
                    rel.setC2Uuid(new UUID(rRec.getC2UuidMsb(), rRec.getC2UuidLsb()));
                    rel.setTypeUuid(lookupRoleType(rRec.getRoleTypeIdx()));
                    rel.setRelGroup(rRec.getGroup());
                    rel.setCharacteristicUuid(zRelCharArray[rRec.getCharacteristic()]);
                    rel.setRefinabilityUuid(zRelRefArray[rRec.getRefinability()]);
                    rel.setStatus(getStatus(rRec.getStatus()));
                    rel.setPathUuid(zPathArray[rRec.getPathIdx()]);
                    rel.setTime(rRec.getRevTime());
                    if (rRec.getUserIdx() == USER_SNOROCKET_IDX) {
                        rel.setAuthorUuid(uuidUserSnorocket);
                    } else {
                        rel.setAuthorUuid(uuidUser);
                    }
                    if (rRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        rel.setModuleUuid(uuidModule);
                    } else {
                        rel.setModuleUuid(zModuleArray[rRec.getModuleIdx()]);
                    }
                    rel.revisions = null;
                } else {
                    TtkRelationshipRevision erv = new TtkRelationshipRevision();
                    erv.setTypeUuid(lookupRoleType(rRec.getRoleTypeIdx()));
                    erv.setRelGroup(rRec.getGroup());
                    erv.setCharacteristicUuid(zRelCharArray[rRec.getCharacteristic()]);
                    erv.setRefinabilityUuid(zRelRefArray[rRec.getRefinability()]);
                    erv.setStatus(getStatus(rRec.getStatus()));
                    erv.setPathUuid(zPathArray[rRec.getPathIdx()]);
                    erv.setTime(rRec.getRevTime());
                    if (rRec.getUserIdx() == USER_SNOROCKET_IDX) {
                        erv.setAuthorUuid(uuidUserSnorocket);
                    } else {
                        erv.setAuthorUuid(uuidUser);
                    }
                    if (rRec.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        erv.setModuleUuid(uuidModule);
                    } else {
                        erv.setModuleUuid(zModuleArray[rRec.getModuleIdx()]);
                    }
                    revisions.add(erv);
                }
            }
            if (rel != null && revisions.size() > 0) {
                rel.revisions = revisions;
            }
            eRelList.add(rel);
            ec.setRelationships(eRelList);
        }

        // ADD REFSET INDEX
        if (rsByConList != null && rsByConList.size() > 0) {
            List<UUID> listRefsetUuidMemberUuidForCon = new ArrayList<UUID>();
            List<UUID> listRefsetUuidMemberUuidForDes = new ArrayList<UUID>();
            List<UUID> listRefsetUuidMemberUuidForImage = new ArrayList<UUID>();
            List<UUID> listRefsetUuidMemberUuidForRefsetMember = new ArrayList<UUID>();
            List<UUID> listRefsetUuidMemberUuidForRel = new ArrayList<UUID>();

            Collections.sort(rsByConList);
            int length = rsByConList.size();
            for (int rIdx = 0; rIdx < length; rIdx++) {
                Sct1_RefSetRecord r = rsByConList.get(rIdx);
                if (rIdx < length - 1) {
                    Sct1_RefSetRecord rNext = rsByConList.get(rIdx + 1);
                    if (r.getRefsetUuidMsb() == rNext.getRefsetUuidMsb()
                            && r.getRefsetUuidLsb() == rNext.getRefsetUuidLsb()
                            && r.getRefsetMemberUuidMsb() == rNext.getRefsetMemberUuidMsb()
                            && r.getRefsetMemberUuidLsb() == rNext.getRefsetMemberUuidLsb()) {
                        continue;
                    }
                }

                if (r.getComponentType() == Sct1_RefSetRecord.ComponentType.CONCEPT) {
                    listRefsetUuidMemberUuidForCon.add(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    listRefsetUuidMemberUuidForCon.add(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                } else if (r.getComponentType() == Sct1_RefSetRecord.ComponentType.DESCRIPTION) {
                    listRefsetUuidMemberUuidForDes.add(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    listRefsetUuidMemberUuidForDes.add(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                } else if (r.getComponentType() == Sct1_RefSetRecord.ComponentType.IMAGE) {
                    listRefsetUuidMemberUuidForImage.add(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    listRefsetUuidMemberUuidForImage.add(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                } else if (r.getComponentType() == Sct1_RefSetRecord.ComponentType.MEMBER) {
                    listRefsetUuidMemberUuidForRefsetMember.add(new UUID(r.getRefsetUuidMsb(),
                            r.getRefsetUuidLsb()));
                    listRefsetUuidMemberUuidForRefsetMember.add(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                } else if (r.getComponentType() == Sct1_RefSetRecord.ComponentType.RELATIONSHIP) {
                    listRefsetUuidMemberUuidForRel.add(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    listRefsetUuidMemberUuidForRel.add(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                } else {
                    throw new UnsupportedOperationException("Cannot handle case");
                }
            }

        }
        
        // ADD REFSET MEMBER VALUES
        if (rsByRsList != null && rsByRsList.size() > 0) {
            List<TtkRefexAbstractMemberChronicle<?>> listErm = new ArrayList<>();
            Collections.sort(rsByRsList);

            if (rsByRsList.size() > 100000) {
            UUID tmpUUID = new UUID(cRec0.getConUuidMsb(), cRec0.getConUuidLsb());
            getLog().info(
                        "::: NOTE: concept with MANY refset members = " + rsByRsList.size()
                        + ", concept UUID = " + tmpUUID.toString());
            }
            
            int rsmMax = rsByRsList.size(); // NUMBER OF REFSET MEMBERS
            int rsmIdx = 0;
            long lastRefsetMemberUuidMsb = Long.MAX_VALUE;
            long lastRefsetMemberUuidLsb = Long.MAX_VALUE;
            Sct1_RefSetRecord r = null;
            boolean hasMembersToProcess = false;
            if (rsmIdx < rsmMax) {
                r = rsByRsList.get(rsmIdx++);
                hasMembersToProcess = true;
            }
            while (hasMembersToProcess) {

                if (r.getValueType() == Sct1_RefSetRecord.ValueType.BOOLEAN) {
                    TtkRefexBooleanMemberChronicle tmp = new TtkRefexBooleanMemberChronicle();
                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
                            r.getReferencedComponentUuidLsb()));
                    tmp.setStatus(getStatus(r.getStatus()));
                    tmp.setTime(r.getRevTime());
                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
                    tmp.setBooleanValue(r.isValueBoolean());
                    if (r.getAuthorIdx() != -1) {
                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                    } else {
                        tmp.setAuthorUuid(uuidUser);
                    }
                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        tmp.setModuleUuid(uuidModule);
                    } else {
                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                    }

                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                        r = rsByRsList.get(rsmIdx++);
                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                            // FIRST REVISION
                            List<TtkRefexBooleanRevision> revisionList = new ArrayList<TtkRefexBooleanRevision>();
                            TtkRefexBooleanRevision revision = new TtkRefexBooleanRevision();
                            revision.setBooleanValue(r.isValueBoolean());
                            revision.setStatus(getStatus(r.getStatus()));
                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
                            revision.setTime(r.getRevTime());
                            if (r.getAuthorIdx() != -1) {
                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                            } else {
                                revision.setAuthorUuid(uuidUser);
                            }
                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                revision.setModuleUuid(uuidModule);
                            } else {
                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                            }
                            revisionList.add(revision);

                            boolean checkForMoreVersions = true;
                            do {
                                // SET UP NEXT MEMBER
                                if (rsmIdx < rsmMax) {
                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                                    r = rsByRsList.get(rsmIdx++);
                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                                        revision = new TtkRefexBooleanRevision();
                                        revision.setBooleanValue(r.isValueBoolean());
                                        revision.setStatus(getStatus(r.getStatus()));
                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
                                        revision.setTime(r.getRevTime());
                                        if (r.getAuthorIdx() != -1) {
                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                                        } else {
                                            revision.setAuthorUuid(uuidUser);
                                        }
                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                            revision.setModuleUuid(uuidModule);
                                        } else {
                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                                        }
                                        revisionList.add(revision);
                                    } else {
                                        checkForMoreVersions = false;
                                    }
                                } else {
                                    checkForMoreVersions = false;
                                    hasMembersToProcess = false;
                                }

                            } while (checkForMoreVersions);

                            tmp.setRevisions(revisionList); // ADD REVISIONS
                        }
                    } else {
                        hasMembersToProcess = false;
                    }

                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
                    listErm.add(tmp);
                } else if (r.getValueType() == Sct1_RefSetRecord.ValueType.CONCEPT) {
                    TtkRefexUuidMemberChronicle tmp = new TtkRefexUuidMemberChronicle();
                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
                            r.getReferencedComponentUuidLsb()));
                    tmp.setStatus(getStatus(r.getStatus()));
                    tmp.setTime(r.getRevTime());
                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
                    tmp.setUuid1(new UUID(r.getValueConUuidMsb(), r.getValueConUuidLsb()));
                    if (r.getAuthorIdx() != -1) {
                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                    } else {
                        tmp.setAuthorUuid(uuidUser);
                    }
                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        tmp.setModuleUuid(uuidModule);
                    } else {
                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                    }

                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                        r = rsByRsList.get(rsmIdx++);
                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                            // FIRST REVISION
                            List<TtkRefexUuidRevision> revisionList = new ArrayList<TtkRefexUuidRevision>();
                            TtkRefexUuidRevision revision = new TtkRefexUuidRevision();
                            revision.setUuid1(new UUID(r.getValueConUuidMsb(), r.getValueConUuidLsb()));
                            revision.setStatus(getStatus(r.getStatus()));
                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
                            revision.setTime(r.getRevTime());
                            if (r.getAuthorIdx() != -1) {
                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                            } else {
                                revision.setAuthorUuid(uuidUser);
                            }
                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                revision.setModuleUuid(uuidModule);
                            } else {
                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                            }
                            revisionList.add(revision);

                            boolean checkForMoreVersions = true;
                            do {
                                // SET UP NEXT MEMBER
                                if (rsmIdx < rsmMax) {
                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                                    r = rsByRsList.get(rsmIdx++);
                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                                        revision = new TtkRefexUuidRevision();
                                        revision.setUuid1(new UUID(r.getValueConUuidMsb(),
                                                r.getValueConUuidLsb()));
                                        revision.setStatus(getStatus(r.getStatus()));
                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
                                        revision.setTime(r.getRevTime());
                                        if (r.getAuthorIdx() != -1) {
                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                                        } else {
                                            revision.setAuthorUuid(uuidUser);
                                        }
                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                            revision.setModuleUuid(uuidModule);
                                        } else {
                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                                        }
                                        revisionList.add(revision);
                                    } else {
                                        checkForMoreVersions = false;
                                    }
                                } else {
                                    checkForMoreVersions = false;
                                    hasMembersToProcess = false;
                                }

                            } while (checkForMoreVersions);

                            tmp.setRevisions(revisionList); // ADD REVISIONS
                        }
                    } else {
                        hasMembersToProcess = false;
                    }

                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
                    listErm.add(tmp);
                } else if (r.getValueType() == Sct1_RefSetRecord.ValueType.INTEGER) {
                    TtkRefexIntMemberChronicle tmp = new TtkRefexIntMemberChronicle();
                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
                            r.getReferencedComponentUuidLsb()));
                    tmp.setStatus(getStatus(r.getStatus()));
                    tmp.setTime(r.getRevTime());
                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
                    tmp.setIntValue(r.getValueInt());
                    if (r.getAuthorIdx() != -1) {
                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                    } else {
                        tmp.setAuthorUuid(uuidUser);
                    }
                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        tmp.setModuleUuid(uuidModule);
                    } else {
                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                    }

                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                        r = rsByRsList.get(rsmIdx++);
                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                            // FIRST REVISION
                            List<TtkRefexIntRevision> revisionList = new ArrayList<>();
                            TtkRefexIntRevision revision = new TtkRefexIntRevision();
                            revision.setIntValue(r.getValueInt());
                            revision.setStatus(getStatus(r.getStatus()));
                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
                            revision.setTime(r.getRevTime());
                            if (r.getAuthorIdx() != -1) {
                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                            } else {
                                revision.setAuthorUuid(uuidUser);
                            }
                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                revision.setModuleUuid(uuidModule);
                            } else {
                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                            }
                            revisionList.add(revision);

                            boolean checkForMoreVersions = true;
                            do {
                                // SET UP NEXT MEMBER
                                if (rsmIdx < rsmMax) {
                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                                    r = rsByRsList.get(rsmIdx++);
                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                                        revision = new TtkRefexIntRevision();
                                        revision.setIntValue(r.getValueInt());
                                        revision.setStatus(getStatus(r.getStatus()));
                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
                                        revision.setTime(r.getRevTime());
                                        if (r.getAuthorIdx() != -1) {
                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                                        } else {
                                            revision.setAuthorUuid(uuidUser);
                                        }
                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                            revision.setModuleUuid(uuidModule);
                                        } else {
                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                                        }
                                        revisionList.add(revision);
                                    } else {
                                        checkForMoreVersions = false;
                                    }
                                } else {
                                    checkForMoreVersions = false;
                                    hasMembersToProcess = false;
                                }

                            } while (checkForMoreVersions);

                            tmp.setRevisions(revisionList); // ADD REVISIONS
                        }
                    } else {
                        hasMembersToProcess = false;
                    }

                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
                    listErm.add(tmp);
                } else if (r.getValueType() == Sct1_RefSetRecord.ValueType.STRING) {
                    // :DEBUG:
                    //                    UUID debugUuid = new UUID(r.refsetMemberUuidMsb, r.refsetMemberUuidLsb);
                    //                    if (debugUuid.compareTo(debugUuid02) == 0) 
                    //                        System.out.println(":DEBUG:");

                    TtkRefexStringMemberChronicle tmp = new TtkRefexStringMemberChronicle();
                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
                            r.getReferencedComponentUuidLsb()));
                    tmp.setStatus(getStatus(r.getStatus()));
                    tmp.setTime(r.getRevTime());
                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
                    tmp.setString1(r.getValueString1());
                    if (r.getAuthorIdx() != -1) {
                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                    } else {
                        tmp.setAuthorUuid(uuidUser);
                    }
                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        tmp.setModuleUuid(uuidModule);
                    } else {
                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                    }

                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                        r = rsByRsList.get(rsmIdx++);
                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                            // FIRST REVISION
                            List<TtkRefexStringRevision> revisionList = new ArrayList<TtkRefexStringRevision>();
                            TtkRefexStringRevision revision = new TtkRefexStringRevision();
                            revision.setString1(r.getValueString1());
                            revision.setStatus(getStatus(r.getStatus()));
                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
                            revision.setTime(r.getRevTime());
                            if (r.getAuthorIdx() != -1) {
                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                            } else {
                                revision.setAuthorUuid(uuidUser);
                            }
                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                revision.setModuleUuid(uuidModule);
                            } else {
                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                            }
                            revisionList.add(revision);

                            boolean checkForMoreVersions = true;
                            do {
                                // SET UP NEXT MEMBER
                                if (rsmIdx < rsmMax) {
                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                                    r = rsByRsList.get(rsmIdx++);
                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                                        revision = new TtkRefexStringRevision();
                                        revision.setString1(r.getValueString1());
                                        revision.setStatus(getStatus(r.getStatus()));
                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
                                        revision.setTime(r.getRevTime());
                                        if (r.getAuthorIdx() != -1) {
                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                                        } else {
                                            revision.setAuthorUuid(uuidUser);
                                        }
                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                            revision.setModuleUuid(uuidModule);
                                        } else {
                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                                        }
                                        revisionList.add(revision);
                                    } else {
                                        checkForMoreVersions = false;
                                    }
                                } else {
                                    checkForMoreVersions = false;
                                    hasMembersToProcess = false;
                                }

                            } while (checkForMoreVersions);

                            tmp.setRevisions(revisionList); // ADD REVISIONS
                        }
                    } else {
                        hasMembersToProcess = false;
                    }

                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
                    listErm.add(tmp);
                }
//                 else if (r.getValueType() == Sct1_RefSetRecord.ValueType.STRING_STRING) {
//                    // :DEBUG:
//                    //                    UUID debugUuid = new UUID(r.refsetMemberUuidMsb, r.refsetMemberUuidLsb);
//                    //                    if (debugUuid.compareTo(debugUuid02) == 0) 
//                    //                        System.out.println(":DEBUG:");
//
//                    ERefsetStrStrMember tmp = new ERefsetStrStrMember();
//                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
//                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
//                            r.getRefsetMemberUuidLsb()));
//                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
//                            r.getReferencedComponentUuidLsb()));
//                    tmp.setStatus(getStatus(r.getStatus()));
//                    tmp.setTime(r.getRevTime());
//                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
//                    tmp.setString1(r.getValueString1());
//                    tmp.setString2(r.getValueString2());
//                    if (r.getAuthorIdx() != -1) {
//                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
//                    } else {
//                        tmp.setAuthorUuid(uuidUser);
//                    }
//                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
//                        tmp.setModuleUuid(uuidModule);
//                    } else {
//                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
//                    }
//
//                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
//                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
//                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
//                        r = rsByRsList.get(rsmIdx++);
//                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
//                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
//                            // FIRST REVISION
//                            List<TkRefsetStrStrRevision> revisionList = new ArrayList<TkRefsetStrStrRevision>();
//                            ERefsetStrStrRevision revision = new ERefsetStrStrRevision();
//                            revision.setString1(r.getValueString1());
//                            revision.setString2(r.getValueString2());
//                            revision.setStatus(getStatus(r.getStatus()));
//                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
//                            revision.setTime(r.getRevTime());
//                            if (r.getAuthorIdx() != -1) {
//                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
//                            } else {
//                                revision.setAuthorUuid(uuidUser);
//                            }
//                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
//                                revision.setModuleUuid(uuidModule);
//                            } else {
//                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
//                            }
//                            revisionList.add(revision);
//
//                            boolean checkForMoreVersions = true;
//                            do {
//                                // SET UP NEXT MEMBER
//                                if (rsmIdx < rsmMax) {
//                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
//                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
//                                    r = rsByRsList.get(rsmIdx++);
//                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
//                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
//                                        revision = new ERefsetStrStrRevision();
//                                        revision.setString1(r.getValueString1());
//                                        revision.setString2(r.getValueString2());
//                                        revision.setStatus(getStatus(r.getStatus()));
//                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
//                                        revision.setTime(r.getRevTime());
//                                        if (r.getAuthorIdx() != -1) {
//                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
//                                        } else {
//                                            revision.setAuthorUuid(uuidUser);
//                                        }
//                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
//                                            revision.setModuleUuid(uuidModule);
//                                        } else {
//                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
//                                        }
//                                        revisionList.add(revision);
//                                    } else {
//                                        checkForMoreVersions = false;
//                                    }
//                                } else {
//                                    checkForMoreVersions = false;
//                                    hasMembersToProcess = false;
//                                }
//
//                            } while (checkForMoreVersions);
//
//                            tmp.setRevisions(revisionList); // ADD REVISIONS
//                        }
//                    } else {
//                        hasMembersToProcess = false;
//                    }
//
//                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
//                    listErm.add(tmp);
//                }
                else if (r.getValueType() == Sct1_RefSetRecord.ValueType.C_FLOAT) {
                    TtkRefexUuidFloatMemberChronicle tmp = new TtkRefexUuidFloatMemberChronicle();
                    tmp.setAssemblageUuid(new UUID(r.getRefsetUuidMsb(), r.getRefsetUuidLsb()));
                    tmp.setPrimordialComponentUuid(new UUID(r.getRefsetMemberUuidMsb(),
                            r.getRefsetMemberUuidLsb()));
                    tmp.setReferencedComponentUuid(new UUID(r.getReferencedComponentUuidMsb(),
                            r.getReferencedComponentUuidLsb()));
                    tmp.setStatus(getStatus(r.getStatus()));
                    tmp.setTime(r.getRevTime());
                    tmp.setPathUuid(zPathArray[r.getPathIdx()]);
                    tmp.setUuid1(new UUID(r.getValueConUuidMsb(), r.getValueConUuidLsb()));
                    tmp.setFloatValue(r.getValueFloat());
                    if (r.getAuthorIdx() != -1) {
                        tmp.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                    } else {
                        tmp.setAuthorUuid(uuidUser);
                    }
                    if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                        tmp.setModuleUuid(uuidModule);
                    } else {
                        tmp.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                    }

                    if (rsmIdx < rsmMax) { // CHECK REVISIONS
                        lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                        lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                        r = rsByRsList.get(rsmIdx++);
                        if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                            // FIRST REVISION
                            List<TtkRefexUuidFloatRevision> revisionList = new ArrayList<>();
                            TtkRefexUuidFloatRevision revision = new TtkRefexUuidFloatRevision();
                            revision.setUuid1(new UUID(r.getValueConUuidMsb(), r.getValueConUuidLsb()));
                            revision.setFloat1(r.getValueFloat());
                            revision.setStatus(getStatus(r.getStatus()));
                            revision.setPathUuid(zPathArray[r.getPathIdx()]);
                            revision.setTime(r.getRevTime());
                            if (r.getAuthorIdx() != -1) {
                                revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                            } else {
                                revision.setAuthorUuid(uuidUser);
                            }
                            if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                revision.setModuleUuid(uuidModule);
                            } else {
                                revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                            }
                            revisionList.add(revision);

                            boolean checkForMoreVersions = true;
                            do {
                                // SET UP NEXT MEMBER
                                if (rsmIdx < rsmMax) {
                                    lastRefsetMemberUuidMsb = r.getRefsetMemberUuidMsb();
                                    lastRefsetMemberUuidLsb = r.getRefsetMemberUuidLsb();
                                    r = rsByRsList.get(rsmIdx++);
                                    if (r.getRefsetMemberUuidMsb() == lastRefsetMemberUuidMsb
                                            && r.getRefsetMemberUuidLsb() == lastRefsetMemberUuidLsb) {
                                        revision = new TtkRefexUuidFloatRevision();
                                        revision.setUuid1(new UUID(r.getValueConUuidMsb(), r.getValueConUuidLsb()));
                                        revision.setFloat1(r.getValueFloat());
                                        revision.setStatus(getStatus(r.getStatus()));
                                        revision.setPathUuid(zPathArray[r.getPathIdx()]);
                                        revision.setTime(r.getRevTime());
                                        if (r.getAuthorIdx() != -1) {
                                            revision.setAuthorUuid(zAuthorUuidArray[r.getAuthorIdx()]);
                                        } else {
                                            revision.setAuthorUuid(uuidUser);
                                        }
                                        if (r.getModuleIdx() == MODULE_DEFAULT_IDX) {
                                            revision.setModuleUuid(uuidModule);
                                        } else {
                                            revision.setModuleUuid(zModuleArray[r.getModuleIdx()]);
                                        }
                                        revisionList.add(revision);
                                    } else {
                                        checkForMoreVersions = false;
                                    }
                                } else {
                                    checkForMoreVersions = false;
                                    hasMembersToProcess = false;
                                }

                            } while (checkForMoreVersions);

                            tmp.setRevisions(revisionList); // ADD REVISIONS
                        }
                    } else {
                        hasMembersToProcess = false;
                    }

                    // :NYI: tmp.setAdditionalIdComponents(additionalIdComponents);
                    listErm.add(tmp);
                }else {
                    throw new UnsupportedOperationException("Cannot handle case");
                }

            }

            ec.setRefsetMembers(listErm);
            //            if (conceptsToWatchMap.containsKey(ec.primordialUuid)) {
            //                getLog().info("Found watch concept after adding refset members: "
            //                        + ec);
            //            }
        }

        try {
            ec.writeExternal(dos);
//            if (theConUUID.compareTo(debugUuid01) == 0) {
//                getLog().info(":DEBUG: "  + ec);
//            }

            countEConWritten++;
            if (countEConWritten % 50000 == 0) {
                getLog().info("  ... econcepts written " + countEConWritten);
            }
        } catch (IOException e) {
            getLog().info(e);
        }

    }

    private Sct1_ConRecord readNextCon(ObjectInputStream ois, ArrayList<Sct1_ConRecord> conList,
            Sct1_ConRecord conNext)
            throws MojoFailureException {
        conList.clear();
        if (conNext != null) {
            conList.add(conNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_ConRecord) {
                    conNext = (Sct1_ConRecord) obj;
                    conList.add(conNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextCon()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextCon()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_ConRecord) {
                    Sct1_ConRecord rec = (Sct1_ConRecord) obj;
                    if (rec.getConUuidMsb() == conNext.getConUuidMsb()
                            && rec.getConUuidLsb() == conNext.getConUuidLsb()) {
                        conList.add(rec);
                    } else {
                        conNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            conNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextCon()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextCon()");
        }

        return conNext; // first record of next concept id
    }

    private Sct1_DesRecord readNextDes(ObjectInputStream ois,
            ArrayList<Sct1_DesRecord> desList,
            Sct1_DesRecord desNext)
            throws MojoFailureException {
        desList.clear();
        if (desNext != null) {
            desList.add(desNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_DesRecord) {
                    desNext = (Sct1_DesRecord) obj;
                    desList.add(desNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextDes()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextDes()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_DesRecord) {
                    Sct1_DesRecord rec = (Sct1_DesRecord) obj;
                    // rec.conSnoId == desNext.conSnoId
                    if (rec.getConUuidMsb() == desNext.getConUuidMsb()
                            && rec.getConUuidLsb() == desNext.getConUuidLsb()) {
                        desList.add(rec);
                    } else {
                        desNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            desNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextDes()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextDes()");
        }

        return desNext; // first record of next concept id
    }

    private Sct1_RelRecord readNextRel(ObjectInputStream ois, ArrayList<Sct1_RelRecord> relList,
            Sct1_RelRecord relNext)
            throws MojoFailureException {
        relList.clear();
        if (relNext != null) {
            relList.add(relNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RelRecord) {
                    relNext = (Sct1_RelRecord) obj;
                    relList.add(relNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextRel()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextRel()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RelRecord) {
                    Sct1_RelRecord rec = (Sct1_RelRecord) obj;
                    if (rec.getC1UuidMsb() == relNext.getC1UuidMsb() && rec.getC1UuidLsb() == relNext.getC1UuidLsb()) {
                        relList.add(rec);
                    } else {
                        relNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            relNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextRel()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextRel()");
        }

        return relNext; // first record of next concept id
    }

    private Sct1_RelDestRecord readNextRelDest(ObjectInputStream ois,
            ArrayList<Sct1_RelDestRecord> relDestList, Sct1_RelDestRecord relDestNext)
            throws MojoFailureException {
        relDestList.clear();
        if (relDestNext != null) {
            relDestList.add(relDestNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RelDestRecord) {
                    relDestNext = (Sct1_RelDestRecord) obj;
                    relDestList.add(relDestNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextRelDest()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextRelDest()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RelDestRecord) {
                    Sct1_RelDestRecord rec = (Sct1_RelDestRecord) obj;
                    // rec.c2SnoId == relDestNext.c2SnoId
                    if (rec.getC2UuidMsb() == relDestNext.getC2UuidMsb()
                            && rec.getC2UuidLsb() == relDestNext.getC2UuidLsb()) {
                        relDestList.add(rec);
                    } else {
                        relDestNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            relDestNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextRelDest()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextRelDest()");
        }

        return relDestNext; // first record of next concept id
    }

    private Sct1_RefSetRecord readNextRsByCon(ObjectInputStream ois,
            ArrayList<Sct1_RefSetRecord> rsByConList, Sct1_RefSetRecord rsByConNext)
            throws MojoFailureException {
        rsByConList.clear();
        if (rsByConNext != null) {
            rsByConList.add(rsByConNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RefSetRecord) {
                    rsByConNext = (Sct1_RefSetRecord) obj;
                    rsByConList.add(rsByConNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextRsByCon()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextRsByCon()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RefSetRecord) {
                    Sct1_RefSetRecord rec = (Sct1_RefSetRecord) obj;
                    if (rec.getConUuidMsb() == rsByConNext.getConUuidMsb()
                            && rec.getConUuidLsb() == rsByConNext.getConUuidLsb()) {
                        rsByConList.add(rec);
                    } else {
                        rsByConNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            rsByConNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextRsByCon()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextRsByCon()");
        }

        return rsByConNext; // first record of next concept id
    }

    private Sct1_RefSetRecord readNextRsByRs(ObjectInputStream ois,
            ArrayList<Sct1_RefSetRecord> rsByRsList, Sct1_RefSetRecord rsByRsNext)
            throws MojoFailureException {
        rsByRsList.clear();
        if (rsByRsNext != null) {
            rsByRsList.add(rsByRsNext);
        } else {
            try { // CHECK FOR FIRST RECORD SITUATION
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RefSetRecord) {
                    rsByRsNext = (Sct1_RefSetRecord) obj;
                    rsByRsList.add(rsByRsNext);
                } else {
                    return null;
                }
            } catch (EOFException ex) {
                return null;
            } catch (IOException e) {
                getLog().info(e);
                throw new MojoFailureException("IO Exception - readNextRsByRs()");
            } catch (ClassNotFoundException e) {
                getLog().info(e);
                throw new MojoFailureException("ClassNotFoundException - readNextRsByRs()");
            }
        }

        try {
            boolean notDone = true;
            while (notDone) {
                Object obj = ois.readUnshared();
                if (obj instanceof Sct1_RefSetRecord) {
                    Sct1_RefSetRecord rec = (Sct1_RefSetRecord) obj;
                    if (rec.getRefsetUuidMsb() == rsByRsNext.getRefsetUuidMsb()
                            && rec.getRefsetUuidLsb() == rsByRsNext.getRefsetUuidLsb()) {
                        rsByRsList.add(rec);
                    } else {
                        rsByRsNext = rec;
                        notDone = false;
                    }
                }
            }
        } catch (EOFException ex) {
            rsByRsNext = null;
            return null; // end reached, no more records
        } catch (IOException e) {
            getLog().info(e);
            throw new MojoFailureException("IO Exception -- readNextRsByRs()");
        } catch (ClassNotFoundException e) {
            getLog().info(e);
            throw new MojoFailureException("ClassNotFoundException -- readNextRsByRs()");
        }

        return rsByRsNext; // first record of next concept id
    }

    private void setupUuids() throws MojoFailureException {
        try {
            uuidPathWbAuxStr = "2faa9260-8fb2-11db-b606-0800200c9a66";
            uuidPathWbAux = UUID.fromString(uuidPathWbAuxStr);
            uuidDescPrefTerm = UUID.fromString("d8e3b37d-7c11-33ef-b1d0-8769e2264d44");
            uuidDescFullSpec = UUID.fromString("5e1fe940-8faf-11db-b606-0800200c9a66");
            uuidWbAuxIsa = UUID.fromString("46bccdc4-8fb6-11db-b606-0800200c9a66");

            uuidPathSnomedInferred = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    "SNOMED Core Inferred");
            uuidPathSnomedInferredStr = uuidPathSnomedInferred.toString();
            getLog().info("SNOMED Core Inferred = " + uuidPathSnomedInferredStr);

            uuidPathSnomedStated = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    "SNOMED Core Stated");
            uuidPathSnomedStatedStr = uuidPathSnomedStated.toString();

            uuidStatedDescFs = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidPathWbAux + uuidDescFullSpec.toString() + "SNOMED Core Stated");

            uuidStatedDescPt = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidWbAuxIsa + uuidDescPrefTerm.toString() + "SNOMED Core Stated");

            uuidStatedRel = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidWbAuxIsa + uuidStatedDescFs.toString() + uuidStatedDescPt.toString());

            uuidInferredDescFs = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidPathWbAux + uuidDescFullSpec.toString() + "SNOMED Core Inferred");

            uuidInferredDescPt = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidWbAuxIsa + uuidDescPrefTerm.toString() + "SNOMED Core Inferred");

            uuidInferredRel = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC,
                    uuidWbAuxIsa + uuidInferredDescFs.toString() + uuidInferredDescPt.toString());

//            uuidSourceCtv3 = ArchitectonicAuxiliary.Concept.CTV3_ID.getUids().iterator().next();
//            uuidSourceSnomedRt = ArchitectonicAuxiliary.Concept.SNOMED_RT_ID.getUids().iterator().next();
            uuidSourceSnomedLong = IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid();

            getLog().info("SNOMED Core Stated   = " + uuidPathSnomedStatedStr);
            getLog().info("  ... Stated rel     = " + uuidStatedRel.toString());

            getLog().info("SNOMED Core Inferred = " + uuidPathSnomedInferredStr);
            getLog().info("  ... Inferred rel   = " + uuidInferredRel.toString());

            getLog().info("SNOMED integer id UUID = " + uuidSourceSnomedLong);
//            getLog().info("SNOMED CTV3 id UUID    = " + uuidSourceCtv3);
//            getLog().info("SNOMED RT id UUID      = " + uuidSourceSnomedRt);

        } catch (Exception e2) {
            getLog().info(e2);
            throw new MojoFailureException("FAILED: SNOMED Core Stated/Inferred Path", e2);
        }
    }

    private List<List<ARFFile>> getArfFiles(String wDir, String subDir, String arfDir,
            String prefix, String postfix)
            throws MojoFailureException {

        List<List<ARFFile>> listOfDirs = new ArrayList<List<ARFFile>>();
        if (arfDir == null) {
            return listOfDirs;
        }

        ArrayList<ARFFile> listOfFiles = new ArrayList<ARFFile>();

        getLog().info(prefix.toUpperCase() + wDir + subDir + arfDir);

        File f1 = new File(new File(wDir, subDir), arfDir);
        ArrayList<File> fv = new ArrayList<File>();
        listFilesRecursive(fv, f1, prefix, postfix);

        File[] files = new File[0];
        files = fv.toArray(files);
        Arrays.sort(files);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (inputFilters == null || inputFilters.length == 0) {
                    return true;
                } else {
                    for (String filter : inputFilters) {
                        if (pathname.getAbsolutePath().replace(File.separatorChar, '/').matches(filter)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };

        for (File f2 : files) {

            if (filter.accept(f2)) {

                listOfFiles.add(new ARFFile(f2));
                getLog().info("    FILE : " + f2.getParent() + FILE_SEPARATOR + f2.getName());
            }

        }

        listOfDirs.add(listOfFiles);
        return listOfDirs;
    }

    private List<List<SCTFile>> getSctFiles(String wDir, String subDir, Sct1Dir[] inDirs,
            String prefix, String postfix)
            throws MojoFailureException {

        List<List<SCTFile>> listOfDirs = new ArrayList<List<SCTFile>>();
        for (Sct1Dir sctDir : inDirs) {
            ArrayList<SCTFile> listOfFiles = new ArrayList<SCTFile>();

            getLog().info(
                    String.format("%1$s (%2$s%3$s%4$s) ", prefix.toUpperCase(), wDir, subDir,
                    sctDir.getDirectoryName()));

            File f1 = new File(new File(wDir, subDir), sctDir.getDirectoryName());
            ArrayList<File> fv = new ArrayList<File>();
            listFilesRecursive(fv, f1, "sct1_" + prefix, postfix);

            File[] files = new File[0];
            files = fv.toArray(files);
            Arrays.sort(files);

            FileFilter filter = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    if (inputFilters == null || inputFilters.length == 0) {
                        return true;
                    } else {
                        for (String filter : inputFilters) {
                            if (pathname.getAbsolutePath().replace(File.separatorChar, '/').matches(filter)) {
                                return true;
                            }
                        }
                    }
                    return false;
                }
            };

            for (File f2 : files) {

                if (filter.accept(f2)) {
                    // ADD SCTFile Entry
                    String revDate = getFileRevDate(f2);

                    try {
                        if (inDateRange(revDate)) {

                            SCTFile fo = new SCTFile(f2, wDir, subDir, revDate, sctDir);
                            listOfFiles.add(fo);
                            getLog().info(
                                    "::: FILE : " + f2.getName() + " " + revDate + " hasSnomedId="
                                    + fo.hasStatedSctRelId + " doCrossMap="
                                    + fo.mapSctIdInferredToStated);
                        }
                    } catch (ParseException e) {
                        getLog().info(e);
                        getLog().info(
                                "::: Date format missing or not supported : " + f2.getName() + " "
                                + revDate);
                    }
                }

            }

            listOfDirs.add(listOfFiles);
        }
        return listOfDirs;
    }

    boolean inDateRange(String revDateStr) throws ParseException {
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        Date revDate = formatter.parse(revDateStr);

        if (dateStartObj != null && revDate.compareTo(dateStartObj) < 0) {
            return false; // precedes start date
        }
        if (dateStopObj != null && revDate.compareTo(dateStopObj) > 0) {
            return false; // after end date
        }
        return true;
    }

    public String getDateStart() {
        return this.dateStart;
    }

    public void setDateStart(String sStart) throws MojoFailureException {
        this.dateStart = sStart;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        try {
            this.dateStartObj = formatter.parse(sStart + " 00:00:00");
        } catch (ParseException e) {
            getLog().info(e);
            throw new MojoFailureException("SimpleDateFormat yyyy.MM.dd dateStart parse error: "
                    + sStart);
        }
        getLog().info("::: START DATE (INCLUSIVE) " + this.dateStart);
    }

    public String getDateStop() {
        return this.dateStop;
    }

    public void setDateStop(String sStop) throws MojoFailureException {
        this.dateStop = sStop;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        try {
            this.dateStopObj = formatter.parse(sStop + " 23:59:59");
        } catch (ParseException e) {
            getLog().info(e);
            throw new MojoFailureException("SimpleDateFormat yyyy.MM.dd dateStop parse error: "
                    + sStop);
        }
        getLog().info(":::  STOP DATE (INCLUSIVE) " + this.dateStop);
    }

    /*
     * ORDER: CONCEPTID CONCEPTSTATUS FULLYSPECIFIEDNAME CTV3ID SNOMEDID ISPRIMITIVE
     *
     * KEEP: CONCEPTID CONCEPTSTATUS ISPRIMITIVE
     *
     * IGNORE: FULLYSPECIFIEDNAME CTV3ID SNOMEDID
     */
    private void processConceptsFiles(String wDir, List<List<SCTFile>> sctv, boolean ctv3idTF,
            boolean snomedrtTF, ObjectOutputStream oos)
            throws Exception {
        int count1, count2; // records in arrays 1 & 2
        String fName1, fName2; // file pathIdx name
        int pathID;
        long revTime;
        Sct1_ConRecord[] a1, a2, a3 = null;

        getLog().info("START CONCEPTS PROCESSING...");

        Iterator<List<SCTFile>> dit = sctv.iterator(); // Directory Iterator
        while (dit.hasNext()) {
            List<SCTFile> fl = dit.next(); // File List
            Iterator<SCTFile> fit = fl.iterator(); // File Iterator
            if (fit.hasNext() == false) {
                continue;
            }

            // READ file1 as MASTER FILE
            SCTFile f1 = fit.next();
            fName1 = f1.file.getPath();
            revTime = f1.zRevTime;
            pathID = f1.pathIdx;

            count1 = countFileLines(fName1);
            getLog().info("BASE FILE:  " + count1 + " records, " + fName1);
            a1 = new Sct1_ConRecord[count1];
            parseConcepts(fName1, a1, count1, ctv3idTF, snomedrtTF);
            writeConcepts(oos, a1, count1, revTime, pathID);

            while (fit.hasNext()) {
                // SETUP CURRENT CONCEPTS INPUT FILE
                SCTFile f2 = fit.next();
                fName2 = f2.file.getPath();
                revTime = f2.zRevTime;
                pathID = f2.pathIdx;

                count2 = countFileLines(fName2);
                getLog().info("Counted: " + count2 + " records, " + fName2);

                // Parse in file2
                a2 = new Sct1_ConRecord[count2];
                parseConcepts(fName2, a2, count2, ctv3idTF, snomedrtTF);

                int r1 = 0, r2 = 0, r3 = 0; // reset record indices
                int nSame = 0, nMod = 0, nAdd = 0, nDrop = 0; // counters
                a3 = new Sct1_ConRecord[count2]; // max3
                while ((r1 < count1) && (r2 < count2)) {

                    switch (compareConcept(a1[r1], a2[r2])) {
                        case 1: // SAME CONCEPT, skip to next
                            r1++;
                            r2++;
                            nSame++;
                            break;

                        case 2: // MODIFIED CONCEPT
                            // Write history
                            a2[r2].setPathIdx(pathID);
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);
                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nMod++;
                            break;

                        case 3: // ADDED CONCEPT
                            // Write history
                            a2[r2].setPathIdx(pathID);
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // Hold pointer to append to master
                            a3[r3] = a2[r2];
                            r2++;
                            r3++;
                            nAdd++;
                            break;

                        case 4: // DROPPED CONCEPT
                            // see ArchitectonicAuxiliary.getStatusFromId()
                            if (a1[r1].getStatus() != 1) { // if not RETIRED
                                a1[r1].setStatus(1); // set to RETIRED
                                a1[r1].setPathIdx(pathID);
                                a1[r1].setRevTime(revTime);
                                oos.writeUnshared(a1[r1]);
                            }
                            r1++;
                            nDrop++;
                            break;

                    }
                } // WHILE (NOT END OF EITHER A1 OR A2)

                // NOT MORE TO COMPARE, HANDLE REMAINING CONCEPTS
                if (r1 < count1) {
                    getLog().info("ERROR: MISSED CONCEPT RECORDS r1 < count1");
                }

                if (r2 < count2) {
                    while (r2 < count2) { // ADD CONCEPT REMAINING INPUT
                        // Write history
                        a2[r2].setPathIdx(pathID);
                        a2[r2].setRevTime(revTime);
                        oos.writeUnshared(a2[r2]);

                        // Add to append array
                        a3[r3] = a2[r2];
                        nAdd++;
                        r2++;
                        r3++;
                    }
                }

                // Check counter numbers to master and input file record counts
                countCheck(count1, count2, nSame, nMod, nAdd, nDrop);

                // SETUP NEW MASTER ARRAY
                a2 = new Sct1_ConRecord[count1 + nAdd];
                r2 = 0;
                while (r2 < count1) {
                    a2[r2] = a1[r2];
                    r2++;
                }
                r3 = 0;
                while (r3 < nAdd) {
                    a2[r2] = a3[r3];
                    r2++;
                    r3++;
                }
                count1 = count1 + nAdd;
                a1 = a2;
                Arrays.sort(a1);

            } // WHILE (EACH CONCEPTS INPUT FILE)
        } // WHILE (EACH CONCEPTS DIRECTORY) *
    }

    private void processDescriptionsFiles(String wDir, List<List<SCTFile>> sctv,
            ObjectOutputStream oos)
            throws Exception {
        int count1, count2; // records in arrays 1 & 2
        String fName1, fName2; // file pathIdx name
        int pathID;
        long revTime;
        Sct1_DesRecord[] a1, a2, a3 = null;

        getLog().info("START DESCRIPTIONS PROCESSING...");

        Iterator<List<SCTFile>> dit = sctv.iterator(); // Directory Iterator
        while (dit.hasNext()) {
            List<SCTFile> fl = dit.next(); // File List
            Iterator<SCTFile> fit = fl.iterator(); // File Iterator
            if (fit.hasNext() == false) {
                continue;
            }

            // READ file1 as MASTER FILE
            SCTFile f1 = fit.next();
            fName1 = f1.file.getPath();
            revTime = f1.zRevTime;
            pathID = f1.pathIdx;

            count1 = countFileLines(fName1);
            getLog().info("BASE FILE:  " + count1 + " records, " + fName1);
            a1 = new Sct1_DesRecord[count1];
            parsTtkDescriptionRevisions(fName1, a1, count1);
            writeTtkDescriptionRevisions(oos, a1, count1, revTime, pathID);

            while (fit.hasNext()) {
                // SETUP CURRENT CONCEPTS INPUT FILE
                SCTFile f2 = fit.next();
                fName2 = f2.file.getPath();
                revTime = f2.zRevTime;
                pathID = f2.pathIdx;

                count2 = countFileLines(fName2);
                getLog().info("Counted: " + count2 + " records, " + fName2);

                // Parse in file2
                a2 = new Sct1_DesRecord[count2];
                parsTtkDescriptionRevisions(fName2, a2, count2);

                int r1 = 0, r2 = 0, r3 = 0; // reset record indices
                int nSame = 0, nMod = 0, nAdd = 0, nDrop = 0; // counters
                a3 = new Sct1_DesRecord[count2];
                while ((r1 < count1) && (r2 < count2)) {

                    switch (comparTtkDescriptionRevision(a1[r1], a2[r2])) {
                        case 1: // SAME DESCRIPTION, skip to next
                            r1++;
                            r2++;
                            nSame++;
                            break;

                        case 2: // MODIFIED DESCRIPTION
                            // Write history
                            a2[r2].setPathIdx(pathID);
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nMod++;
                            break;

                        case 3: // ADDED DESCRIPTION
                            // Write history
                            a2[r2].setPathIdx(pathID);
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // Hold pointer to append to master
                            a3[r3] = a2[r2];
                            r2++;
                            r3++;
                            nAdd++;
                            break;

                        case 4: // DROPPED DESCRIPTION
                            // see ArchitectonicAuxiliary.getStatusFromId()
                            if (a1[r1].getStatus() != 1) { // if not RETIRED
                                a1[r1].setStatus(1); // set to RETIRED
                                a1[r1].setPathIdx(pathID);
                                a1[r1].setRevTime(revTime);
                                oos.writeUnshared(a1[r1]);
                            }
                            r1++;
                            nDrop++;
                            break;

                    }
                } // WHILE (NOT END OF EITHER A1 OR A2)

                // NOT MORE TO COMPARE, HANDLE REMAINING CONCEPTS
                if (r1 < count1) {
                    getLog().info("ERROR: MISSED DESCRIPTION RECORDS r1 < count1");
                }

                if (r2 < count2) {
                    while (r2 < count2) { // ADD REMAINING DESCRIPTION INPUT
                        // Write history
                        a2[r2].setPathIdx(pathID);
                        a2[r2].setRevTime(revTime);
                        oos.writeUnshared(a2[r2]);

                        // Add to append array
                        a3[r3] = a2[r2];
                        nAdd++;
                        r2++;
                        r3++;
                    }
                }

                // Check counter numbers to master and input file record counts
                countCheck(count1, count2, nSame, nMod, nAdd, nDrop);

                // SETUP NEW MASTER ARRAY
                a2 = new Sct1_DesRecord[count1 + nAdd];
                r2 = 0;
                while (r2 < count1) {
                    a2[r2] = a1[r2];
                    r2++;
                }
                r3 = 0;
                while (r3 < nAdd) {
                    a2[r2] = a3[r3];
                    r2++;
                    r3++;
                }
                count1 = count1 + nAdd;
                a1 = a2;
                Arrays.sort(a1);

            } // WHILE (EACH DESCRIPTIONS INPUT FILE)
        } // WHILE (EACH DESCRIPTIONS DIRECTORY) *

    }

    private void processRelationshipsFiles(String wDir, List<List<SCTFile>> sctI,
            ObjectOutputStream oos, ObjectOutputStream oosIds, BufferedWriter er, int user)
            throws Exception {
        int count1, count2; // records in arrays 1 & 2
        String fName1, fName2; // file pathIdx name
        long revTime;
        Sct1_RelRecord[] a1, a2, a3 = null;

        Iterator<List<SCTFile>> dit = sctI.iterator(); // Directory Iterator
        while (dit.hasNext()) {
            List<SCTFile> fl = dit.next(); // File List
            Iterator<SCTFile> fit = fl.iterator(); // File Iterator
            if (fit.hasNext() == false) {
                continue;
            }

            // READ file1 as MASTER FILE
            SCTFile f1 = fit.next();
            fName1 = f1.file.getPath();
            revTime = f1.zRevTime;

            count1 = countFileLines(fName1);
            getLog().info("BASE FILE:  " + count1 + " records, " + fName1);
            a1 = new Sct1_RelRecord[count1];
            a1 = parseRelationships(fName1, a1, count1, f1);
            getLog().info("            " + a1.length + " after non-defining filter");
            a1 = removeDuplRels(a1);
            getLog().info("            " + a1.length + " after duplicate removal");
            count1 = a1.length;
            writeRelationships(oos, oosIds, a1, count1, revTime, user);

            while (fit.hasNext()) {
                // SETUP CURRENT RELATIONSHIPS INPUT FILE
                SCTFile f2 = fit.next();
                fName2 = f2.file.getPath();
                revTime = f2.zRevTime;

                count2 = countFileLines(fName2);
                getLog().info("Counted: " + count2 + " records, " + fName2);

                // Parse in file2
                a2 = new Sct1_RelRecord[count2];
                a2 = parseRelationships(fName2, a2, count2, f2);
                getLog().info("            " + a2.length + " after non-defining filter");
                a2 = removeDuplRels(a2);
                getLog().info("            " + a2.length + " after duplicate removal");
                count2 = a2.length;

                int r1 = 0, r2 = 0, r3 = 0; // reset record indices
                int nSame = 0, nMod = 0, nAdd = 0, nDrop = 0; // counters
                int nModSidChange = 0, nSidOnlyChange = 0; // counters related to SNOMED_ID change
                int nWrite = 0; // counter for memory optimization for object files writing
                a3 = new Sct1_RelRecord[count2];
                while ((r1 < count1) && (r2 < count2)) {

                    // :DEBUG:
                    //                    if (debug)
                    //                        if ((a1[r1].relSnoId == 2455349029L || a2[r2].relSnoId == 2455349029L)
                    //                                || (a1[r1].relSnoId == 2671123026L || a2[r2].relSnoId == 2671123026L)) {
                    //                            int tmpCompare = compareRelationship(a1[r1], a2[r2]);
                    //                            getLog().info("!!! ");
                    //                            getLog().info("!!! CASE == " + tmpCompare);
                    //                            getLog().info("!!! a1[r1] @ " + revTime + " = "
                    //                                    + a1[r1].toString());
                    //                            getLog().info("!!! ");
                    //                            getLog().info("!!! a2[r2] @ " + revTime + " = "
                    //                                    + a2[r2].toString());
                    //                            getLog().info("!!! ");
                    //                        }

                    switch (compareRelationship(a1[r1], a2[r2])) {
                        case 1: // SAME RELATIONSHIP, SAME SNOMED_ID skip to next
                            r1++;
                            r2++;
                            nSame++;
                            break;

                        case 5: // SAME LOGICAL RELATIONSHIP, CHANGED SNOMED_ID
                            // RETIRE EXISTING SNOMED_ID
                            Sct1_IdRecord idOnlyChange = null;
                            if (a1[r1].getRelSnoId() < Long.MAX_VALUE) {
                                idOnlyChange = new Sct1_IdRecord(a1[r1].getRelUuidMsb(), a1[r1].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a1[r1].getRelSnoId(), 1, revTime,
                                        a1[r1].getPathIdx(), user);
                                oosIds.writeUnshared(idOnlyChange);
                            }

                            // WRITE CURRENT SNOMED_ID
                            if (a2[r2].getRelSnoId() < Long.MAX_VALUE) {
                                idOnlyChange = new Sct1_IdRecord(a2[r2].getRelUuidMsb(), a2[r2].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a2[r2].getRelSnoId(), 0, revTime,
                                        a2[r2].getPathIdx(), user);
                                oosIds.writeUnshared(idOnlyChange);
                            }

                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nSidOnlyChange++;

                            // PERIODIC RESET IMPROVES MEMORY USE
                            nWrite++;
                            if (nWrite % ooResetInterval == 0) {
                                oos.reset();
                                oosIds.reset();
                            }
                            break;

                        case 2: // SAME LOGICAL RELATIONSHIP, SAME SNOMED_ID, MODIFIED OTHER
                            // Write history
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nMod++;

                            // PERIODIC RESET IMPROVES MEMORY USE
                            nWrite++;
                            if (nWrite % ooResetInterval == 0) {
                                oos.reset();
                                oosIds.reset();
                            }
                            break;

                        case 7: // SAME LOGICAL RELATIONSHIP, SAME SNOMED_ID, MODIFIED USER
                            // RETIRE PREVIOUS USER
                            a1[r1].setStatus(1); // RETIRE OLD USER
                            a1[r1].setRevTime(revTime);
                            oos.writeUnshared(a1[r1]);

                            // MAKE CURRENT NEW USER
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nMod++;

                            // PERIODIC RESET IMPROVES MEMORY USE
                            nWrite++;
                            if (nWrite % ooResetInterval == 0) {
                                oos.reset();
                                oosIds.reset();
                            }
                            break;

                        case 6: // SAME LOGICAL RELATIONSHIP, CHANGED SNOMED_ID, MODIFIED OTHER
                            // Write history
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // RETIRE EXISTING SNOMED_ID
                            Sct1_IdRecord idMod = null;
                            if (a1[r1].getRelSnoId() < Long.MAX_VALUE) {
                                idMod = new Sct1_IdRecord(a1[r1].getRelUuidMsb(), a1[r1].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a1[r1].getRelSnoId(), 1, revTime,
                                        a1[r1].getPathIdx(), user);
                                oosIds.writeUnshared(idMod);
                            }

                            // WRITE CURRENT SNOMED_ID
                            if (a2[r2].getRelSnoId() < Long.MAX_VALUE) {
                                idMod = new Sct1_IdRecord(a2[r2].getRelUuidMsb(), a2[r2].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a2[r2].getRelSnoId(), 0, revTime,
                                        a2[r2].getPathIdx(), user);
                                oosIds.writeUnshared(idMod);
                            }

                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nModSidChange++;

                            // PERIODIC RESET IMPROVES MEMORY USE
                            nWrite++;
                            if (nWrite % ooResetInterval == 0) {
                                oos.reset();
                                oosIds.reset();
                            }
                            break;

                        case 8: // MODIFIED LOGICAL RELATIONSHIP, CHANGED SNOMED_ID, CHANGED USER
                            // RETIRE PREVIOUS USER
                            a1[r1].setStatus(1); // RETIRE OLD USER
                            a1[r1].setRevTime(revTime);
                            oos.writeUnshared(a1[r1]);

                            // MAKE CURRENT NEW USER
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // RETIRE EXISTING SNOMED_ID
                            Sct1_IdRecord idMod2 = null;
                            if (a1[r1].getRelSnoId() < Long.MAX_VALUE) {
                                idMod2 = new Sct1_IdRecord(a1[r1].getRelUuidMsb(), a1[r1].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a1[r1].getRelSnoId(), 1, revTime,
                                        a1[r1].getPathIdx(), user);
                                oosIds.writeUnshared(idMod2);
                            }

                            // WRITE CURRENT SNOMED_ID
                            if (a2[r2].getRelSnoId() < Long.MAX_VALUE) {
                                idMod2 = new Sct1_IdRecord(a2[r2].getRelUuidMsb(), a2[r2].getRelUuidLsb(),
                                        uuidSourceSnomedIdx, a2[r2].getRelSnoId(), 0, revTime,
                                        a2[r2].getPathIdx(), user);
                                oosIds.writeUnshared(idMod2);
                            }

                            // Update master via pointer assignment
                            a1[r1] = a2[r2];
                            r1++;
                            r2++;
                            nModSidChange++;
                            break;

                        case 3: // ADDED LOGICAL RELATIONSHIP
                            // Write history
                            a2[r2].setRevTime(revTime);
                            oos.writeUnshared(a2[r2]);

                            // WRITE CURRENT SNOMED_ID
                            if (a2[r2].getRelSnoId() < Long.MAX_VALUE) {
                                Sct1_IdRecord idAdded = new Sct1_IdRecord(a2[r2].getRelUuidMsb(),
                                        a2[r2].getRelUuidLsb(), uuidSourceSnomedIdx, a2[r2].getRelSnoId(),
                                        a2[r2].getStatus(), revTime, a2[r2].getPathIdx(), user);
                                oosIds.writeUnshared(idAdded);
                            }

                            // hold pointer to append to master
                            a3[r3] = a2[r2];
                            r2++;
                            r3++;
                            nAdd++;

                            // PERIODIC RESET IMPROVES MEMORY USE
                            nWrite++;
                            if (nWrite % ooResetInterval == 0) {
                                oos.reset();
                                oosIds.reset();
                            }
                            break;

                        case 4: // DROPPED LOGICAL RELATIONSHIP
                            // see ArchitectonicAuxiliary.getStatusFromId()
                            if (a1[r1].getStatus() != 1) { // if not RETIRED
                                a1[r1].setStatus(1); // set to RETIRED
                                a1[r1].setRevTime(revTime);
                                oos.writeUnshared(a1[r1]);

                                // RETIRE EXISTING SNOMED_ID
                                if (a1[r1].getRelSnoId() < Long.MAX_VALUE) {
                                    Sct1_IdRecord idDropped = new Sct1_IdRecord(a1[r1].getRelUuidMsb(),
                                            a1[r1].getRelUuidLsb(), uuidSourceSnomedIdx, a1[r1].getRelSnoId(),
                                            a1[r1].getStatus(), revTime, a1[r1].getPathIdx(), user);
                                    oosIds.writeUnshared(idDropped);
                                }

                                // PERIODIC RESET IMPROVES MEMORY USE
                                nWrite++;
                                if (nWrite % ooResetInterval == 0) {
                                    oos.reset();
                                    oosIds.reset();
                                }
                            }
                            r1++;
                            nDrop++;
                            break;

                    } // SWITCH (COMPARE RELATIONSHIP)
                } // WHILE (NOT END OF EITHER A1 OR A2)

                // NOT MORE TO COMPARE, HANDLE REMAINING RELATIONSHIPS
                if (r1 < count1) {
                    while (r1 < count1) {
                        // see ArchitectonicAuxiliary.getStatusFromId()
                        if (a1[r1].getStatus() != 1) { // if not RETIRED
                            a1[r1].setStatus(1); // set to RETIRED
                            a1[r1].setRevTime(revTime);
                            oos.writeUnshared(a1[r1]);

                            // RETIRE EXISTING SNOMED_ID
                            if (a1[r1].getRelSnoId() < Long.MAX_VALUE) {
                                Sct1_IdRecord idDropped = new Sct1_IdRecord(a1[r1].getRelUuidMsb(),
                                        a1[r1].getRelUuidLsb(), uuidSourceSnomedIdx, a1[r1].getRelSnoId(),
                                        a1[r1].getStatus(), revTime, a1[r1].getPathIdx(), user);
                                oosIds.writeUnshared(idDropped);
                            }
                        }
                        r1++;
                        nDrop++;
                    }
                }

                if (r2 < count2) {
                    while (r2 < count2) { // ADD REMAINING RELATIONSHIP INPUT
                        // Write history
                        a2[r2].setRevTime(revTime);
                        oos.writeUnshared(a2[r2]);

                        // WRITE CURRENT SNOMED_ID
                        if (a2[r2].getRelSnoId() < Long.MAX_VALUE) {
                            Sct1_IdRecord idAdded = new Sct1_IdRecord(a2[r2].getRelUuidMsb(),
                                    a2[r2].getRelUuidLsb(), uuidSourceSnomedIdx, a2[r2].getRelSnoId(),
                                    a2[r2].getStatus(), revTime, a2[r2].getPathIdx(), user);
                            oosIds.writeUnshared(idAdded);
                        }

                        // Add to append array
                        a3[r3] = a2[r2];
                        nAdd++;
                        r2++;
                        r3++;
                    }
                }

                // Check counter numbers to master and input file record counts
                countCheck(count1, count2, nSame, nMod, nAdd, nDrop, nModSidChange, nSidOnlyChange);

                // SETUP NEW MASTER ARRAY
                a2 = new Sct1_RelRecord[count1 + nAdd];
                r2 = 0;
                while (r2 < count1) {
                    a2[r2] = a1[r2];
                    r2++;
                }
                r3 = 0;
                while (r3 < nAdd) {
                    a2[r2] = a3[r3];
                    r2++;
                    r3++;
                }
                count1 = count1 + nAdd;
                a1 = a2;
                Arrays.sort(a1);

            } // WHILE (EACH INPUT RELATIONSHIPS FILE)
        } // WHILE (EACH RELATIONSHIPS DIRECTORY) *

    }

    private int compareConcept(Sct1_ConRecord c1, Sct1_ConRecord c2) {
        if (c1.getConUuidMsb() == c2.getConUuidMsb() && c1.getConUuidLsb() == c2.getConUuidLsb()) {
            if ((c1.getStatus() == c2.getStatus()) && (c1.getIsprimitive() == c2.getIsprimitive())
                    && (c1.getModuleIdx() == c2.getModuleIdx())) {
                return 1; // SAME
            } else {
                return 2; // MODIFIED
            }
        } else if (c1.getConUuidMsb() > c2.getConUuidMsb()) {
            return 3; // ADDED

        } else if (c1.getConUuidMsb() == c2.getConUuidMsb() && c1.getConUuidLsb() > c2.getConUuidLsb()) {
            return 3; // ADDED

        } else {
            return 4; // DROPPED
        }
    }

    private int comparTtkDescriptionRevision(Sct1_DesRecord d1, Sct1_DesRecord d2) {

        if (d1.getDesUuidMsb() == d2.getDesUuidMsb() && d1.getDesUuidLsb() == d2.getDesUuidLsb()) {
            if ((d1.getStatus() == d2.getStatus()) && (d1.getConSnoId() == d2.getConSnoId())
                    && d1.getTermText().contentEquals(d2.getTermText()) && (d1.getCapStatus() == d2.getCapStatus())
                    && (d1.getDescriptionType() == d2.getDescriptionType())
                    && d1.getLanguageCode().contentEquals(d2.getLanguageCode())
                    && d1.getModuleIdx() == d2.getModuleIdx()) {
                return 1; // SAME
            } else {
                return 2; // MODIFIED
            }
        } else if (d1.getDesUuidMsb() > d2.getDesUuidMsb()) {
            return 3; // ADDED

        } else if (d1.getDesUuidMsb() == d2.getDesUuidMsb() && d1.getDesUuidLsb() > d2.getDesUuidLsb()) {
            return 3; // ADDED

        } else {
            return 4; // DROPPED
        }
    }

    private int compareRelationship(Sct1_RelRecord c1, Sct1_RelRecord c2) {
        if (c1.getRelUuidMsb() == c2.getRelUuidMsb() && c1.getRelUuidLsb() == c2.getRelUuidLsb()) {
            // SAME REL UUID
            if ((c1.getStatus() == c2.getStatus()) && (c1.getCharacteristic() == c2.getCharacteristic())
                    && (c1.getRefinability() == c2.getRefinability()) && (c1.getGroup() == c2.getGroup())
                    && (c1.getModuleIdx() == c2.getModuleIdx())) {
                if (c1.getRelSnoId() == c2.getRelSnoId()) {
                    return 1; // SAME LOGICAL REL, SAME SNOMED_ID
                } else {
                    return 5; // SAME LOGICAL REL, CHANGED SNOMED_ID
                }
            } else if (c1.getRelSnoId() == c2.getRelSnoId()) {
                if (isUserChanged(c1.getCharacteristic(), c2.getCharacteristic()) == false) {
                    return 2; // SAME LOGICAL REL, SAME SNOMED_ID, MODIFIED OTHER
                } else {
                    return 7; // SAME LOGICAL REL, SAME SNOMED_ID, MODIFIED USER
                }
            } else if (isUserChanged(c1.getCharacteristic(), c2.getCharacteristic()) == false) {
                return 6; // SAME LOGICAL REL, CHANGED SNOMED_ID, MODIFIED OTHER
            } else {
                return 8; // SAME LOGICAL REL, CHANGED SNOMED_ID, MODIFIED USER
            }

        } else if (c1.getRelUuidMsb() > c2.getRelUuidMsb()) {
            return 3; // ADDED

        } else if (c1.getRelUuidMsb() == c2.getRelUuidMsb() && c1.getRelUuidLsb() > c2.getRelUuidLsb()) {
            return 3; // ADDED

        } else {
            return 4; // DROPPED
        }
    }

    private boolean isUserChanged(int older, int newer) {
        if (older == newer) {
            return false;
        }
        if (older != 0 && newer != 0) // both not defining
        {
            return false;
        }
        return true;
    }

    private void parseConcepts(String fName, Sct1_ConRecord[] a, int count, boolean ctv3idTF,
            boolean snomedrtTF) throws Exception {

        String ctv3Str;
        String snomedrtStr;
        long start = System.currentTimeMillis();

        int CONCEPTID = 0;
        int CONCEPTSTATUS = 1;
        // int FULLYSPECIFIEDNAME = 2;
        int CTV3ID = 3;
        int SNOMEDID = 4; // SNOMED RT ID (Read Code)
        int ISPRIMITIVE = 5;

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fName),
                "UTF-8"));
        int concepts = 0;

        // Header row
        br.readLine();

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);
            long conceptKey = Long.parseLong(line[CONCEPTID]);
            int conceptStatus = Integer.parseInt(line[CONCEPTSTATUS]);
            if (ctv3idTF && (line[CTV3ID].length() > 2)) {
                ctv3Str = new String(line[CTV3ID]);
            } else {
                ctv3Str = null;
            }
            if (snomedrtTF && (line[SNOMEDID].length() > 2)) {
                snomedrtStr = new String(line[SNOMEDID]);
            } else {
                snomedrtStr = null;
            }

            int isPrimitive = Integer.parseInt(line[ISPRIMITIVE]);

            // Save to sortable array
            a[concepts] = new Sct1_ConRecord(conceptKey, conceptStatus, ctv3Str, snomedrtStr,
                    isPrimitive);
            concepts++;
        }
        br.close();
        Arrays.sort(a);

        getLog().info(
                "Parse & sort time: " + concepts + " concepts, "
                + (System.currentTimeMillis() - start) + " milliseconds");
    }

    private void parsTtkDescriptionRevisions(String fName, Sct1_DesRecord[] a, int count)
            throws Exception {

        long start = System.currentTimeMillis();

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(fName),
                "UTF-8"));
        int descriptions = 0;

        int DESCRIPTIONID = 0;
        int DESCRIPTIONSTATUS = 1;
        int CONCEPTID = 2;
        int TERM = 3;
        int INITIALCAPITALSTATUS = 4;
        int DESCRIPTIONTYPE = 5;
        int LANGUAGECODE = 6;

        int RF1_UNSPECIFIED = 0;
        int RF1_PREFERRED = 1;
        int RF1_SYNOMYM = 2;

        // Header row
        r.readLine();

        while (r.ready()) {
            String[] line = r.readLine().split(TAB_CHARACTER);

            // DESCRIPTIONID
            long descriptionId = Long.parseLong(line[DESCRIPTIONID]);
            // DESCRIPTIONSTATUS
            int status = Integer.parseInt(line[DESCRIPTIONSTATUS]);
            // CONCEPTID
            long conSnoId = Long.parseLong(line[CONCEPTID]);
            // TERM
            String text = line[TERM];
            // INITIALCAPITALSTATUS
            int capStatus = Integer.parseInt(line[INITIALCAPITALSTATUS]);
            // DESCRIPTIONTYPE
            int typeInt = Integer.parseInt(line[DESCRIPTIONTYPE]);
            if (rf2Mapping == true && (typeInt == RF1_UNSPECIFIED || typeInt == RF1_PREFERRED)) {
                typeInt = RF1_SYNOMYM;
            }
            // LANGUAGECODE
            String lang = line[LANGUAGECODE];

            // Save to sortable array
            a[descriptions] = new Sct1_DesRecord(descriptionId, status, conSnoId, text, capStatus,
                    typeInt, lang);
            descriptions++;

        }
        r.close();
        Arrays.sort(a);

        getLog().info(
                "Parse & sort time: " + descriptions + " descriptions, "
                + (System.currentTimeMillis() - start) + " milliseconds");
    }

    private Sct1_RelRecord[] parseRelationships(String fName, Sct1_RelRecord[] a, int count,
            SCTFile f)
            throws Exception {

        long start = System.currentTimeMillis();

        int RELATIONSHIPID = 0;
        int CONCEPTID1 = 1;
        int RELATIONSHIPTYPE = 2;
        int CONCEPTID2 = 3;
        int CHARACTERISTICTYPE = 4;
        int REFINABILITY = 5;
        int RELATIONSHIPGROUP = 6;

        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(fName),
                "UTF-8"));
        int relationships = 0;

        // Header row
        br.readLine();

        while (br.ready()) {
            String[] line = br.readLine().split(TAB_CHARACTER);

            // RELATIONSHIPID
            long relID = Long.MAX_VALUE;
            if ((f.isStated == true && f.hasStatedSctRelId) || f.isStated == false) {
                relID = Long.parseLong(line[RELATIONSHIPID]);
            }
            // ADD STATUS VALUE: see ArchitectonicAuxiliary.getStatusFromId()
            // STATUS VALUE MUST BE ADDED BECAUSE NOT PRESENT IN SNOMED INPUT
            int status = 0; // status added as CURRENT '0' for parsed record

            // CONCEPTID1
            long conceptOneID = Long.parseLong(line[CONCEPTID1]);
            // RELATIONSHIPTYPE
            long roleTypeSnoId = Long.parseLong(line[RELATIONSHIPTYPE]);
            int roleTypeIdx = lookupRoleTypeIdxFromSnoId(roleTypeSnoId);
            // CONCEPTID2
            long conceptTwoID = Long.parseLong(line[CONCEPTID2]);
            // CHARACTERISTICTYPE
            int characteristic = Integer.parseInt(line[CHARACTERISTICTYPE]);
            // REFINABILITY
            int refinability = Integer.parseInt(line[REFINABILITY]);
            // RELATIONSHIPGROUP
            int group = Integer.parseInt(line[RELATIONSHIPGROUP]);

            // Save to sortable array
            int pathIdx = f.pathIdx;
            int userIdx = USER_DEFAULT_IDX;
            // 0=Defining
            if (characteristic == 0 && f.isStated) {
                pathIdx = f.pathStatedIdx;
                characteristic = STATED_CHAR_WORKAROUND; // :NOTE: transient use for STATED_RELATIONSHIP 
            } else if (characteristic == 0) {
                pathIdx = f.pathInferredIdx;
                userIdx = USER_SNOROCKET_IDX;
            }

            // 0=Defining, 1=Qualifier, 2=Historical, 3=Additional, 5=STATED_CHAR_WORKAROUND
            if (characteristic == 0 || (characteristic == 1 && f.keepQualifier)
                    || (characteristic == 2 && f.keepHistorical)
                    || (characteristic == 3 && f.keepAdditional)
                    || characteristic == STATED_CHAR_WORKAROUND) {
                a[relationships] = new Sct1_RelRecord(relID, status, conceptOneID, roleTypeSnoId,
                        roleTypeIdx, conceptTwoID, characteristic, refinability, group, pathIdx,
                        userIdx);
                relationships++;
            } else {
                // :NYI: count "not kept"
            }
            //            if (conceptOneID == 391181005 && roleTypeSnoId == 116680003 && conceptTwoID == 6254007) {
            //                getLog().info(":DEBUG: found 391181005-116680003-6254007");
            //            }
        }
        
        br.close();

        a = Arrays.copyOf(a, relationships);
        if (useSctRelId) {
            computeRelationshipUuids(a, f.isStated, f.hasStatedSctRelId);
        } else {
            // :DEPRECIATED:
            computeRelationshipUuids_Old(a, f.hasStatedSctRelId, f.mapSctIdInferredToStated);
        }
        Arrays.sort(a);

        getLog().info(
                "Parse & sort time: " + relationships + " relationships, "
                + (System.currentTimeMillis() - start) + " milliseconds");
        return a;
    }

    private Sct1_RelRecord[] removeDuplRels(Sct1_RelRecord[] a) {

        // REMOVE DUPLICATES
        int lenA = a.length;
        ArrayList<Integer> duplIdxList = new ArrayList<Integer>();
        for (int idx = 0; idx < lenA - 2; idx++) {
            if ((a[idx].getRelUuidMsb() == a[idx + 1].getRelUuidMsb())
                    && (a[idx].getRelUuidLsb() == a[idx + 1].getRelUuidLsb())) {
                duplIdxList.add(Integer.valueOf(idx));
                getLog().info(
                        "::: WARNING -- Logically Duplicate Relationships:" + "\r\n::: A:" + a[idx]
                        + "\r\n::: B:" + a[idx + 1]);
            }
        }
        if (duplIdxList.size() > 0) {
            Sct1_RelRecord[] b = new Sct1_RelRecord[lenA - duplIdxList.size()];
            int aPos = 0;
            int bPos = 0;
            int len;
            for (int dropIdx : duplIdxList) {
                len = dropIdx - aPos;
                System.arraycopy(a, aPos, b, bPos, len);
                bPos = bPos + len;
                aPos = aPos + len + 1;
            }
            len = lenA - aPos;
            System.arraycopy(a, aPos, b, bPos, len);
            return b;
        } else {
            return a;
        }
    }

    private void computeRelationshipUuids(Sct1_RelRecord[] a,
            boolean isStated,
            boolean isStatedSctRelIdPresent)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // SORT BY [C1-Group-RoleType-C2]
        Comparator<Sct1_RelRecord> comp = new Comparator<Sct1_RelRecord>() {
            @Override
            public int compare(Sct1_RelRecord o1, Sct1_RelRecord o2) {
                int thisMore = 1;
                int thisLess = -1;
                // C1
                if (o1.getC1SnoId() > o2.getC1SnoId()) {
                    return thisMore;
                } else if (o1.getC1SnoId() < o2.getC1SnoId()) {
                    return thisLess;
                } else {
                    // GROUP
                    if (o1.getGroup() > o2.getGroup()) {
                        return thisMore;
                    } else if (o1.getGroup() < o2.getGroup()) {
                        return thisLess;
                    } else {
                        // ROLE TYPE
                        if (o1.getRoleTypeSnoId() > o2.getRoleTypeSnoId()) {
                            return thisMore;
                        } else if (o1.getRoleTypeSnoId() < o2.getRoleTypeSnoId()) {
                            return thisLess;
                        } else {
                            // C2
                            if (o1.getC2SnoId() > o2.getC2SnoId()) {
                                return thisMore;
                            } else if (o1.getC2SnoId() < o2.getC2SnoId()) {
                                return thisLess;
                            } else {
                                return 0; // EQUAL
                            }
                        }
                    }
                }
            } // compare()
        };
        Arrays.sort(a, comp);

        long lastC1 = a[0].getC1SnoId();
        int lastGroup = a[0].getGroup();
        String GroupListStr = getGroupListString(a, 0);
        int max = a.length;
        for (int i = 0; i < max; i++) {
            // DETERMINE IF NEW GroupListStr IS NEEDED
            if (lastC1 != a[i].getC1SnoId() || lastGroup != a[i].getGroup()) {
                GroupListStr = getGroupListString(a, i);
            }

            // SET RELATIONSHIP UUID
            if (isStated == true && isStatedSctRelIdPresent == false) {
                UUID uuid = UuidT5Generator.get(REL_ID_NAMESPACE_UUID_TYPE1 + a[i].getC1SnoId()
                        + a[i].getRoleTypeSnoId() + a[i].getC2SnoId() + GroupListStr);
                a[i].setRelUuidMsb(uuid.getMostSignificantBits());
                a[i].setRelUuidLsb(uuid.getLeastSignificantBits());
            } else {
                UUID uuid = UuidT3Generator.fromSNOMED(a[i].getRelSnoId());
                a[i].setRelUuidMsb(uuid.getMostSignificantBits());
                a[i].setRelUuidLsb(uuid.getLeastSignificantBits());
            }

            lastC1 = a[i].getC1SnoId();
            lastGroup = a[i].getGroup();
        }
    }

    private void computeRelationshipUuids_Old(Sct1_RelRecord[] a, boolean hasSnomedId,
            boolean doCrossMap)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // SORT BY [C1-Group-RoleType-C2]
        Comparator<Sct1_RelRecord> comp = new Comparator<Sct1_RelRecord>() {
            @Override
            public int compare(Sct1_RelRecord o1, Sct1_RelRecord o2) {
                int thisMore = 1;
                int thisLess = -1;
                // C1
                if (o1.getC1SnoId() > o2.getC1SnoId()) {
                    return thisMore;
                } else if (o1.getC1SnoId() < o2.getC1SnoId()) {
                    return thisLess;
                } else {
                    // GROUP
                    if (o1.getGroup() > o2.getGroup()) {
                        return thisMore;
                    } else if (o1.getGroup() < o2.getGroup()) {
                        return thisLess;
                    } else {
                        // ROLE TYPE
                        if (o1.getRoleTypeSnoId() > o2.getRoleTypeSnoId()) {
                            return thisMore;
                        } else if (o1.getRoleTypeSnoId() < o2.getRoleTypeSnoId()) {
                            return thisLess;
                        } else {
                            // C2
                            if (o1.getC2SnoId() > o2.getC2SnoId()) {
                                return thisMore;
                            } else if (o1.getC2SnoId() < o2.getC2SnoId()) {
                                return thisLess;
                            } else {
                                return 0; // EQUAL
                            }
                        }
                    }
                }
            } // compare()
        };
        Arrays.sort(a, comp);

        long lastC1 = a[0].getC1SnoId();
        int lastGroup = a[0].getGroup();
        String GroupListStr = getGroupListString(a, 0);
        int max = a.length;
        for (int i = 0; i < max; i++) {
            // DETERMINE IF NEW GroupListStr IS NEEDED
            if (lastC1 != a[i].getC1SnoId() || lastGroup != a[i].getGroup()) {
                GroupListStr = getGroupListString(a, i);
            }

            // SET RELATIONSHIP UUID
            UUID uuid = UuidT5Generator.get(REL_ID_NAMESPACE_UUID_TYPE1 + a[i].getC1SnoId()
                    + a[i].getRoleTypeSnoId() + a[i].getC2SnoId() + GroupListStr);
            a[i].setRelUuidMsb(uuid.getMostSignificantBits());
            a[i].setRelUuidLsb(uuid.getLeastSignificantBits());

            // :DEBUG
            // if (uuid.toString().compareToIgnoreCase("8003f34d-e069-57a5-b7db-919fec994ced") == 0)
            //     getLog().info("!!!:PARSE: 8003f34d-e069-57a5-b7db-919fec994ced... Rel="
            //             + a[i].relSnoId + ":" + a[i].c1SnoId + "-" + a[i].roleTypeSnoId + "-"
            //             + a[i].c2SnoId + " G" + a[i].group + " RG(" + GroupListStr + ")");
            // if (a[i].c1SnoId == 391181005 && a[i].roleTypeSnoId == 116680003 && a[i].c2SnoId == 6254007) {
            //     getLog().info(":DEBUG: found 391181005-116680003-6254007 (compute uuids)");
            // }

            // UPDATE SNOMED ID
            if (doCrossMap) {
                if (hasSnomedId) // relUuidMap.put(a[i].relUuidMsb, a[i].relUuidLsb, a[i].relSnoId);
                {
                    relUuidMap.put(uuid, new Long(a[i].getRelSnoId()));
                } else {
                    // a[i].relSnoId = relUuidMap.get(a[i].relUuidMsb, a[i].relUuidLsb);
                    Long tmpLong = relUuidMap.get(uuid);
                    if (tmpLong != null) {
                        a[i].setRelSnoId(relUuidMap.get(uuid).longValue());
                    } else {
                        a[i].setRelSnoId(Long.MAX_VALUE);
                    }
                }
            }

            lastC1 = a[i].getC1SnoId();
            lastGroup = a[i].getGroup();
        }
    }

    private String getGroupListString(Sct1_RelRecord[] a, int startIdx) {
        StringBuilder sb = new StringBuilder();

        int max = a.length;
        if (a[startIdx].getGroup() > 0) {
            long keepC1 = a[startIdx].getC1SnoId();
            int keepGroup = a[startIdx].getGroup();
            int i = startIdx;
            while ((i < max - 1) && (a[i].getC1SnoId() == keepC1) && (a[i].getGroup() == keepGroup)) {
                sb.append(a[i].getC1SnoId()).append("-");
                sb.append(a[i].getRoleTypeSnoId()).append("-");
                sb.append(a[i].getC2SnoId()).append(";");
                i++;
            }
        }
        return sb.toString();
    }

    private void writeConcepts(ObjectOutputStream oos, Sct1_ConRecord[] a, int count,
            long releaseDateTime, int pathIdx)
            throws Exception {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            a[i].setPathIdx(pathIdx);
            a[i].setRevTime(releaseDateTime);
            oos.writeUnshared(a[i]);

            // PERIODIC RESET IMPROVES MEMORY USE
            if (i % ooResetInterval == 0) {
                oos.reset();
            }
        }

        getLog().info(
                "Output time: " + count + " records, " + (System.currentTimeMillis() - start)
                + " milliseconds");
    }

    private void writeTtkDescriptionRevisions(ObjectOutputStream oos, Sct1_DesRecord[] a, int count,
            long releaseDateTime, int pathIdx)
            throws Exception {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            a[i].setPathIdx(pathIdx);
            a[i].setRevTime(releaseDateTime);
            oos.writeUnshared((Object) a[i]);

            // PERIODIC RESET IMPROVES MEMORY USE
            if (i % ooResetInterval == 0) {
                oos.reset();
            }
        }

        getLog().info(
                "Output time: " + count + " records, " + (System.currentTimeMillis() - start)
                + " milliseconds");
    }

    private void writeRelationships(ObjectOutputStream oos, ObjectOutputStream oosIds,
            Sct1_RelRecord[] a, int count, long releaseDateTime, int user)
            throws Exception {

        long start = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            a[i].setRevTime(releaseDateTime);
            oos.writeUnshared(a[i]);

            if (a[i].getRelSnoId() < Long.MAX_VALUE) {
                Sct1_IdRecord id = new Sct1_IdRecord(a[i].getRelUuidMsb(), a[i].getRelUuidLsb(),
                        uuidSourceSnomedIdx, a[i].getRelSnoId(), a[i].getStatus(), a[i].getRevTime(),
                        a[i].getPathIdx(), user);
                oosIds.writeUnshared(id);
            }

            // PERIODIC RESET IMPROVES MEMORY USE
            if (i % ooResetInterval == 0) {
                oos.reset();
                oosIds.reset();
            }
        }

        getLog().info(
                "Output time: " + count + " records, " + (System.currentTimeMillis() - start)
                + " milliseconds");
    }

    private long convertDateStrToTime(String date) throws MojoFailureException {
        try {
            if (date.contains(".")) {
                return (arfSimpleDateFormatDot.parse(date)).getTime();
            } else if (date.contains("-")) {
                return arfSimpleDateFormatDash.parse(date).getTime();
            } else {
                return arfSimpleDateFormat.parse(date).getTime();
            }
        } catch (ParseException ex) {
            LOG.error("Can't parse date", ex);
            throw new MojoFailureException("CAN NOT PARSE DATE: " + date, ex);
        }
    }

    private void countCheck(int count1, int count2, int same, int modified, int added, int dropped) {

        // CHECK COUNTS TO MASTER FILE1 RECORD COUNT
        if ((same + modified + dropped) == count1) {
            getLog().info(
                    "PASSED1:: SAME+MODIFIED+DROPPED = " + same + "+" + modified + "+" + dropped
                    + " = " + (same + modified + dropped) + " == " + count1);
        } else {
            getLog().info(
                    "FAILED1:: SAME+MODIFIED+DROPPED = " + same + "+" + modified + "+" + dropped
                    + " = " + (same + modified + dropped) + " != " + count1);
        }

        // CHECK COUNTS TO UPDATE FILE2 RECORD COUNT
        if ((same + modified + added) == count2) {
            getLog().info(
                    "PASSED2:: SAME+MODIFIED+ADDED   = " + same + "+" + modified + "+" + added
                    + " = " + (same + modified + added) + " == " + count2);
        } else {
            getLog().info(
                    "FAILED2:: SAME+MODIFIED+ADDED   = " + same + "+" + modified + "+" + added
                    + " = " + (same + modified + added) + " != " + count2);
        }

    }

    private void countCheck(int count1, int count2, int same, int modified, int added, int dropped,
            int idmod, int idonly) {

        // CHECK COUNTS TO MASTER FILE1 RECORD COUNT
        if ((same + modified + dropped + idmod + idonly) == count1) {
            getLog().info(
                    "PASSED1:: SAME+MODIFIED+DROPPED+MODIFIED_IDCHANGE+IDCHANGEONLY = " + same
                    + "+" + modified + "+" + dropped + "+" + idmod + "+" + idonly + " = "
                    + (same + modified + dropped + idmod + idonly) + " == " + count1);
        } else {
            getLog().info(
                    "FAILED1:: SAME+MODIFIED+DROPPED+MODIFIED_IDCHANGE+IDCHANGEONLY = " + same
                    + "+" + modified + "+" + dropped + "+" + idmod + "+" + idonly + " = "
                    + (same + modified + dropped + idmod + idonly) + " != " + count1);
        }

        // CHECK COUNTS TO UPDATE FILE2 RECORD COUNT
        if ((same + modified + added + idmod + idonly) == count2) {
            getLog().info(
                    "PASSED2:: SAME+MODIFIED+ADDED+MODIFIED_IDCHANGE+IDCHANGEONLY   = " + same
                    + "+" + modified + "+" + added + "+" + idmod + "+" + idonly + " = "
                    + (same + modified + added + idmod + idonly) + " == " + count2);
        } else {
            getLog().info(
                    "FAILED2:: SAME+MODIFIED+ADDED+MODIFIED_IDCHANGE+IDCHANGEONLY   = " + same
                    + "+" + modified + "+" + added + "+" + idmod + "+" + idonly + " = "
                    + (same + modified + added + idmod + idonly) + " != " + count2);
        }

    }

    private static int countFileLines(String fileName) throws MojoFailureException {
        int lineCount = 0;
        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(fileName), "UTF-8"));
            try {
                while (br.readLine() != null) {
                    lineCount++;
                }
            } catch (IOException ex) {
                throw new MojoFailureException("FAILED: error counting lines in " + fileName, ex);
            } finally {
                br.close();
            }
        } catch (IOException ex) {
            throw new MojoFailureException("FAILED: error open BufferedReader for " + fileName, ex);
        }

        // lineCount NOTE: COUNT -1 BECAUSE FIRST LINE SKIPPED
        // lineCount NOTE: REQUIRES THAT LAST LINE IS VALID RECORD
        return lineCount - 1;
    }

    private int countFileObjects(String fName) 
            throws FileNotFoundException, IOException, ClassNotFoundException {
        int objCount = 0;

        ObjectInputStream ois;
        ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(fName)));
        try {
            while ((ois.readObject()) != null) {
                objCount++;
            }
        } catch (EOFException ex) {
            getLog().info(" object count = " + objCount + " @EOF " + fName + "\r\n");
        }

        return objCount;
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

        // Check pathIdx for date yyyy-MM-dd
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

    /*
     * 1. build directory buildDir
     */
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
    
    private Status getStatus(int sctStatusType) {
        if (sctStatusType == 0) {
            return Status.INACTIVE;
        }
        else if (sctStatusType == 1) {
            return Status.ACTIVE;
        }
        else {
            throw new RuntimeException("Oops, bad assumption status type " + sctStatusType);
        }
    }
    
    public static UUID getSnomedDescriptionType(int type) {
        switch (type) {
        case 0:
            return IsaacMetadataAuxiliaryBinding.DEFINITION_DESCRIPTION_TYPE.getPrimodialUuid();
        case 1:
            return IsaacMetadataAuxiliaryBinding.PREFERRED.getPrimodialUuid();
        case 2:
            return IsaacMetadataAuxiliaryBinding.SYNONYM.getPrimodialUuid();
        case 3:
            return IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getPrimodialUuid();
        }
        throw new RuntimeException("Unhandled description type " + type);
    }
}