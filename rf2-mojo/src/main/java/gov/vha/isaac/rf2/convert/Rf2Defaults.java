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

import java.util.UUID;
import org.ihtsdo.otf.tcc.api.metadata.binding.TermAux;
import gov.vha.isaac.metadata.source.IsaacMetadataAuxiliaryBinding;

/**
 *
 * @author marc
 */
public class Rf2Defaults {

    // Default value from Terminology Auxiliary concept --> user
    private static final String authorUuidStr = IsaacMetadataAuxiliaryBinding.USER.getPrimodialUuid().toString();

    private static final String pathSnomedCoreUuidStr = IsaacMetadataAuxiliaryBinding.DEVELOPMENT.getPrimodialUuid().toString();

    private static UUID authorUuid = null;
    private static UUID pathSnomedCoreUuid = null;

    public static UUID getAuthorUuid() {
        if (authorUuid == null) {
            authorUuid = UUID.fromString(authorUuidStr);
        }
        return authorUuid;
    }

    public static String getAuthorUuidStr() {
        return authorUuidStr;
    }

    public static UUID getPathSnomedCoreUuid() {
        if (pathSnomedCoreUuid == null) {
            pathSnomedCoreUuid = UUID.fromString(pathSnomedCoreUuidStr);
        }
        return pathSnomedCoreUuid;
    }

    public static String getPathSnomedCoreUuidStr() {
        return pathSnomedCoreUuidStr;
    }
}
