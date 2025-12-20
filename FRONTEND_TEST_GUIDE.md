# Frontend Testing Guide

## 🚀 Quick Start

1. **Start the application:**
```bash
./mvnw spring-boot:run
```

2. **Open browser:**
```
http://localhost:8080/index.html
```

## 📋 Testing Flow

### Step 1: Bootstrap (Create First Incharge)

Since no users exist, you need to create the first incharge via API:

```bash
# Create first incharge (will automatically be INCHARGE)
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "Incharge",
    "mobile": "9876543210",
    "password": "admin123",
    "dept": "Administration"
  }'
```

**OR use bootstrap endpoint:**
```bash
curl -X POST http://localhost:8080/api/bootstrap/create-incharge \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "Incharge",
    "mobile": "9876543210",
    "password": "admin123",
    "dept": "Administration"
  }'
```

### Step 2: Login

1. Open `http://localhost:8080/index.html`
2. Enter mobile: `9876543210`
3. Enter password: `admin123`
4. Click Login

### Step 3: Test Features

#### As INCHARGE:
- ✅ Create Programs (with multiple dates)
- ✅ View Applications
- ✅ Select Sewadars
- ✅ Create Actions
- ✅ Mark Attendance
- ✅ Admin Panel

#### As SEWADAR:
- ✅ View Programs
- ✅ Apply to Programs
- ✅ View Selections
- ✅ View Pending Actions
- ✅ Respond to Actions
- ✅ Drop Consent

## 🧪 Complete Test Sequence

### 1. Create First Incharge
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "Incharge",
    "mobile": "9876543210",
    "password": "admin123",
    "dept": "Administration"
  }'
```

### 2. Create Sewadar
```bash
curl -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "mobile": "1234567890",
    "password": "password123",
    "dept": "IT"
  }'
```

### 3. Login as Incharge (in browser)
- Mobile: `9876543210`
- Password: `admin123`

### 4. Create Program (in browser)
- Click "Create Program"
- Fill form with multiple dates
- Save

### 5. Login as Sewadar (in browser)
- Mobile: `1234567890`
- Password: `password123`

### 6. Apply to Program (in browser)
- View programs
- Click "Apply"

### 7. Login as Incharge again
- View applications
- Select sewadars
- Create actions

### 8. Login as Sewadar
- View pending actions
- Respond to actions

## 🎯 Features to Test

- [ ] Login/Logout
- [ ] Create Program (multiple dates)
- [ ] Apply to Program
- [ ] View Applications (Incharge)
- [ ] Select Sewadars
- [ ] Create Actions
- [ ] Respond to Actions
- [ ] Drop Consent
- [ ] Mark Attendance
- [ ] Role-based UI visibility

## 🐛 Troubleshooting

**Login fails?**
- Check sewadar exists in database
- Verify password is correct
- Check browser console for errors

**Can't see programs?**
- Make sure you're logged in
- Check Authorization header is sent
- Verify token is valid

**Actions not showing?**
- Make sure sewadar is selected for program
- Check program has actions created

