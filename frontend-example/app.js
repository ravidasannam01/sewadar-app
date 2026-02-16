// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Global state
let authToken = null;
let currentUser = null;
let currentSection = 'programs';

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
    checkAuth();
    setupEventListeners();
});

// Check if user is authenticated
function checkAuth() {
    authToken = localStorage.getItem('authToken');
    const userData = localStorage.getItem('currentUser');
    
    if (authToken && userData) {
        try {
            currentUser = JSON.parse(userData);
            showApp();
        } catch (e) {
            localStorage.removeItem('authToken');
            localStorage.removeItem('currentUser');
            showLogin();
        }
    } else {
        showLogin();
    }
}

// Show login page
function showLogin() {
    document.getElementById('login-page').classList.add('active');
    document.getElementById('app-page').classList.remove('active');
}

// Show main app
function showApp() {
    document.getElementById('login-page').classList.remove('active');
    document.getElementById('app-page').classList.add('active');
    setupNavigation();
    loadInitialData();
}

// Setup event listeners
function setupEventListeners() {
    // Program search and filter
    const programSearch = document.getElementById('program-search');
    const programStatusFilter = document.getElementById('program-status-filter');
    if (programSearch) {
        programSearch.addEventListener('input', () => loadPrograms());
    }
    if (programStatusFilter) {
        programStatusFilter.addEventListener('change', () => loadPrograms());
    }
}

// Setup navigation based on user role
function setupNavigation() {
    const nav = document.getElementById('main-nav');
    if (!nav) return;
    
    nav.innerHTML = '';
    
    // Common navigation items
    const navItems = [
        { id: 'programs', label: 'Programs', icon: '📅' },
    ];
    
    if (currentUser?.role === 'ADMIN' || currentUser?.role === 'INCHARGE') {
        navItems.push(
            { id: 'admin', label: 'Admin', icon: '⚙️' },
            { id: 'attendance', label: 'Attendance', icon: '✅' },
            { id: 'dashboard', label: 'Dashboard', icon: '📊' },
            { id: 'workflow', label: 'Workflow', icon: '🔄' }
        );
    } else {
        navItems.push(
            { id: 'pending-actions', label: 'Pending Actions', icon: '📝' }
        );
    }
    
    navItems.forEach(item => {
        const btn = document.createElement('button');
        btn.className = 'nav-btn';
        btn.innerHTML = `${item.icon} ${item.label}`;
        btn.onclick = () => showSection(item.id);
        nav.appendChild(btn);
    });
    
    // Update user info
    const userName = document.getElementById('user-name');
    const userRole = document.getElementById('user-role');
    if (userName) userName.textContent = `${currentUser.firstName} ${currentUser.lastName}`;
    if (userRole) {
        userRole.textContent = currentUser.role;
        userRole.className = `role-badge ${currentUser.role.toLowerCase()}`;
    }
    
    // Show/hide create program button
    const createProgramBtn = document.getElementById('create-program-btn');
    if (createProgramBtn) {
        createProgramBtn.style.display = (currentUser?.role === 'ADMIN' || currentUser?.role === 'INCHARGE') ? 'block' : 'none';
    }
}

// Show section
function showSection(sectionId) {
    currentSection = sectionId;
    
    // Hide all sections
    document.querySelectorAll('.section').forEach(s => s.classList.remove('active'));
    
    // Show selected section
    const section = document.getElementById(`${sectionId}-section`);
    if (section) {
        section.classList.add('active');
    }
    
    // Load section data
    switch(sectionId) {
        case 'programs':
            loadPrograms();
            break;
        case 'admin':
            loadAdminPrograms();
            loadAdminSewadars();
            break;
        case 'attendance':
            loadAttendancePrograms();
            break;
        case 'dashboard':
            // Dashboard loads on filter apply
            break;
        case 'workflow':
            loadWorkflowPrograms();
            break;
        case 'pending-actions':
            loadPendingActions();
            break;
    }
}

// Load initial data
function loadInitialData() {
    showSection('programs');
}

// ==================== AUTHENTICATION ====================

