# Backend of the DDP Sample Management App (DSM)

This is our first DDP backend app with a well structured dependency on https://github.com/broadinstitute/ddp-backend-core

# Prerequisites
1. [Maven 3](https://maven.apache.org/download.cgi)
2. [JDK 8](http://www.oracle.com/technetwork/java/javase/downloads/index.html)

# Setting up maven
In addition to the usual public repos, we have an internal repo: [broad-internal artifactory](http://artifactory.broadinstitute.org).  This is where our DDP artifacts will live.

To enable this repo, edit your `settings.xml` file so that it includes the following:

````
<settings>
    <localRepository>${user.home}/.m3/repository</localRepository>

    <profiles>
      <profile>
	<id>PERSONAL</id>
	<repositories>
	  <repository>
            <id>broad-artifactory-release-local</id>
            <name>artifactory-releases</name>
            <url>http://artifactory.broadinstitute.org/artifactory/libs-release-local</url>
	  </repository>
	</repositories>
      </profile>
    </profiles>

     <activeProfiles>
        <activeProfile>PERSONAL</activeProfile>
     </activeProfiles>

     <servers>
       <server>
	 <id>broad-artifactory-release-local</id>
	 <username>[YOUR ARTIFACTORY USERNAME]</username>
	 <password>[YOUR *encrypted* ARTIFACTORY PASSWORD]</password>
	 </server>
     </servers>

</settings>

````

#Artifactory setup
To get your username and password for artifactory, get in touch with Zim or email help@broad and tell them you need access to the "ddp" group in artifactory.

Once you've got an account, login to [artifactory](https://artifactory.broadinstitute.org), click on your username in the upper right corner, and copy the "encrypted password" field from the form into the `password` field in `settings.xml`.  **Remember: this is the _encrypted_ version of your password.**

# Vault
From within the top level directory, run the `render-templates.sh` script to generate `vault.conf`:
```shell
ENVIRONMENT=dev
docker run --rm -v $PWD:/working -e VAULT_TOKEN=$(cat ~/.vault-token) -e ENVIRONMENT=$ENVIRONMENT -e OUT_PATH=./config -e INPUT_PATH=./config broadinstitute/dsde-toolbox:dev render-templates.sh
```
This will put `vault.conf` into the `config` dir.  `DSMServer` will look at `conf/vault.conf` at boot time.  **Do not
commit any generated vault.conf files!**

# Getting something up and running
This repo has a starter `DSMServer` app.  To setup the code in Intellj, click `File->New->Project From Existing Sources`
and then point Intellij at `pom.xml`.

To run it from Intellij, just right click on the `main()` method, click `debug`,
and then point your browser at [localhost](http://localhost:4567).

# Making a single executable jar
`mvn -DskipTests package` will create `target/DSMServer.jar`.  You can then run this via `java -jar ./target/DSMServer.jar`

# Serving out static files during development
This repo contains backend code, but to serve out static assets, you can use environment variable `-Dserver.static_content_source=[full path to static assets]`
to serve out javascript, html, css, etc. via spark.  If you've checked out the [front end code](https://github.com/broadinstitute/ddp-dsm-ui) at `/foo/bar`
on your local machine, then you can set `-Dserver.static_content_source=/foo/bar` when running `DSMServer` and the app should be visible on
[localhost](http://localhost:4567).
