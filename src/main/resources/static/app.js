// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

// Token storage (for authentication - optional for this simple example)
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

function showTab(tabName) {
    // Hide all tabs
    document.querySelectorAll('.tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });

    // Show selected tab
    document.getElementById(`${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');

    // Load data for selected tab
    if (tabName === 'sewadars') {
        loadSewadars();
    } else if (tabName === 'programs') {
        loadPrograms();
    }
}

// Sewadar Functions
async function loadSewadars() {
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars`);
        if (!response.ok) throw new Error('Failed to load sewadars');
        
        const sewadars = await response.json();
        const listEl = document.getElementById('sewadars-list');
        
        if (sewadars.length === 0) {
            listEl.innerHTML = '<p>No sewadars found. Add one to get started!</p>';
            return;
        }

        listEl.innerHTML = sewadars.map(sewadar => `
            <div class="card">
                <div class="card-header">
                    <h3>${sewadar.firstName} ${sewadar.lastName}</h3>
                    <div class="card-actions">
                        <button class="btn btn-sm btn-primary" onclick="editSewadar('${sewadar.zonalId}')">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteSewadar('${sewadar.zonalId}')">Delete</button>
                    </div>
                </div>
                <div class="card-body">
                    <p><strong>Zonal ID:</strong> ${sewadar.zonalId}</p>
                    <p><strong>Location:</strong> ${sewadar.location || 'N/A'}</p>
                    <p><strong>Mobile:</strong> ${sewadar.mobile || 'N/A'}</p>
                    <p><strong>Role:</strong> ${sewadar.role || 'N/A'}</p>
                    <p><strong>Remarks:</strong> ${sewadar.remarks || 'N/A'}</p>
                    ${sewadar.address ? `
                        <p><strong>Address:</strong> ${sewadar.address.address1 || ''}, ${sewadar.address.address2 || ''}</p>
                        <p><strong>Email:</strong> ${sewadar.address.email || 'N/A'}</p>
                    ` : '<p><strong>Address:</strong> Not assigned</p>'}
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading sewadars: ' + error.message, 'error');
        document.getElementById('sewadars-list').innerHTML = '<p>Error loading data</p>';
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
        zonalIdInput.disabled = true; // Disable zonalId for editing
        loadSewadarForEdit(zonalId);
    } else {
        title.textContent = 'Add Sewadar';
        zonalIdInput.disabled = false; // Enable zonalId for new sewadar
    }
    
    modal.style.display = 'block';
}

async function loadSewadarForEdit(zonalId) {
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${zonalId}`);
        if (!response.ok) throw new Error('Failed to load sewadar');
        
        const sewadar = await response.json();
        document.getElementById('sewadar-zonalId').value = sewadar.zonalId;
        document.getElementById('sewadar-zonalId-input').value = sewadar.zonalId;
        document.getElementById('sewadar-firstName').value = sewadar.firstName || '';
        document.getElementById('sewadar-lastName').value = sewadar.lastName || '';
        document.getElementById('sewadar-location').value = sewadar.location || '';
        document.getElementById('sewadar-mobile').value = sewadar.mobile || '';
        // Load address fields if address exists
        if (sewadar.address) {
            document.getElementById('sewadar-address1').value = sewadar.address.address1 || '';
            document.getElementById('sewadar-address2').value = sewadar.address.address2 || '';
            document.getElementById('sewadar-email').value = sewadar.address.email || '';
        } else {
            document.getElementById('sewadar-address1').value = '';
            document.getElementById('sewadar-address2').value = '';
            document.getElementById('sewadar-email').value = '';
        }
        document.getElementById('sewadar-remarks').value = sewadar.remarks || '';
    } catch (error) {
        showMessage('Error loading sewadar: ' + error.message, 'error');
    }
}

async function saveSewadar(event) {
    event.preventDefault();
    
    const zonalId = document.getElementById('sewadar-zonalId').value;
    const zonalIdInput = document.getElementById('sewadar-zonalId-input').value.trim();
    
    const data = {
        firstName: document.getElementById('sewadar-firstName').value.trim(),
        lastName: document.getElementById('sewadar-lastName').value.trim(),
        location: document.getElementById('sewadar-location').value.trim(),
        mobile: document.getElementById('sewadar-mobile').value.trim(),
        remarks: document.getElementById('sewadar-remarks').value.trim()
    };
    
    // Add zonalId for new sewadars (required)
    if (!zonalId && zonalIdInput) {
        data.zonalId = zonalIdInput;
    }
    
    // Add address fields if provided
    const address1 = document.getElementById('sewadar-address1').value.trim();
    const address2 = document.getElementById('sewadar-address2').value.trim();
    const email = document.getElementById('sewadar-email').value.trim();
    
    if (address1 || address2 || email) {
        data.address1 = address1;
        if (address2) data.address2 = address2;
        if (email) data.email = email;
    }

    try {
        const url = zonalId 
            ? `${API_BASE_URL}/sewadars/${zonalId}`
            : `${API_BASE_URL}/sewadars`;
        
        const method = zonalId ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save sewadar');
        }

        showMessage(zonalId ? 'Sewadar updated successfully!' : 'Sewadar created successfully!');
        closeSewadarForm();
        loadSewadars();
    } catch (error) {
        showMessage('Error saving sewadar: ' + error.message, 'error');
    }
}