async function handleLogin(event) {
    event.preventDefault();
    const zonalId = document.getElementById('login-zonalId').value;
    const password = document.getElementById('login-password').value;
    const loginBtn = document.getElementById('login-btn');
    const errorDiv = document.getElementById('login-error');
    
    loginBtn.disabled = true;
    loginBtn.textContent = 'Logging in...';
    errorDiv.style.display = 'none';
    
    try {
        const response = await fetch(`${API_BASE_URL}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ zonalId, password })
        });
        
        const data = await response.json();
        
        if (!response.ok) {
            throw new Error(data.message || 'Login failed');
        }
        
        if (data.token && data.sewadar) {
            authToken = data.token;
            currentUser = data.sewadar;
            localStorage.setItem('authToken', authToken);
            localStorage.setItem('currentUser', JSON.stringify(currentUser));
            showApp();
        } else {
            throw new Error('Invalid response from server');
        }
    } catch (error) {
        errorDiv.textContent = error.message || 'Login failed. Please check your credentials.';
        errorDiv.style.display = 'block';
    } finally {
        loginBtn.disabled = false;
        loginBtn.textContent = 'Login';
    }
}

function handleLogout() {
    if (confirm('Are you sure you want to logout?')) {
        authToken = null;
        currentUser = null;
        localStorage.removeItem('authToken');
        localStorage.removeItem('currentUser');
        showLogin();
    }
}

// ==================== API HELPERS ====================

async function apiCall(endpoint, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    
    if (authToken) {
        headers['Authorization'] = `Bearer ${authToken}`;
    }
    
    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
        ...options,
        headers
    });
    
    if (response.status === 401) {
        handleLogout();
        throw new Error('Session expired. Please login again.');
    }
    
    if (!response.ok) {
        const error = await response.json().catch(() => ({ message: 'Request failed' }));
        throw new Error(error.message || 'Request failed');
    }
    
    return response.json();
}

// ==================== UTILITY FUNCTIONS ====================

function showMessage(text, type = 'success') {
    const messageEl = document.getElementById('message');
    messageEl.textContent = text;
    messageEl.className = `message ${type}`;
    messageEl.style.display = 'block';
    setTimeout(() => {
        messageEl.style.display = 'none';
    }, 3000);
}

function formatDate(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(dateString) {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString('en-US', { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// ==================== APPLICANTS MODAL ====================

let currentProgramForApplicants = null;

async function showApplicants(programId, programTitle) {
    currentProgramForApplicants = { id: programId, title: programTitle };
    const content = document.getElementById('applicants-modal-content');
    const title = document.getElementById('applicants-modal-title');
    
    title.textContent = `Applicants - ${programTitle}`;
    content.innerHTML = '<p>Loading...</p>';
    document.getElementById('applicants-modal').style.display = 'block';
    
    try {
        const applications = await apiCall(`/program-applications/program/${programId}`);
        
        if (applications.length === 0) {
            content.innerHTML = '<p class="info-message">No applicants found for this program.</p>';
            return;
        }
        
        content.innerHTML = `
            <p style="margin-bottom: 15px; color: #666;"><strong>Total:</strong> ${applications.length} applicant(s)</p>
            <div style="max-height: 400px; overflow-y: auto;">
                ${applications.map(app => `
                    <div style="padding: 12px; border: 1px solid #ddd; border-radius: 5px; margin-bottom: 10px; display: flex; justify-content: space-between; align-items: center;">
                        <div>
                            <div style="font-weight: 600; margin-bottom: 4px;">
                                ${app.sewadar?.firstName || ''} ${app.sewadar?.lastName || ''}
                            </div>
                            <div style="font-size: 12px; color: #666;">
                                ${app.sewadar?.zonalId || ''} ${app.sewadar?.mobile ? `• ${app.sewadar.mobile}` : ''}
                            </div>
                        </div>
                        <span class="status-badge ${app.status}">${app.status}</span>
                    </div>
                `).join('')}
            </div>
        `;
    } catch (error) {
        content.innerHTML = `<p class="error-message">Error loading applicants: ${error.message}</p>`;
    }
}

function closeApplicantsModal() {
    document.getElementById('applicants-modal').style.display = 'none';
    currentProgramForApplicants = null;
}

// ==================== PROGRAMS ====================

async function loadPrograms() {
    try {
        const programs = await apiCall('/programs');
        const searchTerm = document.getElementById('program-search')?.value.toLowerCase() || '';
        const statusFilter = document.getElementById('program-status-filter')?.value || 'all';
        
        let filtered = programs;
        
        // Filter for SEWADAR: only active programs
        // INCHARGE and ADMIN can also load their applications (they can apply to programs too)
        if (currentUser?.role === 'SEWADAR' || currentUser?.role === 'INCHARGE' || currentUser?.role === 'ADMIN') {
            filtered = filtered.filter(p => {
                if (p.status !== 'active') return false;
                // Check if last date is in the future
                if (p.programDates && p.programDates.length > 0) {
                    const lastDate = new Date(Math.max(...p.programDates.map(d => new Date(d))));
                    const today = new Date();
                    today.setHours(0, 0, 0, 0);
                    lastDate.setHours(0, 0, 0, 0);
                    return lastDate >= today;
                }
                return true;
            });
        }
        
        // Apply search and status filters
        filtered = filtered.filter(p => {
            const matchesSearch = !searchTerm || 
                p.title?.toLowerCase().includes(searchTerm) ||
                p.location?.toLowerCase().includes(searchTerm);
            const matchesStatus = statusFilter === 'all' || p.status === statusFilter;
            return matchesSearch && matchesStatus;
        });
        
        const listEl = document.getElementById('programs-list');
        if (!listEl) return;
        
        if (filtered.length === 0) {
            listEl.innerHTML = '<p class="info-message">No programs found.</p>';
            return;
        }
        
        // Load my applications - SEWADAR, INCHARGE, and ADMIN can all apply to programs
        // INCHARGE = SEWADAR + additional permissions, so they can do everything sewadars can do
        let myApplications = [];
        if (currentUser?.role === 'SEWADAR' || currentUser?.role === 'INCHARGE' || currentUser?.role === 'ADMIN') {
            try {
                myApplications = await apiCall(`/program-applications/sewadar/${currentUser.zonalId}`);
            } catch (e) {
                console.error('Error loading applications:', e);
            }
        }
        
        listEl.innerHTML = filtered.map(program => {
            const app = myApplications.find(a => a.programId === program.id);
            const appStatus = app?.status;
            
            return `
                <div class="card">
                    <div class="card-header">
                        <h3>${program.title}</h3>
                        ${(currentUser?.role === 'ADMIN' || currentUser?.role === 'INCHARGE') ? `
                            <button class="btn btn-sm btn-primary" onclick="showProgramForm(${program.id})">Edit</button>
                        ` : ''}
                    </div>
                    <div class="card-body">
                        <p><strong>Status:</strong> <span class="status-badge ${program.status}">${program.status}</span></p>
                        <p><strong>Location:</strong> ${program.location || 'N/A'} ${program.locationType ? `(${program.locationType})` : ''}</p>
                        <p><strong>Dates:</strong> ${program.programDates?.map(d => formatDate(d)).join(', ') || 'N/A'}</p>
                        <p><strong>Applications:</strong> <span class="clickable-link" onclick="showApplicants(${program.id}, ${JSON.stringify(program.title || '')})" style="cursor: pointer; color: #667eea; text-decoration: underline;">${program.applicationCount || 0}${program.maxSewadars ? ` / ${program.maxSewadars}` : ''}</span></p>
                        ${app ? `<p><strong>Applied:</strong> ${formatDate(app.appliedAt)}</p>` : ''}
                        ${(currentUser?.role === 'SEWADAR' || currentUser?.role === 'INCHARGE' || currentUser?.role === 'ADMIN') && program.status === 'active' ? `
                            <div class="card-actions">
                                ${appStatus === 'DROPPED' ? `
                                    <button class="btn btn-primary" onclick="applyToProgram(${program.id})">Reapply</button>
                                ` : appStatus === 'DROP_REQUESTED' ? `
                                    <span class="status-badge DROP_REQUESTED">Drop Request Pending</span>
                                    <span class="status-badge ${appStatus}">${appStatus}</span>
                                ` : appStatus ? `
                                    <span class="status-badge ${appStatus}">${appStatus}</span>
                                    ${(appStatus === 'PENDING' || appStatus === 'APPROVED') ? `
                                        <button class="btn btn-outline-error" onclick="requestDrop(${app.id})">Request Drop</button>
                                    ` : ''}
                                ` : `
                                    <button class="btn btn-primary" onclick="applyToProgram(${program.id})">Apply</button>
                                `}
                            </div>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        showMessage('Error loading programs: ' + error.message, 'error');
        document.getElementById('programs-list').innerHTML = '<p class="error-message">Error loading data</p>';
    }
}

async function applyToProgram(programId) {
    if (!confirm('Apply to this program?')) return;
    
    try {
        await apiCall('/program-applications', {
            method: 'POST',
            body: JSON.stringify({
                programId,
                sewadarId: currentUser.zonalId
            })
        });
        showMessage('Application submitted successfully!');
        loadPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function showProgramForm(programId = null) {
    const modal = document.getElementById('program-modal');
    const form = document.getElementById('program-form');
    const title = document.getElementById('program-form-title');
    
    form.reset();
    document.getElementById('program-id').value = '';
    
    if (programId) {
        title.textContent = 'Edit Program';
        loadProgramForEdit(programId);
    } else {
        title.textContent = 'Create Program';
    }
    
    modal.style.display = 'block';
}

async function loadProgramForEdit(id) {
    try {
        const program = await apiCall(`/programs/${id}`);
        document.getElementById('program-id').value = program.id;
        document.getElementById('program-title').value = program.title || '';
        document.getElementById('program-description').value = program.description || '';
        document.getElementById('program-location').value = program.location || '';
        document.getElementById('program-maxSewadars').value = program.maxSewadars || '';
        document.getElementById('program-status').value = program.status || 'active';
        document.getElementById('program-dates').value = program.programDates?.join(', ') || '';
    } catch (error) {
        showMessage('Error loading program: ' + error.message, 'error');
    }
}

async function saveProgram(event) {
    event.preventDefault();
    
    const id = document.getElementById('program-id').value;
    const datesStr = document.getElementById('program-dates').value;
    const programDates = datesStr ? datesStr.split(',').map(d => d.trim()).filter(d => d) : [];
    
    const data = {
        title: document.getElementById('program-title').value.trim(),
        description: document.getElementById('program-description').value.trim(),
        location: document.getElementById('program-location').value.trim(),
        status: document.getElementById('program-status').value,
        maxSewadars: parseInt(document.getElementById('program-maxSewadars').value) || null,
        createdById: currentUser.zonalId,
        programDates
    };
    
    try {
        if (id) {
            await apiCall(`/programs/${id}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
            showMessage('Program updated successfully!');
        } else {
            await apiCall('/programs', {
                method: 'POST',
                body: JSON.stringify(data)
            });
            showMessage('Program created successfully!');
        }
        closeProgramForm();
        loadPrograms();
        if (currentSection === 'admin') {
            loadAdminPrograms();
        }
    } catch (error) {
        showMessage('Error saving program: ' + error.message, 'error');
    }
}

function closeProgramForm() {
    document.getElementById('program-modal').style.display = 'none';
}

// ==================== APPLICATIONS ====================

async function loadApplications() {
    try {
        const applications = await apiCall(`/program-applications/sewadar/${currentUser.zonalId}`);
        const listEl = document.getElementById('applications-list');
        if (!listEl) return;
        
        // Filter out completed programs
        const filtered = [];
        for (const app of applications) {
            try {
                const program = await apiCall(`/programs/${app.programId}`);
                if (program.status !== 'completed') {
                    filtered.push({ ...app, program });
                }
            } catch (e) {
                // Include if program fetch fails
                filtered.push(app);
            }
        }
        
        if (filtered.length === 0) {
            listEl.innerHTML = '<p class="info-message">You haven\'t applied to any programs yet.</p>';
            return;
        }

        listEl.innerHTML = filtered.map(app => {
            const program = app.program || {};
            return `
            <div class="card">
                <div class="card-header">
                        <h3>${program.title || `Program ${app.programId}`}</h3>
                        <span class="status-badge ${app.status}">${app.status}</span>
                    </div>
                    <div class="card-body">
                        <p><strong>Applied:</strong> ${formatDate(app.appliedAt)}</p>
                        ${app.dropRequestedAt ? `<p><strong>Drop Requested:</strong> ${formatDate(app.dropRequestedAt)}</p>` : ''}
                        ${app.dropApprovedAt ? `<p><strong>Drop Approved:</strong> ${formatDate(app.dropApprovedAt)}</p>` : ''}
                    <div class="card-actions">
                            ${(app.status === 'PENDING' || app.status === 'APPROVED') ? `
                                <button class="btn btn-danger btn-sm" onclick="requestDrop(${app.id})">Request Drop</button>
                            ` : ''}
                            ${app.status === 'DROP_REQUESTED' ? `
                                <span class="status-badge">Drop Request Pending</span>
                            ` : ''}
                            ${app.status === 'DROPPED' ? `
                                <button class="btn btn-primary btn-sm" onclick="reapplyToProgram(${app.programId})">Reapply</button>
                            ` : ''}
                    </div>
                </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
        document.getElementById('applications-list').innerHTML = '<p class="error-message">Error loading data</p>';
    }
}

async function requestDrop(applicationId) {
    if (!confirm('Request to drop from this program? This requires incharge approval.')) return;
    
    try {
        await apiCall(`/program-applications/${applicationId}/request-drop?sewadarId=${currentUser.zonalId}`, {
            method: 'PUT'
        });
        showMessage('Drop request submitted. Waiting for incharge approval.');
        loadPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function reapplyToProgram(programId) {
    if (!confirm('Reapply to this program?')) return;
    
    try {
        await apiCall('/program-applications', {
            method: 'POST',
            body: JSON.stringify({
                programId,
                sewadarId: currentUser.zonalId
            })
        });
        showMessage('Application submitted successfully!');
        loadPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

// ==================== ADMIN - PROGRAMS ====================

async function loadAdminPrograms() {
    try {
        // For INCHARGE role, show ALL programs (not just ones they created)
        // This allows promoted incharges to see all programs in the system
        const programs = await apiCall(`/programs`);
        const listEl = document.getElementById('admin-programs-list');
        if (!listEl) return;
        
        if (programs.length === 0) {
            listEl.innerHTML = '<p class="info-message">No programs created yet. Create one to get started!</p>';
            return;
        }
        
        listEl.innerHTML = programs.map(program => {
            const dropRequestsCount = program.dropRequestsCount || 0;
            return `
                <div class="card">
                    <div class="card-header">
                        <h3>${program.title}</h3>
                        <span class="status-badge ${program.status}">${program.status}</span>
                    </div>
                <div class="card-body">
                        <p><strong>Location:</strong> ${program.location || 'N/A'} ${program.locationType ? `(${program.locationType})` : ''}</p>
                        <p><strong>Applications:</strong> <span class="clickable-link" onclick="showApplicants(${program.id}, ${JSON.stringify(program.title || '')})" style="cursor: pointer; color: #667eea; text-decoration: underline;">${program.applicationCount || 0}${program.maxSewadars ? ` / ${program.maxSewadars}` : ''}</span></p>
                        ${dropRequestsCount > 0 ? `<p class="error-text"><strong>Drop Requests:</strong> ${dropRequestsCount} pending</p>` : ''}
                        <div class="card-actions">
                            <button class="btn btn-primary btn-sm" onclick="viewApplications(${program.id})">View Applications</button>
                            <button class="btn ${dropRequestsCount > 0 ? 'btn-danger' : 'btn-secondary'} btn-sm" onclick="viewDropRequests(${program.id})">
                                Review Drop Requests${dropRequestsCount > 0 ? ` (${dropRequestsCount})` : ''}
                            </button>
                            <button class="btn btn-secondary btn-sm" onclick="showProgramForm(${program.id})">Edit</button>
                </div>
            </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        showMessage('Error loading programs: ' + error.message, 'error');
    }
}

// ==================== ADMIN - SEWADARS ====================

async function loadAdminSewadars() {
    try {
        const sewadars = await apiCall('/sewadars');
        const listEl = document.getElementById('admin-sewadars-list');
        if (!listEl) return;
        
        if (sewadars.length === 0) {
            listEl.innerHTML = '<p class="info-message">No sewadars found. Add one to get started!</p>';
            return;
        }
        
        listEl.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Zonal ID</th>
                        <th>Name</th>
                        <th>Mobile</th>
                        <th>Location</th>
                        <th>Role</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${sewadars.map(sewadar => `
                        <tr>
                            <td>${sewadar.zonalId}</td>
                            <td>${sewadar.firstName} ${sewadar.lastName}</td>
                            <td>${sewadar.mobile || ''}</td>
                            <td>${sewadar.location || ''}</td>
                            <td>
                                <span class="role-badge ${sewadar.role?.toLowerCase()}" 
                                      onclick="${(currentUser?.role === 'ADMIN' || currentUser?.role === 'INCHARGE') && sewadar.zonalId !== currentUser.zonalId ? `changeRole('${sewadar.zonalId}', '${sewadar.role}')` : ''}"
                                      style="${(currentUser?.role === 'ADMIN' || currentUser?.role === 'INCHARGE') && sewadar.zonalId !== currentUser.zonalId ? 'cursor: pointer;' : ''}">
                                    ${sewadar.role || 'SEWADAR'}
                                </span>
                            </td>
                            <td>
                                <button class="btn btn-sm btn-primary" onclick="showSewadarForm('${sewadar.zonalId}')">Edit</button>
                                <button class="btn btn-sm btn-secondary" onclick="changePassword('${sewadar.zonalId}')">Change Password</button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (error) {
        showMessage('Error loading sewadars: ' + error.message, 'error');
    }
}

function showSewadarForm(zonalId = null) {
    const modal = document.getElementById('sewadar-modal');
    const form = document.getElementById('sewadar-form');
    const title = document.getElementById('sewadar-form-title');
    const zonalIdInput = document.getElementById('sewadar-zonalId-input');
    
    form.reset();
    document.getElementById('sewadar-zonalId').value = '';
    
    if (zonalId) {
        title.textContent = 'Edit Sewadar';
        zonalIdInput.disabled = true;
        loadSewadarForEdit(zonalId);
    } else {
        title.textContent = 'Add Sewadar';
        zonalIdInput.disabled = false;
    }
    
    modal.style.display = 'block';
}

async function loadSewadarForEdit(zonalId) {
    try {
        const sewadar = await apiCall(`/sewadars/${zonalId}`);
        document.getElementById('sewadar-zonalId').value = sewadar.zonalId;
        document.getElementById('sewadar-zonalId-input').value = sewadar.zonalId;
        document.getElementById('sewadar-firstName').value = sewadar.firstName || '';
        document.getElementById('sewadar-lastName').value = sewadar.lastName || '';
        document.getElementById('sewadar-location').value = sewadar.location || '';
        document.getElementById('sewadar-mobile').value = sewadar.mobile || '';
        document.getElementById('sewadar-profession').value = sewadar.profession || '';
        document.getElementById('sewadar-aadharNumber').value = sewadar.aadharNumber || '';
        document.getElementById('sewadar-languages').value = sewadar.languages?.join(', ') || '';
        document.getElementById('sewadar-address1').value = sewadar.address?.address1 || '';
        document.getElementById('sewadar-address2').value = sewadar.address?.address2 || '';
        document.getElementById('sewadar-email').value = sewadar.address?.email || '';
        document.getElementById('sewadar-remarks').value = sewadar.remarks || '';
    } catch (error) {
        showMessage('Error loading sewadar: ' + error.message, 'error');
    }
}

async function saveSewadar(event) {
    event.preventDefault();
    
    const zonalId = document.getElementById('sewadar-zonalId').value;
    const zonalIdInput = document.getElementById('sewadar-zonalId-input').value.trim();
    const languagesStr = document.getElementById('sewadar-languages').value;
    const languages = languagesStr ? languagesStr.split(',').map(l => l.trim()).filter(l => l) : [];
    
    const data = {
        firstName: document.getElementById('sewadar-firstName').value.trim(),
        lastName: document.getElementById('sewadar-lastName').value.trim(),
        location: document.getElementById('sewadar-location').value.trim(),
        mobile: document.getElementById('sewadar-mobile').value.trim(),
        profession: document.getElementById('sewadar-profession').value.trim(),
        aadharNumber: document.getElementById('sewadar-aadharNumber').value.trim(),
        languages,
        address1: document.getElementById('sewadar-address1').value.trim(),
        address2: document.getElementById('sewadar-address2').value.trim(),
        email: document.getElementById('sewadar-email').value.trim(),
        remarks: document.getElementById('sewadar-remarks').value.trim()
    };
    
    if (!zonalId && zonalIdInput) {
        data.zonalId = zonalIdInput;
    }
    
    const password = document.getElementById('sewadar-password').value.trim();
    if (password) {
        data.password = password;
    }

    try {
        if (zonalId) {
            await apiCall(`/sewadars/${zonalId}`, {
                method: 'PUT',
                body: JSON.stringify(data)
            });
            showMessage('Sewadar updated successfully!');
        } else {
            await apiCall('/sewadars', {
                method: 'POST',
                body: JSON.stringify(data)
            });
            showMessage('Sewadar created successfully!');
        }
        closeSewadarForm();
        loadAdminSewadars();
    } catch (error) {
        showMessage('Error saving sewadar: ' + error.message, 'error');
    }
}

function closeSewadarForm() {
    document.getElementById('sewadar-modal').style.display = 'none';
}

// ==================== ADMIN - APPLICATIONS ====================

let currentProgramForApplications = null;
let currentSortBy = 'attendance';
let currentSortOrder = 'desc';

async function viewApplications(programId) {
    currentProgramForApplications = programId;
    try {
        const program = await apiCall(`/programs/${programId}`);
        const applications = await apiCall(`/program-applications/program/${programId}/prioritized?sortBy=${currentSortBy}&order=${currentSortOrder}`);
        
        const content = document.getElementById('applications-dialog-content');
        content.innerHTML = `
            <div class="filters" style="margin-bottom: 20px;">
                <label>Sort By:</label>
                <select id="app-sort-by" onchange="updateApplicationSort()">
                    <option value="attendance" ${currentSortBy === 'attendance' ? 'selected' : ''}>Total Attendance</option>
                    <option value="beasAttendance" ${currentSortBy === 'beasAttendance' ? 'selected' : ''}>BEAS Attendance</option>
                    <option value="nonBeasAttendance" ${currentSortBy === 'nonBeasAttendance' ? 'selected' : ''}>Non-BEAS Attendance</option>
                    <option value="days" ${currentSortBy === 'days' ? 'selected' : ''}>Total Days</option>
                </select>
                <label>Order:</label>
                <select id="app-sort-order" onchange="updateApplicationSort()">
                    <option value="desc" ${currentSortOrder === 'desc' ? 'selected' : ''}>Descending</option>
                    <option value="asc" ${currentSortOrder === 'asc' ? 'selected' : ''}>Ascending</option>
                </select>
            </div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Name</th>
                        <th>Mobile</th>
                        <th>Total Attendance</th>
                        <th>Total Days</th>
                        <th>Status</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${applications.map(app => `
                        <tr>
                            <td>${app.sewadar.firstName} ${app.sewadar.lastName}</td>
                            <td>${app.sewadar.mobile || ''}</td>
                            <td>${app.totalAttendanceCount || 0}</td>
                            <td>${app.totalDaysAttended || 0}</td>
                            <td><span class="status-badge ${app.status}">${app.status}</span></td>
                            <td>
                                ${app.status === 'PENDING' ? `
                                    <button class="btn btn-sm btn-success" onclick="approveApplication(${app.id})">Approve</button>
                                    <button class="btn btn-sm btn-danger" onclick="rejectApplication(${app.id})">Reject</button>
                                ` : ''}
                                ${(app.status === 'APPROVED' || app.status === 'REJECTED') ? `
                                    <button class="btn btn-sm btn-warning" onclick="rollbackApplication(${app.id})" title="Rollback to PENDING">Rollback</button>
                                ` : ''}
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
        document.getElementById('applications-dialog-title').textContent = `Applications for: ${program.title}`;
        document.getElementById('applications-dialog-modal').style.display = 'block';
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
    }
}

function updateApplicationSort() {
    currentSortBy = document.getElementById('app-sort-by').value;
    currentSortOrder = document.getElementById('app-sort-order').value;
    if (currentProgramForApplications) {
        viewApplications(currentProgramForApplications);
    }
}

async function approveApplication(applicationId) {
    if (!confirm('Approve this application?')) return;
    
    try {
        await apiCall(`/program-applications/${applicationId}/status?status=APPROVED`, {
            method: 'PUT'
        });
        showMessage('Application approved!');
        if (currentProgramForApplications) {
            viewApplications(currentProgramForApplications);
        }
        loadAdminPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function rejectApplication(applicationId) {
    if (!confirm('Reject this application?')) return;
    
    try {
        await apiCall(`/program-applications/${applicationId}/status?status=REJECTED`, {
            method: 'PUT'
        });
        showMessage('Application rejected.');
        if (currentProgramForApplications) {
            viewApplications(currentProgramForApplications);
        }
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

async function rollbackApplication(applicationId) {
    if (!confirm('Rollback this application to PENDING? This will undo the approval/rejection.')) return;
    
    try {
        await apiCall(`/program-applications/${applicationId}/rollback?inchargeId=${currentUser.zonalId}`, {
            method: 'PUT'
        });
        showMessage('Application rolled back to PENDING.');
        if (currentProgramForApplications) {
            viewApplications(currentProgramForApplications);
        }
        loadAdminPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeApplicationsDialog() {
    document.getElementById('applications-dialog-modal').style.display = 'none';
    currentProgramForApplications = null;
}

// ==================== ADMIN - DROP REQUESTS ====================

async function viewDropRequests(programId) {
    try {
        const program = await apiCall(`/programs/${programId}`);
        const dropRequests = await apiCall(`/program-applications/program/${programId}/drop-requests`);
        
        const content = document.getElementById('drop-requests-dialog-content');
        if (dropRequests.length === 0) {
            content.innerHTML = '<p class="info-message">No drop requests pending.</p>';
        } else {
            content.innerHTML = `
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Mobile</th>
                            <th>Applied At</th>
                            <th>Drop Requested</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${dropRequests.map(req => `
                            <tr>
                                <td>${req.sewadar.firstName} ${req.sewadar.lastName}</td>
                                <td>${req.sewadar.mobile || ''}</td>
                                <td>${formatDate(req.appliedAt)}</td>
                                <td>${req.dropRequestedAt ? formatDate(req.dropRequestedAt) : 'N/A'}</td>
                                <td>
                                    <button class="btn btn-sm btn-success" onclick="approveDropRequest(${req.id})">Approve Drop</button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }
        
        document.getElementById('drop-requests-dialog-title').textContent = `Drop Requests for: ${program.title}`;
        document.getElementById('drop-requests-dialog-modal').style.display = 'block';
    } catch (error) {
        showMessage('Error loading drop requests: ' + error.message, 'error');
    }
}

async function approveDropRequest(applicationId) {
    if (!confirm('Approve this drop request?')) return;
    
    try {
        await apiCall(`/program-applications/${applicationId}/approve-drop?inchargeId=${currentUser.zonalId}&allowReapply=true`, {
            method: 'PUT'
        });
        showMessage('Drop request approved.');
        document.getElementById('drop-requests-dialog-modal').style.display = 'none';
        loadAdminPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeDropRequestsDialog() {
    document.getElementById('drop-requests-dialog-modal').style.display = 'none';
}

// ==================== ADMIN - PASSWORD CHANGE ====================

let currentSewadarForPassword = null;

function changePassword(zonalId) {
    currentSewadarForPassword = zonalId;
    document.getElementById('new-password').value = '';
    document.getElementById('password-modal').style.display = 'block';
}

async function savePassword(event) {
    event.preventDefault();
    const newPassword = document.getElementById('new-password').value;
    
    if (newPassword.length < 6) {
        showMessage('Password must be at least 6 characters long', 'error');
        return;
    }

    try {
        // Get current sewadar data
        const sewadar = await apiCall(`/sewadars/${currentSewadarForPassword}`);
        
        // Prepare update payload
        const updatePayload = {
            firstName: sewadar.firstName || '',
            lastName: sewadar.lastName || '',
            mobile: sewadar.mobile || '',
            location: sewadar.location || '',
            profession: sewadar.profession || '',
            password: newPassword,
            dateOfBirth: sewadar.dateOfBirth || null,
            joiningDate: sewadar.joiningDate || null,
            emergencyContact: sewadar.emergencyContact || null,
            emergencyContactRelationship: sewadar.emergencyContactRelationship || null,
            photoUrl: sewadar.photoUrl || null,
            aadharNumber: sewadar.aadharNumber || null,
            languages: sewadar.languages || [],
            remarks: sewadar.remarks || '',
            address1: sewadar.address?.address1 || '',
            address2: sewadar.address?.address2 || '',
            email: sewadar.address?.email || ''
        };
        
        await apiCall(`/sewadars/${currentSewadarForPassword}`, {
            method: 'PUT',
            body: JSON.stringify(updatePayload)
        });
        
        showMessage('Password changed successfully!');
        closePasswordModal();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closePasswordModal() {
    document.getElementById('password-modal').style.display = 'none';
    currentSewadarForPassword = null;
}

// ==================== ADMIN - ROLE CHANGE ====================

let currentSewadarForRoleChange = null;
let currentSewadarRole = null;

function changeRole(zonalId, currentRole) {
    if (zonalId === currentUser.zonalId) {
        showMessage('You cannot change your own role', 'error');
        return;
    }
    
    currentSewadarForRoleChange = zonalId;
    currentSewadarRole = currentRole;
    document.getElementById('role-change-password').value = '';
    document.getElementById('role-change-title').textContent = currentRole === 'INCHARGE' ? 'Demote Incharge' : 'Promote to Incharge';
    document.getElementById('role-change-btn').textContent = currentRole === 'INCHARGE' ? 'Demote' : 'Promote';
    document.getElementById('role-change-modal').style.display = 'block';
}

async function saveRoleChange(event) {
    event.preventDefault();
    const password = document.getElementById('role-change-password').value;
    
    if (!password) {
        showMessage('Please enter your password', 'error');
        return;
    }
    
    try {
        const isCurrentlyIncharge = currentSewadarRole === 'INCHARGE';
        const endpoint = isCurrentlyIncharge 
            ? `/sewadars/${currentSewadarForRoleChange}/demote?inchargeId=${currentUser.zonalId}&password=${encodeURIComponent(password)}`
            : `/sewadars/${currentSewadarForRoleChange}/promote?inchargeId=${currentUser.zonalId}&password=${encodeURIComponent(password)}`;
        
        await apiCall(endpoint, {
            method: 'POST',
            body: JSON.stringify({})
        });
        
        showMessage(`Sewadar ${isCurrentlyIncharge ? 'demoted' : 'promoted'} successfully!`);
        closeRoleChangeModal();
        loadAdminSewadars();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeRoleChangeModal() {
    document.getElementById('role-change-modal').style.display = 'none';
    currentSewadarForRoleChange = null;
    currentSewadarRole = null;
}

// ==================== ATTENDANCE ====================

async function loadAttendancePrograms() {
    try {
        // For INCHARGE role, show ALL programs (not just ones they created)
        // This allows promoted incharges to see all programs in the system
        // Note: Only program creators can mark attendance (enforced by backend)
        const programs = await apiCall(`/programs`);
        const activePrograms = programs.filter(p => p.status === 'active');
        const listEl = document.getElementById('attendance-programs-list');
        if (!listEl) return;
        
        if (activePrograms.length === 0) {
            listEl.innerHTML = '<p class="info-message">No active programs available for attendance marking.</p>';
            return;
        }

        listEl.innerHTML = activePrograms.map(program => `
            <div class="card">
                <div class="card-header">
                    <h3>${program.title}</h3>
                    <span class="status-badge ${program.status}">${program.status}</span>
                </div>
                <div class="card-body">
                    <p><strong>Location:</strong> ${program.location || 'N/A'} ${program.locationType ? `(${program.locationType})` : ''}</p>
                    <p><strong>Dates:</strong> ${program.programDates?.map(d => formatDate(d)).join(', ') || 'N/A'}</p>
                    <p><strong>Applications:</strong> <span class="clickable-link" onclick="showApplicants(${program.id}, '${program.title}')" style="cursor: pointer; color: #667eea; text-decoration: underline;">${program.applicationCount || 0}${program.maxSewadars ? ` / ${program.maxSewadars}` : ''}</span></p>
                    <div class="card-actions">
                        <button class="btn btn-primary btn-sm" onclick="showMarkAttendanceWithSave(${program.id})">Mark Attendance</button>
                        <button class="btn btn-secondary btn-sm" onclick="showViewAttendance(${program.id})">View Attendance</button>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading programs: ' + error.message, 'error');
    }
}

let currentProgramForAttendance = null;

async function showMarkAttendance(programId) {
    currentProgramForAttendance = programId;
    try {
        const program = await apiCall(`/programs/${programId}`);
        const attendees = await apiCall(`/attendances/program/${programId}/attendees`);
        
        if (attendees.length === 0) {
            showMessage('No approved attendees found for this program', 'error');
            return;
        }
        
        const content = document.getElementById('attendance-modal-content');
        content.innerHTML = `
            <div class="form-group">
                <label>Program Date *</label>
                <select id="attendance-date" required>
                    ${program.programDates?.map(date => `
                        <option value="${date}">${formatDate(date)}</option>
                    `).join('') || '<option value="">No dates available</option>'}
                </select>
            </div>
            <div class="form-group">
                <label>Notes (Optional)</label>
                <textarea id="attendance-notes" rows="2"></textarea>
            </div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th><input type="checkbox" id="select-all-attendance" onchange="toggleAllAttendance(this.checked)"></th>
                        <th>Name</th>
                        <th>Zonal ID</th>
                        <th>Mobile</th>
                    </tr>
                </thead>
                <tbody>
                    ${attendees.map(attendee => `
                        <tr>
                            <td><input type="checkbox" class="attendance-checkbox" value="${attendee.zonalId}"></td>
                            <td>${attendee.firstName || ''} ${attendee.lastName || ''}</td>
                            <td>${attendee.zonalId || ''}</td>
                            <td>${attendee.mobile || '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
        
        document.getElementById('attendance-modal-title').textContent = `Mark Attendance - ${program.title}`;
        document.getElementById('attendance-modal').style.display = 'block';
    } catch (error) {
        showMessage('Error loading attendees: ' + error.message, 'error');
    }
}

function toggleAllAttendance(checked) {
    document.querySelectorAll('.attendance-checkbox').forEach(cb => cb.checked = checked);
}

async function saveAttendance() {
    const selectedDate = document.getElementById('attendance-date')?.value;
    if (!selectedDate) {
        showMessage('Please select a program date', 'error');
        return;
    }
    
    const selectedSewadars = Array.from(document.querySelectorAll('.attendance-checkbox:checked'))
        .map(cb => cb.value);
    
    if (selectedSewadars.length === 0) {
        showMessage('Please select at least one sewadar', 'error');
        return;
    }
    
    const notes = document.getElementById('attendance-notes')?.value || null;
    
    try {
        await apiCall('/attendances', {
            method: 'POST',
            body: JSON.stringify({
                programId: currentProgramForAttendance,
                programDate: selectedDate,
                sewadarIds: selectedSewadars,
                notes
            })
        });
        
        showMessage('Attendance marked successfully!');
        closeAttendanceModal();
        loadAttendancePrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeAttendanceModal() {
    document.getElementById('attendance-modal').style.display = 'none';
    currentProgramForAttendance = null;
}

async function showViewAttendance(programId) {
    try {
        const program = await apiCall(`/programs/${programId}`);
        const records = await apiCall(`/attendances/program/${programId}`);
        
        const content = document.getElementById('view-attendance-modal-content');
        if (records.length === 0) {
            content.innerHTML = '<p class="info-message">No attendance records found for this program.</p>';
        } else {
            content.innerHTML = `
                <table class="data-table">
                    <thead>
                        <tr>
                            <th>Name</th>
                            <th>Zonal ID</th>
                            <th>Status</th>
                            <th>Date</th>
                            <th>Notes</th>
                            <th>Marked At</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${records.map(record => `
                            <tr>
                                <td>${record.sewadar?.firstName || ''} ${record.sewadar?.lastName || ''}</td>
                                <td>${record.sewadar?.zonalId || ''}</td>
                                <td><span class="status-badge success">Present</span></td>
                                <td>${formatDate(record.attendanceDate)}</td>
                                <td>${record.notes || '-'}</td>
                                <td>${formatDateTime(record.markedAt)}</td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            `;
        }
        
        document.getElementById('view-attendance-modal-title').textContent = `Attendance Records - ${program.title}`;
        document.getElementById('view-attendance-modal').style.display = 'block';
    } catch (error) {
        showMessage('Error loading attendance records: ' + error.message, 'error');
    }
}

function closeViewAttendanceModal() {
    document.getElementById('view-attendance-modal').style.display = 'none';
}

// Update showMarkAttendance to include save button
async function showMarkAttendanceWithSave(programId) {
    await showMarkAttendance(programId);
    const content = document.getElementById('attendance-modal-content');
    if (content && !content.querySelector('.attendance-save-btn')) {
        const actions = document.createElement('div');
        actions.className = 'form-actions';
        actions.innerHTML = `
            <button type="button" class="btn btn-secondary" onclick="closeAttendanceModal()">Cancel</button>
            <button type="button" class="btn btn-primary attendance-save-btn" onclick="saveAttendance()">Save Attendance</button>
        `;
        content.appendChild(actions);
    }
}

// Replace the onclick handlers
document.addEventListener('DOMContentLoaded', () => {
    // This will be handled by the onclick attributes in the HTML
});

// ==================== DASHBOARD ====================

async function loadDashboardSewadars() {
    const location = document.getElementById('dashboard-location-filter')?.value || '';
    const languages = document.getElementById('dashboard-languages-filter')?.value || '';
    const languagesList = languages ? languages.split(',').map(l => l.trim()).filter(l => l) : null;
    
    try {
        const response = await apiCall('/dashboard/sewadars', {
            method: 'POST',
            body: JSON.stringify({
                page: 0,
                size: 100,
                location: location || null,
                languages: languagesList,
                languageMatchType: 'ANY',
                joiningDateFrom: null,
                joiningDateTo: null,
                sortBy: null,
                sortOrder: 'ASC'
            })
        });
        
        const sewadars = response.sewadars || [];
        const listEl = document.getElementById('dashboard-sewadars-list');
        if (!listEl) return;
        
        if (sewadars.length === 0) {
            listEl.innerHTML = '<p class="info-message">No sewadars found matching the filters.</p>';
            return;
        }
        
        listEl.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Zonal ID</th>
                        <th>Name</th>
                        <th>Mobile</th>
                        <th>Location</th>
                        <th>Total Programs</th>
                        <th>Total Days</th>
                        <th>BEAS Days</th>
                        <th>Non-BEAS Days</th>
                    </tr>
                </thead>
                <tbody>
                    ${sewadars.map(s => `
                        <tr>
                            <td>${s.zonalId}</td>
                            <td>${s.firstName} ${s.lastName}</td>
                            <td>${s.mobile || ''}</td>
                            <td>${s.location || ''}</td>
                            <td>${s.totalProgramsCount || 0}</td>
                            <td>${s.totalDaysAttended || 0}</td>
                            <td>${s.beasDaysAttended || 0}</td>
                            <td>${s.nonBeasDaysAttended || 0}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (error) {
        showMessage('Error loading sewadars: ' + error.message, 'error');
    }
}

function clearDashboardFilters() {
    document.getElementById('dashboard-location-filter').value = '';
    document.getElementById('dashboard-languages-filter').value = '';
    loadDashboardSewadars();
}

async function loadDashboardApplications() {
    const programId = document.getElementById('dashboard-program-id-filter')?.value;
    const status = document.getElementById('dashboard-status-filter')?.value;
    
    try {
        const response = await apiCall('/dashboard/applications', {
            method: 'POST',
            body: JSON.stringify({
                page: 0,
                size: 100,
                programId: programId ? parseInt(programId) : null,
                statuses: status ? [status] : null
            })
        });
        
        const applications = response.applications || [];
        const listEl = document.getElementById('dashboard-applications-list');
        if (!listEl) return;
        
        if (applications.length === 0) {
            listEl.innerHTML = '<p class="info-message">No applications found matching the filters.</p>';
            return;
        }
        
        listEl.innerHTML = `
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Application ID</th>
                        <th>Zonal ID</th>
                        <th>Name</th>
                        <th>Mobile</th>
                        <th>Status</th>
                        <th>Applied At</th>
                    </tr>
                </thead>
                <tbody>
                    ${applications.map(app => `
                        <tr>
                            <td>${app.applicationId}</td>
                            <td>${app.sewadarZonalId}</td>
                            <td>${app.sewadarName}</td>
                            <td>${app.mobile || ''}</td>
                            <td><span class="status-badge ${app.status}">${app.status}</span></td>
                            <td>${formatDateTime(app.appliedAt)}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;
    } catch (error) {
        showMessage('Error loading applications: ' + error.message, 'error');
    }
}

function clearDashboardApplicationFilters() {
    document.getElementById('dashboard-program-id-filter').value = '';
    document.getElementById('dashboard-status-filter').value = '';
    loadDashboardApplications();
}

function showDashboardTab(tab) {
    document.querySelectorAll('#dashboard-section .tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('#dashboard-section .tab-btn').forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(`dashboard-${tab}-tab`).classList.add('active');
    event.target.classList.add('active');
}

function showAdminTab(tab) {
    document.querySelectorAll('#admin-section .tab-content').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('#admin-section .tab-btn').forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(`admin-${tab}-tab`).classList.add('active');
    event.target.classList.add('active');
}

// ==================== WORKFLOW ====================

async function loadWorkflowPrograms() {
    try {
        // For INCHARGE role, show ALL programs (not just ones they created)
        // This allows promoted incharges to see all programs in the system
        // Note: Only program creators can manage workflows (enforced by backend)
        const programs = await apiCall(`/programs`);
        const listEl = document.getElementById('workflow-programs-list');
        if (!listEl) return;
        
        if (programs.length === 0) {
            listEl.innerHTML = '<p class="info-message">No programs found. Create a program to see its workflow.</p>';
        return;
    }

        // Load workflows for all programs
        const workflows = {};
        for (const program of programs) {
            try {
                const workflow = await apiCall(`/workflow/program/${program.id}`);
                workflows[program.id] = workflow;
            } catch (e) {
                console.error(`Error loading workflow for program ${program.id}:`, e);
            }
        }
        
        listEl.innerHTML = programs.map(program => {
            const workflow = workflows[program.id];
            const steps = [
                'Make Program Active',
                'Post Application Message',
                'Release Form',
                'Collect Details',
                'Post Mail to Area Secretary',
                'Post General Instructions'
            ];
            
            return `
                <div class="card">
                    <div class="card-header">
                        <h3>${program.title}</h3>
                        <span class="status-badge ${program.status}">${program.status}</span>
                    </div>
                    <div class="card-body">
                        ${!workflow ? `
                            <p class="error-message">Workflow not initialized.</p>
                        ` : `
                            <div class="workflow-steps">
                                ${steps.map((step, index) => {
                                    const stepNum = index + 1;
                                    const isCurrent = workflow.currentNode === stepNum;
                                    const isCompleted = workflow.currentNode > stepNum;
                                    return `
                                        <div class="workflow-step ${isCompleted ? 'completed' : isCurrent ? 'current' : ''}">
                                            <span class="step-number">${stepNum}</span>
                                            <span class="step-name">${step}</span>
                                            ${isCurrent ? '<span class="step-badge">Current</span>' : ''}
                                        </div>
                                    `;
                                }).join('')}
                            </div>
                            <div class="card-actions">
                                ${workflow.currentNode < 6 ? `
                                    <button class="btn btn-primary btn-sm" onclick="moveToNextNode(${program.id})">Go to Next Step</button>
                                ` : '<span class="status-badge success">Workflow Complete</span>'}
                            </div>
                        `}
                    </div>
                </div>
            `;
        }).join('');
    } catch (error) {
        showMessage('Error loading workflows: ' + error.message, 'error');
    }
}

async function moveToNextNode(programId) {
    if (!confirm('Move to next step?')) return;
    
    try {
        await apiCall(`/workflow/program/${programId}/next-node`, {
            method: 'POST'
        });
        showMessage('Moved to next step successfully!');
        loadWorkflowPrograms();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

// ==================== PENDING ACTIONS ====================

async function loadPendingActions() {
    try {
        const programs = await apiCall('/programs');
        const pending = [];
        
        for (const program of programs) {
            try {
                const workflow = await apiCall(`/workflow/program/${program.id}`);
                if (workflow.formReleased && workflow.currentNode >= 4) {
                    // Check if sewadar already submitted
                    try {
                        await apiCall(`/form-submissions/program/${program.id}/sewadar/${currentUser.zonalId}`);
                        // Submission exists, skip
                    } catch (e) {
                        // No submission found, add to pending
                        pending.push(program);
                    }
                }
            } catch (e) {
                // Skip if workflow not found
            }
        }
        
        const listEl = document.getElementById('pending-actions-list');
        if (!listEl) return;
        
        if (pending.length === 0) {
            listEl.innerHTML = '<p class="info-message">No pending actions at this time.</p>';
            return;
        }
        
        listEl.innerHTML = pending.map(program => `
            <div class="card">
                <div class="card-header">
                    <h3>${program.title}</h3>
                </div>
                <div class="card-body">
                    <p>Please fill the travel details form for this program.</p>
                    <button class="btn btn-primary" onclick="showFormSubmission(${program.id})">Fill Form</button>
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading pending actions: ' + error.message, 'error');
    }
}

let currentProgramForFormSubmission = null;

function showFormSubmission(programId) {
    currentProgramForFormSubmission = programId;
    document.getElementById('form-submission-form').reset();
    document.getElementById('form-submission-title').textContent = `Travel Details Form - Program ${programId}`;
    document.getElementById('form-submission-modal').style.display = 'block';
}

async function submitForm(event) {
    event.preventDefault();
    
    const formData = {
        programId: currentProgramForFormSubmission,
        name: document.getElementById('form-name').value,
        startingDateTimeFromHome: document.getElementById('form-starting-datetime').value || null,
        reachingDateTimeToHome: document.getElementById('form-reaching-datetime').value || null,
        onwardTrainFlightDateTime: document.getElementById('form-onward-datetime').value || null,
        onwardTrainFlightNo: document.getElementById('form-onward-number').value || null,
        returnTrainFlightDateTime: document.getElementById('form-return-datetime').value || null,
        returnTrainFlightNo: document.getElementById('form-return-number').value || null,
        stayInHotel: document.getElementById('form-stay-hotel').value || null,
        stayInPandal: document.getElementById('form-stay-pandal').value || null
    };
    
    try {
        await apiCall('/form-submissions', {
            method: 'POST',
            body: JSON.stringify(formData)
        });
        showMessage('Form submitted successfully!');
        closeFormSubmissionModal();
        loadPendingActions();
    } catch (error) {
        showMessage('Error: ' + error.message, 'error');
    }
}

function closeFormSubmissionModal() {
    document.getElementById('form-submission-modal').style.display = 'none';
    currentProgramForFormSubmission = null;
}

// ==================== MODAL CLOSE HANDLERS ====================

window.onclick = function(event) {
    const modals = ['program-modal', 'sewadar-modal', 'attendance-modal', 'view-attendance-modal',
                    'applications-dialog-modal', 'drop-requests-dialog-modal', 'password-modal',
                    'role-change-modal', 'form-submission-modal', 'applicants-modal'];
    
    modals.forEach(modalId => {
        const modal = document.getElementById(modalId);
        if (event.target === modal) {
            modal.style.display = 'none';
        }
    });
}

