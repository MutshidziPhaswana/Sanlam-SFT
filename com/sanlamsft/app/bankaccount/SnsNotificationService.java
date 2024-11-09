import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class SnsNotificationService implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(SnsNotificationService.class);

    private SnsClient snsClient;

    @Value("${aws.region}")
    private String region;

    @Value("${sns.topic.arn}")
    private String snsTopicArn;

    @PostConstruct
    public void initializeSnsClient() {
        this.snsClient = SnsClient.builder()
                                   .region(Region.of(region))
                                   .build();
    }

    @Override
    public void sendNotification(WithdrawalEvent event) {
        int attempt = 0;

        while (attempt < 3) {
            try {
                PublishRequest publishRequest = PublishRequest.builder()
                        .message(event.toJson())
                        .topicArn(snsTopicArn)
                        .build();

                PublishResponse publishResponse = snsClient.publish(publishRequest);
                logger.info("Message published successfully to SNS topic: {} with message ID: {}", snsTopicArn, publishResponse.messageId());
                return;
            } catch (Exception e) {
                attempt++;
                logger.error("Attempt {} failed to publish message to SNS topic: {}. Error: {}", attempt, snsTopicArn, e.getMessage());
                if (attempt >= 3) {
                    throw new RuntimeException("Failed to publish message after 3 attempts", e);
                }

                long delay = 500 * (long) Math.pow(2, attempt - 1);
                logger.info("Retrying in {} ms...", delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted during retry delay", interruptedException);
                }
            }
        }

        throw new RuntimeException("Failed to publish message after max retries");
    }

    @PreDestroy
    public void close() {
        if (snsClient != null) {
            snsClient.close();
        }
    }
}
