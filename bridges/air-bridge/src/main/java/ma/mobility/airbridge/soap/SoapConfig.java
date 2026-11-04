package ma.mobility.airbridge.soap;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.transport.http.HttpUrlConnectionMessageSender;

@Configuration
public class SoapConfig {

    @Bean
    WebServiceTemplate airTemplate(@Value("${air.url}") String url,
                                   @Value("${air.connect-timeout-ms}") long connectTimeout,
                                   @Value("${air.read-timeout-ms}") long readTimeout) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("ma.mobility.airbridge.soap.gen");

        HttpUrlConnectionMessageSender sender = new HttpUrlConnectionMessageSender();
        sender.setConnectionTimeout(Duration.ofMillis(connectTimeout));
        sender.setReadTimeout(Duration.ofMillis(readTimeout));

        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri(url);
        template.setMessageSender(sender);
        return template;
    }
}
