# Test Resources - Quick Reference

## Quick Test Commands

### 1. Create Address
```bash
curl -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"123 Main St","address2":"Apt 1","email":"test@example.com"}'
```

### 2. Create Sewadar
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","dept":"IT","mobile":"1234567890","addressId":1,"remarks":"Test"}'
```

### 3. Create Schedule
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"scheduledPlace":"Center","scheduledDate":"2024-02-15","scheduledTime":"10:00:00","scheduledMedium":"In-Person","attendedById":1}'
```

---

## Sample JSON Payloads

### Address Request
```json
{
  "address1": "123 Main Street",
  "address2": "Apt 4B",
  "email": "address1@example.com"
}
```

### Sewadar Request
```json
{
  "firstName": "John",
  "lastName": "Doe",
  "dept": "IT Department",
  "mobile": "9876543210",
  "addressId": 1,
  "remarks": "Active member since 2020"
}
```

### Sewadar Request (Without Address)
```json
{
  "firstName": "Bob",
  "lastName": "Johnson",
  "dept": "Finance",
  "mobile": "9876543212",
  "remarks": "Part-time volunteer"
}
```

### Schedule Request
```json
{
  "scheduledPlace": "Community Center",
  "scheduledDate": "2024-02-15",
  "scheduledTime": "10:00:00",
  "scheduledMedium": "In-Person",
  "attendedById": 1
}
```

---

## Complete Test Sequence (Copy-Paste Ready)

```bash
#!/bin/bash

BASE_URL="http://localhost:8080"

echo "=== Testing Address APIs ==="

# Create Address 1
echo "Creating Address 1..."
ADDRESS1=$(curl -s -X POST $BASE_URL/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"123 Main Street","address2":"Apt 4B","email":"addr1@test.com"}' | jq -r '.id')
echo "Address 1 ID: $ADDRESS1"

# Create Address 2
echo "Creating Address 2..."
ADDRESS2=$(curl -s -X POST $BASE_URL/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"456 Oak Avenue","address2":"Floor 2","email":"addr2@test.com"}' | jq -r '.id')
echo "Address 2 ID: $ADDRESS2"

# Get All Addresses
echo "Getting all addresses..."
curl -X GET $BASE_URL/api/addresses | jq

# Get Address by ID
echo "Getting address by ID..."
curl -X GET $BASE_URL/api/addresses/$ADDRESS1 | jq

echo ""
echo "=== Testing Sewadar APIs ==="

# Create Sewadar 1
echo "Creating Sewadar 1..."
SEWADAR1=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Test\"}" | jq -r '.id')
echo "Sewadar 1 ID: $SEWADAR1"

# Create Sewadar 2
echo "Creating Sewadar 2..."
SEWADAR2=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"dept\":\"HR\",\"mobile\":\"0987654321\",\"addressId\":$ADDRESS2,\"remarks\":\"Test\"}" | jq -r '.id')
echo "Sewadar 2 ID: $SEWADAR2"

# Create Sewadar 3 (without address)
echo "Creating Sewadar 3 (no address)..."
SEWADAR3=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Johnson","dept":"Finance","mobile":"5555555555","remarks":"No address"}' | jq -r '.id')
echo "Sewadar 3 ID: $SEWADAR3"

# Get All Sewadars
echo "Getting all sewadars..."
curl -X GET $BASE_URL/api/sewadars | jq

# Get Sewadar by ID
echo "Getting sewadar by ID..."
curl -X GET $BASE_URL/api/sewadars/$SEWADAR1 | jq

# Update Sewadar
echo "Updating sewadar..."
curl -X PUT $BASE_URL/api/sewadars/$SEWADAR1 \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT Senior\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Updated\"}" | jq

echo ""
echo "=== Testing Schedule APIs ==="

# Create Schedule 1
echo "Creating Schedule 1..."
SCHEDULE1=$(curl -s -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Community Center\",\"scheduledDate\":\"2024-02-15\",\"scheduledTime\":\"10:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}" | jq -r '.id')
echo "Schedule 1 ID: $SCHEDULE1"

# Create Schedule 2
echo "Creating Schedule 2..."
SCHEDULE2=$(curl -s -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Online Meeting\",\"scheduledDate\":\"2024-02-20\",\"scheduledTime\":\"14:30:00\",\"scheduledMedium\":\"Zoom\",\"attendedById\":$SEWADAR1}" | jq -r '.id')
echo "Schedule 2 ID: $SCHEDULE2"

# Get All Schedules
echo "Getting all schedules..."
curl -X GET $BASE_URL/api/schedules | jq

# Get Schedule by ID
echo "Getting schedule by ID..."
curl -X GET $BASE_URL/api/schedules/$SCHEDULE1 | jq

# Update Schedule
echo "Updating schedule..."
curl -X PUT $BASE_URL/api/schedules/$SCHEDULE1 \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Updated Center\",\"scheduledDate\":\"2024-02-16\",\"scheduledTime\":\"11:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}" | jq

echo ""
echo "=== Testing Error Scenarios ==="

# Test 404 - Non-existent Sewadar
echo "Testing 404 error..."
curl -X GET $BASE_URL/api/sewadars/999 | jq

# Test Validation Error
echo "Testing validation error..."
curl -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"lastName":"Doe"}' | jq

# Test Invalid Foreign Key
echo "Testing invalid foreign key..."
curl -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"scheduledPlace":"Test","scheduledDate":"2024-02-15","scheduledTime":"10:00:00","scheduledMedium":"In-Person","attendedById":999}' | jq

echo ""
echo "=== Cleanup (Optional) ==="

# Delete Schedule
echo "Deleting schedule..."
curl -X DELETE $BASE_URL/api/schedules/$SCHEDULE2

# Delete Sewadar
echo "Deleting sewadar..."
curl -X DELETE $BASE_URL/api/sewadars/$SEWADAR3

# Delete Address
echo "Deleting address..."
curl -X DELETE $BASE_URL/api/addresses/$ADDRESS2

echo ""
echo "=== Test Complete ==="
```

