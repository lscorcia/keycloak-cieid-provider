[![Java CI with Maven](https://github.com/lscorcia/keycloak-cieid-provider/actions/workflows/maven.yml/badge.svg)](https://github.com/lscorcia/keycloak-cieid-provider/actions/workflows/maven.yml)
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/lscorcia/keycloak-cieid-provider?sort=semver)](https://img.shields.io/github/v/release/lscorcia/keycloak-cieid-provider?sort=semver) 
[![GitHub All Releases](https://img.shields.io/github/downloads/lscorcia/keycloak-cieid-provider/total)](https://img.shields.io/github/downloads/lscorcia/keycloak-cieid-provider/total)
[![GitHub issues](https://img.shields.io/github/issues/lscorcia/keycloak-cieid-provider)](https://github.com/lscorcia/keycloak-cieid-provider/issues)

# keycloak-cieid-provider
Italian CIE ID authentication provider for Keycloak (https://www.keycloak.org/)

## Project details
This custom authentication provider for Keycloak enables easy integration of CIE ID 
with existing applications by leveraging Keycloak identity brokering features.
Keycloak is a nice product, but still lacking on some aspects of SAML2 compatibility,
and the CIE ID specifications deviate from the SAML2 standard in some key aspects.

I have documented a reference configuration for CIE ID and the workarounds required 
in the project wiki (https://github.com/lscorcia/keycloak-cieid-provider/wiki). Please make 
sure to read it and understand the config steps and the open issues and
limitations before planning your Production environment.

## Status
This project is has been successfully tested for [CIE ID federation](https://docs.italia.it/italia/cie/cie-manuale-operativo-docs/it/master/onboarding.html) and **it's currently used in Production**.

It will be targeting the most recent release of Keycloak as published on the website (see property `version.keycloak` in file `pom.xml`).

Since this plugin uses some Keycloak internal modules, versions of this plugin
are coupled to Keycloak versions. After (major) Keycloak upgrades, you will almost
certainly have also to update this provider.  

## Compatibility
* Keycloak 26.2.x: Release `26.2.3`
* Keycloak 26.1.x: Release `26.1.4`
* Keycloak 26.x.x: Release `26.0.5`
* Keycloak 25.x.x: Release `25.0.1`
* Keycloak 24.x.x: Release `24.0.1`
* Keycloak 23.x.x: Release `1.0.7`
* Keycloak 19.x.x: Release `1.0.6`

## Configuration
### Release 26.2.3 (latest, Keycloak 26.2.x compatibility)
Detailed instructions on how to install and configure this component are 
available in the project wiki (https://github.com/italia/spid-keycloak-provider/wiki/Installing-the-SPID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

### Release 26.1.4 (latest, Keycloak 26.1.x compatibility)
Detailed instructions on how to install and configure this component are 
available in the project wiki (https://github.com/italia/spid-keycloak-provider/wiki/Installing-the-SPID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

**Important when upgrading to 26.1.4**: given the enhanced compatibility with [spid-sp-test](https://github.com/italia/spid-sp-test/) checks make sure your IdP configuration is maintained with "Identity Provider entity ID"
* PROD https://idserver.servizicie.interno.gov.it/idp/profile/SAML2/POST/SSO
* PRE-PROD https://preproduzione.idserver.servizicie.interno.gov.it/idp/profile/SAML2/POST/SSO
* SPID-SP-TEST https://localhost:8443

![image](https://github.com/user-attachments/assets/2828c301-0977-4cc0-9472-b25f3c83d5c8)

### Release 26.0.5 (latest, Keycloak 26.0.5 compatibility)
Detailed instructions on how to install and configure this component are 
available in the project wiki (https://github.com/italia/spid-keycloak-provider/wiki/Installing-the-SPID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

### Release 25.0.1 (latest, Keycloak 25.0.1 compatibility)
Detailed instructions on how to install and configure this component are 
available in the project wiki (https://github.com/italia/spid-keycloak-provider/wiki/Installing-the-SPID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

### Release 24.0.1 (latest, Keycloak 24.0.1 compatibility)
With this release targeting latest Keycloak 24.0.1 it was restored the possibility of configuring the plugin through 
the Keycloak web UI, detailed instructions on how to install and configure this component are 
available in the project wiki (https://github.com/italia/spid-keycloak-provider/wiki/Installing-the-SPID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client
#### IMPORTANT if upgrading from release 1.0.17
Provider ID was changed from `cieid` to `cieid-saml` in order to account for [hardcoded Keycloak 24.x behavior](https://github.com/keycloak/keycloak/blob/a228b6c7c9ec7a54ee91bb547b42cc4097ae38e2/js/apps/admin-ui/src/identity-providers/add/DetailSettings.tsx#L396). Before upgrading the plugin make sure to run this SQL query against Keycloak database:

    UPDATE IDENTITY_PROVIDER SET PROVIDER_ID="cieid-saml" WHERE PROVIDER_ID="cieid"

### Release 1.0.7 (Keycloak 23.x.x compatibility)
With the release targeting Keycloak 23.x.x it's not possible to configure the plugin through the Keycloak web UI, 
but only through REST services. Suggested to use https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

### Release 1.0.6
It's possible to configure the plugin through the Keycloak web UI, detailed instructions are
available in the project wiki (https://github.com/lscorcia/keycloak-cieid-provider/wiki/Installing-the-CIE-ID-provider).
To avoid errors, it's suggested to use anyway https://github.com/nicolabeghin/keycloak-cieid-provider-configuration-client

## Build requirements
* git
* JDK17+
* Maven

## Build (without docker)
Requirements:
* git
* JDK17+
* Maven

Just run:
```
git clone https://github.com/lscorcia/keycloak-cieid-provider.git
cd keycloak-cieid-provider
mvn clean package
```
The output package will be generated under `target/cieid-provider.jar`.

## Build (with docker)
Requirements:
* Docker

Just run:
```
git clone https://github.com/lscorcia/keycloak-cieid-provider.git
cd keycloak-cieid-provider
docker run --rm -v $(pwd):/opt/keycloak-cieid-provider -w /opt/keycloak-cieid-provider maven:3.8.6-openjdk-18-slim bash -c "mvn clean package"
```
The output package will be generated under `target/cieid-provider.jar`.

## Deployment
This provider should be deployed as a module, i.e. copied under
`{$KEYCLOAK_PATH}/providers/`, with the right permissions.
Keycloak will take care of loading the module, no restart needed.  

If successful you will find a new provider type called `CIE ID` in the
`Add Provider` drop down list in the Identity Provider configuration screen.

## Upgrading from previous versions
Upgrades are usually seamless, just repeat the deployment command.  
Then restart Keycloak and it will reload the resources from the packages. Make sure you also clear 
your browser caches or use incognito mode when verifying the correct deployment.
After the first reload you can turn back on the caches and restart Keycloak again.

## Open issues and limitations
Please read the appropriate page on the project wiki 
(https://github.com/lscorcia/keycloak-cieid-provider/wiki/Open-issues-and-limitations). 
If your problem is not mentioned there, feel free to open an issue on GitHub.

## Related projects
If you are interested in Keycloak plugins for the various Italian national auth
systems, you may be interested also in:

* Keycloak SPID Provider - https://github.com/italia/spid-keycloak-provider/  
A Keycloak provider for the SPID federation

* Keycloak CIE ID Provider - https://github.com/lscorcia/keycloak-cieid-provider/  
A Keycloak provider for the CIE ID federation

* Keycloak CNS Authenticator - https://github.com/lscorcia/keycloak-cns-authenticator/  
A Keycloak authenticator to login using CNS tokens and smart cards

## Acknowledgements
This project is released under the Apache License 2.0, same as the main Keycloak
package.
