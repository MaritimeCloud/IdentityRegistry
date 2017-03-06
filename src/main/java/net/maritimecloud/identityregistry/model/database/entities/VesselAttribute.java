/*
 * Copyright 2017 Danish Maritime Authority.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.maritimecloud.identityregistry.model.database.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.maritimecloud.identityregistry.model.database.TimestampModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.util.Date;

/**
 * Model object representing a vessel attribute
 */

@Entity
@Table(name = "vessel_attributes")
@Getter
@Setter
@ToString(exclude = "vessel")
public class VesselAttribute extends TimestampModel {

    public VesselAttribute() {
    }

    @ApiModelProperty(value = "Vessel attribute name", required = true, allowableValues = "imo-number, mmsi-number, callsign, flagstate, ais-class, port-of-register")
    @Column(name = "attribute_name")
    private String attributeName;

    @ApiModelProperty(value = "Vessel attribute value", required = true)
    @Column(name = "attribute_value")
    private String attributeValue;

    @Column(name = "start")
    private Date start;

    @Column(name = "end")
    private Date end;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_vessel")
    private Vessel vessel;

    /******************************/
    /** Getters and setters      **/
    /******************************/

    @Override
    @JsonIgnore
    public Long getId() {
        return id;
    }

    public void setAttributeName(String attributeName) {
        if (attributeName != null) {
            attributeName = attributeName.toLowerCase();
        }
        this.attributeName = attributeName;
    }
}
