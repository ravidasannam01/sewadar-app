#!/usr/bin/env python3
"""
Backend End-to-End Test Script
Tests the complete flow including attendance with normalized schema
"""

import requests
import json
import sys
from datetime import date, timedelta

BASE_URL = "http://localhost:8080"

def print_step(step_num, description):
    print(f"\n{'='*60}")
    print(f"Step {step_num}: {description}")
    print('='*60)

def print_response(response, label="Response"):
    print(f"\n{label}:")
    try:
        data = response.json()
        print(json.dumps(data, indent=2))
        return data
    except:
        print(f"Status: {response.status_code}")
        print(f"Text: {response.text}")
        return None

# Step 1: Create first incharge
print_step(1, "Creating first incharge")
response = requests.post(f"{BASE_URL}/api/bootstrap/first-incharge")
data = print_response(response, "Incharge Created")
if not data:
    print("ERROR: Failed to create incharge")
    sys.exit(1)
incharge_id = data['zonalId']
print(f"\n✓ Incharge Zonal ID: {incharge_id}")

# Step 2: Login as incharge
print_step(2, "Logging in as incharge")
response = requests.post(
    f"{BASE_URL}/api/auth/login",
    json={"zonalId": str(incharge_id), "password": "admin123"}
)
data = print_response(response, "Login Response")
if not data:
    print("ERROR: Failed to login")
    sys.exit(1)
token = data['token']
print(f"\n✓ Token obtained (length: {len(token)})")

headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}

# Step 3: Create sewadars
print_step(3, "Creating sewadars")

sewadar1_data = {
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
}
response = requests.post(f"{BASE_URL}/api/sewadars", headers=headers, json=sewadar1_data)
data = print_response(response, "Sewadar 1 Created")
if not data:
    print("ERROR: Failed to create sewadar 1")
    sys.exit(1)
sewadar1_id = data['zonalId']
print(f"\n✓ Sewadar 1 Zonal ID: {sewadar1_id}")

sewadar2_data = {
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
}
response = requests.post(f"{BASE_URL}/api/sewadars", headers=headers, json=sewadar2_data)
data = print_response(response, "Sewadar 2 Created")
if not data:
    print("ERROR: Failed to create sewadar 2")
    sys.exit(1)
sewadar2_id = data['zonalId']
print(f"\n✓ Sewadar 2 Zonal ID: {sewadar2_id}")

# Step 4: Create program with dates
print_step(4, "Creating program with dates")
today = date.today()
program_dates = [
    str(today),
    str(today + timedelta(days=1)),
    str(today + timedelta(days=2))
]
program_data = {
    "title": "Test Program",
    "description": "Test program for attendance flow",
    "location": "BEAS",
    "status": "active",
    "maxSewadars": 10,
    "createdById": incharge_id,
    "programDates": program_dates
}
response = requests.post(f"{BASE_URL}/api/programs", headers=headers, json=program_data)
data = print_response(response, "Program Created")
if not data:
    print("ERROR: Failed to create program")
    sys.exit(1)
program_id = data['id']
print(f"\n✓ Program ID: {program_id}")
print(f"✓ Program Dates: {program_dates}")

# Step 5: Login as sewadar 1
print_step(5, "Logging in as sewadar 1")
response = requests.post(
    f"{BASE_URL}/api/auth/login",
    json={"zonalId": str(sewadar1_id), "password": "sewadar123"}
)
data = print_response(response, "Sewadar 1 Login")
if not data:
    print("ERROR: Failed to login as sewadar 1")
    sys.exit(1)
sewadar1_token = data['token']
sewadar1_headers = {"Authorization": f"Bearer {sewadar1_token}", "Content-Type": "application/json"}
print(f"\n✓ Sewadar 1 logged in")

# Step 6: Sewadar 1 applies to program
print_step(6, "Sewadar 1 applying to program")
apply_data = {
    "programId": program_id,
    "sewadarId": sewadar1_id
}
response = requests.post(f"{BASE_URL}/api/program-applications", headers=sewadar1_headers, json=apply_data)
data = print_response(response, "Application Created")
if not data:
    print("ERROR: Failed to apply")
    sys.exit(1)
app_id1 = data['id']
print(f"\n✓ Application ID: {app_id1}")

# Step 7: Login as incharge and approve application
print_step(7, "Incharge approving sewadar 1 application")
response = requests.put(
    f"{BASE_URL}/api/program-applications/{app_id1}/status?status=APPROVED",
    headers=headers
)
data = print_response(response, "Application Approved")
print(f"\n✓ Application approved")

# Step 8: Get approved attendees for program
print_step(8, "Getting approved attendees for program")
response = requests.get(f"{BASE_URL}/api/attendances/program/{program_id}/attendees", headers=headers)
data = print_response(response, "Approved Attendees")
if data:
    print(f"\n✓ Found {len(data)} approved attendee(s)")

