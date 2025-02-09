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
package com.osgifx.console.agent.admin;

import static com.osgifx.console.agent.helper.AgentHelper.serviceUnavailable;
import static com.osgifx.console.agent.helper.OSGiCompendiumService.HTTP_RUNTIME;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.osgi.service.http.runtime.HttpServiceRuntime;
import org.osgi.service.http.runtime.dto.ErrorPageDTO;
import org.osgi.service.http.runtime.dto.FilterDTO;
import org.osgi.service.http.runtime.dto.ListenerDTO;
import org.osgi.service.http.runtime.dto.ResourceDTO;
import org.osgi.service.http.runtime.dto.RuntimeDTO;
import org.osgi.service.http.runtime.dto.ServletContextDTO;
import org.osgi.service.http.runtime.dto.ServletDTO;

import com.j256.simplelogging.FluentLogger;
import com.j256.simplelogging.LoggerFactory;
import com.osgifx.console.agent.dto.XHttpComponentDTO;

import jakarta.inject.Inject;

public final class XHttpAdmin {

    private final HttpServiceRuntime httpServiceRuntime;
    private final FluentLogger       logger = LoggerFactory.getFluentLogger(getClass());

    @Inject
    public XHttpAdmin(final Object httpServiceRuntime) {
        this.httpServiceRuntime = (HttpServiceRuntime) httpServiceRuntime;
    }

    public List<XHttpComponentDTO> runtime() {
        if (httpServiceRuntime == null) {
            logger.atWarn().msg(serviceUnavailable(HTTP_RUNTIME)).log();
            return Collections.emptyList();
        }
        try {
            final RuntimeDTO runtime = httpServiceRuntime.getRuntimeDTO();
            return initHttpComponents(runtime);
        } catch (final Exception e) {
            logger.atError().msg("Error occurred while retrieving HTTP components").throwable(e).log();
            return Collections.emptyList();
        }
    }

    private List<XHttpComponentDTO> initHttpComponents(final RuntimeDTO runtime) {
        final List<XHttpComponentDTO> dtos = new ArrayList<>(initServlets(runtime));

        dtos.addAll(initFilters(runtime));
        dtos.addAll(initResources(runtime));
        dtos.addAll(initListeners(runtime));
        dtos.addAll(initErrorPages(runtime));

        return dtos;
    }

    private List<XHttpComponentDTO> initServlets(final RuntimeDTO runtime) {
        // @formatter:off
        return Stream.of(runtime.servletContextDTOs)
                     .map(this::initServletsByContext)
                     .flatMap(List::stream)
                     .collect(toList());
        // @formatter:on
    }

    private List<XHttpComponentDTO> initServletsByContext(final ServletContextDTO servletContextDTO) {
        final List<XHttpComponentDTO> servletDTO = new ArrayList<>();

        for (final ServletDTO sDTO : servletContextDTO.servletDTOs) {
            final XHttpComponentDTO dto = new XHttpComponentDTO();

            dto.contextName      = servletContextDTO.name;
            dto.contextPath      = servletContextDTO.contextPath;
            dto.contextServiceId = servletContextDTO.serviceId;
            dto.patterns         = Arrays.asList(sDTO.patterns);
            dto.name             = sDTO.name;
            dto.asyncSupported   = sDTO.asyncSupported;
            dto.serviceId        = sDTO.serviceId;
            dto.servletInfo      = sDTO.servletInfo;
            dto.type             = "Servlet";

            servletDTO.add(dto);
        }
        return servletDTO;
    }

    private List<XHttpComponentDTO> initFilters(final RuntimeDTO runtime) {
        // @formatter:off
        return Stream.of(runtime.servletContextDTOs)
                     .map(this::initFiltersByContext)
                     .flatMap(List::stream)
                     .collect(toList());
        // @formatter:on
    }

