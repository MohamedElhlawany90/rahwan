package com.blueWhale.Rahwan.location;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Transactional
public class LocationService {

    private final LocationRepository locationRepository;

    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location updateLocation(Location location, double lat, double lng) {

        location.setLatitude(lat);
        location.setLongitude(lng);

        return locationRepository.save(location);
    }
}

