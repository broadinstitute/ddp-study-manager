# Backend of the DDP Study Management App (DSM)

# Prerequisites
1. [Maven 3](https://maven.apache.org/download.cgi)
2. OpenJDK 11
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
   <servers>
       <server>
         <id>github</id>
         <username>...github username...</username>
         <password>...github token...</password>
       </server>
     </servers>
     ...
</settings>

````

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
