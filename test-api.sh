#!/bin/bash

# Complete API Testing Script for Sewadar & Schedule CRUD APIs
# Usage: ./test-api.sh

BASE_URL="http://localhost:8080"
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Sewadar & Schedule API Test Suite${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""

# Check if jq is installed
if ! command -v jq &> /dev/null; then
    echo -e "${YELLOW}Warning: jq is not installed. Install it for better JSON output.${NC}"
    echo -e "${YELLOW}macOS: brew install jq${NC}"
    echo -e "${YELLOW}Linux: sudo apt-get install jq${NC}"
    echo ""
    JQ_CMD="cat"
else
    JQ_CMD="jq"
fi

# Function to print test result
print_result() {
    if [ $1 -eq 0 ]; then
        echo -e "${GREEN}✓${NC} $2"
    else
        echo -e "${RED}✗${NC} $2"
    fi
}

# Test 1: Create Address 1
echo -e "${YELLOW}Test 1: Creating Address 1...${NC}"
ADDRESS1=$(curl -s -X POST $BASE_URL/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"123 Main Street","address2":"Apt 4B","email":"addr1@test.com"}' | $JQ_CMD -r '.id // empty')
if [ -n "$ADDRESS1" ]; then
    print_result 0 "Address 1 created with ID: $ADDRESS1"
else
    print_result 1 "Failed to create Address 1"
    exit 1
fi

# Test 2: Create Address 2
echo -e "${YELLOW}Test 2: Creating Address 2...${NC}"
ADDRESS2=$(curl -s -X POST $BASE_URL/api/addresses \
  -H "Content-Type: application/json" \
  -d '{"address1":"456 Oak Avenue","address2":"Floor 2","email":"addr2@test.com"}' | $JQ_CMD -r '.id // empty')
if [ -n "$ADDRESS2" ]; then
    print_result 0 "Address 2 created with ID: $ADDRESS2"
else
    print_result 1 "Failed to create Address 2"
    exit 1
fi

# Test 3: Get All Addresses
echo -e "${YELLOW}Test 3: Getting all addresses...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/addresses)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get all addresses successful"
else
    print_result 1 "Get all addresses failed with code: $HTTP_CODE"
fi

# Test 4: Create Sewadar 1
echo -e "${YELLOW}Test 4: Creating Sewadar 1...${NC}"
SEWADAR1=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Test\"}" | $JQ_CMD -r '.id // empty')
if [ -n "$SEWADAR1" ]; then
    print_result 0 "Sewadar 1 created with ID: $SEWADAR1"
else
    print_result 1 "Failed to create Sewadar 1"
    exit 1
fi

# Test 5: Create Sewadar 2
echo -e "${YELLOW}Test 5: Creating Sewadar 2...${NC}"
SEWADAR2=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"Jane\",\"lastName\":\"Smith\",\"dept\":\"HR\",\"mobile\":\"0987654321\",\"addressId\":$ADDRESS2,\"remarks\":\"Test\"}" | $JQ_CMD -r '.id // empty')
if [ -n "$SEWADAR2" ]; then
    print_result 0 "Sewadar 2 created with ID: $SEWADAR2"
else
    print_result 1 "Failed to create Sewadar 2"
    exit 1
fi

