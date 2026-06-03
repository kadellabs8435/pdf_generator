package com.bankstatement.dto;

import com.bankstatement.entity.Role;

public record UserResponse(
        String id,
        String name,
        String email,
        String mobile,
        Role role,
        boolean active
) {}
