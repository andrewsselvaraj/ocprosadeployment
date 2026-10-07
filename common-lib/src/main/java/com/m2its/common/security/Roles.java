package com.m2its.common.security;

/** Role names carried in the JWT "roles" claim (Spring adds the ROLE_ prefix). */
public final class Roles {

    /** Platform administrator (legacy admin_info table). Not bound to an organisation. */
    public static final String SUPERADMIN = "SUPERADMIN";
    /** Organisation administrator (user_info.user_role = 'Admin'). */
    public static final String ADMIN = "ADMIN";
    /** Content author (user_info.user_role = 'Data Entry Operator'): edits catalogue and questions only. */
    public static final String DATA_ENTRY = "DATA_ENTRY";
    /** Student / exam taker (user_info.user_role = 'User'). */
    public static final String USER = "USER";

    /** SpEL for @PreAuthorize on endpoints that manage the organisation (users, results). */
    public static final String CAN_MANAGE = "hasAnyRole('ADMIN','SUPERADMIN')";
    /** SpEL for @PreAuthorize on endpoints that edit exam content (classes, subjects, questions). */
    public static final String CAN_EDIT_CONTENT = "hasAnyRole('ADMIN','SUPERADMIN','DATA_ENTRY')";
    public static final String IS_SUPERADMIN = "hasRole('SUPERADMIN')";

    private Roles() {
    }
}
