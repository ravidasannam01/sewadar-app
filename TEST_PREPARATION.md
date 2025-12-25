# Test Suite Preparation Guide

## Initial State Requirements

The test suite is designed to work with **existing data** or **fresh database**. Here's what you need to know:

## Option 1: Use Existing Data (Recommended)

### If you already have:
- ✅ 1 incharge (bootstrap incharge)
- ✅ 2 sewadars
- ✅ Any existing programs/applications

### What the test suite will do:
1. **Detect existing incharge** - Won't try to create a new one
2. **Use existing incharge** - Logs in with existing credentials
3. **Create additional sewadars** - Adds 85 more sewadars (total: 87)
4. **Create new programs** - Adds 12 new programs
5. **Test all flows** - Uses both existing and new data

### Requirements:
- Incharge password must be `admin123` (or update test script)
- Incharge zonal_id should be 1 (or test will auto-detect)

---

## Option 2: Fresh Database (Clean Slate)

### If you want to start fresh:

1. **Reset Database**:
   ```bash
   # Option A: Change application.properties
   spring.jpa.hibernate.ddl-auto=create-drop
   # Then restart application (will drop and recreate all tables)
   
   # Option B: Manually drop tables (if using PostgreSQL)
   # Connect to database and drop all tables
   ```

2. **Run Test Suite**:
   - Test suite will create incharge automatically
   - Then create all sewadars and programs

---

## Current State Detection

The test suite automatically:
- ✅ Checks if incharge exists
- ✅ Uses existing incharge if found
- ✅ Creates new incharge only if needed
- ✅ Works with existing sewadars
- ✅ Adds new sewadars to existing ones

---

## What Gets Created

### With Existing Data:
- **Sewadars**: Your 2 existing + 85 new = **87 total**
- **Programs**: Your existing + 12 new = **12+ total**
- **Applications**: Created for new programs
- **Attendance**: Marked for new programs

### With Fresh Database:
- **Sewadars**: 85 new
- **Programs**: 12 new
- **Applications**: 150-200
- **Attendance**: 50-100 records

---

## Password Requirements

### Default Passwords Used:
- **Incharge**: `admin123`
- **All Sewadars**: `sewadar123`

### If Your Incharge Has Different Password:

Edit `comprehensive_production_test.py` line ~200:
```python
# Change this:
json={"zonalId": str(self.suite.incharge_id), "password": "admin123"}

# To your password:
json={"zonalId": str(self.suite.incharge_id), "password": "YOUR_PASSWORD"}
```

---

## Pre-Test Checklist

Before running the test suite:

- [ ] Spring Boot application is running
- [ ] Database is accessible
- [ ] Incharge exists (or will be created)
- [ ] Incharge password is `admin123` (or update script)
- [ ] Port 8080 is available
- [ ] Python 3 and requests library installed

---

## Running with Existing Data

### Step 1: Verify Current State
```bash
# Check if incharge exists
curl http://localhost:8080/api/bootstrap/status

# Should return: {"hasIncharge": true, "needsBootstrap": false}
```

### Step 2: Run Test Suite
```bash
./run_production_tests.sh
```

### Step 3: Test Suite Will:
1. Detect existing incharge ✅
2. Login with existing incharge ✅
3. Create 85 additional sewadars ✅
4. Create 12 new programs ✅
5. Test all flows ✅

---

## Expected Behavior

### If Incharge Exists:
```
✅ PASS | Bootstrap Status Check | Has Incharge: True, Needs Bootstrap: False
✅ PASS | Find Existing Incharge | Using existing incharge with Zonal ID: 1
✅ PASS | Incharge Login | Token obtained
```

### If No Incharge:
```
✅ PASS | Bootstrap Status Check | Has Incharge: False, Needs Bootstrap: True
✅ PASS | Create Incharge | Created new incharge with Zonal ID: 1
✅ PASS | Incharge Login | Token obtained
```

---

## Troubleshooting

### Issue: "Could not find existing incharge"
**Solution**: 
- Verify incharge exists: `curl http://localhost:8080/api/bootstrap/status`
- Check incharge zonal_id (should be 1)
- Verify password is `admin123`

### Issue: "Login failed"
**Solution**:
- Update password in test script (line ~200)
- Or reset incharge password to `admin123`

### Issue: "Duplicate key violation" for sewadars
**Solution**: 
- This is expected if mobile/aadhar conflicts
- Test suite handles this gracefully
- Some sewadars may fail to create (this is OK)

---

## Data Impact

### What Gets Modified:
- ✅ New sewadars added (85)
- ✅ New programs added (12)
- ✅ New applications created
- ✅ New attendance records

### What Stays Intact:
- ✅ Existing incharge
- ✅ Existing sewadars (your 2 sewadars)
- ✅ Existing programs (if any)
- ✅ Existing applications (if any)

---

## Post-Test Cleanup (Optional)

If you want to clean up test data:

1. **Delete test sewadars** (keep your original 2)
2. **Delete test programs**
3. **Or reset database** (if you want fresh start)

---

## Summary

✅ **You can run the test suite with your existing data!**

The test suite will:
- Use your existing incharge
- Keep your existing 2 sewadars
- Add 85 more sewadars for testing
- Create 12 new programs
- Test all flows comprehensively

No database reset needed! 🎉

