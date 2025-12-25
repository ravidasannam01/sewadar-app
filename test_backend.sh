#!/bin/bash

echo "=== Backend End-to-End Test ==="
echo ""

# Step 1: Create first incharge
echo "Step 1: Creating first incharge..."
INCHARGE_RESPONSE=$(curl -s -X POST http://localhost:8080/api/bootstrap/first-incharge)
echo "$INCHARGE_RESPONSE" | python3 -m json.tool
INCHARGE_ID=$(echo "$INCHARGE_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['zonalId'])")
echo "Incharge Zonal ID: $INCHARGE_ID"
echo ""

# Step 2: Login as incharge
echo "Step 2: Logging in as incharge..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"zonalId\":\"$INCHARGE_ID\",\"password\":\"admin123\"}")
echo "$LOGIN_RESPONSE" | python3 -m json.tool
TOKEN=$(echo "$LOGIN_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")
echo "Token: ${TOKEN:0:50}..."
echo ""

# Step 3: Create sewadars
echo "Step 3: Creating sewadars..."
SEWADAR1_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Raghu",
    "lastName": "V",
    "mobile": "8500601336",
    "password": "sewadar123",
    "location": "BEAS",
    "profession": "Teacher",
    "dateOfBirth": "1990-01-15",
    "emergencyContact": "9876543210",
    "emergencyContactRelationship": "Spouse",
    "languages": ["Hindi", "English"],
    "photoUrl": "https://example.com/photo1.jpg"
  }')
echo "$SEWADAR1_RESPONSE" | python3 -m json.tool
SEWADAR1_ID=$(echo "$SEWADAR1_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['zonalId'])")
echo "Sewadar 1 Zonal ID: $SEWADAR1_ID"
echo ""

SEWADAR2_RESPONSE=$(curl -s -X POST http://localhost:8080/api/sewadars \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Krishna",
    "lastName": "Kumar",
    "mobile": "9876543210",
    "password": "sewadar123",
    "location": "Delhi",
    "profession": "Engineer",
    "dateOfBirth": "1985-05-20",
    "emergencyContact": "9876543211",
    "emergencyContactRelationship": "Brother",
    "languages": ["Hindi", "English", "Punjabi"],
    "photoUrl": "https://example.com/photo2.jpg"
  }')
echo "$SEWADAR2_RESPONSE" | python3 -m json.tool
SEWADAR2_ID=$(echo "$SEWADAR2_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['zonalId'])")
echo "Sewadar 2 Zonal ID: $SEWADAR2_ID"
echo ""

# Step 4: Create program with dates
echo "Step 4: Creating program with dates..."
PROGRAM_RESPONSE=$(curl -s -X POST http://localhost:8080/api/programs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"title\": \"Test Program\",
    \"description\": \"Test program for attendance\",
    \"location\": \"BEAS\",
    \"status\": \"active\",
    \"maxSewadars\": 10,
    \"createdById\": $INCHARGE_ID,
    \"programDates\": [\"2024-12-26\", \"2024-12-27\", \"2024-12-28\"]
  }")
echo "$PROGRAM_RESPONSE" | python3 -m json.tool
PROGRAM_ID=$(echo "$PROGRAM_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
echo "Program ID: $PROGRAM_ID"
echo ""

# Step 5: Login as sewadar 1
echo "Step 5: Logging in as sewadar 1..."
SEWADAR1_LOGIN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"zonalId\":\"$SEWADAR1_ID\",\"password\":\"sewadar123\"}")
SEWADAR1_TOKEN=$(echo "$SEWADAR1_LOGIN" | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])")
echo "Sewadar 1 logged in"
echo ""

# Step 6: Sewadar 1 applies to program
echo "Step 6: Sewadar 1 applying to program..."
APPLY1_RESPONSE=$(curl -s -X POST http://localhost:8080/api/program-applications \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $SEWADAR1_TOKEN" \
  -d "{
    \"programId\": $PROGRAM_ID,
    \"sewadarId\": $SEWADAR1_ID
  }")
