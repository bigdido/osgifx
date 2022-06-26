/*******************************************************************************
 * Copyright 2021-2022 Amit Kumar Mondal
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
package com.osgifx.console.supervisor;

import java.util.Collection;
import java.util.Collections;

import com.osgifx.console.agent.dto.XEventDTO;

/**
 * This is used to receive remote OSGi event admin events
 *
 * @see Supervisor#addOSGiEventListener(EventListener)
 * @see Supervisor#removeOSGiEventListener(EventListener)
 */
@FunctionalInterface
public interface EventListener {

    /**
     * By default, all events from all the topics will be intercepted by this
     * listener. However, you can also specify which event you are interested to
     * intercept.
     *
     * @return the collection of event topics
     */
    default Collection<String> topics() {
        return Collections.singleton("*");
    }

    /**
     * The callback to be executed when the event will be received
     *
     * @param event the remote event
     */
    void onEvent(XEventDTO event);
}
