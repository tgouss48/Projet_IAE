package ma.mobility.tools.rail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import mobility.rail.NotAvailable;
import mobility.rail.RailBooking;
import mobility.rail.RailBookingHelper;
import mobility.rail.Trip;
import org.omg.CORBA.ORB;

// Client de test pour le serveur CORBA ferroviaire
public class RailTestClient {

    public static void main(String[] args) throws Exception {
        String iorFile = System.getenv().getOrDefault("IOR_FILE", "ior/RailBooking.ior");

        // necessaire en Java 17 (voir NoRmiStubDelegate)
        System.setProperty("javax.rmi.CORBA.StubClass", NoRmiStubDelegate.class.getName());

        // on force l'ORB JacORB
        Properties props = new Properties();
        props.put("org.omg.CORBA.ORBClass", "org.jacorb.orb.ORB");
        props.put("org.omg.CORBA.ORBSingletonClass", "org.jacorb.orb.ORBSingleton");
        ORB orb = ORB.init(args, props);

        try {
            // reference de l'objet distant publiee par le serveur
            String ior = Files.readString(Path.of(iorFile)).trim();
            RailBooking rail = RailBookingHelper.narrow(orb.string_to_object(ior));
            System.out.println("[OK] reference lue depuis " + iorFile);

            Trip[] trips = rail.search("Casablanca", "Rabat", "2026-11-15");
            System.out.println("[OK] search : " + trips.length + " trajet(s)");
            for (Trip t : trips) {
                System.out.printf("     %s %s -> %s %s %d %d%n",
                        t.tripId, t.origin, t.destination, t.departure, t.priceMinor, t.seatsAvailable);
            }
            if (trips.length == 0) {
                throw new IllegalStateException("aucun trajet");
            }
            String tripId = trips[0].tripId;

            // cycle nominal puis annulation (cancel doit etre idempotent)
            String holdId = rail.hold(tripId, 2);
            System.out.println("[OK] hold : " + holdId);
            String bookingRef = rail.confirm(holdId);
            System.out.println("[OK] confirm : " + bookingRef);
            rail.cancel(bookingRef);
            rail.cancel(bookingRef);
            System.out.println("[OK] cancel x2 (idempotent)");

            // liberation d'un hold non confirme
            String holdId2 = rail.hold(tripId, 1);
            rail.cancel(holdId2);
            System.out.println("[OK] hold + cancel(holdId)");

            // cas d'erreur attendus : NotAvailable
            try {
                rail.hold(tripId, 10000);
                System.out.println("[KO] hold 10000 places accepte");
            } catch (NotAvailable e) {
                System.out.println("[OK] hold 10000 places refuse : " + e.reason);
            }

            try {
                rail.confirm(holdId2);
                System.out.println("[KO] confirm d'un hold annule accepte");
            } catch (NotAvailable e) {
                System.out.println("[OK] confirm hold annule refuse : " + e.reason);
            }
        } finally {
            orb.shutdown(false);
            orb.destroy();
        }
    }
}
