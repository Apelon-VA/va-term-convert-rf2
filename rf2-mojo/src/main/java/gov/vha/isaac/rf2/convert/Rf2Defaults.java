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

/**
 *
 * @author marc
 */
public class Rf2Defaults {

    // Default value from Terminology Auxiliary concept --> user
    private static final String authorUuidStr = "f7495b58-6630-3499-a44e-2052b5fcf06c";
    // Default value from TkRevision.unspecifiedModuleUuid
    private static final String moduleUuidStr = "40d1c869-b509-32f8-b735-836eac577a67";

    private static final String pathSnomedCoreUuidStr = "8c230474-9f11-30ce-9cad-185a96fd03a2";

    private static UUID authorUuid = null;
    private static UUID moduleUuid = null;
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

    public static UUID getModuleUuid() {
        if (moduleUuid == null) {
            moduleUuid = UUID.fromString(moduleUuidStr);
        }
        return moduleUuid;
    }

    public static String getModuleUuidStr() {
        return moduleUuidStr;
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
