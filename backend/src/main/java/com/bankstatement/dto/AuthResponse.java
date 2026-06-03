package com.bankstatement.dto;

import com.bankstatement.entity.Role;

public record AuthResponse(
        String token,
        String userId,
        String name,
        String email,
        String mobile,
        Role role
) {}
