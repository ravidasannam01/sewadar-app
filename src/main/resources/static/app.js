// API Configuration
const API_BASE_URL = window.location.origin + '/api'; // Use relative URL for production compatibility

// Global state
let currentUser = null;
let authToken = null;

// Utility Functions
function showMessage(text, type = 'success') {
    const messageEl = document.getElementById('message');
    messageEl.textContent = text;
    messageEl.className = `message ${type}`;
    messageEl.style.display = 'block';
    setTimeout(() => {
        messageEl.style.display = 'none';
    }, 3000);
}

function showError(elementId, message) {
    const errorEl = document.getElementById(elementId);
    errorEl.textContent = message;
    errorEl.style.display = 'block';
    setTimeout(() => {
        errorEl.style.display = 'none';
    }, 5000);
}

function getAuthHeaders() {
    return {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${authToken}`
    };
}

// Authentication
async function handleLogin(event) {
    event.preventDefault();
    const zonalId = document.getElementById('login-zonal-id').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ zonalId, password })
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Login failed');
        }

        const data = await response.json();
        authToken = data.token;
        currentUser = data.sewadar;

        // Store token
        localStorage.setItem('authToken', authToken);
        localStorage.setItem('currentUser', JSON.stringify(currentUser));

        // Show main app
        document.getElementById('login-screen').style.display = 'none';
        document.getElementById('main-app').style.display = 'block';

        // Update UI
        updateUserInfo();
        loadInitialData();
    } catch (error) {
        showError('login-error', error.message);
    }
}

function logout() {
    authToken = null;
    currentUser = null;
    localStorage.removeItem('authToken');
    localStorage.removeItem('currentUser');
    document.getElementById('login-screen').style.display = 'block';
    document.getElementById('main-app').style.display = 'none';
}

function updateUserInfo() {
    if (currentUser) {
        document.getElementById('user-name').textContent = `${currentUser.firstName} ${currentUser.lastName}`;
        const roleBadge = document.getElementById('user-role');
        roleBadge.textContent = currentUser.role;
        roleBadge.className = `role-badge ${currentUser.role.toLowerCase()}`;
        
        // Show admin tab if incharge
        if (currentUser.role === 'INCHARGE') {
            document.getElementById('admin-tab-btn').style.display = 'block';
            document.getElementById('create-program-btn').style.display = 'block';
        }
    }
}

function checkAuth() {
    const token = localStorage.getItem('authToken');
    const user = localStorage.getItem('currentUser');
    
    if (token && user) {
        authToken = token;
        currentUser = JSON.parse(user);
        // Ensure zonalId is available (backward compatibility for old localStorage data)
        if (!currentUser.zonalId && currentUser.id) {
            currentUser.zonalId = currentUser.id;
        }
        document.getElementById('login-screen').style.display = 'none';
        document.getElementById('main-app').style.display = 'block';
        updateUserInfo();
        loadInitialData();
    }
}

// Tab Management
function showTab(tabName) {
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');

    // Load data for selected tab
    if (tabName === 'programs') {
        loadPrograms();
    } else if (tabName === 'applications') {
        loadMyApplications();
    } else if (tabName === 'admin') {
        // Always load sewadars when admin tab is clicked (if incharge)
        if (currentUser && currentUser.role === 'INCHARGE') {
            loadAllSewadars();
        }
        loadAdminData();
    }
}

// Program Management
async function loadPrograms() {
    try {
        const response = await fetch(`${API_BASE_URL}/programs`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load programs');
        
        const programs = await response.json();
        const listEl = document.getElementById('programs-list');
        
        if (programs.length === 0) {
            listEl.innerHTML = '<p>No programs found.</p>';
            return;
        }

        // For sewadars, check their applications to show Reapply button
        let myApplications = [];
        if (currentUser.role === 'SEWADAR') {
            try {
                const appsResponse = await fetch(`${API_BASE_URL}/program-applications/sewadar/${currentUser.zonalId}`, {
                    headers: getAuthHeaders()
                });
                if (appsResponse.ok) {
                    myApplications = await appsResponse.json();
                }
            } catch (e) {
                console.error('Error loading applications:', e);
            }
        }

        listEl.innerHTML = programs.map(program => {
            // Check if sewadar has dropped this program
            const myApp = myApplications.find(a => a.programId === program.id);
            let applyButton = '';
            
            if (currentUser.role === 'SEWADAR') {
                // Only show apply button for active programs
                if (program.status === 'active') {
                    if (myApp && myApp.status === 'DROPPED') {
                        // Always allow reapply (reapply_allowed field removed)
                        applyButton = `<button class="btn btn-primary" onclick="applyToProgram(${program.id})">Reapply</button>`;
                    } else if (!myApp || myApp.status === 'DROPPED') {
                        applyButton = `<button class="btn btn-primary" onclick="applyToProgram(${program.id})">Apply</button>`;
                    } else if (myApp.status === 'PENDING' || myApp.status === 'APPROVED') {
                        applyButton = `<span class="status-badge">${myApp.status}</span>`;
                    } else {
                        applyButton = `<span class="status-badge">${myApp.status}</span>`;
                    }
                } else {
                    applyButton = `<span class="status-badge" style="background: #999;">Only active programs accept applications</span>`;
                }
            }
            
            return `
            <div class="card">
                <div class="card-header">
                    <h3>${program.title}</h3>
                    ${currentUser.role === 'INCHARGE' ? `
                        <div class="card-actions">
                            <button class="btn btn-sm btn-primary" onclick="viewProgramDetails(${program.id})">View</button>
                            <button class="btn btn-sm btn-secondary" onclick="editProgram(${program.id})">Edit</button>
                        </div>
                    ` : ''}
                </div>
                <div class="card-body">
                    <p><strong>Location:</strong> ${program.location} ${program.locationType ? `(${program.locationType})` : ''}</p>
                    <p><strong>Dates:</strong> ${program.programDates ? program.programDates.map(d => new Date(d).toLocaleDateString()).join(', ') : 'N/A'}</p>
                    <p><strong>Status:</strong> <span class="status-badge">${program.status || 'scheduled'}</span></p>
                    <p><strong>Applications:</strong> ${program.applicationCount || 0}${program.maxSewadars ? ` / Max: ${program.maxSewadars}` : ''}</p>
                    ${applyButton}
                </div>
            </div>
        `;
        }).join('');
    } catch (error) {
        showMessage('Error loading programs: ' + error.message, 'error');
    }
}

async function applyToProgram(programId) {
    if (!confirm('Apply to this program?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/program-applications`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                programId: programId,
                sewadarId: currentUser.zonalId
            })
        });

        if (!response.ok) throw new Error('Failed to apply');
        
        showMessage('Application submitted successfully!');
        loadMyApplications();
    } catch (error) {
        showMessage('Error applying: ' + error.message, 'error');
    }
}

