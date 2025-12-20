# Bootstrap Guide - Creating First Incharge

## Problem
The system requires an INCHARGE to exist before other operations can happen. But how do you create the first INCHARGE if only an INCHARGE can create/promote others?

## Solution
Two methods are provided:

### Method 1: Automatic Bootstrap (Recommended)
When you create the **first sewadar** in the system (when no incharge exists), it automatically becomes an INCHARGE.

**Steps:**
1. Check if system needs bootstrap:
```bash
GET /api/bootstrap/status
```

2. Create first sewadar (will automatically become incharge):
```bash
POST /api/sewadars
{
  "firstName": "Admin",
  "lastName": "Incharge",
  "mobile": "9876543210",
  "password": "admin123",
  "dept": "Administration",
  "role": "INCHARGE"  // Optional - will be INCHARGE anyway if first user
}
```

### Method 2: Explicit Bootstrap Endpoint
Use the bootstrap endpoint to explicitly create the first incharge.

**Steps:**
1. Check bootstrap status:
```bash
GET /api/bootstrap/status
```

Response if no incharge exists:
```json
{
  "needsBootstrap": true,
  "hasIncharge": false,
  "message": "No incharge found. Please create the first incharge."
}
```

2. Create first incharge:
```bash
POST /api/bootstrap/create-incharge
{
  "firstName": "Admin",
  "lastName": "Incharge",
  "mobile": "9876543210",
  "password": "admin123",  // REQUIRED for incharge
  "dept": "Administration",
  "address1": "123 Admin Street",
  "email": "admin@example.com"
}
```

**Important:** This endpoint only works if NO incharge exists. Once an incharge exists, you must use the promote endpoint.

## After Bootstrap

Once the first incharge is created:

1. **Login as incharge:**
```bash
POST /api/auth/login
{
  "mobile": "9876543210",
  "password": "admin123"
}
```

2. **Create other sewadars** (they will be SEWADAR by default)

3. **Promote sewadars to incharge** (if needed):
```bash
POST /api/sewadars/{sewadarId}/promote?inchargeId={inchargeId}
Authorization: Bearer <token>
```

## Security Notes

- First incharge creation is only allowed when system is empty
- Password is REQUIRED for incharge creation
- After first incharge exists, only existing incharge can promote others
- All passwords are encrypted with BCrypt

## Quick Start Script

```bash
# 1. Check if bootstrap needed
curl http://localhost:8080/api/bootstrap/status

# 2. Create first incharge
curl -X POST http://localhost:8080/api/bootstrap/create-incharge \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "Incharge",
    "mobile": "9876543210",
    "password": "admin123",
    "dept": "Administration"
  }'

# 3. Login
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "mobile": "9876543210",
    "password": "admin123"
  }'
```

