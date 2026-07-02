package br.com.setis.desafiojava.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TokenRequest(@Email String email, @NotBlank String password) {}
