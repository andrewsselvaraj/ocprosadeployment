package com.m2its.academic.api;

import java.time.LocalDateTime;
import java.util.List;

import com.m2its.academic.domain.Chapter;
import com.m2its.academic.domain.SchoolClass;
import com.m2its.academic.domain.Subject;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AcademicDtos {

    private AcademicDtos() {
    }

    /** orgId is only used (and required) when a SUPERADMIN creates the class. */
    public record CreateClassRequest(String orgId, @NotBlank @Size(max = 50) String name) {
    }

    public record CreateSubjectRequest(@NotBlank String classId, @NotBlank @Size(max = 50) String name) {
    }

    public record CreateChapterRequest(@NotBlank String subjectId, @NotBlank @Size(max = 50) String name) {
    }

    public record RenameRequest(@NotBlank @Size(max = 50) String name) {
    }

    public record ClassResponse(String id, String orgId, String name, LocalDateTime updatedAt) {
        public static ClassResponse from(SchoolClass c) {
            return new ClassResponse(c.getId(), c.getOrgId(), c.getName(), c.getUpdatedAt());
        }
    }

    public record SubjectResponse(String id, String orgId, String classId, String name, LocalDateTime updatedAt) {
        public static SubjectResponse from(Subject s) {
            return new SubjectResponse(s.getId(), s.getOrgId(), s.getSchoolClass().getId(), s.getName(),
                    s.getUpdatedAt());
        }
    }

    public record ChapterResponse(String id, String orgId, String classId, String subjectId, String name,
                                  LocalDateTime updatedAt) {
        public static ChapterResponse from(Chapter c) {
            return new ChapterResponse(c.getId(), c.getOrgId(), c.getClassId(), c.getSubject().getId(), c.getName(),
                    c.getUpdatedAt());
        }
    }

    /** Whole catalogue of one class: class -> subjects -> chapters. */
    public record ClassTree(String id, String name, List<SubjectNode> subjects) {
    }

    public record SubjectNode(String id, String name, List<NamedRef> chapters) {
    }

    public record NamedRef(String id, String name) {
    }
}
