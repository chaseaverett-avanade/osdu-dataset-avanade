/*
 * Copyright 2020 Microsoft Corporation
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

package org.opengroup.osdu.delivery.provider.azure.service;

import java.util.Date;

// Needed to add this class to make it easy to unit test the creation of a date
// to the current time of running
public class ExpirationDateHelper {
  public Date getExpirationDate(int azureSignedUrlExpirationTimeInDays){
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60 * 24 * azureSignedUrlExpirationTimeInDays;
    expiration.setTime(expTimeMillis);
    return expiration;
  }
}
