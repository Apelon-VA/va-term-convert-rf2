package gov.vha.isaac.rf2.convert.sct1;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;
import gov.vha.isaac.ochre.util.UuidT5Generator;

public class Sct1Dir {
    private String directoryName;

    private Boolean keepQualifierFromInferred; // 1
    private Boolean keepHistoricalFromInferred; // 2
    private Boolean keepAdditionalFromInferred; // 3

    private Boolean mapSctIdInferredToStated; // :OLD: old mapping approach
    private Boolean statedSctRelIdPresent;

    private String wbPathUuidCoreFromName; // Workbench Path Name
    private String wbPathUuidStatedFromName; // Workbench Path Name
    private String wbPathUuidInferredFromName; // Workbench Path Name
    private UUID wbPathUuidCore; // UUID derived from name
    private UUID wbPathUuidStated; // UUID derived from name
    private UUID wbPathUuidInferred; // UUID derived from name

    public Sct1Dir() {
        this.directoryName = "";

        this.mapSctIdInferredToStated = false;
        this.statedSctRelIdPresent = false;

        this.keepQualifierFromInferred = false; // 1
        this.keepHistoricalFromInferred = false; // 2
        this.keepAdditionalFromInferred = false; // 3
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String dirName) {
        this.directoryName = dirName;
    }

    public Boolean getKeepHistoricalFromInferred() {
        return keepHistoricalFromInferred;
    }

    /*
     * String setter is required because POM passes in String
     */
    public void setKeepHistoricalFromInferred(String s) {
        if (s.equalsIgnoreCase("true"))
            this.keepHistoricalFromInferred = true;
        else
            this.keepHistoricalFromInferred = false;
    }

    public void setKeepHistoricalFromInferred(Boolean keep) {
        this.keepHistoricalFromInferred = keep;
    }

    public Boolean getKeepQualifierFromInferred() {
        return keepQualifierFromInferred;
    }

    public Boolean isStatedSctRelIdPresent() {
        return statedSctRelIdPresent;
    }

    public Boolean getStatedSctRelIdPresent() {
        return statedSctRelIdPresent;
    }

    public void setStatedSctRelIdPresent(Boolean statedHasSctRelId) {
        this.statedSctRelIdPresent = statedHasSctRelId;
    }

    public void setStatedSctRelIdPresent(String s) {
        if (s.equalsIgnoreCase("true"))
            this.statedSctRelIdPresent = true;
        else
            this.statedSctRelIdPresent = false;
    }
    
    /*
     * String setter is required because POM passes in String
     */
    public void setKeepQualifierFromInferred(String s) {
        if (s.equalsIgnoreCase("true"))
            this.keepQualifierFromInferred = true;
        else
            this.keepQualifierFromInferred = false;
    }

    public void setKeepQualifierFromInferred(Boolean keep) {
        this.keepQualifierFromInferred = keep;
    }

    public Boolean getKeepAdditionalFromInferred() {
        return keepAdditionalFromInferred;
    }

    /*
     * String setter is required because POM passes in String
     */
    public void setKeepAdditionalFromInferred(String s) {
        if (s.equalsIgnoreCase("true"))
            this.keepAdditionalFromInferred = true;
        else
            this.keepAdditionalFromInferred = false;
    }

    public void setKeepAdditionalFromInferred(Boolean keep) {
        this.keepAdditionalFromInferred = keep;
    }

    public Boolean getMapSctIdInferredToStated() {
        return mapSctIdInferredToStated;
    }

    public Boolean doMapSctIdInferredToStated() {
        return mapSctIdInferredToStated;
    }

    public void setMapSctIdInferredToStated(String doIdMapping) {
        if (doIdMapping.equalsIgnoreCase("true"))
            this.mapSctIdInferredToStated = true;
        else
            this.mapSctIdInferredToStated = false;
    }

    public void setMapSctIdInferredToStated(Boolean mapSctIdInferredToStated) {
        this.mapSctIdInferredToStated = mapSctIdInferredToStated;
    }

    /* PATHS */
    public UUID getWbPathUuidCore() {
        return wbPathUuidCore;
    }

    /** UUID in String form */
    public void setWbPathUuidCore(String s) {
        this.wbPathUuidCore = UUID.fromString(s);
    }

    public String getWbPathUuidCoreFromName() {
        return wbPathUuidCoreFromName;
    }

    public void setCorePathUuid(String uuid) {
        this.wbPathUuidCoreFromName = uuid;
        this.wbPathUuidCore = UUID.fromString(uuid);
    }

    public void setCorePathName(String name) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        setWbPathUuidCoreFromName(name);
    }

    public void setWbPathUuidCoreFromName(String name) throws UnsupportedEncodingException,
            NoSuchAlgorithmException {
        this.wbPathUuidCoreFromName = name;
        this.wbPathUuidCore = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, name);
    }

    public UUID getWbPathUuidStated() {
        return wbPathUuidStated;
    }

    /** UUID in String form */
    public void setWbPathUuidStated(String s) {
        this.wbPathUuidStated = UUID.fromString(s);
    }

    public String getWbPathUuidStatedFromName() {
        return wbPathUuidStatedFromName;
    }

    public void setStatedPathUuid(String uuid) {
        this.wbPathUuidStatedFromName = uuid;
        this.wbPathUuidStated = UUID.fromString(uuid);
    }

    public void setStatedPathName(String name) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        setWbPathUuidStatedFromName(name);
    }

    public void setWbPathUuidStatedFromName(String name) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        this.wbPathUuidStatedFromName = name;
        this.wbPathUuidStated = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, name);
    }

    public UUID getWbPathUuidInferred() {
        return wbPathUuidInferred;
    }

    /** UUID in String form */
    public void setWbPathUuidInferred(String s) {
        this.wbPathUuidInferred = UUID.fromString(s);
    }

    public String getWbPathUuidInferredFromName() {
        return wbPathUuidInferredFromName;
    }

    public void setInferredPathUuid(String uuid) {
        this.wbPathUuidInferredFromName = uuid;
        this.wbPathUuidInferred = UUID.fromString(uuid);
    }

    public void setInferredPathName(String name) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        setWbPathUuidInferredFromName(name);
    }

    public void setWbPathUuidInferredFromName(String name) throws NoSuchAlgorithmException,
            UnsupportedEncodingException {
        this.wbPathUuidInferredFromName = name;
        this.wbPathUuidInferred = UuidT5Generator.get(UuidT5Generator.PATH_ID_FROM_FS_DESC, name);
    }

}