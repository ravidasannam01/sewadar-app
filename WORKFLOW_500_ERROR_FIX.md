# Workflow 500 Error Fix

## Issues Fixed

### 1. Lazy Loading Exception in Workflow Service
**Problem**: `ProgramWorkflow.getProgram()` was being accessed after the transaction ended, causing `LazyInitializationException`.

**Solution**:
- Changed `mapToResponse()` to accept `Program` as a parameter (already loaded in transaction)
- Changed all repository queries from `findByProgram(program)` to `findByProgramId(programId)`
- Ensured Program data is accessed within the transaction before mapping

### 2. Transaction Issue in Notification Preferences
**Problem**: `initializeDefaults()` was being called from a `@Transactional(readOnly = true)` method, causing transaction errors.

**Solution**:
- Removed `readOnly = true` from `getAllPreferences()` method
- The class-level `@Transactional` will handle write operations when needed

## Changes Made

### ProgramWorkflowService.java
- Updated `getWorkflow()` to use `findByProgramId()` and pass Program to `mapToResponse()`
- Updated `getWorkflowsForIncharge()` to use `findByProgramId()` and handle initialization properly
- Updated `moveToNextNode()`, `releaseForm()`, `markDetailsCollected()` to use `findByProgramId()`
- Updated `mapToResponse()` signature to accept `Program` parameter
- Added `initializeAndGetWorkflow()` for proper transaction handling

### NotificationPreferenceService.java
- Removed `readOnly = true` from `getAllPreferences()` method
- Simplified transaction handling

## Next Steps - IMPORTANT

**You MUST restart your backend server** for these changes to take effect:

```bash
# Stop the current Spring Boot server (Ctrl+C)
# Then restart it:
cd /Users/ravidas.a/Desktop/application
mvn spring-boot:run
```

Or if you're running it as a JAR:
```bash
# Stop the current process
# Then rebuild and restart:
mvn clean package -DskipTests
java -jar target/application-*.jar
```

## Testing After Restart

1. **Check Notification Preferences**:
   - Navigate to Workflow page
   - Should see global notification preferences loaded without 500 errors

2. **Check Workflows**:
   - Should see workflows for all programs
   - No more 500 errors on `/api/workflow/program/{id}` endpoints

3. **Verify Program-Level Preferences**:
   - Each program should show workflow stepper
   - Each node should have a toggle switch

## If Errors Persist

1. **Check Backend Logs**: Look for stack traces in the Spring Boot console
2. **Verify Database**: Ensure `program_workflows` and `notification_preferences` tables exist
3. **Check Database Connection**: Verify Spring Boot can connect to the database
4. **Clear Browser Cache**: Sometimes cached API calls can cause issues

## Database Tables Required

Make sure these tables exist:
- `program_workflows` - Stores workflow state per program
- `notification_preferences` - Global notification settings (auto-initialized)
- `program_notification_preferences` - Program-level notification overrides

If tables don't exist, Spring Boot should create them automatically if `spring.jpa.hibernate.ddl-auto` is set to `update` or `create`.

