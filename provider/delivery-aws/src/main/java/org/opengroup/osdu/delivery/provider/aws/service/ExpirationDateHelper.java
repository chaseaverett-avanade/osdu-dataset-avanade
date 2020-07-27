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

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

/**
 * Responsible for calculating expiration dates
 */
@Component
public class ExpirationDateHelper {

  @Deprecated
  public Date getExpirationDate(int s3SignedUrlExpirationTimeInDays){
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60 * 24 * s3SignedUrlExpirationTimeInDays;
    expiration.setTime(expTimeMillis);
    return expiration;
  }

  /**
   * Adds the timespan to the Local date and returns a Date object of that time
   * @param date - the start date
   * @param timeSpan - a length of time to calculate the future date
   * @return
   */
  public Date getExpiration(Instant date, Duration timeSpan) {
    Instant expiration = date.plus(timeSpan);
    return Date.from(expiration
            .atZone(ZoneId.systemDefault())
            .toInstant());
  }
}
