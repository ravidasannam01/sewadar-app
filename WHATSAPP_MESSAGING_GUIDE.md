# WhatsApp Messaging System - How It Works & Cost Analysis

## How It Currently Works

### Architecture

1. **Scheduler** (`WorkflowScheduler.java`)
   - Runs daily at **9:00 AM** (configurable via cron)
   - Calls `ProgramWorkflowService.sendDailyNotifications()`

2. **Workflow Service** (`ProgramWorkflowService.java`)
   - Checks all programs at their current workflow step
   - For each program at a workflow node:
     - Checks if notifications are enabled (global or program-level)
     - Gets all INCHARGE users
     - Sends WhatsApp message to each incharge's mobile number

3. **WhatsApp Service** (`WhatsAppService.java`)
   - Uses **WhatsApp Cloud API** (Meta/Facebook)
   - Sends messages via REST API to Meta's servers
   - Currently configured but **disabled** (`whatsapp.enabled=false`)

### Message Flow

```
Daily Scheduler (9 AM)
    ↓
Check all programs
    ↓
For each program at workflow node:
    ↓
Check notification enabled?
    ↓
Get all INCHARGE users
    ↓
For each incharge:
    ↓
Send WhatsApp message via Cloud API
```

### Current Configuration

**Status**: ❌ **DISABLED** (whatsapp.enabled=false)

**API Used**: WhatsApp Cloud API (Meta/Facebook)
- Endpoint: `https://graph.facebook.com/v18.0/{phone-number-id}/messages`
- Authentication: Bearer token (Access Token)

**Setup Required**:
1. Create Meta App at https://developers.facebook.com/
2. Add WhatsApp product
3. Get Phone Number ID and Access Token
4. Configure in `application.properties`

---

## Cost Analysis

### WhatsApp Cloud API Pricing (Meta)

#### **Conversation-Based Pricing Model**

Meta charges based on **conversations**, not individual messages:

1. **24-Hour Conversation Window**
   - Once you send a message to a user, you can send unlimited messages to that user for **24 hours** without additional charges
   - After 24 hours, a new conversation starts (new charge)

2. **Conversation Categories**

   **a) User-Initiated Conversations**
   - User messages you first
   - **FREE** - No charge for your replies within 24 hours
   - Best for customer support

   **b) Business-Initiated Conversations**
   - You message the user first (your use case)
   - **PAID** - Charged per conversation
   - Pricing varies by country

#### **Pricing by Country (Business-Initiated)**

**Tier 1 Countries** (Most expensive):
- United States, Canada, UK, Australia, etc.
- **~$0.005 - $0.009 per conversation** (0.5¢ - 0.9¢)

**Tier 2 Countries**:
- Most European countries
- **~$0.002 - $0.005 per conversation** (0.2¢ - 0.5¢)

**Tier 3 Countries** (Cheapest):
- India, Brazil, Mexico, etc.
- **~$0.0005 - $0.002 per conversation** (0.05¢ - 0.2¢)

**Note**: India is typically in Tier 3 (cheapest tier)

#### **Free Tier (Trial Period)**

- **1,000 free conversations per month** for first 90 days
- After that, pay-as-you-go pricing applies

---

## Cost Calculation for Your Use Case

### Current Usage Pattern

Based on your requirements:
- **Maximum 4 messages per day to incharge**
- Daily scheduler runs at 9 AM
- Messages sent to all INCHARGE users

### Example Scenarios

#### Scenario 1: Small Scale
- **1 program** at workflow node
- **2 incharges**
- **1 message per incharge** = 2 conversations/day
- **Monthly**: 2 × 30 = **60 conversations/month**

**Cost (India - Tier 3)**:
- 60 conversations × $0.001 = **$0.06/month** (~₹5/month)
- **Annual**: ~₹60/year

#### Scenario 2: Medium Scale
- **5 programs** at workflow nodes
- **5 incharges** (some programs may have same incharge)
- **4 messages per day** (max) = 4 conversations/day
- **Monthly**: 4 × 30 = **120 conversations/month**

**Cost (India - Tier 3)**:
- 120 conversations × $0.001 = **$0.12/month** (~₹10/month)
- **Annual**: ~₹120/year

#### Scenario 3: Large Scale
- **10 programs** at workflow nodes
- **10 incharges**
- **4 messages per day** = 4 conversations/day
- **Monthly**: 4 × 30 = **120 conversations/month**

**Cost (India - Tier 3)**:
- Same as Scenario 2: **~₹10/month**

### Important Notes

1. **24-Hour Window**: If you send multiple messages to the same incharge within 24 hours, it counts as **1 conversation**, not multiple!

