// ==========================================
// Vehicle Parking Management System - Frontend
// White & Yellow Theme + Starting Login Page
// ==========================================

const API_BASE = '/api';

// Application State
let currentUser = null;
let allSlots = [];
let selectedSlot = null;
let currentAdminBookings = [];
let currentAdminSlots = [];
let slotRefreshSequence = 0;

// Initialize on Load
document.addEventListener('DOMContentLoaded', () => {
    // Check if user session exists in localStorage
    const saved = localStorage.getItem('parkflow_user');
    if (saved) {
        try {
            currentUser = JSON.parse(saved);
        } catch (e) {
            currentUser = null;
        }
    }

    // If logged in, navigate to appropriate view; otherwise start at starting login page
    if (currentUser) {
        updateUserState();
        if (currentUser.role === 'ADMIN') {
            navigateTo('admin');
        } else {
            navigateTo('slots');
        }
    } else {
        updateUserState();
        navigateTo('auth-landing');
    }

    // Default booking date to today
    const dateInput = document.getElementById('bookingDate');
    if (dateInput) {
        dateInput.value = new Date().toISOString().split('T')[0];
    }
});

// ==========================================
// TOAST NOTIFICATIONS
// ==========================================
function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = `toast ${type}`;
    toast.classList.remove('hidden');

    setTimeout(() => {
        toast.classList.add('hidden');
    }, 4000);
}

// ==========================================
// USER AUTHENTICATION & STATE MANAGEMENT
// ==========================================
function updateUserState() {
    const loggedInState = document.getElementById('loggedInState');
    const loggedOutState = document.getElementById('loggedOutState');
    const navLinks = document.getElementById('navLinks');
    const navSlots = document.getElementById('navSlots');
    const navMyBookings = document.getElementById('navMyBookings');
    const navAdmin = document.getElementById('navAdmin');
    const userDisplayName = document.getElementById('userDisplayName');
    const userRoleTag = document.getElementById('userRoleTag');

    if (currentUser) {
        loggedInState.classList.remove('hidden');
        loggedOutState.classList.add('hidden');
        navLinks.classList.remove('hidden');

        userDisplayName.textContent = currentUser.fullName;
        userRoleTag.textContent = currentUser.role;
        userRoleTag.className = `role-tag ${currentUser.role.toLowerCase()}`;

        if (currentUser.role === 'ADMIN') {
            navAdmin.style.display = 'inline-block';
            navSlots.style.display = 'none';
            navMyBookings.style.display = 'none';
        } else {
            navAdmin.style.display = 'none';
            navSlots.style.display = 'inline-block';
            navMyBookings.style.display = 'inline-block';
        }
    } else {
        loggedInState.classList.add('hidden');
        loggedOutState.classList.remove('hidden');
        navLinks.classList.add('hidden');
        navAdmin.style.display = 'none';
    }
}

function handleBrandClick() {
    if (!currentUser) {
        navigateTo('auth-landing');
    } else if (currentUser.role === 'ADMIN') {
        navigateTo('admin');
    } else {
        navigateTo('slots');
    }
}

function showAuthLanding(tab = 'login') {
    navigateTo('auth-landing');
    switchLandingAuthTab(tab);
}

function switchLandingAuthTab(tab) {
    const tabLogin = document.getElementById('tabAuthLogin');
    const tabReg = document.getElementById('tabAuthRegister');
    const formLogin = document.getElementById('landingLoginForm');
    const formReg = document.getElementById('landingRegisterForm');

    if (tab === 'login') {
        tabLogin.classList.add('active');
        tabReg.classList.remove('active');
        formLogin.classList.remove('hidden');
        formReg.classList.add('hidden');
    } else {
        tabLogin.classList.remove('active');
        tabReg.classList.add('active');
        formLogin.classList.add('hidden');
        formReg.classList.remove('hidden');
    }
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const u = document.getElementById('loginUsername').value.trim();
    const p = document.getElementById('loginPassword').value.trim();

    try {
        const res = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username: u, password: p })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            currentUser = data.user;
            localStorage.setItem('parkflow_user', JSON.stringify(currentUser));
            updateUserState();
            showToast(`Welcome back, ${currentUser.fullName}!`, 'success');

            // Route based on role
            if (currentUser.role === 'ADMIN') {
                navigateTo('admin');
            } else {
                navigateTo('slots');
            }
        } else {
            showToast(data.error || 'Login failed. Please verify credentials.', 'error');
        }
    } catch (err) {
        showToast('Network error during login: ' + err.message, 'error');
    }
}

