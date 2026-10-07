package com.m2its.academic.service;

import static com.m2its.common.jpa.RecordStatus.ACTIVE;
import static com.m2its.common.jpa.RecordStatus.DELETED;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.m2its.academic.api.AcademicDtos.ChapterResponse;
import com.m2its.academic.api.AcademicDtos.ClassResponse;
import com.m2its.academic.api.AcademicDtos.ClassTree;
import com.m2its.academic.api.AcademicDtos.CreateChapterRequest;
import com.m2its.academic.api.AcademicDtos.CreateClassRequest;
import com.m2its.academic.api.AcademicDtos.CreateSubjectRequest;
import com.m2its.academic.api.AcademicDtos.NamedRef;
import com.m2its.academic.api.AcademicDtos.SubjectNode;
import com.m2its.academic.api.AcademicDtos.SubjectResponse;
import com.m2its.academic.domain.Chapter;
import com.m2its.academic.domain.ChapterRepository;
import com.m2its.academic.domain.SchoolClass;
import com.m2its.academic.domain.SchoolClassRepository;
import com.m2its.academic.domain.Subject;
import com.m2its.academic.domain.SubjectRepository;
import com.m2its.common.jpa.Ids;
import com.m2its.common.security.AuthenticatedUser;
import com.m2its.common.web.ApiException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Classes, subjects and chapters. Every read and write checks that the record belongs to
 * the caller's organisation (multi-tenancy), except for SUPERADMIN.
 */
@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final SchoolClassRepository classes;
    private final SubjectRepository subjects;
    private final ChapterRepository chapters;

    public CatalogService(SchoolClassRepository classes, SubjectRepository subjects, ChapterRepository chapters) {
        this.classes = classes;
        this.subjects = subjects;
        this.chapters = chapters;
    }

    // ---- classes -------------------------------------------------------------------------

    public List<ClassResponse> listClasses(AuthenticatedUser caller, String orgId) {
        return classes.findByOrgIdAndStatusOrderByName(caller.resolveOrg(orgId), ACTIVE).stream()
                .map(ClassResponse::from).toList();
    }

    public ClassResponse getClass(AuthenticatedUser caller, String id) {
        return ClassResponse.from(findClass(caller, id));
    }

    @Transactional
    public ClassResponse createClass(AuthenticatedUser caller, CreateClassRequest request) {
        SchoolClass created = new SchoolClass(Ids.newId(), caller.resolveOrg(request.orgId()), request.name());
        return ClassResponse.from(classes.save(created));
    }

    @Transactional
    public ClassResponse renameClass(AuthenticatedUser caller, String id, String name) {
        SchoolClass c = findClass(caller, id);
        c.setName(name);
        return ClassResponse.from(c);
    }

    @Transactional
    public void deleteClass(AuthenticatedUser caller, String id) {
        SchoolClass c = findClass(caller, id);
        if (subjects.existsBySchoolClassIdAndStatus(id, ACTIVE)) {
            throw ApiException.conflict("Class still has active subjects; delete them first");
        }
        c.setStatus(DELETED);
    }

    public ClassTree getClassTree(AuthenticatedUser caller, String classId) {
        SchoolClass c = findClass(caller, classId);
        Map<String, SubjectNode> nodes = new LinkedHashMap<>();
        for (Subject s : subjects.findBySchoolClassIdAndStatusOrderByName(classId, ACTIVE)) {
            nodes.put(s.getId(), new SubjectNode(s.getId(), s.getName(), new ArrayList<>()));
        }
        for (Chapter ch : chapters.findTreeByClass(classId, ACTIVE)) {
            SubjectNode node = nodes.get(ch.getSubject().getId());
            if (node != null) {
                node.chapters().add(new NamedRef(ch.getId(), ch.getName()));
            }
        }
        return new ClassTree(c.getId(), c.getName(), List.copyOf(nodes.values()));
    }

    // ---- subjects ------------------------------------------------------------------------

    public List<SubjectResponse> listSubjects(AuthenticatedUser caller, String classId) {
        findClass(caller, classId);
        return subjects.findBySchoolClassIdAndStatusOrderByName(classId, ACTIVE).stream()
                .map(SubjectResponse::from).toList();
    }

    public SubjectResponse getSubject(AuthenticatedUser caller, String id) {
        return SubjectResponse.from(findSubject(caller, id));
    }

    @Transactional
    public SubjectResponse createSubject(AuthenticatedUser caller, CreateSubjectRequest request) {
        SchoolClass c = findClass(caller, request.classId());
        return SubjectResponse.from(subjects.save(new Subject(Ids.newId(), c, request.name())));
    }

    @Transactional
    public SubjectResponse renameSubject(AuthenticatedUser caller, String id, String name) {
        Subject s = findSubject(caller, id);
        s.setName(name);
        return SubjectResponse.from(s);
    }

    @Transactional
    public void deleteSubject(AuthenticatedUser caller, String id) {
        findSubject(caller, id).setStatus(DELETED);
    }

    // ---- chapters ------------------------------------------------------------------------

    public List<ChapterResponse> listChapters(AuthenticatedUser caller, String subjectId) {
        findSubject(caller, subjectId);
        return chapters.findBySubjectIdAndStatusOrderByName(subjectId, ACTIVE).stream()
                .map(ChapterResponse::from).toList();
    }

    public ChapterResponse getChapter(AuthenticatedUser caller, String id) {
        return ChapterResponse.from(findChapter(caller, id));
    }

    @Transactional
    public ChapterResponse createChapter(AuthenticatedUser caller, CreateChapterRequest request) {
        Subject s = findSubject(caller, request.subjectId());
        return ChapterResponse.from(chapters.save(new Chapter(Ids.newId(), s, request.name())));
    }

    @Transactional
    public ChapterResponse renameChapter(AuthenticatedUser caller, String id, String name) {
        Chapter ch = findChapter(caller, id);
        ch.setName(name);
        return ChapterResponse.from(ch);
    }

    @Transactional
    public void deleteChapter(AuthenticatedUser caller, String id) {
        findChapter(caller, id).setStatus(DELETED);
    }

    // ---- lookups with tenant check -------------------------------------------------------

    private SchoolClass findClass(AuthenticatedUser caller, String id) {
        SchoolClass c = classes.findByIdAndStatus(id, ACTIVE).orElseThrow(() -> ApiException.notFound("Class", id));
        caller.checkOrgAccess(c.getOrgId());
        return c;
    }

    private Subject findSubject(AuthenticatedUser caller, String id) {
        Subject s = subjects.findByIdAndStatus(id, ACTIVE).orElseThrow(() -> ApiException.notFound("Subject", id));
        caller.checkOrgAccess(s.getOrgId());
        return s;
    }

    private Chapter findChapter(AuthenticatedUser caller, String id) {
        Chapter ch = chapters.findByIdAndStatus(id, ACTIVE).orElseThrow(() -> ApiException.notFound("Chapter", id));
        caller.checkOrgAccess(ch.getOrgId());
        return ch;
    }
}
