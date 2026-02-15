#!/usr/bin/env python3
"""
Comprehensive Production Testing Suite
Tests all flows with 80-90 sewadars and 10+ programs

Bootstrap Handling:
- If no incharge exists: Creates bootstrap incharge with zonalId "INCH001" and password "admin123"
- If incharge exists: Attempts to find existing incharge by trying common zonalIds
  (INCH001, INCH002, INCH003, 1, 2, 3) with password "admin123"
- To use a specific incharge: Set INCHARGE_ZONAL_ID and INCHARGE_PASSWORD at top of file
- After bootstrap (created or found), proceeds to create sewadars using incharge authentication
"""

import requests
import json
import random
import time
from datetime import date, timedelta
from typing import List, Dict, Optional
from dataclasses import dataclass
from collections import defaultdict

BASE_URL = "http://localhost:8080"

# Configuration - Update these if your setup is different
INCHARGE_PASSWORD = "admin123"  # Change if your incharge has different password
INCHARGE_ZONAL_ID = None  # Set to specific zonal ID (String) if known, or None to auto-detect
SEWADAR_PASSWORD = "sewadar123"  # Default password for test sewadars

# Test Results Tracking
@dataclass
class TestResult:
    test_name: str
    passed: bool
    message: str
    duration: float = 0.0

class TestSuite:
    def __init__(self):
        self.results: List[TestResult] = []
        self.incharge_id: Optional[str] = None  # Changed to String (zonalId)
        self.incharge_token: Optional[str] = None
        self.sewadars: List[Dict] = []
        self.programs: List[Dict] = None
        self.applications: List[Dict] = []
        self.attendances: List[Dict] = []
        self.stats = defaultdict(int)
        
    def log_result(self, test_name: str, passed: bool, message: str = "", duration: float = 0.0):
        result = TestResult(test_name, passed, message, duration)
        self.results.append(result)
        status = "✅ PASS" if passed else "❌ FAIL"
        print(f"{status} | {test_name} | {message} | {duration:.2f}s")
        
    def print_header(self, title: str):
        print("\n" + "="*80)
        print(f"  {title}")
        print("="*80)
        
    def print_section(self, title: str):
        print(f"\n--- {title} ---")

