package com.gpis.marketplace_link.services;

import com.gpis.marketplace_link.valueObjects.LocationInfo;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Service
public class GeocodingService {

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse";

    private final RestTemplate restTemplate;

    public GeocodingService(RestTemplateBuilder builder){
        this.restTemplate = builder.build();
    }

    public Optional<LocationInfo> reverseGeocode(double lat, double lon) {
        try {
            String url = String.format("%s?lat=%f&lon=%f&format=json", NOMINATIM_URL, lat, lon);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("address")) {
                Map<String, String> address = (Map<String, String>) response.get("address");
                //return Optional.of(new LocationInfo(
                //        address.get("city") != null ? address.get("city") : address.get("town"),
                //        address.get("country")
                //));
            }
        } catch (Exception e) {
                // TODO
        }
        return Optional.empty();
    }

}
