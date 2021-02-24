// Copyright © 2021 Amazon Web Services
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

package org.opengroup.osdu.dataset.model.validation;

public class DmsValidationDoc {
    
        
    private DmsValidationDoc() {
        // private constructor
    }

    public static final String RESOURCE_TYPE_NOT_REGISTERED_ERROR = "No DMS handler for resource type '%s' is registered";
    public static final String KIND_SUB_TYPE_NOT_REGISTERED_ERROR = "No DMS handler for kindSubType '%s' is registered";
    public static final String DMS_STORAGE_NOT_SUPPORTED_ERROR = "The requested DMS (%s) does not support storage operations";
}
