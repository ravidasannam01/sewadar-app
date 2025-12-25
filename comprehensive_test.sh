#!/bin/bash
# Comprehensive Backend Testing Script
# Tests all flows with 2 sewadars including edge cases

BASE_URL="http://localhost:8080"
TOKEN=""
INCHARGE_ID=""
SEWADAR1_ID=""
SEWADAR2_ID=""
PROGRAM1_ID=""
PROGRAM2_ID=""

echo "=========================================="
echo "COMPREHENSIVE BACKEND TESTING"
echo "=========================================="
echo ""

# Helper function to extract JSON values
extract_json() {
    echo "$1" | grep -o "\"$2\":\"[^\"]*" | cut -d'"' -f4
}

extract_json_number() {
    echo "$1" | grep -o "\"$2\":[0-9]*" | cut -d':' -f2
}

# Step 1: Check bootstrap status
echo "STEP 1: Checking bootstrap status..."
BOOTSTRAP_RESPONSE=$(curl -s -X GET "$BASE_URL/api/bootstrap/status")
echo "Response: $BOOTSTRAP_RESPONSE"
echo ""

# Step 2: Create first incharge
echo "STEP 2: Creating first incharge..."
INCHARGE_RESPONSE=$(curl -s -X POST "$BASE_URL/api/bootstrap/create-incharge" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Admin",
    "lastName": "Incharge",
    "mobile": "9999999999",
    "password": "admin123",
    "location": "BEAS",
    "profession": "Administrator",
    "dateOfBirth": "1980-01-01",
    "emergencyContact": "8888888888",
    "emergencyContactRelationship": "Spouse",
    "aadharNumber": "123456789012",
    "languages": ["Hindi", "English"],
    "photoUrl": "https://example.com/admin.jpg"
  }')
echo "Response: $INCHARGE_RESPONSE"
INCHARGE_ID=$(extract_json_number "$INCHARGE_RESPONSE" "zonalId")
echo "Incharge Zonal ID: $INCHARGE_ID"
echo ""

# Step 3: Login as incharge
echo "STEP 3: Logging in as incharge..."
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"zonalId\":\"$INCHARGE_ID\",\"password\":\"admin123\"}")
TOKEN=$(extract_json "$LOGIN_RESPONSE" "token")
echo "Token obtained (length: ${#TOKEN})"
echo ""

# Step 4: Create Sewadar 1
echo "STEP 4: Creating Sewadar 1..."
SEWADAR1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sewadars" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "firstName": "Raghu",
    "lastName": "Verma",
    "mobile": "8500601336",
    "password": "sewadar123",
    "location": "BEAS",
    "profession": "Teacher",
    "dateOfBirth": "1990-01-15",
    "emergencyContact": "9876543210",
    "emergencyContactRelationship": "Spouse",
    "aadharNumber": "111122223333",
    "languages": ["Hindi", "English"],
    "photoUrl": "https://example.com/raghu.jpg",
    "address1": "123 Main Street",
    "address2": "Apt 4B",
    "email": "raghu@example.com"
  }')
echo "Response: $SEWADAR1_RESPONSE"
SEWADAR1_ID=$(extract_json_number "$SEWADAR1_RESPONSE" "zonalId")
echo "Sewadar 1 Zonal ID: $SEWADAR1_ID"
echo ""

# Step 5: Create Sewadar 2
echo "STEP 5: Creating Sewadar 2..."
SEWADAR2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/sewadars" \
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
    "aadharNumber": "444455556666",
    "languages": ["Hindi", "English", "Punjabi"],
    "photoUrl": "https://example.com/krishna.jpg"
  }')
echo "Response: $SEWADAR2_RESPONSE"
SEWADAR2_ID=$(extract_json_number "$SEWADAR2_RESPONSE" "zonalId")
echo "Sewadar 2 Zonal ID: $SEWADAR2_ID"
echo ""

# Step 6: Create Program 1 (Active)
echo "STEP 6: Creating Program 1 (Active)..."
TODAY=$(date +%Y-%m-%d)
TOMORROW=$(date -v+1d +%Y-%m-%d 2>/dev/null || date -d "+1 day" +%Y-%m-%d 2>/dev/null || echo "2025-12-26")
DAY3=$(date -v+2d +%Y-%m-%d 2>/dev/null || date -d "+2 days" +%Y-%m-%d 2>/dev/null || echo "2025-12-27")

PROGRAM1_RESPONSE=$(curl -s -X POST "$BASE_URL/api/programs" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"title\": \"BEAS Program 1\",
    \"description\": \"First test program\",
    \"location\": \"BEAS\",
    \"status\": \"active\",
    \"maxSewadars\": 5,
    \"createdById\": $INCHARGE_ID,
    \"programDates\": [\"$TODAY\", \"$TOMORROW\", \"$DAY3\"]
  }")
echo "Response: $PROGRAM1_RESPONSE"
PROGRAM1_ID=$(extract_json_number "$PROGRAM1_RESPONSE" "id")
echo "Program 1 ID: $PROGRAM1_ID"
echo ""

# Step 7: Create Program 2 (Scheduled)
echo "STEP 7: Creating Program 2 (Scheduled)..."
PROGRAM2_RESPONSE=$(curl -s -X POST "$BASE_URL/api/programs" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"title\": \"Delhi Program 2\",
    \"description\": \"Second test program\",
    \"location\": \"Delhi\",
    \"status\": \"scheduled\",
    \"maxSewadars\": 3,
    \"createdById\": $INCHARGE_ID,
    \"programDates\": [\"$TODAY\", \"$TOMORROW\"]
  }")
echo "Response: $PROGRAM2_RESPONSE"
PROGRAM2_ID=$(extract_json_number "$PROGRAM2_RESPONSE" "id")
echo "Program 2 ID: $PROGRAM2_ID"
echo ""

echo "=========================================="
echo "BASIC SETUP COMPLETE"
echo "Incharge ID: $INCHARGE_ID"
echo "Sewadar 1 ID: $SEWADAR1_ID"
echo "Sewadar 2 ID: $SEWADAR2_ID"
echo "Program 1 ID: $PROGRAM1_ID (Active)"
echo "Program 2 ID: $PROGRAM2_ID (Scheduled)"
echo "=========================================="
echo ""