---

## Postman Collection JSON

Save this as `Sewadar_API.postman_collection.json` and import into Postman:

```json
{
  "info": {
    "name": "Sewadar & Schedule CRUD APIs",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Address APIs",
      "item": [
        {
          "name": "Create Address",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"address1\": \"123 Main Street\",\n  \"address2\": \"Apt 4B\",\n  \"email\": \"test@example.com\"\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/addresses",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "addresses"]
            }
          }
        },
        {
          "name": "Get All Addresses",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/addresses",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "addresses"]
            }
          }
        },
        {
          "name": "Get Address by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/addresses/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "addresses", "1"]
            }
          }
        },
        {
          "name": "Update Address",
          "request": {
            "method": "PUT",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"address1\": \"Updated Address\",\n  \"address2\": \"Updated Apt\",\n  \"email\": \"updated@example.com\"\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/addresses/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "addresses", "1"]
            }
          }
        },
        {
          "name": "Delete Address",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "http://localhost:8080/api/addresses/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "addresses", "1"]
            }
          }
        }
      ]
    },
    {
      "name": "Sewadar APIs",
      "item": [
        {
          "name": "Create Sewadar",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"firstName\": \"John\",\n  \"lastName\": \"Doe\",\n  \"dept\": \"IT Department\",\n  \"mobile\": \"9876543210\",\n  \"addressId\": 1,\n  \"remarks\": \"Active member\"\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/sewadars",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars"]
            }
          }
        },
        {
          "name": "Get All Sewadars",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/sewadars",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars"]
            }
          }
        },
        {
          "name": "Get Sewadar by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/sewadars/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars", "1"]
            }
          }
        },
        {
          "name": "Update Sewadar",
          "request": {
            "method": "PUT",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"firstName\": \"John\",\n  \"lastName\": \"Doe\",\n  \"dept\": \"IT Senior\",\n  \"mobile\": \"9876543210\",\n  \"addressId\": 1,\n  \"remarks\": \"Updated\"\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/sewadars/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars", "1"]
            }
          }
        },
        {
          "name": "Delete Sewadar",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "http://localhost:8080/api/sewadars/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars", "1"]
            }
          }
        }
      ]
    },
    {
      "name": "Schedule APIs",
      "item": [
        {
          "name": "Create Schedule",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"scheduledPlace\": \"Community Center\",\n  \"scheduledDate\": \"2024-02-15\",\n  \"scheduledTime\": \"10:00:00\",\n  \"scheduledMedium\": \"In-Person\",\n  \"attendedById\": 1\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/schedules",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules"]
            }
          }
        },
        {
          "name": "Get All Schedules",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/schedules",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules"]
            }
          }
        },
        {
          "name": "Get Schedule by ID",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/schedules/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules", "1"]
            }
          }
        },
        {
          "name": "Update Schedule",
          "request": {
            "method": "PUT",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"scheduledPlace\": \"Updated Center\",\n  \"scheduledDate\": \"2024-02-16\",\n  \"scheduledTime\": \"11:00:00\",\n  \"scheduledMedium\": \"In-Person\",\n  \"attendedById\": 1\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/schedules/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules", "1"]
            }
          }
        },
        {
          "name": "Delete Schedule",
          "request": {
            "method": "DELETE",
            "url": {
              "raw": "http://localhost:8080/api/schedules/1",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules", "1"]
            }
          }
        }
      ]
    },
    {
      "name": "Error Scenarios",
      "item": [
        {
          "name": "Validation Error - Missing Fields",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"lastName\": \"Doe\"\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/sewadars",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars"]
            }
          }
        },
        {
          "name": "404 - Resource Not Found",
          "request": {
            "method": "GET",
            "url": {
              "raw": "http://localhost:8080/api/sewadars/999",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "sewadars", "999"]
            }
          }
        },
        {
          "name": "Invalid Foreign Key",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\n  \"scheduledPlace\": \"Test\",\n  \"scheduledDate\": \"2024-02-15\",\n  \"scheduledTime\": \"10:00:00\",\n  \"scheduledMedium\": \"In-Person\",\n  \"attendedById\": 999\n}"
            },
            "url": {
              "raw": "http://localhost:8080/api/schedules",
              "host": ["localhost"],
              "port": "8080",
              "path": ["api", "schedules"]
            }
          }
        }
      ]
    }
  ]
}
```

