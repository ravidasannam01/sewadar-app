# WhatsApp Email Bridge Format

## Overview
When `whatsapp.mode=email-bridge`, the system sends **ONE email per node trigger** with all recipients listed, instead of sending individual emails for each recipient.

## Email Format

### Subject
```
WHATSAPP_SEND
```

### Body Format
```
PROGRAM: Program Title
NODE: 2
MESSAGE: Your notification message here

RECIPIENTS:
916303623749
919999000001
919999000002
919999000003

---
This is an automated WhatsApp trigger email.
Another machine should read this and send WhatsApp messages to all recipients listed above.
All recipients receive the same message.
Timestamp: 2026-02-18T14:30:00
Total Recipients: 4
```

## Example Email

**Subject:** `WHATSAPP_SEND`

**Body:**
```
PROGRAM: Today's Program - 2026-02-18
NODE: 2
MESSAGE: Please post an application message in the community group for program 'Today's Program - 2026-02-18'. New program available: 'Today's Program - 2026-02-18'. Please apply for this program through the application system. Please post an application message in the community group for program 'Today's Program - 2026-02-18'.

RECIPIENTS:
919999000001
919999000002
919999000003

---
This is an automated WhatsApp trigger email.
Another machine should read this and send WhatsApp messages to all recipients listed above.
All recipients receive the same message.
Timestamp: 2026-02-18T14:30:45.123
Total Recipients: 3
```

## How Your Python Script Should Parse

### Step 1: Filter Emails
- Look for emails with subject: `WHATSAPP_SEND`
- Mark as read after processing

### Step 2: Parse Email Body
```python
# Extract fields
program = extract_line_starting_with("PROGRAM: ")
node = extract_line_starting_with("NODE: ")
message = extract_line_starting_with("MESSAGE: ")

# Extract recipients (lines after "RECIPIENTS:")
recipients = []
in_recipients_section = False
for line in email_body.split('\n'):
    if line.strip() == "RECIPIENTS:":
        in_recipients_section = True
        continue
    if in_recipients_section:
        if line.strip() and not line.startswith("---"):
            recipients.append(line.strip())
        elif line.startswith("---"):
            break
```

### Step 3: Send WhatsApp Messages
```python
import pywhatkit as pwt
import time
from datetime import datetime

# Get current time
now = datetime.now()
hour = now.hour
minute = now.minute + 1  # Send 1 minute from now

# Send to all recipients with 2-second delay
for i, phone in enumerate(recipients):
    pwt.sendwhatmsg(f"+{phone}", message, hour, minute + (i * 2))
    time.sleep(2)  # Additional delay between sends
```

## Benefits

1. **One Email Per Trigger**: Instead of 10 emails for 10 recipients, you get 1 email
2. **Efficient**: Your Python script processes one email per node trigger
3. **Structured Format**: Easy to parse programmatically
4. **Context Included**: Program title and node number for logging/debugging
5. **Same Message**: All recipients get the same message (as per your workflow)

## Configuration

In `application.properties`:
```properties
whatsapp.mode=email-bridge
whatsapp.enabled=true
whatsapp.bridge.email=whatsapp-handler@yourdomain.com
```

## Notes

- Phone numbers are formatted as digits only (country code + number)
- Example: `+91 6303623749` becomes `916303623749`
- All recipients receive the **same message**
- Email is sent once per node trigger, regardless of number of recipients
- Your Python script should add delays (2 seconds) between WhatsApp sends

