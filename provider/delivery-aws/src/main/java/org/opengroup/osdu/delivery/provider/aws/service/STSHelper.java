package org.opengroup.osdu.delivery.provider.aws.service;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.S3Actions;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest;
import com.amazonaws.services.securitytoken.model.AssumeRoleResult;
import com.amazonaws.services.securitytoken.model.Credentials;
import org.opengroup.osdu.core.aws.sts.STSConfig;
import org.opengroup.osdu.delivery.provider.aws.AwsServiceConfig;
import org.opengroup.osdu.delivery.provider.aws.model.S3Location;
import org.opengroup.osdu.delivery.provider.aws.model.TemporaryCredentials;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.time.Instant;
import java.util.Date;

@Component
public class STSHelper {

  // role to role chaining limits credential duration to a max of 1 hr
  // https://docs.aws.amazon.com/IAM/latest/UserGuide/id_roles_terms-and-concepts.html#iam-term-role-chaining
  private static final Integer MAX_DURATION_IN_SECONDS = 3600;

  @Inject
  private AwsServiceConfig awsServiceConfig;

  @Inject
  private InstantHelper instantHelper;

  private AWSSecurityTokenService sts;

  @PostConstruct
  public void init() {
    STSConfig config = new STSConfig(awsServiceConfig.amazonRegion);
    sts = config.amazonSTS();
  }

  public TemporaryCredentials getCredentials(String srn, S3Location fileLocation,
                                             String roleArn, String user, Date expiration) {

    Instant now = instantHelper.now();
    String roleSessionName = String.format("%s_%s", user, now.toEpochMilli());
    Policy policy = createPolicy(srn, fileLocation);

    Long duration = ((expiration.getTime() - now.toEpochMilli()) / 1000);
    duration = duration > MAX_DURATION_IN_SECONDS ? MAX_DURATION_IN_SECONDS : duration;

    AssumeRoleRequest roleRequest = new AssumeRoleRequest()
            .withRoleArn(roleArn)
            .withRoleSessionName(roleSessionName)
            .withDurationSeconds(duration.intValue())
            .withPolicy(policy.toJson());

    AssumeRoleResult response = sts.assumeRole(roleRequest);

    Credentials sessionCredentials = response.getCredentials();

    TemporaryCredentials temporaryCredentials = TemporaryCredentials
            .builder()
            .accessKeyId(sessionCredentials.getAccessKeyId())
            .expiration(sessionCredentials.getExpiration())
            .secretAccessKey(sessionCredentials.getSecretAccessKey())
            .sessionToken(sessionCredentials.getSessionToken())
            .build();

    return temporaryCredentials;
  }

  private Policy createPolicy(String srn, S3Location fileLocation) {
    Policy policy = new Policy();
    Statement statement = new Statement(Statement.Effect.Allow);
    String resource;

    if (srn.toLowerCase().contains("ovds")) {
      // map to bucket
      resource = String.format("arn:aws:s3:::%s/%s/*", fileLocation.bucket, fileLocation.key);
    } else {
      resource = String.format("arn:aws:s3:::%s/%s", fileLocation.bucket, fileLocation.key);
    }

    statement.withActions(S3Actions.GetObject).withResources(new Resource(resource));
    policy.withStatements(statement);

    return policy;
  }
}
