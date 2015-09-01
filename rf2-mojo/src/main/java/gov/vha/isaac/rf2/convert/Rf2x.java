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

import gov.vha.isaac.ochre.util.UuidT3Generator;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

public class Rf2x {

    private static final SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static Sct2_IdLookUp sctid2UuidCache = null;
    private static long notMappedCounter;

    public Rf2x() {
        sctid2UuidCache = null;
    }

    public static void setupIdCache(File idCacheFile, boolean enableUUIDtoSCTIDMap) 
            throws IOException {

        System.out.println(":::      " + idCacheFile.getAbsolutePath());
        if (!idCacheFile.exists()) {
            System.out.println(":::      SCTID/UUID cache file does not exist!!!@!");
            // Initialize with an empty id list
            sctid2UuidCache = new Sct2_IdLookUp(new ArrayList<Sct2_IdCompact>(), enableUUIDtoSCTIDMap);
            notMappedCounter = 0;
        } else {
            System.out.println(":::      SCTID/UUID cache file exists!!!@!");
            sctid2UuidCache = new Sct2_IdLookUp(idCacheFile, enableUUIDtoSCTIDMap);
            notMappedCounter = 0;
        }
    }

    /**
     * Convert "yyyyMMdd" effective time
     * <code>String</code> to "yyyy-MM-dd 00:00:00"
     * <code>String</code>.
     *
     * @param effectiveTimeStr
     * @return
     */
    public static String convertEffectiveTimeToDate(String effectiveTimeStr) {
        return effectiveTimeStr.substring(0, 4) + "-"
                + effectiveTimeStr.substring(4, 6) + "-"
                + effectiveTimeStr.substring(6, 8)
                + " 00:00:00";
    }

    /**
     * Convert "yyyy-MM-dd 00:00:00" date
     * <code>String</code> to
     * <code>long</code> time milliseconds.
     *
     * @param date
     * @return
     * @throws ParseException
     */
    static long convertDateToTime(String date) throws ParseException {
        return formatter.parse(date).getTime();
    }

    /**
     * Convert
     * <code>long</code> time milliseconds to "yyyy-MM-dd HH:mm:ss"
     * <code>String</code>
     *
     * @param time
     * @return
     * @throws ParseException
     */
    static String convertTimeToDate(long time) throws ParseException {
        return formatter.format(new Date(time));
    }

    /**
     * converts "1" to
     * <code><b>true</code></b>
     */
    static boolean convertStringToBoolean(String s) {
        if (s.startsWith("1")) {
            return true;
        } else {
            return false;
        }
    }

    static String convertActiveToStatusUuid(String activeString)
            throws IOException {
        return convertActiveToStatusUuid(convertStringToBoolean(activeString));
    }

    /**
     * Converts
     * <code><b>true</code></b> to RF2 Active UUID. Converts
     * <code><b>false</code></b> to RF2 Inactive UUID.<br> <br> <i>Note:<br> RF1
     * definition: "0" = current, "1" = noncurrent<br> RF2 definition: "0" =
     * inactive, "1" = active<br> ARF Status UUID is most general and supports
     * RF1 and RF2<br></i>
     *
     * @param active
     * @return
     * @throws IOException
     * @throws TerminologyException
     */
    public static String convertActiveToStatusUuid(boolean active) throws IOException {
        if (active) {
            // return ArchitectonicAuxiliary.Concept.CURRENT.getPrimoridalUid().toString();
            return "d12702ee-c37f-385f-a070-61d56d4d0f1f"; // RF2 Active
        } else {
            // return ArchitectonicAuxiliary.Concept.RETIRED.getPrimoridalUid().toString();
            return "a5daba09-7feb-37f0-8d6d-c3cadfc7f724"; // RF2 Inactive
        }
    }

    /**
     * If "900000000000074008" returns true, otherwise returns false. <br>
     * <pre><i>
     * Notes:
     * RF1 definition: "0" Fully defined, "1" Primitive
     * RF2 Set to a child of |Definition status| in the metadata hierarchy.
     *     900000000000074008 == primitive
     * ARF definition: boolean string where 0 (false == defined) or 1 (true == primitive)
     * <i></pre>
     *
     * @param defStatus
     * @return
     */
    static boolean convertDefinitionStatusToIsPrimitive(String defStatus) {
        if (defStatus.equalsIgnoreCase("900000000000074008")) {
            return true;
        } else {
            return false;
        }
    }

    static String convertSctIdToUuidStr(String idStr) 
            throws IOException {
        long id = Long.parseLong(idStr);
        return convertSctIdToUuidStr(id);
    }

    public static String convertSctIdToUuidStr(long id) 
            throws IOException {
        return convertSctIdToUuid(id).toString();
    }

    static UUID convertSctIdToUuid(long id) 
            throws IOException {
        UUID uuid;
        if (sctid2UuidCache == null) {
            throw new IOException("Rf2x.setupIdCache(path) needs to be called first");
        }
        uuid = sctid2UuidCache.getUuid(id);
        if (uuid == null) {
            notMappedCounter++;
            if (notMappedCounter % 100000 == 1) {
                System.out.println(":::INFO: UUID notMappedCounter=" + notMappedCounter + " SCTID=" + Long.toString(id));
            }
            return UuidT3Generator.fromSNOMED(id);                    
        } else {
            return uuid;
        }
    }

    static boolean convertCaseSignificanceIdToCapStatus(String caseSignifcanceId) {
        // Case Significant RF2==900000000000017005, RF1=="1" or true
        // Case Not Significant RF2==900000000000020002, RF1=="0" or false
        if (caseSignifcanceId.equalsIgnoreCase("900000000000017005")) {
            return true;
        } else {
            return false;
        }
    }
    
       
    public static boolean isSctIdInUuidCache(long sctId) throws IOException {
        UUID uuid;
        if (sctid2UuidCache == null) {
            throw new IOException("Rf2x.setupIdCache(path) needs to be called first");
        }
        uuid = sctid2UuidCache.getUuid(sctId);
        if (uuid == null) {
            return false;
        } 
        return true;
    }
    
    public static Long getSCTIDforUUID(UUID uuid)
    {
        if (sctid2UuidCache == null) {
            throw new RuntimeException("Rf2x.setupIdCache(path) needs to be called first");
        }
        return sctid2UuidCache.getSCTId(uuid);
    }

}
