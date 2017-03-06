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
package net.maritimecloud.identityregistry.security.x509;

import net.maritimecloud.identityregistry.model.database.Certificate;
import net.maritimecloud.identityregistry.model.database.Organization;
import net.maritimecloud.identityregistry.model.database.Role;
import net.maritimecloud.identityregistry.services.CertificateService;
import net.maritimecloud.identityregistry.services.OrganizationService;
import net.maritimecloud.identityregistry.services.RoleService;
import net.maritimecloud.identityregistry.utils.CertificateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.ldap.userdetails.InetOrgPerson;
import org.springframework.stereotype.Service;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Service("userDetailsService")
public class X509HeaderUserDetailsService implements UserDetailsService {

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private RoleService roleService;
    @Autowired
    private CertificateService certificateService;
    @Autowired
    private CertificateUtil certUtil;

    private static final Logger logger = LoggerFactory.getLogger(X509HeaderUserDetailsService.class);

    @Override
    public UserDetails loadUserByUsername(String certificateHeader) throws UsernameNotFoundException {
        if (certificateHeader == null || certificateHeader.length() < 10) {
            logger.warn("No certificate header found");
            throw new UsernameNotFoundException("No certificate header found");
        }
        X509Certificate userCertificate = certUtil.getCertFromString(certificateHeader);
        if (userCertificate == null) {
            logger.error("Extracting certificate from header failed");
            throw new UsernameNotFoundException("Extracting certificate from header failed");
        }
        
        // Actually authenticate certificate against root cert.
        if (!certUtil.verifyCertificate(userCertificate)) {
            logger.warn("Certificate could not be verified");
            throw new UsernameNotFoundException("Certificate could not be verified");
        }
        // Check that the certificate has not been revoked
        long certId = userCertificate.getSerialNumber().longValue();
        Certificate cert = certificateService.getCertificateById(certId);
        if (cert.isRevoked()) {
            Calendar cal = Calendar.getInstance();
            Date now = cal.getTime();
            if (cert.getRevokedAt() == null || cert.getRevokedAt().before(now)) {
                logger.warn("The certificate has been revoked! Cert #" + certId);
                throw new UsernameNotFoundException("The certificate has been revoked! Cert #" + certId);
            }
        }
        // Get user details from the certificate
        UserDetails user = certUtil.getUserFromCert(userCertificate);
        if (user == null) {
            logger.warn("Extraction of data from the certificate failed");
            throw new UsernameNotFoundException("Extraction of data from the client certificate failed");
        }
        // Convert the permissions extracted from the certificate to authorities in this API
        InetOrgPerson person = ((InetOrgPerson)user);
        String certOrg = person.getO();
        Organization org = organizationService.getOrganizationByMrn(certOrg);
        if (org == null) {
            logger.warn("Unknown Organization '" + certOrg + "' in client certificate");
            throw new UsernameNotFoundException("Unknown Organization in client certificate");
        }
        Collection<GrantedAuthority> newRoles = new ArrayList<>();
        logger.debug("Looking up roles");
        for (GrantedAuthority role : user.getAuthorities()) {
            logger.debug("Looking up roles");
            String auth = role.getAuthority();
            String[] auths = auth.split(",");
            for (String auth2 : auths) {
                logger.debug("Looking up role: " + auth2);
                List<Role> foundRoles = roleService.getRolesByIdOrganizationAndPermission(org.getId(), auth2);
                if (foundRoles != null) {
                    for (Role foundRole : foundRoles) {
                        newRoles.add(new SimpleGrantedAuthority(foundRole.getRoleName()));
                    }
                }
            }
        }
        // Add ROLE_USER as standard for authenticated users with no other role.
        if (newRoles.isEmpty()) {
            newRoles.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        InetOrgPerson.Essence essence = new InetOrgPerson.Essence((InetOrgPerson) user);
        essence.setAuthorities(newRoles);
        return essence.createUserDetails();
    }
}
