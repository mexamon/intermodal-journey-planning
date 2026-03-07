package com.thy.cloud.service.api.modules.transport.specs;

import com.thy.cloud.service.api.modules.transport.model.EdgeSearchRequest;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge;
import com.thy.cloud.service.dao.entity.transport.TransportationEdge_;
import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity_;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;

public class EdgeSpecs {

    private EdgeSpecs() {
    }

    public static Specification<TransportationEdge> filter(EdgeSearchRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getOriginLocationId() != null) {
                predicates.add(cb.equal(
                        root.get(TransportationEdge_.originLocation).get(AbstractAuditionGuidKeyEntity_.id),
                        request.getOriginLocationId()));
            }

            if (request.getDestinationLocationId() != null) {
                predicates.add(cb.equal(
                        root.get(TransportationEdge_.destinationLocation).get(AbstractAuditionGuidKeyEntity_.id),
                        request.getDestinationLocationId()));
            }

            if (request.getTransportModeId() != null) {
                predicates.add(cb.equal(
                        root.get(TransportationEdge_.transportMode).get(AbstractAuditionGuidKeyEntity_.id),
                        request.getTransportModeId()));
            }

            if (request.getStatus() != null) {
                predicates.add(cb.equal(root.get(TransportationEdge_.status), request.getStatus()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
