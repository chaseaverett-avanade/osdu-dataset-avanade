package org.opengroup.osdu.delivery.provider.aws.service;

import java.time.Instant;

public class InstantHelper {
    public Instant getCurrentInstant() {
        return Instant.now();
    }
}
