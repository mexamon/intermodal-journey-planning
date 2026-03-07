package com.thy.cloud.service.dao.entity.transport;

import com.thy.cloud.data.jpa.entity.AbstractAuditionGuidKeyEntity;
import com.thy.cloud.service.dao.entity.inventory.Location;
import lombok.*;

import jakarta.persistence.*;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Table(name = "transport_stop")
public class TransportStop extends AbstractAuditionGuidKeyEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    @ToString.Exclude
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_area_id", nullable = false)
    @ToString.Exclude
    private TransportServiceArea serviceArea;

    @Column(name = "stop_code")
    private String stopCode;

    @Column(name = "stop_name", nullable = false)
    private String stopName;

    @Column(name = "stop_sequence")
    private Integer stopSequence;

    @Column(name = "is_terminal", nullable = false)
    private Boolean isTerminal;

    @Column(name = "platform_info")
    private String platformInfo;

    @Column(name = "attrs_json", columnDefinition = "JSONB")
    private String attrsJson;
}
