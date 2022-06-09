package org.opengroup.osdu.dataset.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opengroup.osdu.core.common.logging.audit.AuditAction;
import org.opengroup.osdu.core.common.logging.audit.AuditPayload;
import org.opengroup.osdu.core.common.logging.audit.AuditStatus;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;


@RunWith(MockitoJUnitRunner.class)
public class AuditEventsTest {

    private static final String READ_STORAGE_INSTRUCTIONS_ACTION_ID = "DS001";
    private static final String READ_STORAGE_INSTRUCTIONS_MESSAGE = "Read storage instructions";

    private static final String READ_RETRIEVAL_INSTRUCTIONS_ACTION_ID = "DS002";
    private static final String READ_RETRIEVAL_INSTRUCTIONS_MESSAGE = "Read retrieval instructions";

    private static final String REGISTER_DATASET_ACTION_ID = "DS003";
    private static final String REGISTER_DATASET_MESSAGE = "Registered dataset";

    private static final String READ_DATASET_REGISTRIES_ACTION_ID = "DS004";
    private static final String READ_DATASET_REGISTRIES_MESSAGE = "Read dataset registries";

    private String user = "dummyUser";

    AuditEvents auditEvents;

    @Before
    public void setup() {

        auditEvents = new AuditEvents("dummyUser");
    }

    @Test
    public void testGetReadStorageInstructionsEvent() throws Exception {
        List<String> resourceList = new ArrayList();
        resourceList.add("dummyValue");

        AuditStatus status = AuditStatus.SUCCESS;

        AuditPayload expectedPayload = AuditPayload.builder().action(AuditAction.READ).status(status).user(this.user).
                actionId(READ_STORAGE_INSTRUCTIONS_ACTION_ID).message(READ_STORAGE_INSTRUCTIONS_MESSAGE + " - " + status.name().toLowerCase()).
                resources(new ArrayList<>()).build();

        AuditPayload actualAuditPayload = auditEvents.getReadStorageInstructionsEvent(status, resourceList);

        assertEquals(actualAuditPayload.get("message"), expectedPayload.get("message"));

    }

}
