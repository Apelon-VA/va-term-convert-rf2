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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.maven.plugin.MojoFailureException;
import org.ihtsdo.otf.tcc.api.metadata.binding.SnomedMetadataRf2;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;
import gov.vha.isaac.ochre.util.UuidT5Generator;

/**
 *
 * @author marc
 */
public class Rf2_RefsetId {

    public static final String SUBSETMEMBER_ID_NAMESPACE_UUID_TYPE1 = "b7d13800-e38d-11df-bccf-0800200c9a66";
    public static final String REFSET_ID_NAMESPACE_UUID_TYPE1 = "d0b3c9c0-e395-11df-bccf-0800200c9a66";
    public static final String SUBSETPATH_ID_NAMESPACE_UUID_TYPE1 = "e1cff9e0-e395-11df-bccf-0800200c9a66";
    public static final String HISTORY_TABLE_REFERENCES_NAMESPACE_UUID_TYPE1 =
            "22928260-08d8-11e0-81e0-0800200c9a66";
    private long refsetSctIdOriginal;
    private String refsetUuidStr;
    private String refsetDate;
    private String refsetPathUuidStr;
    private String refsetPrefTerm;
    private String refsetFsName;
    private String refsetParentUuid;
    private boolean isSnomedRefset; // Use SNOMED 'Is a'

    public Rf2_RefsetId(long refsetSctIdOriginal, String refsetDate,
            String refsetPathUuidStr, String refsetPrefTerm,
            String refsetFsName, String refsetParentUuid)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {

        this.refsetSctIdOriginal = refsetSctIdOriginal;
        if (this.refsetSctIdOriginal < Long.MAX_VALUE) {
            this.refsetUuidStr = UuidT5Generator.get(REFSET_ID_NAMESPACE_UUID_TYPE1
                    + Long.toString(refsetSctIdOriginal)).toString();
        } else {
            this.refsetUuidStr = UuidT5Generator.get(REFSET_ID_NAMESPACE_UUID_TYPE1
                    + refsetFsName).toString();
        }
        this.refsetDate = refsetDate;
        this.refsetPathUuidStr = refsetPathUuidStr;
        this.refsetPrefTerm = refsetPrefTerm;
        this.refsetFsName = refsetFsName;
        this.refsetParentUuid = refsetParentUuid;
        this.isSnomedRefset = false;
    }

    public Rf2_RefsetId(long refsetSctIdOriginal, String refsetDate,
            String refsetPathUuidStr, String refsetPrefTerm,
            String refsetFsName, String refsetParentUuid,
            boolean snomedIsa)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        this.refsetSctIdOriginal = refsetSctIdOriginal;
        if (this.refsetSctIdOriginal < Long.MAX_VALUE) {
            this.refsetUuidStr = UuidT5Generator.get(REFSET_ID_NAMESPACE_UUID_TYPE1
                    + Long.toString(refsetSctIdOriginal)).toString();
        } else {
            this.refsetUuidStr = UuidT5Generator.get(REFSET_ID_NAMESPACE_UUID_TYPE1
                    + refsetFsName).toString();
        }
        this.refsetDate = refsetDate;
        this.refsetPathUuidStr = refsetPathUuidStr;
        this.refsetPrefTerm = refsetPrefTerm;
        this.refsetFsName = refsetFsName;
        this.refsetParentUuid = refsetParentUuid;
        this.isSnomedRefset = snomedIsa;
    }

