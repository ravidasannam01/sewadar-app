#!/usr/bin/env python3
"""
Test script for Gmail SMTP using App Password.
This script tests if your Gmail App Password is working correctly.

Usage:
1. Update GMAIL_ADDRESS and APP_PASSWORD below
2. Run: python3 test_gmail_smtp.py
"""

import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
from datetime import datetime

# ============================================
# CONFIGURATION - UPDATE THESE VALUES
# ============================================
GMAIL_ADDRESS = "annamravidas54@gmail.com"  # Replace with your Gmail address
APP_PASSWORD = "soxj bkpi nrby rino"     # Replace with your App Password (spaces don't matter)
TO_EMAIL = "ravidasannam0123@gmail.com"  # Recipient email

# ============================================
# SMTP Configuration
# ============================================
SMTP_SERVER = "smtp.gmail.com"
SMTP_PORT = 587  # TLS port (alternative: 465 for SSL)

def send_test_email():
    """Send a test email using Gmail SMTP with App Password."""
    
    print("=" * 60)
    print("Gmail SMTP Test Script")
    print("=" * 60)
    print(f"From: {GMAIL_ADDRESS}")
    print(f"To: {TO_EMAIL}")
    print(f"SMTP Server: {SMTP_SERVER}:{SMTP_PORT}")
    print("=" * 60)
    print()
    
    # Validate configuration
    if GMAIL_ADDRESS == "your-email@gmail.com":
        print("❌ ERROR: Please update GMAIL_ADDRESS with your actual Gmail address")
        return False
    
    if APP_PASSWORD == "soxj bkpi nrby rino":
        print("⚠️  WARNING: Using example App Password. Update APP_PASSWORD if needed.")
        print()
    
    # Remove spaces from App Password (spaces are optional)
    app_password_clean = APP_PASSWORD.replace(" ", "")
    
    try:
        # Create message
        msg = MIMEMultipart()
        msg['From'] = GMAIL_ADDRESS
        msg['To'] = TO_EMAIL
        msg['Subject'] = f"Test Email from Sewadar Management App - {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}"
        
        # Email body
        body = f"""
Hello!

This is a test email sent from the Sewadar Management System.

Test Details:
- Sent at: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}
- From: {GMAIL_ADDRESS}
- SMTP Server: {SMTP_SERVER}:{SMTP_PORT}
- Method: Gmail App Password

If you received this email, your Gmail App Password is working correctly! ✅

You can now use this configuration in your Spring Boot application.

Best regards,
Sewadar Management System Test Script
        """
        
        msg.attach(MIMEText(body, 'plain'))
        
        print("📧 Connecting to Gmail SMTP server...")
        
        # Connect to SMTP server
        server = smtplib.SMTP(SMTP_SERVER, SMTP_PORT)
        server.starttls()  # Enable TLS encryption
        
        print("🔐 Authenticating with App Password...")
        
        # Login with Gmail address and App Password
        server.login(GMAIL_ADDRESS, app_password_clean)
        
        print("✅ Authentication successful!")
        print("📤 Sending email...")
        
        # Send email
        text = msg.as_string()
        server.sendmail(GMAIL_ADDRESS, TO_EMAIL, text)
        
        # Close connection
        server.quit()
        
        print("✅ Email sent successfully!")
        print()
        print("=" * 60)
        print("SUCCESS! Check your inbox at:", TO_EMAIL)
        print("=" * 60)
        return True
        
    except smtplib.SMTPAuthenticationError as e:
        print("❌ AUTHENTICATION FAILED!")
        print("Error:", str(e))
        print()
        print("Possible issues:")
        print("1. App Password is incorrect - check if you copied it correctly")
        print("2. 2-Step Verification is not enabled")
        print("3. App Password was revoked - generate a new one")
        print("4. Gmail address is incorrect")
        return False
        
    except smtplib.SMTPException as e:
        print("❌ SMTP ERROR!")
        print("Error:", str(e))
        print()
        print("Possible issues:")
        print("1. SMTP server is unreachable")
        print("2. Port 587 is blocked by firewall")
        print("3. Gmail is blocking the connection")
        return False
        
    except Exception as e:
        print("❌ UNEXPECTED ERROR!")
        print("Error:", str(e))
        print("Error Type:", type(e).__name__)
        return False

if __name__ == "__main__":
    print()
    success = send_test_email()
    print()
    
    if success:
        print("🎉 Test completed successfully!")
        print("Your Gmail App Password is working. You can now use it in your Spring Boot app.")
    else:
        print("💔 Test failed. Please check the error messages above and fix the issues.")
    
    print()

