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
This project is still at a beta stage, but it has been successfully tested for [CIE ID federation](https://docs.italia.it/italia/cie/cie-manuale-operativo-docs/it/master/onboarding.html) and **it's currently used in Production**.

Until the project gets to a stable release, it will be targeting the most recent release 
of Keycloak as published on the website (see property `version.keycloak` in file `pom.xml`).

Since this plugin uses some Keycloak internal modules, versions of this plugin
are coupled to Keycloak versions. After (major) Keycloak upgrades, you will almost
certainly have also to update this provider.  

## Compatibility
* Keycloak 23.x.x: Release 1.0.7
* Keycloak 19.x.x: Release 1.0.6

## Configuration
### Release 1.0.7 (latest, Keycloak 23.x.x compatibility)
With the latest release targeting latest Keycloak 23.x.x it's not possible to configure the plugin through the Keycloak web UI, 
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
Just run `mvn clean package` for a full rebuild. The output package will
be generated under `target/cieid-provider.jar`.

## Build (with docker)
Requirements:
* Docker

Just run:
```
git clone https://github.com/italia/keycloak-cieid-provider.git
docker run --rm -v $(pwd)/keycloak-cieid-provider:/opt/keycloak-cieid-provider -w /opt/keycloak-cieid-provider maven:3.8.6-openjdk-18-slim bash -c "mvn clean package"
```
The output package will be generated under `cieid-provider/target/cieid-provider.jar`.

## Deployment
This provider should be deployed as a module, i.e. copied under
`{$KEYCLOAK_PATH}/providers/`, with the right permissions.
Keycloak will take care of loading the module, no restart needed.  

If successful you will find a new provider type called `CIE ID` in the
`Add Provider` drop down list in the Identity Provider configuration screen.

## Upgrading from previous versions
Upgrades are usually seamless, just repeat the deployment command.  
Sometimes Keycloak caches don't get flushed when a new deployment occurs; in that case you will need
to edit the file `{$KEYCLOAK_PATH}/standalone/configuration/standalone.xml`, find the following section
```
<theme>
  <staticMaxAge>2592000</staticMaxAge>
  <cacheThemes>true</cacheThemes>
  <cacheTemplates>true</cacheTemplates>
  <dir>${jboss.home.dir}/themes</dir>
</theme>
```
and change it to:
```
<theme>
  <staticMaxAge>-1</staticMaxAge>
  <cacheThemes>false</cacheThemes>
  <cacheTemplates>false</cacheTemplates>
  <dir>${jboss.home.dir}/themes</dir>
</theme>
```

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
