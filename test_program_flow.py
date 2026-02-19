#!/usr/bin/env python3
"""
Comprehensive Program Flow Test Script
Tests a single program with all features:
- Bootstrap admin and incharge check/creation
- Create 3 sewadars with complete data and email
- Promote 1 sewadar to incharge
- Create program with today's date and deadlines
- All sewadars apply
- Approve up to maxSewadars
- Mark attendance for current date
- Outputs curl commands and responses to a text file
"""

import requests
import json
from datetime import datetime, timedelta
from typing import Optional, Dict, List
import sys
import random

BASE_URL = "http://localhost:8080"
OUTPUT_FILE = "test_program_flow_output.txt"

# Configuration
ADMIN_ZONAL_ID = "ADMIN001"
ADMIN_PASSWORD = "admin123"
ADMIN_EMAIL = "sawisam08@gmail.com"
INCHARGE_ZONAL_ID = "INCH001"
INCHARGE_PASSWORD = "incharge123"
SEWADAR_PASSWORD = "sew123"
NUM_SEWADARS = 3
MAX_SEWADARS_IN_PROGRAM = 30  # Max sewadars for the program

# Email pool for random assignment
EMAIL_POOL = [
    "sawanannam9@gmail.com",
    "pda30360@gmail.com",
    "raghuyadav99591@gmail.com",
    "ramaa8500601@gmail.com"
]

