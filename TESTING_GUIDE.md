# Complete Testing Guide for Sewadar & Schedule CRUD APIs

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Database Setup](#database-setup)
3. [Starting the Application](#starting-the-application)
4. [Testing Sewadar APIs](#testing-sewadar-apis)
5. [Testing Schedule APIs](#testing-schedule-apis)
6. [Testing Error Scenarios](#testing-error-scenarios)
7. [Complete Test Sequence](#complete-test-sequence)

---

## Prerequisites

1. **Java 21** installed and configured
2. **PostgreSQL** running on localhost:5432
3. **Maven** installed (or use `./mvnw`)
4. **API Testing Tool** (Postman, curl, or any REST client)

---

## Database Setup

### Step 1: Create Database
```sql
-- Connect to PostgreSQL
psql -U postgres

-- Create database
CREATE DATABASE sewadar_db;

-- Verify database creation
\l
```

### Step 2: Verify Connection
The application will automatically create tables on first run using `spring.jpa.hibernate.ddl-auto=update`.

---

## Starting the Application

### Option 1: Using Maven Wrapper
```bash
./mvnw spring-boot:run
```

### Option 2: Using Maven (if installed)
```bash
mvn spring-boot:run
```

### Option 3: Build and Run JAR
```bash
./mvnw clean package
java -jar target/application-0.0.1-SNAPSHOT.jar
```

**Expected Output:**
```
Started Application in X.XXX seconds
```

**Application URL:** `http://localhost:8080`

---

## Testing Sewadar APIs

### Base URL
```
http://localhost:8080/api/sewadars
```

---

### Test 1: Create Address (Prerequisite for Sewadar)

**Note:** Since Sewadar has an optional relationship with Address, we'll create addresses first for complete testing.

#### Create Address 1
```bash
curl -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{
    "address1": "123 Main Street",
    "address2": "Apt 4B",
    "email": "address1@example.com"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "address1": "123 Main Street",
  "address2": "Apt 4B",
  "email": "address1@example.com"
}
```

#### Create Address 2
```bash
curl -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{
    "address1": "456 Oak Avenue",
    "address2": "Floor 2",
    "email": "address2@example.com"
  }'
```

**Save Address IDs:** Note the `id` values returned (e.g., `1` and `2`) for use in Sewadar creation.

---

### Test 2: Create Sewadar (POST)

#### Create Sewadar 1 (with address)
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dept": "IT Department",
    "mobile": "9876543210",
    "addressId": 1,
    "remarks": "Active member since 2020"
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dept": "IT Department",
  "mobile": "9876543210",
  "address": {
    "id": 1,
    "address1": "123 Main Street",
    "address2": "Apt 4B",
    "email": "address1@example.com"
  },
  "remarks": "Active member since 2020"
}
```

#### Create Sewadar 2 (with address)
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "dept": "HR Department",
    "mobile": "9876543211",
    "addressId": 2,
    "remarks": "New member"
  }'
```

#### Create Sewadar 3 (without address - optional)
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Bob",
    "lastName": "Johnson",
    "dept": "Finance",
    "mobile": "9876543212",
    "remarks": "Part-time volunteer"
  }'
```

**Save Sewadar IDs:** Note the `id` values (e.g., `1`, `2`, `3`) for subsequent tests.

---

### Test 3: Get All Sewadars (GET)

```bash
curl -X GET http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
[
  {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dept": "IT Department",
    "mobile": "9876543210",
    "address": {
      "id": 1,
      "address1": "123 Main Street",
      "address2": "Apt 4B",
      "email": "address1@example.com"
    },
    "remarks": "Active member since 2020"
  },
  {
    "id": 2,
    "firstName": "Jane",
    "lastName": "Smith",
    "dept": "HR Department",
    "mobile": "9876543211",
    "address": {
      "id": 2,
      "address1": "456 Oak Avenue",
      "address2": "Floor 2",
      "email": "address2@example.com"
    },
    "remarks": "New member"
  },
  {
    "id": 3,
    "firstName": "Bob",
    "lastName": "Johnson",
    "dept": "Finance",
    "mobile": "9876543212",
    "address": null,
    "remarks": "Part-time volunteer"
  }
]
```

---

### Test 4: Get Sewadar by ID (GET)

```bash
curl -X GET http://localhost:8080/api/sewadars/1 \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dept": "IT Department",
  "mobile": "9876543210",
  "address": {
    "id": 1,
    "address1": "123 Main Street",
    "address2": "Apt 4B",
    "email": "address1@example.com"
  },
  "remarks": "Active member since 2020"
}
```

---

### Test 5: Update Sewadar (PUT)

```bash
curl -X PUT http://localhost:8080/api/sewadars/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dept": "IT Department - Senior",
    "mobile": "9876543210",
    "addressId": 1,
    "remarks": "Promoted to Senior position"
  }'
```

**Expected Response (200 OK):**
```json
{
  "id": 1,
  "firstName": "John",
  "lastName": "Doe",
  "dept": "IT Department - Senior",
  "mobile": "9876543210",
  "address": {
    "id": 1,
    "address1": "123 Main Street",
    "address2": "Apt 4B",
    "email": "address1@example.com"
  },
  "remarks": "Promoted to Senior position"
}
```

#### Update Sewadar to change address
```bash
curl -X PUT http://localhost:8080/api/sewadars/3 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Bob",
    "lastName": "Johnson",
    "dept": "Finance",
    "mobile": "9876543212",
    "addressId": 1,
    "remarks": "Part-time volunteer - now with address"
  }'
```

#### Update Sewadar to remove address (set addressId to null)
```bash
curl -X PUT http://localhost:8080/api/sewadars/2 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Smith",
    "dept": "HR Department",
    "mobile": "9876543211",
    "remarks": "Address removed"
  }'
