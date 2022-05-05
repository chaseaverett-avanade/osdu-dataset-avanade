package org.opengroup.osdu.dataset.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.model.http.DpsHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;


import javax.servlet.ServletException;
import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class DatasetFilterTest {

    @Mock
    private DpsHeaders dpsHeaders;

    @InjectMocks
    private DatasetFilter datasetFilter;

    private static final String FOR_HEADER_NAME = "frame-of-reference";

    @Test
    public void doFilter()
            throws IOException, ServletException {

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(FOR_HEADER_NAME,"dummy-value");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        datasetFilter.doFilter(request,response,filterChain);

    }
}
