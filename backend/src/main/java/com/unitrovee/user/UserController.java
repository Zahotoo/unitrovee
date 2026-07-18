package com.unitrovee.user;

import com.unitrovee.common.ApiResponse;
import com.unitrovee.user.dto.CurrentUserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> getCurrentUser(Authentication authentication) {
        CurrentUserResponse response = userService.getCurrentUser(authentication.getName());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
