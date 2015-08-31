package gov.vha.isaac.rf2.convert.sct1;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class Sct1_RefSetRecord implements Comparable<Sct1_RefSetRecord>, Serializable {

    public enum ComponentType {

        CONCEPT, DESCRIPTION, IMAGE, MEMBER, RELATIONSHIP, UNKNOWN
    };

    public enum ValueType {

        BOOLEAN, CONCEPT, INTEGER, STRING, C_FLOAT, STRING_STRING
    };
    private static final long serialVersionUID = 1L;
    private long conUuidMsb; // ENVELOP CONCEPTID (eConcept to which this concept belongs)
    private long conUuidLsb; // ENVELOP CONCEPTID
    private long referencedComponentUuidMsb;
    private long referencedComponentUuidLsb;
    private ComponentType componentType;
    private long refsetUuidMsb;
    private long refsetUuidLsb;
    private long refsetMemberUuidMsb; // aka primordialComponentUuidMsb
    private long refsetMemberUuidLsb; // aka primordialComponentUuidLsb
    private boolean valueBoolean;
    private long valueConUuidMsb;
    private long valueConUuidLsb;
    private int valueInt;
    private float valueFloat;
    private String valueString1;
    private String valueString2;
    private ValueType valueType;
    private int status; // CONCEPTSTATUS
    private long revTime;
    private int pathIdx;
    private int authorIdx;
    private int moduleIdx;

    // BOOLEAN
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx,
            boolean valueBoolean) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(valueBoolean);
        this.setValueConUuidMsb(Long.MAX_VALUE);
        this.setValueConUuidLsb(Long.MAX_VALUE);
        this.setValueInt(Integer.MAX_VALUE);
        this.setValueFloat(Float.MAX_VALUE);
        this.setValueString1(null);
        this.setValueString2(null);
        this.setValueType(ValueType.BOOLEAN); // BOOLEAN

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }

    // CONCEPT
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx,
            UUID vConcept) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(false);
        this.setValueConUuidMsb(vConcept.getMostSignificantBits());
        this.setValueConUuidLsb(vConcept.getLeastSignificantBits());
        this.setValueInt(Integer.MAX_VALUE);
        this.setValueFloat(Float.MAX_VALUE);
        this.setValueString1(null);
        this.setValueString2(null);
        this.setValueType(ValueType.CONCEPT); // CONCEPT

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }

    // INTEGER
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx,
            int vInteger) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(false);
        this.setValueConUuidMsb(Long.MAX_VALUE);
        this.setValueConUuidLsb(Long.MAX_VALUE);
        this.setValueInt(vInteger);
        this.setValueFloat(Float.MAX_VALUE);
        this.setValueString1(null);
        this.setValueString2(null);
        this.setValueType(ValueType.INTEGER); // INTEGER

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }
    
    // FLOAT
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx, UUID vConcept,
            float vFloat) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(false);
        this.setValueConUuidMsb(vConcept.getMostSignificantBits());
        this.setValueConUuidLsb(vConcept.getLeastSignificantBits());
        this.setValueInt(Integer.MAX_VALUE);
        this.setValueFloat(vFloat);
        this.setValueString1(null);
        this.setValueString2(null);
        this.setValueType(ValueType.C_FLOAT); // FLOAT

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }

    // STRING
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx,
            String vString) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(false);
        this.setValueConUuidMsb(Long.MAX_VALUE);
        this.setValueConUuidLsb(Long.MAX_VALUE);
        this.setValueInt(Integer.MAX_VALUE);
        this.setValueFloat(Float.MAX_VALUE);
        this.setValueString1(vString);
        this.setValueString2(null);
        this.setValueType(ValueType.STRING); // STRING

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }
    
    // STRING_STRING
    public Sct1_RefSetRecord(UUID refsetUuid, UUID memberUuid, UUID componentUuid, int status,
            long zRevTime, int zPathIdx, int zAuthIdx, int zModuleIdx,
            String vString1, String vString2) {
        super();
        this.setConUuidMsb(Long.MAX_VALUE);
        this.setConUuidLsb(Long.MAX_VALUE);
        this.setReferencedComponentUuidMsb(componentUuid.getMostSignificantBits());
        this.setReferencedComponentUuidLsb(componentUuid.getLeastSignificantBits());
        this.setComponentType(ComponentType.UNKNOWN);
        this.setRefsetUuidMsb(refsetUuid.getMostSignificantBits());
        this.setRefsetUuidLsb(refsetUuid.getLeastSignificantBits());
        this.setRefsetMemberUuidMsb(memberUuid.getMostSignificantBits());
        this.setRefsetMemberUuidLsb(memberUuid.getLeastSignificantBits());

        this.setValueBoolean(false);
        this.setValueConUuidMsb(Long.MAX_VALUE);
        this.setValueConUuidLsb(Long.MAX_VALUE);
        this.setValueInt(Integer.MAX_VALUE);
        this.setValueFloat(Float.MAX_VALUE);
        this.setValueString1(vString1);
        this.setValueString2(vString2);
        this.setValueType(ValueType.STRING_STRING); // STRING_STRING

        this.setStatus(status);
        this.setRevTime(zRevTime);
        this.setPathIdx(zPathIdx);
        this.setAuthorIdx(zAuthIdx);
        this.setModuleIdx(zModuleIdx);
    }

    public void setEnvelopConUuid(UUID conUuid, ComponentType cType) {
        this.setConUuidMsb(conUuid.getMostSignificantBits());
        this.setConUuidLsb(conUuid.getLeastSignificantBits());
        this.setComponentType(cType);
    }

    @Override
    public int compareTo(Sct1_RefSetRecord o) {
        int thisMore = 1;
        int thisLess = -1;
        if (this.getReferencedComponentUuidMsb() < o.getReferencedComponentUuidMsb()) {
            return thisLess; // instance less than received
        } else if (this.getReferencedComponentUuidMsb() > o.getReferencedComponentUuidMsb()) {
            return thisMore; // instance greater than received
        } else {
            if (this.getReferencedComponentUuidLsb() < o.getReferencedComponentUuidLsb()) {
                return thisLess;
            } else if (this.getReferencedComponentUuidLsb() > o.getReferencedComponentUuidLsb()) {
                return thisMore;
            } else {
                if (this.getRefsetUuidMsb() < o.getRefsetUuidMsb()) {
                    return thisLess; // instance less than received
                } else if (this.getRefsetUuidMsb() > o.getRefsetUuidMsb()) {
                    return thisMore; // instance greater than received
                } else {
                    if (this.getRefsetUuidLsb() < o.getRefsetUuidLsb()) {
                        return thisLess;
                    } else if (this.getRefsetUuidLsb() > o.getRefsetUuidLsb()) {
                        return thisMore;
                    } else {
                        if (this.getRefsetMemberUuidMsb() < o.getRefsetMemberUuidMsb()) {
                            return thisLess; // instance less than received
                        } else if (this.getRefsetMemberUuidMsb() > o.getRefsetMemberUuidMsb()) {
                            return thisMore; // instance greater than received
                        } else {
                            if (this.getRefsetMemberUuidLsb() < o.getRefsetMemberUuidLsb()) {
                                return thisLess;
                            } else if (this.getRefsetMemberUuidLsb() > o.getRefsetMemberUuidLsb()) {
                                return thisMore;
                            } else {
                                if (this.getRevTime() < o.getRevTime()) {
                                    return thisLess;
                                } else if (this.getRevTime() > o.getRevTime()) {
                                    return thisMore;
                                } else {
                                    return 0; // instance == received
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("::: REFSET MEMBER RECORD :::");
        sb.append("\r\n::: referencedComponentUuid ");
        sb.append(new UUID(this.getReferencedComponentUuidMsb(), this.getReferencedComponentUuidLsb()));

        sb.append("\r\n::: (envelop) conUuid ");
        sb.append(new UUID(this.getConUuidMsb(), this.getConUuidLsb()));

        sb.append("\r\n::: referencedComponentUuid ");
        sb.append(new UUID(this.getReferencedComponentUuidMsb(), this.getReferencedComponentUuidLsb()));

        sb.append("\r\n::: refsetMemberUuid ");
        sb.append(new UUID(this.getRefsetMemberUuidMsb(), this.getRefsetMemberUuidLsb()));

        sb.append("\r\n::: status ").append(this.getStatus());

        Date d = new Date(this.getRevTime());
        String pattern = "yyyy-MM-dd HH:mm:ss";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        formatter.format(d);

        sb.append("\r\n::: revision date ").append(formatter.format(d).toString());

        if (this.getValueType() == Sct1_RefSetRecord.ValueType.STRING) {
            sb.append("\r\n::: value string ").append(this.getValueString1());
        }else if (this.getValueType() == Sct1_RefSetRecord.ValueType.STRING_STRING) {
            sb.append("\r\n::: value string1 ").append(this.getValueString1());
            sb.append("\r\n::: value string2 ").append(this.getValueString2());
        } else if (this.getValueType() == Sct1_RefSetRecord.ValueType.BOOLEAN) {
            sb.append("\r\n::: value boolean ").append(this.isValueBoolean());
        } else if (this.getValueType() == Sct1_RefSetRecord.ValueType.INTEGER) {
            sb.append("\r\n::: value integer ").append(this.getValueInt());
        } else if (this.getValueType() == Sct1_RefSetRecord.ValueType.C_FLOAT) {
            sb.append("\r\n::: value float ").append(this.getValueFloat());
        } else if (this.getValueType() == Sct1_RefSetRecord.ValueType.CONCEPT) {
            sb.append("\r\n::: value concept ");
            sb.append(new UUID(this.getValueConUuidMsb(), this.getValueConUuidLsb()));
        }
        sb.append("\r\n:::\r\n");

        return sb.toString();
    }

    public long getConUuidMsb()
    {
        return conUuidMsb;
    }

    public void setConUuidMsb(long conUuidMsb)
    {
        this.conUuidMsb = conUuidMsb;
    }

    public long getConUuidLsb()
    {
        return conUuidLsb;
    }

    public void setConUuidLsb(long conUuidLsb)
    {
        this.conUuidLsb = conUuidLsb;
    }

    public ComponentType getComponentType()
    {
        return componentType;
    }

    public void setComponentType(ComponentType componentType)
    {
        this.componentType = componentType;
    }

    public long getReferencedComponentUuidMsb()
    {
        return referencedComponentUuidMsb;
    }

    public void setReferencedComponentUuidMsb(long referencedComponentUuidMsb)
    {
        this.referencedComponentUuidMsb = referencedComponentUuidMsb;
    }

    public long getReferencedComponentUuidLsb()
    {
        return referencedComponentUuidLsb;
    }

    public void setReferencedComponentUuidLsb(long referencedComponentUuidLsb)
    {
        this.referencedComponentUuidLsb = referencedComponentUuidLsb;
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

    public long getRefsetUuidLsb()
    {
        return refsetUuidLsb;
    }

    public void setRefsetUuidLsb(long refsetUuidLsb)
    {
        this.refsetUuidLsb = refsetUuidLsb;
    }

    public long getRefsetUuidMsb()
    {
        return refsetUuidMsb;
    }

    public void setRefsetUuidMsb(long refsetUuidMsb)
    {
        this.refsetUuidMsb = refsetUuidMsb;
    }

    public long getRefsetMemberUuidMsb()
    {
        return refsetMemberUuidMsb;
    }

    public void setRefsetMemberUuidMsb(long refsetMemberUuidMsb)
    {
        this.refsetMemberUuidMsb = refsetMemberUuidMsb;
    }

    public long getRefsetMemberUuidLsb()
    {
        return refsetMemberUuidLsb;
    }

    public void setRefsetMemberUuidLsb(long refsetMemberUuidLsb)
    {
        this.refsetMemberUuidLsb = refsetMemberUuidLsb;
    }

    public int getAuthorIdx()
    {
        return authorIdx;
    }

    public void setAuthorIdx(int authorIdx)
    {
        this.authorIdx = authorIdx;
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

    public boolean isValueBoolean()
    {
        return valueBoolean;
    }

    public void setValueBoolean(boolean valueBoolean)
    {
        this.valueBoolean = valueBoolean;
    }

    public long getValueConUuidLsb()
    {
        return valueConUuidLsb;
    }

    public void setValueConUuidLsb(long valueConUuidLsb)
    {
        this.valueConUuidLsb = valueConUuidLsb;
    }

    public long getValueConUuidMsb()
    {
        return valueConUuidMsb;
    }

    public void setValueConUuidMsb(long valueConUuidMsb)
    {
        this.valueConUuidMsb = valueConUuidMsb;
    }

    public int getValueInt()
    {
        return valueInt;
    }

    public void setValueInt(int valueInt)
    {
        this.valueInt = valueInt;
    }

    public ValueType getValueType()
    {
        return valueType;
    }

    public void setValueType(ValueType valueType)
    {
        this.valueType = valueType;
    }

    public String getValueString1()
    {
        return valueString1;
    }

    public void setValueString1(String valueString1)
    {
        this.valueString1 = valueString1;
    }

    public float getValueFloat()
    {
        return valueFloat;
    }

    public void setValueFloat(float valueFloat)
    {
        this.valueFloat = valueFloat;
    }

    public String getValueString2()
    {
        return valueString2;
    }

    public void setValueString2(String valueString2)
    {
        this.valueString2 = valueString2;
    }
}
