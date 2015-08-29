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
import java.util.UUID;
import org.apache.maven.plugin.MojoFailureException;

public class Sct2_DesRecord implements Comparable<Sct2_DesRecord>, Serializable {

    private static final long serialVersionUID = 1L;
    private static final String LINE_TERMINATOR = "\r\n";
    private static final String TAB_CHARACTER = "\t";
    long desSnoIdL; // DESCRIPTIONID
    String desUuidStr; // id
    String effDateStr; // effectiveTime
    long timeL;
    boolean isActive; // STATUS
    long statusConceptL; // extended from AttributeValue file
    String conUuidStr; // CONCEPTID
    String termText; // TERM
    boolean capStatus; // INITIALCAPITALSTATUS -- capitalization
    String descriptionTypeStr; // DESCRIPTIONTYPE
    String languageCodeStr; // LANGUAGECODE
    String pathUuidStr; // SNOMED Core default
    // String authorUuidStr; // saved as user
    String moduleUuidStr;

    public Sct2_DesRecord(long dId, String dateStr, boolean activeB, String moduleUuidStr,
            String conUuidStr, String termStr,
            boolean capitalization, String desTypeStr, String langCodeStr,
            long statusConceptL, String pathUuid)
            throws ParseException, IOException {
        desSnoIdL = dId;
        // UUID tmpUUID = Type3UuidFactory.fromSNOMED(desSnoIdL);
        UUID tmpUUID = Rf2x.convertSctIdToUuid(desSnoIdL);
        this.desUuidStr = tmpUUID.toString();
        this.effDateStr = dateStr;
        this.timeL = Rf2x.convertDateToTime(dateStr);
        this.isActive = activeB;

        this.conUuidStr = conUuidStr; // CONCEPTID

        this.termText = termStr; // TERM
        this.capStatus = capitalization; // INITIALCAPITALSTATUS -- capitalization
        this.descriptionTypeStr = desTypeStr; // DESCRIPTIONTYPE
        this.languageCodeStr = langCodeStr; // LANGUAGECODE

        this.statusConceptL = statusConceptL;

        // POM parameter.
        this.pathUuidStr = pathUuid;
        // this.authorUuidStr = Rf2Defaults.getAuthorUuidStr();
        this.moduleUuidStr = moduleUuidStr;
    }

    public Sct2_DesRecord(Sct2_DesRecord in, long time, long status)
            throws ParseException {
        this.desSnoIdL = in.desSnoIdL;
        this.desUuidStr = in.desUuidStr;
        this.effDateStr = in.effDateStr;
        this.timeL = time;
        this.isActive = in.isActive;

        this.conUuidStr = in.conUuidStr; // CONCEPTID

        this.termText = in.termText; // TERM
        this.capStatus = in.capStatus; // INITIALCAPITALSTATUS -- capitalization
        this.descriptionTypeStr = in.descriptionTypeStr; // DESCRIPTIONTYPE
        this.languageCodeStr = in.languageCodeStr; // LANGUAGECODE

        this.pathUuidStr = in.pathUuidStr;
        // this.authorUuidStr = in.authorUuidStr;
        this.moduleUuidStr = in.moduleUuidStr;

        this.statusConceptL = status;
    }

