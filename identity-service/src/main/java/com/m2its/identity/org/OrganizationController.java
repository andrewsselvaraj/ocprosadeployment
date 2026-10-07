package com.m2its.identity.org;

import java.util.List;

import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.security.Roles;
import com.m2its.identity.org.OrganizationDtos.CreateOrganizationRequest;
import com.m2its.identity.org.OrganizationDtos.OrganizationResponse;
import com.m2its.identity.org.OrganizationDtos.UpdateOrganizationRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orgs")
public class OrganizationController {

    private final OrganizationService service;

    public OrganizationController(OrganizationService service) {
        this.service = service;
    }

    @GetMapping
    public List<OrganizationResponse> list() {
        return service.list(AuthenticatedUser.current());
    }

    @GetMapping("/{id}")
    public OrganizationResponse get(@PathVariable String id) {
        return service.get(AuthenticatedUser.current(), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.IS_SUPERADMIN)
    public OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request) {
        return service.create(AuthenticatedUser.current(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.IS_SUPERADMIN)
    public OrganizationResponse update(@PathVariable String id, @Valid @RequestBody UpdateOrganizationRequest request) {
        return service.update(AuthenticatedUser.current(), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.IS_SUPERADMIN)
    public void delete(@PathVariable String id) {
        service.delete(AuthenticatedUser.current(), id);
    }
}
