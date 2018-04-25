Unfortunately, at the moment there is no CI so some manual work needs to be done.

#### Local Anka Controller and Registry
In order to run tests, we need to have Anka Controller and Anka registry to be running locally.
It can be done in 3 easy steps:
  1. Download and unarchive Anka Controller and Registry from [here](https://veertu.com/download-anka-build/).
  2. Open `anka-controller.docker` and change `REGISTRY_ADDR` to `http://anka-registry`
  3. Run `docker-compose up` to start controller and registry.
  
Now, let's make this local registry a default one:

```bash
anka registry add local http://localhost:80
anka registry set local
```

#### Configuring Local Anka Build

Once a local Anka Controller is running it's possible to join it!

```bash
$: ankacluster join localhost:8090
Testing connection to controller...: Ok
Testing connection to the registry...: Ok
Testing connection to the queue...: Ok
Cluster join success
```

Don't forget to [install Anka Build](https://veertu.com/download-anka-build/) to have `ankacluster`.

#### Populating Local Registry

Tests are using `osx-10.13-base` image which can be found [here](https://github.com/cirruslabs/osx-images). Once `osx-10.13-base`
is built locally using packer, it can be pushed to the local registry:

```bash
anka registry push -d local osx-10.13-base latest
```

#### Distribute Template

Now that everything up and running there is the last step to distribute `osx-10.13-base` template to the connected local node. 
Simply go to http://localhost:8090/distribute-templates and click Distribute.

#### Running Tests

Simply run `./gradlew check` once local Anka Controller is up and running.