    static Sct2_DesRecord[] attachStatus(Sct2_DesRecord[] a, Rf2_RefsetCRecord[] b)
            throws ParseException, MojoFailureException {
        ArrayList<Sct2_DesRecord> addedRecords = new ArrayList<>();
        Rf2_RefsetCRecord zeroB = new Rf2_RefsetCRecord("ZERO", "2000-01-01 00:00:00", false,
                null, Long.MAX_VALUE, Long.MAX_VALUE, Long.MAX_VALUE);

        Arrays.sort(a);
        Arrays.sort(b);

        int idxA = 0;
        int idxB = 0;
        long currentId = a[0].desSnoIdL;
        while (idxA < a.length) {
            ArrayList<Sct2_DesRecord> listA = new ArrayList<>();
            ArrayList<Rf2_RefsetCRecord> listB = new ArrayList<>();
            while (idxA < a.length && a[idxA].desSnoIdL == currentId) {
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
                            addedRecords.add(new Sct2_DesRecord(listA.get(idxAA),
                                    listB.get(idxBB).timeL, listB.get(idxBB).valueIdL));
                        } else {
                            addedRecords.add(new Sct2_DesRecord(listA.get(idxAA),
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
                currentId = a[idxA].desSnoIdL;
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

    static private Sct2_DesRecord[] removeDuplicates(Sct2_DesRecord[] a)
            throws MojoFailureException {
        Arrays.sort(a);

        // REMOVE DUPLICATES
        int lenA = a.length;
        ArrayList<Integer> duplIdxList = new ArrayList<>();
        for (int idx = 0; idx < lenA - 2; idx++) {
            if ((a[idx].desSnoIdL == a[idx + 1].desSnoIdL)
                    && (a[idx].statusConceptL == a[idx + 1].statusConceptL)
                    && (a[idx].capStatus == a[idx + 1].capStatus)
                    && (a[idx].conUuidStr.compareToIgnoreCase(a[idx + 1].conUuidStr) == 0)
                    && (a[idx].termText.compareTo(a[idx + 1].termText) == 0)
                    && (a[idx].descriptionTypeStr.compareToIgnoreCase(a[idx + 1].descriptionTypeStr) == 0)
                    && (a[idx].languageCodeStr.compareTo(a[idx + 1].languageCodeStr) == 0)
                    && a[idx].moduleUuidStr.equalsIgnoreCase(a[idx + 1].moduleUuidStr)) {
                if (a[idx].statusConceptL == Long.MAX_VALUE) {
                    if (a[idx].isActive == a[idx + 1].isActive) {
                        duplIdxList.add(Integer.valueOf(idx + 1));
                    }
                } else {
                    duplIdxList.add(Integer.valueOf(idx + 1));
                }
            }
        }
        if (duplIdxList.size() > 0) {
            Sct2_DesRecord[] b = new Sct2_DesRecord[lenA - duplIdxList.size()];
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

    public static Sct2_DesRecord[] parseDescriptions(Rf2File f, String pathUuid)
            throws IOException, ParseException {

        int count = Rf2File.countFileLines(f);
        Sct2_DesRecord[] a = new Sct2_DesRecord[count];

        // DATA COLUMNS
        int ID = 0; // id
        int EFFECTIVE_TIME = 1; // effectiveTime
        int ACTIVE = 2; // active
        int MODULE_ID = 3; // moduleId
        int CONCEPT_ID = 4; // conceptId
        int LANGUAGE_CODE = 5; // languageCodeStr
        int TYPE_ID = 6; // typeId
        int TERM = 7; // term
        int CASE_SIGNIFICANCE_ID = 8; // caseSignificanceId

        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f.file), "UTF-8"));

        int idx = 0;
        r.readLine();  // Header row
        while (r.ready()) {
            String[] line = r.readLine().split(TAB_CHARACTER);

            a[idx] = new Sct2_DesRecord(Long.parseLong(line[ID]),
                    Rf2x.convertEffectiveTimeToDate(line[EFFECTIVE_TIME]),
                    Rf2x.convertStringToBoolean(line[ACTIVE]),
                    Rf2x.convertSctIdToUuidStr(line[MODULE_ID]),
                    Rf2x.convertSctIdToUuidStr(line[CONCEPT_ID]),
                    line[TERM],
                    Rf2x.convertCaseSignificanceIdToCapStatus(line[CASE_SIGNIFICANCE_ID]),
                    Rf2x.convertSctIdToUuidStr(line[TYPE_ID]),
                    line[LANGUAGE_CODE],
                    Long.MAX_VALUE,
                    pathUuid);
            idx++;
        }

        return a;
    }

    // Create string to show some input fields for exception reporting
    // DESCRIPTIONID DESCRIPTIONSTATUS CONCEPTID TERM INITIALCAPITALSTATUS DESCRIPTIONTYPE LANGUAGECODE
    public static String toStringHeader() {
        return "DESCRIPTIONID" + TAB_CHARACTER
                + "DESCRIPTIONSTATUS" + TAB_CHARACTER
                + "CONCEPTID" + TAB_CHARACTER
                + "TERM" + TAB_CHARACTER
                + "INITIALCAPITALSTATUS" + TAB_CHARACTER
                + "DESCRIPTIONTYPE" + TAB_CHARACTER
                + "LANGUAGECODE";
    }

    // Create string to show some input fields for exception reporting
    @Override
    public String toString() {
        return desUuidStr + TAB_CHARACTER 
                + isActive + TAB_CHARACTER
                + conUuidStr + TAB_CHARACTER
                + termText + TAB_CHARACTER
                + capStatus + TAB_CHARACTER
                + descriptionTypeStr + TAB_CHARACTER
                + languageCodeStr;
    }

    public void setPath(String pathStr) {
    	this.pathUuidStr = pathStr;
    }
    
    public void writeArf(BufferedWriter writer)
            throws IOException, ParseException {
        // Description UUID
        // writer.append(desUuidStr + TAB_CHARACTER);
        writer.append(Rf2x.convertSctIdToUuidStr(desSnoIdL) + TAB_CHARACTER);

        // Status UUID
        if (statusConceptL < Long.MAX_VALUE) {
            writer.append(Rf2x.convertSctIdToUuidStr(statusConceptL) + TAB_CHARACTER);
        } else {
            writer.append(Rf2x.convertActiveToStatusUuid(isActive) + TAB_CHARACTER);
        }

        // Concept UUID
        writer.append(conUuidStr + TAB_CHARACTER);

        // Term
        writer.append(termText + TAB_CHARACTER);

        // Capitalization Status
        if (capStatus) {
            writer.append("1" + TAB_CHARACTER);
        } else {
            writer.append("0" + TAB_CHARACTER);
        }

        // Description Type UUID
        writer.append(descriptionTypeStr + TAB_CHARACTER);

        // Language Code
        writer.append(languageCodeStr + TAB_CHARACTER);

        // Effective Date   yyyy-MM-dd HH:mm:ss
        writer.append(Rf2x.convertTimeToDate(timeL) + TAB_CHARACTER);

        // Path UUID String
        writer.append(this.pathUuidStr + TAB_CHARACTER);

        // Author UUID String --> user
        writer.append(Rf2Defaults.getAuthorUuidStr() + TAB_CHARACTER);

        // Module UUID String
        writer.append(this.moduleUuidStr + LINE_TERMINATOR);
    }

    @Override
    public int compareTo(Sct2_DesRecord t) {
        if (this.desSnoIdL < t.desSnoIdL) {
            return -1; // instance less than received
        } else if (this.desSnoIdL > t.desSnoIdL) {
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
}
