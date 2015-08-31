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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoFailureException;

public class Sct2_ConRecord implements Comparable<Sct2_ConRecord>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String LINE_TERMINATOR = "\r\n";
    private static final String TAB_CHARACTER = "\t";
    // RECORD FIELDS
    private long conSnoIdL; //  id
    private String effDateStr; // effectiveTime
    long timeL;
    boolean isActive; // CONCEPTSTATUS
    boolean isPrimitiveB; // ISPRIMITIVE
    long statusConceptL; // extended from AttributeValue file

    private String pathUuidStr; // SNOMED Core default
    // String authorUuidStr; // saved as user
    private String moduleUuidStr;

    public Sct2_ConRecord(long conIdL, String dateStr, boolean active, String moduleUuidStr,
            boolean isPrim, long statusConceptL, String pathUuid) throws ParseException {
        this.setConSnoIdL(conIdL); // column 0 - id
        this.setEffDateStr(dateStr); // column 1 - effectiveTime
        this.timeL = Rf2x.convertDateToTime(dateStr);
        this.isActive = active; // column 2 - active


        // column 4 - defintionStatusId converted to isPrimative
        this.isPrimitiveB = isPrim;

        this.statusConceptL = statusConceptL;

        // POM parameter.
        this.setPathUuidStr(pathUuid);
        // this.authorUuidStr = Rf2Defaults.getAuthorUuidStr();
        this.setModuleUuidStr(moduleUuidStr);
    }

    public Sct2_ConRecord(Sct2_ConRecord in, long time, long status) throws ParseException {
        this.setConSnoIdL(in.getConSnoIdL());
        this.setEffDateStr(in.getEffDateStr());
        this.timeL = time;
        this.isActive = in.isActive;
        this.isPrimitiveB = in.isPrimitiveB;

        this.statusConceptL = status;

        this.setPathUuidStr(in.getPathUuidStr());
        // this.authorUuidStr = in.authorUuidStr;
        this.setModuleUuidStr(in.getModuleUuidStr());
    }

    public static Sct2_ConRecord[] attachStatus(Sct2_ConRecord[] a, Rf2_RefsetCRecord[] b)
            throws ParseException, MojoFailureException {
        ArrayList<Sct2_ConRecord> addedRecords = new ArrayList<>();
        Rf2_RefsetCRecord zeroB = new Rf2_RefsetCRecord("ZERO", "2000-01-01 00:00:00", false,
                null, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        Arrays.sort(a);
        Arrays.sort(b);

        int idxA = 0;
        int idxB = 0;
        long currentId = a[0].getConSnoIdL();
        while (idxA < a.length) {
            ArrayList<Sct2_ConRecord> listA = new ArrayList<>();
            ArrayList<Rf2_RefsetCRecord> listB = new ArrayList<>();
            while (idxA < a.length && a[idxA].getConSnoIdL() == currentId) {
                listA.add(a[idxA]);
                idxA++;
            }
            while (idxB < b.length && b[idxB].referencedComponentIdL == currentId) {
                listB.add(b[idxB]);
                idxB++;
            }

            // PROCESS ID
            if (listB.size() > 0) {
                if (listA.get(0).timeL < listB.get(0).timeL) {
                    listB.add(0, zeroB);
                }
                int idxAA = 0;
                int idxBB = 0;
                boolean moreToDo = true;
                while (moreToDo) {
                    // determine time range
                    long timeInAA = listA.get(idxAA).timeL;
                    long timeOutAA = Long.MAX_VALUE;
                    if (idxAA + 1 < listA.size()) {
                        timeOutAA = listA.get(idxAA + 1).timeL;
                    }
                    long timeInBB = listB.get(idxBB).timeL;
                    long timeOutBB = Long.MAX_VALUE;
                    if (idxBB + 1 < listB.size()) {
                        timeOutBB = listB.get(idxBB + 1).timeL;
                    }

                    // UPDATE VALUES
                    if (timeInAA >= timeInBB) {
                        if (listB.get(idxBB).isActive) {
                            listA.get(idxAA).statusConceptL = listB.get(idxBB).valueIdL;
                        } else {
                            // no change
                        }
                    } else {
                        if (listB.get(idxBB).isActive) {
                            addedRecords.add(new Sct2_ConRecord(listA.get(idxAA),
                                    listB.get(idxBB).timeL, listB.get(idxBB).valueIdL));
                        } else {
                            addedRecords.add(new Sct2_ConRecord(listA.get(idxAA),
                                    listB.get(idxBB).timeL, Long.MAX_VALUE));
                        }
                    }

                    // DETERMINE NEXT TO PROCESS
                    if (timeOutAA == Long.MAX_VALUE && timeOutBB == Long.MAX_VALUE) {
                        moreToDo = false;
                    } else if (timeOutAA < timeOutBB) {
                        idxAA++;
                    } else if (timeOutAA == timeOutBB) {
                        idxAA++;
                        idxBB++;
                    } else if (timeOutAA > timeOutBB) {
                        idxBB++;
                    }
                }
            }

            // NEXT ID
            if (idxA < a.length) {
                currentId = a[idxA].getConSnoIdL();
            }
            if (idxB < b.length) {
                while (idxB < b.length && b[idxB].referencedComponentIdL < currentId) {
                    idxB++;
                }
            }
        }

        if (addedRecords.size() > 0) {
            int offsetI = a.length;
            a = Arrays.copyOf(a, a.length + addedRecords.size());
            for (int i = 0; i < addedRecords.size(); i++) {
                a[offsetI + i] = addedRecords.get(i);
            }
        }

        // REMOVE DUPLICATES
        a = removeDuplicates(a);

        return a;
    }

    static private Sct2_ConRecord[] removeDuplicates(Sct2_ConRecord[] a) throws MojoFailureException {
        Arrays.sort(a);

        // REMOVE DUPLICATES
        int lenA = a.length;
        ArrayList<Integer> duplIdxList = new ArrayList<>();
        for (int idx = 0; idx < lenA - 2; idx++) {
            if ((a[idx].getConSnoIdL() == a[idx + 1].getConSnoIdL())
                    && (a[idx].isPrimitiveB == a[idx + 1].isPrimitiveB)
                    && (a[idx].statusConceptL == a[idx + 1].statusConceptL)
                    && a[idx].getModuleUuidStr().equalsIgnoreCase(a[idx + 1].getModuleUuidStr())) {
                if (a[idx].statusConceptL == Long.MAX_VALUE) {
                    if (a[idx].isActive == a[idx + 1].isActive) {
                        duplIdxList.add(Integer.valueOf(idx + 1));
                    }
                } else {
                    duplIdxList.add(Integer.valueOf(idx + 1));
                }
            }
        }

        if (duplIdxList.size()
                > 0) {
            Sct2_ConRecord[] b = new Sct2_ConRecord[lenA - duplIdxList.size()];
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

    public static Sct2_ConRecord[] parseConcepts(Rf2File f, String pathUuid) throws MojoFailureException {
        try {
            int count = Rf2File.countFileLines(f);
            Sct2_ConRecord[] a = new Sct2_ConRecord[count];

            // DATA COLUMNS
            int ID = 0;// id
            int EFFECTIVE_TIME = 1; // effectiveTime
            int ACTIVE = 2; // active
            int MODULE_ID = 3; // moduleId
            int DEFINITION_STATUS_ID = 4; // definitionStatusId

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(f.getFile()), "UTF-8"));

            int idx = 0;
            br.readLine(); // Header row
            while (br.ready()) {
                String[] line = br.readLine().split(TAB_CHARACTER);

                a[idx] = new Sct2_ConRecord(Long.parseLong(line[ID]),
                        Rf2x.convertEffectiveTimeToDate(line[EFFECTIVE_TIME]),
                        Rf2x.convertStringToBoolean(line[ACTIVE]),
                        Rf2x.convertSctIdToUuidStr(line[MODULE_ID]),
                        Rf2x.convertDefinitionStatusToIsPrimitive(line[DEFINITION_STATUS_ID]),
                        Long.MAX_VALUE,
                        pathUuid);
                idx++;
            }

            return a;

        } catch (ParseException | IOException ex) {
            Logger.getLogger(Sct2_ConRecord.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoFailureException(
                    "error parsing rf2 concepts", ex);
        }
    }

    // Create string to show some input fields for exception reporting
    @Override
    public String toString() {
        return getConSnoIdL() + TAB_CHARACTER + isActive + TAB_CHARACTER + isPrimitiveB;
    }

    public void setPath(String pathStr) {
        this.setPathUuidStr(pathStr);
    }
    
    public void writeArf(BufferedWriter writer) throws IOException, ParseException {
        // Concept UUID
        writer.append(Rf2x.convertSctIdToUuidStr(getConSnoIdL()) + TAB_CHARACTER);

        // Status UUID
        if (statusConceptL < Long.MAX_VALUE) {
            writer.append(Rf2x.convertSctIdToUuidStr(statusConceptL) + TAB_CHARACTER);
        } else {
            writer.append(Rf2x.convertActiveToStatusUuid(isActive) + TAB_CHARACTER);
        }

        // Primitive string 0 (false == defined) or 1 (true == primitive)
        if (isPrimitiveB) {
            writer.append("1" + TAB_CHARACTER);
        } else {
            writer.append("0" + TAB_CHARACTER);
        }

        // Effective Date yyyy-MM-dd HH:mm:ss
        writer.append(Rf2x.convertTimeToDate(timeL) + TAB_CHARACTER);

        // Path UUID String
        writer.append(this.getPathUuidStr() + TAB_CHARACTER);

        // Author UUID String --> user
        writer.append(Rf2Defaults.getAuthorUuidStr() + TAB_CHARACTER);

        // Module UUID String
        writer.append(this.getModuleUuidStr() + LINE_TERMINATOR);
    }

    @Override
    public int compareTo(Sct2_ConRecord t) {
        if (this.getConSnoIdL() < t.getConSnoIdL()) {
            return -1; // instance less than received
        } else if (this.getConSnoIdL() > t.getConSnoIdL()) {
            return 1; // instance greater than received
        } else {
            if (this.timeL < t.timeL) {
                return -1; // instance less than received
            } else if (this.timeL > t.timeL) {
                return 1; // instance greater than received
            }
        }
        return 0; // instance == received
    }

    public String getPathUuidStr()
    {
        return pathUuidStr;
    }

    public void setPathUuidStr(String pathUuidStr)
    {
        this.pathUuidStr = pathUuidStr;
    }

    public String getEffDateStr()
    {
        return effDateStr;
    }

    public void setEffDateStr(String effDateStr)
    {
        this.effDateStr = effDateStr;
    }

    public long getConSnoIdL()
    {
        return conSnoIdL;
    }

    public void setConSnoIdL(long conSnoIdL)
    {
        this.conSnoIdL = conSnoIdL;
    }

    public String getModuleUuidStr()
    {
        return moduleUuidStr;
    }

    public void setModuleUuidStr(String moduleUuidStr)
    {
        this.moduleUuidStr = moduleUuidStr;
    }
}