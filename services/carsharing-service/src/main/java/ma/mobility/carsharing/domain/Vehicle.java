package ma.mobility.carsharing.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Vehicle {

    @Id
    private String id;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String model;

    @Column(name = "daily_price_minor", nullable = false)
    private long dailyPriceMinor;

    protected Vehicle() {
    }

    public String getId() {
        return id;
    }

    public String getCity() {
        return city;
    }

    public String getModel() {
        return model;
    }

    public long getDailyPriceMinor() {
        return dailyPriceMinor;
    }
}