# Test 6: Create Sewadar 3 (without address)
echo -e "${YELLOW}Test 6: Creating Sewadar 3 (without address)...${NC}"
SEWADAR3=$(curl -s -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Bob","lastName":"Johnson","dept":"Finance","mobile":"5555555555","remarks":"No address"}' | $JQ_CMD -r '.id // empty')
if [ -n "$SEWADAR3" ]; then
    print_result 0 "Sewadar 3 created with ID: $SEWADAR3"
else
    print_result 1 "Failed to create Sewadar 3"
    exit 1
fi

# Test 7: Get All Sewadars
echo -e "${YELLOW}Test 7: Getting all sewadars...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/sewadars)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get all sewadars successful"
else
    print_result 1 "Get all sewadars failed with code: $HTTP_CODE"
fi

# Test 8: Get Sewadar by ID
echo -e "${YELLOW}Test 8: Getting sewadar by ID...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/sewadars/$SEWADAR1)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get sewadar by ID successful"
else
    print_result 1 "Get sewadar by ID failed with code: $HTTP_CODE"
fi

# Test 9: Update Sewadar
echo -e "${YELLOW}Test 9: Updating sewadar...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X PUT $BASE_URL/api/sewadars/$SEWADAR1 \
  -H "Content-Type: application/json" \
  -d "{\"firstName\":\"John\",\"lastName\":\"Doe\",\"dept\":\"IT Senior\",\"mobile\":\"1234567890\",\"addressId\":$ADDRESS1,\"remarks\":\"Updated\"}")
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Update sewadar successful"
else
    print_result 1 "Update sewadar failed with code: $HTTP_CODE"
fi

# Test 10: Create Schedule 1
echo -e "${YELLOW}Test 10: Creating Schedule 1...${NC}"
SCHEDULE1=$(curl -s -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Community Center\",\"scheduledDate\":\"2024-02-15\",\"scheduledTime\":\"10:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}" | $JQ_CMD -r '.id // empty')
if [ -n "$SCHEDULE1" ]; then
    print_result 0 "Schedule 1 created with ID: $SCHEDULE1"
else
    print_result 1 "Failed to create Schedule 1"
    exit 1
fi

# Test 11: Create Schedule 2
echo -e "${YELLOW}Test 11: Creating Schedule 2...${NC}"
SCHEDULE2=$(curl -s -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Online Meeting\",\"scheduledDate\":\"2024-02-20\",\"scheduledTime\":\"14:30:00\",\"scheduledMedium\":\"Zoom\",\"attendedById\":$SEWADAR1}" | $JQ_CMD -r '.id // empty')
if [ -n "$SCHEDULE2" ]; then
    print_result 0 "Schedule 2 created with ID: $SCHEDULE2"
else
    print_result 1 "Failed to create Schedule 2"
    exit 1
fi

# Test 12: Get All Schedules
echo -e "${YELLOW}Test 12: Getting all schedules...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/schedules)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get all schedules successful"
else
    print_result 1 "Get all schedules failed with code: $HTTP_CODE"
fi

# Test 13: Get Schedule by ID
echo -e "${YELLOW}Test 13: Getting schedule by ID...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/schedules/$SCHEDULE1)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Get schedule by ID successful"
else
    print_result 1 "Get schedule by ID failed with code: $HTTP_CODE"
fi

# Test 14: Update Schedule
echo -e "${YELLOW}Test 14: Updating schedule...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X PUT $BASE_URL/api/schedules/$SCHEDULE1 \
  -H "Content-Type: application/json" \
  -d "{\"scheduledPlace\":\"Updated Center\",\"scheduledDate\":\"2024-02-16\",\"scheduledTime\":\"11:00:00\",\"scheduledMedium\":\"In-Person\",\"attendedById\":$SEWADAR1}")
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "200" ]; then
    print_result 0 "Update schedule successful"
else
    print_result 1 "Update schedule failed with code: $HTTP_CODE"
fi

# Test 15: Error - Get Non-existent Sewadar
echo -e "${YELLOW}Test 15: Testing 404 error (non-existent resource)...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/sewadars/999)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "404" ]; then
    print_result 0 "404 error handling works correctly"
else
    print_result 1 "404 error handling failed, got code: $HTTP_CODE"
fi

# Test 16: Error - Validation Error
echo -e "${YELLOW}Test 16: Testing validation error...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X POST $BASE_URL/api/sewadars \
  -H "Content-Type: application/json" \
  -d '{"lastName":"Doe"}')
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "400" ]; then
    print_result 0 "Validation error handling works correctly"
else
    print_result 1 "Validation error handling failed, got code: $HTTP_CODE"
fi

# Test 17: Error - Invalid Foreign Key
echo -e "${YELLOW}Test 17: Testing invalid foreign key...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X POST $BASE_URL/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"scheduledPlace":"Test","scheduledDate":"2024-02-15","scheduledTime":"10:00:00","scheduledMedium":"In-Person","attendedById":999}')
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "404" ]; then
    print_result 0 "Invalid foreign key error handling works correctly"
else
    print_result 1 "Invalid foreign key error handling failed, got code: $HTTP_CODE"
fi

# Test 18: Delete Schedule
echo -e "${YELLOW}Test 18: Deleting schedule...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X DELETE $BASE_URL/api/schedules/$SCHEDULE2)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "204" ]; then
    print_result 0 "Delete schedule successful"
else
    print_result 1 "Delete schedule failed with code: $HTTP_CODE"
fi

# Test 19: Delete Sewadar
echo -e "${YELLOW}Test 19: Deleting sewadar...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X DELETE $BASE_URL/api/sewadars/$SEWADAR3)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "204" ]; then
    print_result 0 "Delete sewadar successful"
else
    print_result 1 "Delete sewadar failed with code: $HTTP_CODE"
fi

# Test 20: Verify Deletion
echo -e "${YELLOW}Test 20: Verifying deletion...${NC}"
RESPONSE=$(curl -s -w "%{http_code}" -X GET $BASE_URL/api/schedules/$SCHEDULE2)
HTTP_CODE="${RESPONSE: -3}"
if [ "$HTTP_CODE" = "404" ]; then
    print_result 0 "Deletion verified (resource not found)"
else
    print_result 1 "Deletion verification failed, got code: $HTTP_CODE"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  Test Suite Complete!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "Created Resources:"
echo "  Address IDs: $ADDRESS1, $ADDRESS2"
echo "  Sewadar IDs: $SEWADAR1, $SEWADAR2"
echo "  Schedule ID: $SCHEDULE1"
echo ""
echo "You can now test manually using the IDs above."
echo "See TESTING_GUIDE.md for detailed instructions."

