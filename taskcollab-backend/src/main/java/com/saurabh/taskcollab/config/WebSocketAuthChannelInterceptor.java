package com.saurabh.taskcollab.config;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.exception.ResourceNotFoundException;
import com.saurabh.taskcollab.repository.BoardRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.UserRepository;
import com.saurabh.taskcollab.service.BoardAccessService;
import com.saurabh.taskcollab.service.JwtService;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern LIST_TOPIC_PATTERN = Pattern.compile("/topic/list/(\\d+)");
    private static final Pattern BOARD_TOPIC_PATTERN = Pattern.compile("/topic/board/(\\d+)(?:/(meta|activity))?");
    private static final Pattern USER_BOARDS_TOPIC_PATTERN = Pattern.compile("/topic/boards/([a-z0-9_]+)");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final TaskListRepository taskListRepository;
    private final BoardRepository boardRepository;
    private final BoardAccessService boardAccessService;

    public WebSocketAuthChannelInterceptor(JwtService jwtService,
                                           UserRepository userRepository,
                                           TaskListRepository taskListRepository,
                                           BoardRepository boardRepository,
                                           BoardAccessService boardAccessService) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.taskListRepository = taskListRepository;
        this.boardRepository = boardRepository;
        this.boardAccessService = boardAccessService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(UNAUTHORIZED, "Missing Authorization header");
        }

        String token = authHeader.substring(7);
        String email = jwtService.extractEmail(token).toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid websocket token"));

        if (!jwtService.isTokenValid(token, email)) {
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid websocket token");
        }

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        user.getEmail(),
                        null,
                        List.of(() -> "ROLE_USER")
                );

        accessor.setUser(authentication);
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();

        if (principal == null) {
            throw new ResponseStatusException(UNAUTHORIZED, "Unauthorized websocket subscription");
        }

        User user = userRepository.findByEmail(principal.getName().toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }

        Matcher userBoardsMatcher = USER_BOARDS_TOPIC_PATTERN.matcher(destination);
        if (userBoardsMatcher.matches()) {
            String destinationUserKey = userBoardsMatcher.group(1);
            String currentUserKey = sanitizeEmailForTopic(user.getEmail());

            if (!destinationUserKey.equals(currentUserKey)) {
                throw new ResponseStatusException(FORBIDDEN, "Not authorized");
            }
            return;
        }

        Matcher boardMatcher = BOARD_TOPIC_PATTERN.matcher(destination);
        if (boardMatcher.matches()) {
            Long boardId = Long.valueOf(boardMatcher.group(1));
            Board board = boardRepository.findById(boardId)
                    .orElseThrow(() -> new ResourceNotFoundException("Board not found"));

            if (!boardAccessService.isBoardUser(board, user)) {
                throw new ResponseStatusException(FORBIDDEN, "Not authorized");
            }
            return;
        }

        Matcher listMatcher = LIST_TOPIC_PATTERN.matcher(destination);
        if (listMatcher.matches()) {
            Long listId = Long.valueOf(listMatcher.group(1));
            TaskList list = taskListRepository.findById(listId)
                    .orElseThrow(() -> new ResourceNotFoundException("List not found"));

            if (!boardAccessService.isBoardUser(list.getBoard(), user)) {
                throw new ResponseStatusException(FORBIDDEN, "Not authorized");
            }
            return;
        }

        if (destination.startsWith("/topic/")) {
            throw new ResponseStatusException(FORBIDDEN, "Unsupported websocket destination");
        }
    }

    private String sanitizeEmailForTopic(String email) {
        return email.toLowerCase().replaceAll("[^a-z0-9]", "_");
    }
}
