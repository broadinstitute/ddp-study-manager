# Backend of the DDP Study Management App (DSM)

# Prerequisites
1. [Maven 3](https://maven.apache.org/download.cgi)
2. [OpenJDK 11](https://openjdk.java.net/)
3. [gcloud CLI](https://cloud.google.com/sdk)
4. [Homebrew](https://brew.sh/)
5. [Lombok](https://projectlombok.org/setup/overview)

## Installing java
Building DDP Study Manager requires the installation of both an OpenJDK 11-compatible distribution
and maven. Homebrew includes packages for both, and can be installed with the commands
```
brew install java11
brew install maven
```

# Setting up maven
In addition to the Maven Central public repository, we use GitHub Packages' [Maven registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
for the [broadinstitute/lddp-backend](https://github.com/broadinstitute/lddp-backend) dependency.

To install this dependency, [create](https://docs.github.com/en/github/authenticating-to-github/keeping-your-account-and-data-secure/creating-a-personal-access-token)
a GitHub personal access token with the `read:packages` scope, and add the token to your
`~/.m2/settings.xml`. An example `settings.xml` file follows- replace `USERNAME` and `TOKEN` with your GitHub username and personal access token, respectively.
```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                      http://maven.apache.org/xsd/settings-1.0.0.xsd">
  
  <activeProfiles>
    <activeProfile>broad-institute</activeProfile>
  </activeProfiles>

  <servers>
    <server>
      <id>github</id>
      <username>USERNAME</username>
      <password>TOKEN</password>
    </server>
  </servers>
  
  <profiles>
    <profile>
      <id>broad-institute</id>
        <repositories>
          <repository>
            <id>central</id>
            <url>https://repo1.maven.org/maven2</url>
            <releases>
              <enabled>true</enabled>
            </releases>
            <snapshots>
              <enabled>true</enabled>
            </snapshots>
          </repository>
          <repository>
            <id>github</id>
            <name>broadinstitute/ddp-study-manager</name>
            <url>https://maven.pkg.github.com/broadinstitute/ddp-study-manager</url>
          </repository>
        </repositories>
    </profile>
  </profiles>
</settings>
```

DSM uses a single `pom.xml` but two different profile values for building the backend APIs
for GAE and background jobs for Cloud Functions.  When building the APIs, use `-Pbackend`.  To
build cloud functions, use `-Pcloud-function`

# Secrets
DSM uses GCP's [secret manager (SM)](https://cloud.google.com/secret-manager) to store credentials.

To read secrets for a specific environment:
```
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="${CONFIG_SECRETS}" > config/vault.conf
```
This will put `vault.conf` into the `config` dir.  `DSMServer` will look at `conf/vault.conf` at boot time.  **Do not
commit any generated .conf files!**

To seed configuration values for local development, run `render-testing-configs.sh`.   This will put
various `*.conf` files into the `/config` dir.  **Do not commit any generated .conf files!**

# Running Tests
Point your test configuration at the `test-log4j.xml` file and set the fallback config file:

```
export TEST_CONFIG_FILE=config/test-config.conf
java -Dlog4j.configuration=test-log4j.xml ...
```


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

# Building and deploying
DSM has two components: the backend APIS and Cloud Functions, which are essentially background tasks.

### Building and deploying backend APIS

To deploy to a specific project, just run the `build-and-deploy` script, which takes two args: the name of a project and the name
of the GCP secret to read from said project.  To deploy to dev:

```
cd appengine/deploy
./build-and-deploy.sh broad-ddp-dev study-manager-config
```

### Building and deploying cloud functions
 ```
 ./build-and-deploy-cloud-functions.sh project gcp-secret study-manager-schema study-server-schema
 ```


# Viewing Logs
One way to view logs is via the [GCP console](https://console.cloud.google.com/logs/viewer).  \


For backend API logs, drill into `GAE Application -> study-manager-backend -> version`.

For could functions, use the `resource.type="cloud_function"` resource.
