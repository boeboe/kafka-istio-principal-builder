/*
Copyright © 2022 Tetrate
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package io.tetrate.kafka.security.istio;

import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.security.auth.AuthenticationContext;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.KafkaPrincipalBuilder;
import org.apache.kafka.common.security.auth.SslAuthenticationContext;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.List;

/**
 * This class derives the Istio SPIFFE based URI SubjectAlternativeName for the peer certificate
 * presented by a client in an mTLS session. The client in this case would be an Envoy sidecar
 * from a workload within an istio Service Mesh.
 */
public class IstioPrincipalBuilder implements KafkaPrincipalBuilder {
  private final static Logger log = LogManager.getLogger(IstioPrincipalBuilder.class);

  @Override
  public KafkaPrincipal build(AuthenticationContext authenticationContext) throws KafkaException {
    try {
      KafkaPrincipal kafkaPrincipal;

      if (authenticationContext instanceof SslAuthenticationContext) {
        SslAuthenticationContext context = (SslAuthenticationContext) authenticationContext;

        Certificate[] peerCertificates = context.session().getPeerCertificates();
        if (peerCertificates == null || peerCertificates.length == 0) {
          throw new KafkaException("Failed to build KafkaPrincipal. No PeerCertificate found.");
        }

        X509Certificate x509Certificate = (X509Certificate) peerCertificates[0];
        Collection<List<?>> sans = x509Certificate.getSubjectAlternativeNames();
        if (sans == null || sans.size() == 0) {
          throw new KafkaException("Failed to build KafkaPrincipal. No SubjectAlternativeNames found in PeerCertificate.");
        }

        String uriSan = null;
        for (List<?> entry : sans) {
          final int type = ((Integer) entry.get(0)).intValue();
          // URI
          if (type == 6) {
            uriSan = (String) entry.get(1);
            break;
          }
        }
        if (uriSan == null || uriSan == "") {
          throw new KafkaException("Failed to build KafkaPrincipal. No URI SubjectAlternativeName found.");
        }

        kafkaPrincipal = new KafkaPrincipal("User", uriSan);
        log.info("Successfully extracted principle with name '" + uriSan + "'");
        return kafkaPrincipal;
      } else {
        throw new KafkaException("Failed to build KafkaPrincipal. SslAuthenticationContext is required.");
      }
    } catch (Exception ex) {
      throw new KafkaException("Failed to build KafkaPrincipal due to: ", ex);
    }
  }
}