# Report Response Examples

Minimal dummy response data for all incharge reports to differentiate them.

---

## 1. SEWADAR REPORTS

### 1.1 Sewadar Dashboard Report
**Endpoint:** `POST /api/dashboard/sewadars`  
**Export:** `POST /api/dashboard/sewadars/export/{CSV|XLSX|PDF}`

**Response:**
```json
{
  "sewadars": [
    {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210",
      "location": "Delhi",
      "profession": "Engineer",
      "joiningDate": "2023-01-15",
      "languages": ["Hindi", "English"],
      "totalProgramsCount": 5,
      "totalDaysAttended": 12,
      "beasProgramsCount": 2,
      "beasDaysAttended": 5,
      "nonBeasProgramsCount": 3,
      "nonBeasDaysAttended": 7
    },
    {
      "zonalId": "SEW002",
      "firstName": "Priya",
      "lastName": "Sharma",
      "mobile": "9876543211",
      "location": "Mumbai",
      "profession": "Doctor",
      "joiningDate": "2023-03-20",
      "languages": ["Hindi", "English", "Marathi"],
      "totalProgramsCount": 3,
      "totalDaysAttended": 8,
      "beasProgramsCount": 1,
      "beasDaysAttended": 3,
      "nonBeasProgramsCount": 2,
      "nonBeasDaysAttended": 5
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 25
}
```

**Key Features:**
- Aggregated statistics (counts, totals)
- Multiple sewadars in one response
- Pagination metadata
- Filterable and sortable

---

### 1.2 Individual Sewadar Detailed Attendance
**Endpoint:** `GET /api/dashboard/sewadar/{sewadarId}/attendance`  
**Export:** `GET /api/dashboard/sewadar/{sewadarId}/attendance/export/{CSV|XLSX|PDF}`

**Response:**
```json
{
  "sewadarId": "SEW001",
  "sewadarName": "Raghu Kumar",
  "mobile": "9876543210",
  "records": [
    {
      "programId": 1,
      "programTitle": "BEAS Satsang Program",
      "programLocation": "BEAS",
      "attendanceDate": "2024-01-15",
      "status": "Present"
    },
    {
      "programId": 1,
      "programTitle": "BEAS Satsang Program",
      "programLocation": "BEAS",
      "attendanceDate": "2024-01-22",
      "status": "Present"
    },
    {
      "programId": 2,
      "programTitle": "Delhi Satsang",
      "programLocation": "Delhi",
      "attendanceDate": "2024-02-10",
      "status": "Present"
    }
  ],
  "totalRecords": 3
}
```

**Key Features:**
- One row per program-date combination
- Shows individual attendance dates
- Single sewadar focus
- Chronological list of all attendances

---

### 1.3 Sewadar Attendance Summary
**Endpoint:** `GET /api/attendances/sewadar/{sewadarId}/summary`

**Response:**
```json
{
  "sewadarId": "SEW001",
  "sewadarName": "Raghu Kumar",
  "mobile": "9876543210",
  "beasProgramsCount": 2,
  "beasDaysAttended": 5,
  "beasAttendances": [
    {
      "programId": 1,
      "programTitle": "BEAS Satsang Program",
      "location": "BEAS",
      "locationType": "BEAS",
      "attended": true,
      "daysParticipated": 3,
      "markedAt": "2024-01-15T10:30:00"
    },
    {
      "programId": 3,
      "programTitle": "BEAS Annual Program",
      "location": "BEAS",
      "locationType": "BEAS",
      "attended": true,
      "daysParticipated": 2,
      "markedAt": "2024-03-20T09:15:00"
    }
  ],
  "nonBeasProgramsCount": 3,
  "nonBeasDaysAttended": 7,
  "nonBeasAttendances": [
    {
      "programId": 2,
      "programTitle": "Delhi Satsang",
      "location": "Delhi",
      "locationType": "NON_BEAS",
      "attended": true,
      "daysParticipated": 4,
      "markedAt": "2024-02-10T11:00:00"
    },
    {
      "programId": 4,
      "programTitle": "Mumbai Satsang",
      "location": "Mumbai",
      "locationType": "NON_BEAS",
      "attended": true,
      "daysParticipated": 3,
      "markedAt": "2024-04-05T10:45:00"
    }
  ],
  "totalProgramsCount": 5,
  "totalDaysAttended": 12
}
```

