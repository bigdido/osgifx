/*******************************************************************************
 * Copyright 2021-2023 Amit Kumar Mondal
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 ******************************************************************************/
package com.osgifx.console.agent.dto;

import java.util.List;
import java.util.Map;

import org.osgi.dto.DTO;
import org.osgi.framework.wiring.dto.BundleRevisionDTO;

public class XBundleDTO extends DTO {

    public long                  id;
    public String                state;
    public String                location;
    public String                category;
    public int                   revisions;
    public boolean               isFragment;
    public long                  lastModified;
    public long                  dataFolderSize;
    public String                documentation;
    public String                vendor;
    public String                version;
    public String                description;
    public int                   startLevel;
    public int                   frameworkStartLevel;
    public String                symbolicName;
    public long                  startDurationInMillis;
    public BundleRevisionDTO     bundleRevision;
    public List<XPackageDTO>     exportedPackages;
    public List<XPackageDTO>     importedPackages;
    public List<XBundleInfoDTO>  wiredBundlesAsProvider;
    public List<XBundleInfoDTO>  wiredBundlesAsRequirer;
    public List<XServiceInfoDTO> registeredServices;
    public Map<String, String>   manifestHeaders;
    public List<XServiceInfoDTO> usedServices;
    public List<XBundleInfoDTO>  hostBundles;
    public List<XBundleInfoDTO>  fragmentsAttached;
    public boolean               isPersistentlyStarted;
    public boolean               isActivationPolicyUsed;

}
