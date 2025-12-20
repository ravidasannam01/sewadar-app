# Quick Start Guide

## 🚀 Get Started in 3 Steps

### Step 1: Setup Database
```bash
# Connect to PostgreSQL
psql -U postgres

# Create database
CREATE DATABASE sewadar_db;

# Exit
\q
```

### Step 2: Start Application
```bash
./mvnw spring-boot:run
```

Wait for: `Started Application in X.XXX seconds`

### Step 3: Run Tests
```bash
# Option 1: Automated test script
./test-api.sh

# Option 2: Manual testing
curl http://localhost:8080/api/sewadars
```

---

## 📚 Documentation Files

1. **TESTING_GUIDE.md** - Complete step-by-step testing instructions
2. **TEST_RESOURCES.md** - Quick reference with all curl commands and JSON samples
3. **test-api.sh** - Automated test script (executable)

---

## 🔗 API Endpoints

### Address APIs
- `POST /api/addresses` - Create address
- `GET /api/addresses` - Get all addresses
- `GET /api/addresses/{id}` - Get address by ID
- `PUT /api/addresses/{id}` - Update address
- `DELETE /api/addresses/{id}` - Delete address

### Sewadar APIs
- `POST /api/sewadars` - Create sewadar
- `GET /api/sewadars` - Get all sewadars
- `GET /api/sewadars/{id}` - Get sewadar by ID
- `PUT /api/sewadars/{id}` - Update sewadar
- `DELETE /api/sewadars/{id}` - Delete sewadar

### Schedule APIs
- `POST /api/schedules` - Create schedule
- `GET /api/schedules` - Get all schedules
- `GET /api/schedules/{id}` - Get schedule by ID
- `PUT /api/schedules/{id}` - Update schedule
- `DELETE /api/schedules/{id}` - Delete schedule

---

## ⚙️ Configuration

Edit `src/main/resources/application.properties` to change:
- Database connection (currently PostgreSQL)
- Server port (default: 8080)
- Database credentials

---

## 🧪 Quick Test

```bash
# Create Address
curl -X POST http://localhost:8080/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"123 Main St","email":"test@example.com"}'

# Create Sewadar (use address ID from above)
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe","dept":"IT","addressId":1}'

# Get All Sewadars
curl http://localhost:8080/api/sewadars
```

---

## 📝 Testing Checklist

After running tests, report:
- ✅ Which tests passed
- ❌ Which tests failed (with error messages)
- 🔧 Any modifications needed
- 📝 Any enhancements desired

---

## 🆘 Troubleshooting

**Connection Refused?**
- Check PostgreSQL is running: `psql -U postgres -c "SELECT 1;"`
- Verify credentials in `application.properties`

**Port Already in Use?**
- Change `server.port` in `application.properties`
- Or stop process using port 8080

**Tables Not Created?**
- Check `spring.jpa.hibernate.ddl-auto=update` is set
- Check database connection in logs

---

## 📖 Full Documentation

See **TESTING_GUIDE.md** for complete testing instructions with all scenarios.

