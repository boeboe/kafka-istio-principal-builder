# Kafka Istio - Principal Builder

### Instructions

Build the code:
```console
# git clone https://github.com/boeboe/kafka-istio-principal-builder.git
# cd kafka-istio-principal-builder
# mvn clean install
```

Copy the jar `target/kafka-istio-principal-builder-0.0.1.jar` on your broker nodes, 
in the lib directory, for example `/opt/kafka/libs/`

### Explanation

The motivation behind this code is the following: producers and consumers deployed
in a Kubernetes cluster, often want to leverage Istio as workload Identity mechanism
to enforce Authentication and Authorization. Istio is able to sidecar container
workloads with an Envoy proxy, that will automatically get provided with client and
server certificates to mTLS connection establishment.

Istio issued Certificates leverage the `X509v3 Subject Alternative Name` extension,
which will contain a [SPIFFE](https://spiffe.io) based URI field, containing three
parameters that form the cornerstone of Istio identity enforcement.

```
URI:spiffe://cluster.local/ns/mynamespace/sa/myserviceaccount
```

The three parameters are
 - Trust domain: `cluster.local`
 - Namespace: `mynamespace`
 - ServiceAccount: `myserviceaccount`

Kafka does support mTLS and is also able to parse data from the offered client
certificate. However, current implementation only allows for `X500 Distinguished 
Names` in the Common Name of the certificate to be mapped onto Kafka users.

This extension will extract the SPIFFE URI from the certificate to be used as
principle for Authorization decision-making (Kafka topic ACL).