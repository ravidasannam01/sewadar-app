#!/bin/bash

# Comprehensive Test Script for All Requirements
# Tests all endpoints and business logic

API_BASE="http://localhost:8080/api"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "  COMPREHENSIVE REQUIREMENTS TEST"
echo "=========================================="
echo ""

# Test 1: Create Sewadar
echo -e "${YELLOW}Test 1: Create Sewadar${NC}"
RESPONSE=$(curl -s -X POST "$API_BASE/sewadars" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Test",
    "lastName": "User",
    "mobile": "9999999999",
    "password": "test123",
    "dept": "Testing",
    "profession": "Tester",
    "joiningDate": "2024-01-01"
  }')
SEWADAR_ID=$(echo $RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
if [ ! -z "$SEWADAR_ID" ]; then
  echo -e "${GREEN}✓ Sewadar created with ID: $SEWADAR_ID${NC}"
else
  echo -e "${RED}✗ Failed to create sewadar${NC}"
  echo $RESPONSE
fi
echo ""

# Test 2: Login
echo -e "${YELLOW}Test 2: Login${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "$API_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"9876543210","password":"admin123"}')
TOKEN=$(echo $LOGIN_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
if [ ! -z "$TOKEN" ]; then
  echo -e "${GREEN}✓ Login successful, token obtained${NC}"
  AUTH_HEADER="Authorization: Bearer $TOKEN"
else
  echo -e "${RED}✗ Login failed${NC}"
  echo $LOGIN_RESPONSE
  exit 1
fi
echo ""

# Test 3: Get All Sewadars (with auth)
echo -e "${YELLOW}Test 3: Get All Sewadars (Authenticated)${NC}"
RESPONSE=$(curl -s -X GET "$API_BASE/sewadars" -H "$AUTH_HEADER")
COUNT=$(echo $RESPONSE | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ ! -z "$COUNT" ] && [ "$COUNT" -gt 0 ]; then
  echo -e "${GREEN}✓ Retrieved $COUNT sewadars${NC}"
else
  echo -e "${RED}✗ Failed to get sewadars${NC}"
fi
echo ""

# Test 4: Promote Sewadar to Incharge
echo -e "${YELLOW}Test 4: Promote Sewadar to Incharge${NC}"
if [ ! -z "$SEWADAR_ID" ]; then
  PROMOTE_RESPONSE=$(curl -s -X POST "$API_BASE/sewadars/$SEWADAR_ID/promote?inchargeId=1" -H "$AUTH_HEADER")
  ROLE=$(echo $PROMOTE_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['role'])" 2>/dev/null)
  if [ "$ROLE" == "INCHARGE" ]; then
    echo -e "${GREEN}✓ Sewadar promoted to INCHARGE successfully${NC}"
  else
    echo -e "${RED}✗ Promotion failed${NC}"
    echo $PROMOTE_RESPONSE
  fi
else
  echo -e "${YELLOW}⚠ Skipped (no sewadar ID)${NC}"
fi
echo ""

# Test 5: Create Program (with multiple dates)
echo -e "${YELLOW}Test 5: Create Program with Multiple Dates${NC}"
PROGRAM_RESPONSE=$(curl -s -X POST "$API_BASE/programs" \
  -H "$AUTH_HEADER" \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Test Program",
    "description": "Testing program creation",
    "location": "Mumbai",
    "programDates": ["2025-01-15", "2025-01-16", "2025-01-17"],
    "maxSewadars": 10,
    "createdById": 1
  }')
PROGRAM_ID=$(echo $PROGRAM_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
if [ ! -z "$PROGRAM_ID" ]; then
  echo -e "${GREEN}✓ Program created with ID: $PROGRAM_ID${NC}"
  DATES_COUNT=$(echo $PROGRAM_RESPONSE | python3 -c "import sys, json; print(len(json.load(sys.stdin).get('programDates', [])))" 2>/dev/null)
  echo -e "${GREEN}  Program has $DATES_COUNT dates${NC}"
else
  echo -e "${RED}✗ Failed to create program${NC}"
  echo $PROGRAM_RESPONSE
fi
echo ""

# Test 6: Apply to Program
echo -e "${YELLOW}Test 6: Apply to Program${NC}"
if [ ! -z "$PROGRAM_ID" ] && [ ! -z "$SEWADAR_ID" ]; then
  # Login as the test sewadar first
  SEWADAR_TOKEN=$(curl -s -X POST "$API_BASE/auth/login" \
    -H "Content-Type: application/json" \
    -d '{"mobile":"9999999999","password":"test123"}' | python3 -c "import sys, json; print(json.load(sys.stdin)['token'])" 2>/dev/null)
  
  if [ ! -z "$SEWADAR_TOKEN" ]; then
    APP_RESPONSE=$(curl -s -X POST "$API_BASE/program-applications" \
      -H "Authorization: Bearer $SEWADAR_TOKEN" \
      -H "Content-Type: application/json" \
      -d "{\"programId\": $PROGRAM_ID, \"sewadarId\": $SEWADAR_ID}")
    APP_ID=$(echo $APP_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
    if [ ! -z "$APP_ID" ]; then
      echo -e "${GREEN}✓ Application created with ID: $APP_ID${NC}"
    else
      echo -e "${RED}✗ Failed to create application${NC}"
      echo $APP_RESPONSE
    fi
  fi
else
  echo -e "${YELLOW}⚠ Skipped (no program or sewadar ID)${NC}"
fi
echo ""

# Test 7: Create Action
echo -e "${YELLOW}Test 7: Create Action for Program${NC}"
if [ ! -z "$PROGRAM_ID" ]; then
  ACTION_RESPONSE=$(curl -s -X POST "$API_BASE/actions" \
    -H "$AUTH_HEADER" \
    -H "Content-Type: application/json" \
    -d "{
      \"programId\": $PROGRAM_ID,
      \"title\": \"Provide Travel Details\",
      \"description\": \"Please provide your travel specifications\",
      \"actionType\": \"TRAVEL_DETAILS\",
      \"createdById\": 1,
      \"sequenceOrder\": 1
    }")
  ACTION_ID=$(echo $ACTION_RESPONSE | python3 -c "import sys, json; print(json.load(sys.stdin)['id'])" 2>/dev/null)
  if [ ! -z "$ACTION_ID" ]; then
    echo -e "${GREEN}✓ Action created with ID: $ACTION_ID${NC}"
  else
    echo -e "${RED}✗ Failed to create action${NC}"
    echo $ACTION_RESPONSE
  fi
else
  echo -e "${YELLOW}⚠ Skipped (no program ID)${NC}"
fi
echo ""

# Test 8: Get Programs by Incharge
echo -e "${YELLOW}Test 8: Get Programs by Incharge${NC}"
RESPONSE=$(curl -s -X GET "$API_BASE/programs/incharge/1" -H "$AUTH_HEADER")
COUNT=$(echo $RESPONSE | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
if [ ! -z "$COUNT" ]; then
  echo -e "${GREEN}✓ Found $COUNT program(s) for incharge${NC}"
else
  echo -e "${RED}✗ Failed to get programs${NC}"
fi
echo ""

# Test 9: Get Applications for Program
echo -e "${YELLOW}Test 9: Get Applications for Program${NC}"
if [ ! -z "$PROGRAM_ID" ]; then
  RESPONSE=$(curl -s -X GET "$API_BASE/program-applications/program/$PROGRAM_ID" -H "$AUTH_HEADER")
  COUNT=$(echo $RESPONSE | python3 -c "import sys, json; print(len(json.load(sys.stdin)))" 2>/dev/null)
  if [ ! -z "$COUNT" ]; then
    echo -e "${GREEN}✓ Found $COUNT application(s) for program${NC}"
  else
    echo -e "${YELLOW}⚠ No applications found (this is OK)${NC}"
  fi
else
  echo -e "${YELLOW}⚠ Skipped (no program ID)${NC}"
fi
echo ""

# Test 10: Validate Token
echo -e "${YELLOW}Test 10: Validate JWT Token${NC}"
VALIDATE_RESPONSE=$(curl -s -X GET "$API_BASE/auth/validate" -H "$AUTH_HEADER")
if echo "$VALIDATE_RESPONSE" | grep -q "valid\|true"; then
  echo -e "${GREEN}✓ Token is valid${NC}"
else
  echo -e "${RED}✗ Token validation failed${NC}"
  echo $VALIDATE_RESPONSE
fi
echo ""

echo "=========================================="
echo -e "${GREEN}  TEST COMPLETE${NC}"
echo "=========================================="

