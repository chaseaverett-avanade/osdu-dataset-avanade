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

package org.opengroup.osdu.delivery.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.net.URI;
import java.net.URL;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class SignedUrl implements IDeliveryData {

  @JsonProperty("uri")
  URI uri;
  
  @JsonProperty("url")
  URL url;

  @JsonProperty("createdAt")
  Instant createdAt;

  @JsonProperty("connectionString")
  String connectionString;
}
