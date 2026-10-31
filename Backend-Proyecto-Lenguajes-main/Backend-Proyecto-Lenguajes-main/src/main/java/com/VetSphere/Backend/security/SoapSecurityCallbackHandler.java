// Aqui se implementa la interfaz estandar de JAAS

package com.VetSphere.Backend.security;

import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler; //es el nuvleo de JAAS para la autenticacion 
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class SoapSecurityCallbackHandler implements CallbackHandler {
    private final String usuarioSoap;
    private final String passwordSoap;

    public SoapSecurityCallbackHandler(
            @Value("${vetsphere.soap.security.username}") String usuarioSoap,
            @Value("${vetsphere.soap.security.password}") String passwordSoap) {
        this.usuarioSoap = usuarioSoap;
        this.passwordSoap = passwordSoap;
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback passwordCallback) {
                boolean usuarioValido = MessageDigest.isEqual(
                        usuarioSoap.getBytes(StandardCharsets.UTF_8),
                        String.valueOf(passwordCallback.getIdentifier()).getBytes(StandardCharsets.UTF_8));
                if (usuarioValido) {
                    passwordCallback.setPassword(passwordSoap);
                    return;
                }
                throw new IOException("Usuario SOAP no autorizado");
            }
        }
    }
}