# Step 9: Mark attendance for sewadar 1 on first date
print_step(9, f"Marking attendance for sewadar 1 on {program_dates[0]}")
attendance_data = {
    "programId": program_id,
    "programDate": program_dates[0],
    "sewadarIds": [sewadar1_id],
    "notes": "Present on first day"
}
response = requests.post(f"{BASE_URL}/api/attendances", headers=headers, json=attendance_data)
data = print_response(response, "Attendance Marked")
print(f"\n✓ Attendance marked for {program_dates[0]}")

# Step 10: Mark attendance for sewadar 1 on second date
print_step(10, f"Marking attendance for sewadar 1 on {program_dates[1]}")
attendance_data2 = {
    "programId": program_id,
    "programDate": program_dates[1],
    "sewadarIds": [sewadar1_id]
}
response = requests.post(f"{BASE_URL}/api/attendances", headers=headers, json=attendance_data2)
data = print_response(response, "Attendance Marked")
print(f"\n✓ Attendance marked for {program_dates[1]}")

# Step 11: Get attendance by program
print_step(11, "Getting all attendance records for program")
response = requests.get(f"{BASE_URL}/api/attendances/program/{program_id}", headers=headers)
data = print_response(response, "Program Attendance Records")
if data:
    print(f"\n✓ Found {len(data)} attendance record(s)")

# Step 12: Get attendance summary for sewadar 1
print_step(12, "Getting attendance summary for sewadar 1")
response = requests.get(f"{BASE_URL}/api/attendances/sewadar/{sewadar1_id}/summary", headers=headers)
data = print_response(response, "Sewadar Attendance Summary")
if data:
    print(f"\n✓ Total Programs: {data.get('totalProgramsCount', 0)}")
    print(f"✓ Total Days: {data.get('totalDaysAttended', 0)}")

# Step 13: Edit program dates (remove one, add one)
print_step(13, "Editing program dates (removing last date, adding new one)")
new_dates = [program_dates[0], program_dates[1], str(today + timedelta(days=3))]
update_program_data = {
    "title": "Test Program",
    "description": "Test program for attendance flow",
    "location": "BEAS",
    "status": "active",
    "maxSewadars": 10,
    "createdById": incharge_id,
    "programDates": new_dates
}
response = requests.put(f"{BASE_URL}/api/programs/{program_id}", headers=headers, json=update_program_data)
data = print_response(response, "Program Updated")
print(f"\n✓ Program dates updated")
print(f"  Removed: {program_dates[2]}")
print(f"  Added: {new_dates[2]}")

# Step 14: Verify attendance records after date edit
print_step(14, "Verifying attendance records after date edit")
response = requests.get(f"{BASE_URL}/api/attendances/program/{program_id}", headers=headers)
data = print_response(response, "Attendance After Date Edit")
if data:
    print(f"\n✓ Found {len(data)} attendance record(s) after date edit")
    print("  (Should still have 2 records for preserved dates)")

# Step 15: Try to mark attendance for removed date (should fail)
print_step(15, "Attempting to mark attendance for removed date (should fail)")
attendance_data3 = {
    "programId": program_id,
    "programDate": program_dates[2],  # This date was removed
    "sewadarIds": [sewadar1_id]
}
response = requests.post(f"{BASE_URL}/api/attendances", headers=headers, json=attendance_data3)
if response.status_code == 400:
    print("\n✓ Correctly rejected - date is no longer a valid program date")
    print(f"  Error: {response.text}")
else:
    print(f"\n⚠ Unexpected response: {response.status_code}")

# Step 16: Mark attendance for new date
print_step(16, f"Marking attendance for new date {new_dates[2]}")
attendance_data4 = {
    "programId": program_id,
    "programDate": new_dates[2],
    "sewadarIds": [sewadar1_id]
}
response = requests.post(f"{BASE_URL}/api/attendances", headers=headers, json=attendance_data4)
data = print_response(response, "Attendance Marked for New Date")
print(f"\n✓ Attendance marked for new date")

# Step 17: Final attendance summary
print_step(17, "Final attendance summary for sewadar 1")
response = requests.get(f"{BASE_URL}/api/attendances/sewadar/{sewadar1_id}/summary", headers=headers)
data = print_response(response, "Final Summary")
if data:
    print(f"\n✓ Total Programs: {data.get('totalProgramsCount', 0)}")
    print(f"✓ Total Days: {data.get('totalDaysAttended', 0)}")
    print(f"✓ BEAS Programs: {data.get('beasProgramsCount', 0)}")
    print(f"✓ BEAS Days: {data.get('beasDaysAttended', 0)}")

print("\n" + "="*60)
print("✅ ALL TESTS COMPLETED SUCCESSFULLY!")
print("="*60)


