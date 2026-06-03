package com.bankstatement.dto;

import com.bankstatement.entity.Role;

public record UserRequest(
        String name,
        String email,
        String mobile,
        String password,
        Role role,
        boolean active
) {}
