package com.saurabh.taskcollab.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.saurabh.taskcollab.dto.LoginRequest;
import com.saurabh.taskcollab.dto.RegisterRequest;
import com.saurabh.taskcollab.entity.Board;
import com.saurabh.taskcollab.entity.TaskList;
import com.saurabh.taskcollab.entity.User;
import com.saurabh.taskcollab.exception.ResourceNotFoundException;
import com.saurabh.taskcollab.repository.BoardRepository;
import com.saurabh.taskcollab.repository.TaskListRepository;
import com.saurabh.taskcollab.repository.UserRepository;
import com.saurabh.taskcollab.service.JwtService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final BoardRepository boardRepository;
    private final TaskListRepository taskListRepository;

    public AuthController(UserRepository userRepository,
                          BCryptPasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          BoardRepository boardRepository,
                          TaskListRepository taskListRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.boardRepository = boardRepository;
        this.taskListRepository = taskListRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<?> signup(@Valid @RequestBody RegisterRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        if (userRepository.findByEmail(email).isPresent()) {
            return ResponseEntity.badRequest().body("Email already registered");
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));

        User savedUser = userRepository.save(user);

        Board board = new Board();
        board.setName("My Board");
        board.setOwner(savedUser);

        Board savedBoard = boardRepository.save(board);

        TaskList todo = new TaskList();
        todo.setName("To Do");
        todo.setPosition(1);
        todo.setBoard(savedBoard);

        TaskList inProgress = new TaskList();
        inProgress.setName("In Progress");
        inProgress.setPosition(2);
        inProgress.setBoard(savedBoard);

        TaskList done = new TaskList();
        done.setName("Done");
        done.setPosition(3);
        done.setBoard(savedBoard);

        taskListRepository.saveAll(List.of(todo, inProgress, done));

        return ResponseEntity.ok("User registered successfully");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        String email = request.getEmail().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body("Invalid password");
        }

        String token = jwtService.generateToken(email);

        return ResponseEntity.ok(token);
    }
}
