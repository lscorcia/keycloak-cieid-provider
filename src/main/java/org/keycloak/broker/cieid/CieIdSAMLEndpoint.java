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
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.KeyManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.rotation.KeyLocator;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.saml.processing.core.saml.v2.constants.X500SAMLProfileConstants;
import org.keycloak.saml.processing.core.saml.v2.util.AssertionUtil;
import org.keycloak.saml.validators.ConditionsValidator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.StringUtil;
import org.w3c.dom.Element;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import javax.xml.namespace.QName;
import java.net.URI;
import java.util.Collections;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * CIE ID-specific SAML endpoint that extends the standard SAMLEndpoint
 * with CIE ID response validation rules.
 */
public class CieIdSAMLEndpoint extends SAMLEndpoint {
    protected static final Logger logger = Logger.getLogger(CieIdSAMLEndpoint.class);

    private final CieIdIdentityProviderConfig cieIdConfig;
    private final CieIdChecks cieIdChecks;

    public CieIdSAMLEndpoint(KeycloakSession session, CieIdIdentityProvider provider,
                           CieIdIdentityProviderConfig config,
                           SAMLIdentityProvider.AuthenticationCallback callback,
                           DestinationValidator destinationValidator) {
        super(session, provider, config, callback, destinationValidator);
        this.cieIdConfig = config;
        this.cieIdChecks = new CieIdChecks(config);
    }

    @GET
    @Override
    public Response redirectBinding(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                    @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                    @QueryParam(GeneralConstants.SAML_ARTIFACT_KEY) String samlArt,
                                    @QueryParam(GeneralConstants.RELAY_STATE) String relayState) {
        return new CieIdRedirectBinding().execute(samlRequest, samlResponse, samlArt, relayState, null);
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Override
    public Response postBinding(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                @FormParam(GeneralConstants.SAML_ARTIFACT_KEY) String samlArt,
                                @FormParam(GeneralConstants.RELAY_STATE) String relayState) {
        return new CieIdPostBinding().execute(samlRequest, samlResponse, samlArt, relayState, null);
    }

    @Path("clients/{client_id}")
    @GET
    @Override
    public Response redirectBindingIdpInitiated(@QueryParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                                @QueryParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                                @QueryParam(GeneralConstants.RELAY_STATE) String relayState,
                                                @PathParam("client_id") String clientId) {
        return new CieIdRedirectBinding().execute(samlRequest, samlResponse, null, relayState, clientId);
    }

    @Path("clients/{client_id}")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Override
    public Response postBindingIdpInitiated(@FormParam(GeneralConstants.SAML_REQUEST_KEY) String samlRequest,
                                           @FormParam(GeneralConstants.SAML_RESPONSE_KEY) String samlResponse,
                                           @FormParam(GeneralConstants.RELAY_STATE) String relayState,
                                           @PathParam("client_id") String clientId) {
        return new CieIdPostBinding().execute(samlRequest, samlResponse, null, relayState, clientId);
    }

    /**
     * CIE ID-specific POST binding that overrides handleLoginResponse with CIE ID validation.
     */
    protected class CieIdPostBinding extends PostBinding {
        @Override
        protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder,
                                               ResponseType responseType, String relayState, String clientId) {
            try {
                AuthenticationSessionModel authSession;
                if (StringUtil.isNotBlank(clientId)) {
                    authSession = samlIdpInitiatedSSO(clientId);
                } else if (StringUtil.isNotBlank(relayState)) {
                    authSession = callback.getAndVerifyAuthenticationSession(relayState);
                } else {
                    logger.error("SAML RelayState parameter was null when it should be returned by the IDP");
                    event.event(EventType.LOGIN);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
                return handleCieIdLoginResponse(this::validateAssertionSignature, samlResponse, holder, responseType, authSession);
            } catch (WebApplicationException e) {
                return e.getResponse();
            }
        }

        private boolean validateAssertionSignature(Element assertionElement, SAMLDocumentHolder holder) {
            return validateAssertionSignatureImpl(
                assertionElement,
                getIDPKeyLocator(),
                containsUnencryptedSignature(holder)
            );
        }
    }

    /**
     * CIE ID-specific Redirect binding that overrides handleLoginResponse with CIE ID validation.
     */
    protected class CieIdRedirectBinding extends RedirectBinding {
        @Override
        protected Response handleLoginResponse(String samlResponse, SAMLDocumentHolder holder,
                                               ResponseType responseType, String relayState, String clientId) {
            try {
                AuthenticationSessionModel authSession;
                if (StringUtil.isNotBlank(clientId)) {
                    authSession = samlIdpInitiatedSSO(clientId);
                } else if (StringUtil.isNotBlank(relayState)) {
                    authSession = callback.getAndVerifyAuthenticationSession(relayState);
                } else {
                    logger.error("SAML RelayState parameter was null when it should be returned by the IDP");
                    event.event(EventType.LOGIN);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, null, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
                }
                return handleCieIdLoginResponse(this::validateAssertionSignature, samlResponse, holder, responseType, authSession);
            } catch (WebApplicationException e) {
                return e.getResponse();
            }
        }

        private boolean validateAssertionSignature(Element assertionElement, SAMLDocumentHolder holder) {
            return validateAssertionSignatureImpl(
                assertionElement,
                getIDPKeyLocator(),
                containsUnencryptedSignature(holder)
            );
        }
    }

    /**
     * Handles the SAML login response with CIE ID-specific validation rules.
     * This is the core CIE ID-specific logic that differs from the parent SAMLEndpoint.
     *
     * @param signatureValidator function to validate assertion signature (provided by binding subclass)
     * @param authSession        the already-resolved authentication session
     */
    protected Response handleCieIdLoginResponse(
            BiFunction<Element, SAMLDocumentHolder, Boolean> signatureValidator,
            String samlResponse, SAMLDocumentHolder holder,
            ResponseType responseType, AuthenticationSessionModel authSession) {
        EventBuilder event = new EventBuilder(realm, session, clientConnection);

        try {
            session.getContext().setAuthenticationSession(authSession);

            KeyManager.ActiveRsaKey keys = new KeyManager.ActiveRsaKey(session.keys().getActiveKey(realm, KeyUse.SIG, Algorithm.RS256));

            // CIE ID-specific: Handle error responses with CIE ID error code translation
            if (!isSuccessfulSamlResponse(responseType)) {
                if (cieIdChecks.isCieIdFault(responseType)) {
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST,
                        cieIdChecks.formatCieIdFaultMessage(responseType.getStatus().getStatusMessage()));
                } else {
                    String statusMessage = responseType.getStatus() == null || responseType.getStatus().getStatusMessage() == null
                        ? Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR
                        : responseType.getStatus().getStatusMessage();
                    event.event(EventType.IDENTITY_PROVIDER_RESPONSE_ERROR);
                    event.error(Errors.INVALID_SAML_RESPONSE);
                    return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, statusMessage);
                }
            }

            if (responseType.getAssertions() == null || responseType.getAssertions().isEmpty()) {
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.IDENTITY_PROVIDER_UNEXPECTED_ERROR);
            }