```

---

### Test 6: Delete Sewadar (DELETE)

**⚠️ Important:** Only delete Sewadar 3 (Bob) as we'll need Sewadar 1 and 2 for Schedule testing.

```bash
curl -X DELETE http://localhost:8080/api/sewadars/3 \
  -H "Content-Type: application/json"
```

**Expected Response (204 No Content):** Empty response body

**Verify Deletion:**
```bash
curl -X GET http://localhost:8080/api/sewadars/3
```

**Expected Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Sewadar not found with id : '3'"
}
```

---

## Testing Schedule APIs

### Base URL
```
http://localhost:8080/api/schedules
```

---

### Test 7: Create Schedule (POST)

#### Create Schedule 1
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Community Center",
    "scheduledDate": "2024-02-15",
    "scheduledTime": "10:00:00",
    "scheduledMedium": "In-Person",
    "attendedById": 1
  }'
```

**Expected Response (201 Created):**
```json
{
  "id": 1,
  "scheduledPlace": "Community Center",
  "scheduledDate": "2024-02-15",
  "scheduledTime": "10:00:00",
  "scheduledMedium": "In-Person",
  "attendedBy": {
    "id": 1,
    "firstName": "John",
    "lastName": "Doe",
    "dept": "IT Department - Senior",
    "mobile": "9876543210",
    "address": {
      "id": 1,
      "address1": "123 Main Street",
      "address2": "Apt 4B",
      "email": "address1@example.com"
    },
    "remarks": "Promoted to Senior position"
  }
}
```

#### Create Schedule 2
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Online Meeting",
    "scheduledDate": "2024-02-20",
    "scheduledTime": "14:30:00",
    "scheduledMedium": "Zoom",
    "attendedById": 1
  }'
```

#### Create Schedule 3
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Office Building",
    "scheduledDate": "2024-02-25",
    "scheduledTime": "09:00:00",
    "scheduledMedium": "In-Person",
    "attendedById": 2
  }'
```

**Save Schedule IDs:** Note the `id` values (e.g., `1`, `2`, `3`).

---

### Test 8: Get All Schedules (GET)

```bash
curl -X GET http://localhost:8080/api/schedules \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):** Array of all schedules with full Sewadar details.

---

### Test 9: Get Schedule by ID (GET)

```bash
curl -X GET http://localhost:8080/api/schedules/1 \
  -H "Content-Type: application/json"
```

**Expected Response (200 OK):** Single schedule object with full details.

---

### Test 10: Update Schedule (PUT)

```bash
curl -X PUT http://localhost:8080/api/schedules/1 \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Updated Community Center",
    "scheduledDate": "2024-02-16",
    "scheduledTime": "11:00:00",
    "scheduledMedium": "In-Person",
    "attendedById": 1
  }'
```

**Expected Response (200 OK):** Updated schedule object.

