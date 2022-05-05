package org.opengroup.osdu.dataset.util;


import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import javassist.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.JaxRsDpsLog;
import org.opengroup.osdu.core.common.model.http.AppException;
import org.opengroup.osdu.dataset.model.validation.DmsValidationDoc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.context.request.WebRequest;

import javax.validation.ValidationException;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class GlobalExceptionMapperTest {

    @Mock
    private JaxRsDpsLog logger;

    @InjectMocks
    private GlobalExceptionMapper globalExceptionMapper;

    Class<?> c;

    @Before
    public void setup() throws NoSuchMethodException {
        c = globalExceptionMapper.getClass();
    }

    @Test
    public void handleAppException() throws Exception {
        Method m = c.getDeclaredMethod("handleAppException", AppException.class);
        m.setAccessible(true);
        AppException e = new AppException(HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                String.format(DmsValidationDoc.KIND_SUB_TYPE_NOT_REGISTERED_ERROR, "error"));
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper, e);
    }

    @Test
    public void handleValidationException() throws Exception {
        Method m = c.getDeclaredMethod("handleValidationException", ValidationException.class);
        m.setAccessible(true);
        ValidationException e = new ValidationException(HttpStatus.NOT_ACCEPTABLE.toString());
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper, e);
        assertEquals(response.getStatusCode(),HttpStatus.BAD_REQUEST);
    }

    @Test
    public void handleNotFoundException() throws Exception {
        Method m = c.getDeclaredMethod("handleNotFoundException", NotFoundException.class);
        m.setAccessible(true);
        NotFoundException e = new NotFoundException("Not Found");
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper, e);
        assertEquals(response.getStatusCode(),HttpStatus.NOT_FOUND);
    }

    @Test
    public void handleUnrecognizedPropertyException() throws Exception {
        Method m = c.getDeclaredMethod("handleUnrecognizedPropertyException", UnrecognizedPropertyException.class);
        m.setAccessible(true);
        JsonParser jsonParser = null;
        String message = "Property Not Exist";
        JsonLocation jsonLocation = null;
        UnrecognizedPropertyException e = new UnrecognizedPropertyException(jsonParser, message, jsonLocation, null,
                "Prop Name", null);
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper, e);
        assertEquals(response.getStatusCode(),HttpStatus.BAD_REQUEST);
    }

    @Test
    public void handleAccessDeniedException() throws Exception {
        Method m = c.getDeclaredMethod("handleAccessDeniedException", AccessDeniedException.class);
        m.setAccessible(true);
        AccessDeniedException e = new AccessDeniedException("Access Denied");
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper, e);
        assertEquals(response.getStatusCode(),HttpStatus.FORBIDDEN);

    }

    @Test
    public void testHandleHttpRequestMethodNotSupported() throws Exception {

        Method m = c.getDeclaredMethod("handleHttpRequestMethodNotSupported",
                HttpRequestMethodNotSupportedException.class,HttpHeaders.class,HttpStatus.class,WebRequest.class);
        m.setAccessible(true);
        HttpRequestMethodNotSupportedException e = new HttpRequestMethodNotSupportedException("Method Not Supported");
        HttpHeaders headers = new HttpHeaders();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        WebRequest request = mock(WebRequest.class);
        ResponseEntity<Object> response = (ResponseEntity<Object>) m.invoke(globalExceptionMapper,e,headers,status, request);
        assertEquals(response.getStatusCode().value(),org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED);
    }

}
