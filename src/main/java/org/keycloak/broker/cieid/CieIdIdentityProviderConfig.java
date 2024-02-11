/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.cieid;

import static org.keycloak.common.util.UriUtils.checkUrl;

import org.keycloak.common.enums.SslRequired;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.saml.SamlPrincipalType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;

public class CieIdIdentityProviderConfig extends IdentityProviderModel {

    public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;

    public static final String ENTITY_ID = "entityId";
    public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
    public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
    public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
    public static final String FORCE_AUTHN = "forceAuthn";
    public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
    public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
    public static final String POST_BINDING_LOGOUT = "postBindingLogout";
    public static final String POST_BINDING_RESPONSE = "postBindingResponse";
    public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
    public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
    public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
    public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
    public static final String VALIDATE_SIGNATURE = "validateSignature";
    public static final String PRINCIPAL_TYPE = "principalType";
    public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
    public static final String WANT_ASSERTIONS_ENCRYPTED = "wantAssertionsEncrypted";
    public static final String WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
    public static final String WANT_AUTHN_REQUESTS_SIGNED = "wantAuthnRequestsSigned";
    public static final String XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER = "xmlSigKeyInfoKeyNameTransformer";
    public static final String ENABLED_FROM_METADATA  = "enabledFromMetadata";
    public static final String AUTHN_CONTEXT_COMPARISON_TYPE = "authnContextComparisonType";
    public static final String AUTHN_CONTEXT_CLASS_REFS = "authnContextClassRefs";
    public static final String AUTHN_CONTEXT_DECL_REFS = "authnContextDeclRefs";
    public static final String SIGN_SP_METADATA = "signSpMetadata";
    public static final String ALLOW_CREATE = "allowCreate";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_INDEX = "attributeConsumingServiceIndex";
    public static final String ATTRIBUTE_CONSUMING_SERVICE_NAME = "attributeConsumingServiceName";
    public static final String ORGANIZATION_NAMES = "organizationNames";
    public static final String ORGANIZATION_DISPLAY_NAMES = "organizationDisplayNames";
    public static final String ORGANIZATION_URLS = "organizationUrls";
    public static final String ADMINISTRATIVE_CONTACT_SP_PRIVATE = "administrativeContactIsSpPrivate";
    public static final String ADMINISTRATIVE_CONTACT_COMPANY = "administrativeContactCompany";
    public static final String ADMINISTRATIVE_CONTACT_IPA_CATEGORY = "administrativeContactIpaCategory";
    public static final String ADMINISTRATIVE_CONTACT_IPA_CODE = "administrativeContactIpaCode";
    public static final String ADMINISTRATIVE_CONTACT_VAT_NUMBER = "administrativeContactVatNumber";
    public static final String ADMINISTRATIVE_CONTACT_FISCAL_CODE = "administrativeContactFiscalCode";
    public static final String ADMINISTRATIVE_CONTACT_NACE2_CODES = "administrativeContactNace2Codes";
    public static final String ADMINISTRATIVE_CONTACT_MUNICIPALITY = "administrativeContactMunicipality";
    public static final String ADMINISTRATIVE_CONTACT_PROVINCE = "administrativeContactProvince";
    public static final String ADMINISTRATIVE_CONTACT_COUNTRY = "administrativeContactCountry";
    public static final String ADMINISTRATIVE_CONTACT_PHONE = "administrativeContactPhone";
    public static final String ADMINISTRATIVE_CONTACT_EMAIL = "administrativeContactEmail";
    public static final String TECHNICAL_CONTACT_COMPANY = "technicalContactCompany";
    public static final String TECHNICAL_CONTACT_VAT_NUMBER = "technicalContactVatNumber";
    public static final String TECHNICAL_CONTACT_FISCAL_CODE = "technicalContactFiscalCode";
    public static final String TECHNICAL_CONTACT_NACE2_CODES = "technicalContactNace2Codes";
    public static final String TECHNICAL_CONTACT_MUNICIPALITY = "technicalContactMunicipality";
    public static final String TECHNICAL_CONTACT_PROVINCE = "technicalContactProvince";
    public static final String TECHNICAL_CONTACT_COUNTRY = "technicalContactCountry";
    public static final String TECHNICAL_CONTACT_PHONE = "technicalContactPhone";
    public static final String TECHNICAL_CONTACT_EMAIL = "technicalContactEmail";
    public static final String REALM_KEYS_PROVIDER_ID = "realmKeysProviderId";

    public CieIdIdentityProviderConfig(){
    }

