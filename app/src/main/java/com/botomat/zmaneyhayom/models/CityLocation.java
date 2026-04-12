package com.botomat.zmaneyhayom.models;

public class CityLocation {
    private String key;
    private String name;
    private double latitude;
    private double longitude;
    private double elevation;
    private String timeZone;

    public CityLocation(String key, String name, double latitude, double longitude,
                        double elevation, String timeZone) {
        this.key = key;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.timeZone = timeZone;
    }

    public String getKey() { return key; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getElevation() { return elevation; }
    public String getTimeZone() { return timeZone; }

    public static CityLocation[] getIsraeliCities() {
        return new CityLocation[]{
            new CityLocation("jerusalem", "ירושלים", 31.7683, 35.2137, 800, "Asia/Jerusalem"),
            new CityLocation("tel_aviv", "תל אביב", 32.0853, 34.7818, 5, "Asia/Jerusalem"),
            new CityLocation("haifa", "חיפה", 32.7940, 34.9896, 250, "Asia/Jerusalem"),
            new CityLocation("beer_sheva", "באר שבע", 31.2530, 34.7915, 280, "Asia/Jerusalem")
        };
    }

    public static CityLocation findByKey(String key) {
        for (CityLocation city : getIsraeliCities()) {
            if (city.getKey().equals(key)) return city;
        }
        return getIsraeliCities()[0];
    }
}
