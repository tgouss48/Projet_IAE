package ma.mobility.booking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ma.mobility.booking.provider.OfferNotAvailableException;
import ma.mobility.booking.provider.ProviderClient;
import ma.mobility.booking.provider.ProviderUnavailableException;

// Faux operateurs : enregistrent les appels et echouent a la demande
class FakeProviders implements ProviderClient {

    final List<String> calls = new ArrayList<>();
    final Map<String, RuntimeException> holdFailures = new HashMap<>();
    final Map<String, RuntimeException> confirmFailures = new HashMap<>();
    final Map<String, Integer> cancelOutages = new HashMap<>();

    void reset() {
        calls.clear();
        holdFailures.clear();
        confirmFailures.clear();
        cancelOutages.clear();
    }

    void refuseHold(String mode) {
        holdFailures.put(mode, new OfferNotAvailableException("Plus assez de places"));
    }

    void refuseConfirm(String mode) {
        confirmFailures.put(mode, new OfferNotAvailableException("Maintien expire"));
    }

    // les n prochains cancel sur ce mode echouent (operateur injoignable)
    void cancelDown(String mode, int times) {
        cancelOutages.put(mode, times);
    }

    @Override
    public String hold(String mode, String offerId, int quantity) {
        calls.add("hold " + mode);
        if (holdFailures.containsKey(mode)) {
            throw holdFailures.get(mode);
        }
        return "HOLD-" + mode;
    }

    @Override
    public String confirm(String mode, String holdId) {
        calls.add("confirm " + mode);
        if (confirmFailures.containsKey(mode)) {
            throw confirmFailures.get(mode);
        }
        return "BK-" + mode;
    }

    @Override
    public void cancel(String mode, String reference) {
        calls.add("cancel " + mode + " " + reference);
        int down = cancelOutages.getOrDefault(mode, 0);
        if (down > 0) {
            cancelOutages.put(mode, down - 1);
            throw new ProviderUnavailableException(mode + " injoignable");
        }
    }
}
