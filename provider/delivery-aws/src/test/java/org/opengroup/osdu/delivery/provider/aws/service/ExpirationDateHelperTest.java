package org.opengroup.osdu.delivery.provider.aws.service;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.util.Date;

@RunWith(MockitoJUnitRunner.class)
public class ExpirationDateHelperTest {

  @Test
  public void should_offset_time_by_duration() throws ParseException {
    ExpirationDateHelper CUT = new ExpirationDateHelper();
    long offSetInDays = 1;
    DateFormat dt = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    Date start = dt.parse("01/01/2020 00:00:00");
    Duration span = Duration.ofDays(offSetInDays);

    LocalDateTime newDateTime =  start.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

    Date actual = CUT.getExpiration(start.toInstant(), span);
    Date expected = dt.parse("02/01/2020 00:00:00");

    Assert.assertEquals(expected, actual);
  }

}
