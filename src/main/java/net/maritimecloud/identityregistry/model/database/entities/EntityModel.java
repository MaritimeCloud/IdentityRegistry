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
import net.maritimecloud.identityregistry.model.database.CertificateModel;
import net.maritimecloud.identityregistry.validators.MRN;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.util.Objects;

@MappedSuperclass
@Getter
@Setter
@ToString
public abstract class EntityModel extends CertificateModel {


    @JsonIgnore
    @Column(name = "id_organization")
    private Long idOrganization;

    @ApiModelProperty(value = "The Maritime Resource Name", required = true)
    @MRN
    @Column(name = "mrn")
    private String mrn;

    @ApiModelProperty(value = "Permissions as assigned from the organization")
    @Column(name = "permissions")
    private String permissions;

    /** Copies this entity into the other */
    public EntityModel copyTo(EntityModel entity) {
        Objects.requireNonNull(entity);
        entity.setId(id);
        entity.setIdOrganization(idOrganization);
        entity.setMrn(mrn);
        entity.setPermissions(permissions);
        return entity;
    }

    /** Copies this entity into the other
     * Only update things that are allowed to change on update */
    public EntityModel selectiveCopyTo(EntityModel entity) {
        Objects.requireNonNull(entity);
        entity.setMrn(mrn);
        entity.setPermissions(permissions);
        return entity;
    }

}
