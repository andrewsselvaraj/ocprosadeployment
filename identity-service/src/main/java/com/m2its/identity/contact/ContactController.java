package com.m2its.identity.contact;

import java.time.LocalDateTime;

import com.m2its.common.jpa.Ids;
import com.m2its.common.jpa.RecordStatus;
import com.m2its.common.security.Roles;
import com.m2its.common.web.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST is public (path is in m2its.security.public-paths); GET is still protected by
 * method security, so anonymous callers get 403 there.
 */
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactMessageRepository messages;

    public ContactController(ContactMessageRepository messages) {
        this.messages = messages;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContactResponse submit(@Valid @RequestBody ContactRequest request) {
        ContactMessage saved = messages.save(new ContactMessage(Ids.newId(), request.name(), request.email(),
                request.subject(), request.message()));
        return ContactResponse.from(saved);
    }

    @GetMapping
    @PreAuthorize(Roles.IS_SUPERADMIN)
    public PageResponse<ContactResponse> list(
            @PageableDefault(size = 20, sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return PageResponse.of(messages.findByStatus(RecordStatus.ACTIVE, pageable), ContactResponse::from);
    }

    public record ContactRequest(@NotBlank @Size(max = 50) String name,
                                 @NotBlank @Email @Size(max = 50) String email,
                                 @NotBlank @Size(max = 50) String subject,
                                 @NotBlank @Size(max = 500) String message) {
    }

    public record ContactResponse(String id, String name, String email, String subject, String message,
                                  LocalDateTime receivedAt) {

        static ContactResponse from(ContactMessage m) {
            return new ContactResponse(m.getId(), m.getName(), m.getEmail(), m.getSubject(), m.getMessage(),
                    m.getReceivedAt());
        }
    }
}
