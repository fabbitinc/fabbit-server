package com.fabbitinc.server.application.auth.port;

public interface TurnstilePort {

    void verify(String token);
}
