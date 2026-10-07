package com.m2its.common.jpa;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Transient;

import org.springframework.data.domain.Persistable;

/**
 * Base class for entities whose String id is assigned by the application (see {@link Ids}).
 *
 * Without this, Spring Data's save() sees a non-null id, assumes the row exists and calls
 * EntityManager.merge(), which costs an extra SELECT per insert. Implementing Persistable
 * lets save() call persist() for new instances.
 */
@MappedSuperclass
public abstract class AssignedIdEntity implements Persistable<String> {

    @Transient
    private boolean newEntity = true;

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }
}
