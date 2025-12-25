#!/usr/bin/env python3
"""
Edge Case Analysis Script
Shows detailed information about which edge cases passed/failed
"""

import json
import sys

def analyze_edge_cases():
    try:
        with open('test_report.json', 'r') as f:
            report = json.load(f)
    except FileNotFoundError:
        print("❌ test_report.json not found. Please run the test suite first.")
        return
    
    print("="*80)
    print("  EDGE CASE ANALYSIS")
    print("="*80)
    print()
    
    # Get edge case details from stats
    stats = report.get('stats', {})
    edge_case_details = stats.get('edge_case_details', [])
    
    if edge_case_details:
        print("📋 Individual Edge Case Results:")
        print("-"*80)
        for i, case in enumerate(edge_case_details, 1):
            status = "✅ PASS" if case['passed'] else "❌ FAIL"
            print(f"{i}. {status} | {case['name']}")
            print(f"   Message: {case['message']}")
            print()
    else:
        # Fallback: analyze from results
        results = report.get('results', [])
        edge_case_result = next((r for r in results if 'Edge' in r['test_name']), None)
        
        if edge_case_result:
            print(f"Edge Cases Test: {'✅ PASSED' if edge_case_result['passed'] else '❌ FAILED'}")
            print(f"Message: {edge_case_result['message']}")
            print()
            print("Note: Run the test suite again to see individual edge case details.")
    
    # Summary
    total_edge_cases = 3
    passed = stats.get('edge_cases_passed', 0)
    failed = total_edge_cases - passed
    
    print("="*80)
    print("  SUMMARY")
    print("="*80)
    print(f"Total Edge Cases: {total_edge_cases}")
    print(f"Passed: {passed} ✅")
    print(f"Failed: {failed} ❌")
    print()
    
    if failed > 0:
        print("⚠️  Failed Edge Cases:")
        print("-"*80)
        for case in edge_case_details:
            if not case['passed']:
                print(f"❌ {case['name']}")
                print(f"   Reason: {case['message']}")
                print()
        
        print("💡 Recommendations:")
        print("-"*80)
        for case in edge_case_details:
            if not case['passed']:
                if "future" in case['name'].lower():
                    print("• Future Date Attendance: Check if backend validation is working")
                    print("  Expected: Reject with 'future' in error message")
                    print("  Actual: Got different error")
                elif "scheduled" in case['name'].lower():
                    print("• Scheduled Program: Check application validation logic")
                elif "duplicate" in case['name'].lower():
                    print("• Duplicate Aadhar: Check unique constraint enforcement")
    else:
        print("✅ All edge cases passed!")

if __name__ == "__main__":
    analyze_edge_cases()

