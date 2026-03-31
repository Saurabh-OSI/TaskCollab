package com.saurabh.taskcollab.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.saurabh.taskcollab.dto.ActivityLogResponse;
import com.saurabh.taskcollab.dto.PageResponse;
import com.saurabh.taskcollab.dto.UserSummaryResponse;
import com.saurabh.taskcollab.entity.ActivityLog;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.repository.ActivityLogRepository;

@Service
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public ActivityLogService(ActivityLogRepository activityLogRepository,
                              SimpMessagingTemplate messagingTemplate) {
        this.activityLogRepository = activityLogRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public ActivityLogResponse log(Board board,
                                   User actor,
                                   String action,
                                   String entityType,
                                   Long entityId,
                                   String description) {
        ActivityLog activityLog = new ActivityLog();
        activityLog.setBoard(board);
        activityLog.setUser(actor);
        activityLog.setAction(action);
        activityLog.setEntityType(entityType);
        activityLog.setEntityId(entityId);
        activityLog.setDescription(description);

        ActivityLog savedActivityLog = activityLogRepository.save(activityLog);
        ActivityLogResponse response = map(savedActivityLog);

        Map<String, Object> event = new HashMap<>();
        event.put("type", "CREATED");
        event.put("activity", response);

        messagingTemplate.convertAndSend("/topic/board/" + board.getId() + "/activity", event);

        return response;
    }

    public PageResponse<ActivityLogResponse> getBoardActivity(Board board, int page, int size) {
        return PageResponse.from(
                activityLogRepository.findByBoardIdOrderByCreatedAtDesc(
                        board.getId(),
                        PageRequest.of(page, size)
                ).map(this::map)
        );
    }

    private ActivityLogResponse map(ActivityLog activityLog) {
        return new ActivityLogResponse(
                activityLog.getId(),
                activityLog.getBoard().getId(),
                activityLog.getAction(),
                activityLog.getEntityType(),
                activityLog.getEntityId(),
                activityLog.getDescription(),
                new UserSummaryResponse(
                        activityLog.getUser().getId(),
                        activityLog.getUser().getName(),
                        activityLog.getUser().getEmail()
                ),
                activityLog.getCreatedAt()
        );
    }
}