class CurlLogger:
    """Helper class to log curl commands and responses"""
    def __init__(self, output_file: str):
        self.output_file = output_file
        self.file = open(output_file, 'w', encoding='utf-8')
        self.write_header()
    
    def write_header(self):
        self.file.write("="*100 + "\n")
        self.file.write("COMPREHENSIVE PROGRAM FLOW TEST - CURL COMMANDS AND RESPONSES\n")
        self.file.write(f"Generated at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        self.file.write("="*100 + "\n\n")
    
    def log_request(self, method: str, url: str, headers: Dict = None, data: Dict = None, 
                    description: str = ""):
        """Log the curl command equivalent"""
        full_url = f"{BASE_URL}{url}"
        
        self.file.write(f"\n{'='*100}\n")
        self.file.write(f"STEP: {description}\n")
        self.file.write(f"{'='*100}\n\n")
        
        # Build curl command
        curl_cmd = f"curl -X {method.upper()} '{full_url}'"
        
        if headers:
            for key, value in headers.items():
                curl_cmd += f" \\\n  -H '{key}: {value}'"
        
        if data:
            json_data = json.dumps(data, indent=2)
            curl_cmd += f" \\\n  -d '{json_data}'"
        
        self.file.write("CURL COMMAND:\n")
        self.file.write("-" * 100 + "\n")
        self.file.write(curl_cmd + "\n")
        self.file.write("-" * 100 + "\n\n")
        
        return curl_cmd
    
    def log_response(self, response: requests.Response, description: str = ""):
        """Log the response"""
        self.file.write("RESPONSE:\n")
        self.file.write("-" * 100 + "\n")
        self.file.write(f"Status Code: {response.status_code}\n")
        self.file.write(f"Status: {response.reason}\n\n")
        
        try:
            response_json = response.json()
            self.file.write("Response Body (JSON):\n")
            self.file.write(json.dumps(response_json, indent=2) + "\n")
        except:
            self.file.write("Response Body (Text):\n")
            self.file.write(response.text + "\n")
        
        self.file.write("-" * 100 + "\n\n")
    
    def log_error(self, error: Exception, description: str = ""):
        """Log errors"""
        self.file.write("ERROR:\n")
        self.file.write("-" * 100 + "\n")
        self.file.write(f"{type(error).__name__}: {str(error)}\n")
        self.file.write("-" * 100 + "\n\n")
    
    def close(self):
        self.file.close()

class TestFlow:
    def __init__(self):
        self.logger = CurlLogger(OUTPUT_FILE)
        self.admin_token: Optional[str] = None
        self.incharge_token: Optional[str] = None
        self.promoted_incharge_token: Optional[str] = None
        self.promoted_incharge_zonal_id: Optional[str] = None
        self.sewadars: List[Dict] = []
        self.program_id: Optional[int] = None
        self.applications: List[Dict] = []
        
    def print_step(self, step: str):
        print(f"\n{'='*80}")
        print(f"  {step}")
        print(f"{'='*80}")
    
    def make_request(self, method: str, url: str, headers: Dict = None, data: Dict = None,
                     description: str = "", token: Optional[str] = None) -> requests.Response:
        """Make HTTP request and log curl command and response"""
        if token:
            if headers is None:
                headers = {}
            headers['Authorization'] = f'Bearer {token}'
        
        if headers is None:
            headers = {'Content-Type': 'application/json'}
        elif 'Content-Type' not in headers:
            headers['Content-Type'] = 'application/json'
        
        curl_cmd = self.logger.log_request(method, url, headers, data, description)
        
        try:
            if method.upper() == 'GET':
                response = requests.get(f"{BASE_URL}{url}", headers=headers)
            elif method.upper() == 'POST':
                response = requests.post(f"{BASE_URL}{url}", headers=headers, json=data)
            elif method.upper() == 'PUT':
                response = requests.put(f"{BASE_URL}{url}", headers=headers, json=data)
            elif method.upper() == 'DELETE':
                response = requests.delete(f"{BASE_URL}{url}", headers=headers)
            else:
                raise ValueError(f"Unsupported method: {method}")
            
            self.logger.log_response(response, description)
            return response
        except Exception as e:
            self.logger.log_error(e, description)
            raise
    
    def check_bootstrap_status(self):
        """Check if bootstrap admin and incharge exist"""
        self.print_step("Step 1: Check Bootstrap Status")
        
        response = self.make_request('GET', '/api/bootstrap/status', 
                                    description="Check bootstrap status")
        
        if response.status_code == 200:
            status = response.json()
            print(f"  Bootstrap Status: needsBootstrap={status.get('needsBootstrap')}")
            print(f"  Has Admin: {status.get('hasAdmin')}")
            print(f"  Has Incharge: {status.get('hasIncharge')}")
            return status
        else:
            raise Exception(f"Failed to check bootstrap status: {response.status_code}")
    
    def create_bootstrap_admin(self):
        """Create bootstrap admin"""
        self.print_step("Step 2: Create Bootstrap Admin")
        
        data = {
            "firstName": "Admin",
            "lastName": "User",
            "zonalId": ADMIN_ZONAL_ID,
            "location": "BEAS",
            "mobile": "919999000001",
            "password": ADMIN_PASSWORD,
            "emailId": ADMIN_EMAIL
        }
        
        response = self.make_request('POST', '/api/bootstrap/create-admin', data=data,
                                    description="Create bootstrap admin")
        
        if response.status_code == 201:
            admin = response.json()
            print(f"  ✅ Admin created: {admin.get('zonalId')} (Email: {ADMIN_EMAIL})")
            return admin
        else:
            error_msg = response.json().get('message', response.text) if response.status_code != 201 else ""
            if "already has an admin" in error_msg:
                print(f"  ℹ️  Admin already exists")
                return None
            raise Exception(f"Failed to create admin: {response.status_code} - {error_msg}")
    
    def create_bootstrap_incharge(self):
        """Create bootstrap incharge"""
        self.print_step("Step 3: Create Bootstrap Incharge")
        
        # Assign random email from pool
        incharge_email = random.choice(EMAIL_POOL)
        
        data = {
            "firstName": "Incharge",
            "lastName": "User",
            "zonalId": INCHARGE_ZONAL_ID,
            "location": "BEAS",
            "mobile": "919999000002",
            "password": INCHARGE_PASSWORD,
            "emailId": incharge_email,
            "address1": "123 Main Street",
            "address2": "Sector 5",
            "email": "incharge.address@example.com",  # Address email
            "profession": "Manager",
            "joiningDate": "2020-01-15",
            "dateOfBirth": "1985-05-20",
            "emergencyContact": "919999000099",
            "emergencyContactRelationship": "Spouse",
            "aadharNumber": "123456789012",
            "languages": ["Hindi", "English"],
            "fatherHusbandName": "Incharge Father",
            "gender": "MALE",
            "screenerCode": "INCH001",
            "satsangPlace": "BEAS Main Hall",
            "remarks": "Bootstrap incharge user"
        }
        
        response = self.make_request('POST', '/api/bootstrap/create-incharge', data=data,
                                    description="Create bootstrap incharge")
        
        if response.status_code == 201:
            incharge = response.json()
            print(f"  ✅ Incharge created: {incharge.get('zonalId')} (Email: {incharge_email})")
            return incharge
        else:
            error_msg = response.json().get('message', response.text) if response.status_code != 201 else ""
            if "already has an incharge" in error_msg:
                print(f"  ℹ️  Incharge already exists")
                return None
            raise Exception(f"Failed to create incharge: {response.status_code} - {error_msg}")
    
    def login(self, zonal_id: str, password: str, role: str = "ADMIN"):
        """Login and get JWT token"""
        self.print_step(f"Step 4: Login as {role} ({zonal_id})")
        
        data = {
            "zonalId": zonal_id,
            "password": password
        }
        
        response = self.make_request('POST', '/api/auth/login', data=data,
                                    description=f"Login as {zonal_id}")
        
        if response.status_code == 200:
            auth_response = response.json()
            token = auth_response.get('token')
            print(f"  ✅ Login successful")
            return token
        else:
            raise Exception(f"Login failed: {response.status_code} - {response.text}")
    
    def create_sewadars(self, count: int):
        """Create sewadars with complete data and email"""
        self.print_step(f"Step 5: Create {count} Sewadars with Complete Data")
        
        # Shuffle email pool to assign randomly
        available_emails = EMAIL_POOL.copy()
        random.shuffle(available_emails)
        
        created = []
        for i in range(1, count + 1):
            zonal_id = f"SEW{i:03d}"
            # Start mobile numbers from 91999900100 to avoid conflict with ADMIN (919999000001) and INCHARGE (919999000002)
            mobile_number = f"91999900{100 + i:03d}"
            
            # Assign email from pool (cycle if needed)
            email = available_emails[(i - 1) % len(available_emails)]
            
            # Generate complete data for sewadar
            data = {
                "firstName": f"Sewadar",
                "lastName": f"User{i:03d}",
                "zonalId": zonal_id,
                "location": "Delhi" if i % 2 == 0 else "Mumbai",
                "mobile": mobile_number,
                "password": SEWADAR_PASSWORD,
                "emailId": email,  # Sewadar email
                "address1": f"{100 + i} Street",
                "address2": f"Area {i}",
                "email": f"sewadar{i}.address@example.com",  # Address email
                "profession": ["Engineer", "Teacher", "Doctor"][(i - 1) % 3],
                "joiningDate": (datetime.now() - timedelta(days=365 * (i % 3 + 1))).strftime("%Y-%m-%d"),
                "dateOfBirth": (datetime.now() - timedelta(days=365 * (25 + i % 10))).strftime("%Y-%m-%d"),
                "emergencyContact": f"91999900{200 + i:03d}",
                "emergencyContactRelationship": ["Father", "Mother", "Spouse"][(i - 1) % 3],
                "aadharNumber": f"{123456789000 + i:012d}",
                "languages": [["Hindi", "English"], ["Hindi", "Punjabi"], ["Hindi", "English", "Punjabi"]][(i - 1) % 3],
                "fatherHusbandName": f"Father{i:03d}",
                "gender": ["MALE", "FEMALE"][(i - 1) % 2],
                "screenerCode": f"SCR{i:03d}",
                "satsangPlace": ["BEAS Main Hall", "Delhi Center", "Mumbai Center"][(i - 1) % 3],
                "remarks": f"Test sewadar {i} with complete data"
            }
            
            try:
                response = self.make_request('POST', '/api/sewadars', data=data,
                                          description=f"Create sewadar {zonal_id} with complete data",
                                          token=self.admin_token)
                
                if response.status_code == 201:
                    sewadar = response.json()
                    created.append(sewadar)
                    print(f"  ✅ Created sewadar {zonal_id} (Email: {email})")
                else:
                    error_msg = response.json().get('message', response.text) if response.status_code != 201 else ""
                    print(f"  ⚠️  Failed to create sewadar {zonal_id}: {error_msg}")
            except Exception as e:
                print(f"  ⚠️  Error creating sewadar {zonal_id}: {e}")
        
        self.sewadars = created
        print(f"\n  ✅ Created {len(created)}/{count} sewadars with complete data and emails")
        return created
    
    def promote_sewadar_to_incharge(self, sewadar_zonal_id: str):
        """Promote a sewadar to incharge"""
        self.print_step(f"Step 6: Promote Sewadar {sewadar_zonal_id} to Incharge")
        
        # Use bootstrap incharge to promote
        url = f'/api/sewadars/{sewadar_zonal_id}/promote?inchargeId={INCHARGE_ZONAL_ID}&password={INCHARGE_PASSWORD}'
        
        response = self.make_request('POST', url,
                                    description=f"Promote {sewadar_zonal_id} to incharge",
                                    token=self.incharge_token)
        
        if response.status_code == 200:
            promoted = response.json()
            self.promoted_incharge_zonal_id = promoted.get('zonalId')
            print(f"  ✅ Promoted {sewadar_zonal_id} to incharge")
            
            # Login as promoted incharge
            self.promoted_incharge_token = self.login(
                self.promoted_incharge_zonal_id, 
                SEWADAR_PASSWORD, 
                "PROMOTED_INCHARGE"
            )
            return promoted
        else:
            raise Exception(f"Failed to promote sewadar: {response.status_code} - {response.text}")
    
    def create_program(self):
        """Create a program with today's date and specific deadlines"""
        self.print_step("Step 7: Create Program with Today's Date")
        
        # Calculate dates
        today = datetime.now().date()
        today_start = datetime.combine(today, datetime.min.time())
        
        # lastDateToApply: 2 hours before day closing (23:59:59 - 2 hours = 21:59:59)
        last_date_to_apply = today_start.replace(hour=21, minute=59, second=59)
        
        # lastDateToSubmitForm: 1 hour before day closing (23:59:59 - 1 hour = 22:59:59)
        last_date_to_submit_form = today_start.replace(hour=22, minute=59, second=59)
        
        data = {
            "title": f"Today's Program - {today.strftime('%Y-%m-%d')}",
            "description": "Test program with today's date and deadline enforcement",
            "location": "BEAS",
            "programDates": [today.isoformat()],
            "status": "active",
            "maxSewadars": MAX_SEWADARS_IN_PROGRAM,
            "lastDateToApply": last_date_to_apply.isoformat(),
            "lastDateToSubmitForm": last_date_to_submit_form.isoformat(),
            "createdById": self.promoted_incharge_zonal_id
        }
        
        response = self.make_request('POST', '/api/programs', data=data,
                                    description="Create program with today's date",
                                    token=self.promoted_incharge_token)
        
        if response.status_code == 201:
            program = response.json()
            self.program_id = program.get('id')
            print(f"  ✅ Program created: ID={self.program_id}, Title={program.get('title')}")
            print(f"  📅 Program Date: {today.isoformat()}")
            print(f"  ⏰ Last Date to Apply: {last_date_to_apply.isoformat()}")
            print(f"  ⏰ Last Date to Submit Form: {last_date_to_submit_form.isoformat()}")
            return program
        else:
            raise Exception(f"Failed to create program: {response.status_code} - {response.text}")
    
    def apply_all_sewadars(self):
        """Make all sewadars apply for the program"""
        self.print_step(f"Step 8: All Sewadars Apply for Program")
        
        applied = []
        for sewadar in self.sewadars:
            zonal_id = sewadar.get('zonalId')
            
            # Login as sewadar
            try:
                sewadar_token = self.login(zonal_id, SEWADAR_PASSWORD, f"SEWADAR_{zonal_id}")
            except Exception as e:
                print(f"  ⚠️  Failed to login as {zonal_id}: {e}")
                continue
            
            data = {
                "sewadarId": zonal_id,
                "programId": self.program_id
            }
            
            try:
                response = self.make_request('POST', '/api/program-applications', data=data,
                                           description=f"Apply to program as {zonal_id}",
                                           token=sewadar_token)
                
                if response.status_code == 201:
                    application = response.json()
                    applied.append(application)
                    print(f"  ✅ {zonal_id} applied")
                else:
                    error_msg = response.json().get('message', response.text) if response.status_code != 201 else ""
                    print(f"  ⚠️  {zonal_id} failed to apply: {error_msg}")
            except Exception as e:
                print(f"  ⚠️  Error applying {zonal_id}: {e}")
        
        self.applications = applied
        print(f"\n  ✅ {len(applied)}/{len(self.sewadars)} sewadars applied")
        return applied
    
    def approve_applications(self):
        """Approve applications up to maxSewadars"""
        self.print_step(f"Step 9: Approve Applications (up to {MAX_SEWADARS_IN_PROGRAM})")
        
        # Get all applications for the program
        response = self.make_request('GET', f'/api/program-applications/program/{self.program_id}',
                                    description="Get all applications for program",
                                    token=self.promoted_incharge_token)
        
        if response.status_code != 200:
            raise Exception(f"Failed to get applications: {response.status_code}")
        
        all_applications = response.json()
        pending_applications = [app for app in all_applications if app.get('status') == 'PENDING']
        
        approved_count = 0
        approved = []
        
        for app in pending_applications[:MAX_SEWADARS_IN_PROGRAM]:
            app_id = app.get('id')
            sewadar_id = app.get('sewadar', {}).get('zonalId')
            
            url = f'/api/program-applications/{app_id}/status?status=APPROVED'
            
            try:
                response = self.make_request('PUT', url,
                                            description=f"Approve application {app_id} for {sewadar_id}",
                                            token=self.promoted_incharge_token)
                
                if response.status_code == 200:
                    approved.append(response.json())
                    approved_count += 1
                    print(f"  ✅ Approved {sewadar_id} (Application ID: {app_id})")
                else:
                    error_msg = response.json().get('message', response.text) if response.status_code != 200 else ""
                    print(f"  ⚠️  Failed to approve {sewadar_id}: {error_msg}")
            except Exception as e:
                print(f"  ⚠️  Error approving {sewadar_id}: {e}")
        
        print(f"\n  ✅ Approved {approved_count} applications")
        return approved
    
    def mark_attendance(self, count: int = 5):
        """Mark attendance for a few people for current date"""
        self.print_step(f"Step 10: Mark Attendance for {count} People (Today's Date)")
        
        # Get approved attendees
        response = self.make_request('GET', f'/api/attendances/program/{self.program_id}/attendees',
                                    description="Get approved attendees",
                                    token=self.promoted_incharge_token)
        
        if response.status_code != 200:
            raise Exception(f"Failed to get attendees: {response.status_code}")
        
        attendees = response.json()
        today = datetime.now().date()
        
        # Select first 'count' attendees
        selected_attendees = attendees[:count]
        sewadar_ids = [attendee.get('zonalId') for attendee in selected_attendees]
        
        data = {
            "programId": self.program_id,
            "programDate": today.isoformat(),
            "sewadarIds": sewadar_ids,
            "notes": f"Test attendance marking for {today.isoformat()}"
        }
        
        response = self.make_request('POST', '/api/attendances', data=data,
                                    description=f"Mark attendance for {len(sewadar_ids)} sewadars",
                                    token=self.promoted_incharge_token)
        
        if response.status_code == 201:
            attendance_records = response.json()
            print(f"  ✅ Marked attendance for {len(attendance_records)} sewadars:")
            for record in attendance_records:
                sewadar_name = record.get('sewadar', {}).get('firstName', '') + ' ' + record.get('sewadar', {}).get('lastName', '')
                zonal_id = record.get('sewadar', {}).get('zonalId')
                print(f"    - {zonal_id} ({sewadar_name})")
            return attendance_records
        else:
            raise Exception(f"Failed to mark attendance: {response.status_code} - {response.text}")
    
    def run(self):
        """Run the complete test flow"""
        try:
            print("\n" + "="*80)
            print("  COMPREHENSIVE PROGRAM FLOW TEST")
            print("="*80)
            
            # Step 1: Check bootstrap status
            status = self.check_bootstrap_status()
            
            # Step 2: Create admin if needed
            if not status.get('hasAdmin'):
                self.create_bootstrap_admin()
            else:
                print("\n  ℹ️  Admin already exists, skipping creation")
            
            # Step 3: Create incharge if needed
            if not status.get('hasIncharge'):
                self.create_bootstrap_incharge()
            else:
                print("\n  ℹ️  Incharge already exists, skipping creation")
            
            # Step 4: Login as admin and incharge
            self.admin_token = self.login(ADMIN_ZONAL_ID, ADMIN_PASSWORD, "ADMIN")
            self.incharge_token = self.login(INCHARGE_ZONAL_ID, INCHARGE_PASSWORD, "INCHARGE")
            
            # Step 5: Create sewadars
            self.create_sewadars(NUM_SEWADARS)
            
            # Step 6: Promote first sewadar to incharge
            if len(self.sewadars) > 0:
                first_sewadar_zonal_id = self.sewadars[0].get('zonalId')
                self.promote_sewadar_to_incharge(first_sewadar_zonal_id)
            
            # Step 7: Create program
            self.create_program()
            
            # Step 8: All sewadars apply
            self.apply_all_sewadars()
            
            # Step 9: Approve applications
            self.approve_applications()
            
            # Step 10: Mark attendance
            self.mark_attendance(count=5)
            
            print("\n" + "="*80)
            print("  ✅ TEST COMPLETED SUCCESSFULLY")
            print("="*80)
            print(f"\n📄 All curl commands and responses saved to: {OUTPUT_FILE}")
            
        except Exception as e:
            print(f"\n❌ TEST FAILED: {e}")
            import traceback
            traceback.print_exc()
        finally:
            self.logger.close()

if __name__ == "__main__":
    test = TestFlow()
    test.run()

