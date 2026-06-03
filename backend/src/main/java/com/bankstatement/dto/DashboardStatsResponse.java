package com.bankstatement.dto;

public record DashboardStatsResponse(
        long totalUsers,
        long totalGeneratedPdfs,
        long totalDrafts,
        long totalApproved
) {}
