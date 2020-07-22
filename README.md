# Backend of the DDP Study Management App (DSM)

# Prerequisites
1. [Maven 3](https://maven.apache.org/download.cgi)
2. OpenJDK 11
3. [gcloud CLI](https://cloud.google.com/sdk)
```
brew tap homebrew/cask-versions
brew cask install java11
```

# Setting up maven
In addition to the usual public repos, we use github package manager for the older lddp core dependency.

To download this dependency, generate a github token and add it to your `~/.m2/settings.xml`:

````
<settings>
   
   ...
   
    <activeProfiles>
       <activeProfile>github</activeProfile>
     </activeProfiles>
   
   ...
   
   <servers>
       <server>
         <id>github</id>
         <username>...github username...</username>
         <password>...github token...</password>
       </server>
     </servers>
     
     ...
     
     <profiles>
         <profile>
           <id>github</id>
           <repositories>
             <repository>
               <id>central</id>
               <url>https://repo1.maven.org/maven2</url>
               <releases><enabled>true</enabled></releases>
               <snapshots><enabled>true</enabled></snapshots>
             </repository>
             <repository>
               <id>github</id>
               <name>GitHub OWNER Apache Maven Packages</name>
               <url>https://maven.pkg.github.com/broadinstitute/ddp-study-manager</url>
               </repository>
     	</repositories>
         </profile>
       </profiles>
       
       ...
       
       
</settings>

````

# Secrets
DSM uses GCP's [secret manager (SM)](https://cloud.google.com/secret-manager) to store credentials.

To read secrets for a specific environment:
```
gcloud --project=${PROJECT_ID} secrets versions access latest --secret="${CONFIG_SECRETS}" > config/vault.conf
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

# Building and deploying
To deploy to a specific project, just run the `build-and-deploy` script, which takes two args: the name of a project and the name
of the GCP secret to read from said project.  To deploy to dev:

```
cd appengine/deploy
./build-and-deploy.sh broad-ddp-dev study-manager-config
```

# Viewing Logs
One way to view logs is via the [GCP console](https://console.cloud.google.com/logs/viewer).  Drill into `GAE Application -> study-manager-backend -> version`.
