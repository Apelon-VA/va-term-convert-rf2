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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;
import gov.vha.isaac.ochre.util.UuidT3Generator;
import gov.vha.isaac.rf2.convert.Rf2x;

public class Sct1_DesRecord implements Comparable<Object>, Serializable {
    private static final long serialVersionUID = 1L;
    private static final String TAB_CHARACTER = "\t";

    private long desSnoId; // DESCRIPTIONID
    private long desUuidMsb;
    private long desUuidLsb;
    private int status; // DESCRIPTIONSTATUS
    // ArrayList<EIdentifier> additionalIds;
    private ArrayList<Sct1_IdRecord> addedIds;
    private long conSnoId; // CONCEPTID
    private long conUuidMsb; // CONCEPTID
    private long conUuidLsb; // CONCEPTID    
    private String termText; // TERM
    private int capStatus; // INITIALCAPITALSTATUS -- capitalization
    private int descriptionType; // DESCRIPTIONTYPE
    private String languageCode; // LANGUAGECODE
    private long revTime;
    private int pathIdx;
    int authorIdx;
    private int moduleIdx;

    public Sct1_DesRecord(long dId, int s, long cId, String text, int cStat, int typeInt, String lang) {
        this.setDesSnoId(dId);
        UUID tmpUUID = UuidT3Generator.fromSNOMED(this.getDesSnoId());
        this.setDesUuidMsb(tmpUUID.getMostSignificantBits());
        this.setDesUuidLsb(tmpUUID.getLeastSignificantBits());

        this.setStatus(s);
        // additionalIds = null;
        this.setAddedIds(null);

        this.setConSnoId(cId);
        tmpUUID = UuidT3Generator.fromSNOMED(this.getConSnoId());
        this.setConUuidMsb(tmpUUID.getMostSignificantBits());
        this.setConUuidLsb(tmpUUID.getLeastSignificantBits());

        this.setTermText(new String(text));
        this.setCapStatus(cStat);
        this.setDescriptionType(typeInt);
        this.setLanguageCode(new String(lang));
        this.setPathIdx(-1);
        this.authorIdx = -1;
        this.setModuleIdx(-1);
    }

    public Sct1_DesRecord(UUID desUuid, int status, UUID uuidCon, String termStr,
            int capitalization, int desTypeIdx, String langCodeStr, long revTime,
            int pathIdx, int authorIdx, int moduleIdx) {
        Long temp = Rf2x.getSCTIDforUUID(desUuid);
        this.setDesSnoId(temp == null ? Long.MAX_VALUE : temp); // DESCRIPTIONID
        this.setDesUuidMsb(desUuid.getMostSignificantBits());
        this.setDesUuidLsb(desUuid.getLeastSignificantBits());
        this.setStatus(status); // DESCRIPTIONSTATUS
        // additionalIds = null;
        setAddedIds(null);
        this.setConSnoId(Long.MAX_VALUE); // CONCEPTID
        this.setConUuidMsb(uuidCon.getMostSignificantBits()); // CONCEPTID
        this.setConUuidLsb(uuidCon.getLeastSignificantBits()); // CONCEPTID    
        this.setTermText(termStr); // TERM
        this.setCapStatus(capitalization); // INITIALCAPITALSTATUS -- capitalization
        this.setDescriptionType(desTypeIdx); // DESCRIPTIONTYPE
        this.setLanguageCode(langCodeStr); // LANGUAGECODE
        this.setRevTime(revTime);
        this.setPathIdx(pathIdx);
        this.authorIdx = authorIdx;
        this.setModuleIdx(moduleIdx);
    }

    // method required for object to be sortable (comparable) in arrays
    @Override
    public int compareTo(Object obj) {
        Sct1_DesRecord o2 = (Sct1_DesRecord) obj;
        int thisMore = 1;
        int thisLess = -1;
        // DESCRIPTION UUID
        if (this.getDesUuidMsb() > o2.getDesUuidMsb()) {
            return thisMore;
        } else if (this.getDesUuidMsb() < o2.getDesUuidMsb()) {
            return thisLess;
        } else {
            if (this.getDesUuidLsb() > o2.getDesUuidLsb()) {
                return thisMore;
            } else if (this.getDesUuidLsb() < o2.getDesUuidLsb()) {
                return thisLess;
            } else {
                // Path
                if (this.getPathIdx() > o2.getPathIdx()) {
                    return thisMore;
                } else if (this.getPathIdx() < o2.getPathIdx()) {
                    return thisLess;
                } else {
                    // Revision
                    if (this.getRevTime() > o2.getRevTime()) {
                        return thisMore;
                    } else if (this.getRevTime() < o2.getRevTime()) {
                        return thisLess;
                    } else {
                        return 0; // EQUAL
                    }
                }
            }
        }
    }

