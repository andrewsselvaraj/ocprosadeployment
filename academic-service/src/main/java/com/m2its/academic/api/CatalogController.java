package com.m2its.academic.api;

import static com.m2its.common.security.AuthenticatedUser.current;

import java.util.List;

import com.m2its.academic.api.AcademicDtos.ChapterResponse;
import com.m2its.academic.api.AcademicDtos.ClassResponse;
import com.m2its.academic.api.AcademicDtos.ClassTree;
import com.m2its.academic.api.AcademicDtos.CreateChapterRequest;
import com.m2its.academic.api.AcademicDtos.CreateClassRequest;
import com.m2its.academic.api.AcademicDtos.CreateSubjectRequest;
import com.m2its.academic.api.AcademicDtos.RenameRequest;
import com.m2its.academic.api.AcademicDtos.SubjectResponse;
import com.m2its.academic.service.CatalogService;
import com.m2its.common.security.Roles;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Reads are open to every authenticated member of the organisation (students need them to
 * pick an exam); writes need a content editor role (ADMIN, DATA_ENTRY or SUPERADMIN).
 */
@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService service;

    public CatalogController(CatalogService service) {
        this.service = service;
    }

    // ---- classes -------------------------------------------------------------------------

    @GetMapping("/classes")
    public List<ClassResponse> listClasses(@RequestParam(required = false) String orgId) {
        return service.listClasses(current(), orgId);
    }

    @GetMapping("/classes/{id}")
    public ClassResponse getClass(@PathVariable String id) {
        return service.getClass(current(), id);
    }

    @GetMapping("/classes/{id}/tree")
    public ClassTree getClassTree(@PathVariable String id) {
        return service.getClassTree(current(), id);
    }

    @GetMapping("/classes/{id}/subjects")
    public List<SubjectResponse> listSubjects(@PathVariable String id) {
        return service.listSubjects(current(), id);
    }

    @PostMapping("/classes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public ClassResponse createClass(@Valid @RequestBody CreateClassRequest request) {
        return service.createClass(current(), request);
    }

    @PutMapping("/classes/{id}")
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public ClassResponse renameClass(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameClass(current(), id, request.name());
    }

    @DeleteMapping("/classes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public void deleteClass(@PathVariable String id) {
        service.deleteClass(current(), id);
    }

    // ---- subjects ------------------------------------------------------------------------

    @GetMapping("/subjects/{id}")
    public SubjectResponse getSubject(@PathVariable String id) {
        return service.getSubject(current(), id);
    }

    @GetMapping("/subjects/{id}/chapters")
    public List<ChapterResponse> listChapters(@PathVariable String id) {
        return service.listChapters(current(), id);
    }

    @PostMapping("/subjects")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public SubjectResponse createSubject(@Valid @RequestBody CreateSubjectRequest request) {
        return service.createSubject(current(), request);
    }

    @PutMapping("/subjects/{id}")
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public SubjectResponse renameSubject(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameSubject(current(), id, request.name());
    }

    @DeleteMapping("/subjects/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public void deleteSubject(@PathVariable String id) {
        service.deleteSubject(current(), id);
    }

    // ---- chapters ------------------------------------------------------------------------

    @GetMapping("/chapters/{id}")
    public ChapterResponse getChapter(@PathVariable String id) {
        return service.getChapter(current(), id);
    }

    @PostMapping("/chapters")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public ChapterResponse createChapter(@Valid @RequestBody CreateChapterRequest request) {
        return service.createChapter(current(), request);
    }

    @PutMapping("/chapters/{id}")
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public ChapterResponse renameChapter(@PathVariable String id, @Valid @RequestBody RenameRequest request) {
        return service.renameChapter(current(), id, request.name());
    }

    @DeleteMapping("/chapters/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_EDIT_CONTENT)
    public void deleteChapter(@PathVariable String id) {
        service.deleteChapter(current(), id);
    }
}
