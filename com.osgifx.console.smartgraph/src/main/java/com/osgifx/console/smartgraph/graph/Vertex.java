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
package com.osgifx.console.smartgraph.graph;

/**
 * A vertex contains an element of type <code>V</code> and is used both in
 * graphs and digraphs.
 *
 * @param <V> Type of value stored in the vertex.
 *
 * @see Graph
 * @see Digraph
 */
public interface Vertex<V> {

    /**
     * Returns the element stored in the vertex.
     *
     * @return stored element
     */
    V element();
}
