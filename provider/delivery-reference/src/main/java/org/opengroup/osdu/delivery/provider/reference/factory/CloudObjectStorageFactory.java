package org.opengroup.osdu.delivery.provider.reference.factory;

import io.minio.MinioClient;
import io.minio.errors.InvalidEndpointException;
import io.minio.errors.InvalidPortException;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class CloudObjectStorageFactory {
  private static final Logger logger = LoggerFactory.getLogger(CloudObjectStorageFactory.class);
  @Value("${minio.endpoint_url}")
  private String endpointURL;
  @Value("${minio.access_key}")
  private String accessKey;
  @Value("${minio.secret_key}")
  private String secretKey;
  @Value("${minio.region:us-east-1}")
  private String region;
  @Value("${minio.prefix:local-dev}")
  private String bucketNamePrefix;
  private MinioClient minioClient;
  private String bucketName;

  public CloudObjectStorageFactory() { }

  @PostConstruct
  public void init() throws InvalidEndpointException, InvalidPortException {
    this.minioClient = new MinioClient(this.endpointURL, this.accessKey, this.secretKey, this.region);
    logger.info("Minio client initialized");
  }

  public MinioClient getClient() {
    return this.minioClient;
  }
}
