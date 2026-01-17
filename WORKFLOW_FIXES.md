# Workflow System Fixes and Improvements

## Issues Fixed

### 1. Program-Level Workflows
- **Issue**: Workflows were not properly initialized for existing programs
- **Fix**: 
  - Added automatic workflow initialization when fetching workflows for an incharge
  - Added `initializeAllMissingWorkflows()` method to initialize workflows for all existing programs
  - Workflows are now created automatically when programs are created

### 2. Program-Level Notification Preferences
- **Issue**: Only global notification preferences existed, no program-level overrides
- **Fix**:
  - Created `ProgramNotificationPreference` entity for program-specific settings
  - Program-level preferences override global preferences
  - Each program can have its own notification settings per workflow node
  - Frontend now displays both global and program-level preferences

### 3. Frontend Workflow Display
- **Issue**: Workflows were not showing in the frontend
- **Fix**:
  - Improved error handling in frontend workflow loading
  - Added proper loading states and error messages
  - Fixed API endpoint calls to match backend routes
  - Added program-level preference toggles in the UI

## New Features

### Program-Level Settings
- Each program can have its own notification preferences
- Program-level settings override global settings
- Toggle between: Use Global → On → Off → Use Global (cycle)

### Workflow Initialization
- New programs automatically get workflows initialized
- Existing programs get workflows initialized when accessed
- Endpoint to initialize all missing workflows: `POST /api/workflow/initialize-all`

## Database Schema

### New Tables
1. **program_notification_preferences**
   - `id` (PK)
   - `program_id` (FK to programs)
   - `node_number` (1-6)
   - `enabled` (null = use global, true/false = override)
   - `created_at`, `updated_at`

## API Endpoints

### Workflow
- `GET /api/workflow/program/{programId}` - Get workflow for a program
- `GET /api/workflow/incharge/{inchargeId}` - Get all workflows for an incharge
- `POST /api/workflow/program/{programId}/next-node` - Move to next node
- `POST /api/workflow/program/{programId}/release-form` - Release form
- `POST /api/workflow/program/{programId}/mark-details-collected` - Mark details collected
- `POST /api/workflow/initialize-all` - Initialize workflows for all programs

### Program Notification Preferences
- `GET /api/program-notification-preferences/program/{programId}` - Get preferences for a program
- `PUT /api/program-notification-preferences/program/{programId}/node/{nodeNumber}?enabled={true/false/null}` - Update preference

## How It Works

### Workflow Progression
1. **Node 1**: Make Program Active
   - Program starts in "scheduled" status
   - Incharge changes status to "active" → auto-advances to Node 2

2. **Node 2**: Post Application Message
   - Daily reminder to post application message
   - When applications are full → auto-advances to Node 3

3. **Node 3**: Release Form
   - Incharge clicks "Release Form" button
   - Sewadars see form in "Pending Actions"
   - Incharge clicks "Go to Next Node" → advances to Node 4

4. **Node 4**: Collect Details
   - Sewadars fill travel details form
   - Incharge clicks "Mark Details Collected" → advances to Node 5

5. **Node 5**: Post Mail to Area Secretary
   - Incharge clicks "Go to Next Node" → advances to Node 6

6. **Node 6**: Post General Instructions
   - Final node, workflow complete

### Notification System
- **Global Preferences**: Apply to all programs by default
- **Program Preferences**: Override global settings for specific programs
- **Effective Setting**: Program-level if set, otherwise global
- **Daily Scheduler**: Sends notifications at 9:00 AM for enabled nodes

## Frontend Usage

### For INCHARGE:
1. Navigate to **Workflow** page
2. See all programs with their workflow status
3. Toggle global notification preferences (top section)
4. Toggle program-level preferences (per program, per node)
5. Click "Go to Next Node" to advance workflow
6. Click "Release Form" when at Node 3
7. Click "Mark Details Collected" when at Node 4

### Workflow Display
- Each program shows a vertical stepper with 6 nodes
- Completed nodes show green checkmark
- Current node shows blue circle
- Future nodes show gray circle
- Each node has a toggle switch showing:
  - "Global" = using global setting
  - "On" = program-level override enabled
  - "Off" = program-level override disabled

## Testing

1. **Initialize Missing Workflows**:
   ```bash
   curl -X POST http://localhost:8080/api/workflow/initialize-all \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

2. **Check Workflow for Program**:
   ```bash
   curl http://localhost:8080/api/workflow/program/1 \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

3. **Update Program Preference**:
   ```bash
   curl -X PUT "http://localhost:8080/api/program-notification-preferences/program/1/node/1?enabled=true" \
     -H "Authorization: Bearer YOUR_TOKEN"
   ```

## Troubleshooting

### Workflows Not Showing
1. Check if programs exist: `GET /api/programs/incharge/{inchargeId}`
2. Initialize missing workflows: `POST /api/workflow/initialize-all`
3. Check browser console for API errors
4. Verify backend is running and endpoints are accessible

### Preferences Not Working
1. Ensure global preferences are initialized (check `notification_preferences` table)
2. Check program preferences in `program_notification_preferences` table
3. Verify notification scheduler is running (check application logs)

## Next Steps

1. Test the workflow system with real programs
2. Verify WhatsApp notifications are being sent
3. Test program-level preference overrides
4. Ensure all existing programs have workflows initialized

