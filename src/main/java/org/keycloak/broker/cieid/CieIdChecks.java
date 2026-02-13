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

import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Pattern;

/**
 * CIE ID-specific response validation checks.
 * Contains validation logic for CIE ID SAML responses according to CIE ID technical rules.
 */
public class CieIdChecks {
    private static final Logger logger = Logger.getLogger(CieIdChecks.class);

    // ISO 8601 fully compliant regex for date/time validation
    private static final String _UTC_STRING = "^(-?(?:[1-9][0-9]*)?[0-9]{4})-(1[0-2]|0[1-9])-(3[01]|0[1-9]|[12][0-9])T(2[0-3]|[01][0-9]):([0-5][0-9]):([0-5][0-9])(\\.[0-9]+)?(Z|[+-](?:2[0-3]|[01][0-9]):[0-5][0-9])?$";

    // CIE ID authentication levels
    private static final String[] CIEID_LEVEL = {
        "https://www.spid.gov.it/SpidL1",
        "https://www.spid.gov.it/SpidL2",
        "https://www.spid.gov.it/SpidL3"
    };

    private final CieIdIdentityProviderConfig config;

    public CieIdChecks(CieIdIdentityProviderConfig config) {
        this.config = config;
    }

    /**
     * Orchestrates CIE ID response validation by extracting parameters from the session
     * and validating the SAML response and assertion.
     * Returns null if validation passes, or an error code string if validation fails.
     *
     * @param authSession the authentication session containing request metadata
     * @param holder the SAML document holder
     * @param assertionElement the assertion element (decrypted if necessary)
     * @return null if valid, error code string if validation fails
     */
    public String validateCieIdResponse(AuthenticationSessionModel authSession,
                                       SAMLDocumentHolder holder,
                                       Element assertionElement) {
        // Extract parameters from authentication session
        String expectedRequestId = authSession.getClientNote(SamlProtocol.SAML_REQUEST_ID_BROKER);
        String requestIssueInstant = authSession.getClientNote(CieIdIdentityProvider.CIEID_REQUEST_ISSUE_INSTANT);
        String idpEntityId = config.getIdpEntityId();

        // Perform comprehensive CIE ID validation
        return verifyCieIdResponse(
            holder.getSamlDocument().getDocumentElement(),
            assertionElement,
            expectedRequestId,
            requestIssueInstant,
            idpEntityId
        );
    }

    /**
     * Performs comprehensive CIE ID response validation according to CIE ID technical rules.
     * Returns null if validation passes, or an error code string if validation fails.
     */
    public String verifyCieIdResponse(Element documentElement, Element assertionElement,
                                     String expectedRequestId, String requestIssueInstant, String idpEntityId) {
        // 08: Response > ID empty
        String responseIDToValue = documentElement.getAttribute("ID");
        if (responseIDToValue.isEmpty()) {
            return "CieIdSamlCheck_nr08";
        }

        // 13: Response > IssueInstant invalid format
        String responseIssueInstantToValue = documentElement.getAttribute("IssueInstant");
        if (!responseIssueInstantToValue.isEmpty()) {
            Pattern utcPattern = Pattern.compile(_UTC_STRING);
            if (!utcPattern.matcher(responseIssueInstantToValue).find()) {
                return "CieIdSamlCheck_nr13";
            }
        }

        try {
            // 14: IssueInstant attribute prior to IssueInstant of the request
            XMLGregorianCalendar requestIssueInstantTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(requestIssueInstant);
            XMLGregorianCalendar responseIssueInstantTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(responseIssueInstantToValue);
            if (responseIssueInstantTime.compare(requestIssueInstantTime) == DatatypeConstants.LESSER) {
                return "CieIdSamlCheck_nr14";
            }
            // 15: IssueInstant attribute following the instant of receipt
            XMLGregorianCalendar requestFutureIssueInstantTime = (XMLGregorianCalendar) requestIssueInstantTime.clone();
            requestFutureIssueInstantTime.add(DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 0, 3, 0));
            if (responseIssueInstantTime.compare(requestFutureIssueInstantTime) == DatatypeConstants.GREATER) {
                return "CieIdSamlCheck_nr15";
            }
        } catch (DatatypeConfigurationException e) {
            logger.error(e);
            return "CieIdSamlCheck_nr14";
        }

