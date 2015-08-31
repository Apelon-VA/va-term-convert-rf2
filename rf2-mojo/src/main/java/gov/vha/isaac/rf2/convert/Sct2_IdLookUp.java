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
package gov.vha.isaac.rf2.convert;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 *
 * @author Marc Campbell
 */
public class Sct2_IdLookUp {

    private Logger LOG = LogManager.getLogger();
    private long sctIdArray[];
    private long uuidMsbArray[];
    private long uuidLsbArray[];
//    private BufferedWriter uuidsWriter;
//    private File additionalUuidsFile;
    private HashMap<UUID, Long> UUIDtoSCTMap = null;
    private ArrayList<Long> additionalIDs = new ArrayList<>();

    public Sct2_IdLookUp(File idCacheFile, boolean enableUUIDtoSCTMap) throws IOException {
//        additionalUuidsFile = new File(arfOutPath, "additional.txt");
//        FileOutputStream uuidsOs = new FileOutputStream(additionalUuidsFile);
//        uuidsWriter = new BufferedWriter(new OutputStreamWriter(uuidsOs, "UTF8"));
        
        ArrayList<Sct2_IdCompact> idList = new ArrayList<>();
        ObjectInputStream ois;
        ois = new ObjectInputStream(new BufferedInputStream(new FileInputStream(idCacheFile)));
        try {
            Object obj;
            while ((obj = ois.readObject()) != null) {
                if (obj instanceof Sct2_IdCompact) {
                    idList.add((Sct2_IdCompact) obj);
                }
            }
        } catch (ClassNotFoundException ex) {
            LOG.error(ex);
        } catch (EOFException ex) {
            // getLog().info(" relationship count = " + count + " @EOF\r\n");
            ois.close();
        }
        if (enableUUIDtoSCTMap) {
            UUIDtoSCTMap = new HashMap<UUID, Long>();
        }
        setupArrays(idList);
    }

    public Sct2_IdLookUp(ArrayList<Sct2_IdCompact> idList, boolean enableUUIDtoSCTMap) throws IOException {
        if (enableUUIDtoSCTMap) {
            UUIDtoSCTMap = new HashMap<UUID, Long>();
        }
        setupArrays(idList);
    }
    
    public List<Long> getAdditionalIDs() {
        return additionalIDs;
    }

    private void setupArrays(ArrayList<Sct2_IdCompact> idList) throws IOException {
        int countSctDuplicates = 0;
        int countSctPairUuidChanged = 0;
        StringBuilder sb = new StringBuilder();
        ArrayList<Sct2_IdCompact> tempIdList = new ArrayList<>(idList);
        Collections.sort(idList); // required for binarySearch
        Collections.sort(tempIdList);
        for (int i = 0; i < tempIdList.size() - 1; i++) {
            if (tempIdList.get(i).getSctId() == tempIdList.get(i + 1).getSctId()) {
                //remove and write to additional ids file
                idList.remove(i);
                Sct2_IdCompact sct = tempIdList.remove(i);
                i--;
                additionalIDs.add(sct.getSctId());
//                uuidsWriter.write(sct.toString());
            }
        }
//        uuidsWriter.flush();
//        uuidsWriter.close();
        sb.append("\r\n::: countSctDuplicates = ");
        sb.append(countSctDuplicates);
        sb.append("\r\n::: countSctPairUuidChanged = ");
        sb.append(countSctPairUuidChanged);
        sb.append("\r\n");
        LOG.info(sb.toString());
        if (countSctDuplicates > 0) {
            throw new UnsupportedOperationException("duplicate sctids not supported");
        }
        this.sctIdArray = new long[idList.size()];
        this.uuidMsbArray = new long[idList.size()];
        this.uuidLsbArray = new long[idList.size()];
        for (int i = 0; i < idList.size(); i++) {
            Sct2_IdCompact sct2_IdCompact = idList.get(i);
            this.sctIdArray[i] = sct2_IdCompact.getSctId();
            this.uuidMsbArray[i] = sct2_IdCompact.getUuidMsb();
            this.uuidLsbArray[i] = sct2_IdCompact.getUuidLsb();
            if (UUIDtoSCTMap != null) {
                UUIDtoSCTMap.put(new UUID(sct2_IdCompact.getUuidMsb(), sct2_IdCompact.getUuidLsb()), sct2_IdCompact.getSctId());
            }
        }
    }

    public UUID getUuid(String sctIdString) {
        return getUuid(Long.parseLong(sctIdString));
    }

    public UUID getUuid(long sctId) {
        int idx = Arrays.binarySearch(sctIdArray, sctId);
        if (idx >= 0) {
            long msb = uuidMsbArray[idx];
            long lsb = uuidLsbArray[idx];
            return new UUID(msb, lsb);
        } else {
            return null;
        }
    }
    
    public Long getSCTId(UUID uuid)
    {
        return UUIDtoSCTMap == null ? null : UUIDtoSCTMap.get(uuid);
    }
}
