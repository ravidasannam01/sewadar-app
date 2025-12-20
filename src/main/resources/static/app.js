// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

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
    const mobile = document.getElementById('login-mobile').value;
    const password = document.getElementById('login-password').value;

    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mobile, password })
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
    } else if (tabName === 'selections') {
        loadMySelections();
    } else if (tabName === 'actions') {
        loadMyActions();
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

        listEl.innerHTML = programs.map(program => `
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
                    <p><strong>Location:</strong> ${program.location}</p>
                    <p><strong>Dates:</strong> ${program.programDates ? program.programDates.join(', ') : 'N/A'}</p>
                    <p><strong>Status:</strong> ${program.status}</p>
                    <p><strong>Applications:</strong> ${program.applicationCount || 0}</p>
                    <p><strong>Selected:</strong> ${program.selectionCount || 0}</p>
                    ${currentUser.role === 'SEWADAR' ? `
                        <button class="btn btn-primary" onclick="applyToProgram(${program.id})">Apply</button>
                    ` : ''}
                </div>
            </div>
        `).join('');
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
                sewadarId: currentUser.id
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
        locationType: document.getElementById('program-location-type').value,
        programDates: dates,
        maxSewadars: document.getElementById('program-max-sewadars').value ? 
            parseInt(document.getElementById('program-max-sewadars').value) : null,
        createdById: currentUser.id
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
        const response = await fetch(`${API_BASE_URL}/program-applications/sewadar/${currentUser.id}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load applications');
        
        const applications = await response.json();
        const listEl = document.getElementById('applications-list');
        
        if (applications.length === 0) {
            listEl.innerHTML = '<p>No applications found.</p>';
            return;
        }

        listEl.innerHTML = applications.map(app => `
            <div class="card">
                <div class="card-header">
                    <h3>${app.programTitle}</h3>
                    <span class="status-badge ${app.status.toLowerCase()}">${app.status}</span>
                </div>
                <div class="card-body">
                    <p><strong>Applied:</strong> ${new Date(app.appliedAt).toLocaleDateString()}</p>
                    ${app.status === 'PENDING' ? `
                        <button class="btn btn-danger" onclick="dropConsent(${app.id})">Drop Consent</button>
                    ` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
    }
}

async function dropConsent(applicationId) {
    if (!confirm('Drop consent for this program?')) return;

    try {
        const response = await fetch(`${API_BASE_URL}/program-applications/${applicationId}/drop`, {
            method: 'PUT',
            headers: getAuthHeaders()
        });

        if (!response.ok) throw new Error('Failed to drop consent');
        
        showMessage('Consent dropped successfully');
        loadMyApplications();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

// Selections
async function loadMySelections() {
    try {
        const response = await fetch(`${API_BASE_URL}/program-selections/sewadar/${currentUser.id}`, {
            headers: getAuthHeaders()
        });
        if (!response.ok) throw new Error('Failed to load selections');
        
        const selections = await response.json();
        const listEl = document.getElementById('selections-list');
        
        if (selections.length === 0) {
            listEl.innerHTML = '<p>You have not been selected for any programs yet.</p>';
            return;
        }

        listEl.innerHTML = selections.map(sel => `
            <div class="card">
                <div class="card-header">
                    <h3>${sel.programTitle}</h3>
                    <span class="status-badge ${sel.status.toLowerCase()}">${sel.status}</span>
                </div>
                <div class="card-body">
                    <p><strong>Selected on:</strong> ${new Date(sel.selectedAt).toLocaleDateString()}</p>
                    <p><strong>Status:</strong> ${sel.status}</p>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading selections: ' + error.message, 'error');
    }
}

// Actions
async function loadMyActions() {
    try {
        // Get all programs where user is selected
        const selectionsResponse = await fetch(`${API_BASE_URL}/program-selections/sewadar/${currentUser.id}`, {
            headers: getAuthHeaders()
        });
        if (!selectionsResponse.ok) throw new Error('Failed to load selections');
        
        const selections = await selectionsResponse.json();
        const listEl = document.getElementById('actions-list');
        
        if (selections.length === 0) {
            listEl.innerHTML = '<p>No pending actions.</p>';
            return;
        }

        // Get actions for all selected programs
        let allActions = [];
        for (const sel of selections) {
            try {
                const actionsResponse = await fetch(`${API_BASE_URL}/actions/program/${sel.programId}/sewadar/${currentUser.id}`, {
                    headers: getAuthHeaders()
                });
                if (actionsResponse.ok) {
                    const actions = await actionsResponse.json();
                    allActions = allActions.concat(actions);
                }
            } catch (e) {
                console.error('Error loading actions for program:', sel.programId);
            }
        }

        if (allActions.length === 0) {
            listEl.innerHTML = '<p>No pending actions.</p>';
            return;
        }

        listEl.innerHTML = allActions.map(action => `
            <div class="card">
                <div class="card-header">
                    <h3>${action.title}</h3>
                    <span class="status-badge">${action.status}</span>
                </div>
                <div class="card-body">
                    <p><strong>Program:</strong> ${action.programTitle}</p>
                    <p><strong>Description:</strong> ${action.description || 'N/A'}</p>
                    <p><strong>Due Date:</strong> ${action.dueDate ? new Date(action.dueDate).toLocaleDateString() : 'N/A'}</p>
                    <button class="btn btn-primary" onclick="showActionResponseForm(${action.id}, '${action.title}')">Respond</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading actions: ' + error.message, 'error');
    }
}

function showActionResponseForm(actionId, actionTitle) {
    document.getElementById('action-response-action-id').value = actionId;
    document.getElementById('action-response-title').textContent = `Respond to: ${actionTitle}`;
    document.getElementById('action-response-form').reset();
    document.getElementById('action-response-modal').style.display = 'block';
}

async function submitActionResponse(event) {
    event.preventDefault();
    
    const actionId = document.getElementById('action-response-action-id').value;
    const data = {
        actionId: parseInt(actionId),
        sewadarId: currentUser.id,
        responseData: document.getElementById('action-response-data').value,
        notes: document.getElementById('action-response-notes').value
    };

    try {
        const response = await fetch(`${API_BASE_URL}/action-responses`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify(data)
        });

        if (!response.ok) throw new Error('Failed to submit response');

        showMessage('Response submitted successfully!');
        closeActionResponseForm();
        loadMyActions();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeActionResponseForm() {
    document.getElementById('action-response-modal').style.display = 'none';
}

// Admin Functions
async function loadAdminData() {
    if (currentUser.role !== 'INCHARGE') return;
    
    // Load programs created by this incharge
    try {
        const response = await fetch(`${API_BASE_URL}/programs/incharge/${currentUser.id}`, {
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
    
    listEl.innerHTML = programs.map(program => `
        <div class="card" style="margin-bottom: 15px;">
            <div class="card-header">
                <h4>${program.title}</h4>
                <span class="status-badge">${program.status || 'UPCOMING'}</span>
            </div>
            <div class="card-body">
                <p><strong>Location:</strong> ${program.location} (${program.locationType || 'NON_BEAS'})</p>
                <p><strong>Applications:</strong> ${program.applicationCount || 0}</p>
                <p><strong>Selected:</strong> ${program.selectionCount || 0}</p>
                <button class="btn btn-primary" onclick="viewProgramApplications(${program.id}, '${program.title}')">
                    View & Select Applications
                </button>
            </div>
        </div>
    `).join('');
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
                    <div class="card-actions">
                        <button class="btn btn-sm btn-success" onclick="selectApplication(${app.id}, ${app.sewadar.id}, ${currentProgramIdForApplications})">
                            Select
                        </button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
        listEl.innerHTML = '<p>Error loading applications.</p>';
    }
}

async function selectApplication(applicationId, sewadarId, programId) {
    if (!confirm('Are you sure you want to select this sewadar for the program?')) {
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE_URL}/program-selections`, {
            method: 'POST',
            headers: getAuthHeaders(),
            body: JSON.stringify({
                programId: programId,
                sewadarIds: [sewadarId], // Backend expects array
                selectedById: currentUser.id // Required field
            })
        });
        
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: 'Failed to select sewadar' }));
            throw new Error(errorData.message || 'Failed to select sewadar');
        }
        
        showMessage('Sewadar selected successfully!');
        loadPrioritizedApplications();
        // Refresh admin data to update selection counts
        loadAdminData();
    } catch (error) {
        console.error('Error selecting sewadar:', error);
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
            sewadars.map(s => `<option value="${s.id}">${s.firstName} ${s.lastName} (${s.mobile})</option>`).join('');
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
        document.getElementById('program-location-type').value = program.locationType || 'NON_BEAS';
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
    
    if (sewadarId) {
        loadSewadarForEdit(sewadarId);
    }
    
    document.getElementById('sewadar-modal').style.display = 'block';
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
        document.getElementById('sewadar-id').value = sewadar.id;
        document.getElementById('sewadar-first-name').value = sewadar.firstName || '';
        document.getElementById('sewadar-last-name').value = sewadar.lastName || '';
        document.getElementById('sewadar-mobile').value = sewadar.mobile || '';
        document.getElementById('sewadar-dept').value = sewadar.dept || '';
        document.getElementById('sewadar-profession').value = sewadar.profession || '';
        document.getElementById('sewadar-joining-date').value = sewadar.joiningDate || '';
        document.getElementById('sewadar-remarks').value = sewadar.remarks || '';
        
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
    const data = {
        firstName: document.getElementById('sewadar-first-name').value,
        lastName: document.getElementById('sewadar-last-name').value,
        mobile: document.getElementById('sewadar-mobile').value,
        dept: document.getElementById('sewadar-dept').value,
        profession: document.getElementById('sewadar-profession').value,
        joiningDate: document.getElementById('sewadar-joining-date').value || null,
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
                    <p><strong>Mobile:</strong> ${sewadar.mobile || 'N/A'}</p>
                    <p><strong>Department:</strong> ${sewadar.dept || 'N/A'}</p>
                    <p><strong>Profession:</strong> ${sewadar.profession || 'N/A'}</p>
                    ${sewadar.joiningDate ? `<p><strong>Joining Date:</strong> ${new Date(sewadar.joiningDate).toLocaleDateString()}</p>` : ''}
                    ${sewadar.address ? `<p><strong>Email:</strong> ${sewadar.address.email || 'N/A'}</p>` : ''}
                    <div class="card-actions" style="margin-top: 15px; display: flex; gap: 10px;">
                        <button class="btn btn-sm btn-primary" onclick="showSewadarForm(${sewadar.id})">Edit</button>
                        ${sewadar.role !== 'INCHARGE' ? `<button class="btn btn-sm btn-success" onclick="promoteSewadar(${sewadar.id}, '${fullName}')">Promote to Incharge</button>` : '<span class="role-badge incharge">Already Incharge</span>'}
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
        const response = await fetch(`${API_BASE_URL}/sewadars/${sewadarId}/promote?inchargeId=${currentUser.id}`, {
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

// Close modals on outside click
window.onclick = function(event) {
    const modals = ['program-modal', 'action-response-modal', 'sewadar-modal', 'program-applications-modal', 'sewadar-attendance-modal'];
    modals.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