        Element issuerElement = getDirectChild(documentElement, "Issuer");

        // 28: Missing Issuer element
        if (issuerElement == null) {
            return "CieIdSamlCheck_nr28";
        }

        // 27: Issuer element is empty
        if (!issuerElement.hasChildNodes() ||
            !org.keycloak.saml.common.util.StringUtil.isNotNull(issuerElement.getFirstChild().getNodeValue()) ||
            hasNamedChild(issuerElement)) {
            return "CieIdSamlCheck_nr27";
        }

        // 29: Issuer element different from EntityID IdP
        if (!issuerElement.getFirstChild().getNodeValue().equals(idpEntityId)) {
            return "CieIdSamlCheck_nr29";
        }

        // 30: Issuer Format attribute must be omitted or take value urn:oasis:names:tc:SAML:2.0:nameid-format:entity
        if (issuerElement.hasAttribute("Format")) {
            if (!issuerElement.getAttribute("Format").equals(JBossSAMLURIConstants.NAMEID_FORMAT_ENTITY.get())) {
                return "CieIdSamlCheck_nr30";
            }
        }

        // 33: Assertion ID attribute is empty
        String responseAssertionIDToValue = assertionElement.getAttribute("ID");
        if (responseAssertionIDToValue.isEmpty()) {
            return "CieIdSamlCheck_nr33";
        }

        String responseAssertionIssueInstantToValue = assertionElement.getAttribute("IssueInstant");
        try {
            // 39: IssueInstant attribute of the Assertion prior to the IssueInstant of the Request
            XMLGregorianCalendar requestIssueInstantTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(requestIssueInstant);
            XMLGregorianCalendar assertionIssueInstantTime = DatatypeFactory.newInstance().newXMLGregorianCalendar(responseAssertionIssueInstantToValue);
            if (assertionIssueInstantTime.compare(requestIssueInstantTime) == DatatypeConstants.LESSER) {
                return "CieIdSamlCheck_nr39";
            }
            // 40: IssueInstant attribute of the Assertion following the IssueInstant of the Request
            XMLGregorianCalendar requestFutureIssueInstantTime = (XMLGregorianCalendar) requestIssueInstantTime.clone();
            requestFutureIssueInstantTime.add(DatatypeFactory.newInstance().newDuration(true, 0, 0, 0, 0, 3, 0));
            if (assertionIssueInstantTime.compare(requestFutureIssueInstantTime) == DatatypeConstants.GREATER) {
                return "CieIdSamlCheck_nr40";
            }
        } catch (DatatypeConfigurationException e) {
            logger.error(e);
            return "CieIdSamlCheck_nr39";
        }

        Element subjectElement = getDirectChild(assertionElement, "Subject");

        // 42: Assertion > Subject missing
        if (subjectElement == null) {
            return "CieIdSamlCheck_nr42";
        }

        // 41: Assertion > Subject element is empty
        if (!hasNamedChild(subjectElement)) {
            return "CieIdSamlCheck_nr41";
        }

        Element nameIdElement = getDirectChild(subjectElement, "NameID");

        // 44: Missing Assertion NameID element
        if (nameIdElement == null) {
            return "CieIdSamlCheck_nr44";
        }

        // 43: NameID element of the Assertion is empty
        if (!nameIdElement.hasChildNodes() ||
            !org.keycloak.saml.common.util.StringUtil.isNotNull(nameIdElement.getFirstChild().getNodeValue()) ||
            hasNamedChild(nameIdElement)) {
            return "CieIdSamlCheck_nr43";
        }

        if (nameIdElement.hasAttribute("Format")) {
            // 45: Format attribute of the NameID element of the Assertion is empty
            if (nameIdElement.getAttribute("Format").isEmpty()) {
                return "CieIdSamlCheck_nr45";
            }
            // 47: Assertion NameID Format attribute other than urn:oasis:names:tc:SAML:2.0:nameid-format:transient
            if (!nameIdElement.getAttribute("Format").equals(JBossSAMLURIConstants.NAMEID_FORMAT_TRANSIENT.get())) {
                return "CieIdSamlCheck_nr47";
            }
        } else {
            // 46: Missing Assertion NameID Element Format attribute
            return "CieIdSamlCheck_nr46";
        }