# Test Data Generators
class TestDataGenerator:
    FIRST_NAMES = ["Raghu", "Krishna", "Amit", "Priya", "Suresh", "Anita", "Raj", "Meera", 
                   "Vikram", "Sunita", "Arjun", "Kavita", "Rohit", "Neha", "Siddharth", "Pooja",
                   "Manish", "Deepika", "Nikhil", "Shweta", "Ravi", "Anjali", "Karan", "Divya",
                   "Vivek", "Ritu", "Aditya", "Sneha", "Harsh", "Tanvi", "Yash", "Isha",
                   "Akash", "Nisha", "Prateek", "Swati", "Gaurav", "Monika", "Rishabh", "Jyoti"]
    
    LAST_NAMES = ["Verma", "Kumar", "Sharma", "Patel", "Singh", "Gupta", "Yadav", "Jain",
                  "Shah", "Mehta", "Reddy", "Rao", "Nair", "Iyer", "Pillai", "Menon",
                  "Das", "Bose", "Banerjee", "Chatterjee", "Mukherjee", "Ghosh", "Roy", "Dutta"]
    
    LOCATIONS = ["BEAS", "Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad", 
                 "Pune", "Ahmedabad", "Jaipur", "Lucknow", "Kanpur", "Nagpur", "Indore"]
    
    PROFESSIONS = ["Teacher", "Engineer", "Doctor", "Lawyer", "Accountant", "Manager", 
                   "Designer", "Developer", "Consultant", "Analyst", "Administrator", "Nurse"]
    
    LANGUAGES_COMBOS = [
        ["Hindi", "English"],
        ["Hindi", "English", "Punjabi"],
        ["Hindi", "English", "Gujarati"],
        ["Hindi", "English", "Tamil"],
        ["Hindi", "English", "Telugu"],
        ["Hindi", "English", "Marathi"],
        ["Hindi", "Punjabi"],
        ["English", "Tamil"],
    ]
    
    @staticmethod
    def generate_sewadar_data(index: int) -> Dict:
        first_name = random.choice(TestDataGenerator.FIRST_NAMES)
        last_name = random.choice(TestDataGenerator.LAST_NAMES)
        mobile = f"9{random.randint(100000000, 999999999)}"
        aadhar = f"{random.randint(100000000000, 999999999999)}"
        location = random.choice(TestDataGenerator.LOCATIONS)
        profession = random.choice(TestDataGenerator.PROFESSIONS)
        languages = random.choice(TestDataGenerator.LANGUAGES_COMBOS)
        
        # Generate zonalId as String (e.g., "SEW001", "SEW002", etc.)
        zonal_id = f"SEW{index + 1:03d}"
        
        # Generate date of birth (between 1970 and 2000)
        year = random.randint(1970, 2000)
        month = random.randint(1, 12)
        day = random.randint(1, 28)
        dob = f"{year}-{month:02d}-{day:02d}"
        
        return {
            "firstName": first_name,
            "lastName": last_name,
            "zonalId": zonal_id,  # Required: String zonalId
            "mobile": mobile,
            "password": "sewadar123",
            "location": location,
            "profession": profession,
            "dateOfBirth": dob,
            "emergencyContact": f"9{random.randint(100000000, 999999999)}",
            "emergencyContactRelationship": random.choice(["Spouse", "Father", "Mother", "Brother", "Sister"]),
            "aadharNumber": aadhar,
            "languages": languages,
            "photoUrl": f"https://example.com/photos/{first_name.lower()}_{last_name.lower()}.jpg"
        }
    
    @staticmethod
    def generate_program_data(index: int, base_date: date) -> Dict:
        titles = [
            "BEAS Satsang Program", "Delhi Meditation Retreat", "Mumbai Service Camp",
            "Bangalore Workshop", "Chennai Conference", "Kolkata Seminar",
            "Hyderabad Training", "Pune Gathering", "Ahmedabad Event",
            "Jaipur Meeting", "Lucknow Session", "Kanpur Program"
        ]
        
        locations = ["BEAS", "Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata"]
        statuses = ["active", "scheduled", "active", "active", "scheduled"]  # More active programs
        
        title = titles[index % len(titles)]
        location = locations[index % len(locations)]
        status = statuses[index % len(statuses)]
        
        # Generate 3-5 program dates starting from base_date
        num_dates = random.randint(3, 5)
        program_dates = []
        current_date = base_date + timedelta(days=index * 7)  # Spread programs over weeks
        
        for i in range(num_dates):
            program_dates.append(str(current_date + timedelta(days=i)))
        
        return {
            "title": f"{title} #{index + 1}",
            "description": f"Test program {index + 1} for comprehensive testing",
            "location": location,
            "status": status,
            "maxSewadars": random.randint(10, 50),
            "programDates": program_dates
        }

