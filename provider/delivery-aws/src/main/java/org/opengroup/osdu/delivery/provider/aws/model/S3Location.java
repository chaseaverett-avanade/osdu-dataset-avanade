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

package org.opengroup.osdu.delivery.provider.aws.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

public class S3Location {

  @Getter
  @Setter(AccessLevel.PRIVATE)
  public String bucket;

  @Getter
  @Setter(AccessLevel.PRIVATE)
  public String key;

  @Getter
  @Setter(AccessLevel.PRIVATE)
  public boolean isValid = false;

  private static final String UNSIGNED_URL_PREFIX = "s3://";

  public S3Location(String uri) {
    if (uri != null && uri.startsWith(UNSIGNED_URL_PREFIX)) {
      String[] bucketAndKey = uri.substring(UNSIGNED_URL_PREFIX.length()).split("/", 2);
      if (bucketAndKey.length == 2) {
        bucket = bucketAndKey[0];
        key = bucketAndKey[1];
        isValid = true;
      }
    }
  }
}
