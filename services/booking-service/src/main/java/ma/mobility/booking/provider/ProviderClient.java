package ma.mobility.booking.provider;

// Interface commune aux trois operateurs (bridges CORBA/SOAP et autopartage)
public interface ProviderClient {

    String hold(String mode, String offerId, int quantity);

    String confirm(String mode, String holdId);

    void cancel(String mode, String reference);
}
