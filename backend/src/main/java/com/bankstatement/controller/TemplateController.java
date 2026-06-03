package com.bankstatement.controller;

import com.bankstatement.dto.BankTemplateResponse;
import com.bankstatement.service.template.TemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @GetMapping
    public List<BankTemplateResponse> list() {
        return templateService.getActiveTemplates();
    }

    @GetMapping("/{code}")
    public BankTemplateResponse get(@PathVariable String code) {
        return templateService.getByCode(code);
    }
}