2. **Optimization Opportunity**: 
   - Currently, if 5 programs are at the same node, you might send 5 messages to the same incharge
   - This would be **1 conversation** (if within 24 hours) or **5 conversations** (if spread across days)
   - You could batch messages: "You have 5 programs requiring action..."

3. **Free Tier**: First 1,000 conversations/month are **FREE** for 90 days

---

## Cost Comparison with Alternatives

### 1. WhatsApp Cloud API (Current Implementation)
- **Setup**: Medium (requires Meta App setup)
- **Cost**: ~₹5-10/month for your use case
- **Reliability**: High (Meta infrastructure)
- **Scalability**: Excellent
- **Features**: Rich messaging, delivery receipts

### 2. Twilio WhatsApp API
- **Setup**: Easy
- **Cost**: ~$0.005 per message (₹0.40 per message)
- **For 120 messages/month**: ~₹48/month
- **More expensive** than Meta's Cloud API

### 3. Email (SMTP)
- **Setup**: Easy
- **Cost**: **FREE** (Gmail, SendGrid free tier, etc.)
- **Reliability**: Medium (may go to spam)
- **User Engagement**: Lower than WhatsApp

### 4. SMS (via Twilio/other)
- **Setup**: Easy
- **Cost**: ~₹0.50-1 per SMS
- **For 120 messages/month**: ~₹60-120/month
- **More expensive** than WhatsApp

### 5. WhatsApp Business API (via Partners)
- **Setup**: Complex (requires business verification)
- **Cost**: Similar to Cloud API
- **Features**: More business features

---

## Recommendations

### For Your Use Case (4 messages/day max)

**Best Option**: **WhatsApp Cloud API** (current implementation)
- ✅ **Lowest cost** (~₹5-10/month)
- ✅ Already implemented
- ✅ High delivery rate
- ✅ User-friendly (everyone uses WhatsApp)

### Cost Optimization Tips

1. **Batch Messages**: Instead of sending separate messages for each program, send one message listing all programs:
   ```
   "You have 3 programs requiring action:
   - Program A: Make Active
   - Program B: Post Application Message
   - Program C: Release Form"
   ```
   This reduces conversations if sent within 24 hours.

2. **Smart Scheduling**: 
   - Send all messages at once (9 AM) to maximize 24-hour window
   - If incharge acts on one program, you can send follow-up messages for free within 24 hours

3. **Use Free Tier**: 
   - First 90 days: 1,000 conversations/month FREE
   - Your usage (120/month) fits well within free tier

4. **Monitor Usage**: 
   - Track actual conversations sent
   - Meta provides usage dashboard

---

## Setup Instructions

### Step 1: Create Meta App

1. Go to https://developers.facebook.com/
2. Click "My Apps" → "Create App"
3. Choose "Business" type
4. Fill in app details

### Step 2: Add WhatsApp Product

1. In app dashboard, click "Add Product"
2. Find "WhatsApp" → "Set Up"
3. Follow setup wizard

### Step 3: Get Credentials

1. **Phone Number ID**: 
   - WhatsApp → API Setup
   - Copy "Phone number ID"

2. **Access Token**:
   - For testing: Use "Temporary access token" (expires in 24 hours)
   - For production: Create System User → Get permanent token

3. **Verify Phone Number**:
   - Add your WhatsApp Business number
   - Verify via SMS/call

### Step 4: Configure Application

Edit `src/main/resources/application.properties`:

```properties
whatsapp.enabled=true
whatsapp.api.url=https://graph.facebook.com/v18.0
whatsapp.phone.number.id=YOUR_PHONE_NUMBER_ID
whatsapp.access.token=YOUR_ACCESS_TOKEN
```

### Step 5: Test

1. Restart backend
2. Create a test program
3. Check logs for WhatsApp API calls
4. Verify message received

---

## Current Status

- ✅ **Code Implemented**: WhatsApp service ready
- ❌ **Not Configured**: `whatsapp.enabled=false`
- ❌ **No Credentials**: Phone Number ID and Access Token empty
- ✅ **Scheduler Ready**: Daily notifications configured

**To Enable**: Just configure credentials and set `whatsapp.enabled=true`

---

## Summary

**Cost for Your Use Case**: 
- **~₹5-10/month** (India pricing)
- **FREE for first 90 days** (1,000 conversations/month free)
- **Very affordable** for the value provided

**Recommendation**: 
- ✅ Use WhatsApp Cloud API (current implementation)
- ✅ Enable it by configuring Meta App credentials
- ✅ Monitor usage in first 90 days (free tier)
- ✅ Consider message batching for optimization

