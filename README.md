# Unitrovee

Unitrovee is a planned full-stack marketplace for verified students across Irish
universities. The goal is to help students sell, swap, or give away useful
second-hand items such as textbooks, furniture, electronics, bikes, kitchen
items, and other campus-life essentials.

```mermaid
flowchart LR
    A["🏫 School email"] --> B["✉️ Register"]
    B --> C["🛡️ Verify identity"]
    C --> D["🔑 Login"]
    D --> E["🛍️ List, swap, sell, or give away"]
```

```mermaid
flowchart TD
    A["Client sends email, password, displayName"] --> B["Validate request body"]
    B -->|Invalid| X["400 VALIDATION_ERROR"]

    B --> C["Normalize email<br/>trim + lowercase"]
    C --> D["Check duplicate email"]
    D -->|Already used| Y["409 EMAIL_ALREADY_EXISTS"]

    D --> E["Extract email domain"]
    E --> F["Find matching active school"]
    F -->|No matching active school| Z["400 UNSUPPORTED_SCHOOL_EMAIL"]

    F --> G["BCrypt-hash password"]
    G --> H["Create unverified STUDENT user"]
    H --> I["201 Created<br/>Safe registration response"]
```

```mermaid
flowchart TD
    A["Client sends email + password"] --> B["Validate and normalize email"]
    B --> C["Spring Security AuthenticationManager"]
    C --> D["Load user from PostgreSQL"]
    D --> E["BCrypt compares password"]

    E -->|Wrong password or unknown email| X["401 INVALID_CREDENTIALS"]
    E -->|Valid credentials| F["JwtService signs access token"]
    F --> G["200 OK<br/>accessToken"]

    G --> H["Client sends<br/>Authorization: Bearer token"]
    H --> I["JwtAuthenticationFilter verifies token"]
    I --> J["Protected endpoint can access authenticated user"]
```