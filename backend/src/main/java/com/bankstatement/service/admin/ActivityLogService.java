package com.bankstatement.service.admin;

import com.bankstatement.dto.ActivityItemResponse;
import com.bankstatement.entity.ActivityLog;
import com.bankstatement.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public void log(String userId, String userName, String action, String details, String entityType, String entityId) {
        ActivityLog log = ActivityLog.builder()
                .userId(userId)
                .userName(userName)
                .action(action)
                .details(details)
                .entityType(entityType)
                .entityId(entityId)
                .build();
        activityLogRepository.save(log);
    }

    public Page<ActivityItemResponse> getRecent(Pageable pageable) {
        return activityLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(log -> new ActivityItemResponse(
                        log.getId(),
                        log.getUserName(),
                        log.getAction(),
                        log.getDetails(),
                        log.getCreatedAt()
                ));
    }
}
