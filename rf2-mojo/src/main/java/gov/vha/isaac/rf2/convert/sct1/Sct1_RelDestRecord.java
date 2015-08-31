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

public class Sct1_RelDestRecord implements Comparable<Object>, Serializable {
    private static final long serialVersionUID = 1L;

    // private UUID uuid; // COMPUTED RELATIONSHIPID
    long relUuidMsb; // RELATIONSHIPID
    long relUuidLsb; // RELATIONSHIPID
    private long c2UuidMsb;
    private long c2UuidLsb;
    private int roleTypeIdx; // RELATIONSHIPTYPE  IDX !!!

    public Sct1_RelDestRecord(long uuidRelMsb, long uuidRelLsb, long uuidC2Msb, long uuidC2Lsb,
            int roleTypeIdx) {
        super();
        this.relUuidMsb = uuidRelMsb;
        this.relUuidLsb = uuidRelLsb;
        setC2UuidMsb(uuidC2Msb);
        setC2UuidLsb(uuidC2Lsb);
        this.setRoleTypeIdx(roleTypeIdx);
    }

    // method required for object to be sortable (comparable) in arrays
    @Override
    public int compareTo(Object obj) {
        Sct1_RelDestRecord tmp = (Sct1_RelDestRecord) obj;
        int thisMore = 1;
        int thisLess = -1;
        if (getRoleTypeIdx() > tmp.getRoleTypeIdx()) {
            return thisMore;
        } else if (getRoleTypeIdx() < tmp.getRoleTypeIdx()) {
            return thisLess;
        } else {
            return 0; // EQUAL
        }
    }

    public long getC2UuidMsb()
    {
        return c2UuidMsb;
    }

    public void setC2UuidMsb(long c2UuidMsb)
    {
        this.c2UuidMsb = c2UuidMsb;
    }

    public long getC2UuidLsb()
    {
        return c2UuidLsb;
    }

    public void setC2UuidLsb(long c2UuidLsb)
    {
        this.c2UuidLsb = c2UuidLsb;
    }

    public int getRoleTypeIdx()
    {
        return roleTypeIdx;
    }

    public void setRoleTypeIdx(int roleTypeIdx)
    {
        this.roleTypeIdx = roleTypeIdx;
    }

}
