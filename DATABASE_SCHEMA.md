# Complete Database Schema Documentation

## Tables Overview

1. **sewadars** - User accounts (Incharge and Sewadars)
2. **addresses** - Address information for sewadars
3. **programs** - Spiritual programs
4. **program_dates** - Dates for each program
5. **program_applications** - Sewadar applications to programs
6. **program_selections** - Incharge selections of sewadars
7. **actions** - Actions/steps created by incharge for programs
8. **action_responses** - Sewadar responses to actions
9. **attendances** - Attendance records
10. **notifications** - Notifications for incharge
11. **schedules** - Legacy schedule table

---

## Table: sewadars

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| first_name | varchar(100) | NO | - | First name |
| last_name | varchar(100) | NO | - | Last name |
| mobile | varchar(20) | YES | - | Mobile number (UNIQUE) |
| password | varchar(255) | NO | - | Hashed password |
| role | varchar(255) | NO | - | INCHARGE or SEWADAR |
| dept | varchar(100) | YES | - | Department |
| profession | varchar(100) | YES | - | Profession |
| joining_date | date | YES | - | Joining date |
| remarks | varchar(500) | YES | - | Remarks |
| address_id | bigint | YES | - | FK to addresses(id) |

**Constraints:**
- role CHECK: 'INCHARGE' or 'SEWADAR'
- mobile UNIQUE

**Foreign Keys:**
- address_id → addresses(id)

**Referenced By:**
- programs.created_by
- program_applications.sewadar_id
- program_selections.sewadar_id
- program_selections.selected_by
- actions.created_by
- action_responses.sewadar_id
- attendances.sewadar_id
- notifications.sewadar_id (dropped sewadar)
- notifications.incharge_id
- schedules.attended_by

---

## Table: addresses

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| address1 | varchar(255) | NO | - | Address line 1 |
| address2 | varchar(255) | YES | - | Address line 2 |
| email | varchar(100) | YES | - | Email address |

**Referenced By:**
- sewadars.address_id

---

## Table: programs

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| title | varchar(255) | NO | - | Program title |
| description | varchar(1000) | YES | - | Description |
| location | varchar(255) | NO | - | Location |
| location_type | varchar(50) | YES | - | BEAS or NON_BEAS |
| status | varchar(50) | YES | - | Program status |
| max_sewadars | integer | YES | - | Maximum sewadars allowed |
| created_by | bigint | NO | - | FK to sewadars(id) - incharge |

**Foreign Keys:**
- created_by → sewadars(id)

**Referenced By:**
- program_dates.program_id
- program_applications.program_id
- program_selections.program_id
- actions.program_id
- attendances.program_id
- notifications.program_id

---

## Table: program_dates

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| program_date | date | NO | - | Date |
| status | varchar(50) | YES | - | Date status |

**Foreign Keys:**
- program_id → programs(id)

---

## Table: program_applications

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| sewadar_id | bigint | NO | - | FK to sewadars(id) |
| applied_at | timestamp | NO | - | Application timestamp |
| status | varchar(50) | YES | - | PENDING, APPROVED, REJECTED, DROP_REQUESTED, DROPPED |
| notes | varchar(500) | YES | - | Notes |
| reapply_allowed | boolean | YES | - | Can sewadar reapply? |
| drop_requested_at | timestamp | YES | - | When drop was requested |
| drop_approved_at | timestamp | YES | - | When drop was approved |
| drop_approved_by | bigint | YES | - | FK to sewadars(id) - incharge who approved |

**Foreign Keys:**
- program_id → programs(id)
- sewadar_id → sewadars(id)
- drop_approved_by → sewadars(id)

**Status Values:**
- PENDING
- APPROVED
- REJECTED
- DROP_REQUESTED
- DROPPED

---

## Table: program_selections

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| sewadar_id | bigint | NO | - | FK to sewadars(id) |
| selected_by | bigint | NO | - | FK to sewadars(id) - incharge |
| selected_at | timestamp | NO | - | Selection timestamp |
| status | varchar(50) | YES | - | SELECTED, CONFIRMED, DROPPED, REPLACED |
| priority_score | integer | YES | - | Priority score |
| selection_reason | varchar(500) | YES | - | Reason for selection |

**Foreign Keys:**
- program_id → programs(id)
- sewadar_id → sewadars(id)
- selected_by → sewadars(id)