        // 49: NameQualifier attribute of NameID of Assertion is missing
        if (!nameIdElement.hasAttribute("NameQualifier")) {
            return "CieIdSamlCheck_nr49";
        }

        // 48: NameQualifier attribute of NameID of the Assertion is empty
        if (nameIdElement.getAttribute("NameQualifier").isEmpty()) {
            return "CieIdSamlCheck_nr48";
        }

        // 17: Response > InResponseTo missing
        if (!documentElement.hasAttribute("InResponseTo")) {
            return "CieIdSamlCheck_nr17";
        }

        // 16: Response > InResponseTo empty
        String responseInResponseToValue = documentElement.getAttribute("InResponseTo");
        if (responseInResponseToValue.isEmpty()) {
            return "CieIdSamlCheck_nr16";
        }

        // 18: Response > InResponseTo does not match request ID
        if (!responseInResponseToValue.equals(expectedRequestId)) {
            return "CieIdSamlCheck_nr18";
        }

        // 52: Assertion > Subject > Confirmation missing
        Element subjectConfirmationElement = getDirectChild(subjectElement, "SubjectConfirmation");
        if (subjectConfirmationElement == null) {
            return "CieIdSamlCheck_nr52";
        }

        // 51: Assertion > Subject > Confirmation empty
        if (!hasNamedChild(subjectConfirmationElement)) {
            return "CieIdSamlCheck_nr51";
        }

        // 54: Assertion > Subject > Confirmation > Method missing
        if (!subjectConfirmationElement.hasAttribute("Method")) {
            return "CieIdSamlCheck_nr54";
        }

        // 53: Assertion > Subject > Confirmation > Method empty
        String subjectConfirmationMethodValue = subjectConfirmationElement.getAttribute("Method");
        if (subjectConfirmationMethodValue.isEmpty()) {
            return "CieIdSamlCheck_nr53";
        }

        // 55: Assertion > Subject > Confirmation > Method is not SUBJECT_CONFIRMATION_BEARER
        if (!subjectConfirmationMethodValue.equals(JBossSAMLURIConstants.SUBJECT_CONFIRMATION_BEARER.get())) {
            return "CieIdSamlCheck_nr55";
        }

        Element subjectConfirmationDataElement = getDirectChild(subjectConfirmationElement, "SubjectConfirmationData");

        // 56: Assertion > Subject > Confirmation > SubjectConfirmationData missing
        if (subjectConfirmationDataElement == null) {
            return "CieIdSamlCheck_nr56";
        }

        // 58: Assertion > Subject > Confirmation > SubjectConfirmationData > Recipient missing
        if (!subjectConfirmationDataElement.hasAttribute("Recipient")) {
            return "CieIdSamlCheck_nr58";
        }

        // 57: Assertion > Subject > Confirmation > SubjectConfirmationData > Recipient is empty
        String subjectConfirmationDataRecipientValue = subjectConfirmationDataElement.getAttribute("Recipient");
        if (subjectConfirmationDataRecipientValue.isEmpty()) {
            return "CieIdSamlCheck_nr57";
        }

        // 59: Recipient does not match Destination
        if (!subjectConfirmationDataRecipientValue.equals(documentElement.getAttribute("Destination"))) {
            return "CieIdSamlCheck_nr59";
        }

        // 61: Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo missing
        if (!subjectConfirmationDataElement.hasAttribute("InResponseTo")) {
            return "CieIdSamlCheck_nr61";
        }

        // 60: Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo is empty
        String subjectConfirmationDataInResponseToValue = subjectConfirmationDataElement.getAttribute("InResponseTo");
        if (subjectConfirmationDataInResponseToValue.isEmpty()) {
            return "CieIdSamlCheck_nr60";
        }

        // 62: Assertion > Subject > Confirmation > SubjectConfirmationData > InResponseTo does not match request ID
        if (!subjectConfirmationDataInResponseToValue.equals(expectedRequestId)) {
            return "CieIdSamlCheck_nr62";
        }

        // 64: NotOnOrAfter attribute of SubjectConfirmationData is missing
        if (!subjectConfirmationDataElement.hasAttribute("NotOnOrAfter")) {
            return "CieIdSamlCheck_nr64";
        }

