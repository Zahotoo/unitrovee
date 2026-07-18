package com.unitrovee.user;

import com.unitrovee.user.dto.CurrentUserResponse;

public interface UserService {

    CurrentUserResponse getCurrentUser(String email);
}