**Key Features:**
- BEAS vs Non-BEAS breakdown
- List of individual program attendances
- Days participated per program
- Aggregated totals

---

## 2. APPLICATION REPORTS

### 2.1 Applications Dashboard Report
**Endpoint:** `POST /api/dashboard/applications`  
**Export:** `POST /api/dashboard/applications/export/{CSV|XLSX|PDF}`

**Response:**
```json
{
  "applications": [
    {
      "applicationId": 1,
      "sewadarZonalId": "SEW001",
      "sewadarName": "Raghu Kumar",
      "mobile": "9876543210",
      "status": "APPROVED",
      "appliedAt": "2024-01-10T14:30:00"
    },
    {
      "applicationId": 2,
      "sewadarZonalId": "SEW002",
      "sewadarName": "Priya Sharma",
      "mobile": "9876543211",
      "status": "PENDING",
      "appliedAt": "2024-01-11T09:15:00"
    },
    {
      "applicationId": 3,
      "sewadarZonalId": "SEW003",
      "sewadarName": "Amit Patel",
      "mobile": "9876543212",
      "status": "DROPPED",
      "appliedAt": "2024-01-08T16:20:00"
    }
  ],
  "totalElements": 3,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 25
}
```

**Key Features:**
- Flat list of applications
- Status tracking
- Timestamp of application
- Pagination support

---

### 2.2 Prioritized Applications (for a program)
**Endpoint:** `GET /api/program-applications/program/{programId}/prioritized?sortBy=attendance&order=desc`

**Response:**
```json
[
  {
    "id": 1,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "status": "PENDING",
    "appliedAt": "2024-01-10T14:30:00",
    "totalAttendanceCount": 15,
    "beasAttendanceCount": 8,
    "nonBeasAttendanceCount": 7,
    "totalDaysAttended": 20,
    "beasDaysAttended": 12,
    "nonBeasDaysAttended": 8,
    "profession": "Engineer",
    "joiningDate": "2023-01-15",
    "priorityScore": 95.5
  },
  {
    "id": 2,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW002",
      "firstName": "Priya",
      "lastName": "Sharma",
      "mobile": "9876543211"
    },
    "status": "PENDING",
    "appliedAt": "2024-01-11T09:15:00",
    "totalAttendanceCount": 10,
    "beasAttendanceCount": 5,
    "nonBeasAttendanceCount": 5,
    "totalDaysAttended": 15,
    "beasDaysAttended": 8,
    "nonBeasDaysAttended": 7,
    "profession": "Doctor",
    "joiningDate": "2023-03-20",
    "priorityScore": 78.2
  }
]
```

**Key Features:**
- Includes priority metrics (attendance counts, days, score)
- Sorted by selected metric
- Full sewadar object embedded
- Used for decision-making on applications

---

### 2.3 Drop Requests Report
**Endpoint:** `GET /api/program-applications/program/{programId}/drop-requests`

**Response:**
```json
[
  {
    "id": 3,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW003",
      "firstName": "Amit",
      "lastName": "Patel",
      "mobile": "9876543212"
    },
    "status": "DROP_REQUESTED",
    "appliedAt": "2024-01-08T16:20:00",
    "dropRequestedAt": "2024-01-25T10:30:00",
    "notes": null
  },
  {
    "id": 5,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW005",
      "firstName": "Sunita",
      "lastName": "Singh",
      "mobile": "9876543214"
    },
    "status": "DROP_REQUESTED",
    "appliedAt": "2024-01-12T11:45:00",
    "dropRequestedAt": "2024-01-26T14:20:00",
    "notes": null
  }
]
```

**Key Features:**
- Only DROP_REQUESTED status applications
- Shows drop request timestamp
- Pending approval list

---

## 3. PROGRAM ATTENDANCE REPORTS

### 3.1 Program Detailed Attendance
**Endpoint:** `GET /api/dashboard/program/{programId}/attendance`  
**Export:** `GET /api/dashboard/program/{programId}/attendance/export/{CSV|XLSX|PDF}`

