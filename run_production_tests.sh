#!/bin/bash

# Comprehensive Production Test Suite Runner
# This script runs the production test suite with proper setup

echo "=========================================="
echo "  PRODUCTION TEST SUITE RUNNER"
echo "=========================================="
echo ""

# Check if Python 3 is installed
if ! command -v python3 &> /dev/null; then
    echo "❌ Python 3 is not installed. Please install Python 3.7+"
    exit 1
fi

echo "✅ Python 3 found: $(python3 --version)"
echo ""

# Check if requests library is installed
if ! python3 -c "import requests" 2>/dev/null; then
    echo "⚠️  'requests' library not found. Installing..."
    pip3 install requests
    if [ $? -ne 0 ]; then
        echo "❌ Failed to install requests library"
        exit 1
    fi
    echo "✅ requests library installed"
else
    echo "✅ requests library found"
fi

echo ""

# Check if Spring Boot app is running
echo "🔍 Checking if Spring Boot application is running..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/bootstrap/status 2>/dev/null)

if [ "$HTTP_CODE" != "200" ]; then
    echo "⚠️  WARNING: Spring Boot application may not be running"
    echo "   Expected: HTTP 200, Got: HTTP $HTTP_CODE"
    echo "   Please ensure your application is running on http://localhost:8080"
    echo ""
    read -p "Continue anyway? (y/n) " -n 1 -r
    echo ""
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        exit 1
    fi
else
    echo "✅ Spring Boot application is running"
fi

echo ""
echo "=========================================="
echo "  Starting Test Suite..."
echo "=========================================="
echo ""

# Run the test suite
python3 comprehensive_production_test.py

EXIT_CODE=$?

echo ""
echo "=========================================="
if [ $EXIT_CODE -eq 0 ]; then
    echo "  ✅ Test Suite Completed"
else
    echo "  ⚠️  Test Suite Completed with Errors"
fi
echo "=========================================="
echo ""
echo "📄 Check test_report.json for detailed results"
echo ""

exit $EXIT_CODE