    public CieIdIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
    }

    public String getEntityId() {
        return getConfig().get(ENTITY_ID);
    }

    public void setEntityId(String entityId) {
        getConfig().put(ENTITY_ID, entityId);
    }

    public String getSingleSignOnServiceUrl() {
        return getConfig().get(SINGLE_SIGN_ON_SERVICE_URL);
    }

    public void setSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
        getConfig().put(SINGLE_SIGN_ON_SERVICE_URL, singleSignOnServiceUrl);
    }

    public String getSingleLogoutServiceUrl() {
        return getConfig().get(SINGLE_LOGOUT_SERVICE_URL);
    }

    public void setSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
        getConfig().put(SINGLE_LOGOUT_SERVICE_URL, singleLogoutServiceUrl);
    }

    public boolean isValidateSignature() {
        return Boolean.valueOf(getConfig().get(VALIDATE_SIGNATURE));
    }

    public void setValidateSignature(boolean validateSignature) {
        getConfig().put(VALIDATE_SIGNATURE, String.valueOf(validateSignature));
    }

    public boolean isForceAuthn() {
        return Boolean.valueOf(getConfig().get(FORCE_AUTHN));
    }

    public void setForceAuthn(boolean forceAuthn) {
        getConfig().put(FORCE_AUTHN, String.valueOf(forceAuthn));
    }

    /**
     * @deprecated Prefer {@link #getSigningCertificates()}}
     * @param signingCertificate
     */
    public String getSigningCertificate() {
        return getConfig().get(SIGNING_CERTIFICATE_KEY);
    }

    /**
     * @deprecated Prefer {@link #addSigningCertificate(String)}}
     * @param signingCertificate
     */
    public void setSigningCertificate(String signingCertificate) {
        getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
    }

    public void addSigningCertificate(String signingCertificate) {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            getConfig().put(SIGNING_CERTIFICATE_KEY, signingCertificate);
        } else {
            // Note that "," is not coding character per PEM format specification:
            // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
            getConfig().put(SIGNING_CERTIFICATE_KEY, crt + "," + signingCertificate);
        }
    }

    public String[] getSigningCertificates() {
        String crt = getConfig().get(SIGNING_CERTIFICATE_KEY);
        if (crt == null || crt.isEmpty()) {
            return new String[] { };
        }
        // Note that "," is not coding character per PEM format specification:
        // see https://tools.ietf.org/html/rfc1421, section 4.3.2.4 Step 4: Printable Encoding
        return crt.split(",");
    }

    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }

    public void setNameIDPolicyFormat(String nameIDPolicyFormat) {
        getConfig().put(NAME_ID_POLICY_FORMAT, nameIDPolicyFormat);
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public void setWantAuthnRequestsSigned(boolean wantAuthnRequestsSigned) {
        getConfig().put(WANT_AUTHN_REQUESTS_SIGNED, String.valueOf(wantAuthnRequestsSigned));
    }

    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    public void setWantAssertionsSigned(boolean wantAssertionsSigned) {
        getConfig().put(WANT_ASSERTIONS_SIGNED, String.valueOf(wantAssertionsSigned));
    }

    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_ENCRYPTED));
    }

    public void setWantAssertionsEncrypted(boolean wantAssertionsEncrypted) {
        getConfig().put(WANT_ASSERTIONS_ENCRYPTED, String.valueOf(wantAssertionsEncrypted));
    }

    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(getConfig().get(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
    }

    public void setAddExtensionsElementWithKeyInfo(boolean addExtensionsElementWithKeyInfo) {
        getConfig().put(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO, String.valueOf(addExtensionsElementWithKeyInfo));
    }

    public String getSignatureAlgorithm() {
        return getConfig().get(SIGNATURE_ALGORITHM);
    }

    public void setSignatureAlgorithm(String signatureAlgorithm) {
        getConfig().put(SIGNATURE_ALGORITHM, signatureAlgorithm);
    }

    public String getEncryptionPublicKey() {
        return getConfig().get(ENCRYPTION_PUBLIC_KEY);
    }

    public void setEncryptionPublicKey(String encryptionPublicKey) {
        getConfig().put(ENCRYPTION_PUBLIC_KEY, encryptionPublicKey);
    }

    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }

    public void setPostBindingAuthnRequest(boolean postBindingAuthnRequest) {
        getConfig().put(POST_BINDING_AUTHN_REQUEST, String.valueOf(postBindingAuthnRequest));
    }

    public boolean isPostBindingResponse() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_RESPONSE));
    }

    public void setPostBindingResponse(boolean postBindingResponse) {
        getConfig().put(POST_BINDING_RESPONSE, String.valueOf(postBindingResponse));
    }

    public boolean isPostBindingLogout() {
        String postBindingLogout = getConfig().get(POST_BINDING_LOGOUT);
        if (postBindingLogout == null) {
            // To maintain unchanged behavior when adding this field, we set the inital value to equal that
            // of the binding for the response:
            return isPostBindingResponse();
        }
        return Boolean.valueOf(postBindingLogout);
    }

    public void setPostBindingLogout(boolean postBindingLogout) {
        getConfig().put(POST_BINDING_LOGOUT, String.valueOf(postBindingLogout));
    }

    public boolean isBackchannelSupported() {
        return Boolean.valueOf(getConfig().get(BACKCHANNEL_SUPPORTED));
    }

    public void setBackchannelSupported(boolean backchannel) {
        getConfig().put(BACKCHANNEL_SUPPORTED, String.valueOf(backchannel));
    }

    /**
     * Always returns non-{@code null} result.
     * @return Configured ransformer of {@link #DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER} if not set.
     */
    public XmlKeyInfoKeyNameTransformer getXmlSigKeyInfoKeyNameTransformer() {
        return XmlKeyInfoKeyNameTransformer.from(getConfig().get(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER), DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER);
    }

    public void setXmlSigKeyInfoKeyNameTransformer(XmlKeyInfoKeyNameTransformer xmlSigKeyInfoKeyNameTransformer) {
        getConfig().put(XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER,
          xmlSigKeyInfoKeyNameTransformer == null
            ? null
            : xmlSigKeyInfoKeyNameTransformer.name());
    }

    public int getAllowedClockSkew() {
        int result = 0;
        String allowedClockSkew = getConfig().get(ALLOWED_CLOCK_SKEW);
        if (allowedClockSkew != null && !allowedClockSkew.isEmpty()) {
            try {
                result = Integer.parseInt(allowedClockSkew);
                if (result < 0) {
                    result = 0;
                }
            } catch (NumberFormatException e) {
                // ignore it and use 0
            }
        }
        return result;
    }

    public void setAllowedClockSkew(int allowedClockSkew) {
        if (allowedClockSkew < 0) {
            getConfig().remove(ALLOWED_CLOCK_SKEW);
        } else {
            getConfig().put(ALLOWED_CLOCK_SKEW, String.valueOf(allowedClockSkew));
        }
    }

    public SamlPrincipalType getPrincipalType() {
        return SamlPrincipalType.from(getConfig().get(PRINCIPAL_TYPE), SamlPrincipalType.SUBJECT);
    }

    public void setPrincipalType(SamlPrincipalType principalType) {
        getConfig().put(PRINCIPAL_TYPE,
            principalType == null
                ? null
                : principalType.name());
    }

    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    public void setPrincipalAttribute(String principalAttribute) {
        getConfig().put(PRINCIPAL_ATTRIBUTE, principalAttribute);
    }

    public boolean isEnabledFromMetadata() {
        return Boolean.valueOf(getConfig().get(ENABLED_FROM_METADATA ));
    }

    public void setEnabledFromMetadata(boolean enabled) {
        getConfig().put(ENABLED_FROM_METADATA , String.valueOf(enabled));
    }

    public AuthnContextComparisonType getAuthnContextComparisonType() {
        return AuthnContextComparisonType.fromValue(getConfig().getOrDefault(AUTHN_CONTEXT_COMPARISON_TYPE, AuthnContextComparisonType.EXACT.value()));
    }

    public void setAuthnContextComparisonType(AuthnContextComparisonType authnContextComparisonType) {
        getConfig().put(AUTHN_CONTEXT_COMPARISON_TYPE, authnContextComparisonType.value());
    }

    public String getAuthnContextClassRefs() {
        return getConfig().get(AUTHN_CONTEXT_CLASS_REFS);
    }

    public void setAuthnContextClassRefs(String authnContextClassRefs) {
        getConfig().put(AUTHN_CONTEXT_CLASS_REFS, authnContextClassRefs);
    }

    public String getAuthnContextDeclRefs() {
        return getConfig().get(AUTHN_CONTEXT_DECL_REFS);
    }

    public void setAuthnContextDeclRefs(String authnContextDeclRefs) {
        getConfig().put(AUTHN_CONTEXT_DECL_REFS, authnContextDeclRefs);
    }

    public boolean isSignSpMetadata() {
        return Boolean.valueOf(getConfig().get(SIGN_SP_METADATA));
    }

    public void setSignSpMetadata(boolean signSpMetadata) {
        getConfig().put(SIGN_SP_METADATA, String.valueOf(signSpMetadata));
    }
    
    public boolean isAllowCreate() {
        return Boolean.valueOf(getConfig().get(ALLOW_CREATE));
    }

    public void setAllowCreated(boolean allowCreate) {
        getConfig().put(ALLOW_CREATE, String.valueOf(allowCreate));
    }

    public Integer getAttributeConsumingServiceIndex() {
        Integer result = null;
        String strAttributeConsumingServiceIndex = getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        if (strAttributeConsumingServiceIndex != null && !strAttributeConsumingServiceIndex.isEmpty()) {
            try {
                result = Integer.parseInt(strAttributeConsumingServiceIndex);
                if (result < 0) {
                    result = null;
                }
            } catch (NumberFormatException e) {
                // ignore it and use null
            }
        }
        return result;
    }

    public void setAttributeConsumingServiceIndex(Integer attributeConsumingServiceIndex) {
        if (attributeConsumingServiceIndex == null || attributeConsumingServiceIndex < 0) {
            getConfig().remove(ATTRIBUTE_CONSUMING_SERVICE_INDEX);
        } else {
            getConfig().put(ATTRIBUTE_CONSUMING_SERVICE_INDEX, String.valueOf(attributeConsumingServiceIndex));
        }
    }

    public void setAttributeConsumingServiceName(String attributeConsumingServiceName) {
        getConfig().put(ATTRIBUTE_CONSUMING_SERVICE_NAME, attributeConsumingServiceName);
    }

    public String getAttributeConsumingServiceName() {
        return getConfig().get(ATTRIBUTE_CONSUMING_SERVICE_NAME);
    }

    public String getOrganizationNames() {
        return getConfig().get(ORGANIZATION_NAMES);
    }

    public void setOrganizationNames(String organizationNames) {
        getConfig().put(ORGANIZATION_NAMES, organizationNames);
    }

    public String getOrganizationDisplayNames() {
        return getConfig().get(ORGANIZATION_DISPLAY_NAMES);
    }

    public void setOrganizationDisplayNames(String organizationDisplayNames) {
        getConfig().put(ORGANIZATION_DISPLAY_NAMES, organizationDisplayNames);
    }

    public String getOrganizationUrls() {
        return getConfig().get(ORGANIZATION_URLS);
    }

    public void setOrganizationUrls(String organizationUrls) {
        getConfig().put(ORGANIZATION_URLS, organizationUrls);
    }

    @Override
    public void validate(RealmModel realm) {
        SslRequired sslRequired = realm.getSslRequired();

        checkUrl(sslRequired, getSingleLogoutServiceUrl(), SINGLE_LOGOUT_SERVICE_URL);
        checkUrl(sslRequired, getSingleSignOnServiceUrl(), SINGLE_SIGN_ON_SERVICE_URL);
        //transient name id format is not accepted together with principaltype SubjectnameId
        if (JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get().equals(getNameIDPolicyFormat()) && SamlPrincipalType.SUBJECT == getPrincipalType())
            throw new IllegalArgumentException("Can not have Transient NameID Policy Format together with SUBJECT Principal Type");
        
    }

    public boolean isSpPrivate() {
        return Boolean.valueOf(getConfig().get(ADMINISTRATIVE_CONTACT_SP_PRIVATE));
    }

    public void setSpPrivate(boolean isPrivate) {
        getConfig().put(ADMINISTRATIVE_CONTACT_SP_PRIVATE, String.valueOf(isPrivate));
    }

    public String getIpaCode() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_IPA_CODE);
    }

    public void setIpaCode(String ipaCode) {
        getConfig().put(ADMINISTRATIVE_CONTACT_IPA_CODE, ipaCode);
    }

    public String getIpaCategory() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_IPA_CATEGORY);
    }

    public void setIpaCategory(String ipaCategory) {
        getConfig().put(ADMINISTRATIVE_CONTACT_IPA_CATEGORY, ipaCategory);
    }

    public String getAdministrativeContactVatNumber() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_VAT_NUMBER);
    }

    public void setAdministrativeContactVatNumber(String vatNumber) {
        getConfig().put(ADMINISTRATIVE_CONTACT_VAT_NUMBER, vatNumber);
    }

    public String getAdministrativeContactFiscalCode() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_FISCAL_CODE);
    }

    public void setAdministrativeContactFiscalCode(String fiscalCode) {
        getConfig().put(ADMINISTRATIVE_CONTACT_FISCAL_CODE, fiscalCode);
    }

    public String getAdministrativeContactNace2Codes() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_NACE2_CODES);
    }

    public void setAdministrativeContactNace2Codes(String nace2Codes) {
        getConfig().put(ADMINISTRATIVE_CONTACT_NACE2_CODES, nace2Codes);
    }

    public String getAdministrativeContactMunicipality() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_MUNICIPALITY);
    }

    public void setAdministrativeContactMunicipality(String municipality) {
        getConfig().put(ADMINISTRATIVE_CONTACT_MUNICIPALITY, municipality);
    }

    public String getAdministrativeContactProvince() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_PROVINCE);
    }

    public void setAdministrativeContactProvince(String province) {
        getConfig().put(ADMINISTRATIVE_CONTACT_PROVINCE, province);
    }

    public String getAdministrativeContactCountry() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_COUNTRY);
    }

    public void setAdministrativeContactCountry(String country) {
        getConfig().put(ADMINISTRATIVE_CONTACT_COUNTRY, country);
    }

    public String getAdministrativeContactEmail() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_EMAIL);
    }

    public void setAdministrativeContactEmail(String contactEmail) {
        getConfig().put(ADMINISTRATIVE_CONTACT_EMAIL, contactEmail);
    }

    public String getAdministrativeContactCompany() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_COMPANY);
    }

    public void setAdministrativeContactCompany(String contactCompany) {
        getConfig().put(ADMINISTRATIVE_CONTACT_COMPANY, contactCompany);
    }

    public String getAdministrativeContactPhone() {
        return getConfig().get(ADMINISTRATIVE_CONTACT_PHONE);
    }

    public void setAdministrativeContactPhone(String contactPhone) {
        getConfig().put(ADMINISTRATIVE_CONTACT_PHONE, contactPhone);
    }

    public String getTechnicalContactVatNumber() {
        return getConfig().get(TECHNICAL_CONTACT_VAT_NUMBER);
    }

    public void setTechnicalContactVatNumber(String vatNumber) {
        getConfig().put(TECHNICAL_CONTACT_VAT_NUMBER, vatNumber);
    }

    public String getTechnicalContactFiscalCode() {
        return getConfig().get(TECHNICAL_CONTACT_FISCAL_CODE);
    }

    public void setTechnicalContactFiscalCode(String fiscalCode) {
        getConfig().put(TECHNICAL_CONTACT_FISCAL_CODE, fiscalCode);
    }

    public String getTechnicalContactNace2Codes() {
        return getConfig().get(TECHNICAL_CONTACT_NACE2_CODES);
    }

    public void setTechnicalContactNace2Codes(String nace2Codes) {
        getConfig().put(TECHNICAL_CONTACT_NACE2_CODES, nace2Codes);
    }

    public String getTechnicalContactMunicipality() {
        return getConfig().get(TECHNICAL_CONTACT_MUNICIPALITY);
    }

    public void setTechnicalContactMunicipality(String municipality) {
        getConfig().put(TECHNICAL_CONTACT_MUNICIPALITY, municipality);
    }

    public String getTechnicalContactProvince() {
        return getConfig().get(TECHNICAL_CONTACT_PROVINCE);
    }

    public void setTechnicalContactProvince(String province) {
        getConfig().put(TECHNICAL_CONTACT_PROVINCE, province);
    }

    public String getTechnicalContactCountry() {
        return getConfig().get(TECHNICAL_CONTACT_COUNTRY);
    }

    public void setTechnicalContactCountry(String country) {
        getConfig().put(TECHNICAL_CONTACT_COUNTRY, country);
    }

    public String getTechnicalContactEmail() {
        return getConfig().get(TECHNICAL_CONTACT_EMAIL);
    }

    public void setTechnicalContactEmail(String contactEmail) {
        getConfig().put(TECHNICAL_CONTACT_EMAIL, contactEmail);
    }

    public String getTechnicalContactCompany() {
        return getConfig().get(TECHNICAL_CONTACT_COMPANY);
    }

    public void setTechnicalContactCompany(String contactCompany) {
        getConfig().put(TECHNICAL_CONTACT_COMPANY, contactCompany);
    }

    public String getTechnicalContactPhone() {
        return getConfig().get(TECHNICAL_CONTACT_PHONE);
    }

    public void setTechnicalContactPhone(String contactPhone) {
        getConfig().put(TECHNICAL_CONTACT_PHONE, contactPhone);
    }

    public void setRealmKeysProviderId(String realmKeysProviderId) {
        getConfig().put(REALM_KEYS_PROVIDER_ID, realmKeysProviderId);
    }

    public String getRealmKeysProviderId() {
        return getConfig().get(REALM_KEYS_PROVIDER_ID);
    }

}