//    public Rf2_RefsetId(long refsetSctIdOriginal, String refsetDate,
//            String refsetPathUuidStr, String refsetPrefTerm,
//            String refsetFsName, String refsetParentUuid, boolean snomedIsa)
//            throws NoSuchAlgorithmException, UnsupportedEncodingException {
//    }
    public String getRefsetUuidStr() {
        return refsetUuidStr;
    }

    public static void saveRefsetConcept(String arfDir, List<Rf2_RefsetId> subsetIds)
            throws MojoFailureException {

        try {
            String uuidCurrentStr = SnomedMetadataRf2.ACTIVE_VALUE_RF2.getPrimodialUuid().toString();

            String infix = subsetIds.get(0).refsetFsName.replace(" ", "");
            infix = infix.replace("-", "");
            infix = infix.replace("(", "");
            infix = infix.replace(")", "");

            Writer concepts;
            concepts = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(
                    arfDir, "concepts_" + infix + ".txt")), "UTF-8"));
            Writer descriptions = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    new File(arfDir, "descriptions_" + infix + ".txt")), "UTF-8"));
            Writer relationships = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(
                    new File(arfDir, "relationships_" + infix + ".txt")), "UTF-8"));
            Writer ids = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(
                    arfDir, "ids_" + infix + ".txt")), "UTF-8"));

            for (Rf2_RefsetId sid : subsetIds) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
                Date d = format.parse(sid.refsetDate);
                format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String effectiveDate = format.format(d);

                concepts.append(sid.refsetUuidStr); // refset concept uuid
                concepts.append("\t");
                concepts.append(uuidCurrentStr); //status uuid
                concepts.append("\t");
                concepts.append("1"); // primitive
                concepts.append("\t");
                concepts.append(effectiveDate); // effective date
                concepts.append("\t");
                concepts.append(sid.refsetPathUuidStr); //path uuid
                concepts.append("\n");

                if (sid.refsetSctIdOriginal < Long.MAX_VALUE) {
                    ids.append(sid.refsetUuidStr); // refset concept uuid
                    ids.append("\t");
                    //source uuid
                    ids.append(IsaacMetadataAuxiliaryBinding.SNOMED_INTEGER_ID.getPrimodialUuid().toString());
                    ids.append("\t");
                    ids.append(Long.toString(sid.refsetSctIdOriginal)); //source id
                    ids.append("\t");
                    ids.append(uuidCurrentStr); //status uuid
                    ids.append("\t");
                    ids.append(effectiveDate); // effective date
                    ids.append("\t");
                    ids.append(sid.refsetPathUuidStr); //path uuid
                    ids.append("\n");
                }

                if (sid.refsetFsName != null) {
                    descriptions.append(UuidT5Generator.get(
                            REFSET_ID_NAMESPACE_UUID_TYPE1
                            + "Subset Fully Specified Name"
                            + sid.refsetFsName).toString()); // description uuid
                    descriptions.append("\t");
                    descriptions.append(uuidCurrentStr); // status uuid
                    descriptions.append("\t");
                    descriptions.append(sid.refsetUuidStr).toString(); // refset concept uuid
                    descriptions.append("\t");
                    descriptions.append(sid.refsetFsName); // term
                    descriptions.append("\t");
                    descriptions.append("1"); // primitive
                    descriptions.append("\t");
                    descriptions.append(IsaacMetadataAuxiliaryBinding.FULLY_SPECIFIED_NAME.getPrimodialUuid().toString());
                    descriptions.append("\t");
                    descriptions.append("en"); // language code
                    descriptions.append("\t");
                    descriptions.append(effectiveDate); // effective date
                    descriptions.append("\t");
                    descriptions.append(sid.refsetPathUuidStr); //path uuid
                    descriptions.append("\n");
                }

                if (sid.refsetPrefTerm != null) {
                    descriptions.append(UuidT5Generator.get(
                            REFSET_ID_NAMESPACE_UUID_TYPE1 + "Subset Preferred Name"
                            + sid.refsetPrefTerm).toString()); // description uuid
                    descriptions.append("\t");
                    descriptions.append(uuidCurrentStr); // status uuid
                    descriptions.append("\t");
                    descriptions.append(sid.refsetUuidStr).toString(); // refset concept uuid
                    descriptions.append("\t");
                    descriptions.append(sid.refsetPrefTerm); // term
                    descriptions.append("\t");
                    descriptions.append("1"); // primitive
                    descriptions.append("\t");
                    descriptions.append(IsaacMetadataAuxiliaryBinding.PREFERRED.getPrimodialUuid().toString());
                    descriptions.append("\t");
                    descriptions.append("en"); // language code
                    descriptions.append("\t");
                    descriptions.append(effectiveDate); // effective date
                    descriptions.append("\t");
                    descriptions.append(sid.refsetPathUuidStr); //path uuid
                    descriptions.append("\n");
                }

                if (sid.refsetSctIdOriginal < Long.MAX_VALUE) {
                    relationships.append(UuidT5Generator.get(
                            REFSET_ID_NAMESPACE_UUID_TYPE1 + "Relationship"
                            + sid.refsetSctIdOriginal).toString()); // relationship uuid
                } else {
                    relationships.append(UuidT5Generator.get(
                            REFSET_ID_NAMESPACE_UUID_TYPE1 + "Relationship"
                            + sid.refsetUuidStr).toString()); // relationship uuid
                }
                relationships.append("\t");
                relationships.append(uuidCurrentStr); // status uuid
                relationships.append("\t");
                relationships.append(sid.refsetUuidStr); // refset source concept uuid
                relationships.append("\t");
                if (sid.isSnomedRefset) {
                    relationships.append(IsaacMetadataAuxiliaryBinding.IS_A.getPrimodialUuid().toString());
                } else {
                    relationships.append(TermAux.IS_A.getPrimodialUuid().toString());
                }
                relationships.append("\t");
                relationships.append(sid.refsetParentUuid); // destination concept uuid
                relationships.append("\t");
                relationships.append(IsaacMetadataAuxiliaryBinding.STATED.getPrimodialUuid().toString());
                relationships.append("\t");
                relationships.append(SnomedMetadataRf2.NOT_REFINABLE_RF2.getPrimodialUuid().toString());
                relationships.append("\t");
                relationships.append("0"); // relationship group
                relationships.append("\t");
                relationships.append(effectiveDate); // effective date
                relationships.append("\t");
                relationships.append(sid.refsetPathUuidStr); // path uuid
                relationships.append("\n");
            }

            concepts.close();
            descriptions.close();
            relationships.close();
            ids.close();

        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(Rf2_RefsetId.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoFailureException("saveRefsetConcept UnsupportedEncodingException", ex);
        } catch (ParseException ex) {
            Logger.getLogger(Rf2_RefsetId.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoFailureException("saveRefsetConcept Parse Error", ex);
        } catch (IOException ex) {
            Logger.getLogger(Rf2_RefsetId.class.getName()).log(Level.SEVERE, null, ex);
            throw new MojoFailureException("saveRefsetConcept IO Error", ex);
        } 
    }
}