        try {
            // 66: NotOnOrAfter attribute of SubjectConfirmationData prior to the time the response was received
            XMLGregorianCalendar notOnOrAfterTime = DatatypeFactory.newInstance()
                .newXMLGregorianCalendar(subjectConfirmationDataElement.getAttribute("NotOnOrAfter"));
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            if (notOnOrAfterTime.compare(now) == DatatypeConstants.LESSER) {
                return "CieIdSamlCheck_nr66";
            }
        } catch (DatatypeConfigurationException e) {
            logger.error(e);
            return "CieIdSamlCheck_nr66";
        }

        Element assertionIssuerElement = getDirectChild(assertionElement, "Issuer");

        // 68: Missing Issuer element of the Assertion
        if (assertionIssuerElement == null) {
            return "CieIdSamlCheck_nr68";
        }

        // 67: Issuer element of the Assertion is empty
        if (!assertionIssuerElement.hasChildNodes() ||
            !org.keycloak.saml.common.util.StringUtil.isNotNull(assertionIssuerElement.getFirstChild().getNodeValue()) ||
            hasNamedChild(assertionIssuerElement)) {
            return "CieIdSamlCheck_nr67";
        }

        // 69: Issuer element of the Assertion different from EntityID IdP
        if (!assertionIssuerElement.getFirstChild().getNodeValue().equals(idpEntityId)) {
            return "CieIdSamlCheck_nr69";
        }

        // not required for CIEID
        // 70: Format attribute of Issuer of the Assertion is empty (SPID check nr70)
        // 71: Missing Assertion Issuer Format attribute (SPID check nr71)
        // 72: Format attribute of Issuer of the Assertion, if present, must have value urn:oasis:names:tc:SAML:2.0:nameid-format:entity (SPID check nr72)
        if (assertionIssuerElement.hasAttribute("Format")) {
            if (!assertionIssuerElement.getAttribute("Format").equals(JBossSAMLURIConstants.NAMEID_FORMAT_ENTITY.get())) {
                return "CieIdSamlCheck_nr72";
            }
        }

        Element conditionsElement = getDirectChild(assertionElement, "Conditions");

        // 74: Missing Assertion Conditions element
        if (conditionsElement == null) {
            return "CieIdSamlCheck_nr74";
        }

        // 73: Conditions element of the Assertion is empty
        if (!hasNamedChild(conditionsElement)) {
            return "CieIdSamlCheck_nr73";
        }

        // 76: Missing Assertion Condition NotBefore attribute
        if (!conditionsElement.hasAttribute("NotBefore")) {
            return "CieIdSamlCheck_nr76";
        }

        // 80: Missing Assertion Condition NotOnOrAfter attribute
        if (!conditionsElement.hasAttribute("NotOnOrAfter")) {
            return "CieIdSamlCheck_nr80";
        }

        Element audienceRestrictionElement = getDirectChild(conditionsElement, "AudienceRestriction");

        // 84: Missing Assertion Condition AudienceRestriction element
        if (audienceRestrictionElement == null) {
            return "CieIdSamlCheck_nr84";
        }

        Element authnStatementElement = getDirectChild(assertionElement, "AuthnStatement");

        // 89: Missing AuthStatement element of the Assertion
        if (authnStatementElement == null) {
            return "CieIdSamlCheck_nr89";
        }

        // 88: AuthStatement element of the Assertion is empty
        if (!hasNamedChild(authnStatementElement)) {
            return "CieIdSamlCheck_nr88";
        }

        Element authnContextElement = getDirectChild(authnStatementElement, "AuthnContext");

        // 91: Missing AuthStatement AuthnContext Element of Assertion
        if (authnContextElement == null) {
            return "CieIdSamlCheck_nr91";
        }

        // 90: AuthnContext of AuthStatement of Assertion is empty
        if (!hasNamedChild(authnContextElement)) {
            return "CieIdSamlCheck_nr90";
        }

        Element authnContextClassRef = getDirectChild(authnContextElement, "AuthnContextClassRef");

        // 93: AuthStatement AuthContextClassRef Element of the Missing Assertion
        if (authnContextClassRef == null) {
            return "CieIdSamlCheck_nr93";
        }

