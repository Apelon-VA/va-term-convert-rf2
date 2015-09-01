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
import java.util.ArrayList;
import java.util.UUID;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import gov.vha.isaac.rf2.convert.Rf2x;

public class Sct1_ConRecord implements Comparable<Object>, Serializable {
    private static final long serialVersionUID = 1L;

    private static final String TAB_CHARACTER = "\t";

    // RECORD FIELDS
    private long conSnoId; //  CONCEPTID
    private long conUuidMsb; // CONCEPTID
    private long conUuidLsb; // CONCEPTID
    private int status; // CONCEPTSTATUS
    // ArrayList<EIdentifier> additionalIds;
    private ArrayList<Sct1_IdRecord> addedIds;
    private String ctv3id; // CTV3ID
    private String snomedrtid; // SNOMEDID (SNOMED RT ID)
    private int isprimitive; // ISPRIMITIVE
    private int pathIdx;
    int authorIdx;
    private int moduleIdx;
    private long revTime;

    public Sct1_ConRecord(long csId, int s, String ctv, String rt, int p) {
        setConSnoId(csId);
        UUID tmpUUID = UuidT3Generator.fromSNOMED(getConSnoId());
        this.setConUuidMsb(tmpUUID.getMostSignificantBits());
        this.setConUuidLsb(tmpUUID.getLeastSignificantBits());
        this.setStatus(s);
        // additionalIds = null;
        this.setAddedIds(null);
        this.setCtv3id(ctv);
        this.setSnomedrtid(rt);
        this.setIsprimitive(p);
        this.authorIdx = -1;
        this.setModuleIdx(-1);
    }
    
    public Sct1_ConRecord(UUID cUuid, int s, int p,
            long revDate, int pathIdx, int authorIdx, int moduleIdx) {
        Long temp = Rf2x.getSCTIDforUUID(cUuid);
        this.setConSnoId(temp == null ? Long.MAX_VALUE : temp);
        this.setConUuidMsb(cUuid.getMostSignificantBits());
        this.setConUuidLsb(cUuid.getLeastSignificantBits());
        this.setStatus(s);
        // additionalIds = null;
        this.setAddedIds(null);
        this.setCtv3id(null);
        this.setSnomedrtid(null);
        this.setIsprimitive(p);
        this.setRevTime(revDate);
        this.setPathIdx(pathIdx);
        this.authorIdx = authorIdx;
        this.setModuleIdx(moduleIdx);
    }

    // method required for object to be sortable (comparable) in arrays
    @Override
    public int compareTo(Object obj) {
        Sct1_ConRecord tmp = (Sct1_ConRecord) obj;
        int thisMore = 1;
        int thisLess = -1;
        if (this.getConUuidMsb() < tmp.getConUuidMsb()) {
            return thisLess; // instance less than received
        } else if (this.getConUuidMsb() > tmp.getConUuidMsb()) {
            return thisMore; // instance greater than received
        } else {
            if (getConUuidLsb() < tmp.getConUuidLsb()) {
                return thisLess;
            } else if (getConUuidLsb() > tmp.getConUuidLsb()) {
                return thisMore;
            } else {
                if (this.getPathIdx() < tmp.getPathIdx()) {
                    return thisLess; // instance less than received
                } else if (this.getPathIdx() > tmp.getPathIdx()) {
                    return thisMore; // instance greater than received
                } else {
                    if (this.getRevTime() < tmp.getRevTime()) {
                        return thisLess; // instance less than received
                    } else if (this.getRevTime() > tmp.getRevTime()) {
                        return thisMore; // instance greater than received
                    } else {
                        return 0; // instance == received
                    }
                }
            }
        }
    }

    // Create string to show some input fields for exception reporting
    @Override
    public String toString() {
        UUID uuid = new UUID(getConUuidMsb(), getConUuidLsb()); // :yyy:
        return uuid + TAB_CHARACTER + getStatus() + TAB_CHARACTER + getIsprimitive();
    }

    public ArrayList<Sct1_IdRecord> getAddedIds()
    {
        return addedIds;
    }

    public void setAddedIds(ArrayList<Sct1_IdRecord> addedIds)
    {
        this.addedIds = addedIds;
    }

    public long getConSnoId()
    {
        return conSnoId;
    }

    public void setConSnoId(long conSnoId)
    {
        this.conSnoId = conSnoId;
    }

    public long getConUuidLsb()
    {
        return conUuidLsb;
    }

    public void setConUuidLsb(long conUuidLsb)
    {
        this.conUuidLsb = conUuidLsb;
    }

    public long getConUuidMsb()
    {
        return conUuidMsb;
    }

    public void setConUuidMsb(long conUuidMsb)
    {
        this.conUuidMsb = conUuidMsb;
    }

    public int getPathIdx()
    {
        return pathIdx;
    }

    public void setPathIdx(int pathIdx)
    {
        this.pathIdx = pathIdx;
    }

    public long getRevTime()
    {
        return revTime;
    }

    public void setRevTime(long revTime)
    {
        this.revTime = revTime;
    }

    public int getIsprimitive()
    {
        return isprimitive;
    }

    public void setIsprimitive(int isprimitive)
    {
        this.isprimitive = isprimitive;
    }

    public int getModuleIdx()
    {
        return moduleIdx;
    }

    public void setModuleIdx(int moduleIdx)
    {
        this.moduleIdx = moduleIdx;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public String getSnomedrtid()
    {
        return snomedrtid;
    }

    public void setSnomedrtid(String snomedrtid)
    {
        this.snomedrtid = snomedrtid;
    }

    public String getCtv3id()
    {
        return ctv3id;
    }

    public void setCtv3id(String ctv3id)
    {
        this.ctv3id = ctv3id;
    }

}
