# UserSetupApplication — RBAC Developer Manual

**Version:** 1.0  
**Stack:** Spring Boot 4.0.1 · Spring Security 7.0.2 · PostgreSQL 13 · JWT (jjwt 0.11.5)  
**Base URL:** `http://localhost:8082`

---

## Table of Contents

1. [Overview of RBAC](#1-overview-of-rbac)
2. [Architecture](#2-architecture)
3. [Authentication](#3-authentication)
4. [Managing Users](#4-managing-users)
5. [Managing Roles](#5-managing-roles)
6. [Managing Permissions](#6-managing-permissions)
7. [API Endpoint Reference](#7-api-endpoint-reference)

---

## 1. Overview of RBAC

Role-Based Access Control (RBAC) is a security model that restricts system access based on the roles assigned to users. In UserSetupApplication, every action is guarded by a named permission. Permissions are grouped into roles, and roles are assigned to users.

### Key Concepts

**Permission** — A named action a user can perform. Example: `create_user`, `view_invoice`, `delete_role`. Permissions can be enabled or disabled independently.

**Role** — A named collection of permissions. Example: `Accountant`, `Super Admin`, `Manager`. Roles can be enabled or disabled. Disabling a role removes all its permissions from assigned users at runtime — without deleting any data.

**User** — A system user assigned one or more roles. Users can also have direct permissions assigned outside of roles.

### How Permission Checks Work

Every protected endpoint is annotated with `@RequirePermission`. Before the request reaches the controller, the `PermissionInterceptor` checks if the current user has the required permission by:

1. Checking the user's **direct permissions** (only ACTIVE ones)
2. Checking permissions from the user's **ACTIVE roles** (only ACTIVE permissions within those roles)

If either check passes, access is granted. Otherwise a `403 Forbidden` is returned.

```
Request → JwtAuthenticationFilter → PermissionInterceptor → Controller
              (validates token)        (checks permission)
```

### Data Model

```
users
  └── user_roles (join) ──→ roles
                               └── role_permissions (join) ──→ permissions
  └── user_permissions (join) ──→ permissions
```

---

## 2. Architecture

### Security Filter Chain

| Component | Responsibility |
|---|---|
| `JwtAuthenticationFilter` | Extracts Bearer token, validates it, loads `UserPrincipal` into `SecurityContext` |
| `CustomUserDetailsService` | Loads user by ID from DB, builds authorities |
| `UserPrincipal` | Wraps `User` entity, provides authorities (roles + permissions) |
| `PermissionInterceptor` | AOP aspect — intercepts `@RequirePermission`, delegates to `PermissionService` |
| `PermissionService` | Checks current user's permissions against required permission, respects ACTIVE/DISABLED status |
| `SecurityUtils` | Extracts `UserPrincipal` from `SecurityContextHolder` |

### Token Strategy

| Token | Lifetime | Storage | Purpose |
|---|---|---|---|
| Access Token | 15 minutes | Client memory | Sent as `Authorization: Bearer <token>` |
| Refresh Token | 7 days | DB + httpOnly cookie | Used to obtain new access token |

---

## 3. Authentication

### Login

**POST** `/api/auth/login`

Request:
```json
{
  "email": "admin@usersetupapplication.com",
  "password": "Admin@1234"
}
```

Response:
```json
{
  "accessToken": "eyJhbGci...",
  "userId": "1",
  "email": "admin@usersetupapplication.com",
  "firstName": "Admin",
  "lastName": "User",
  "roles": [
    {
      "name": "Super Admin",
      "permissions": ["create_user", "delete_user", "view_invoice"]
    }
  ],
  "directPermissions": ["export_report"]
}
```

- The `refreshToken` is set automatically as an `httpOnly` cookie.
- The `accessToken` must be sent as a `Bearer` token on all protected requests.

### Logout

**POST** `/api/auth/logout`

- No body required.
- Revokes the refresh token in the database.
- Clears the `refreshToken` cookie.

### Refresh Access Token

**POST** `/api/auth/refresh`

- No body required.
- Reads the `refreshToken` from the httpOnly cookie.
- Returns a new `accessToken`.

Response:
```json
{
  "accessToken": "eyJhbGci..."
}
```

---

## 4. Managing Users

All user endpoints require a valid Bearer token and the corresponding permission.

### Create User

**POST** `/users`  
**Permission required:** `create_user`

```json
{
  "email": "john@example.com",
  "password": "Pass@1234",
  "firstName": "John",
  "lastName": "Doe",
  "roleIds": [1],
  "permissionIds": [3, 5]
}
```

- `roleIds` — IDs from the `roles` table. At least one recommended.
- `permissionIds` — Direct permissions outside of roles. Can be empty `[]`.

### Get All Users

**GET** `/users?page=0&size=20&sort=firstName,asc`  
**Permission required:** `view_users`

Returns a paginated list of users with their roles and permissions.

### Get User by ID

**GET** `/users/{id}`  
**Permission required:** `view_user`

Returns the full user profile including roles (with permissions) and direct permissions.

### Update User

**PUT** `/users/{id}`  
**Permission required:** `update_user`

```json
{
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane@example.com",
  "status": "ACTIVE",
  "roleIds": [1, 2],
  "permissionIds": []
}
```

All fields are optional — only include what you want to change.

### Toggle User Status (Enable / Disable)

**DELETE** `/users/{id}`  
**Permission required:** `delete_user`

- If the user is `ACTIVE` → sets to `DISABLED`
- If the user is `DISABLED` → sets to `ACTIVE`
- A disabled user cannot log in.
- Roles and permissions are **not** removed — they are preserved for when the user is reactivated.

---

## 5. Managing Roles

### Get All Roles

**GET** `/roles`  
**Permission required:** `view_role`

Returns all roles with their assigned permissions and status.

### Get Role by ID

**GET** `/roles/{id}`  
**Permission required:** `view_role`

### Create Role

**POST** `/roles`  
**Permission required:** `create_role`

```json
{
  "name": "Accountant",
  "guardName": "web",
  "permissionIds": [1, 2, 3]
}
```

- `guardName` is optional — defaults to `"web"`.
- `permissionIds` can be empty `[]`.

### Update Role

**PUT** `/roles/{id}`  
**Permission required:** `edit_role`

```json
{
  "name": "Senior Accountant",
  "permissionIds": [1, 2, 3, 4]
}
```

Both fields are optional. Sending `permissionIds` replaces the existing permissions entirely.

### Toggle Role Status (Enable / Disable)

**DELETE** `/roles/{id}`  
**Permission required:** `delete_role`

- If the role is `ACTIVE` → sets to `DISABLED`
- If the role is `DISABLED` → sets to `ACTIVE`

**Important:** When a role is disabled:
- All users assigned that role **lose all permissions** attached to it immediately
- The permissions are **not deleted** — they are simply ignored at runtime
- Re-enabling the role restores all permissions instantly with no reassignment needed
- Hard deletion of roles is intentionally not supported to prevent fraud and maintain audit integrity

---

## 6. Managing Permissions

### Get All Permissions

**GET** `/permissions`  
**Permission required:** `view_role`

Returns all permissions with their status.

### Toggle Permission Status (Enable / Disable)

**DELETE** `/permissions/{id}`  
**Permission required:** `view_role`

- If the permission is `ACTIVE` → sets to `DISABLED`
- If the permission is `DISABLED` → sets to `ACTIVE`

**Important:** When a permission is disabled:
- All users lose that permission regardless of whether it is assigned directly or through a role
- The assignment is **not removed** — it is ignored at runtime
- Re-enabling restores it instantly

---

## 7. API Endpoint Reference

### Authentication

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/api/auth/login` | Public | Login and get tokens |
| POST | `/api/auth/logout` | Authenticated | Revoke refresh token |
| POST | `/api/auth/refresh` | Public (cookie) | Get new access token |

### Users

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| POST | `/users` | `create_user` | Create new user |
| GET | `/users` | `view_users` | Get all users (paginated) |
| GET | `/users/{id}` | `view_user` | Get user by ID |
| PUT | `/users/{id}` | `update_user` | Update user |
| DELETE | `/users/{id}` | `delete_user` | Toggle user status |

### Roles

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| GET | `/roles` | `view_role` | Get all roles |
| GET | `/roles/{id}` | `view_role` | Get role by ID |
| POST | `/roles` | `create_role` | Create new role |
| PUT | `/roles/{id}` | `edit_role` | Update role |
| DELETE | `/roles/{id}` | `delete_role` | Toggle role status |

### Permissions

| Method | Endpoint | Permission | Description |
|---|---|---|---|
| GET | `/permissions` | `view_role` | Get all permissions |
| DELETE | `/permissions/{id}` | `view_role` | Toggle permission status |

---

## Error Responses

All errors follow this structure:

```json
{
  "success": false,
  "message": "Error description here",
  "content": null
}
```

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Invalid input or validation failure |
| `401 Unauthorized` | Token expired or invalid |
| `403 Forbidden` | Valid token but missing required permission |
| `404 Not Found` | Resource not found |
| `409 Conflict` | Duplicate email or role name |
| `500 Internal Server Error` | Unexpected server error |

---

*UserSetupApplication RBAC Manual — v1.0*
