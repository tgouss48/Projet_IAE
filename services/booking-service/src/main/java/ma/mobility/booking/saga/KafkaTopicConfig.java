package ma.mobility.booking.saga;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // 3 partitions, cle = bookingId : les evenements d'un meme dossier restent ordonnes
    @Bean
    NewTopic bookingEventsTopic(@Value("${booking.topic}") String topic) {
        return TopicBuilder.name(topic).partitions(3).replicas(1).build();
    }
}