**Status Values:**
- SELECTED
- CONFIRMED
- DROPPED
- REPLACED

---

## Table: actions

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| title | varchar(255) | NO | - | Action title |
| description | varchar(1000) | YES | - | Description |
| action_type | varchar(100) | YES | - | Action type |
| created_by | bigint | NO | - | FK to sewadars(id) - incharge |
| created_at | timestamp | NO | - | Creation timestamp |
| due_date | timestamp | YES | - | Due date |
| status | varchar(50) | YES | - | ACTIVE, COMPLETED, etc. |
| sequence_order | integer | YES | - | Order sequence |

**Foreign Keys:**
- program_id → programs(id)
- created_by → sewadars(id)

**Referenced By:**
- action_responses.action_id

---

## Table: action_responses

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| action_id | bigint | NO | - | FK to actions(id) |
| sewadar_id | bigint | NO | - | FK to sewadars(id) |
| response_data | text | YES | - | Response data |
| notes | varchar(500) | YES | - | Notes |
| status | varchar(50) | YES | - | PENDING, COMPLETED |
| submitted_at | timestamp | YES | - | Submission timestamp |

**Foreign Keys:**
- action_id → actions(id)
- sewadar_id → sewadars(id)

---

## Table: attendances

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| sewadar_id | bigint | NO | - | FK to sewadars(id) |
| attended | boolean | NO | - | Attended or not |
| days_participated | integer | YES | - | Days participated |
| marked_by | bigint | NO | - | FK to sewadars(id) - incharge |
| marked_at | timestamp | NO | - | Marking timestamp |
| notes | varchar(500) | YES | - | Notes |

**Foreign Keys:**
- program_id → programs(id)
- sewadar_id → sewadars(id)
- marked_by → sewadars(id)

---

## Table: notifications

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| program_id | bigint | NO | - | FK to programs(id) |
| sewadar_id | bigint | NO | - | FK to sewadars(id) - dropped sewadar |
| incharge_id | bigint | NO | - | FK to sewadars(id) - incharge |
| notification_type | varchar(50) | YES | - | DROP_REQUEST, REFILL_REQUIRED |
| message | varchar(500) | YES | - | Notification message |
| created_at | timestamp | NO | - | Creation timestamp |
| resolved | boolean | NO | false | Is resolved? |
| resolved_at | timestamp | YES | - | Resolution timestamp |
| resolved_by | bigint | YES | - | FK to sewadars(id) - incharge who resolved |

**Foreign Keys:**
- program_id → programs(id)
- sewadar_id → sewadars(id) - dropped sewadar
- incharge_id → sewadars(id) - incharge

---

## Table: schedules

| Column | Type | Nullable | Default | Description |
|--------|------|----------|---------|-------------|
| id | bigint | NO | auto | Primary key |
| attended_by | bigint | NO | - | FK to sewadars(id) |
| scheduled_place | varchar(255) | NO | - | Place |
| scheduled_date | date | NO | - | Date |
| scheduled_time | time | NO | - | Time |
| scheduled_medium | varchar(100) | YES | - | Medium |

**Foreign Keys:**
- attended_by → sewadars(id)

---

## Current Status Values in Database

### program_applications.status:
- PENDING
- DROPPED
- (Also supports: APPROVED, REJECTED, DROP_REQUESTED)

### program_selections.status:
- SELECTED
- DROPPED
- (Also supports: CONFIRMED, REPLACED)

### actions.status:
- ACTIVE
- (Also supports: COMPLETED, etc.)

### action_responses.status:
- (Currently empty, supports: PENDING, COMPLETED)

---

## Key Relationships

1. **Program → Applications**: One program has many applications
2. **Program → Selections**: One program has many selections
3. **Program → Actions**: One program has many actions
4. **Program → Dates**: One program has many dates
5. **Sewadar → Applications**: One sewadar can have many applications
6. **Sewadar → Selections**: One sewadar can have many selections
7. **Action → Responses**: One action can have many responses
8. **Application ↔ Selection**: Linked by program_id + sewadar_id

---

## Current Data Summary

- **Sewadars**: 4 (1 INCHARGE, 3 SEWADAR)
- **Programs**: 1 (max_sewadars=1)
- **Applications**: 3 (1 DROPPED, 2 PENDING)
- **Selections**: 2 (1 DROPPED, 1 SELECTED)
- **Actions**: 2 (both ACTIVE)
- **Notifications**: 1 (unresolved)