async function handleRegisterSubmit(e) {
    e.preventDefault();
    const fullName = document.getElementById('regFullName').value.trim();
    const username = document.getElementById('regUsername').value.trim();
    const phone = document.getElementById('regPhone').value.trim();
    const password = document.getElementById('regPassword').value.trim();

    if (!fullName) {
        showToast('Full Name cannot be blank.', 'warning');
        return;
    }
    if (!/^[0-9]{10}$/.test(phone)) {
        showToast('Phone number must contain exactly 10 digits.', 'warning');
        return;
    }
    if (password.length < 4) {
        showToast('Password must be at least 4 characters.', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ fullName, username, phone, password })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            currentUser = data.user;
            localStorage.setItem('parkflow_user', JSON.stringify(currentUser));
            updateUserState();
            showToast(`🎉 Registration successful! Welcome, ${currentUser.fullName}!`, 'success');
            navigateTo('slots');
        } else {
            showToast(data.error || 'Registration failed.', 'error');
        }
    } catch (err) {
        showToast('Network error during registration: ' + err.message, 'error');
    }
}

function quickLogin(u, p) {
    document.getElementById('loginUsername').value = u;
    document.getElementById('loginPassword').value = p;
    handleLoginSubmit(new Event('submit'));
}

function logout() {
    currentUser = null;
    localStorage.removeItem('parkflow_user');
    updateUserState();
    navigateTo('auth-landing');
    showToast('Logged out successfully.', 'info');
}

// ==========================================
// NAVIGATION & ROUTING
// ==========================================
function navigateTo(view) {
    // Hide all main sections
    document.getElementById('viewAuthLanding').classList.add('hidden');
    document.getElementById('viewSlots').classList.add('hidden');
    document.getElementById('viewMyBookings').classList.add('hidden');
    document.getElementById('viewAdmin').classList.add('hidden');

    // Reset active nav buttons
    document.querySelectorAll('.nav-btn').forEach(btn => btn.classList.remove('active'));

    if (view === 'auth-landing') {
        document.getElementById('viewAuthLanding').classList.remove('hidden');
    } else if (view === 'slots') {
        if (!currentUser) {
            navigateTo('auth-landing');
            return;
        }
        document.getElementById('viewSlots').classList.remove('hidden');
        document.getElementById('navSlots').classList.add('active');
        refreshSlots();
    } else if (view === 'my-bookings') {
        if (!currentUser) {
            navigateTo('auth-landing');
            return;
        }
        document.getElementById('viewMyBookings').classList.remove('hidden');
        document.getElementById('navMyBookings').classList.add('active');
        loadMyBookings();
    } else if (view === 'admin') {
        if (!currentUser || currentUser.role !== 'ADMIN') {
            showToast('Access Denied: Admin authorization required.', 'error');
            navigateTo('auth-landing');
            return;
        }
        document.getElementById('viewAdmin').classList.remove('hidden');
        document.getElementById('navAdmin').classList.add('active');
        loadAdminDashboard();
    }
}

// ==========================================
// 1. SLOTS SEARCH & BROWSING
// ==========================================
async function refreshSlots() {
    const requestSequence = ++slotRefreshSequence;
    try {
        const res = await fetch(`${API_BASE}/slots`, { cache: 'no-store' });
        const data = await res.json();
        if (!res.ok) {
            throw new Error(data.error || 'Unable to load parking slots.');
        }
        if (!Array.isArray(data.slots)) {
            throw new Error('The server returned an invalid parking-slot list.');
        }
        if (requestSequence !== slotRefreshSequence) return;
        allSlots = data.slots;
        handleSlotSearch();
    } catch (err) {
        if (requestSequence === slotRefreshSequence) {
            showToast('Error loading parking slots: ' + err.message, 'error');
        }
    }
}

