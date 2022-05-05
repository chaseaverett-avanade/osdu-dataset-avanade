package org.opengroup.osdu.dataset.util;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.http.HttpResponse;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.dataset.dms.DmsException;

@RunWith(MockitoJUnitRunner.class)
public class ExceptionUtilTest {

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testHandleDmsExceptionCatchBlock() {

        DmsException e = new DmsException("message", new HttpResponse());
        exceptionRule.expect(AppException.class);
        exceptionRule.expectMessage("Failed to parse error from DMS Service");
        ExceptionUtils.handleDmsException(e);
    }

    @Test
    public void testHandleDmsExceptionTryBlock() {

        exceptionRule.expect(AppException.class);
        exceptionRule.expectMessage("dummy");
        HttpResponse httpResponse = new HttpResponse();
        httpResponse.setBody("{\n" + "\t\"code\": \"200\",\n" + "\t\"reason\": \"dummy\",\n" + "\t\"message\": \"dummy\"\n" + "}");
        DmsException e = new DmsException("message", httpResponse);
        ExceptionUtils.handleDmsException(e);
    }
}