#### Update Schedule to change attendedBy
```bash
curl -X PUT http://localhost:8080/api/schedules/2 \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Online Meeting",
    "scheduledDate": "2024-02-20",
    "scheduledTime": "14:30:00",
    "scheduledMedium": "Zoom",
    "attendedById": 2
  }'
```

---

### Test 11: Delete Schedule (DELETE)

```bash
curl -X DELETE http://localhost:8080/api/schedules/3 \
  -H "Content-Type: application/json"
```

**Expected Response (204 No Content):** Empty response body.

**Verify Deletion:**
```bash
curl -X GET http://localhost:8080/api/schedules/3
```

**Expected Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Schedule not found with id : '3'"
}
```

---

## Testing Error Scenarios

### Test 12: Validation Errors

#### Missing Required Fields (Sewadar)
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "lastName": "Doe",
    "dept": "IT"
  }'
```

**Expected Response (400 Bad Request):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "message": "Invalid input data",
  "validationErrors": {
    "firstName": "First name is required"
  }
}
```

#### Missing Required Fields (Schedule)
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Test Place"
  }'
```

**Expected Response (400 Bad Request):** Validation errors for missing required fields.

---

### Test 13: Resource Not Found Errors

#### Get Non-existent Sewadar
```bash
curl -X GET http://localhost:8080/api/sewadars/999
```

**Expected Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Sewadar not found with id : '999'"
}
```

#### Update Non-existent Sewadar
```bash
curl -X PUT http://localhost:8080/api/sewadars/999 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User"
  }'
```

**Expected Response (404 Not Found):** Same as above.

#### Delete Non-existent Sewadar
```bash
curl -X DELETE http://localhost:8080/api/sewadars/999
```

**Expected Response (404 Not Found):** Same as above.

---

### Test 14: Invalid Foreign Key References

#### Create Schedule with Invalid Sewadar ID
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Test Place",
    "scheduledDate": "2024-02-15",
    "scheduledTime": "10:00:00",
    "scheduledMedium": "In-Person",
    "attendedById": 999
  }'
```

**Expected Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Sewadar not found with id : '999'"
}
```

#### Update Sewadar with Invalid Address ID
```bash
curl -X PUT http://localhost:8080/api/sewadars/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "dept": "IT",
    "addressId": 999
  }'
```

**Expected Response (404 Not Found):**
```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Resource Not Found",
  "message": "Address not found with id : '999'"
}
```

---

### Test 15: Invalid Date/Time Format

#### Invalid Date Format
```bash
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{
    "scheduledPlace": "Test Place",
    "scheduledDate": "invalid-date",
    "scheduledTime": "10:00:00",
    "scheduledMedium": "In-Person",
    "attendedById": 1
  }'
```

**Expected Response (400 Bad Request):** Error about date format.

---

## Complete Test Sequence

### Quick Test Script (Execute in Order)

```bash
# 1. Start Application (in separate terminal)
./mvnw spring-boot:run

# 2. Create Addresses
ADDRESS1=$(curl -s -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"123 Main St","address2":"Apt 1","email":"addr1@test.com"}' | jq -r '.id')

ADDRESS2=$(curl -s -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"456 Oak Ave","address2":"Floor 2","email":"addr2@test.com"}' | jq -r '.id')

echo "Created Address IDs: $ADDRESS1, $ADDRESS2"

# 3. Create Sewadars
SEWADAR1=$(curl -s -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Test\"}" | jq -r '.id')

SEWADAR2=$(curl -s -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"dept\":\"HR\",\"mobile\":\"0987654321\",\"addressId\":$ADDRESS2,\"remarks\":\"Test\"}" | jq -r '.id')

echo "Created Sewadar IDs: $SEWADAR1, $SEWADAR2"

# 4. Get All Sewadars
curl -X GET http://localhost:8080/api/sewadars

# 5. Get Sewadar by ID
curl -X GET http://localhost:8080/api/sewadars/$SEWADAR1

# 6. Update Sewadar
curl -X PUT http://localhost:8080/api/sewadars/$SEWADAR1 \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT Senior\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Updated\"}"

# 7. Create Schedules
SCHEDULE1=$(curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Center\",\"scheduledDate\":\"2024-02-15\",\"scheduledTime\":\"10:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}" | jq -r '.id')