async function editSewadar(zonalId) {
    showSewadarForm(zonalId);
}

async function deleteSewadar(zonalId) {
    if (!confirm('Are you sure you want to delete this sewadar? This will also delete all associated data.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${zonalId}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete sewadar');

        showMessage('Sewadar deleted successfully!');
        loadSewadars();
    } catch (error) {
        showMessage('Error deleting sewadar: ' + error.message, 'error');
    }
}

function closeSewadarForm() {
    document.getElementById('sewadar-modal').style.display = 'none';
}

// Program Functions
async function loadPrograms() {
    try {
        const response = await fetch(`${API_BASE_URL}/programs`);
        if (!response.ok) throw new Error('Failed to load programs');
        
        const programs = await response.json();
        const listEl = document.getElementById('programs-list');
        
        if (programs.length === 0) {
            listEl.innerHTML = '<p>No programs found. Add one to get started!</p>';
            return;
        }

        listEl.innerHTML = programs.map(program => `
            <div class="card">
                <div class="card-header">
                    <h3>${program.title}</h3>
                    <div class="card-actions">
                        <button class="btn btn-sm btn-primary" onclick="editProgram(${program.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteProgram(${program.id})">Delete</button>
                    </div>
                </div>
                <div class="card-body">
                    <p><strong>Description:</strong> ${program.description || 'N/A'}</p>
                    <p><strong>Location:</strong> ${program.location || 'N/A'}</p>
                    <p><strong>Status:</strong> ${program.status || 'N/A'}</p>
                    <p><strong>Max Sewadars:</strong> ${program.maxSewadars || 'N/A'}</p>
                    ${program.createdBy ? `<p><strong>Created By:</strong> ${program.createdBy.firstName} ${program.createdBy.lastName} (${program.createdBy.zonalId})</p>` : ''}
                    ${program.programDates && program.programDates.length > 0 ? `
                        <p><strong>Program Dates:</strong> ${program.programDates.join(', ')}</p>
                    ` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading programs: ' + error.message, 'error');
        document.getElementById('programs-list').innerHTML = '<p>Error loading data</p>';
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
        title.textContent = 'Add Program';
    }
    
    modal.style.display = 'block';
}

async function loadProgramForEdit(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/programs/${id}`);
        if (!response.ok) throw new Error('Failed to load program');
        
        const program = await response.json();
        document.getElementById('program-id').value = program.id;
        document.getElementById('program-title').value = program.title || '';
        document.getElementById('program-description').value = program.description || '';
        document.getElementById('program-location').value = program.location || '';
        document.getElementById('program-maxSewadars').value = program.maxSewadars || '';
        document.getElementById('program-status').value = program.status || 'active';
    } catch (error) {
        showMessage('Error loading program: ' + error.message, 'error');
    }
}

async function saveProgram(event) {
    event.preventDefault();
    
    const id = document.getElementById('program-id').value;
    const data = {
        title: document.getElementById('program-title').value.trim(),
        description: document.getElementById('program-description').value.trim(),
        location: document.getElementById('program-location').value.trim(),
        status: document.getElementById('program-status').value,
        maxSewadars: parseInt(document.getElementById('program-maxSewadars').value) || null
    };
    
    // Note: createdById should be set from authenticated user context
    // For this simple example, we'll skip it (backend may require it)
    // In production, this should come from the authenticated user's token

    try {
        const url = id 
            ? `${API_BASE_URL}/programs/${id}`
            : `${API_BASE_URL}/programs`;
        
        const method = id ? 'PUT' : 'POST';
        
        const headers = {
            'Content-Type': 'application/json'
        };
        
        // Add auth token if available
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }
        
        const response = await fetch(url, {
            method: method,
            headers: headers,
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save program');
        }

        showMessage(id ? 'Program updated successfully!' : 'Program created successfully!');
        closeProgramForm();
        loadPrograms();
    } catch (error) {
        showMessage('Error saving program: ' + error.message, 'error');
    }
}

async function editProgram(id) {
    showProgramForm(id);
}

async function deleteProgram(id) {
    if (!confirm('Are you sure you want to delete this program?')) {
        return;
    }

    try {
        const headers = {};
        if (authToken) {
            headers['Authorization'] = `Bearer ${authToken}`;
        }
        
        const response = await fetch(`${API_BASE_URL}/programs/${id}`, {
            method: 'DELETE',
            headers: headers
        });

        if (!response.ok) throw new Error('Failed to delete program');

        showMessage('Program deleted successfully!');
        loadPrograms();
    } catch (error) {
        showMessage('Error deleting program: ' + error.message, 'error');
    }
}

function closeProgramForm() {
    document.getElementById('program-modal').style.display = 'none';
}

// Close modals when clicking outside
window.onclick = function(event) {
    const sewadarModal = document.getElementById('sewadar-modal');
    const programModal = document.getElementById('program-modal');
    
    if (event.target === sewadarModal) {
        closeSewadarForm();
    }
    if (event.target === programModal) {
        closeProgramForm();
    }
}

// Load initial data
loadSewadars();