---

## Testing Checklist

Print this checklist and mark items as you test:

### Address APIs
- [ ] POST /api/addresses - Create
- [ ] GET /api/addresses - Get All
- [ ] GET /api/addresses/{id} - Get One
- [ ] PUT /api/addresses/{id} - Update
- [ ] DELETE /api/addresses/{id} - Delete

### Sewadar APIs
- [ ] POST /api/sewadars - Create (with address)
- [ ] POST /api/sewadars - Create (without address)
- [ ] GET /api/sewadars - Get All
- [ ] GET /api/sewadars/{id} - Get One
- [ ] PUT /api/sewadars/{id} - Update
- [ ] PUT /api/sewadars/{id} - Update (change address)
- [ ] PUT /api/sewadars/{id} - Update (remove address)
- [ ] DELETE /api/sewadars/{id} - Delete

### Schedule APIs
- [ ] POST /api/schedules - Create
- [ ] GET /api/schedules - Get All
- [ ] GET /api/schedules/{id} - Get One
- [ ] PUT /api/schedules/{id} - Update
- [ ] PUT /api/schedules/{id} - Update (change attendedBy)
- [ ] DELETE /api/schedules/{id} - Delete

### Error Handling
- [ ] Validation Error (400)
- [ ] Resource Not Found (404)
- [ ] Invalid Foreign Key (404)

---

## Quick Start Commands

```bash
# 1. Start PostgreSQL (if not running)
# macOS: brew services start postgresql
# Linux: sudo systemctl start postgresql

# 2. Create database
psql -U postgres -c "CREATE DATABASE sewadar_db;"

# 3. Start application
./mvnw spring-boot:run

# 4. Test in new terminal
curl http://localhost:8080/api/sewadars
```

---

## Expected HTTP Status Codes

- **200 OK**: Successful GET, PUT operations
- **201 Created**: Successful POST operations
- **204 No Content**: Successful DELETE operations
- **400 Bad Request**: Validation errors
- **404 Not Found**: Resource not found
- **500 Internal Server Error**: Server errors (should not occur in normal flow)

---

## Notes

1. Replace `localhost:8080` with your server URL if different
2. Replace IDs (1, 2, etc.) with actual IDs returned from create operations
3. Date format: `YYYY-MM-DD` (e.g., `2024-02-15`)
4. Time format: `HH:mm:ss` (e.g., `10:00:00`)
5. Use `jq` for pretty JSON output: `curl ... | jq`
6. Save IDs from create responses for use in subsequent requests

