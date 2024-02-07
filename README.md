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

Besides the CIE ID-SAML2 protocol differences, some of the SP behaviors
are hardcoded to work with simple IdPs only (i.e. there is no support for generating SP metadata
that joins multiple SPs) . Keycloak is slowly improving on this aspect, so over time this plugin
will become simpler and targeted on implementing only the specific changes required by SPID.

I have documented a reference configuration for CIE ID and the workarounds required 
in the project wiki (https://github.com/lscorcia/keycloak-cieid-provider/wiki). Please make 
sure to read it and understand the config steps and the open issues and
limitations before planning your Production environment.

## Status
This project is still at an alpha stage. It is currently under development 
and things may change quickly. It builds and successfully allows the CIE ID authentication
process, but I'm still working on it and I haven't tested it extensively since
I don't have access to the CIE ID Production environment yet.  
As far as I know it has not been used in Production in any environment yet.  

Until the project gets to a stable release, it will be targeting the most recent release 
of Keycloak as published on the website (see property `version.keycloak` in file `pom.xml`).
Currently the main branch is targeting Keycloak 16.1.1. **Do not use the latest release with previous
versions of Keycloak, it won't work!**  

Since this plugin uses some Keycloak internal modules, versions of this plugin
are coupled to Keycloak versions. After (major) Keycloak upgrades, you will almost
certainly have also to update this provider.  

Detailed instructions on how to install and configure this component are
available in the project wiki (https://github.com/lscorcia/keycloak-cieid-provider/wiki/Installing-the-CIE-ID-provider).

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
`{$KEYCLOAK_PATH}/standalone/deployments/`, with the right permissions.
Keycloak will take care of loading the module, no restart needed.  

Use this command for reference:  
```
mvn clean package && \
sudo install -C -o keycloak -g keycloak target/cieid-provider.jar /opt/keycloak/standalone/deployments/
```

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
