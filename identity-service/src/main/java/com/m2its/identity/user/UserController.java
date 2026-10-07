package com.m2its.identity.user;

import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.security.Roles;
import com.m2its.common.web.PageResponse;
import com.m2its.identity.user.UserDtos.CreateUserRequest;
import com.m2its.identity.user.UserDtos.UpdateUserRequest;
import com.m2its.identity.user.UserDtos.UserResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize(Roles.CAN_MANAGE)
    public PageResponse<UserResponse> list(@RequestParam(required = false) String orgId,
                                           @PageableDefault(size = 20, sort = "name", direction = Sort.Direction.ASC)
                                           Pageable pageable) {
        return service.list(AuthenticatedUser.current(), orgId, pageable);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable String id) {
        return service.get(AuthenticatedUser.current(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_MANAGE)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return service.create(AuthenticatedUser.current(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_MANAGE)
    public UserResponse update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest request) {
        return service.update(AuthenticatedUser.current(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_MANAGE)
    public void delete(@PathVariable String id) {
        service.delete(AuthenticatedUser.current(), id);
    }
}