function handleSlotSearch() {
    const q = document.getElementById('slotSearchInput').value.toLowerCase().trim();
    if (!q) {
        renderSlots(allSlots);
        return;
    }
    const filtered = allSlots.filter(s => {
        const cat = s.schedule ? s.schedule.category.toLowerCase() : '';
        return s.identifier.toLowerCase().includes(q) ||
               s.name.toLowerCase().includes(q) ||
               cat.includes(q);
    });
    renderSlots(filtered);
}

function renderSlots(slots) {
    const container = document.getElementById('slotsGrid');
    container.innerHTML = '';

    if (!slots || slots.length === 0) {
        container.innerHTML = '<div class="empty-state"><p>No parking slots found matching your criteria.</p></div>';
        return;
    }

    slots.forEach(slot => {
        const isAvail = slot.availableQuantity > 0;
        const scheduleText = slot.schedule ? `${slot.schedule.category} • ${slot.schedule.operatingHours}` : 'Standard Bay';

        const card = document.createElement('div');
        card.className = 'slot-card';
        card.innerHTML = `
            <div>
                <div class="slot-header">
                    <span class="slot-identifier">${slot.identifier}</span>
                    <span class="slot-status-tag ${isAvail ? 'available' : 'soldout'}">
                        ${isAvail ? '🟢 Available' : '🔴 Sold Out'}
                    </span>
                </div>
                <h3 class="slot-name">${slot.name}</h3>
                <p class="slot-category">${scheduleText}</p>
            </div>
            <div>
                <div class="slot-pricing-row">
                    <span class="slot-rate-label">Rate / Slot</span>
                    <span class="slot-rate-val">₹${slot.rate.toFixed(2)}</span>
                </div>
                <div class="slot-footer">
                    <span class="slot-avail-count">${slot.availableQuantity} of ${slot.totalCapacity} spots left</span>
                    <button class="btn btn-sm ${isAvail ? 'btn-primary' : 'btn-secondary'}"
                            ${!isAvail ? 'disabled' : ''}
                            onclick="openBookingModal(${slot.id})">
                        ${isAvail ? '⚡ Book Now' : 'Sold Out'}
                    </button>
                </div>
            </div>
        `;
        container.appendChild(card);
    });
}

// ==========================================
// 2. BOOKING MODAL & LIVE FEE CALCULATION
// ==========================================
function openBookingModal(slotId) {
    if (!currentUser) {
        navigateTo('auth-landing');
        return;
    }

    selectedSlot = allSlots.find(s => s.id === slotId);
    if (!selectedSlot || selectedSlot.availableQuantity <= 0) {
        showToast('Selected parking slot is not available.', 'warning');
        return;
    }

    const preview = document.getElementById('bookingSlotPreview');
    preview.innerHTML = `
        <div>
            <strong style="font-size: 1.05rem; color: var(--dark-charcoal);">🅿️ ${selectedSlot.identifier} - ${selectedSlot.name}</strong>
            <div style="font-size: 0.8rem; color: var(--text-muted);">${selectedSlot.schedule ? selectedSlot.schedule.category : 'General'}</div>
        </div>
        <div style="text-align: right;">
            <div style="font-size: 1.2rem; font-weight: 800; color: var(--dark-charcoal);">₹${selectedSlot.rate.toFixed(2)}</div>
            <div style="font-size: 0.75rem; color: var(--success); font-weight: 700;">${selectedSlot.availableQuantity} spots available</div>
        </div>
    `;

    // Prefill form
    document.getElementById('bookingQuantity').value = "1";
    document.getElementById('bookingCustomerName').value = currentUser.fullName || '';
    document.getElementById('bookingPhone').value = currentUser.phone || '';
    document.getElementById('bookingVehicle').value = 'MH-12-AB-1234';
    document.getElementById('bookingDate').value = new Date().toISOString().split('T')[0];

    calculateLiveFee();
    document.getElementById('bookingModal').classList.remove('hidden');
}