**Response:**
```json
{
  "programId": 1,
  "programTitle": "BEAS Satsang Program",
  "programDates": ["2024-01-15", "2024-01-22", "2024-01-29"],
  "sewadarRows": [
    {
      "zonalId": "SEW001",
      "sewadarName": "Raghu Kumar",
      "mobile": "9876543210",
      "dateStatusMap": {
        "2024-01-15": "Present",
        "2024-01-22": "Present",
        "2024-01-29": "Absent"
      }
    },
    {
      "zonalId": "SEW002",
      "sewadarName": "Priya Sharma",
      "mobile": "9876543211",
      "dateStatusMap": {
        "2024-01-15": "Present",
        "2024-01-22": "Present",
        "2024-01-29": "Present"
      }
    }
  ],
  "totalSewadars": 2
}
```

**Key Features:**
- Matrix format: rows = sewadars, columns = dates
- Date columns dynamically created from program dates
- Shows Present/Absent for each date
- Spreadsheet-like structure

---

### 3.2 Program Attendance List
**Endpoint:** `GET /api/attendances/program/{programId}`

**Response:**
```json
[
  {
    "id": 1,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "attendanceDate": "2024-01-15",
    "programDateId": 10,
    "markedAt": "2024-01-15T10:30:00",
    "notes": "Arrived on time"
  },
  {
    "id": 2,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "attendanceDate": "2024-01-22",
    "programDateId": 11,
    "markedAt": "2024-01-22T10:25:00",
    "notes": null
  },
  {
    "id": 3,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW002",
      "firstName": "Priya",
      "lastName": "Sharma",
      "mobile": "9876543211"
    },
    "attendanceDate": "2024-01-15",
    "programDateId": 10,
    "markedAt": "2024-01-15T10:35:00",
    "notes": "Late arrival"
  }
]
```

**Key Features:**
- One row per attendance record
- Includes notes and markedAt timestamp
- Full sewadar object per record
- Chronological list

---

### 3.3 Program Attendance Statistics
**Endpoint:** `GET /api/attendances/program/{programId}/statistics`

**Response:**
```json
[
  {
    "id": 1,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar"
    },
    "attendanceDate": "2024-01-15",
    "programDateId": 10,
    "markedAt": "2024-01-15T10:30:00",
    "notes": null
  }
]
```

**Note:** This endpoint currently returns the same structure as 3.2. The statistics calculation may be done client-side or needs implementation.

---

## 4. AGGREGATE REPORTS

### 4.1 All Sewadars Attendance Summary
**Endpoint:** `GET /api/attendances/all-sewadars/summary`

**Response:**
```json
{
  "sewadars": [
    {
      "sewadarId": "SEW001",
      "sewadarName": "Raghu Kumar",
      "mobile": "9876543210",
      "beasProgramsCount": 2,
      "beasDaysAttended": 5,
      "nonBeasProgramsCount": 3,
      "nonBeasDaysAttended": 7,
      "totalProgramsCount": 5,
      "totalDaysAttended": 12
    },
    {
      "sewadarId": "SEW002",
      "sewadarName": "Priya Sharma",
      "mobile": "9876543211",
      "beasProgramsCount": 1,
      "beasDaysAttended": 3,
      "nonBeasProgramsCount": 2,
      "nonBeasDaysAttended": 5,
      "totalProgramsCount": 3,
      "totalDaysAttended": 8
    },
    {
      "sewadarId": "SEW003",
      "sewadarName": "Amit Patel",
      "mobile": "9876543212",
      "beasProgramsCount": 0,
      "beasDaysAttended": 0,
      "nonBeasProgramsCount": 1,
      "nonBeasDaysAttended": 2,
      "totalProgramsCount": 1,
      "totalDaysAttended": 2
    }
  ]
}
```

**Key Features:**
- All sewadars in one response
- Aggregated statistics only (no detailed lists)
- BEAS/non-BEAS breakdown
- Summary-level data

---

## 5. OPERATIONAL REPORTS

### 5.1 Approved Attendees List
**Endpoint:** `GET /api/attendances/program/{programId}/attendees`

**Response:**
```json
[
  {
    "zonalId": "SEW001",
    "firstName": "Raghu",
    "lastName": "Kumar",
    "mobile": "9876543210",
    "applicationId": 1,
    "applicationStatus": "APPROVED"
  },
  {
    "zonalId": "SEW002",
    "firstName": "Priya",
    "lastName": "Sharma",
    "mobile": "9876543211",
    "applicationId": 2,
    "applicationStatus": "APPROVED"
  }
]
```

