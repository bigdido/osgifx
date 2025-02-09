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

import org.osgi.dto.DTO;

public class XHttpComponentDTO extends DTO {

    public String       name;
    public boolean      asyncSupported;
    public long         serviceId;
    public List<String> patterns;
    public String       contextName;
    public String       contextPath;
    public long         contextServiceId;
    public String       type;

    // ERROR PAGE
    public List<String> exceptions;
    public List<Long>   errorCodes;

    // FILTER
    public List<String> servletNames;
    public List<String> regexs;
    public List<String> dispatcher;

    // LISTENER
    public List<String> types;

    // RESOURCE
    public String prefix;

    // SERVLET
    public String servletInfo;

}