            boolean assertionIsEncrypted = AssertionUtil.isAssertionEncrypted(responseType);

            if (cieIdConfig.isWantAssertionsEncrypted() && !assertionIsEncrypted) {
                logger.error("The assertion is not encrypted, which is required.");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            Element assertionElement;
            if (assertionIsEncrypted) {
                assertionElement = AssertionUtil.decryptAssertion(responseType, keys.getPrivateKey());
            } else {
                assertionElement = DocumentUtil.getElement(holder.getSamlDocument(), new QName(JBossSAMLConstants.ASSERTION.get()));
            }

            // CIE ID-specific: Apply CIE ID response validation rules
            String cieIdResponseValidationError = cieIdChecks.validateCieIdResponse(authSession, holder, assertionElement);

            if (cieIdResponseValidationError != null) {
                logger.error("CIE ID Response Validation Error: " + cieIdResponseValidationError);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST,
                    cieIdConfig.isDebugEnabled() ? cieIdResponseValidationError : "CieIdSamlCheck_GenericError");
            }

            // Validate the response Issuer
            final String responseIssuer = responseType.getIssuer() != null ? responseType.getIssuer().getValue() : null;
            if (cieIdConfig.getIdpEntityId() != null && !cieIdConfig.getIdpEntityId().equals(responseIssuer)) {
                logger.errorf("Response Issuer validation failed: expected %s, actual %s", cieIdConfig.getIdpEntityId(), responseIssuer);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            // Validate InResponseTo attribute
            String expectedRequestId = authSession.getClientNote(SamlProtocol.SAML_REQUEST_ID_BROKER);
            if (!validateInResponseToAttribute(responseType, expectedRequestId)) {
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            // Validate assertion signature
            if (!signatureValidator.apply(assertionElement, holder)) {
                logger.error("validation failed");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SIGNATURE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            if (AssertionUtil.isIdEncrypted(responseType)) {
                AssertionUtil.decryptId(responseType, data -> Collections.singletonList(keys.getPrivateKey()));
            }

            AssertionType assertion = responseType.getAssertions().get(0).getAssertion();

            // Validate the assertion Issuer
            final String assertionIssuer = assertion.getIssuer() != null ? assertion.getIssuer().getValue() : null;
            if (cieIdConfig.getIdpEntityId() != null && !cieIdConfig.getIdpEntityId().equals(assertionIssuer)) {
                logger.errorf("Assertion Issuer validation failed: expected %s, actual %s", cieIdConfig.getIdpEntityId(), assertionIssuer);
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            NameIDType subjectNameID = getSubjectNameID(assertion);
            String principal = getPrincipal(assertion);

            if (principal == null) {
                logger.errorf("no principal in assertion; expected: %s", expectedPrincipalType());
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.INVALID_REQUESTER);
            }

            BrokeredIdentityContext identity = new BrokeredIdentityContext(principal, cieIdConfig);
            identity.getContextData().put(SAML_LOGIN_RESPONSE, responseType);
            identity.getContextData().put(SAML_ASSERTION, assertion);
            identity.setAuthenticationSession(authSession);
            identity.setUsername(principal);

            if (subjectNameID != null && subjectNameID.getFormat() != null &&
                subjectNameID.getFormat().toString().equals(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get())) {
                identity.setEmail(subjectNameID.getValue());
            }

            if (cieIdConfig.isStoreToken()) {
                identity.setToken(samlResponse);
            }

            ConditionsValidator.Builder cvb = new ConditionsValidator.Builder(
                assertion.getID(), assertion.getConditions(), destinationValidator)
                .clockSkewInMillis(1000 * cieIdConfig.getAllowedClockSkew());
            try {
                String issuerURL = getEntityId();
                cvb.addAllowedAudience(URI.create(issuerURL));
                if (responseType.getDestination() != null) {
                    cvb.addAllowedAudience(URI.create(responseType.getDestination()));
                }
            } catch (IllegalArgumentException ex) {
                // warning has been already emitted
            }

            if (!cvb.build().isValid()) {
                logger.error("Assertion expired.");
                event.event(EventType.IDENTITY_PROVIDER_RESPONSE);
                event.error(Errors.INVALID_SAML_RESPONSE);
                return ErrorPage.error(session, authSession, Response.Status.BAD_REQUEST, Messages.EXPIRED_CODE);
            }

            AuthnStatementType authn = null;
            for (Object statement : assertion.getStatements()) {
                if (statement instanceof AuthnStatementType) {
                    authn = (AuthnStatementType) statement;
                    identity.getContextData().put(SAML_AUTHN_STATEMENT, authn);
                    break;
                }
            }

            if (assertion.getAttributeStatements() != null) {
                String email = getX500Attribute(assertion, X500SAMLProfileConstants.EMAIL);
                if (email != null) {
                    identity.setEmail(email);
                }
            }

            String brokerUserId = cieIdConfig.getAlias() + "." + principal;
            identity.setBrokerUserId(brokerUserId);
            identity.setIdp(provider); // parent's protected provider field

            if (authn != null && authn.getSessionIndex() != null) {
                identity.setBrokerSessionId(cieIdConfig.getAlias() + "." + authn.getSessionIndex());
            }

            return callback.authenticated(identity);

        } catch (WebApplicationException e) {
            return e.getResponse();
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not process response from SAML identity provider.", e);
        }
    }

    private boolean validateAssertionSignatureImpl(Element assertionElement,
                                                   KeyLocator keyLocator,
                                                   boolean hasUnencryptedSignature) {
        boolean signed = AssertionUtil.isSignedElement(assertionElement);
        final boolean assertionSignatureNotExistsWhenRequired = cieIdConfig.isWantAssertionsSigned() && !signed;
        final boolean signatureNotValid = signed && cieIdConfig.isValidateSignature() && !AssertionUtil.isSignatureValid(assertionElement, keyLocator);
        final boolean hasNoSignatureWhenRequired = !signed && cieIdConfig.isValidateSignature() && !hasUnencryptedSignature;

        return !(assertionSignatureNotExistsWhenRequired || signatureNotValid || hasNoSignatureWhenRequired);
    }

    // isSuccessfulSamlResponse is available as protected final in SAMLEndpoint.Binding (inner class only),
    // so we keep a copy here for use in handleCieIdLoginResponse (outer class context).
    private boolean isSuccessfulSamlResponse(ResponseType responseType) {
        return responseType != null
            && responseType.getStatus() != null
            && responseType.getStatus().getStatusCode() != null
            && responseType.getStatus().getStatusCode().getValue() != null
            && !responseType.getStatus().getStatusCode().getValue().toString().isEmpty()
            && Objects.equals(responseType.getStatus().getStatusCode().getValue().toString(),
                              JBossSAMLURIConstants.STATUS_SUCCESS.get());
    }

    private String getEntityId() {
        String configEntityId = cieIdConfig.getEntityId();
        if (configEntityId == null || configEntityId.isEmpty()) {
            return UriBuilder.fromUri(session.getContext().getUri().getBaseUri())
                .path("realms").path(realm.getName()).build().toString();
        }
        return configEntityId;
    }

}
