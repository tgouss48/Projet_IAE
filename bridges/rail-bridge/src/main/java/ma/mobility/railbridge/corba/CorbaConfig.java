package ma.mobility.railbridge.corba;

import java.util.Properties;

import org.omg.CORBA.ORB;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CorbaConfig {

    @Bean(destroyMethod = "destroy")
    ORB orb(@Value("${rail.connect-timeout-ms}") int connectTimeout,
            @Value("${rail.reply-timeout-ms}") int replyTimeout) {
        System.setProperty("javax.rmi.CORBA.StubClass", NoRmiStubDelegate.class.getName());

        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
        // sans ces timeouts un serveur bloque bloquerait aussi le bridge
        props.put("jacorb.connection.client.connect_timeout", String.valueOf(connectTimeout));
        props.put("jacorb.connection.client.pending_reply_timeout", String.valueOf(replyTimeout));
        // par defaut JacORB retente 5 fois la connexion (~40 s si le serveur est arrete)
        props.put("jacorb.retries", "0");
        return ORB.init(new String[0], props);
    }
}