    public static Sct1_DesRecord[] parseDescriptions(Sct1File sct1File) throws IOException {

        int count = Sct1File.countFileLines(sct1File);
        Sct1_DesRecord[] a = new Sct1_DesRecord[count];

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(
                sct1File.file), "UTF-8"));
        int descriptions = 0;

        int DESCRIPTIONID = 0;
        int DESCRIPTIONSTATUS = 1;
        int CONCEPTID = 2;
        int TERM = 3;
        int INITIALCAPITALSTATUS = 4;
        int DESCRIPTIONTYPE = 5;
        int LANGUAGECODE = 6;

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
            // LANGUAGECODE
            String lang = line[LANGUAGECODE];

            // Save to sortable array
            a[descriptions] = new Sct1_DesRecord(descriptionId, status, conSnoId, text, capStatus,
                    typeInt, lang);
            descriptions++;

        }
        Arrays.sort(a);
        r.close();
        
        return a;
    }
    
    // Create string to show some input fields for exception reporting
    // DESCRIPTIONID    DESCRIPTIONSTATUS   CONCEPTID   TERM    INITIALCAPITALSTATUS    DESCRIPTIONTYPE LANGUAGECODE
    public static String toStringHeader() {
        return "DESCRIPTIONID" + TAB_CHARACTER + "DESCRIPTIONSTATUS" + TAB_CHARACTER + "CONCEPTID"
                + TAB_CHARACTER + "TERM" + TAB_CHARACTER + "INITIALCAPITALSTATUS" + TAB_CHARACTER
                + "DESCRIPTIONTYPE" + TAB_CHARACTER + "LANGUAGECODE";
    }

    // Create string to show some input fields for exception reporting
    @Override
    public String toString() {
        return getDesSnoId() + TAB_CHARACTER + getStatus() + TAB_CHARACTER + getConSnoId() + TAB_CHARACTER
                + getTermText() + TAB_CHARACTER + getCapStatus() + TAB_CHARACTER + getDescriptionType()
                + TAB_CHARACTER + getLanguageCode();
    }

    public ArrayList<Sct1_IdRecord> getAddedIds()
    {
        return addedIds;
    }

    public void setAddedIds(ArrayList<Sct1_IdRecord> addedIds)
    {
        this.addedIds = addedIds;
    }

    public long getDesSnoId()
    {
        return desSnoId;
    }

    public void setDesSnoId(long desSnoId)
    {
        this.desSnoId = desSnoId;
    }

    public long getDesUuidMsb()
    {
        return desUuidMsb;
    }

    public void setDesUuidMsb(long desUuidMsb)
    {
        this.desUuidMsb = desUuidMsb;
    }

    public long getDesUuidLsb()
    {
        return desUuidLsb;
    }

    public void setDesUuidLsb(long desUuidLsb)
    {
        this.desUuidLsb = desUuidLsb;
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

    public int getStatus()
    {
        return status;
    }

    public void setStatus(int status)
    {
        this.status = status;
    }

    public long getConSnoId()
    {
        return conSnoId;
    }

    public void setConSnoId(long conSnoId)
    {
        this.conSnoId = conSnoId;
    }

    public String getTermText()
    {
        return termText;
    }

    public void setTermText(String termText)
    {
        this.termText = termText;
    }

    public int getModuleIdx()
    {
        return moduleIdx;
    }

    public void setModuleIdx(int moduleIdx)
    {
        this.moduleIdx = moduleIdx;
    }

    public int getCapStatus()
    {
        return capStatus;
    }

    public void setCapStatus(int capStatus)
    {
        this.capStatus = capStatus;
    }

    public int getDescriptionType()
    {
        return descriptionType;
    }

    public void setDescriptionType(int descriptionType)
    {
        this.descriptionType = descriptionType;
    }

    public String getLanguageCode()
    {
        return languageCode;
    }

    public void setLanguageCode(String languageCode)
    {
        this.languageCode = languageCode;
    }

}
