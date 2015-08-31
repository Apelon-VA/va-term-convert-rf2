/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
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
package gov.vha.isaac.rf2.convert.sct1;

import java.io.Serializable;
import java.util.UUID;

public class Sct1_IdRecord implements Comparable<Sct1_IdRecord>, Serializable {

    private static final long serialVersionUID = 1L;
    private long primaryUuidMsb; // CONCEPTID/PRIMARYID
    private long primaryUuidLsb; // CONCEPTID/PRIMARYID
    // SOURCE UUID
    // ArchitectonicAuxiliary.Concept.ICD_9.getUids().get(0)
    private int srcSystemIdx;
    // SOURCE ID -- DENOTATION
    private String denotation;
    private long denotationLong;
    // STATUS UUID
    // ArchitectonicAuxiliary.Concept.CURRENT.getUids().get(0)
    private int status;
    private long revTime; // EFFECTIVE DATE
    private int pathIdx; // PATH
    private int userIdx; // USER
    private int moduleIdx; // MODULE

    // Each non-String must be conditionally added.
    public Sct1_IdRecord(UUID uuidPrimaryId, int sourceSystemIdx, String idFromSourceSystem,
            int status, long revDateTime, int pathIdx, int userIdx, int moduleIdx) {
        this.setPrimaryUuidMsb(uuidPrimaryId.getMostSignificantBits()); // CONCEPTID/PRIMARYID
        this.setPrimaryUuidLsb(uuidPrimaryId.getLeastSignificantBits()); // CONCEPTID/PRIMARYID
        this.setSrcSystemIdx(sourceSystemIdx);
        if (sourceSystemIdx == 0) { // SNOMED Long is index "0"
            this.setDenotation(null);
            this.setDenotationLong(Long.parseLong(idFromSourceSystem));
        } else {
            this.setDenotation(idFromSourceSystem);
            this.setDenotationLong(Long.MAX_VALUE);
        }
        this.setStatus(status);
        this.setRevTime(revDateTime);
        this.setPathIdx(pathIdx);
        this.setUserIdx(userIdx);
        this.setModuleIdx(moduleIdx);
    }

    // :NYI:HACK:
    // long long UUID called only from SCT1 input
    // in this case, the denotation is always long for the SNOMED_ID 
    public Sct1_IdRecord(long uuidPrimaryMsb, long uuidPrimaryLsb, int sourceSystemIdx, long idFromSourceSystem,
            int status, long revDateTime, int pathIdx, int uIdx) {
        this.setPrimaryUuidMsb(uuidPrimaryMsb); // MSB CONCEPTID/PRIMARYID
        this.setPrimaryUuidLsb(uuidPrimaryLsb); // LSB CONCEPTID/PRIMARYID
        this.setSrcSystemIdx(sourceSystemIdx);
        this.setDenotation(null);
        this.setDenotationLong(idFromSourceSystem);
        this.setStatus(status);
        this.setRevTime(revDateTime);
        this.setPathIdx(pathIdx);
        this.setUserIdx(uIdx);
        this.setModuleIdx(-1);
    }

    @Override
    public int compareTo(Sct1_IdRecord o) {
        int thisMore = 1;
        int thisLess = -1;
        if (getPrimaryUuidMsb() > o.getPrimaryUuidMsb()) {
            return thisMore;
        } else if (getPrimaryUuidMsb() < o.getPrimaryUuidMsb()) {
            return thisLess;
        } else {
            if (getPrimaryUuidLsb() > o.getPrimaryUuidLsb()) {
                return thisMore;
            } else if (getPrimaryUuidLsb() < o.getPrimaryUuidLsb()) {
                return thisLess;
            } else {
                if (this.getPathIdx() > o.getPathIdx()) {
                    return thisMore;
                } else if (this.getPathIdx() < o.getPathIdx()) {
                    return thisLess;
                } else {
                    if (this.getRevTime() > o.getRevTime()) {
                        return thisMore;
                    } else if (this.getRevTime() < o.getRevTime()) {
                        return thisLess;
                    } else {
                        if (this.getUserIdx() > o.getUserIdx()) {
                            return thisMore;
                        } else if (this.getUserIdx() < o.getUserIdx()) {
                            return thisLess;
                        } else {
                            return 0; // EQUAL
                        }
                    }
                }
            }
        }
    }

    public int getSrcSystemIdx()
    {
        return srcSystemIdx;
    }

    public void setSrcSystemIdx(int srcSystemIdx)
    {
        this.srcSystemIdx = srcSystemIdx;
    }

    public long getPrimaryUuidMsb()
    {
        return primaryUuidMsb;
    }

    public void setPrimaryUuidMsb(long primaryUuidMsb)
    {
        this.primaryUuidMsb = primaryUuidMsb;
    }

    public long getPrimaryUuidLsb()
    {
        return primaryUuidLsb;
    }

    public void setPrimaryUuidLsb(long primaryUuidLsb)
    {
        this.primaryUuidLsb = primaryUuidLsb;
    }

    public long getRevTime()
    {
        return revTime;
    }

    public void setRevTime(long revTime)
    {
        this.revTime = revTime;
    }

    public int getUserIdx()
    {
        return userIdx;
    }

    public void setUserIdx(int userIdx)
    {
        this.userIdx = userIdx;
    }

    public int getModuleIdx()
    {
        return moduleIdx;
    }

    public void setModuleIdx(int moduleIdx)
    {
        this.moduleIdx = moduleIdx;
    }

    public int getPathIdx()
    {
        return pathIdx;
    }

    public void setPathIdx(int pathIdx)
    {
        this.pathIdx = pathIdx;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getDenotation()
    {
        return denotation;
    }

    public void setDenotation(String denotation)
    {
        this.denotation = denotation;
    }

    public long getDenotationLong()
    {
        return denotationLong;
    }

    public void setDenotationLong(long denotationLong)
    {
        this.denotationLong = denotationLong;
    }
}
