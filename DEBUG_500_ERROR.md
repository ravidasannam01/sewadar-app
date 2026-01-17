# Debugging 500 Error in Workflow Endpoint

## Current Status
The code compiles successfully, but you're still getting 500 errors. This means there's a **runtime error** happening in the backend.

## Critical: Check Backend Logs

**The most important step is to check your Spring Boot console/logs** to see the actual error message. The error will show:
- The exception type (e.g., `LazyInitializationException`, `DataIntegrityViolationException`, `SQLException`)
- The stack trace showing exactly where it fails
- The root cause

## Common Causes and Fixes

### 1. Database Table Missing
**Error**: `Table "program_workflows" does not exist`

**Fix**: 
- Check if `program_workflows` table exists in your database
- If using `spring.jpa.hibernate.ddl-auto=update`, Spring should create it automatically
- If not, you may need to set it to `create` temporarily or create the table manually

**SQL to create table manually**:
```sql
CREATE TABLE IF NOT EXISTS program_workflows (
    id BIGSERIAL PRIMARY KEY,
    program_id BIGINT NOT NULL UNIQUE,
    current_node INTEGER NOT NULL DEFAULT 1,
    form_released BOOLEAN NOT NULL DEFAULT FALSE,
    details_collected BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,
    CONSTRAINT fk_program FOREIGN KEY (program_id) REFERENCES programs(id)
);
```

### 2. Foreign Key Constraint Issue
**Error**: Foreign key violation or constraint error

**Fix**: 
- Ensure the `programs` table exists
- Ensure the program with the given ID exists
- Check foreign key constraints

### 3. Transaction Issue
**Error**: Transaction-related errors

**Fix**: 
- The code now uses self-injection for proper transaction handling
- If still failing, try restarting the backend

### 4. Lazy Loading Exception
**Error**: `LazyInitializationException` or `could not initialize proxy`

**Fix**: 
- This should be fixed in the latest code
- Make sure you restarted the backend after the latest changes

## Steps to Debug

1. **Check Backend Console/Logs**:
   ```bash
   # Look for lines like:
   ERROR c.r.a.service.ProgramWorkflowService - Error getting workflow...
   # or
   Exception in thread...
   ```

2. **Check Database**:
   ```sql
   -- Connect to your database
   psql -U postgres -d sewadar_db
   
   -- Check if table exists
   \d program_workflows
   
   -- Check if programs exist
   SELECT id, title FROM programs LIMIT 5;
   ```

3. **Test the Endpoint Directly**:
   ```bash
   # Get your JWT token from browser (from localStorage or network tab)
   curl -X GET "http://localhost:8080/api/workflow/program/12" \
     -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     -H "Content-Type: application/json"
   ```

4. **Check Application Properties**:
   - Verify `spring.jpa.hibernate.ddl-auto=update` is set
   - Verify database connection is working

## Quick Fix: Initialize All Workflows

Try calling the initialization endpoint first:

```bash
curl -X POST "http://localhost:8080/api/workflow/initialize-all" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

This will create workflows for all existing programs.

## What to Share

If errors persist, please share:
1. **The exact error message from backend logs** (the full stack trace)
2. **Database table status** (does `program_workflows` exist?)
3. **Any SQL errors** from the database logs

## Latest Code Changes

The latest code includes:
- ✅ Self-injection for transaction handling
- ✅ Better error logging
- ✅ Proper lazy loading handling
- ✅ Fallback mechanisms

Make sure you've **restarted the backend** after pulling these changes!

