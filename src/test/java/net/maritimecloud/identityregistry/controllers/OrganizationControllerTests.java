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
package net.maritimecloud.identityregistry.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.maritimecloud.identityregistry.model.database.IdentityProviderAttribute;
import net.maritimecloud.identityregistry.model.database.Organization;
import net.maritimecloud.identityregistry.model.database.entities.Device;
import net.maritimecloud.identityregistry.model.database.entities.Service;
import net.maritimecloud.identityregistry.model.database.entities.User;
import net.maritimecloud.identityregistry.model.database.entities.Vessel;
import net.maritimecloud.identityregistry.services.CertificateService;
import net.maritimecloud.identityregistry.services.EntityService;
import net.maritimecloud.identityregistry.services.OrganizationService;
import net.maritimecloud.identityregistry.services.RoleService;
import net.maritimecloud.identityregistry.utils.AccessControlUtil;
import net.maritimecloud.identityregistry.utils.EmailUtil;
import net.maritimecloud.identityregistry.utils.KeycloakAdminUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration
@WebAppConfiguration
public class OrganizationControllerTests {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @MockBean
    private EntityService<Device> deviceService;
    @MockBean
    private EntityService<Service> serviceService;
    @MockBean
    private EntityService<User> userService;
    @MockBean
    private EntityService<Vessel> vesselService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private EmailUtil emailUtil;

    @MockBean
    private OrganizationService organizationService;

    @MockBean
    private KeycloakAdminUtil keycloakAU;

    @MockBean
    private CertificateService certificateService;

    @MockBean
    private AccessControlUtil accessControlUtil;

