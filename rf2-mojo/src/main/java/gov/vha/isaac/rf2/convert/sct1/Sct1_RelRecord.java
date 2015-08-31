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

public class Sct1_RelRecord implements Comparable<Object>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAB_CHARACTER = "\t";
        
    // :yyy: private UUID uuid; // COMPUTED RELATIONSHIPID
    private long relSnoId; // SNOMED RELATIONSHIPID, if applicable
    private long relUuidMsb;
    private long relUuidLsb;
    // List<EIdentifier> additionalIds;
    private ArrayList<Sct1_IdRecord> addedIds;
    private int status; // status is computed for relationships
    private long c1SnoId; // CONCEPTID1
    private long c1UuidMsb;
    private long c1UuidLsb;
    private long roleTypeSnoId; // RELATIONSHIPTYPE .. SNOMED ID
    private int roleTypeIdx; // RELATIONSHIPTYPE .. index
    private long c2SnoId; // CONCEPTID2
    private long c2UuidMsb;
    private long c2UuidLsb;
    private int characteristic; // CHARACTERISTICTYPE
    private int refinability; // REFINABILITY
    private int group; // RELATIONSHIPGROUP
    boolean exceptionFlag; // to handle Concept ID change exception
    private long revTime;
    private int pathIdx; // index
    private int userIdx; // user: 0=unassigned, 1=inferred/classifier
    private int moduleIdx;
    

    public Sct1_RelRecord(long relID, int st,
            long cOneID, long roleTypeSnoId, int roleTypeIdx, long cTwoID,
            int characterType, int r, int grp) {
        this.setRelSnoId(relID); // RELATIONSHIPID
        UUID tmpUUID = UuidT3Generator.fromSNOMED(getRelSnoId());
        this.setRelUuidMsb(tmpUUID.getMostSignificantBits());
        this.setRelUuidLsb(tmpUUID.getLeastSignificantBits());

        // additionalIds = null;
        setAddedIds(null);
        this.setStatus(st); // status is computed for relationships
        this.setC1SnoId(cOneID); // CONCEPTID1
        
        tmpUUID = UuidT3Generator.fromSNOMED(getC1SnoId());
        this.setC1UuidMsb(tmpUUID.getMostSignificantBits());
        this.setC1UuidLsb(tmpUUID.getLeastSignificantBits());
        
        this.setRoleTypeSnoId(roleTypeSnoId); // RELATIONSHIPTYPE (SNOMED ID) 
        this.setRoleTypeIdx(roleTypeIdx); // RELATIONSHIPTYPE  <-- INDEX (NOT SNOMED ID) 
        
        this.setC2SnoId(cTwoID); // CONCEPTID2
        tmpUUID = UuidT3Generator.fromSNOMED(getC2SnoId());
        this.setC2UuidMsb(tmpUUID.getMostSignificantBits());
        this.setC2UuidLsb(tmpUUID.getLeastSignificantBits());

        this.setCharacteristic(characterType); // CHARACTERISTICTYPE
        this.setRefinability(r); // REFINABILITY
        this.setGroup(grp); // RELATIONSHIPGROUP
        this.exceptionFlag = false;
        this.setUserIdx(0);
        this.setModuleIdx(-1);
    }

    public Sct1_RelRecord(long relID, int st,
            long cOneID, long roleTypeSnoId, int roleTypeIdx, long cTwoID,
            int characterType, int r, int grp, int pathIdx, int userIdx) {
    
        this.setRelSnoId(relID); // RELATIONSHIPID
        UUID tmpUUID = UuidT3Generator.fromSNOMED(getRelSnoId());
        this.setRelUuidMsb(tmpUUID.getMostSignificantBits());
        this.setRelUuidLsb(tmpUUID.getLeastSignificantBits());

        // additionalIds = null;
        setAddedIds(null);
        this.setStatus(st); // status is computed for relationships
        this.setC1SnoId(cOneID); // CONCEPTID1
        
        tmpUUID = UuidT3Generator.fromSNOMED(getC1SnoId());
        this.setC1UuidMsb(tmpUUID.getMostSignificantBits());
        this.setC1UuidLsb(tmpUUID.getLeastSignificantBits());
        
        this.setRoleTypeSnoId(roleTypeSnoId); // RELATIONSHIPTYPE (SNOMED ID) 
        this.setRoleTypeIdx(roleTypeIdx); // RELATIONSHIPTYPE  <-- INDEX (NOT SNOMED ID) 
        
        this.setC2SnoId(cTwoID); // CONCEPTID2
        tmpUUID = UuidT3Generator.fromSNOMED(getC2SnoId());
        this.setC2UuidMsb(tmpUUID.getMostSignificantBits());
        this.setC2UuidLsb(tmpUUID.getLeastSignificantBits());

        this.setCharacteristic(characterType); // CHARACTERISTICTYPE
        this.setRefinability(r); // REFINABILITY
        this.setGroup(grp); // RELATIONSHIPGROUP
        this.exceptionFlag = false;
        
        this.setPathIdx(pathIdx);
        this.setUserIdx(userIdx);
        this.setModuleIdx(-1);
    }
    
    public Sct1_RelRecord(UUID uuidRelId, int status,
            UUID uuidC1, int roleTypeIdx, UUID uuidC2,
            int characteristic, int refinability, int group, long revTime,
            int pathIdx, int userIdx, int moduleIdx) {
        
        Long temp = Rf2x.getSCTIDforUUID(uuidRelId);
        this.setRelSnoId(temp == null ? Long.MAX_VALUE : temp); // SNOMED RELATIONSHIPID, if applicable
        this.setRelUuidMsb(uuidRelId.getMostSignificantBits());
        this.setRelUuidLsb(uuidRelId.getLeastSignificantBits());
        // additionalIds = null;
        setAddedIds(null);
        this.setStatus(status); // status is computed for relationships
        this.setC1SnoId(Long.MAX_VALUE); // CONCEPTID1
        this.setC1UuidMsb(uuidC1.getMostSignificantBits());
        this.setC1UuidLsb(uuidC1.getLeastSignificantBits());
        this.setRoleTypeSnoId(Long.MAX_VALUE); // max not assigned or unknown
        this.setRoleTypeIdx(roleTypeIdx); // RELATIONSHIPTYPE
        this.setC2SnoId(Long.MAX_VALUE); // CONCEPTID2
        this.setC2UuidMsb(uuidC2.getMostSignificantBits());
        this.setC2UuidLsb(uuidC2.getLeastSignificantBits());
        this.setCharacteristic(characteristic); // CHARACTERISTICTYPE
        this.setRefinability(refinability); // REFINABILITY
        this.setGroup(group); // RELATIONSHIPGROUP
        this.exceptionFlag = false; // to handle Concept ID change exception
        this.setPathIdx(pathIdx);
        this.setRevTime(revTime);
        this.setUserIdx(userIdx); // ARF user id.
        this.setModuleIdx(moduleIdx);
    }

    // method required for object to be sortable (comparable) in arrays
    // SORT ORDER MATTERS WHEN ATTACHING IDS
    // THIS SORT MUST RETAIN UUID AT THE PRIMARY SORT ORDER
    @Override
    public int compareTo(Object obj) {
        Sct1_RelRecord tmp = (Sct1_RelRecord) obj;
        // :yyy: return this.uuid.compareTo(tmp.uuid);
        int thisMore = 1;
        int thisLess = -1;
        if (getRelUuidMsb() > tmp.getRelUuidMsb()) {
            return thisMore;
        } else if (getRelUuidMsb() < tmp.getRelUuidMsb()) {
            return thisLess;
        } else {
            if (getRelUuidLsb() > tmp.getRelUuidLsb()) {
                return thisMore;
            } else if (getRelUuidLsb() < tmp.getRelUuidLsb()) {
                return thisLess;
            } else {
                if (this.getPathIdx() > tmp.getPathIdx()) {
                    return thisMore;
                } else if (this.getPathIdx() < tmp.getPathIdx()) {
                    return thisLess;
                } else {
                    if (this.getRevTime() > tmp.getRevTime()) {
                        return thisMore;
                    } else if (this.getRevTime() < tmp.getRevTime()) {
                        return thisLess;
                    } else {
                        if (this.getUserIdx() > tmp.getUserIdx()) {
                            return thisMore;
                        } else if (this.getUserIdx() < tmp.getUserIdx()) {
                            return thisLess;
                        } else {
                            return 0; // EQUAL
                        }
                    }
                }
            }
        }
    }

    // Create string to show some input fields for exception reporting
    @Override
    public String toString() {
        UUID uuid = new UUID(getRelUuidMsb(), getRelUuidLsb()); // :yyy:
        return uuid + TAB_CHARACTER + getRelSnoId() + TAB_CHARACTER + getStatus() + TAB_CHARACTER
                + getC1SnoId() + TAB_CHARACTER + getRoleTypeIdx() + TAB_CHARACTER + getC2SnoId() + TAB_CHARACTER + getGroup();
    }

    public long getC2UuidLsb()
    {
        return c2UuidLsb;
    }

    public void setC2UuidLsb(long c2UuidLsb)
    {
        this.c2UuidLsb = c2UuidLsb;
    }

    public long getRelUuidMsb()
    {
        return relUuidMsb;
    }

    public void setRelUuidMsb(long relUuidMsb)
    {
        this.relUuidMsb = relUuidMsb;
    }

    public long getRelUuidLsb()
    {
        return relUuidLsb;
    }

    public void setRelUuidLsb(long relUuidLsb)
    {
        this.relUuidLsb = relUuidLsb;
    }

    public long getC2UuidMsb()
    {
        return c2UuidMsb;
    }

    public void setC2UuidMsb(long c2UuidMsb)
    {
        this.c2UuidMsb = c2UuidMsb;
    }

    public long getC1UuidMsb()
    {
        return c1UuidMsb;
    }

    public void setC1UuidMsb(long c1UuidMsb)
    {
        this.c1UuidMsb = c1UuidMsb;
    }

    public long getC1UuidLsb()
    {
        return c1UuidLsb;
    }

    public void setC1UuidLsb(long c1UuidLsb)
    {
        this.c1UuidLsb = c1UuidLsb;
    }

    public long getRelSnoId()
    {
        return relSnoId;
    }

    public void setRelSnoId(long relSnoId)
    {
        this.relSnoId = relSnoId;
    }

    public long getC1SnoId()
    {
        return c1SnoId;
    }

    public void setC1SnoId(long c1SnoId)
    {
        this.c1SnoId = c1SnoId;
    }

    public ArrayList<Sct1_IdRecord> getAddedIds()
    {
        return addedIds;
    }

    public void setAddedIds(ArrayList<Sct1_IdRecord> addedIds)
    {
        this.addedIds = addedIds;
    }

    public int getGroup()
    {
        return group;
    }

    public void setGroup(int group)
    {
        this.group = group;
    }

    public int getRoleTypeIdx()
    {
        return roleTypeIdx;
    }

    public void setRoleTypeIdx(int roleTypeIdx)
    {
        this.roleTypeIdx = roleTypeIdx;
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

    public int getCharacteristic()
    {
        return characteristic;
    }

    public void setCharacteristic(int characteristic)
    {
        this.characteristic = characteristic;
    }

    public int getRefinability()
    {
        return refinability;
    }

    public void setRefinability(int refinability)
    {
        this.refinability = refinability;
    }

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
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

    public long getRoleTypeSnoId()
    {
        return roleTypeSnoId;
    }

    public void setRoleTypeSnoId(long roleTypeSnoId)
    {
        this.roleTypeSnoId = roleTypeSnoId;
    }

    public long getC2SnoId()
    {
        return c2SnoId;
    }

    public void setC2SnoId(long c2SnoId)
    {
        this.c2SnoId = c2SnoId;
    }
}
