package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.git.exception.InvalidRepositoryUrlException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Component
public class RepositoryUrlValidator {

    public String validate(String repositoryUrl) {
        if (repositoryUrl == null || repositoryUrl.isBlank()) {
            throw new InvalidRepositoryUrlException("repositoryUrl is required.");
        }

        try {
            URI uri = new URI(repositoryUrl.trim());

            if (!"http".equalsIgnoreCase(uri.getScheme())
                    && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new InvalidRepositoryUrlException("Only HTTP/HTTPS Git URLs are allowed.");
            }

            if (uri.getHost() == null || uri.getHost().isBlank() || uri.getUserInfo() != null) {
                throw new InvalidRepositoryUrlException("A valid public Git repository URL is required.");
            }

            return uri.toString();
        } catch (URISyntaxException exception) {
            throw new InvalidRepositoryUrlException("A valid public Git repository URL is required.");
        }
    }

}