function closeBookingModal() {
    document.getElementById('bookingModal').classList.add('hidden');
}

function calculateLiveFee() {
    if (!selectedSlot) return;

    const qty = parseInt(document.getElementById('bookingQuantity').value, 10);
    const rate = selectedSlot.rate;
    const base = rate * qty;
    // Rule: ₹50 if base < ₹1000, otherwise ₹0
    const charge = (base < 1000.0) ? 50.0 : 0.0;
    const grandTotal = base + charge;

    document.getElementById('calcFormula').textContent = `${qty} × ₹${rate.toFixed(2)}`;
    document.getElementById('calcBaseAmount').textContent = `₹${base.toFixed(2)}`;
    document.getElementById('calcBookingCharge').textContent = `₹${charge.toFixed(2)}`;

    const note = document.getElementById('calcChargeNote');
    if (base < 1000.0) {
        note.textContent = '₹50 booking charge applied (Base < ₹1000)';
        note.style.color = '#d97706';
    } else {
        note.textContent = 'FREE booking charge applied (Base ≥ ₹1000)';
        note.style.color = 'var(--success)';
    }

    document.getElementById('calcGrandTotal').textContent = `₹${grandTotal.toFixed(2)}`;
}

async function handleBookingSubmit(e) {
    e.preventDefault();
    if (!currentUser || !selectedSlot) return;

    const qty = parseInt(document.getElementById('bookingQuantity').value, 10);
    const customerName = document.getElementById('bookingCustomerName').value.trim();
    const customerPhone = document.getElementById('bookingPhone').value.trim();
    const vehicleNumber = document.getElementById('bookingVehicle').value.trim();
    const bookingDate = document.getElementById('bookingDate').value;

    if (!customerName) {
        showToast('Customer name cannot be blank.', 'warning');
        return;
    }
    if (!/^[0-9]{10}$/.test(customerPhone)) {
        showToast('Phone number must contain exactly 10 digits.', 'warning');
        return;
    }
    if (!vehicleNumber) {
        showToast('Vehicle number cannot be blank.', 'warning');
        return;
    }

    const payload = {
        userId: currentUser.id,
        resourceId: selectedSlot.id,
        quantity: qty,
        customerName: customerName,
        customerPhone: customerPhone,
        vehicleNumber: vehicleNumber,
        bookingDate: bookingDate
    };

    try {
        const res = await fetch(`${API_BASE}/bookings`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (res.ok && data.success) {
            closeBookingModal();
            showToast(`🎉 Reservation ${data.reservation.bookingReference} confirmed!`, 'success');
            await refreshSlots();
            openReceiptModal(data.reservation);
        } else {
            showToast(data.error || 'Booking confirmation failed.', 'error');
        }
    } catch (err) {
        showToast('Error confirming booking: ' + err.message, 'error');
    }
}

// ==========================================
// 3. MY BOOKINGS VIEW & USER CANCELLATIONS
// ==========================================
async function loadMyBookings() {
    if (!currentUser) return;

    try {
        const res = await fetch(`${API_BASE}/bookings?userId=${currentUser.id}`);
        const data = await res.json();
        const bookings = data.bookings || [];

        const tbody = document.getElementById('myBookingsTableBody');
        const emptyState = document.getElementById('myBookingsEmpty');
        tbody.innerHTML = '';

        if (bookings.length === 0) {
            emptyState.classList.remove('hidden');
            return;
        }
        emptyState.classList.add('hidden');

        bookings.forEach(b => {
            let slotSnapshot = '-';
            let qty = 1;
            if (b.items && b.items.length > 0) {
                slotSnapshot = `${b.items[0].resourceIdentifierSnapshot} - ${b.items[0].resourceNameSnapshot}`;
                qty = b.items[0].quantity;
            }

            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>${b.bookingReference}</strong></td>
                <td>${slotSnapshot}</td>
                <td>${qty}</td>
                <td>${b.vehicleNumber}</td>
                <td>${b.bookingDate}</td>
                <td>₹${b.baseAmount.toFixed(2)}</td>
                <td>₹${b.bookingCharge.toFixed(2)}</td>
                <td><strong>₹${b.grandTotal.toFixed(2)}</strong></td>
                <td><span class="status-badge ${b.status}">${b.status}</span></td>
                <td class="text-right">
                    <button class="btn btn-sm btn-secondary" onclick="viewBookingDetails(${b.id})">🧾 Pass</button>
                    <button class="btn btn-sm btn-secondary" onclick="viewAuditHistory(${b.id}, '${b.bookingReference}')">📜 Audit</button>
                    ${b.status === 'CONFIRMED'
                        ? `<button class="btn btn-sm btn-danger" onclick="cancelUserBooking(${b.id}, '${b.bookingReference}')">❌ Cancel</button>`
                        : ''}
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showToast('Error loading user bookings: ' + err.message, 'error');
    }
}

async function cancelUserBooking(bookingId, ref) {
    if (!confirm(`Are you sure you want to cancel booking ${ref}?\nThis will restore the reserved parking slot capacity.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/bookings/${bookingId}/cancel`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId: currentUser.id })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showToast(`Booking ${ref} cancelled. Slot capacity has been restored.`, 'success');
            loadMyBookings();
            refreshSlots();
        } else {
            showToast(data.error || 'Cancellation failed', 'error');
        }
    } catch (err) {
        showToast('Error cancelling booking: ' + err.message, 'error');
    }
}

// ==========================================
// 4. ADMIN DASHBOARD & ADVANCE STATUS
// ==========================================
async function loadAdminDashboard() {
    if (!currentUser || currentUser.role !== 'ADMIN') return;

    try {
        // Load stats
        const statsRes = await fetch(`${API_BASE}/admin/stats?userId=${currentUser.id}`);
        const statsData = await statsRes.json();
        if (!statsRes.ok) {
            throw new Error(statsData.error || 'Unable to load admin data.');
        }

        if (statsData.metrics) {
            document.getElementById('statTotal').textContent = statsData.metrics.total;
            document.getElementById('statConfirmed').textContent = statsData.metrics.confirmed;
            document.getElementById('statCheckedIn').textContent = statsData.metrics.checkedIn;
            document.getElementById('statCompleted').textContent = statsData.metrics.completed;
            document.getElementById('statRevenue').textContent = `₹${statsData.metrics.revenue.toFixed(2)}`;
        }

        // Render slots overview
        if (statsData.schedules) {
            const scheduleSelect = document.getElementById('adminSpotSchedule');
            const selectedSchedule = scheduleSelect.value;
            scheduleSelect.replaceChildren();
            statsData.schedules.forEach(schedule => {
                const option = document.createElement('option');
                option.value = schedule.id;
                option.textContent = `${schedule.category} (${schedule.operatingHours})`;
                scheduleSelect.appendChild(option);
            });
            if (statsData.schedules.some(schedule => String(schedule.id) === selectedSchedule)) {
                scheduleSelect.value = selectedSchedule;
            }
        }

        if (statsData.slots) {
            currentAdminSlots = statsData.slots;
            const slotsTbody = document.getElementById('adminSlotsTableBody');
            slotsTbody.innerHTML = '';
            statsData.slots.forEach(s => {
                const occ = Math.max(0, s.totalCapacity - s.availableQuantity);
                const occRate = (s.totalCapacity > 0) ? ((occ / s.totalCapacity) * 100).toFixed(1) : '0.0';
                const tr = document.createElement('tr');
                const values = [
                    s.identifier,
                    s.name,
                    s.schedule ? `${s.schedule.category} (${s.schedule.operatingHours})` : '-',
                    `₹${s.rate.toFixed(2)}`,
                    s.totalCapacity,
                    s.availableQuantity,
                    occ,
                    `${occRate}%`,
                    s.active ? 'ACTIVE' : 'INACTIVE'
                ];
                values.forEach((value, index) => {
                    const cell = document.createElement('td');
                    cell.textContent = value;
                    if (index === 0 || index === 5) {
                        const strong = document.createElement('strong');
                        strong.textContent = value;
                        cell.replaceChildren(strong);
                    }
                    tr.appendChild(cell);
                });
                const actions = document.createElement('td');
                const editButton = document.createElement('button');
                editButton.type = 'button';
                editButton.className = 'btn btn-sm btn-secondary';
                editButton.textContent = 'Edit';
                editButton.addEventListener('click', () => editAdminSpot(s.id));
                actions.appendChild(editButton);
                tr.appendChild(actions);
                slotsTbody.appendChild(tr);
            });
        }

        // Load all bookings
        const bookingsRes = await fetch(`${API_BASE}/admin/bookings?userId=${currentUser.id}`);
        const bookingsData = await bookingsRes.json();
        currentAdminBookings = bookingsData.bookings || [];
        filterAdminBookings();

    } catch (err) {
        showToast('Error loading admin dashboard: ' + err.message, 'error');
    }
}

async function saveAdminSpot(event) {
    event.preventDefault();
    if (!currentUser || currentUser.role !== 'ADMIN') return;

    const id = document.getElementById('adminSpotId').value;
    const spot = {
        adminUserId: currentUser.id,
        identifier: document.getElementById('adminSpotIdentifier').value.trim(),
        name: document.getElementById('adminSpotName').value.trim(),
        scheduleId: Number(document.getElementById('adminSpotSchedule').value),
        rate: Number(document.getElementById('adminSpotRate').value),
        totalCapacity: Number(document.getElementById('adminSpotCapacity').value),
        active: document.getElementById('adminSpotActive').checked
    };

    try {
        const url = id
            ? `${API_BASE}/admin/slots?id=${encodeURIComponent(id)}`
            : `${API_BASE}/admin/slots`;
        const res = await fetch(url, {
            method: id ? 'PUT' : 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(spot)
        });
        const data = await res.json();
        if (!res.ok || !data.success) {
            showToast(data.error || 'Unable to save parking spot.', 'error');
            return;
        }

        showToast(data.message, 'success');
        resetAdminSpotForm();
        await loadAdminDashboard();
        refreshSlots();
    } catch (err) {
        showToast('Error saving parking spot: ' + err.message, 'error');
    }
}

function editAdminSpot(spotId) {
    if (!currentUser || currentUser.role !== 'ADMIN') return;
    const spot = currentAdminSlots.find(item => item.id === spotId);
    if (!spot) {
        showToast('Parking spot could not be found. Refresh and try again.', 'error');
        return;
    }

    document.getElementById('adminSpotId').value = spot.id;
    document.getElementById('adminSpotIdentifier').value = spot.identifier;
    document.getElementById('adminSpotName').value = spot.name;
    document.getElementById('adminSpotSchedule').value = spot.scheduleId;
    document.getElementById('adminSpotRate').value = spot.rate;
    document.getElementById('adminSpotCapacity').value = spot.totalCapacity;
    document.getElementById('adminSpotActive').checked = spot.active;
    document.getElementById('adminSlotFormTitle').textContent = `Update Parking Spot ${spot.identifier}`;
    document.getElementById('adminSpotSubmit').textContent = 'Save Changes';
    document.getElementById('adminSpotCancel').classList.remove('hidden');
    document.getElementById('adminSlotForm').scrollIntoView({ behavior: 'smooth', block: 'center' });
}

function resetAdminSpotForm() {
    const form = document.getElementById('adminSlotForm');
    form.reset();
    document.getElementById('adminSpotId').value = '';
    document.getElementById('adminSlotFormTitle').textContent = 'Add a Parking Spot';
    document.getElementById('adminSpotSubmit').textContent = 'Add Spot';
    document.getElementById('adminSpotCancel').classList.add('hidden');
    document.getElementById('adminSpotActive').checked = true;
}

function switchAdminTab(tab) {
    const tabRes = document.getElementById('tabAdminReservations');
    const tabSlots = document.getElementById('tabAdminSlots');
    const panelRes = document.getElementById('adminPanelReservations');
    const panelSlots = document.getElementById('adminPanelSlots');

    if (tab === 'reservations') {
        tabRes.classList.add('active');
        tabSlots.classList.remove('active');
        panelRes.classList.remove('hidden');
        panelSlots.classList.add('hidden');
    } else {
        tabRes.classList.remove('active');
        tabSlots.classList.add('active');
        panelRes.classList.add('hidden');
        panelSlots.classList.remove('hidden');
    }
}

function filterAdminBookings() {
    const filterStatus = document.getElementById('adminStatusFilter').value;
    const query = document.getElementById('adminSearchInput').value.toLowerCase().trim();

    const filtered = currentAdminBookings.filter(b => {
        if (filterStatus !== 'ALL' && b.status !== filterStatus) return false;
        if (query) {
            return b.bookingReference.toLowerCase().includes(query) ||
                   b.customerName.toLowerCase().includes(query) ||
                   b.vehicleNumber.toLowerCase().includes(query);
        }
        return true;
    });

    renderAdminBookings(filtered);
}

function renderAdminBookings(bookings) {
    const tbody = document.getElementById('adminBookingsTableBody');
    tbody.innerHTML = '';

    if (bookings.length === 0) {
        tbody.innerHTML = '<tr><td colspan="10" style="text-align: center; color: var(--text-muted);">No matching reservations found.</td></tr>';
        return;
    }

    bookings.forEach(b => {
        let slotSnapshot = '-';
        let qty = 1;
        if (b.items && b.items.length > 0) {
            slotSnapshot = `${b.items[0].resourceIdentifierSnapshot} - ${b.items[0].resourceNameSnapshot}`;
            qty = b.items[0].quantity;
        }

        // Determine sequential advance button
        let advanceBtn = '';
        if (b.status === 'CONFIRMED') {
            advanceBtn = `<button class="btn btn-sm btn-warning" onclick="advanceBookingStatus(${b.id}, 'CHECKED_IN', '${b.bookingReference}')">🟢 Check-In (➔ CHECKED_IN)</button>`;
        } else if (b.status === 'CHECKED_IN') {
            advanceBtn = `<button class="btn btn-sm btn-success" onclick="advanceBookingStatus(${b.id}, 'COMPLETED', '${b.bookingReference}')">🔵 Complete (➔ COMPLETED)</button>`;
        }

        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${b.bookingReference}</strong></td>
            <td>${b.customerName}</td>
            <td>${b.customerPhone}</td>
            <td>${b.vehicleNumber}</td>
            <td>${slotSnapshot}</td>
            <td>${qty}</td>
            <td>${b.bookingDate}</td>
            <td><strong>₹${b.grandTotal.toFixed(2)}</strong></td>
            <td><span class="status-badge ${b.status}">${b.status}</span></td>
            <td class="text-right">
                ${advanceBtn}
                <button class="btn btn-sm btn-secondary" onclick="viewBookingDetails(${b.id})">🧾</button>
                <button class="btn btn-sm btn-secondary" onclick="viewAuditHistory(${b.id}, '${b.bookingReference}')">📜</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function advanceBookingStatus(bookingId, targetStatus, ref) {
    if (!currentUser || currentUser.role !== 'ADMIN') return;

    if (!confirm(`Advance status for booking ${ref} to ${targetStatus}?`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/admin/status`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bookingId: bookingId,
                adminUserId: currentUser.id,
                targetStatus: targetStatus
            })
        });
        const data = await res.json();

        if (res.ok && data.success) {
            showToast(`Status updated to ${targetStatus}!`, 'success');
            loadAdminDashboard();
        } else {
            showToast(data.error || 'Failed to update status', 'error');
        }
    } catch (err) {
        showToast('Error updating status: ' + err.message, 'error');
    }
}

// ==========================================
// 5. RECEIPT / PASS MODAL
// ==========================================
async function viewBookingDetails(bookingId) {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/bookings?userId=${currentUser.id}`);
        const data = await res.json();
        const booking = (data.bookings || []).find(b => b.id === bookingId);
        if (booking) {
            openReceiptModal(booking);
        } else if (currentUser.role === 'ADMIN') {
            const adminRes = await fetch(`${API_BASE}/admin/bookings?userId=${currentUser.id}`);
            const adminData = await adminRes.json();
            const b = (adminData.bookings || []).find(x => x.id === bookingId);
            if (b) openReceiptModal(b);
        }
    } catch (err) {
        showToast('Error fetching pass details: ' + err.message, 'error');
    }
}

function openReceiptModal(res) {
    document.getElementById('receiptCreatedAt').textContent = `Created: ${res.createdAt || res.bookingDate}`;
    document.getElementById('receiptRef').textContent = res.bookingReference;

    const statusTag = document.getElementById('receiptStatus');
    statusTag.textContent = res.status;
    statusTag.className = `status-badge ${res.status}`;

    document.getElementById('receiptCustomerName').textContent = res.customerName;
    document.getElementById('receiptPhone').textContent = res.customerPhone;
    document.getElementById('receiptVehicle').textContent = res.vehicleNumber;
    document.getElementById('receiptDate').textContent = res.bookingDate;

    // Items list
    const itemsList = document.getElementById('receiptItemsList');
    itemsList.innerHTML = '';
    if (res.items) {
        res.items.forEach(item => {
            const row = document.createElement('div');
            row.className = 'receipt-item-row';
            row.innerHTML = `
                <span>${item.resourceIdentifierSnapshot} - ${item.resourceNameSnapshot} (${item.quantity} slot(s) @ ₹${item.rateSnapshot.toFixed(2)})</span>
                <span>₹${item.subtotal.toFixed(2)}</span>
            `;
            itemsList.appendChild(row);
        });
    }

    document.getElementById('receiptBaseAmount').textContent = `₹${res.baseAmount.toFixed(2)}`;
    const feeLabel = res.baseAmount < 1000.0 ? `₹${res.bookingCharge.toFixed(2)} (Base < ₹1000)` : `₹0.00 (Free for ₹1000+)`;
    document.getElementById('receiptBookingCharge').textContent = feeLabel;
    document.getElementById('receiptGrandTotal').textContent = `₹${res.grandTotal.toFixed(2)}`;

    document.getElementById('receiptModal').classList.remove('hidden');
}

function closeReceiptModal() {
    document.getElementById('receiptModal').classList.add('hidden');
}

// ==========================================
// 6. AUDIT TRAIL / STATUS HISTORY MODAL
// ==========================================
async function viewAuditHistory(bookingId, ref) {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/bookings/${bookingId}/history?userId=${currentUser.id}`);
        const data = await res.json();
        const histories = data.history || [];

        document.getElementById('historySubtitle').textContent = `Booking Reference: ${ref}`;
        const tbody = document.getElementById('historyTableBody');
        tbody.innerHTML = '';

        if (histories.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center;">No history entries recorded.</td></tr>';
        } else {
            histories.forEach((h, idx) => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${idx + 1}</td>
                    <td>${h.fromStatus ? `<span class="status-badge ${h.fromStatus}">${h.fromStatus}</span>` : 'Initial Creation'}</td>
                    <td><span class="status-badge ${h.toStatus}">${h.toStatus}</span></td>
                    <td>${h.changedByUsername || 'User #' + h.changedByUserId}</td>
                    <td><strong>${h.changedByRole}</strong></td>
                    <td>${h.changedAt || '-'}</td>
                    <td>${h.remarks || '-'}</td>
                `;
                tbody.appendChild(tr);
            });
        }

        document.getElementById('historyModal').classList.remove('hidden');
    } catch (err) {
        showToast('Error loading audit history: ' + err.message, 'error');
    }
}

function closeHistoryModal() {
    document.getElementById('historyModal').classList.add('hidden');
}