echo "$APPLY1_RESPONSE" | python3 -m json.tool
echo ""

# Step 7: Login as incharge and approve application
echo "Step 7: Incharge approving sewadar 1 application..."
APP_ID1=$(echo "$APPLY1_RESPONSE" | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])")
APPROVE1_RESPONSE=$(curl -s -X PUT "http://localhost:8080/api/program-applications/$APP_ID1/status?status=APPROVED" \
  -H "Authorization: Bearer $TOKEN")
echo "$APPROVE1_RESPONSE" | python3 -m json.tool
echo ""

# Step 8: Get approved attendees for program
echo "Step 8: Getting approved attendees for program..."
ATTENDEES=$(curl -s -X GET "http://localhost:8080/api/attendances/program/$PROGRAM_ID/attendees" \
  -H "Authorization: Bearer $TOKEN")
echo "$ATTENDEES" | python3 -m json.tool
echo ""

# Step 9: Mark attendance for sewadar 1 on first date
echo "Step 9: Marking attendance for sewadar 1 on 2024-12-26..."
ATTENDANCE1=$(curl -s -X POST http://localhost:8080/api/attendances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"programId\": $PROGRAM_ID,
    \"programDate\": \"2024-12-26\",
    \"sewadarIds\": [$SEWADAR1_ID],
    \"notes\": \"Present on first day\"
  }")
echo "$ATTENDANCE1" | python3 -m json.tool
echo ""

# Step 10: Mark attendance for sewadar 1 on second date
echo "Step 10: Marking attendance for sewadar 1 on 2024-12-27..."
ATTENDANCE2=$(curl -s -X POST http://localhost:8080/api/attendances \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"programId\": $PROGRAM_ID,
    \"programDate\": \"2024-12-27\",
    \"sewadarIds\": [$SEWADAR1_ID]
  }")
echo "$ATTENDANCE2" | python3 -m json.tool
echo ""

# Step 11: Get attendance by program
echo "Step 11: Getting all attendance for program..."
PROGRAM_ATTENDANCE=$(curl -s -X GET "http://localhost:8080/api/attendances/program/$PROGRAM_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "$PROGRAM_ATTENDANCE" | python3 -m json.tool
echo ""

# Step 12: Get attendance summary for sewadar 1
echo "Step 12: Getting attendance summary for sewadar 1..."
SEWADAR_SUMMARY=$(curl -s -X GET "http://localhost:8080/api/attendances/sewadar/$SEWADAR1_ID/summary" \
  -H "Authorization: Bearer $TOKEN")
echo "$SEWADAR_SUMMARY" | python3 -m json.tool
echo ""

# Step 13: Edit program dates (remove one, add one)
echo "Step 13: Editing program dates (removing 2024-12-28, adding 2024-12-29)..."
UPDATE_PROGRAM=$(curl -s -X PUT "http://localhost:8080/api/programs/$PROGRAM_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"title\": \"Test Program\",
    \"description\": \"Test program for attendance\",
    \"location\": \"BEAS\",
    \"status\": \"active\",
    \"maxSewadars\": 10,
    \"createdById\": $INCHARGE_ID,
    \"programDates\": [\"2024-12-26\", \"2024-12-27\", \"2024-12-29\"]
  }")
echo "$UPDATE_PROGRAM" | python3 -m json.tool
echo ""

# Step 14: Verify attendance records after date edit
echo "Step 14: Verifying attendance records after date edit..."
PROGRAM_ATTENDANCE_AFTER=$(curl -s -X GET "http://localhost:8080/api/attendances/program/$PROGRAM_ID" \
  -H "Authorization: Bearer $TOKEN")
echo "$PROGRAM_ATTENDANCE_AFTER" | python3 -m json.tool
echo ""

echo "=== Test Complete ==="


