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
package gov.vha.isaac.rf2.convert;

import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Simple case: single date (no versioning). all active. one id schema. Handle
 * remapping of non-computed primordial UUIDs.
 *
 * @author marc campbell
 */
public class Sct2_IdRecord {

    private static Logger LOG = LogManager.getLogger();
    protected static final String LINE_TERMINATOR = "\r\n";
    protected static final String TAB_CHARACTER = "\t";
    // RECORD FILES
    // long identifierScheme; // IDENTIFIER_SCHEME_ID column 0;
//    private UUID primordialUuid; // ALTERNATE_IDENTIFIER column 1;
//    private long revTimeL; // EFFECTIVE_TIME column 2; e.g. date time of "20020131"
//    private int active; // ACTIVE column 3; value 1 is active
//    private long moduleSctId; // MODULE_ID column 4; value 900000000000207008
//    private long referencedComponentSctId; // REFERENCED_COMPONENT_ID column 5 SCTID
    // PATH
    // int pathIdx;
    // USER
    // int userIdx;

//    public Sct2_IdRecord(UUID pUuid, long revTime, int active, long moduleId, long referencedComponent) {
//        this.primordialUuid = pUuid; // CONCEPTID/PRIMARYID
//        this.revTimeL = revTime;
//        this.active = active;
//        this.moduleSctId = moduleId;
//        this.referencedComponentSctId = referencedComponent;
//    }

