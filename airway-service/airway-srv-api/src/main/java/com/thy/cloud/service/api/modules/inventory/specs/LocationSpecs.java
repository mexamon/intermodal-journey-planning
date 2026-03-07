package com.thy.cloud.service.api.modules.inventory.specs;

import com.thy.cloud.service.api.modules.inventory.model.LocationSearchRequest;
import com.thy.cloud.service.dao.entity.inventory.Location;
import com.thy.cloud.service.dao.entity.inventory.Location_;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.ObjectUtils;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class LocationSpecs {

    private LocationSpecs() {
    }

    public static Specification<Location> filter(LocationSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (!ObjectUtils.isEmpty(request.getName())) {
                predicates
                        .add(cb.like(cb.lower(root.get(Location_.name)), "%" + request.getName().toLowerCase() + "%"));
            }

            if (!ObjectUtils.isEmpty(request.getIataCode())) {
                predicates.add(cb.equal(root.get(Location_.iataCode), request.getIataCode()));
            }

            if (!ObjectUtils.isEmpty(request.getIcaoCode())) {
                predicates.add(cb.equal(root.get(Location_.icaoCode), request.getIcaoCode()));
            }

            if (!ObjectUtils.isEmpty(request.getCountryIsoCode())) {
                predicates.add(cb.equal(root.get(Location_.countryIsoCode), request.getCountryIsoCode()));
            }

            if (!ObjectUtils.isEmpty(request.getCity())) {
                predicates
                        .add(cb.like(cb.lower(root.get(Location_.city)), "%" + request.getCity().toLowerCase() + "%"));
            }

            if (request.getType() != null) {
                predicates.add(cb.equal(root.get(Location_.type), request.getType()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
