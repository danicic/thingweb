package de.webthing.binding.auth;

/**
 * Created by Johannes on 05.10.2015.
 */
public interface TokenVerifier {
    boolean isAuthorized(String jwt);
}