    /**
     *
     * prepare id lookup cache file.
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static void parseToIdPreCacheFile(List<Rf2File> fList, File idCacheOutputPathFname)
            throws MojoFailureException {
        // SNOMED CT UUID scheme
        try {
            long sctUuidSchemeIdL = Long.parseLong("900000000000002006");
            long countNonActiveL;
            long countNonComputedIdsL;
            long nonParsableLinesL;
            long totalParsedLinesL;
            Set<Long> idSchemeSet = new HashSet<>();
            Set<Long> dateTimeSet = new HashSet<>();
            Set<Long> moduleIdSet = new HashSet<>();
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(idCacheOutputPathFname)))) {
                        // open searchable text file
                        File txtFile = new File(idCacheOutputPathFname.getParentFile(), idCacheOutputPathFname.getName() + ".txt");
                        BufferedWriter bw = new BufferedWriter(new FileWriter(txtFile));
                        bw.append("SCT");
                        bw.append(TAB_CHARACTER);
                        bw.append("COMPUTED");
                        bw.append(TAB_CHARACTER);
                        bw.append("ASSIGNED");
                        bw.append(LINE_TERMINATOR);

                        int IDENTIFIER_SCHEME_ID = 0;
                        int ALTERNATE_IDENTIFIER = 1;
                        int EFFECTIVE_TIME = 2;
                        int ACTIVE = 3;
                        int MODULE_ID = 4;
                        int REFERENCED_COMPONENT_ID = 5;
                        for (Rf2File f : fList) {
                            BufferedReader br = new BufferedReader(
                                    new InputStreamReader(
                                            new FileInputStream(f.file), "UTF-8"));
                            br.readLine();
                            countNonActiveL = 0;
                            countNonComputedIdsL = 0;
                            totalParsedLinesL = 0;
                            nonParsableLinesL = 0;
                            while (br.ready()) {
                                String tempLine = br.readLine();
                                String[] line = tempLine.split(TAB_CHARACTER);
                                if (line.length < REFERENCED_COMPONENT_ID + 1) {
                                    System.err.println("not parsed: " + tempLine);
                                    nonParsableLinesL++;
                                    continue;
                                } else {
                                    totalParsedLinesL++;
                                }
                                // IDENTIFIER_SCHEME_ID
                                long identifierScheme = Long.parseLong(line[IDENTIFIER_SCHEME_ID]);
                                if (identifierScheme != sctUuidSchemeIdL) {
                                    idSchemeSet.add(identifierScheme);
                                    continue;
                                }
                                // ACTIVE
                                int activeData = Integer.parseInt(line[ACTIVE]);
                                if (activeData != 1) {
                                    countNonActiveL++;
                                }
                                // EFFECTIVE_TIME
                                String eTimeStr = Rf2x.convertEffectiveTimeToDate(line[EFFECTIVE_TIME]);
                                long eTime = Rf2x.convertDateToTime(eTimeStr);
                                dateTimeSet.add(eTime);

                                // MODULE_ID
                                Long moduleIdL = Long.parseLong(line[MODULE_ID]);
                                moduleIdSet.add(moduleIdL);

                                // ALTERNATE_IDENTIFIER and REFERENCED_COMPONENT_ID
                                // Assigned uuid
                                UUID aUuid = UUID.fromString(line[ALTERNATE_IDENTIFIER]);
                                // Computed uuid
                                long sctIdL = Long.parseLong(line[REFERENCED_COMPONENT_ID]);
                                UUID cUuid = UUID.fromString(
                                		UuidT3Generator.fromSNOMED(sctIdL).toString());
                                // UUID cUuid = UUID.fromString(Rf2x.convertSctIdToUuidStr(sctIdL));
                                if (aUuid.compareTo(cUuid) != 0) {
                                    countNonComputedIdsL++;
                                }

                                Sct2_IdCompact tempIdCompact = new Sct2_IdCompact(
                                        aUuid.getMostSignificantBits(),
                                        aUuid.getLeastSignificantBits(),
                                        sctIdL,
                                        eTime);
                                // Write to JBIN file
                                oos.writeUnshared(tempIdCompact);

                                // Write to TEXT file
                                StringBuilder sb = new StringBuilder();
                                sb.append(tempIdCompact.sctId_);
                                sb.append(TAB_CHARACTER);
                                sb.append(cUuid);
                                sb.append(TAB_CHARACTER);
                                sb.append(aUuid);
                                sb.append(LINE_TERMINATOR);
                                bw.append(sb.toString());
                            }
                            StringBuilder sb = new StringBuilder();
                            sb.append("\n::: parseToIdPreCacheFile(..) ");
                            sb.append("\n::: PARSED & WRITTEN TO ID CACHE: ");
                            sb.append(f.file.toURI().toString());
                            if (idSchemeSet.size() > 0) {
                                Long[] idSchemeArray = idSchemeSet.toArray(new Long[0]);
                                for (Long long1 : idSchemeArray) {
                                    sb.append("\n::: WARNING unsupported id scheme: ");
                                    sb.append(long1.toString());
                                }
                            } else {
                                sb.append("\n::: Schema OK (900000000000002006)");
                            }
                            sb.append("\n::: countNonActive=");
                            sb.append(countNonActiveL);
                            sb.append("\n::: countNonComputedIDs=");
                            sb.append(countNonComputedIdsL);
                            sb.append("\n::: totalParsedLinesL=");
                            sb.append(totalParsedLinesL);
                            sb.append("\n::: nonParsableLines=");
                            sb.append(nonParsableLinesL);
                            sb.append("\n::: dateTimeCount=");
                            sb.append(dateTimeSet.size());
                            Long[] moduleIdArray = moduleIdSet.toArray(new Long[0]);
                            for (Long moduleIdLong : moduleIdArray) {
                                sb.append("\n::: ModuleID: ");
                                sb.append(moduleIdLong.toString());
                            }
                            Long[] dateTimeArray = dateTimeSet.toArray(new Long[0]);
                            for (Long dateLong : dateTimeArray) {
                                sb.append("\n:::     ");
                                sb.append(Rf2x.convertTimeToDate(dateLong));
                            }
                            sb.append("\n::: \n");
                            LOG.info(sb.toString());
                            br.close();
                        }
                        oos.flush();
                        oos.close();
                        bw.flush();
                        bw.close();
                    }
        } catch (Exception ex) {
            throw new MojoFailureException(ex.getLocalizedMessage(), ex);
        }
    }

    /**
     *
     * :NYI: SCTIDs UUID pairing are not versioned.
     *
     * @param f
     * @return
     * @throws Exception
     */
    public static void parseIdsToArf(List<Rf2File> fList,
            BufferedWriter arfWriter,
            Sct2_IdLookUp idLookUp, UUID pathUuid, UUID authorUuid)
            throws MojoFailureException {
        try {
            // SNOMED integer id
            UUID sourceSystemUuid = IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid();
            if (sourceSystemUuid == null) {
                throw new MojoFailureException("Source System ID not found in ID lookup");
            }

            // DATA COLUMNS
//            int IDENTIFIER_SCHEME_ID = 0;
            int ALTERNATE_IDENTIFIER = 1; // UUID of interest
            int EFFECTIVE_TIME = 2; // effectiveTime 20020131
            int ACTIVE = 3; // active 1
            int MODULE_ID = 4; // moduleSctId 900000000000207008
            int REFERENCED_COMPONENT_ID = 5; // referencedComponentId 100000000
            HashSet<String> additionalIds = new HashSet<>();
            idLookUp.getAdditionalIDs().forEach(id -> additionalIds.add(id + ""));
//            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(idLookUp.additionalUuidsFile), "UTF-8"));
//            String line = reader.readLine();
//            while (line != null) {
//                additionalIds.add(line.split("\t")[0]);
//                line = reader.readLine();
//            }
//            reader.close();
            for (Rf2File f : fList) {
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(
                                new FileInputStream(f.file), "UTF-8"));

                // Header row
                br.readLine();

                while (br.ready()) {
                    String tempLine = br.readLine();
                    String[] parts = tempLine.split(TAB_CHARACTER);
                    if (parts.length < REFERENCED_COMPONENT_ID + 1) {
                        System.err.println("not parsed: " + tempLine);
                        continue;
                    }
                    // SCTID
                    String componentSctIdString = parts[REFERENCED_COMPONENT_ID];
                    UUID sctIdUuid = idLookUp.getUuid(componentSctIdString);
                    // PRIMARY_UUID
                    UUID pUuid = UUID.fromString(parts[ALTERNATE_IDENTIFIER]);
                    if (sctIdUuid == null || pUuid.compareTo(sctIdUuid) != 0) {
                        if (!additionalIds.contains(parts[REFERENCED_COMPONENT_ID])) {
                            throw new MojoFailureException("ALTERNATE_IDENTIFIER not found in cache: " + parts[REFERENCED_COMPONENT_ID]);
                        }
                        continue;
                    }

                    // yyyy-MM-dd HH:mm:ss
                    String dateStr = Rf2x.convertEffectiveTimeToDate(parts[EFFECTIVE_TIME]);

                    // module uuid
                    UUID moduleUuid = idLookUp.getUuid(parts[MODULE_ID]);
                    if (moduleUuid == null) {
                        throw new MojoFailureException("MODULE_ID not in id cache");
                    }

                    // Write to ARF file
                    writeArf(arfWriter,
                            pUuid, // PRIMARY_UUID = 0
                            sourceSystemUuid, // SOURCE_SYSTEM_UUID = 1
                            parts[REFERENCED_COMPONENT_ID], // ID_FROM_SOURCE_SYSTEM
                            parts[ACTIVE], // STATUS_UUID = 3
                            dateStr, // EFFECTIVE_DATE = 4; yyyy-MM-dd HH:mm:ss
                            pathUuid, // PATH_UUID = 5;
                            authorUuid, // AUTHOR_UUID = 6;
                            moduleUuid); // MODULE_UUID = 7;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("\n::: parseIdsToArf() FILE: ");
                sb.append(f.file.toURI().toString());
                LOG.info(sb.toString());
                br.close();
            }
        } catch (Exception ex) {
            throw new MojoFailureException(ex.getMessage(), ex);
        }
    }

