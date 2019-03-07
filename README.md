[![Build Status](https://api.cirrus-ci.com/github/cirruslabs/anka-controller-extended.svg)](https://cirrus-ci.com/github/cirruslabs/anka-controller-extended)
[![Docker Image](https://images.microbadger.com/badges/version/cirrusci/anka-controller-extended.svg)](https://microbadger.com/images/cirrusci/anka-controller-extended)
[![](https://images.microbadger.com/badges/image/cirrusci/anka-controller-extended.svg)](https://microbadger.com/images/cirrusci/anka-controller-extended)

Anka Controller has it's own [REST API](https://ankadoc.bitbucket.io/using-controller/#controller-rest-apis) which is very basic. Out of the box it lacks two important features:

1. **Authorization**. There is no built-in way to secure Controller's API endpoint. 
2. **Startup script**. There is no option to provide a startup script when creating a VM.

This project aim to solve these issues. It provides a Docker container that can connect to an Anka Controller through a VPN
and exposes a GRPC service that has authorization and supports creating VMs with a custom startup script.

### VPN

ACE Docker Container has `openconnect` VPN client pre-installed. If your Anka Controller is in a separate VPN from ACE, then
you need not to forget to provide `ANYCONNECT_*` environment variables when running the container. Also **you need to run
the container in privileged mode for VPN to work**. 

### Deploying

Since Anka Controller Extended (ACE) is packed into a Docker Container it's very easy to run it on any platform. In this section,
we'll use Google Cloud as an example.

First things first, we need to create an [environment variables file](https://docs.docker.com/compose/env-file/) with all
the information needed for ACE. 

```properties
ANYCONNECT_SERVER=<Optional VPN server IP>
ANYCONNECT_USER=<Optional username to use to connect to VPN>
ANYCONNECT_PASSWORD=<Optional password to use to connect to VPN>
ANKA_HOST=<Required IP of Anka's Controller API endpoint>
ANKA_PORT=<Required PORT of Anka's Controller API endpoint. Usually port 80 is used.>
ACCESS_TOKEN=<Optional secret access token for authorization of clients>
```

If you are planning to use ACE with [Cirrus CI](https://cirrus-ci.org), you need to make sure to publicly expose ACE.
One way to do so is to reserve a static IP address and use it via `--address` when creating ACE instance. Please refer 
to [documentation](https://cloud.google.com/compute/docs/ip-addresses/reserve-static-external-ip-address) for more details. 

Now let's create a Google Compute instance from `cirruslabs/anka-controller-extended` container with our custom 
environment variables file: 

```bash
gcloud beta compute instances create-with-container anka-controller-extended \
     --container-image docker.io/cirrusci/anka-controller-extended:latest \
     --tags anka-controller \
     --container-privileged \
     --address $STATIC_IP \
     --container-env-file $PATH_TO_ENV_FILE
```

Now we need to create a firewall rule to allow incoming traffic for `8239` port.

```bash
gcloud compute firewall-rules create allow-grpc \
    --allow tcp:8239 --target-tags anka-controller
```

### Testing locally

```bash
docker run --privileged \
  --env-file $PATH_TO_ENV_FILE \
  docker.io/cirrusci/anka-controller-extended:latest
```

### Published Kotlin Libraries

This repository also provides two Kotlin libraries that can be integrated in other projects. To use them please add Cirrus
Labs' Maven repository like this:

```groovy
// build.gradle
repositories {
    maven {
        url  "https://dl.bintray.com/cirruslabs/maven" 
    }
}
```

#### Anka SDK [![Anks SDK](https://api.bintray.com/packages/cirruslabs/maven/anka-sdk/images/download.svg) ](https://bintray.com/cirruslabs/maven/anka-sdk/_latestVersion)

`sdk` module represents a Kotlin rewrite of Anka SDK from [Anka's Jenkins Plugin](https://github.com/jenkinsci/anka-build-plugin).

#### Controller Client [![Controller Client](https://api.bintray.com/packages/cirruslabs/maven/anka-controller-extended-client/images/download.svg) ](https://bintray.com/cirruslabs/maven/anka-controller-extended-client/_latestVersion)

This library allows to create a GRPC client for ACE. Here is an example: 

```kotlin
val channel = ManagedChannelBuilder.forTarget("<IP>:8239")
  .usePlaintext(true)
  .build()
val client = ClientFactory.create(channel)
val request = VMStatusRequest.newBuilder()
  .setVmId("not-exists")
  .build()
```
