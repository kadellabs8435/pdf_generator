package com.bankstatement.controller;

import com.bankstatement.dto.*;
import com.bankstatement.service.admin.ActivityLogService;
import com.bankstatement.service.template.TemplateService;
import com.bankstatement.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final TemplateService templateService;
    private final ActivityLogService activityLogService;

    @GetMapping("/users")
    public List<UserResponse> listUsers() {
        return userService.findAll();
    }

    @PostMapping("/users")
    public UserResponse createUser(@Valid @RequestBody UserRequest request) {
        return userService.create(request);
    }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable String id, @Valid @RequestBody UserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.delete(id);
    }

    @GetMapping("/templates")
    public List<BankTemplateResponse> listTemplates() {
        return templateService.getAllTemplates();
    }

    @PostMapping("/templates")
    public BankTemplateResponse createTemplate(@Valid @RequestBody BankTemplateRequest request) {
        return templateService.create(request);
    }

    @PutMapping("/templates/{id}")
    public BankTemplateResponse updateTemplate(@PathVariable String id, @Valid @RequestBody BankTemplateRequest request) {
        return templateService.update(id, request);
    }

    @DeleteMapping("/templates/{id}")
    public void deleteTemplate(@PathVariable String id) {
        templateService.delete(id);
    }

    @GetMapping("/logs")
    public Page<ActivityItemResponse> logs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return activityLogService.getRecent(PageRequest.of(page, size));
    }
}
