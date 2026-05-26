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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.saml.validators.DestinationValidator;

/**
 * CIE ID Identity Provider that extends the standard SAML Identity Provider
 * with CIE ID-specific functionality.
 *
 * CIE ID-specific modifications to AuthnRequest and LogoutRequest (Issuer NameQualifier,
 * Format, NameIDPolicy SPNameQualifier) are handled by CieIdSamlAuthenticationPreprocessor.
 *
 * CIE ID-specific response validation is handled by CieIdSAMLEndpoint.
 */
public class CieIdIdentityProvider extends SAMLIdentityProvider {
    protected static final Logger logger = Logger.getLogger(CieIdIdentityProvider.class);

    /**
     * Client note key for storing the request IssueInstant, used for CIE ID response validation.
     */
    public static final String CIEID_REQUEST_ISSUE_INSTANT = "CIEID_REQUEST_ISSUE_INSTANT";

    /**
     * Marker key set on the auth/user session to indicate this is a CIE ID flow.
     * Read by CieIdSamlAuthenticationPreprocessor to skip non-CIE ID SAML providers.
     */
    public static final String CIEID_FLOW_MARKER = "CIEID_FLOW";

    private final DestinationValidator destinationValidator;

    private final CieIdIdentityProviderConfig cieIdConfig;

    public CieIdIdentityProvider(KeycloakSession session, CieIdIdentityProviderConfig config,
                                DestinationValidator destinationValidator) {
        super(session, (SAMLIdentityProviderConfig) config, destinationValidator);
        this.cieIdConfig = config;
        this.destinationValidator = destinationValidator;
    }

    @Override
    public CieIdIdentityProviderConfig getConfig() {
        return cieIdConfig;
    }

    @Override
    public Response performLogin(AuthenticationRequest request) {
        request.getAuthenticationSession().setClientNote(CIEID_FLOW_MARKER, "true");
        return super.performLogin(request);
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession,
                                                    UriInfo uriInfo, RealmModel realm) {
        userSession.setNote(CIEID_FLOW_MARKER, "true");
        return super.keycloakInitiatedBrowserLogout(session, userSession, uriInfo, realm);
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession,
                                  UriInfo uriInfo, RealmModel realm) {
        userSession.setNote(CIEID_FLOW_MARKER, "true");
        super.backchannelLogout(session, userSession, uriInfo, realm);
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
        return new CieIdSAMLEndpoint(session, this, getConfig(), callback, destinationValidator);
    }
}