    // writeSctSnomedLongId
    // BufferedWriter writer, long sctId, String date, String path
    private static void writeArf(BufferedWriter writer,
            UUID pUuid,
            UUID sourceSystemUuid,
            String sctIdString,
            String activeStr,
            String dateStr,
            UUID pathUuid,
            UUID authorUuid,
            UUID moduleUuid)
            throws IOException {
        // PRIMARY_UUID = 0;
        writer.append(pUuid.toString() + TAB_CHARACTER);
        // SOURCE_SYSTEM_UUID = 1;
        writer.append(sourceSystemUuid + TAB_CHARACTER);
        // ID_FROM_SOURCE_SYSTEM = 2;
        writer.append(sctIdString + TAB_CHARACTER);
        // STATUS_UUID = 3;
        writer.append(Rf2x.convertActiveToStatusUuid(activeStr) + TAB_CHARACTER);
        // EFFECTIVE_DATE = 4; // yyyy-MM-dd HH:mm:ss
        // writer.append(date + TAB_CHARACTER);
        writer.append(dateStr + TAB_CHARACTER);
        // PATH_UUID = 5;
        writer.append(pathUuid.toString() + TAB_CHARACTER);
        // AUTHOR_UUID = 6;
        writer.append(authorUuid.toString() + TAB_CHARACTER);
        // MODULE_UUID = 7;
        writer.append(moduleUuid.toString() + LINE_TERMINATOR);
    }
}