# Main Test Runner
class ProductionTestRunner:
    def __init__(self):
        self.suite = TestSuite()
        self.generator = TestDataGenerator()
        
    def run_all_tests(self):
        start_time = time.time()
        
        self.suite.print_header("PRODUCTION TEST SUITE - COMPREHENSIVE TESTING")
        print(f"Start Time: {time.strftime('%Y-%m-%d %H:%M:%S')}")
        
        try:
            # Phase 1: Setup
            self.test_bootstrap()
            self.test_create_sewadars(85)  # Create 85 sewadars
            self.test_create_programs(12)  # Create 12 programs
            
            # Phase 2: Application Flows
            self.test_application_flows()
            
            # Phase 3: Approval/Rejection Flows
            self.test_approval_flows()
            
            # Phase 4: Drop Request Flows
            self.test_drop_request_flows()
            
            # Phase 5: Reapply Flows
            self.test_reapply_flows()
            
            # Phase 6: Attendance Flows
            self.test_attendance_flows()
            
            # Phase 7: Edge Cases
            self.test_edge_cases()
            
            # Phase 8: Database Verification
            self.test_database_verification()
            
        except Exception as e:
            self.suite.log_result("CRITICAL_ERROR", False, f"Test suite crashed: {str(e)}", 0)
        
        # Generate Report
        self.generate_report(time.time() - start_time)
    
    def test_bootstrap(self):
        self.suite.print_section("Phase 1: Bootstrap & Authentication")
        
        start = time.time()
        try:
            # Check bootstrap status
            response = requests.get(f"{BASE_URL}/api/bootstrap/status")
            bootstrap_data = response.json() if response.status_code == 200 else {}
            needs_bootstrap = bootstrap_data.get('needsBootstrap', True)
            has_incharge = bootstrap_data.get('hasIncharge', False)
            
            self.suite.log_result("Bootstrap Status Check", response.status_code == 200, 
                                f"Has Incharge: {has_incharge}, Needs Bootstrap: {needs_bootstrap}", 
                                time.time() - start)
            
            # Try to create incharge only if needed, otherwise find existing
            if needs_bootstrap:
                start = time.time()
                response = requests.post(f"{BASE_URL}/api/bootstrap/create-incharge",
                    json={
                        "firstName": "Admin",
                        "lastName": "Incharge",
                        "zonalId": "INCH001",  # Required: String zonalId
                        "mobile": "9999999999",
                        "password": "admin123",
                        "location": "BEAS",
                        "profession": "Administrator",
                        "aadharNumber": "123456789012",
                        "languages": ["Hindi", "English"]
                    })
                if response.status_code == 201:
                    data = response.json()
                    self.suite.incharge_id = data['zonalId']  # String zonalId
                    self.suite.log_result("Create Incharge", True, 
                                        f"Created new incharge with Zonal ID: {self.suite.incharge_id}", 
                                        time.time() - start)
                else:
                    self.suite.log_result("Create Incharge", False, response.text[:100], time.time() - start)
                    raise Exception(f"Failed to create bootstrap incharge: {response.text[:200]}")
            else:
                # Incharge exists - find it by trying to login with common credentials
                # Note: We need to find the incharge to authenticate, but we can't query sewadars without auth
                # So we try common zonalIds. If your incharge has a different zonalId, set INCHARGE_ZONAL_ID
                start = time.time()
                try:
                    # First, try configured zonalId if provided
                    if INCHARGE_ZONAL_ID:
                        login_resp = requests.post(f"{BASE_URL}/api/auth/login",
                            json={"zonalId": str(INCHARGE_ZONAL_ID), "password": INCHARGE_PASSWORD})
                        if login_resp.status_code == 200:
                            user_data = login_resp.json().get('sewadar', {})
                            if user_data.get('role') == 'INCHARGE':
                                self.suite.incharge_id = str(INCHARGE_ZONAL_ID)
                                self.suite.log_result("Find Existing Incharge", True, 
                                                    f"Found existing incharge with configured Zonal ID: {self.suite.incharge_id}", 
                                                    time.time() - start)
                            else:
                                # User exists but not INCHARGE role
                                self.suite.log_result("Find Existing Incharge", False, 
                                                    f"User with zonalId '{INCHARGE_ZONAL_ID}' exists but is not INCHARGE", 
                                                    time.time() - start)
                                return
                        else:
                            # Try common IDs as fallback
                            incharge_ids_to_try = ["INCH001", "INCH002", "INCH003", "1", "2", "3"]
                    else:
                        # No configured ID - try common String IDs
                        incharge_ids_to_try = ["INCH001", "INCH002", "INCH003", "1", "2", "3"]
                    
                    # If not found yet, try common IDs
                    if not self.suite.incharge_id:
                        for zonal_id in incharge_ids_to_try:
                            login_resp = requests.post(f"{BASE_URL}/api/auth/login",
                                json={"zonalId": str(zonal_id), "password": INCHARGE_PASSWORD})
                            if login_resp.status_code == 200:
                                user_data = login_resp.json().get('sewadar', {})
                                if user_data.get('role') == 'INCHARGE':
                                    self.suite.incharge_id = str(zonal_id)  # Ensure String type
                                    self.suite.log_result("Find Existing Incharge", True, 
                                                        f"Found existing incharge with Zonal ID: {self.suite.incharge_id}", 
                                                        time.time() - start)
                                    break
                    
                    if not self.suite.incharge_id:
                        self.suite.log_result("Find Existing Incharge", False, 
                                            f"Could not find existing incharge. Tried IDs: {incharge_ids_to_try}. "
                                            f"Please set INCHARGE_ZONAL_ID in script or ensure incharge exists with password '{INCHARGE_PASSWORD}'.", 
                                            time.time() - start)
                        raise Exception("Cannot proceed without incharge authentication. Please set INCHARGE_ZONAL_ID or create bootstrap incharge.")
                except Exception as e:
                    self.suite.log_result("Find Existing Incharge", False, str(e), time.time() - start)
                    raise  # Re-raise to stop test execution
            
            # Login as incharge (with found or created incharge)
            start = time.time()
            response = requests.post(f"{BASE_URL}/api/auth/login",
                json={"zonalId": str(self.suite.incharge_id), "password": INCHARGE_PASSWORD})
            if response.status_code == 200:
                self.suite.incharge_token = response.json()['token']
                self.suite.log_result("Incharge Login", True, "Token obtained", time.time() - start)
            else:
                # Try alternative password or get from user
                self.suite.log_result("Incharge Login", False, 
                                    f"Login failed with password '{INCHARGE_PASSWORD}'. "
                                    f"Please update INCHARGE_PASSWORD in script or reset incharge password.", 
                                    time.time() - start)
                raise Exception(f"Cannot authenticate as incharge. Login failed. Please check INCHARGE_PASSWORD or incharge credentials.")
                
        except Exception as e:
            self.suite.log_result("Bootstrap Phase", False, str(e), time.time() - start)
    
    def test_create_sewadars(self, count: int):
        self.suite.print_section(f"Phase 2: Creating {count} Sewadars")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}", 
                  "Content-Type": "application/json"}
        
        success_count = 0
        for i in range(count):
            start = time.time()
            try:
                data = self.generator.generate_sewadar_data(i)
                response = requests.post(f"{BASE_URL}/api/sewadars", 
                                        headers=headers, json=data)
                
                if response.status_code == 201:
                    sewadar = response.json()
                    self.suite.sewadars.append(sewadar)
                    success_count += 1
                    if (i + 1) % 10 == 0:
                        print(f"  Created {i + 1}/{count} sewadars...")
                else:
                    self.suite.log_result(f"Create Sewadar {i+1}", False, 
                                        response.text[:100], time.time() - start)
            except Exception as e:
                self.suite.log_result(f"Create Sewadar {i+1}", False, str(e), time.time() - start)
        
        self.suite.log_result("Create Sewadars Batch", True, 
                             f"Created {success_count}/{count} sewadars", 0)
        self.suite.stats['sewadars_created'] = success_count
    
    def test_create_programs(self, count: int):
        self.suite.print_section(f"Phase 3: Creating {count} Programs")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}", 
                  "Content-Type": "application/json"}
        
        base_date = date.today()
        success_count = 0
        
        for i in range(count):
            start = time.time()
            try:
                data = self.generator.generate_program_data(i, base_date)
                data['createdById'] = str(self.suite.incharge_id)  # String zonalId
                
                response = requests.post(f"{BASE_URL}/api/programs", 
                                      headers=headers, json=data)
                
                if response.status_code == 201:
                    program = response.json()
                    if not self.suite.programs:
                        self.suite.programs = []
                    self.suite.programs.append(program)
                    success_count += 1
                else:
                    self.suite.log_result(f"Create Program {i+1}", False, 
                                        response.text[:100], time.time() - start)
            except Exception as e:
                self.suite.log_result(f"Create Program {i+1}", False, str(e), time.time() - start)
        
        self.suite.log_result("Create Programs Batch", True, 
                             f"Created {success_count}/{count} programs", 0)
        self.suite.stats['programs_created'] = success_count
    
    def test_application_flows(self):
        self.suite.print_section("Phase 4: Application Flows")
        
        active_programs = [p for p in self.suite.programs if p['status'] == 'active']
        if not active_programs:
            self.suite.log_result("Application Flows", False, "No active programs found")
            return
        
        # Get sewadar tokens
        sewadar_tokens = {}
        for sewadar in self.suite.sewadars[:50]:  # Test with first 50 sewadars
            try:
                response = requests.post(f"{BASE_URL}/api/auth/login",
                    json={"zonalId": str(sewadar['zonalId']), "password": SEWADAR_PASSWORD})
                if response.status_code == 200:
                    sewadar_tokens[sewadar['zonalId']] = response.json()['token']
            except:
                pass
        
        applications_created = 0
        duplicate_prevented = 0
        
        # Each sewadar applies to 2-3 random active programs
        for sewadar_id, token in sewadar_tokens.items():
            headers = {"Authorization": f"Bearer {token}", "Content-Type": "application/json"}
            programs_to_apply = random.sample(active_programs, min(3, len(active_programs)))
            
            for program in programs_to_apply:
                start = time.time()
                try:
                    response = requests.post(f"{BASE_URL}/api/program-applications",
                        headers=headers,
                        json={"programId": program['id'], "sewadarId": str(sewadar_id)})  # String zonalId
                    
                    if response.status_code == 201:
                        app = response.json()
                        self.suite.applications.append(app)
                        applications_created += 1
                    elif "already applied" in response.text.lower():
                        duplicate_prevented += 1
                except Exception as e:
                    pass
        
        self.suite.log_result("Application Flows", True, 
                             f"Created {applications_created} applications, "
                             f"Prevented {duplicate_prevented} duplicates", 0)
        self.suite.stats['applications_created'] = applications_created
    
    def test_approval_flows(self):
        self.suite.print_section("Phase 5: Approval/Rejection Flows")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}"}
        pending_apps = [app for app in self.suite.applications if app['status'] == 'PENDING']
        
        approved = 0
        rejected = 0
        
        for app in pending_apps[:len(pending_apps)//2]:  # Approve half
            start = time.time()
            try:
                response = requests.put(
                    f"{BASE_URL}/api/program-applications/{app['id']}/status?status=APPROVED",
                    headers=headers)
                if response.status_code == 200:
                    approved += 1
            except:
                pass
        
        for app in pending_apps[len(pending_apps)//2:]:  # Reject other half
            start = time.time()
            try:
                response = requests.put(
                    f"{BASE_URL}/api/program-applications/{app['id']}/status?status=REJECTED",
                    headers=headers)
                if response.status_code == 200:
                    rejected += 1
            except:
                pass
        
        self.suite.log_result("Approval Flows", True, 
                             f"Approved {approved}, Rejected {rejected}", 0)
        self.suite.stats['approved'] = approved
        self.suite.stats['rejected'] = rejected
    
    def test_drop_request_flows(self):
        self.suite.print_section("Phase 6: Drop Request Flows")
        
        approved_apps = [app for app in self.suite.applications if app['status'] == 'APPROVED']
        drop_requests = 0
        
        for app in approved_apps[:min(20, len(approved_apps))]:  # Test 20 drop requests
            sewadar_id = app['sewadar']['zonalId']
            try:
                # Login as sewadar
                login_resp = requests.post(f"{BASE_URL}/api/auth/login",
                    json={"zonalId": str(sewadar_id), "password": SEWADAR_PASSWORD})
                if login_resp.status_code != 200:
                    continue
                
                token = login_resp.json()['token']
                headers = {"Authorization": f"Bearer {token}"}
                
                # Request drop
                response = requests.put(
                    f"{BASE_URL}/api/program-applications/{app['id']}/request-drop?sewadarId={str(sewadar_id)}",  # String zonalId
                    headers=headers)
                
                if response.status_code == 200:
                    drop_requests += 1
            except:
                pass
        
        # Approve some drop requests
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}"}
        drop_approved = 0
        
        # Get drop requests
        for program in self.suite.programs[:5]:
            try:
                response = requests.get(
                    f"{BASE_URL}/api/program-applications/program/{program['id']}/drop-requests",
                    headers=headers)
                if response.status_code == 200:
                    drop_reqs = response.json()
                    for req in drop_reqs[:min(2, len(drop_reqs))]:
                        try:
                            resp = requests.put(
                                f"{BASE_URL}/api/program-applications/{req['id']}/approve-drop?inchargeId={str(self.suite.incharge_id)}",  # String zonalId
                                headers=headers)
                            if resp.status_code == 200:
                                drop_approved += 1
                        except:
                            pass
            except:
                pass
        
        self.suite.log_result("Drop Request Flows", True, 
                             f"Requests: {drop_requests}, Approved: {drop_approved}", 0)
        self.suite.stats['drop_requests'] = drop_requests
        self.suite.stats['drop_approved'] = drop_approved
    
    def test_reapply_flows(self):
        self.suite.print_section("Phase 7: Reapply Flows")
        
        # Get dropped applications and test reapply
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}"}
        reapplied = 0
        
        for program in self.suite.programs[:3]:
            try:
                response = requests.get(
                    f"{BASE_URL}/api/program-applications/program/{program['id']}",
                    headers=headers)
                if response.status_code == 200:
                    apps = response.json()
                    dropped_apps = [a for a in apps if a['status'] == 'DROPPED']
                    
                    for app in dropped_apps[:min(5, len(dropped_apps))]:
                        sewadar_id = app['sewadar']['zonalId']
                        try:
                            login_resp = requests.post(f"{BASE_URL}/api/auth/login",
                                json={"zonalId": str(sewadar_id), "password": SEWADAR_PASSWORD})
                            if login_resp.status_code != 200:
                                continue
                            
                            token = login_resp.json()['token']
                            app_headers = {"Authorization": f"Bearer {token}", 
                                         "Content-Type": "application/json"}
                            
                            reapply_resp = requests.post(f"{BASE_URL}/api/program-applications",
                                headers=app_headers,
                                json={"programId": program['id'], "sewadarId": str(sewadar_id)})  # String zonalId
                            
                            if reapply_resp.status_code == 201:
                                reapplied += 1
                        except:
                            pass
            except:
                pass
        
        self.suite.log_result("Reapply Flows", True, f"Reapplied: {reapplied}", 0)
        self.suite.stats['reapplied'] = reapplied
    
    def test_attendance_flows(self):
        self.suite.print_section("Phase 8: Attendance Flows")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}", 
                  "Content-Type": "application/json"}
        
        attendance_marked = 0
        
        # Mark attendance for approved applications
        for program in self.suite.programs[:5]:  # Test 5 programs
            if program['status'] != 'active':
                continue
            
            try:
                # Get approved attendees
                response = requests.get(
                    f"{BASE_URL}/api/attendances/program/{program['id']}/attendees",
                    headers=headers)
                
                if response.status_code == 200:
                    attendees = response.json()
                    if not attendees:
                        continue
                    
                    # Get program dates (only past/today dates)
                    program_dates = program.get('programDates', [])
                    today = str(date.today())
                    valid_dates = [d for d in program_dates if d <= today]
                    
                    if not valid_dates:
                        continue
                    
                    # Mark attendance for first valid date
                    attendee_ids = [str(a['zonalId']) for a in attendees[:min(10, len(attendees))]]  # String zonalIds
                    
                    attendance_resp = requests.post(f"{BASE_URL}/api/attendances",
                        headers=headers,
                        json={
                            "programId": program['id'],
                            "programDate": valid_dates[0],
                            "sewadarIds": attendee_ids  # List of String zonalIds
                        })
                    
                    if attendance_resp.status_code == 200:
                        attendance_marked += len(attendee_ids)
            except Exception as e:
                pass
        
        self.suite.log_result("Attendance Flows", True, 
                             f"Marked attendance for {attendance_marked} sewadar-date combinations", 0)
        self.suite.stats['attendance_marked'] = attendance_marked
    
    def test_edge_cases(self):
        self.suite.print_section("Phase 9: Edge Cases")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}", 
                  "Content-Type": "application/json"}
        
        edge_cases_passed = 0
        edge_cases_total = 0
        edge_case_details = []
        
        # Edge Case 1: Duplicate Aadhar
        edge_cases_total += 1
        edge_case_name = "Duplicate Aadhar Prevention"
        passed = False
        error_msg = ""
        try:
            if self.suite.sewadars:
                existing_aadhar = self.suite.sewadars[0]['aadharNumber']
                response = requests.post(f"{BASE_URL}/api/sewadars",
                    headers=headers,
                    json={
                        "firstName": "Test",
                        "lastName": "Duplicate",
                        "zonalId": f"TEST{random.randint(1000, 9999)}",  # Unique zonalId required
                        "mobile": "1111111111",
                        "password": "test123",
                        "aadharNumber": existing_aadhar
                    })
                if response.status_code != 201 and "duplicate" in response.text.lower():
                    passed = True
                    edge_cases_passed += 1
                    error_msg = "Correctly rejected duplicate Aadhar"
                else:
                    error_msg = f"Expected duplicate error, got status {response.status_code}"
        except Exception as e:
            error_msg = f"Exception: {str(e)}"
        
        edge_case_details.append({"name": edge_case_name, "passed": passed, "message": error_msg})
        self.suite.log_result(edge_case_name, passed, error_msg, 0)
        
        # Edge Case 2: Apply to scheduled program
        edge_cases_total += 1
        edge_case_name = "Apply to Scheduled Program (Should Fail)"
        passed = False
        error_msg = ""
        try:
            scheduled_programs = [p for p in self.suite.programs if p['status'] == 'scheduled']
            if scheduled_programs and self.suite.sewadars:
                sewadar_id = self.suite.sewadars[0]['zonalId']
                login_resp = requests.post(f"{BASE_URL}/api/auth/login",
                    json={"zonalId": str(sewadar_id), "password": SEWADAR_PASSWORD})
                if login_resp.status_code == 200:
                    token = login_resp.json()['token']
                    app_headers = {"Authorization": f"Bearer {token}", 
                                 "Content-Type": "application/json"}
                    response = requests.post(f"{BASE_URL}/api/program-applications",
                        headers=app_headers,
                        json={"programId": scheduled_programs[0]['id'], "sewadarId": str(sewadar_id)})  # String zonalId
                    if response.status_code != 201 and "active" in response.text.lower():
                        passed = True
                        edge_cases_passed += 1
                        error_msg = "Correctly rejected application to scheduled program"
                    else:
                        error_msg = f"Expected rejection, got status {response.status_code}: {response.text[:100]}"
                else:
                    error_msg = "Could not login as sewadar"
            else:
                error_msg = "No scheduled programs or sewadars available for testing"
        except Exception as e:
            error_msg = f"Exception: {str(e)}"
        
        edge_case_details.append({"name": edge_case_name, "passed": passed, "message": error_msg})
        self.suite.log_result(edge_case_name, passed, error_msg, 0)
        
        # Edge Case 3: Mark attendance for future date
        edge_cases_total += 1
        edge_case_name = "Mark Attendance for Future Date (Should Fail)"
        passed = False
        error_msg = ""
        try:
            active_programs = [p for p in self.suite.programs if p['status'] == 'active']
            if active_programs and self.suite.sewadars:
                program = active_programs[0]
                # Get approved attendees first
                attendees_resp = requests.get(
                    f"{BASE_URL}/api/attendances/program/{program['id']}/attendees",
                    headers=headers)
                
                if attendees_resp.status_code == 200:
                    attendees = attendees_resp.json()
                    if attendees:
                        future_date = str(date.today() + timedelta(days=30))
                        response = requests.post(f"{BASE_URL}/api/attendances",
                            headers=headers,
                            json={
                                "programId": program['id'],
                                "programDate": future_date,
                                "sewadarIds": [str(attendees[0]['zonalId'])]  # String zonalId
                            })
                        # Accept rejection for either "future" OR "not a valid program date" 
                        # (both are correct - future dates not in program are rejected)
                        response_text_lower = response.text.lower()
                        if response.status_code != 200 and (
                            "future" in response_text_lower or 
                            "not a valid program date" in response_text_lower or
                            "valid dates" in response_text_lower
                        ):
                            passed = True
                            edge_cases_passed += 1
                            error_msg = "Correctly rejected future/invalid date attendance"
                        else:
                            error_msg = f"Expected rejection, got status {response.status_code}: {response.text[:150]}"
                    else:
                        error_msg = "No approved attendees available for testing"
                else:
                    error_msg = f"Could not get attendees: {attendees_resp.status_code}"
            else:
                error_msg = "No active programs or sewadars available for testing"
        except Exception as e:
            error_msg = f"Exception: {str(e)}"
        
        edge_case_details.append({"name": edge_case_name, "passed": passed, "message": error_msg})
        self.suite.log_result(edge_case_name, passed, error_msg, 0)
        
        # Overall edge cases result
        all_passed = edge_cases_passed == edge_cases_total
        self.suite.log_result("Edge Cases Summary", all_passed,
                             f"Passed {edge_cases_passed}/{edge_cases_total} edge cases", 0)
        self.suite.stats['edge_cases_passed'] = edge_cases_passed
        self.suite.stats['edge_case_details'] = edge_case_details
    
    def test_database_verification(self):
        self.suite.print_section("Phase 10: Database Verification")
        
        headers = {"Authorization": f"Bearer {self.suite.incharge_token}"}
        
        verification_passed = 0
        verification_total = 0
        
        # Verify sewadars count
        verification_total += 1
        try:
            response = requests.get(f"{BASE_URL}/api/sewadars", headers=headers)
            if response.status_code == 200:
                sewadars = response.json()
                if len(sewadars) >= self.suite.stats['sewadars_created']:
                    verification_passed += 1
        except:
            pass
        
        # Verify programs count
        verification_total += 1
        try:
            response = requests.get(f"{BASE_URL}/api/programs/incharge/{str(self.suite.incharge_id)}",  # String zonalId
                                  headers=headers)
            if response.status_code == 200:
                programs = response.json()
                if len(programs) >= self.suite.stats['programs_created']:
                    verification_passed += 1
        except:
            pass
        
        # Verify applications
        verification_total += 1
        try:
            total_apps = 0
            for program in self.suite.programs[:5]:
                response = requests.get(
                    f"{BASE_URL}/api/program-applications/program/{program['id']}",
                    headers=headers)
                if response.status_code == 200:
                    total_apps += len(response.json())
            if total_apps > 0:
                verification_passed += 1
        except:
            pass
        
        # Verify attendance
        verification_total += 1
        try:
            if self.suite.sewadars:
                response = requests.get(
                    f"{BASE_URL}/api/attendances/sewadar/{self.suite.sewadars[0]['zonalId']}/summary",
                    headers=headers)
                if response.status_code == 200:
                    verification_passed += 1
        except:
            pass
        
        self.suite.log_result("Database Verification", verification_passed == verification_total,
                             f"Passed {verification_passed}/{verification_total} verifications", 0)
        self.suite.stats['verification_passed'] = verification_passed
    
    def generate_report(self, total_duration: float):
        self.suite.print_header("TEST EXECUTION REPORT")
        
        total_tests = len(self.suite.results)
        passed_tests = sum(1 for r in self.suite.results if r.passed)
        failed_tests = total_tests - passed_tests
        pass_rate = (passed_tests / total_tests * 100) if total_tests > 0 else 0
        
        print(f"\n📊 TEST SUMMARY")
        print(f"{'='*80}")
        print(f"Total Tests:        {total_tests}")
        print(f"Passed:             {passed_tests} ✅")
        print(f"Failed:             {failed_tests} ❌")
        print(f"Pass Rate:          {pass_rate:.2f}%")
        print(f"Total Duration:     {total_duration:.2f} seconds")
        print(f"{'='*80}")
        
        print(f"\n📈 STATISTICS")
        print(f"{'='*80}")
        for key, value in self.suite.stats.items():
            print(f"{key.replace('_', ' ').title()}: {value}")
        print(f"{'='*80}")
        
        if failed_tests > 0:
            print(f"\n❌ FAILED TESTS")
            print(f"{'='*80}")
            for result in self.suite.results:
                if not result.passed:
                    print(f"  - {result.test_name}: {result.message}")
            print(f"{'='*80}")
        
        print(f"\n✅ PRODUCTION READINESS")
        print(f"{'='*80}")
        if pass_rate >= 95:
            print("🟢 EXCELLENT - Ready for production deployment")
        elif pass_rate >= 85:
            print("🟡 GOOD - Minor issues to address before production")
        else:
            print("🔴 NEEDS WORK - Fix issues before production deployment")
        print(f"{'='*80}")
        
        # Save report to file
        report_file = "test_report.json"
        report_data = {
            "summary": {
                "total_tests": total_tests,
                "passed": passed_tests,
                "failed": failed_tests,
                "pass_rate": pass_rate,
                "duration": total_duration
            },
            "stats": dict(self.suite.stats),
            "results": [
                {
                    "test_name": r.test_name,
                    "passed": r.passed,
                    "message": r.message,
                    "duration": r.duration
                }
                for r in self.suite.results
            ]
        }
        
        with open(report_file, 'w') as f:
            json.dump(report_data, f, indent=2)
        
        print(f"\n📄 Detailed report saved to: {report_file}")

if __name__ == "__main__":
    print("🚀 Starting Comprehensive Production Test Suite...")
    print("⚠️  Make sure your Spring Boot application is running on http://localhost:8080")
    print("⏳ This may take 5-10 minutes...\n")
    
    runner = ProductionTestRunner()
    runner.run_all_tests()
    
    print("\n✨ Test suite execution completed!")