**Key Features:**
- Only approved sewadars
- Used for attendance marking UI
- Links to application ID
- Minimal data (name, mobile, zonalId)

---

### 5.2 Sewadar Attendance by Sewadar
**Endpoint:** `GET /api/attendances/sewadar/{sewadarId}`

**Response:**
```json
[
  {
    "id": 1,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "attendanceDate": "2024-01-15",
    "programDateId": 10,
    "markedAt": "2024-01-15T10:30:00",
    "notes": "Arrived on time"
  },
  {
    "id": 2,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "attendanceDate": "2024-01-22",
    "programDateId": 11,
    "markedAt": "2024-01-22T10:25:00",
    "notes": null
  },
  {
    "id": 5,
    "programId": 2,
    "programTitle": "Delhi Satsang",
    "sewadar": {
      "zonalId": "SEW001",
      "firstName": "Raghu",
      "lastName": "Kumar",
      "mobile": "9876543210"
    },
    "attendanceDate": "2024-02-10",
    "programDateId": 20,
    "markedAt": "2024-02-10T11:00:00",
    "notes": null
  }
]
```

**Key Features:**
- All attendance records for one sewadar
- Across all programs
- Same structure as 3.2 but filtered by sewadar
- Chronological across programs

---

### 5.3 Form Submissions Report
**Endpoint:** `GET /api/form-submissions/program/{programId}`

**Response:**
```json
[
  {
    "id": 1,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadarId": "SEW001",
    "sewadarName": "Raghu Kumar",
    "name": "Raghu Kumar",
    "startingDateTimeFromHome": "2024-01-14T18:00:00",
    "reachingDateTimeToHome": "2024-01-16T20:00:00",
    "onwardTrainFlightDateTime": "2024-01-14T20:30:00",
    "onwardTrainFlightNo": "12345",
    "returnTrainFlightDateTime": "2024-01-16T18:00:00",
    "returnTrainFlightNo": "12346",
    "stayInHotel": "Hotel ABC, Room 101",
    "stayInPandal": null,
    "submittedAt": "2024-01-13T15:30:00"
  },
  {
    "id": 2,
    "programId": 1,
    "programTitle": "BEAS Satsang Program",
    "sewadarId": "SEW002",
    "sewadarName": "Priya Sharma",
    "name": "Priya Sharma",
    "startingDateTimeFromHome": "2024-01-14T19:00:00",
    "reachingDateTimeToHome": "2024-01-16T21:00:00",
    "onwardTrainFlightDateTime": "2024-01-14T21:00:00",
    "onwardTrainFlightNo": "67890",
    "returnTrainFlightDateTime": "2024-01-16T19:00:00",
    "returnTrainFlightNo": "67891",
    "stayInHotel": null,
    "stayInPandal": "Main Pandal, Section A",
    "submittedAt": "2024-01-13T16:45:00"
  }
]
```

**Key Features:**
- Travel details for program
- Train/flight information
- Stay arrangements (hotel/pandal)
- Submission timestamps
- Used for logistics planning

---

## KEY DIFFERENCES SUMMARY

| Report Type | Structure | Focus | Use Case |
|------------|-----------|-------|----------|
| **1.1 Dashboard** | Aggregated stats | Multiple sewadars | Overview, filtering, sorting |
| **1.2 Detailed** | One row per date | Single sewadar, all dates | Individual history |
| **1.3 Summary** | BEAS/non-BEAS split | Single sewadar breakdown | Location-based analysis |
| **2.1 Applications** | Flat list | All applications | Status tracking |
| **2.2 Prioritized** | With priority metrics | Program applications | Decision making |
| **2.3 Drop Requests** | Filtered list | Pending drops | Approval workflow |
| **3.1 Program Detailed** | Matrix (rows=sewadars, cols=dates) | Single program | Attendance matrix view |
| **3.2 Program List** | One row per record | Single program | Detailed records |
| **4.1 All Summary** | Aggregated only | All sewadars | High-level overview |
| **5.1 Attendees** | Minimal data | Approved only | Attendance marking UI |
| **5.2 By Sewadar** | All records | Single sewadar | Complete history |
| **5.3 Forms** | Travel details | Program logistics | Planning & coordination |