echo "Created Schedule ID: $SCHEDULE1"

# 8. Get All Schedules
curl -X GET http://localhost:8080/api/schedules

# 9. Get Schedule by ID
curl -X GET http://localhost:8080/api/schedules/$SCHEDULE1

# 10. Update Schedule
curl -X PUT http://localhost:8080/api/schedules/$SCHEDULE1 \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Updated Center\",\"scheduledDate\":\"2024-02-16\",\"scheduledTime\":\"11:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}"

# 11. Test Error - Get Non-existent
curl -X GET http://localhost:8080/api/sewadars/999

# 12. Test Error - Invalid Foreign Key
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"scheduledPlace":"Test","scheduledDate":"2024-02-15","scheduledTime":"10:00:00","scheduledMedium":"In-Person","attendedById":999}'

# 13. Delete Schedule
curl -X DELETE http://localhost:8080/api/schedules/$SCHEDULE1

# 14. Delete Sewadar
curl -X DELETE http://localhost:8080/api/sewadars/$SEWADAR2

# 15. Verify Deletions
curl -X GET http://localhost:8080/api/schedules/$SCHEDULE1
curl -X GET http://localhost:8080/api/sewadars/$SEWADAR2
```

---

## Postman Collection

If you prefer using Postman, here's a collection you can import:

### Collection Structure:
1. **Sewadar APIs**
   - Create Sewadar
   - Get All Sewadars
   - Get Sewadar by ID
   - Update Sewadar
   - Delete Sewadar

2. **Schedule APIs**
   - Create Schedule
   - Get All Schedules
   - Get Schedule by ID
   - Update Schedule
   - Delete Schedule

3. **Error Scenarios**
   - Validation Errors
   - Resource Not Found
   - Invalid Foreign Keys

---

## Testing Checklist

### ✅ Sewadar CRUD Operations
- [ ] Create Sewadar with address
- [ ] Create Sewadar without address
- [ ] Get all Sewadars
- [ ] Get Sewadar by ID
- [ ] Update Sewadar (all fields)
- [ ] Update Sewadar to change address
- [ ] Update Sewadar to remove address
- [ ] Delete Sewadar

### ✅ Schedule CRUD Operations
- [ ] Create Schedule
- [ ] Get all Schedules
- [ ] Get Schedule by ID
- [ ] Update Schedule (all fields)
- [ ] Update Schedule to change attendedBy
- [ ] Delete Schedule

### ✅ Error Handling
- [ ] Validation errors (missing required fields)
- [ ] Resource not found (404)
- [ ] Invalid foreign key references
- [ ] Invalid date/time formats

### ✅ Relationship Testing
- [ ] Sewadar with Address relationship
- [ ] Schedule with Sewadar relationship
- [ ] Cascading behavior (if applicable)

---

## Expected Database State After Complete Testing

After running all tests, you should have:
- **Addresses table:** 2 records
- **Sewadars table:** 1-2 records (depending on deletions)
- **Schedules table:** 0-2 records (depending on deletions)

---

## Troubleshooting

### Issue: Connection Refused
**Solution:** Ensure PostgreSQL is running and credentials in `application.properties` are correct.

### Issue: Table Not Found
**Solution:** Check `spring.jpa.hibernate.ddl-auto=update` is set in `application.properties`.

### Issue: Port Already in Use
**Solution:** Change `server.port` in `application.properties` or stop the process using port 8080.

### Issue: Validation Errors Not Showing
**Solution:** Ensure `@Valid` annotation is present on controller methods and `spring-boot-starter-validation` is in dependencies.

---

## Notes

1. **Date Format:** Use `YYYY-MM-DD` format for dates (e.g., `2024-02-15`)
2. **Time Format:** Use `HH:mm:ss` format for times (e.g., `10:00:00`)
3. **IDs:** Save IDs from create responses for use in subsequent requests
4. **Cascading:** Deleting a Sewadar will cascade delete associated Schedules (if configured)
5. **Optional Fields:** `addressId` in Sewadar and `scheduledMedium` in Schedule are optional

---

## Next Steps After Testing

After completing all tests, report:
1. ✅ Which tests passed
2. ❌ Which tests failed (with error messages)
3. 🔧 Any modifications or enhancements needed
4. 📝 Any additional features you'd like to add

Happy Testing! 🚀