        // 92: AuthStatement AuthContextClassRef Element of the Assertion is empty
        if (!authnContextClassRef.hasChildNodes() ||
            !org.keycloak.saml.common.util.StringUtil.isNotNull(authnContextClassRef.getFirstChild().getNodeValue()) ||
            hasNamedChild(authnContextClassRef)) {
            return "CieIdSamlCheck_nr92";
        }

        // 97: AuthContextClassRef element set to an unexpected value
        String responseCieIdLevel = authnContextClassRef.getFirstChild().getNodeValue();
        int cieIdLevelResponse = Arrays.asList(CIEID_LEVEL).indexOf(responseCieIdLevel) + 1;

        List<String> cieIdLevelRequestList = null;
        try {
            cieIdLevelRequestList = Arrays.asList(JsonSerialization.readValue(config.getAuthnContextClassRefs(), String[].class));
        } catch (Exception e) {
            logger.error("Could not json-deserialize AuthContextClassRefs config entry: " + config.getAuthnContextClassRefs(), e);
            return "CieIdSamlCheck_nr97";
        }
        int cieIdLevelRequest = Arrays.asList(CIEID_LEVEL).indexOf(cieIdLevelRequestList.get(0)) + 1;

        if (cieIdLevelResponse < 1) {
            return "CieIdSamlCheck_nr97";
        }

        // 94-96: AuthContextClassRef element set to wrong CIE ID level
        if (config.getAuthnContextComparisonType().equals(AuthnContextComparisonType.EXACT)) {
            if (cieIdLevelResponse != cieIdLevelRequest) {
                return getCieIdLevelAssertion(cieIdLevelResponse);
            }
        } else if (config.getAuthnContextComparisonType().equals(AuthnContextComparisonType.MINIMUM)) {
            if (cieIdLevelResponse < cieIdLevelRequest) {
                return getCieIdLevelAssertion(cieIdLevelResponse);
            }
        } else if (config.getAuthnContextComparisonType().equals(AuthnContextComparisonType.MAXIMUM)) {
            if (cieIdLevelResponse > cieIdLevelRequest) {
                return getCieIdLevelAssertion(cieIdLevelResponse);
            }
        } else if (config.getAuthnContextComparisonType().equals(AuthnContextComparisonType.BETTER)) {
            if (!responseCieIdLevel.equals(config.getAuthnContextClassRefs())) {
                return getCieIdLevelAssertion(cieIdLevelResponse);
            }
        }

        return null;
    }

    /**
     * Checks if a SAML response contains a CIE ID fault error code.
     * CIE ID faults are indicated by status messages starting with "ErrorCode nr".
     *
     * @param responseType the SAML response to check
     * @return true if the response contains a CIE ID fault, false otherwise
     */
    public boolean isCieIdFault(StatusResponseType responseType) {
        return responseType.getStatus() != null
            && responseType.getStatus().getStatusMessage() != null
            && responseType.getStatus().getStatusMessage().startsWith("ErrorCode nr");
    }

    /**
     * Formats a CIE ID fault status message for error page display.
     * Converts "ErrorCode nr XX" to "CieIdFault_ErrorCode_nr_XX" format.
     *
     * @param statusMessage the raw status message from the CIE ID response
     * @return formatted error message suitable for error page display
     */
    public String formatCieIdFaultMessage(String statusMessage) {
        return "CieIdFault_" + statusMessage.replace(' ', '_');
    }

    private String getCieIdLevelAssertion(int cieIdLevel) {
        switch (cieIdLevel) {
            case 1:
                return "CieIdSamlCheck_nr94";
            case 2:
                return "CieIdSamlCheck_nr95";
            case 3:
                return "CieIdSamlCheck_nr96";
            default:
                return "CieIdSamlCheck_nr97";
        }
    }

    private boolean hasNamedChild(Element element) {
        NodeList childNodes = element.getChildNodes();
        if (childNodes == null) return false;

        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node node = childNodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE && node.getNodeName() != null) {
                return true;
            }
        }
        return false;
    }

    private Element getDirectChild(Element parent, String name) {
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && name.equals(child.getLocalName())) {
                return (Element) child;
            }
        }
        return null;
    }
}
