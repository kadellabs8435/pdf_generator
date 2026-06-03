package com.bankstatement.service.dashboard;

import com.bankstatement.dto.ActivityItemResponse;
import com.bankstatement.dto.DashboardStatsResponse;
import com.bankstatement.entity.StatementStatus;
import com.bankstatement.repository.StatementRepository;
import com.bankstatement.repository.UserRepository;
import com.bankstatement.service.admin.ActivityLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final StatementRepository statementRepository;
    private final ActivityLogService activityLogService;

    public DashboardStatsResponse getStats() {
        return new DashboardStatsResponse(
                userRepository.countByActiveTrue(),
                statementRepository.countByStatus(StatementStatus.GENERATED)
                        + statementRepository.countByStatus(StatementStatus.APPROVED)
                        + statementRepository.countByStatus(StatementStatus.PREVIEWED),
                statementRepository.countByStatus(StatementStatus.DRAFT),
                statementRepository.countByStatus(StatementStatus.APPROVED)
        );
    }

    public Page<ActivityItemResponse> getRecentActivity(int page, int size) {
        return activityLogService.getRecent(PageRequest.of(page, size));
    }
}
