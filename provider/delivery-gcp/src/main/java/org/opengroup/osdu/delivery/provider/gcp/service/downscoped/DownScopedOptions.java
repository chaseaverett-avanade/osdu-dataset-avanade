/*
 * Copyright 2020 Google LLC
 * Copyright 2020 EPAM Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opengroup.osdu.delivery.provider.gcp.service.downscoped;

import lombok.Getter;
import lombok.ToString;
import lombok.extern.java.Log;

import java.io.Serializable;
import java.util.List;

@Log
@Getter
@ToString
public class DownScopedOptions implements Serializable {
    private static final long serialVersionUID = 7827449965185981719L;
    private final AccessBoundary accessBoundary;

    public DownScopedOptions(List<AccessBoundaryRule> accessBoundaryRules){
        this.accessBoundary = new AccessBoundary(accessBoundaryRules);
    }

}
