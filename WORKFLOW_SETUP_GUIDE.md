# Program Workflow System - Setup Guide

## Overview

The workflow system implements a sequential 6-node process for managing programs from creation to completion, with WhatsApp notifications at each step.

## Workflow Nodes

1. **Node 1: Make Program Active** - Remind incharge to activate scheduled programs
2. **Node 2: Post Application Message** - Remind to post in WhatsApp group for applications
3. **Node 3: Release Form** - Applications full, release form for sewadars to fill
4. **Node 4: Collect Details** - Sewadars submit travel details form
5. **Node 5: Post Mail to Area Secretary** - Remind to send mail to area secretary
6. **Node 6: Post General Instructions** - Final step - post general instructions

## WhatsApp Cloud API Setup

### Step 1: Create Meta App

1. Go to https://developers.facebook.com/
2. Click "My Apps" → "Create App"
3. Choose "Business" type
4. Fill in app details

### Step 2: Add WhatsApp Product

1. In your app dashboard, click "Add Product"
2. Find "WhatsApp" and click "Set Up"
3. Follow the setup wizard

### Step 3: Get Credentials

1. **Phone Number ID**: 
   - Go to WhatsApp → API Setup
   - Copy "Phone number ID"

2. **Access Token**:
   - Go to WhatsApp → API Setup
   - Copy "Temporary access token" (for testing)
   - For production, set up a System User and get permanent token

3. **Verify Phone Number**:
   - Add your WhatsApp Business number
   - Verify it via SMS/call

### Step 4: Configure Application

Edit `src/main/resources/application.properties`:

```properties
# WhatsApp Cloud API Configuration
whatsapp.enabled=true
whatsapp.api.url=https://graph.facebook.com/v18.0
whatsapp.phone.number.id=YOUR_PHONE_NUMBER_ID
whatsapp.access.token=YOUR_ACCESS_TOKEN
```

### Step 5: Test

1. Restart the backend
2. Create a program in "scheduled" state
3. Check logs for WhatsApp messages
4. Verify message received on WhatsApp

## Database Tables

The system creates these new tables:

- `program_workflows` - Tracks workflow state per program
- `sewadar_form_submissions` - Stores travel details forms
- `notification_preferences` - Global notification settings

## How It Works

### Automatic Workflow Progression

- **Node 1 → Node 2**: When program status changes to "active"
- **Node 2 → Node 3**: When approved applications >= maxSewadars
- **Node 3 → Node 4**: When incharge clicks "Release Form"
- **Node 4 → Node 5**: When incharge clicks "Mark Details Collected"
- **Node 5 → Node 6**: When incharge clicks "Go to Next Node"
- **Node 6**: Final node (workflow complete)

### Daily Notifications

- Scheduler runs daily at 9:00 AM
- Sends WhatsApp messages to all incharges
- Only for programs at current workflow node
- Only if notification is enabled for that node
- Stops when node is marked complete

### Notification Preferences

- Global settings (apply to all programs)
- Each node can be toggled on/off
- Managed in Admin → Workflow page

## Frontend Pages

### For INCHARGE:
- **Workflow** page: View all program workflows, manage nodes, toggle notifications

### For SEWADAR:
- **Pending Actions** page: See programs requiring form submission, fill travel details

## API Endpoints

### Workflow
- `GET /api/workflow/program/{programId}` - Get workflow status
- `POST /api/workflow/program/{programId}/next-node` - Move to next node
- `POST /api/workflow/program/{programId}/release-form` - Release form
- `POST /api/workflow/program/{programId}/mark-details-collected` - Mark details collected

### Form Submissions
- `POST /api/form-submissions` - Submit travel details form
- `GET /api/form-submissions/program/{programId}` - Get all submissions for program
- `GET /api/form-submissions/program/{programId}/sewadar/{sewadarId}` - Get sewadar's submission

### Notification Preferences
- `GET /api/notification-preferences` - Get all preferences
- `PUT /api/notification-preferences/{id}/toggle?enabled={true/false}` - Toggle preference

## Testing the Workflow

1. **Create Program** (scheduled) → Workflow starts at Node 1
2. **Enable Node 1 notification** → Will receive daily reminder
3. **Change program to active** → Auto-advances to Node 2
4. **Enable Node 2 notification** → Will receive daily reminder
5. **Applications reach max** → Auto-advances to Node 3
6. **Click "Release Form"** → Moves to Node 4, sewadars see form
7. **Sewadars fill form** → Submit travel details
8. **Click "Mark Details Collected"** → Moves to Node 5
9. **Click "Go to Next Node"** → Moves to Node 6
10. **Click "Go to Next Node"** → Workflow complete

## Troubleshooting

### WhatsApp Not Sending
- Check `whatsapp.enabled=true` in properties
- Verify Phone Number ID and Access Token
- Check phone number format (country code + number, no +)
- Check Meta App permissions
- Review application logs

### Workflow Not Advancing
- Check program status matches expected state
- Verify application count logic
- Check database for workflow records

### Forms Not Showing
- Verify workflow is at node 4+
- Check `formReleased` is true
- Verify sewadar has approved application

## Cost Considerations

WhatsApp Cloud API:
- **Free Tier**: 1,000 conversations/month
- **Paid**: $0.005 - $0.09 per conversation
- **With 4 messages/day max**: ~120 messages/month = well within free tier

## Next Steps

1. Set up WhatsApp Cloud API credentials
2. Test workflow with a sample program
3. Configure notification preferences
4. Train users on workflow process

