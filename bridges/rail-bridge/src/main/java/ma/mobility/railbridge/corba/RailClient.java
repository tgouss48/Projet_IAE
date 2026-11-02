package ma.mobility.railbridge.corba;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ma.mobility.railbridge.api.OfferNotAvailableException;
import ma.mobility.railbridge.api.ProviderUnavailableException;
import mobility.rail.NotAvailable;
import mobility.rail.RailBooking;
import mobility.rail.RailBookingHelper;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.ORB;
import org.omg.CORBA.SystemException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// Acces a l'objet RailBooking distant, traduit les erreurs CORBA en erreurs du bridge
@Component
public class RailClient {

    private static final Logger log = LoggerFactory.getLogger(RailClient.class);

    @FunctionalInterface
    public interface RailCall<T> {
        T apply(RailBooking rail) throws NotAvailable;
    }

    private final ORB orb;
    private final Path iorFile;
    private volatile RailBooking rail;

    public RailClient(ORB orb, @Value("${rail.ior-file}") String iorFile) {
        this.orb = orb;
        this.iorFile = Path.of(iorFile);
    }

    public <T> T call(RailCall<T> op) {
        try {
            return op.apply(reference());
        } catch (NotAvailable e) {
            throw new OfferNotAvailableException(e.reason);
        } catch (SystemException e) {
            // l'IOR change quand le serveur redemarre : on relit le fichier et on rejoue,
            // seulement si l'appel n'a pas ete execute cote serveur
            rail = null;
            if (e.completed != CompletionStatus.COMPLETED_NO) {
                throw unavailable(e);
            }
            log.warn("appel CORBA non execute ({}), nouvelle tentative avec l'IOR relu", e.getClass().getSimpleName());
            try {
                return op.apply(reference());
            } catch (NotAvailable e2) {
                throw new OfferNotAvailableException(e2.reason);
            } catch (SystemException e2) {
                rail = null;
                throw unavailable(e2);
            }
        }
    }

    private synchronized RailBooking reference() {
        if (rail == null) {
            String ior;
            try {
                ior = Files.readString(iorFile).trim();
            } catch (IOException e) {
                throw new ProviderUnavailableException("IOR illisible : " + iorFile);
            }
            rail = RailBookingHelper.narrow(orb.string_to_object(ior));
        }
        return rail;
    }

    private static ProviderUnavailableException unavailable(SystemException e) {
        return new ProviderUnavailableException("operateur ferroviaire injoignable : " + e.getClass().getSimpleName());
    }
}