    private List<XHttpComponentDTO> initFiltersByContext(final ServletContextDTO servletContextDTO) {
        final List<XHttpComponentDTO> filterDTOs = new ArrayList<>();

        for (final FilterDTO fDTO : servletContextDTO.filterDTOs) {
            final XHttpComponentDTO dto = new XHttpComponentDTO();

            dto.contextName      = servletContextDTO.name;
            dto.contextPath      = servletContextDTO.contextPath;
            dto.contextServiceId = servletContextDTO.serviceId;
            dto.patterns         = Arrays.asList(fDTO.patterns);
            dto.name             = fDTO.name;
            dto.asyncSupported   = fDTO.asyncSupported;
            dto.serviceId        = fDTO.serviceId;
            dto.dispatcher       = Arrays.asList(fDTO.dispatcher);
            dto.regexs           = Arrays.asList(fDTO.regexs);
            dto.servletNames     = Arrays.asList(fDTO.servletNames);
            dto.type             = "Filter";

            filterDTOs.add(dto);
        }
        return filterDTOs;
    }

    private List<XHttpComponentDTO> initResources(final RuntimeDTO runtime) {
        // @formatter:off
        return Stream.of(runtime.servletContextDTOs)
                     .map(this::initResourcesByContext)
                     .flatMap(List::stream)
                     .collect(toList());
        // @formatter:on
    }

    private List<XHttpComponentDTO> initResourcesByContext(final ServletContextDTO servletContextDTO) {
        final List<XHttpComponentDTO> resourceDTO = new ArrayList<>();

        for (final ResourceDTO rDTO : servletContextDTO.resourceDTOs) {
            final XHttpComponentDTO dto = new XHttpComponentDTO();

            dto.contextName      = servletContextDTO.name;
            dto.contextPath      = servletContextDTO.contextPath;
            dto.contextServiceId = servletContextDTO.serviceId;
            dto.patterns         = Arrays.asList(rDTO.patterns);
            dto.prefix           = rDTO.prefix;
            dto.serviceId        = rDTO.serviceId;
            dto.type             = "Resource";

            resourceDTO.add(dto);
        }
        return resourceDTO;
    }

    private List<XHttpComponentDTO> initListeners(final RuntimeDTO runtime) {
        // @formatter:off
        return Stream.of(runtime.servletContextDTOs)
                     .map(this::initListenersByContext)
                     .flatMap(List::stream)
                     .collect(toList());
        // @formatter:on
    }

    private List<XHttpComponentDTO> initListenersByContext(final ServletContextDTO servletContextDTO) {
        final List<XHttpComponentDTO> listenerDTO = new ArrayList<>();

        for (final ListenerDTO lDTO : servletContextDTO.listenerDTOs) {
            final XHttpComponentDTO dto = new XHttpComponentDTO();

            dto.contextName      = servletContextDTO.name;
            dto.contextPath      = servletContextDTO.contextPath;
            dto.contextServiceId = servletContextDTO.serviceId;
            dto.types            = Arrays.asList(lDTO.types);
            dto.serviceId        = lDTO.serviceId;
            dto.type             = "Listener";

            listenerDTO.add(dto);
        }
        return listenerDTO;
    }

    private List<XHttpComponentDTO> initErrorPages(final RuntimeDTO runtime) {
        // @formatter:off
        return Stream.of(runtime.servletContextDTOs)
                     .map(this::initErrorPagesByContext)
                     .flatMap(List::stream)
                     .collect(toList());
        // @formatter:on
    }

    private List<XHttpComponentDTO> initErrorPagesByContext(final ServletContextDTO servletContextDTO) {
        final List<XHttpComponentDTO> errorPageDTO = new ArrayList<>();

        for (final ErrorPageDTO eDTO : servletContextDTO.errorPageDTOs) {
            final XHttpComponentDTO dto = new XHttpComponentDTO();

            dto.contextName      = servletContextDTO.name;
            dto.contextPath      = servletContextDTO.contextPath;
            dto.contextServiceId = servletContextDTO.serviceId;
            dto.name             = eDTO.name;
            dto.asyncSupported   = eDTO.asyncSupported;
            dto.serviceId        = eDTO.serviceId;
            dto.servletInfo      = eDTO.servletInfo;
            dto.exceptions       = Arrays.asList(eDTO.exceptions);
            dto.errorCodes       = Arrays.stream(eDTO.errorCodes).boxed().collect(toList());
            dto.type             = "Error Page";

            errorPageDTO.add(dto);
        }
        return errorPageDTO;
    }

}
