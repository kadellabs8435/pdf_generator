package com.bankstatement.service.user;

import com.bankstatement.dto.UserRequest;
import com.bankstatement.dto.UserResponse;
import com.bankstatement.entity.User;
import com.bankstatement.exception.ApiException;
import com.bankstatement.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(this::toResponse).toList();
    }

    public UserResponse create(UserRequest request) {
        if (request.email() != null && userRepository.findByEmail(request.email()).isPresent()) {
            throw new ApiException("Email already exists", HttpStatus.CONFLICT.value());
        }
        if (request.mobile() != null && userRepository.findByMobile(request.mobile()).isPresent()) {
            throw new ApiException("Mobile already exists", HttpStatus.CONFLICT.value());
        }

        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .mobile(request.mobile())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .active(request.active())
                .build();

        return toResponse(userRepository.save(user));
    }

    public UserResponse update(String id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND.value()));

        user.setName(request.name());
        user.setEmail(request.email());
        user.setMobile(request.mobile());
        user.setRole(request.role());
        user.setActive(request.active());

        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return toResponse(userRepository.save(user));
    }

    public void delete(String id) {
        if (!userRepository.existsById(id)) {
            throw new ApiException("User not found", HttpStatus.NOT_FOUND.value());
        }
        userRepository.deleteById(id);
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getMobile(), user.getRole(), user.isActive());
    }
}
