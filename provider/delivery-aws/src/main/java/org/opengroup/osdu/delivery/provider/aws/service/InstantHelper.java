// Copyright © Amazon Web Services
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.opengroup.osdu.delivery.provider.aws.service;

import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * A non static implementation of the Instant.now() method that's unit testable
 */
@Component
public class InstantHelper {

    @Deprecated
    public Instant getCurrentInstant() {
        return Instant.now();
    }

    /**
     * Returns Instant.now()
     * @return Instant
     */
    public Instant now() {
        return Instant.now();
    }
}
