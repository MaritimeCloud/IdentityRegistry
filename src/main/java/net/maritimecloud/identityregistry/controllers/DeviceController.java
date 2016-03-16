/* Copyright 2016 Danish Maritime Authority.
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

import org.springframework.web.bind.annotation.RestController;

import net.maritimecloud.identityregistry.exception.McBasicRestException;
import net.maritimecloud.identityregistry.model.Certificate;
import net.maritimecloud.identityregistry.model.CertificateRevocation;
import net.maritimecloud.identityregistry.model.Organization;
import net.maritimecloud.identityregistry.model.Device;
import net.maritimecloud.identityregistry.services.CertificateService;
import net.maritimecloud.identityregistry.services.OrganizationService;
import net.maritimecloud.identityregistry.services.DeviceService;
import net.maritimecloud.identityregistry.utils.AccessControlUtil;
import net.maritimecloud.identityregistry.utils.CertificateUtil;
import net.maritimecloud.identityregistry.utils.MCIdRegConstants;

import java.security.KeyPair;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping(value={"oidc", "x509"})
public class DeviceController {
    private DeviceService deviceService;
    private OrganizationService organizationService;
    private CertificateService certificateService;

    @Autowired
    public void setCertificateService(CertificateService certificateService) {
        this.certificateService = certificateService;
    }

    @Autowired
    public void setOrganizationService(OrganizationService organizationService) {
        this.organizationService = organizationService;
    }
    @Autowired
    public void setDeviceService(DeviceService organizationService) {
        this.deviceService = organizationService;
    }

    /**
     * Creates a new Device
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */ 
    @RequestMapping(
            value = "/api/org/{orgShortName}/device",
            method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> createDevice(HttpServletRequest request, @PathVariable String orgShortName, @RequestBody Device input) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                input.setIdOrganization(org.getId());
                Device newDevice = this.deviceService.saveDevice(input);
                return new ResponseEntity<Device>(newDevice, HttpStatus.OK);
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Returns info about the device identified by the given ID
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/device/{deviceId}",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public ResponseEntity<?> getDevice(HttpServletRequest request, @PathVariable String orgShortName, @PathVariable Long deviceId) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                Device device = this.deviceService.getDeviceById(deviceId);
                if (device == null) {
                    throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.DEVICE_NOT_FOUND, request.getServletPath());
                }
                if (device.getIdOrganization().compareTo(org.getId()) == 0) {
                    return new ResponseEntity<Device>(device, HttpStatus.OK);
                }
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Updates a Device
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/device/{deviceId}",
            method = RequestMethod.PUT)
    @ResponseBody
    public ResponseEntity<?> updateDevice(HttpServletRequest request, @PathVariable String orgShortName, @PathVariable Long deviceId, @RequestBody Device input) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                Device device = this.deviceService.getDeviceById(deviceId);
                if (device == null) {
                    throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.VESSEL_NOT_FOUND, request.getServletPath());
                }
                if (device.getId().compareTo(input.getId()) == 0 && device.getIdOrganization().compareTo(org.getId()) == 0) {
                    input.selectiveCopyTo(device);
                    this.deviceService.saveDevice(device);
                    return new ResponseEntity<>(HttpStatus.OK);
                }
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Deletes a Device
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/device/{deviceId}",
            method = RequestMethod.DELETE)
    @ResponseBody
    public ResponseEntity<?> deleteDevice(HttpServletRequest request, @PathVariable String orgShortName, @PathVariable Long deviceId) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                Device device = this.deviceService.getDeviceById(deviceId);
                if (device == null) {
                    throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.VESSEL_NOT_FOUND, request.getServletPath());
                }
                if (device.getIdOrganization().compareTo(org.getId()) == 0) {
                    this.deviceService.deleteDevice(deviceId);
                    return new ResponseEntity<>(HttpStatus.OK);
                }
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Returns a list of devices owned by the organization identified by the given ID
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/devices",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> getOrganizationDevices(HttpServletRequest request, @PathVariable String orgShortName) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                List<Device> devices = this.deviceService.listOrgDevices(org.getId());
                return new ResponseEntity<List<Device>>(devices, HttpStatus.OK);
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Returns new certificate for the device identified by the given ID
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/device/{deviceId}/generatecertificate",
            method = RequestMethod.GET,
            produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> newOrgCert(HttpServletRequest request, @PathVariable String orgShortName, @PathVariable Long deviceId) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                Device device = this.deviceService.getDeviceById(deviceId);
                if (device == null) {
                    throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.DEVICE_NOT_FOUND, request.getServletPath());
                }
                if (device.getIdOrganization().compareTo(org.getId()) == 0) {
                    // Create the certificate and save it so that it gets an id that can be use as certificate serialnumber
                    Certificate newMCCert = new Certificate();
                    newMCCert.setDevice(device);
                    newMCCert = this.certificateService.saveCertificate(newMCCert);
                    // Generate keypair for device
                    KeyPair deviceKeyPair = CertificateUtil.generateKeyPair();
                    X509Certificate deviceCert = CertificateUtil.generateCertForEntity(newMCCert.getId(), org.getCountry(), org.getName(), device.getName(), device.getName(), "", deviceKeyPair.getPublic(), null);
                    String pemCertificate = "";
                    try {
                        pemCertificate = CertificateUtil.getPemFromEncoded("CERTIFICATE", deviceCert.getEncoded());
                    } catch (CertificateEncodingException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    String pemPublicKey = CertificateUtil.getPemFromEncoded("PUBLIC KEY", deviceKeyPair.getPublic().getEncoded());
                    String pemPrivateKey = CertificateUtil.getPemFromEncoded("PRIVATE KEY", deviceKeyPair.getPrivate().getEncoded());
                    newMCCert.setCertificate(pemCertificate);
                    newMCCert.setStart(deviceCert.getNotBefore());
                    newMCCert.setEnd(deviceCert.getNotAfter());
                    newMCCert.setDevice(device);
                    this.certificateService.saveCertificate(newMCCert);
                    String jsonReturn = "{ \"publickey\":\"" + pemPublicKey + "\", \"privatekey\":\"" + pemPrivateKey + "\", \"certificate\":\"" + pemCertificate + "\"  }";

                    return new ResponseEntity<String>(jsonReturn, HttpStatus.OK);
                }
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }

    /**
     * Revokes certificate for the device identified by the given ID
     * 
     * @return a reply...
     * @throws McBasicRestException 
     */
    @RequestMapping(
            value = "/api/org/{orgShortName}/device/{deviceId}/certificates/{certId}/revoke",
            method = RequestMethod.POST,
            produces = "application/json;charset=UTF-8")
    public ResponseEntity<?> revokeVesselCert(HttpServletRequest request, @PathVariable String orgShortName, @PathVariable Long deviceId, @PathVariable Long certId,  @RequestBody CertificateRevocation input) throws McBasicRestException {
        Organization org = this.organizationService.getOrganizationByShortName(orgShortName);
        if (org != null) {
            // Check that the device has the needed rights
            if (AccessControlUtil.hasAccessToOrg(org.getName(), orgShortName)) {
                Device device = this.deviceService.getDeviceById(deviceId);
                if (device == null) {
                    throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.DEVICE_NOT_FOUND, request.getServletPath());
                }
                if (device.getIdOrganization().compareTo(org.getId()) == 0) {
                    Certificate cert = this.certificateService.getCertificateById(certId);
                    Device certDevice = cert.getDevice();
                    if (certDevice != null && certDevice.getId().compareTo(device.getId()) == 0) {
                        if (!input.validateReason()) {
                            throw new McBasicRestException(HttpStatus.BAD_REQUEST, MCIdRegConstants.INVALID_REVOCATION_REASON, request.getServletPath());
                        }
                        if (input.getRevokedAt() == null) {
                            throw new McBasicRestException(HttpStatus.BAD_REQUEST, MCIdRegConstants.INVALID_REVOCATION_DATE, request.getServletPath());
                        }
                        cert.setRevokedAt(input.getRevokedAt());
                        cert.setRevokeReason(input.getRevokationReason());
                        cert.setRevoked(true);
                        this.certificateService.saveCertificate(cert);
                        return new ResponseEntity<>(HttpStatus.OK);
                    }
                }
            }
            throw new McBasicRestException(HttpStatus.FORBIDDEN, MCIdRegConstants.MISSING_RIGHTS, request.getServletPath());
        } else {
            throw new McBasicRestException(HttpStatus.NOT_FOUND, MCIdRegConstants.ORG_NOT_FOUND, request.getServletPath());
        }
    }


}

