package com.okerry.okerry_diagram.git.service;

import com.okerry.okerry_diagram.git.exception.InvalidRepositoryUrlException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RepositoryUrlValidatorTests {

    private final RepositoryUrlValidator validator = new RepositoryUrlValidator();

    @Test
    void acceptsPublicHttpsRepositoryUrl() {
        assertEquals("https://github.com/example/sample.git",
                validator.validate("https://github.com/example/sample.git"));
    }

    @Test
    void rejectsNonHttpRepositoryUrl() {
        assertThrows(InvalidRepositoryUrlException.class,
                () -> validator.validate("git@github.com:example/sample.git"));
    }

    @Test
    void rejectsUrlWithCredentials() {
        assertThrows(InvalidRepositoryUrlException.class,
                () -> validator.validate("https://token@github.com/example/sample.git"));
    }

}
