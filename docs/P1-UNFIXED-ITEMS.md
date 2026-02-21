# P1 Unfixed Items

Last reviewed: 2026-02-20  
Scope: unresolved findings after commits `b68618b` and `fb58121`

## 1) Missing ACL checks on several document read paths
- Severity: High
- Status: Not fixed
- Details: ACL filtering was added to list/update/delete flows, but direct read endpoints can still bypass ACL.
- References:
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:267`
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:394`
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:407`
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:467`

## 2) Dev security profile still permits all requests
- Severity: High
- Status: Not fixed
- Details: `dev` profile still uses `anyRequest().permitAll()`.
- Reference:
  - `src/backend/src/main/java/com/sedin/presales/config/SecurityConfig.java:47`

## 3) JWT audience validation not configured
- Severity: Medium
- Status: Not fixed
- Details: Resource server validates issuer but no audience constraint is configured.
- Reference:
  - `src/backend/src/main/resources/application.yml:21`

## 4) Role/status trust model still relies on JWT claims
- Severity: Medium
- Status: Not fixed
- Details: Admin bypass and role inference rely on token claims; DB `users.status` (`INACTIVE`) is not enforced on request path.
- References:
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:199`
  - `src/backend/src/main/java/com/sedin/presales/config/UserPrincipal.java:23`
  - `src/backend/src/main/java/com/sedin/presales/application/service/AclService.java:135`

## 5) Upload failure branches still lack test coverage
- Severity: Low
- Status: Not fixed
- Details: I/O failure branches for blob upload are not explicitly tested.
- References:
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:164`
  - `src/backend/src/main/java/com/sedin/presales/application/service/DocumentService.java:356`
  - `src/backend/src/test/java/com/sedin/presales/application/service/DocumentServiceTest.java:124`
