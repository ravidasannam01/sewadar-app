#!/bin/bash

# Test script for Zonal ID Refactoring
# Tests basic CRUD operations with String zonalId

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "Zonal ID Refactoring Test Suite"
echo "=========================================="
echo ""

# Test 1: Create Sewadar with String zonalId
echo -e "${YELLOW}Test 1: Create Sewadar with String zonalId${NC}"
ZONAL_ID="ABC123"
RESPONSE=$(curl -s -X POST "${BASE_URL}/api/sewadars" \
  -H "Content-Type: application/json" \
  -d '{
    "zonalId": "'"${ZONAL_ID}"'",
    "firstName": "Test",
    "lastName": "User",
    "mobile": "9876543210",
    "password": "test123",
    "location": "BEAS",
    "profession": "Engineer"
  }')

if echo "$RESPONSE" | grep -q "\"zonalId\""; then
  echo -e "${GREEN}✓ Sewadar created successfully${NC}"
  echo "Response: $RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
else
  echo -e "${RED}✗ Failed to create sewadar${NC}"
  echo "Response: $RESPONSE"
  exit 1
fi

echo ""

# Test 2: Login with String zonalId
echo -e "${YELLOW}Test 2: Login with String zonalId${NC}"
LOGIN_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "zonalId": "'"${ZONAL_ID}"'",
    "password": "test123"
  }')

TOKEN=$(echo "$LOGIN_RESPONSE" | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -n "$TOKEN" ]; then
  echo -e "${GREEN}✓ Login successful${NC}"
  echo "Token: ${TOKEN:0:50}..."
else
  echo -e "${RED}✗ Login failed${NC}"
  echo "Response: $LOGIN_RESPONSE"
  exit 1
fi

echo ""

# Test 3: Get Sewadar by String zonalId
echo -e "${YELLOW}Test 3: Get Sewadar by String zonalId${NC}"
GET_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/sewadars/${ZONAL_ID}" \
  -H "Authorization: Bearer ${TOKEN}")

if echo "$GET_RESPONSE" | grep -q "\"zonalId\":\"${ZONAL_ID}\""; then
  echo -e "${GREEN}✓ Retrieved sewadar successfully${NC}"
  echo "Response: $GET_RESPONSE" | jq '.' 2>/dev/null || echo "$GET_RESPONSE"
else
  echo -e "${RED}✗ Failed to retrieve sewadar${NC}"
  echo "Response: $GET_RESPONSE"
  exit 1
fi

echo ""

# Test 4: Update Sewadar (zonalId should not change)
echo -e "${YELLOW}Test 4: Update Sewadar (zonalId should remain unchanged)${NC}"
UPDATE_RESPONSE=$(curl -s -X PUT "${BASE_URL}/api/sewadars/${ZONAL_ID}" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "zonalId": "'"${ZONAL_ID}"'",
    "firstName": "Updated",
    "lastName": "User",
    "mobile": "9876543210",
    "location": "Delhi",
    "profession": "Manager"
  }')

if echo "$UPDATE_RESPONSE" | grep -q "\"zonalId\":\"${ZONAL_ID}\""; then
  echo -e "${GREEN}✓ Updated sewadar successfully${NC}"
  echo "Response: $UPDATE_RESPONSE" | jq '.' 2>/dev/null || echo "$UPDATE_RESPONSE"
else
  echo -e "${RED}✗ Failed to update sewadar${NC}"
  echo "Response: $UPDATE_RESPONSE"
  exit 1
fi

echo ""

# Test 5: Create another sewadar with different zonalId
echo -e "${YELLOW}Test 5: Create another sewadar with different String zonalId${NC}"
ZONAL_ID_2="XYZ789"
RESPONSE2=$(curl -s -X POST "${BASE_URL}/api/sewadars" \
  -H "Content-Type: application/json" \
  -d '{
    "zonalId": "'"${ZONAL_ID_2}"'",
    "firstName": "Second",
    "lastName": "User",
    "mobile": "9876543211",
    "password": "test123",
    "location": "Mumbai",
    "profession": "Doctor"
  }')

if echo "$RESPONSE2" | grep -q "\"zonalId\""; then
  echo -e "${GREEN}✓ Second sewadar created successfully${NC}"
else
  echo -e "${RED}✗ Failed to create second sewadar${NC}"
  echo "Response: $RESPONSE2"
fi

echo ""

# Test 6: Try to create duplicate zonalId (should fail)
echo -e "${YELLOW}Test 6: Try to create duplicate zonalId (should fail)${NC}"
DUPLICATE_RESPONSE=$(curl -s -X POST "${BASE_URL}/api/sewadars" \
  -H "Content-Type: application/json" \
  -d '{
    "zonalId": "'"${ZONAL_ID}"'",
    "firstName": "Duplicate",
    "lastName": "User",
    "mobile": "9876543212",
    "password": "test123"
  }')

if echo "$DUPLICATE_RESPONSE" | grep -qi "already exists\|duplicate\|unique"; then
  echo -e "${GREEN}✓ Correctly rejected duplicate zonalId${NC}"
else
  echo -e "${RED}✗ Should have rejected duplicate zonalId${NC}"
  echo "Response: $DUPLICATE_RESPONSE"
fi

echo ""

# Test 7: Get all sewadars
echo -e "${YELLOW}Test 7: Get all sewadars${NC}"
ALL_RESPONSE=$(curl -s -X GET "${BASE_URL}/api/sewadars" \
  -H "Authorization: Bearer ${TOKEN}")

if echo "$ALL_RESPONSE" | grep -q "\"zonalId\""; then
  echo -e "${GREEN}✓ Retrieved all sewadars successfully${NC}"
  COUNT=$(echo "$ALL_RESPONSE" | grep -o "\"zonalId\"" | wc -l)
  echo "Found $COUNT sewadar(s)"
else
  echo -e "${RED}✗ Failed to retrieve sewadars${NC}"
  echo "Response: $ALL_RESPONSE"
fi

echo ""
echo "=========================================="
echo -e "${GREEN}All basic tests completed!${NC}"
echo "=========================================="

