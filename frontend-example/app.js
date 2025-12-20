// API Configuration
const API_BASE_URL = 'http://localhost:8080/api';

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
    } else if (tabName === 'schedules') {
        loadSchedules();
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
                        <button class="btn btn-sm btn-primary" onclick="editSewadar(${sewadar.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteSewadar(${sewadar.id})">Delete</button>
                    </div>
                </div>
                <div class="card-body">
                    <p><strong>Department:</strong> ${sewadar.dept || 'N/A'}</p>
                    <p><strong>Mobile:</strong> ${sewadar.mobile || 'N/A'}</p>
                    <p><strong>Remarks:</strong> ${sewadar.remarks || 'N/A'}</p>
                    ${sewadar.address ? `
                        <p><strong>Address:</strong> ${sewadar.address.address1}, ${sewadar.address.address2 || ''}</p>
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

function showSewadarForm(sewadarId = null) {
    const modal = document.getElementById('sewadar-modal');
    const form = document.getElementById('sewadar-form');
    const title = document.getElementById('sewadar-form-title');
    
    form.reset();
    document.getElementById('sewadar-id').value = '';
    
    if (sewadarId) {
        title.textContent = 'Edit Sewadar';
        loadSewadarForEdit(sewadarId);
    } else {
        title.textContent = 'Add Sewadar';
    }
    
    modal.style.display = 'block';
}

async function loadSewadarForEdit(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${id}`);
        if (!response.ok) throw new Error('Failed to load sewadar');
        
        const sewadar = await response.json();
        document.getElementById('sewadar-id').value = sewadar.id;
        document.getElementById('sewadar-firstName').value = sewadar.firstName;
        document.getElementById('sewadar-lastName').value = sewadar.lastName;
        document.getElementById('sewadar-dept').value = sewadar.dept || '';
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
    
    const id = document.getElementById('sewadar-id').value;
    const data = {
        firstName: document.getElementById('sewadar-firstName').value,
        lastName: document.getElementById('sewadar-lastName').value,
        dept: document.getElementById('sewadar-dept').value,
        mobile: document.getElementById('sewadar-mobile').value,
        remarks: document.getElementById('sewadar-remarks').value
    };
    
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
        const url = id 
            ? `${API_BASE_URL}/sewadars/${id}`
            : `${API_BASE_URL}/sewadars`;
        
        const method = id ? 'PUT' : 'POST';
        
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

        showMessage(id ? 'Sewadar updated successfully!' : 'Sewadar created successfully!');
        closeSewadarForm();
        loadSewadars();
    } catch (error) {
        showMessage('Error saving sewadar: ' + error.message, 'error');
    }
}

async function editSewadar(id) {
    showSewadarForm(id);
}

async function deleteSewadar(id) {
    if (!confirm('Are you sure you want to delete this sewadar? This will also delete all associated schedules.')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/sewadars/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete sewadar');

        showMessage('Sewadar deleted successfully!');
        loadSewadars();
        loadSchedules(); // Refresh schedules in case any were deleted
    } catch (error) {
        showMessage('Error deleting sewadar: ' + error.message, 'error');
    }
}

function closeSewadarForm() {
    document.getElementById('sewadar-modal').style.display = 'none';
}

// Schedule Functions
async function loadSchedules() {
    try {
        const response = await fetch(`${API_BASE_URL}/schedules`);
        if (!response.ok) throw new Error('Failed to load schedules');
        
        const schedules = await response.json();
        const listEl = document.getElementById('schedules-list');
        
        if (schedules.length === 0) {
            listEl.innerHTML = '<p>No schedules found. Add one to get started!</p>';
            return;
        }

        listEl.innerHTML = schedules.map(schedule => `
            <div class="card">
                <div class="card-header">
                    <h3>${schedule.scheduledPlace}</h3>
                    <div class="card-actions">
                        <button class="btn btn-sm btn-primary" onclick="editSchedule(${schedule.id})">Edit</button>
                        <button class="btn btn-sm btn-danger" onclick="deleteSchedule(${schedule.id})">Delete</button>
                    </div>
                </div>
                <div class="card-body">
                    <p><strong>Date:</strong> ${schedule.scheduledDate}</p>
                    <p><strong>Time:</strong> ${schedule.scheduledTime}</p>
                    <p><strong>Medium:</strong> ${schedule.scheduledMedium || 'N/A'}</p>
                    ${schedule.attendedBy ? `
                        <p><strong>Attended By:</strong> ${schedule.attendedBy.firstName} ${schedule.attendedBy.lastName} (ID: ${schedule.attendedBy.id})</p>
                        <p><strong>Department:</strong> ${schedule.attendedBy.dept || 'N/A'}</p>
                    ` : ''}
                </div>
            </div>
        `).join('');
    } catch (error) {
        showMessage('Error loading schedules: ' + error.message, 'error');
        document.getElementById('schedules-list').innerHTML = '<p>Error loading data</p>';
    }
}

