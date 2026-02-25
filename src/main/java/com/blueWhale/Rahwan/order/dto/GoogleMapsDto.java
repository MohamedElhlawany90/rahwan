package com.blueWhale.Rahwan.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

/**
 * DTOs for Google Maps Distance Matrix API Response
 */
public class GoogleMapsDto {

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DistanceMatrixResponse {
        @JsonProperty("destination_addresses")
        private List<String> destinationAddresses;

        @JsonProperty("origin_addresses")
        private List<String> originAddresses;

        private List<Row> rows;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Row {
        private List<Element> elements;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Element {
        private Distance distance;
        private Duration duration;
        private String status;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Distance {
        private String text;  // "10.5 km"
        private int value;    // 10500 (meters)
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Duration {
        private String text;  // "15 mins"
        private int value;    // 900 (seconds)
    }
}