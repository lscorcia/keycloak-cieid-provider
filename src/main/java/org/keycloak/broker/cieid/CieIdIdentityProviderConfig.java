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

import java.util.List;

import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

public class CieIdIdentityProviderConfig extends SAMLIdentityProviderConfig {

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

    public CieIdIdentityProviderConfig(){
    }

    public CieIdIdentityProviderConfig(IdentityProviderModel identityProviderModel) {
        super(identityProviderModel);
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
    

}
