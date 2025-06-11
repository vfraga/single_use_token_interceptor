package org.sample.single.use.token.interceptor.utils;

import org.apache.http.conn.ssl.AbstractVerifier;

import javax.net.ssl.SSLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Custom Hostname Verifier that allows localhost and local IP addresses.
 * This is used to verify the hostname against the common names and subject alternative names
 * that might not be in SSL certificates, allowing for local development and testing.
 */
public class CustomHostnameVerifier extends AbstractVerifier {

    private static final List<String> LOOPBACK_HOSTS = Collections.unmodifiableList(
            Arrays.asList("::1", "127.0.0.1", "localhost", "localhost.localdomain")
    );

    @Override
    public void verify(final String hostname, final String[] commonNames, final String[] subjectAlternativeNames) throws SSLException {
        final List<String> effectiveSubjectAlts = new ArrayList<>(LOOPBACK_HOSTS);

        if (subjectAlternativeNames != null) {
            Collections.addAll(effectiveSubjectAlts, subjectAlternativeNames);
        }

        if (commonNames != null && commonNames.length > 0 && !effectiveSubjectAlts.contains(commonNames[0])) {
            effectiveSubjectAlts.add(commonNames[0]);
        }

        super.verify(hostname, commonNames, effectiveSubjectAlts.toArray(new String[0]), false);
    }
}