    @Before
    public void setup() {
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                //.alwaysDo(print())
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    /**
     * Try to approve an organization without the appropriate role
     */
    @WithMockUser(roles="ORG_ADMIN")
    @Test
    public void testAccessApproveOrgWithoutRights() {
        try {
            mvc.perform(get("/oidc/api/org/urn:mrn:mcl:org:dma/approve").header("Origin", "bla")).andExpect(status().isForbidden());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to approve an organization with the appropriate role
     */
    @WithMockUser(roles="SITE_ADMIN")
    @Test
    public void testAccessApproveOrgWithRights() {
        given(this.organizationService.getOrganizationByMrnDisregardApproved("urn:mrn:mcl:org:dma")).willReturn(new Organization());
        try {
            mvc.perform(get("/oidc/api/org/urn:mrn:mcl:org:dma/approve").header("Origin", "bla")).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to delete an organization without the appropriate role
     */
    @WithMockUser(roles="ORG_ADMIN")
    @Test
    public void testAccessDeleteOrgWithoutRights() {
        try {
            mvc.perform(delete("/oidc/api/org/urn:mrn:mcl:org:dma").header("Origin", "bla")).andExpect(status().isForbidden());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to delete an organization with the appropriate role
     */
    @WithMockUser(roles="SITE_ADMIN")
    @Test
    public void testAccessDeleteOrgWithRights() {
        given(this.organizationService.getOrganizationByMrnDisregardApproved("urn:mrn:mcl:org:dma")).willReturn(new Organization());
        try {
            mvc.perform(delete("/oidc/api/org/urn:mrn:mcl:org:dma").header("Origin", "bla")).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to update an organization with the appropriate role
     */
    @Test
    public void testAccessUpdateOrgWithRights() {
        // Build org object to test with
        Organization org = new Organization();
        org.setMrn("urn:mrn:mcl:org:dma");
        org.setAddress("Carl Jakobsensvej 31, 2500 Valby");
        org.setCountry("Denmark");
        org.setUrl("http://dma.dk");
        org.setEmail("dma@dma.dk");
        org.setName("Danish Maritime Authority");
        List<IdentityProviderAttribute> identityProviderAttributes = new ArrayList<>();
        org.setIdentityProviderAttributes(identityProviderAttributes);
        // Serialize org object
        String orgJson = this.serialize(org);
        // Create fake authentication object
        Authentication auth = TokenGenerator.generateKeycloakToken("urn:mrn:mcl:org:dma", "ROLE_ORG_ADMIN", "");
        given(this.organizationService.getOrganizationByMrn("urn:mrn:mcl:org:dma")).willReturn(org);
        try {
            mvc.perform(put("/oidc/api/org/urn:mrn:mcl:org:dma").with(authentication(auth))
                            .header("Origin", "bla")
                            .content(orgJson)
                            .contentType("application/json")
                        ).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to update an organization with data mismatch between json and url
     */
    @Test
    public void testAccessUpdateOrgWithDataMismatch() {
        // Build org object to test with
        Organization org = new Organization();
        // The mrn is deliberately wrong - that is the point of the test
        org.setMrn("urn:mrn:mcl:org:sma");
        org.setAddress("Carl Jakobsensvej 31, 2500 Valby");
        org.setCountry("Denmark");
        org.setUrl("http://dma.dk");
        org.setEmail("dma@dma.dk");
        org.setName("Danish Maritime Authority");
        List<IdentityProviderAttribute> identityProviderAttributes = new ArrayList<>();
        org.setIdentityProviderAttributes(identityProviderAttributes);
        // Serialize org object
        String orgJson = this.serialize(org);
        // Create fake authentication object
        Authentication auth = TokenGenerator.generatePreAuthenticatedAuthenticationToken("urn:mrn:mcl:org:dma", "ROLE_ORG_ADMIN", "");
        given(this.organizationService.getOrganizationByMrn("urn:mrn:mcl:org:dma")).willReturn(org);
        try {
            // Note that the mrn in the url is different from the org mrn - should mean it fails
            mvc.perform(put("/oidc/api/org/urn:mrn:mcl:org:dma").with(authentication(auth))
                    .header("Origin", "bla")
                    .content(orgJson)
                    .contentType("application/json")
            ).andExpect(status().isBadRequest());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to update an organization without the appropriate association
     */
    @Test
    public void testAccessUpdateOrgWithoutRights() {
        // Build org object to test with
        Organization org = new Organization();
        org.setMrn("urn:mrn:mcl:org:dma");
        org.setAddress("Carl Jakobsensvej 31, 2500 Valby");
        org.setCountry("Denmark");
        org.setUrl("http://dma.dk");
        org.setEmail("dma@dma.dk");
        org.setName("Danish Maritime Authority");
        List<IdentityProviderAttribute> identityProviderAttributes = new ArrayList<>();
        org.setIdentityProviderAttributes(identityProviderAttributes);
        // Serialize org object
        String orgJson = this.serialize(org);
        // Create fake authentication object - note that the users orgMrn is different from mrn of the org - means it should fail
        Authentication auth = TokenGenerator.generateKeycloakToken("urn:mrn:mcl:org:sma", "ROLE_ORG_ADMIN", "");
        given(this.organizationService.getOrganizationByMrn("urn:mrn:mcl:org:dma")).willReturn(org);
        try {
            mvc.perform(put("/oidc/api/org/urn:mrn:mcl:org:dma").with(authentication(auth))
                    .header("Origin", "bla")
                    .content(orgJson)
                    .contentType("application/json")
            ).andExpect(status().isForbidden());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }


    /**
     * Try to access an organization with the appropriate role
     */
    @Test
    public void testAccessGetOrgWithRights() {
        // Build org object to test with
        Organization org = new Organization();
        org.setMrn("urn:mrn:mcl:org:dma");
        org.setAddress("Carl Jakobsensvej 31, 2500 Valby");
        org.setCountry("Denmark");
        org.setUrl("http://dma.dk");
        org.setEmail("dma@dma.dk");
        org.setName("Danish Maritime Authority");
        List<IdentityProviderAttribute> identityProviderAttributes = new ArrayList<>();
        org.setIdentityProviderAttributes(identityProviderAttributes);
        // Serialize org object
        String orgJson = this.serialize(org);
        // Create fake authentication object
        Authentication auth = TokenGenerator.generateKeycloakToken("urn:mrn:mcl:org:dma", "ROLE_ORG_ADMIN", "");
        given(this.organizationService.getOrganizationByMrn("urn:mrn:mcl:org:dma")).willReturn(org);
        try {
            mvc.perform(get("/oidc/api/org/urn:mrn:mcl:org:dma").with(authentication(auth))
                    .header("Origin", "bla")
            ).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }

    /**
     * Try to access an organization with the appropriate role as SITE_ADMIN
     */
    @Test
    public void testAccessGetOrgWithRights2() {
        // Build org object to test with
        Organization org = new Organization();
        org.setMrn("urn:mrn:mcl:org:dma");
        org.setAddress("Carl Jakobsensvej 31, 2500 Valby");
        org.setCountry("Denmark");
        org.setUrl("http://dma.dk");
        org.setEmail("dma@dma.dk");
        org.setName("Danish Maritime Authority");
        List<IdentityProviderAttribute> identityProviderAttributes = new ArrayList<>();
        org.setIdentityProviderAttributes(identityProviderAttributes);
        // Serialize org object
        String orgJson = this.serialize(org);
        // Create fake authentication object - note that the user mrn is from a different org that the organization, but the role should overrule that
        Authentication auth = TokenGenerator.generateKeycloakToken("urn:mrn:mcl:org:sma", "ROLE_SITE_ADMIN", "");
        given(this.organizationService.getOrganizationByMrn("urn:mrn:mcl:org:dma")).willReturn(org);
        try {
            mvc.perform(get("/oidc/api/org/urn:mrn:mcl:org:dma").with(authentication(auth))
                    .header("Origin", "bla")
            ).andExpect(status().isOk());
        } catch (Exception e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }


    /**
     * Helper function to serialize an organization to json
     * @param org
     * @return
     */
    private String serialize(Organization org) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Convert object to JSON string
            String jsonInString = mapper.writeValueAsString(org);
            //System.out.println(jsonInString);

            // Convert object to JSON string and pretty print
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(org);
            //System.out.println(jsonInString);

            return jsonInString;
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
