/*
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

package org.keycloak.broker.cieid.metadata;

import org.jboss.logging.Logger;

import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.ContactType;
import org.keycloak.dom.saml.v2.metadata.ContactTypeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.ExtensionsType;
import org.keycloak.dom.saml.v2.metadata.IndexedEndpointType;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.OrganizationType;
import org.keycloak.dom.saml.v2.metadata.SPSSODescriptorType;
import org.keycloak.models.KeyManager;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.common.util.StringUtil;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.services.resource.RealmResourceProvider;

import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyPair;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.keycloak.broker.cieid.CieIdIdentityProvider;
import org.keycloak.broker.cieid.CieIdIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderMapper;

public class CieIdSpMetadataResourceProvider implements RealmResourceProvider {
    protected static final Logger logger = Logger.getLogger(CieIdSpMetadataResourceProvider.class);

    public static final String XMLNS_NS = "http://www.w3.org/2000/xmlns/";
    public static final String CIEID_METADATA_EXTENSIONS_NS = "https://www.cartaidentita.interno.gov.it/saml-extensions";

    private KeycloakSession session;

    public CieIdSpMetadataResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Produces("text/xml; charset=utf-8")
    public Response get() {
        try
        {
            // Retrieve all enabled CIE ID Identity Providers for this realms
            RealmModel realm = session.getContext().getRealm();
            List<IdentityProviderModel> lstCieIdIdentityProviders = realm.getIdentityProvidersStream()
                .filter(t -> t.getProviderId().equals(CieIdIdentityProviderFactory.PROVIDER_ID) &&
                    t.isEnabled())
                .sorted((o1,o2)-> o1.getAlias().compareTo(o2.getAlias()))
                .collect(Collectors.toList());

            if (lstCieIdIdentityProviders.size() == 0)
                throw new Exception("No CIE ID providers found!");

            // Create an instance of the first CIE ID Identity Provider in alphabetical order
            CieIdIdentityProviderFactory providerFactory = new CieIdIdentityProviderFactory();
            CieIdIdentityProvider firstCieIdProvider = providerFactory.create(session, lstCieIdIdentityProviders.get(0));

            // Retrieve the context URI
            UriInfo uriInfo = session.getContext().getUri();

            //
            URI authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();

            if (firstCieIdProvider.getConfig().isPostBindingAuthnRequest()) {
                authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();
            }

            URI endpoint = uriInfo.getBaseUriBuilder()
                    .path("realms").path(realm.getName())
                    .path("broker")
                    .path(firstCieIdProvider.getConfig().getAlias())
                    .path("endpoint")
                    .build();

            boolean wantAuthnRequestsSigned = firstCieIdProvider.getConfig().isWantAuthnRequestsSigned();
            boolean wantAssertionsSigned = firstCieIdProvider.getConfig().isWantAssertionsSigned();
            boolean wantAssertionsEncrypted = firstCieIdProvider.getConfig().isWantAssertionsEncrypted();
            String configEntityId = firstCieIdProvider.getConfig().getEntityId();
            String entityId = getEntityId(configEntityId, uriInfo, realm);
            String nameIDPolicyFormat = firstCieIdProvider.getConfig().getNameIDPolicyFormat();
            int attributeConsumingServiceIndex = firstCieIdProvider.getConfig().getAttributeConsumingServiceIndex() != null ? firstCieIdProvider.getConfig().getAttributeConsumingServiceIndex(): 1;
            String attributeConsumingServiceName = firstCieIdProvider.getConfig().getAttributeConsumingServiceName();
            String[] attributeConsumingServiceNames = attributeConsumingServiceName != null ? attributeConsumingServiceName.split(","): null;

            List<Element> signingKeys = new LinkedList<>();
            List<Element> encryptionKeys = new LinkedList<>();

            session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256)
                    .filter(Objects::nonNull)
                    .filter(key -> key.getCertificate() != null)
                    .sorted(SamlService::compareKeys)
                    .forEach(key -> {
                        try {
                            Element element = SPMetadataDescriptor
                                    .buildKeyInfoElement(key.getKid(), PemUtils.encodeCertificate(key.getCertificate()));
                            signingKeys.add(element);

                            if (key.getStatus() == KeyStatus.ACTIVE) {
                                encryptionKeys.add(element);
                            }
                        } catch (ParserConfigurationException e) {
                            logger.warn("Failed to export SAML SP Metadata!", e);
                            throw new RuntimeException(e);
                        }
                    });

            // Prepare the metadata descriptor model
            StringWriter sw = new StringWriter();
            XMLStreamWriter writer = StaxUtil.getXMLStreamWriter(sw);
            SAMLMetadataWriter metadataWriter = new SAMLMetadataWriter(writer);

            EntityDescriptorType entityDescriptor = SPMetadataDescriptor.buildSPdescriptor(
                authnBinding, authnBinding, endpoint, endpoint,
                wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
                entityId, nameIDPolicyFormat, signingKeys, encryptionKeys);

            // Create the AttributeConsumingService
            AttributeConsumingServiceType attributeConsumingService = new AttributeConsumingServiceType(attributeConsumingServiceIndex);
            attributeConsumingService.setIsDefault(true);

            if (attributeConsumingServiceNames != null && attributeConsumingServiceNames.length > 0)
            {
                for (String attributeConsumingServiceNameStr: attributeConsumingServiceNames)
                {
                    String currentLocale = realm.getDefaultLocale() == null ? "en": realm.getDefaultLocale();

                    String[] parsedName = attributeConsumingServiceNameStr.split("\\|", 2);
                    String serviceNameLocale = parsedName.length >= 2 ? parsedName[0]: currentLocale;

                    LocalizedNameType attributeConsumingServiceNameElement = new LocalizedNameType(serviceNameLocale);
                    attributeConsumingServiceNameElement.setValue(parsedName[1]);
                    attributeConsumingService.addServiceName(attributeConsumingServiceNameElement);
                }
            }
    
            // Look for the SP descriptor and add the attribute consuming service
            for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();

                if (descriptors != null) {
                    for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                        if (descriptor.getSpDescriptor() != null) {
                            descriptor.getSpDescriptor().addAttributeConsumerService(attributeConsumingService);
                        }
                    }
                }
            }
            
            // Add the attribute mappers
            realm.getIdentityProviderMappersByAliasStream(firstCieIdProvider.getConfig().getAlias())
                .forEach(mapper -> {
                    IdentityProviderMapper target = (IdentityProviderMapper) session.getKeycloakSessionFactory().getProviderFactory(IdentityProviderMapper.class, mapper.getIdentityProviderMapper());
                    if (target instanceof SamlMetadataDescriptorUpdater)
                    {
                        SamlMetadataDescriptorUpdater metadataAttrProvider = (SamlMetadataDescriptorUpdater)target;
                        metadataAttrProvider.updateMetadata(mapper, entityDescriptor);
                    }
                });
				
			// Additional EntityDescriptor customizations
            String strOrganizationNames = firstCieIdProvider.getConfig().getOrganizationNames();
            String[] organizationNames = strOrganizationNames != null ? strOrganizationNames.split(","): null;

            String strOrganizationDisplayNames = firstCieIdProvider.getConfig().getOrganizationDisplayNames();
            String[] organizationDisplayNames = strOrganizationDisplayNames != null ? strOrganizationDisplayNames.split(","): null;

            String strOrganizationUrls = firstCieIdProvider.getConfig().getOrganizationUrls();
            String[] organizationUrls = strOrganizationUrls != null ? strOrganizationUrls.split(","): null;

            boolean isSpPrivate = firstCieIdProvider.getConfig().isSpPrivate();
            String ipaCode = firstCieIdProvider.getConfig().getIpaCode();
            String ipaCategory = firstCieIdProvider.getConfig().getIpaCategory();
            String administrativeContactCompany = firstCieIdProvider.getConfig().getAdministrativeContactCompany();
            String administrativeContactVatNumber = firstCieIdProvider.getConfig().getAdministrativeContactVatNumber();
            String administrativeContactFiscalCode = firstCieIdProvider.getConfig().getAdministrativeContactFiscalCode();
            String administrativeContactEmail = firstCieIdProvider.getConfig().getAdministrativeContactEmail();
            String administrativeContactPhone = firstCieIdProvider.getConfig().getAdministrativeContactPhone();
            String strAdministrativeContactNace2Codes = firstCieIdProvider.getConfig().getAdministrativeContactNace2Codes();
            String[] administrativeContactNace2Codes = strAdministrativeContactNace2Codes != null ? strAdministrativeContactNace2Codes.split(","): null;
            String administrativeContactMunicipality = firstCieIdProvider.getConfig().getAdministrativeContactMunicipality();
            String administrativeContactProvince = firstCieIdProvider.getConfig().getAdministrativeContactProvince();
            String administrativeContactCountry = firstCieIdProvider.getConfig().getAdministrativeContactCountry();
            String technicalContactCompany = firstCieIdProvider.getConfig().getTechnicalContactCompany();
            String technicalContactVatNumber = firstCieIdProvider.getConfig().getTechnicalContactVatNumber();
            String technicalContactFiscalCode = firstCieIdProvider.getConfig().getTechnicalContactFiscalCode();
            String technicalContactEmail = firstCieIdProvider.getConfig().getTechnicalContactEmail(); 
            String technicalContactPhone = firstCieIdProvider.getConfig().getTechnicalContactPhone();
            String strTechnicalContactNace2Codes = firstCieIdProvider.getConfig().getTechnicalContactNace2Codes();
            String[] technicalContactNace2Codes = strTechnicalContactNace2Codes!= null ? strTechnicalContactNace2Codes.split(","): null;
            String technicalContactMunicipality = firstCieIdProvider.getConfig().getTechnicalContactMunicipality();
            String technicalContactProvince = firstCieIdProvider.getConfig().getTechnicalContactProvince();
            String technicalContactCountry = firstCieIdProvider.getConfig().getTechnicalContactCountry();

			// Additional EntityDescriptor customizations
            customizeEntityDescriptor(entityDescriptor,
              organizationNames, organizationDisplayNames, organizationUrls,
              isSpPrivate, ipaCode, ipaCategory,
              administrativeContactCompany, administrativeContactVatNumber, administrativeContactFiscalCode,
              administrativeContactEmail, administrativeContactPhone, administrativeContactNace2Codes,
              administrativeContactMunicipality, administrativeContactProvince, administrativeContactCountry,
              technicalContactCompany, technicalContactVatNumber, technicalContactFiscalCode, 
              technicalContactEmail, technicalContactPhone, technicalContactNace2Codes,
              technicalContactMunicipality, technicalContactProvince, technicalContactCountry);

            // Additional SPSSODescriptor customizations
            List<URI> assertionEndpoints = lstCieIdIdentityProviders.stream()
                    .map(t -> uriInfo.getBaseUriBuilder()
                        .path("realms").path(realm.getName())
                        .path("broker")
                        .path(t.getAlias())
                        .path("endpoint")
                    .build()).collect(Collectors.toList());

            List<URI> logoutEndpoints = lstCieIdIdentityProviders.stream()
                .map(t -> uriInfo.getBaseUriBuilder()
                    .path("realms").path(realm.getName())
                    .path("broker")
                    .path(t.getAlias())
                    .path("endpoint")
                    .build()).collect(Collectors.toList());

            for (EntityDescriptorType.EDTChoiceType choiceType: entityDescriptor.getChoiceType()) {
                List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = choiceType.getDescriptors();
    
                if (descriptors != null) {
                    for (EntityDescriptorType.EDTDescriptorChoiceType descriptor: descriptors) {
                        SPSSODescriptorType spDescriptor = descriptor.getSpDescriptor();
                        
                        if (spDescriptor != null) {
                            customizeSpDescriptor(spDescriptor,
                                authnBinding, authnBinding,
                                assertionEndpoints, logoutEndpoints);
                        }
                    }
                }
            }

            // Write the metadata and export it to a string
            metadataWriter.writeEntityDescriptor(entityDescriptor);

            String descriptor = sw.toString();

            // Metadata signing
            if (firstCieIdProvider.getConfig().isSignSpMetadata()) {
                KeyManager.ActiveRsaKey activeKey = session.keys().getActiveRsaKey(realm);
                String keyName = firstCieIdProvider.getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(activeKey.getKid(), activeKey.getCertificate());
                KeyPair keyPair = new KeyPair(activeKey.getPublicKey(), activeKey.getPrivateKey());

                Document metadataDocument = DocumentUtil.getDocument(descriptor);
                SAML2Signature signatureHelper = new SAML2Signature();
                signatureHelper.setSignatureMethod(firstCieIdProvider.getSignatureAlgorithm().getXmlSignatureMethod());
                signatureHelper.setDigestMethod(firstCieIdProvider.getSignatureAlgorithm().getXmlSignatureDigestMethod());

                Node nextSibling = metadataDocument.getDocumentElement().getFirstChild();
                signatureHelper.setNextSibling(nextSibling);

                signatureHelper.signSAMLDocument(metadataDocument, keyName, keyPair, CanonicalizationMethod.EXCLUSIVE);

                descriptor = DocumentUtil.getDocumentAsString(metadataDocument);
            }

            return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
        } catch (Exception e) {
            logger.warn("Failed to export SAML SP Metadata!", e);
            throw new RuntimeException(e);
        }
    }

    private String getEntityId(String configEntityId, UriInfo uriInfo, RealmModel realm) {
        if (configEntityId == null || configEntityId.isEmpty())
            return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
        else
            return configEntityId;
    }

    private static void customizeEntityDescriptor(EntityDescriptorType entityDescriptor,
		String[] organizationNames, String[] organizationDisplayNames, String[] organizationUrls,
        boolean isSpPrivate, String ipaCode, String ipaCategory,
        String administrativeContactCompany, String administrativeContactVatNumber, String administrativeContactFiscalCode,
        String administrativeContactEmail, String administrativeContactPhone, String[] administrativeContactNace2Codes,
        String administrativeContactMunicipality, String administrativeContactProvince, String administrativeContactCountry,
        String technicalContactCompany, String technicalContactVatNumber, String technicalContactFiscalCode,
        String technicalContactEmail, String technicalContactPhone, String[] technicalContactNace2Codes,
        String technicalContactMunicipality, String technicalContactProvince, String technicalContactCountry) 
        throws ConfigurationException
    {
        // Organization
        if (organizationNames != null && organizationNames.length > 0 ||
            organizationDisplayNames != null && organizationDisplayNames.length > 0 ||
            organizationUrls != null && organizationUrls.length > 0)
        {
            OrganizationType organizationType = new OrganizationType();

            if (organizationNames != null) {
                for (String organizationNameStr: organizationNames)
                {
                    String[] parsedName = organizationNameStr.split("\\|", 2);
                    if (parsedName.length < 2) continue;

                    LocalizedNameType organizationName = new LocalizedNameType(parsedName[0].trim());
                    organizationName.setValue(parsedName[1].trim());
                    organizationType.addOrganizationName(organizationName);
                }
            }

            if (organizationDisplayNames != null) {
                for (String organizationDisplayNameStr: organizationDisplayNames)
                {
                    String[] parsedDisplayName = organizationDisplayNameStr.split("\\|", 2);
                    if (parsedDisplayName.length < 2) continue;

                    LocalizedNameType organizationDisplayName = new LocalizedNameType(parsedDisplayName[0].trim());
                    organizationDisplayName.setValue(parsedDisplayName[1].trim());
                    organizationType.addOrganizationDisplayName(organizationDisplayName);
                }
            }

            if (organizationUrls != null) {
                for (String organizationUrlStr: organizationUrls)
                {
                    String[] parsedUrl = organizationUrlStr.split("\\|", 2);
                    if (parsedUrl.length < 2) continue;

                    LocalizedURIType organizationUrl = new LocalizedURIType(parsedUrl[0].trim());
                    try {
                        organizationUrl.setValue(new URI(parsedUrl[1].trim()));
                    } catch (URISyntaxException e) { logger.error("Error creating URI for Organization URL"); continue; };
                    organizationType.addOrganizationURL(organizationUrl);
                }
            }

            // ContactPerson type=ADMINISTRATIVE
            if (!StringUtil.isNullOrEmpty(administrativeContactCompany) || 
                !StringUtil.isNullOrEmpty(administrativeContactEmail) || 
                !StringUtil.isNullOrEmpty(administrativeContactPhone)) 
            {
                ContactType administrativeContactPerson = new ContactType(ContactTypeType.ADMINISTRATIVE);

                if (!StringUtil.isNullOrEmpty(administrativeContactCompany)) administrativeContactPerson.setCompany(administrativeContactCompany);
                if (!StringUtil.isNullOrEmpty(administrativeContactEmail)) administrativeContactPerson.addEmailAddress(administrativeContactEmail);
                if (!StringUtil.isNullOrEmpty(administrativeContactPhone)) administrativeContactPerson.addTelephone(administrativeContactPhone);

                // Extensions
                if (administrativeContactPerson.getExtensions() == null)
                administrativeContactPerson.setExtensions(new ExtensionsType());

                Document doc = DocumentUtil.createDocument();

                if (!isSpPrivate)
                {
                    // Public SP Extensions
						
                    // Public qualifier
                    Element spTypeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Public");
                    spTypeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    administrativeContactPerson.getExtensions().addExtension(spTypeElement);

                    // IPA Code
                    if (!StringUtil.isNullOrEmpty(ipaCode))
                    {
                        Element ipaCodeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:IPACode");
                        ipaCodeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                        ipaCodeElement.setTextContent(ipaCode);
                        administrativeContactPerson.getExtensions().addExtension(ipaCodeElement);
                    }

                    // IPA Category
                    if (!StringUtil.isNullOrEmpty(ipaCategory))
                    {
                        Element ipaCategoryElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:IPACategory");
                        ipaCategoryElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                        ipaCategoryElement.setTextContent(ipaCategory);
                        administrativeContactPerson.getExtensions().addExtension(ipaCategoryElement);
                    }
                }
                else
                {
                    // Private SP Extensions
					
                    // Private qualifier
                    Element spTypeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Private");
                    spTypeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    administrativeContactPerson.getExtensions().addExtension(spTypeElement);
                }

                // VAT Number
                if (!StringUtil.isNullOrEmpty(administrativeContactVatNumber))
                {
                    Element vatNumberElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:VATNumber");
                    vatNumberElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    vatNumberElement.setTextContent(administrativeContactVatNumber);
                    administrativeContactPerson.getExtensions().addExtension(vatNumberElement);
                }

                // Fiscal Code	
                if (!StringUtil.isNullOrEmpty(administrativeContactFiscalCode))
                {
                    Element fiscalCodeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:FiscalCode");
                    fiscalCodeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    fiscalCodeElement.setTextContent(administrativeContactFiscalCode);
                    administrativeContactPerson.getExtensions().addExtension(fiscalCodeElement);
                }

                // NACE2 Codes
                if (administrativeContactNace2Codes != null && administrativeContactNace2Codes.length > 0)
                {
                    for (String naceCode : administrativeContactNace2Codes)
                    {
                        Element naceCodeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:NACE2Code");
                        naceCodeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                        naceCodeElement.setTextContent(naceCode);
                        administrativeContactPerson.getExtensions().addExtension(naceCodeElement);
                    }
                }

                // Municipality
                if (!StringUtil.isNullOrEmpty(administrativeContactMunicipality))
                {
                    Element municipalityElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Municipality");
                    municipalityElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    municipalityElement.setTextContent(administrativeContactMunicipality);
                    administrativeContactPerson.getExtensions().addExtension(municipalityElement);
                }

                // Province	
                if (!StringUtil.isNullOrEmpty(administrativeContactProvince))
                {
                    Element provinceElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Province");
                    provinceElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    provinceElement.setTextContent(administrativeContactProvince);
                    administrativeContactPerson.getExtensions().addExtension(provinceElement);
                }

                // Country
                if (!StringUtil.isNullOrEmpty(administrativeContactCountry))
                {
                    Element countryElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Country");
                    countryElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    countryElement.setTextContent(administrativeContactCountry);
                    administrativeContactPerson.getExtensions().addExtension(countryElement);
                }

                entityDescriptor.addContactPerson(administrativeContactPerson);
            }

            // ContactPerson type=TECHNICAL
            if (!StringUtil.isNullOrEmpty(technicalContactCompany) || 
                !StringUtil.isNullOrEmpty(technicalContactEmail) || 
                !StringUtil.isNullOrEmpty(technicalContactPhone)) {
                ContactType technicalContactPerson = new ContactType(ContactTypeType.TECHNICAL);

                if (!StringUtil.isNullOrEmpty(technicalContactCompany)) technicalContactPerson.setCompany(technicalContactCompany);
                if (!StringUtil.isNullOrEmpty(technicalContactEmail)) technicalContactPerson.addEmailAddress(technicalContactEmail);
                if (!StringUtil.isNullOrEmpty(technicalContactPhone)) technicalContactPerson.addTelephone(technicalContactPhone);

                // Extensions
                if (technicalContactPerson.getExtensions() == null)
                technicalContactPerson.setExtensions(new ExtensionsType());

                Document doc = DocumentUtil.createDocument();

                // VAT Number
                if (!StringUtil.isNullOrEmpty(technicalContactVatNumber))
                {
                    Element vatNumberElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:VATNumber");
                    vatNumberElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    vatNumberElement.setTextContent(technicalContactVatNumber);
                    technicalContactPerson.getExtensions().addExtension(vatNumberElement);
                }

                // Fiscal Code	
                if (!StringUtil.isNullOrEmpty(technicalContactFiscalCode))
                {
                    Element fiscalCodeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:FiscalCode");
                    fiscalCodeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    fiscalCodeElement.setTextContent(technicalContactFiscalCode);
                    technicalContactPerson.getExtensions().addExtension(fiscalCodeElement);
                }

                // NACE2 Codes
                if (technicalContactNace2Codes != null && technicalContactNace2Codes.length > 0)
                {
                    for (String naceCode : technicalContactNace2Codes)
                    {
                        Element naceCodeElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:NACE2Code");
                        naceCodeElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                        naceCodeElement.setTextContent(naceCode);
                        technicalContactPerson.getExtensions().addExtension(naceCodeElement);
                    }
                }

                // Municipality
                if (!StringUtil.isNullOrEmpty(technicalContactMunicipality))
                {
                    Element municipalityElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Municipality");
                    municipalityElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    municipalityElement.setTextContent(technicalContactMunicipality);
                    technicalContactPerson.getExtensions().addExtension(municipalityElement);
                }

                // Province	
                if (!StringUtil.isNullOrEmpty(technicalContactProvince))
                {
                    Element provinceElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Province");
                    provinceElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    provinceElement.setTextContent(technicalContactProvince);
                    technicalContactPerson.getExtensions().addExtension(provinceElement);
                }

                // Country
                if (!StringUtil.isNullOrEmpty(technicalContactCountry))
                {
                    Element countryElement = doc.createElementNS(CIEID_METADATA_EXTENSIONS_NS, "cie:Country");
                    countryElement.setAttributeNS(XMLNS_NS, "xmlns:cie", CIEID_METADATA_EXTENSIONS_NS);
                    countryElement.setTextContent(technicalContactCountry);
                    technicalContactPerson.getExtensions().addExtension(countryElement);
                }

                entityDescriptor.addContactPerson(technicalContactPerson);
            }

            entityDescriptor.setOrganization(organizationType);
        }
    }

    private static void customizeSpDescriptor(SPSSODescriptorType spDescriptor,
        URI loginBinding, URI logoutBinding, 
        List<URI> assertionEndpoints, List<URI> logoutEndpoints)
    {
        // Remove any existing SingleLogoutService endpoints
        List<EndpointType> lstSingleLogoutService = spDescriptor.getSingleLogoutService();
        for (int i = lstSingleLogoutService.size() - 1; i >= 0; --i)
            spDescriptor.removeSingleLogoutService(lstSingleLogoutService.get(i));

        // Add the new SingleLogoutService endpoints
        for (URI logoutEndpoint: logoutEndpoints)
            spDescriptor.addSingleLogoutService(new EndpointType(logoutBinding, logoutEndpoint));

        // Remove any existing AssertionConsumerService endpoints
        List<IndexedEndpointType> lstAssertionConsumerService = spDescriptor.getAssertionConsumerService();
        for (int i = lstAssertionConsumerService.size() - 1; i >= 0; --i)
            spDescriptor.removeAssertionConsumerService(lstAssertionConsumerService.get(i));

        // Add the new AssertionConsumerService endpoints
        int assertionEndpointIndex = 0;
        for (URI assertionEndpoint: assertionEndpoints)
        {
            IndexedEndpointType assertionConsumerEndpoint = new IndexedEndpointType(loginBinding, assertionEndpoint);
            if (assertionEndpointIndex == 0) assertionConsumerEndpoint.setIsDefault(true);
            assertionConsumerEndpoint.setIndex(assertionEndpointIndex);

            spDescriptor.addAssertionConsumerService(assertionConsumerEndpoint);
            assertionEndpointIndex++;
        }
    }

    @Override
    public void close() {
    }

}
