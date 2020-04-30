package org.opengroup.osdu.delivery.provider.aws.service;

import java.util.Date;

// Needed to add this class to make it easy to unit test the creation of a date
// to the current time of running
public class ExpirationDateHelper {
  public Date getExpirationDate(int s3SignedUrlExpirationTimeInDays){
    Date expiration = new Date();
    long expTimeMillis = expiration.getTime();
    expTimeMillis += 1000 * 60 * 60 * 24 * s3SignedUrlExpirationTimeInDays;
    expiration.setTime(expTimeMillis);
    return expiration;
  }
}