function showScheduleForm(scheduleId = null) {
    const modal = document.getElementById('schedule-modal');
    const form = document.getElementById('schedule-form');
    const title = document.getElementById('schedule-form-title');
    
    form.reset();
    document.getElementById('schedule-id').value = '';
    
    if (scheduleId) {
        title.textContent = 'Edit Schedule';
        loadScheduleForEdit(scheduleId);
    } else {
        title.textContent = 'Add Schedule';
    }
    
    modal.style.display = 'block';
}

async function loadScheduleForEdit(id) {
    try {
        const response = await fetch(`${API_BASE_URL}/schedules/${id}`);
        if (!response.ok) throw new Error('Failed to load schedule');
        
        const schedule = await response.json();
        document.getElementById('schedule-id').value = schedule.id;
        document.getElementById('schedule-place').value = schedule.scheduledPlace;
        document.getElementById('schedule-date').value = schedule.scheduledDate;
        document.getElementById('schedule-time').value = schedule.scheduledTime;
        document.getElementById('schedule-medium').value = schedule.scheduledMedium || '';
        document.getElementById('schedule-attendedBy').value = schedule.attendedBy?.id || '';
    } catch (error) {
        showMessage('Error loading schedule: ' + error.message, 'error');
    }
}

async function saveSchedule(event) {
    event.preventDefault();
    
    const id = document.getElementById('schedule-id').value;
    const data = {
        scheduledPlace: document.getElementById('schedule-place').value,
        scheduledDate: document.getElementById('schedule-date').value,
        scheduledTime: document.getElementById('schedule-time').value,
        scheduledMedium: document.getElementById('schedule-medium').value,
        attendedById: parseInt(document.getElementById('schedule-attendedBy').value)
    };

    try {
        const url = id 
            ? `${API_BASE_URL}/schedules/${id}`
            : `${API_BASE_URL}/schedules`;
        
        const method = id ? 'PUT' : 'POST';
        
        const response = await fetch(url, {
            method: method,
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        });

        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.message || 'Failed to save schedule');
        }

        showMessage(id ? 'Schedule updated successfully!' : 'Schedule created successfully!');
        closeScheduleForm();
        loadSchedules();
    } catch (error) {
        showMessage('Error saving schedule: ' + error.message, 'error');
    }
}

async function editSchedule(id) {
    showScheduleForm(id);
}

async function deleteSchedule(id) {
    if (!confirm('Are you sure you want to delete this schedule?')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE_URL}/schedules/${id}`, {
            method: 'DELETE'
        });

        if (!response.ok) throw new Error('Failed to delete schedule');

        showMessage('Schedule deleted successfully!');
        loadSchedules();
    } catch (error) {
        showMessage('Error deleting schedule: ' + error.message, 'error');
    }
}

function closeScheduleForm() {
    document.getElementById('schedule-modal').style.display = 'none';
}

// Close modals when clicking outside
window.onclick = function(event) {
    const sewadarModal = document.getElementById('sewadar-modal');
    const scheduleModal = document.getElementById('schedule-modal');
    
    if (event.target === sewadarModal) {
        closeSewadarForm();
    }
    if (event.target === scheduleModal) {
        closeScheduleForm();
    }
}

// Load initial data
loadSewadars();