function showProgramForm(programId = null) {
    if (currentUser.role !== 'INCHARGE') {
        showMessage('Only incharge can create programs', 'error');
        return;
    }

    const modal = document.getElementById('program-modal');
    const form = document.getElementById('program-form');
    form.reset();
    document.getElementById('program-id').value = '';
    document.getElementById('program-dates-container').innerHTML = `
        <div class="date-input-group">
            <input type="date" class="program-date-input" required>
            <button type="button" class="btn btn-sm btn-danger" onclick="removeDateInput(this)">Remove</button>
        </div>
    `;

    if (programId) {
        loadProgramForEdit(programId);
    }
    
    modal.style.display = 'block';
}

function addDateInput() {
    const container = document.getElementById('program-dates-container');
    const div = document.createElement('div');
    div.className = 'date-input-group';
    div.innerHTML = `
        <input type="date" class="program-date-input" required>
        <button type="button" class="btn btn-sm btn-danger" onclick="removeDateInput(this)">Remove</button>
    `;
    container.appendChild(div);
}

function removeDateInput(btn) {
    btn.parentElement.remove();
}

async function saveProgram(event) {
    event.preventDefault();
    
    const id = document.getElementById('program-id').value;
    const dates = Array.from(document.querySelectorAll('.program-date-input'))
        .map(input => input.value)
        .filter(date => date);

    if (dates.length === 0) {
        showMessage('Please add at least one program date', 'error');
        return;
    }

    const data = {
        title: document.getElementById('program-title').value,
        description: document.getElementById('program-description').value,
        location: document.getElementById('program-location').value,
        // locationType is derived from location (if location='BEAS' then BEAS, else NON_BEAS)
        status: document.getElementById('program-status').value || 'scheduled',
        programDates: dates,
        maxSewadars: document.getElementById('program-max-sewadars').value ? 
            parseInt(document.getElementById('program-max-sewadars').value) : null,
        createdById: currentUser.zonalId
    };

    try {
        const url = id ? `${API_BASE_URL}/programs/${id}` : `${API_BASE_URL}/programs`;
        const method = id ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: getAuthHeaders(),
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Failed to save program');

        showMessage(id ? 'Program updated!' : 'Program created!');
        closeProgramForm();
        loadPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeProgramForm() {
    document.getElementById('program-modal').style.display = 'none';
}

// Applications
async function loadMyApplications() {
    try {
        if (!currentUser || !currentUser.zonalId) {
            showMessage('User information not available. Please login again.', 'error');
            return;
        }
        const response = await fetch(`${API_BASE_URL}/program-applications/sewadar/${currentUser.zonalId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load applications');
        
        const applications = await response.json();
        const listEl = document.getElementById('applications-list');
        
        if (applications.length === 0) {
            listEl.innerHTML = '<p>No applications found.</p>';
            return;
        }

        listEl.innerHTML = applications.map(app => {
            const statusClass = app.status.toLowerCase().replace('_', '-');
            let actionButton = '';
            
            if (app.status === 'PENDING' || app.status === 'APPROVED') {
                actionButton = `<button class="btn btn-danger" onclick="requestDrop(${app.id})">Request Drop</button>`;
            } else if (app.status === 'DROP_REQUESTED') {
                actionButton = `<span class="status-badge" style="background: orange;">Drop Request Pending</span>`;
            } else if (app.status === 'DROPPED') {
                // Always allow reapply (reapply_allowed field removed)
                actionButton = `<button class="btn btn-primary" onclick="applyToProgram(${app.programId})">Reapply</button>`;
            }
            
            return `
            <div class="card">
                <div class="card-header">
                    <h3>${app.programTitle}</h3>
                    <span class="status-badge ${statusClass}">${app.status}</span>
                </div>
                <div class="card-body">
                    <p><strong>Applied:</strong> ${new Date(app.appliedAt).toLocaleDateString()}</p>
                    ${app.dropRequestedAt ? `<p><strong>Drop Requested:</strong> ${new Date(app.dropRequestedAt).toLocaleDateString()}</p>` : ''}
                    ${app.dropApprovedAt ? `<p><strong>Drop Approved:</strong> ${new Date(app.dropApprovedAt).toLocaleDateString()}</p>` : ''}
                    ${actionButton}
                </div>
            </div>
        `;
        }).join('');
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
    }
}

async function requestDrop(applicationId) {
    if (!confirm('Request to drop from this program? This requires incharge approval.')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/program-applications/${applicationId}/request-drop?sewadarId=${currentUser.zonalId}`, {
            method: 'PUT',
            headers: getAuthHeaders()
        });

        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Failed to request drop' }));
            throw new Error(error.message || 'Failed to request drop');
        }
        
        showMessage('Drop request submitted. Waiting for incharge approval.');
        loadMyApplications();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function requestDropFromSelection(programId) {
    // Find the application for this program
    try {
        const appsResponse = await fetch(`${API_BASE_URL}/program-applications/sewadar/${currentUser.zonalId}`, {
            headers: getAuthHeaders()
        });
        if (!appsResponse.ok) throw new Error('Failed to load applications');
        
        const applications = await appsResponse.json();
        const app = applications.find(a => a.programId === programId);
        
        if (!app) {
            showMessage('Application not found for this program', 'error');
            return;
        }
        
        await requestDrop(app.id);
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

// reapplyToProgram removed - use applyToProgram instead (reapply logic handled by backend)

// Selections and Actions features removed - tables dropped from schema

// Admin Functions
async function loadAdminData() {
    if (currentUser.role !== 'INCHARGE') return;
    
    // Load programs created by this incharge
    try {
        const response = await fetch(`${API_BASE_URL}/programs/incharge/${currentUser.zonalId}`, {
            headers: getAuthHeaders()
        });
        if (response.ok) {
            const programs = await response.json();
            // Display admin data
            document.getElementById('admin-selections').innerHTML = `<p>You have created ${programs.length} program(s)</p>`;
            
            // Load programs list for applications view
            loadAdminProgramsList(programs);
        }
    } catch (error) {
        console.error('Error loading admin data:', error);
    }
}

async function loadAdminProgramsList(programs) {
    const listEl = document.getElementById('admin-programs-list');
    if (!listEl) return;
    
    if (programs.length === 0) {
        listEl.innerHTML = '<p>No programs created yet.</p>';
        return;
    }
    
    // Load drop requests for all programs
    const dropRequestsMap = {};
    for (const program of programs) {
        try {
            const dropResponse = await fetch(`${API_BASE_URL}/program-applications/program/${program.id}/drop-requests`, {
                headers: getAuthHeaders()
            });
            if (dropResponse.ok) {
                dropRequestsMap[program.id] = await dropResponse.json();
            }
        } catch (e) {
            console.error('Error loading drop requests:', e);
        }
    }
    
    listEl.innerHTML = programs.map(program => {
        const dropRequests = dropRequestsMap[program.id] || [];
        const dropRequestsCount = dropRequests.length;
        
        return `
        <div class="card" style="margin-bottom: 15px;">
            <div class="card-header">
                <h4>${program.title}</h4>
                <span class="status-badge">${program.status || 'scheduled'}</span>
            </div>
            <div class="card-body">
                <p><strong>Location:</strong> ${program.location} (${program.locationType || 'NON_BEAS'})</p>
                <p><strong>Applications:</strong> ${program.applicationCount || 0}</p>
                <p><strong>Selected:</strong> ${program.selectionCount || 0}${program.maxSewadars ? ` / ${program.maxSewadars}` : ''}</p>
                ${dropRequestsCount > 0 ? `<p style="color: orange;"><strong>Drop Requests:</strong> ${dropRequestsCount}</p>` : ''}
                <div style="display: flex; gap: 10px; margin-top: 10px;">
                    <button class="btn btn-primary" onclick="viewProgramApplications(${program.id}, '${program.title}')">
                        View & Select Applications
                    </button>
                    ${dropRequestsCount > 0 ? `
                        <button class="btn btn-warning" onclick="viewDropRequests(${program.id}, '${program.title}')">
                            Review Drop Requests (${dropRequestsCount})
                        </button>
                    ` : ''}
                </div>
            </div>
        </div>
    `;
    }).join('');
}

let currentProgramIdForApplications = null;

function viewProgramApplications(programId, programTitle) {
    currentProgramIdForApplications = programId;
    document.getElementById('program-applications-title').textContent = `Applications for: ${programTitle}`;
    document.getElementById('program-applications-modal').style.display = 'block';
    loadPrioritizedApplications();
}

function closeProgramApplicationsModal() {
    document.getElementById('program-applications-modal').style.display = 'none';
    currentProgramIdForApplications = null;
}

async function loadPrioritizedApplications() {
    if (!currentProgramIdForApplications) return;
    
    const listEl = document.getElementById('prioritized-applications-list');
    listEl.innerHTML = '<p>Loading...</p>';
    
    const sortBy = document.getElementById('sort-by-select').value;
    const order = document.getElementById('sort-order-select').value;
    
    try {
        const response = await fetch(
            `${API_BASE_URL}/program-applications/program/${currentProgramIdForApplications}/prioritized?sortBy=${sortBy}&order=${order}`,
            { headers: getAuthHeaders() }
        );
        
        if (!response.ok) throw new Error('Failed to load applications');
        
        const applications = await response.json();
        
        if (applications.length === 0) {
            listEl.innerHTML = '<p>No applications found for this program.</p>';
            return;
        }
        
        listEl.innerHTML = applications.map(app => `
            <div class="card" style="margin-bottom: 15px;">
                <div class="card-header">
                    <h4>${app.sewadar.firstName} ${app.sewadar.lastName}</h4>
                    <span class="status-badge">${app.status}</span>
                </div>
                <div class="card-body">
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 10px;">
                        <div>
                            <p><strong>Mobile:</strong> ${app.sewadar.mobile}</p>
                            <p><strong>Profession:</strong> ${app.profession || 'N/A'}</p>
                            <p><strong>Joining Date:</strong> ${app.joiningDate ? new Date(app.joiningDate).toLocaleDateString() : 'N/A'}</p>
                        </div>
                        <div>
                            <p><strong>Priority Score:</strong> ${app.priorityScore || 0}</p>
                            <p><strong>Total Attendance:</strong> ${app.totalAttendanceCount || 0} programs</p>
                            <p><strong>Total Days:</strong> ${app.totalDaysAttended || 0} days</p>
                        </div>
                    </div>
                    <div style="background: #f5f5f5; padding: 10px; border-radius: 5px; margin-bottom: 10px;">
                        <p><strong>BEAS:</strong> ${app.beasAttendanceCount || 0} programs, ${app.beasDaysAttended || 0} days</p>
                        <p><strong>Non-BEAS:</strong> ${app.nonBeasAttendanceCount || 0} programs, ${app.nonBeasDaysAttended || 0} days</p>
                    </div>
                    <div class="card-actions" style="margin-top: 10px; display: flex; gap: 10px;">
                        ${app.status === 'PENDING' ? `
                            <button class="btn btn-sm btn-success" onclick="approveApplication(${app.id})">Approve</button>
                            <button class="btn btn-sm btn-danger" onclick="rejectApplication(${app.id})">Reject</button>
                        ` : app.status === 'APPROVED' ? `
                            <span class="status-badge" style="background: green;">APPROVED</span>
                        ` : app.status === 'REJECTED' ? `
                            <span class="status-badge" style="background: red;">REJECTED</span>
                        ` : ''}
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
        listEl.innerHTML = '<p>Error loading applications.</p>';
    }
}

// Application approval/rejection functions
async function approveApplication(applicationId) {
    if (!confirm('Are you sure you want to approve this application?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/program-applications/${applicationId}/status?status=APPROVED`, {
            method: 'PUT',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Failed to approve application' }));
            throw new Error(error.message || 'Failed to approve application');
        }
        
        showMessage('Application approved successfully!');
        loadPrioritizedApplications(); // Refresh the list
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function rejectApplication(applicationId) {
    if (!confirm('Are you sure you want to reject this application?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/program-applications/${applicationId}/status?status=REJECTED`, {
            method: 'PUT',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Failed to reject application' }));
            throw new Error(error.message || 'Failed to reject application');
        }
        
        showMessage('Application rejected.');
        loadPrioritizedApplications(); // Refresh the list
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function showAllSewadarsAttendance() {
    const attendanceEl = document.getElementById('admin-attendance');
    attendanceEl.innerHTML = '<p>Loading...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/attendances/all-sewadars/summary`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load attendance');
        
        const data = await response.json();
        
        if (data.sewadars.length === 0) {
            attendanceEl.innerHTML = '<p>No attendance records found.</p>';
            return;
        }
        
        attendanceEl.innerHTML = `
            <h4>All Sewadars Attendance Summary</h4>
            <div style="overflow-x: auto;">
                <table style="width: 100%; border-collapse: collapse;">
                    <thead>
                        <tr style="background: #f5f5f5;">
                            <th style="padding: 10px; border: 1px solid #ddd;">Sewadar</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">BEAS Programs</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">BEAS Days</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">Non-BEAS Programs</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">Non-BEAS Days</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">Total Programs</th>
                            <th style="padding: 10px; border: 1px solid #ddd;">Total Days</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${data.sewadars.map(s => `
                            <tr>
                                <td style="padding: 10px; border: 1px solid #ddd;">${s.sewadarName}</td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${s.beasProgramsCount || 0}</td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${s.beasDaysAttended || 0}</td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${s.nonBeasProgramsCount || 0}</td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;">${s.nonBeasDaysAttended || 0}</td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;"><strong>${s.totalProgramsCount || 0}</strong></td>
                                <td style="padding: 10px; border: 1px solid #ddd; text-align: center;"><strong>${s.totalDaysAttended || 0}</strong></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } catch (error) {
        showMessage('Error loading attendance: ' + error.message, 'error');
        attendanceEl.innerHTML = '<p>Error loading attendance.</p>';
    }
}

function showSewadarAttendanceForm() {
    document.getElementById('sewadar-attendance-modal').style.display = 'block';
    loadSewadarsForAttendance();
}

function closeSewadarAttendanceModal() {
    document.getElementById('sewadar-attendance-modal').style.display = 'none';
}

async function loadSewadarsForAttendance() {
    const selectEl = document.getElementById('sewadar-select-attendance');
    
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load sewadars');
        
        const sewadars = await response.json();
        
        selectEl.innerHTML = '<option value="">-- Select Sewadar --</option>' +
            sewadars.map(s => `<option value="${s.zonalId}">${s.firstName} ${s.lastName} (${s.mobile})</option>`).join('');
    } catch (error) {
        showMessage('Error loading sewadars: ' + error.message, 'error');
    }
}

async function loadSewadarAttendanceSummary() {
    const sewadarId = document.getElementById('sewadar-select-attendance').value;
    const detailsEl = document.getElementById('sewadar-attendance-details');
    
    if (!sewadarId) {
        detailsEl.innerHTML = '';
        return;
    }
    
    detailsEl.innerHTML = '<p>Loading...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/attendances/sewadar/${sewadarId}/summary`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load attendance');
        
        const summary = await response.json();
        
        detailsEl.innerHTML = `
            <h4>${summary.sewadarName} - Attendance Summary</h4>
            <div style="margin: 20px 0;">
                <div style="background: #e3f2fd; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                    <h5>BEAS Locations</h5>
                    <p><strong>Programs:</strong> ${summary.beasProgramsCount || 0}</p>
                    <p><strong>Days Attended:</strong> ${summary.beasDaysAttended || 0}</p>
                    ${summary.beasAttendances && summary.beasAttendances.length > 0 ? `
                        <ul>
                            ${summary.beasAttendances.map(a => `
                                <li>${a.programTitle} (${a.location}) - ${a.daysParticipated || 0} days</li>
                            `).join('')}
                        </ul>
                    ` : '<p>No BEAS attendance records.</p>'}
                </div>
                <div style="background: #fff3e0; padding: 15px; border-radius: 5px; margin-bottom: 15px;">
                    <h5>Non-BEAS Locations</h5>
                    <p><strong>Programs:</strong> ${summary.nonBeasProgramsCount || 0}</p>
                    <p><strong>Days Attended:</strong> ${summary.nonBeasDaysAttended || 0}</p>
                    ${summary.nonBeasAttendances && summary.nonBeasAttendances.length > 0 ? `
                        <ul>
                            ${summary.nonBeasAttendances.map(a => `
                                <li>${a.programTitle} (${a.location}) - ${a.daysParticipated || 0} days</li>
                            `).join('')}
                        </ul>
                    ` : '<p>No non-BEAS attendance records.</p>'}
                </div>
                <div style="background: #f5f5f5; padding: 15px; border-radius: 5px;">
                    <h5>Total</h5>
                    <p><strong>Total Programs:</strong> ${summary.totalProgramsCount || 0}</p>
                    <p><strong>Total Days:</strong> ${summary.totalDaysAttended || 0}</p>
                </div>
            </div>
        `;
    } catch (error) {
        showMessage('Error loading attendance: ' + error.message, 'error');
        detailsEl.innerHTML = '<p>Error loading attendance.</p>';
    }
}

async function viewProgramDetails(programId) {
    try {
        const response = await fetch(`${API_BASE_URL}/programs/${programId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load program');
        
        const program = await response.json();
        
        // Load applications for this program
        const appsResponse = await fetch(`${API_BASE_URL}/program-applications/program/${programId}`, {
            headers: getAuthHeaders()
        });
        const applications = appsResponse.ok ? await appsResponse.json() : [];
        
        // Load selections
        const selResponse = await fetch(`${API_BASE_URL}/program-selections/program/${programId}`, {
            headers: getAuthHeaders()
        });
        const selections = selResponse.ok ? await selResponse.json() : [];
        
        // Show details in a modal or alert
        alert(`Program: ${program.title}\nLocation: ${program.location}\nApplications: ${applications.length}\nSelected: ${selections.length}`);
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function editProgram(programId) {
    showProgramForm(programId);
}

async function loadProgramForEdit(programId) {
    try {
        const response = await fetch(`${API_BASE_URL}/programs/${programId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load program');
        
        const program = await response.json();
        document.getElementById('program-id').value = program.id;
        document.getElementById('program-title').value = program.title;
        document.getElementById('program-description').value = program.description || '';
        document.getElementById('program-location').value = program.location;
        document.getElementById('program-status').value = program.status || 'scheduled';
        document.getElementById('program-max-sewadars').value = program.maxSewadars || '';
        
        // Set dates
        const container = document.getElementById('program-dates-container');
        container.innerHTML = '';
        if (program.programDates && program.programDates.length > 0) {
            program.programDates.forEach(date => {
                const div = document.createElement('div');
                div.className = 'date-input-group';
                div.innerHTML = `
                    <input type="date" class="program-date-input" value="${date}" required>
                    <button type="button" class="btn btn-sm btn-danger" onclick="removeDateInput(this)">Remove</button>
                `;
                container.appendChild(div);
            });
        } else {
            addDateInput();
        }
        
        document.getElementById('program-form-title').textContent = 'Edit Program';
    } catch (error) {
        showMessage('Error loading program: ' + error.message, 'error');
    }
}

async function loadAllPrograms() {
    loadPrograms();
}

// Sewadar Management Functions
function showSewadarForm(sewadarId = null) {
    document.getElementById('sewadar-form').reset();
    document.getElementById('sewadar-id').value = '';
    document.getElementById('sewadar-form-title').textContent = sewadarId ? 'Edit Sewadar' : 'Add Sewadar';
    
    // Reset languages container
    const languagesContainer = document.getElementById('languages-container');
    languagesContainer.innerHTML = '<input type="text" class="language-input" placeholder="e.g., Hindi, English" style="margin-bottom: 5px;">';
    
    if (sewadarId) {
        loadSewadarForEdit(sewadarId);
    }
    
    document.getElementById('sewadar-modal').style.display = 'block';
}

function addLanguageInput() {
    const container = document.getElementById('languages-container');
    const input = document.createElement('input');
    input.type = 'text';
    input.className = 'language-input';
    input.placeholder = 'e.g., Hindi, English';
    input.style.marginBottom = '5px';
    container.appendChild(input);
}

function closeSewadarForm() {
    document.getElementById('sewadar-modal').style.display = 'none';
}

async function loadSewadarForEdit(sewadarId) {
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${sewadarId}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load sewadar');
        
        const sewadar = await response.json();
        document.getElementById('sewadar-id').value = sewadar.zonalId;
        document.getElementById('sewadar-first-name').value = sewadar.firstName || '';
        document.getElementById('sewadar-last-name').value = sewadar.lastName || '';
        document.getElementById('sewadar-mobile').value = sewadar.mobile || '';
        document.getElementById('sewadar-location').value = sewadar.location || '';
        document.getElementById('sewadar-profession').value = sewadar.profession || '';
        document.getElementById('sewadar-date-of-birth').value = sewadar.dateOfBirth || '';
        document.getElementById('sewadar-joining-date').value = sewadar.joiningDate || '';
        document.getElementById('sewadar-emergency-contact').value = sewadar.emergencyContact || '';
        document.getElementById('sewadar-emergency-relationship').value = sewadar.emergencyContactRelationship || '';
        document.getElementById('sewadar-photo-url').value = sewadar.photoUrl || '';
        document.getElementById('sewadar-remarks').value = sewadar.remarks || '';
        
        // Load languages
        const languagesContainer = document.getElementById('languages-container');
        if (sewadar.languages && sewadar.languages.length > 0) {
            languagesContainer.innerHTML = sewadar.languages.map(lang => 
                `<input type="text" class="language-input" value="${lang}" style="margin-bottom: 5px;">`
            ).join('');
        } else {
            languagesContainer.innerHTML = '<input type="text" class="language-input" placeholder="e.g., Hindi, English" style="margin-bottom: 5px;">';
        }
        
        if (sewadar.address) {
            document.getElementById('sewadar-address1').value = sewadar.address.address1 || '';
            document.getElementById('sewadar-address2').value = sewadar.address.address2 || '';
            document.getElementById('sewadar-email').value = sewadar.address.email || '';
        }
        
        // Don't load password for edit
        document.getElementById('sewadar-password').required = false;
        document.getElementById('sewadar-password').placeholder = 'Leave blank to keep current password';
    } catch (error) {
        showMessage('Error loading sewadar: ' + error.message, 'error');
    }
}

async function saveSewadar(event) {
    event.preventDefault();
    
    const sewadarId = document.getElementById('sewadar-id').value;
    
    // Collect languages from inputs
    const languageInputs = Array.from(document.querySelectorAll('.language-input'))
        .map(input => input.value.trim())
        .filter(lang => lang.length > 0);
    
    // Split comma-separated languages and combine
    const languages = [];
    languageInputs.forEach(input => {
        const langs = input.split(',').map(l => l.trim()).filter(l => l.length > 0);
        languages.push(...langs);
    });
    
    const data = {
        firstName: document.getElementById('sewadar-first-name').value,
        lastName: document.getElementById('sewadar-last-name').value,
        mobile: document.getElementById('sewadar-mobile').value,
        location: document.getElementById('sewadar-location').value,
        profession: document.getElementById('sewadar-profession').value,
        dateOfBirth: document.getElementById('sewadar-date-of-birth').value || null,
        joiningDate: document.getElementById('sewadar-joining-date').value || null,
        emergencyContact: document.getElementById('sewadar-emergency-contact').value || null,
        emergencyContactRelationship: document.getElementById('sewadar-emergency-relationship').value || null,
        photoUrl: document.getElementById('sewadar-photo-url').value || null,
        languages: languages.length > 0 ? languages : null,
        remarks: document.getElementById('sewadar-remarks').value,
        address1: document.getElementById('sewadar-address1').value,
        address2: document.getElementById('sewadar-address2').value,
        email: document.getElementById('sewadar-email').value
    };
    
    // Only include password if provided (for new sewadars or password change)
    const password = document.getElementById('sewadar-password').value;
    if (password) {
        data.password = password;
    }
    
    try {
        const url = sewadarId 
            ? `${API_BASE_URL}/sewadars/${sewadarId}`
            : `${API_BASE_URL}/sewadars`;
        const method = sewadarId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: getAuthHeaders(),
            body: JSON.stringify(data)
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save sewadar');
        }
        
        showMessage(sewadarId ? 'Sewadar updated successfully!' : 'Sewadar created successfully!');
        closeSewadarForm();
        loadAllSewadars();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function loadAllSewadars() {
    const listEl = document.getElementById('sewadars-list');
    if (!listEl) {
        console.error('sewadars-list element not found!');
        return;
    }
    
    listEl.innerHTML = '<p>Loading sewadars...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            if (response.status === 401) {
                showMessage('Session expired. Please login again.', 'error');
                logout();
                return;
            }
            throw new Error('Failed to load sewadars');
        }
        
        const sewadars = await response.json();
        
        if (sewadars.length === 0) {
            listEl.innerHTML = '<p>No sewadars found. Click "+ Add Sewadar" to create one.</p>';
            return;
        }
        
        listEl.innerHTML = sewadars.map(sewadar => {
            // Escape quotes in names to prevent XSS
            const firstName = (sewadar.firstName || '').replace(/'/g, "\\'");
            const lastName = (sewadar.lastName || '').replace(/'/g, "\\'");
            const fullName = `${firstName} ${lastName}`;
            
            return `
            <div class="card" style="margin-bottom: 15px;">
                <div class="card-header">
                    <h3>${sewadar.firstName} ${sewadar.lastName}</h3>
                    <span class="role-badge ${sewadar.role ? sewadar.role.toLowerCase() : 'sewadar'}">${sewadar.role || 'SEWADAR'}</span>
                </div>
                <div class="card-body">
                    <p><strong>Zonal ID:</strong> ${sewadar.zonalId || 'N/A'}</p>
                    <p><strong>Mobile:</strong> ${sewadar.mobile || 'N/A'}</p>
                    <p><strong>Location:</strong> ${sewadar.location || 'N/A'}</p>
                    <p><strong>Profession:</strong> ${sewadar.profession || 'N/A'}</p>
                    ${sewadar.dateOfBirth ? `<p><strong>Date of Birth:</strong> ${new Date(sewadar.dateOfBirth).toLocaleDateString()}</p>` : ''}
                    ${sewadar.joiningDate ? `<p><strong>Joining Date:</strong> ${new Date(sewadar.joiningDate).toLocaleDateString()}</p>` : ''}
                    ${sewadar.emergencyContact ? `<p><strong>Emergency Contact:</strong> ${sewadar.emergencyContact} (${sewadar.emergencyContactRelationship || 'N/A'})</p>` : ''}
                    ${sewadar.languages && sewadar.languages.length > 0 ? `<p><strong>Languages:</strong> ${sewadar.languages.join(', ')}</p>` : ''}
                    ${sewadar.address ? `<p><strong>Email:</strong> ${sewadar.address.email || 'N/A'}</p>` : ''}
                    <div class="card-actions" style="margin-top: 15px; display: flex; gap: 10px;">
                        <button class="btn btn-sm btn-primary" onclick="showSewadarForm(${sewadar.zonalId})">Edit</button>
                        ${sewadar.role !== 'INCHARGE' ? `<button class="btn btn-sm btn-success" onclick="promoteSewadar(${sewadar.zonalId}, '${fullName}')">Promote to Incharge</button>` : '<span class="role-badge incharge">Already Incharge</span>'}
                    </div>
                </div>
            </div>
        `;
        }).join('');
    } catch (error) {
        console.error('Error loading sewadars:', error);
        showMessage('Error loading sewadars: ' + error.message, 'error');
        listEl.innerHTML = '<p style="color: red;">Error loading sewadars. Please try again.</p>';
    }
}

async function promoteSewadar(sewadarId, sewadarName) {
    if (!confirm(`Are you sure you want to promote "${sewadarName}" to Incharge?`)) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${sewadarId}/promote?inchargeId=${currentUser.zonalId}`, {
            method: 'POST',
            headers: getAuthHeaders()
        });
        
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to promote sewadar');
        }
        
        showMessage('Sewadar promoted to Incharge successfully!');
        loadAllSewadars();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function loadInitialData() {
    loadPrograms();
    if (currentUser && currentUser.role === 'INCHARGE') {
        loadAllSewadars();
    }
}

// Initialize
checkAuth();

// Drop Request Functions
let currentProgramIdForDropRequests = null;

function viewDropRequests(programId, programTitle) {
    currentProgramIdForDropRequests = programId;
    document.getElementById('drop-requests-title').textContent = `Drop Requests for: ${programTitle}`;
    document.getElementById('drop-requests-modal').style.display = 'block';
    loadDropRequests();
}

function closeDropRequestsModal() {
    document.getElementById('drop-requests-modal').style.display = 'none';
    currentProgramIdForDropRequests = null;
}

async function loadDropRequests() {
    if (!currentProgramIdForDropRequests) return;
    
    const listEl = document.getElementById('drop-requests-list');
    listEl.innerHTML = '<p>Loading...</p>';
    
    try {
        const response = await fetch(`${API_BASE_URL}/program-applications/program/${currentProgramIdForDropRequests}/drop-requests`, {
            headers: getAuthHeaders()
        });
        
        if (!response.ok) throw new Error('Failed to load drop requests');
        
        const dropRequests = await response.json();
        
        if (dropRequests.length === 0) {
            listEl.innerHTML = '<p>No drop requests pending.</p>';
            return;
        }
        
        listEl.innerHTML = dropRequests.map(req => `
            <div class="card" style="margin-bottom: 15px;">
                <div class="card-header">
                    <h4>${req.sewadar.firstName} ${req.sewadar.lastName}</h4>
                    <span class="status-badge" style="background: orange;">DROP_REQUESTED</span>
                </div>
                <div class="card-body">
                    <p><strong>Mobile:</strong> ${req.sewadar.mobile}</p>
                    <p><strong>Applied:</strong> ${new Date(req.appliedAt).toLocaleDateString()}</p>
                    <p><strong>Drop Requested:</strong> ${req.dropRequestedAt ? new Date(req.dropRequestedAt).toLocaleDateString() : 'N/A'}</p>
                    <div class="card-actions" style="margin-top: 15px; display: flex; gap: 10px;">
                        <button class="btn btn-success" onclick="approveDropRequest(${req.id}, true)">
                            Approve Drop Request
                        </button>
                        <small style="color: #666; margin-left: 10px;">Note: Reapply is always allowed</small>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading drop requests: ' + error.message, 'error');
        listEl.innerHTML = '<p>Error loading drop requests.</p>';
    }
}

async function approveDropRequest(applicationId, allowReapply) {
    try {
        const response = await fetch(
            `${API_BASE_URL}/program-applications/${applicationId}/approve-drop?inchargeId=${currentUser.zonalId}&allowReapply=${allowReapply}`,
            {
                method: 'PUT',
                headers: getAuthHeaders()
            }
        );
        
        if (!response.ok) {
            const error = await response.json().catch(() => ({ message: 'Failed to approve drop request' }));
            throw new Error(error.message || 'Failed to approve drop request');
        }
        
        showMessage(`Drop request approved. ${allowReapply ? 'Reapply allowed.' : 'Reapply not allowed.'}`);
        loadDropRequests();
        loadAdminProgramsList(await getAdminPrograms());
        loadPrioritizedApplications();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

// rejectDropRequest removed - incharge can only approve drop requests

async function getAdminPrograms() {
    const response = await fetch(`${API_BASE_URL}/programs/incharge/${currentUser.zonalId}`, {
        headers: getAuthHeaders()
    });
    return response.ok ? await response.json() : [];
}

// Close modals on outside click
window.onclick = function(event) {
    const modals = ['program-modal', 'action-response-modal', 'sewadar-modal', 'program-applications-modal', 'sewadar-attendance-modal', 'drop-requests-modal'];
    modals.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

