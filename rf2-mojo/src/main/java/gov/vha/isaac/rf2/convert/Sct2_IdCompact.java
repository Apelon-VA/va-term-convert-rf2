/*
 * Copyright 2012 International Health Terminology Standards Development Organisation.
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

import java.io.Serializable;
import static gov.vha.isaac.rf2.convert.Sct2_IdRecord.LINE_TERMINATOR;
import static gov.vha.isaac.rf2.convert.Sct2_IdRecord.TAB_CHARACTER;

/**
 *
 * @author logger
 */
public class Sct2_IdCompact implements Comparable<Sct2_IdCompact>, Serializable {

    private static final long serialVersionUID = 1L;
    public long uuidMsb_;
    public long uuidLsb_;
    public long sctId_;
    public long time_;

    public Sct2_IdCompact(long uuidMsbL, long uuidLsbL, long sctIdL, long time) {
        this.uuidMsb_ = uuidMsbL;
        this.uuidLsb_ = uuidLsbL;
        this.sctId_ = sctIdL;
        this.time_ = time; //assuming that path, module, user will all be the same (for now)
    }

    /**
     * Sort order:  SCTID, UUID long
     * @param o
     * @return
     */
    @Override
    public int compareTo(Sct2_IdCompact o) {
        if (this.sctId_ < o.sctId_) {
            return -1; // instance less than received
        } else if (this.sctId_ > o.sctId_) {
            return 1; // instance greater than received
        } else {
            if (this.uuidMsb_ < o.uuidMsb_) {
                return -1; // instance less than received
            } else if (this.uuidMsb_ > o.uuidMsb_) {
                return 1; // instance greater than received
            } else {
                if (this.uuidLsb_ < o.uuidLsb_) {
                    return -1; // instance less than received
                } else if (this.uuidLsb_ > o.uuidLsb_) {
                    return 1; // instance greater than received
                }
            }
            return 0; // instance == received
        }
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sctId_);
        sb.append(TAB_CHARACTER);
        sb.append(uuidLsb_);
        sb.append(TAB_CHARACTER);
        sb.append(uuidMsb_);
        sb.append(TAB_CHARACTER);
        sb.append(time_);
        sb.append(LINE_TERMINATOR);
        return sb.toString();
    }

}
