// ==============================================================================
// REDOPS MOBILE — WEB SIMULATOR CONTROLLER & REST API CLIENT
// Phase 4: Full Multi-Module Functional Implementation
// ==============================================================================

const API_BASE = '/api/v1';
let currentAssessmentId = 'asm-acme-2026';
let authToken = null;
let apiLogCount = 0;

// Filter & state caches
let currentActiveActivityFilter = 'ALL';
let currentModalActivity = null;
let currentNotebookCategoryFilter = 'ALL';
let allAssessmentsData = [];
let allTargetsData = [];
let allFindingsData = [];
let allReconData = [];
let allNotebookData = [];
let allActivities = [];

// Initialize on page load
document.addEventListener('DOMContentLoaded', () => {
  initClock();
  setupEventListeners();
  initSlideableBars();
  authenticateDemoOperator();
});

// Slideable Horizontal Bars Controller (Left to Right / Right to Left)
function slideQuickActions(amount) {
  const bar = document.getElementById('quick-actions-bar');
  if (bar) {
    bar.scrollBy({ left: amount, behavior: 'smooth' });
    setTimeout(updateSliderArrows, 260);
  }
}

function updateSliderArrows() {
  const bar = document.getElementById('quick-actions-bar');
  const leftBtn = document.getElementById('qa-arrow-left');
  const rightBtn = document.getElementById('qa-arrow-right');
  if (!bar || !leftBtn || !rightBtn) return;

  const scrollLeft = bar.scrollLeft;
  const maxScroll = Math.max(0, bar.scrollWidth - bar.clientWidth);

  if (scrollLeft <= 3) {
    leftBtn.classList.add('disabled');
  } else {
    leftBtn.classList.remove('disabled');
  }

  if (scrollLeft >= maxScroll - 3) {
    rightBtn.classList.add('disabled');
  } else {
    rightBtn.classList.remove('disabled');
  }
}

function slideNoteChips(amount) {
  const bar = document.getElementById('notebook-filter-chips');
  if (bar) {
    bar.scrollBy({ left: amount, behavior: 'smooth' });
    setTimeout(updateNoteChipsArrows, 260);
  }
}

function updateNoteChipsArrows() {
  const bar = document.getElementById('notebook-filter-chips');
  const leftBtn = document.getElementById('note-chips-arrow-left');
  const rightBtn = document.getElementById('note-chips-arrow-right');
  if (!bar || !leftBtn || !rightBtn) return;

  const scrollLeft = bar.scrollLeft;
  const maxScroll = Math.max(0, bar.scrollWidth - bar.clientWidth);

  if (scrollLeft <= 3) {
    leftBtn.classList.add('disabled');
  } else {
    leftBtn.classList.remove('disabled');
  }

  if (scrollLeft >= maxScroll - 3) {
    rightBtn.classList.add('disabled');
  } else {
    rightBtn.classList.remove('disabled');
  }
}

function slideSubNav(id, amount) {
  const bar = document.getElementById(id);
  if (bar) {
    bar.scrollBy({ left: amount, behavior: 'smooth' });
    setTimeout(() => updateSubNavArrows(id), 260);
  }
}

function updateSubNavArrows(id) {
  const bar = document.getElementById(id);
  if (!bar) return;
  const leftBtn = document.querySelector(`[data-subnav-left="${id}"]`);
  const rightBtn = document.querySelector(`[data-subnav-right="${id}"]`);
  if (!leftBtn || !rightBtn) return;

  const scrollLeft = bar.scrollLeft;
  const maxScroll = Math.max(0, bar.scrollWidth - bar.clientWidth);

  if (scrollLeft <= 3) {
    leftBtn.classList.add('disabled');
  } else {
    leftBtn.classList.remove('disabled');
  }

  if (scrollLeft >= maxScroll - 3) {
    rightBtn.classList.add('disabled');
  } else {
    rightBtn.classList.remove('disabled');
  }
}

function initSlideableBars() {
  const bars = document.querySelectorAll('.quick-actions-bar, .activity-filter-bar, .filter-chips, .sub-nav-bar');
  bars.forEach(bar => {
    let isDown = false;
    let startX = 0;
    let scrollStart = 0;
    let hasDragged = false;
    let capturedPointerId = null;

    // Track scroll events to update arrows dynamically
    bar.addEventListener('scroll', () => {
      if (bar.id === 'quick-actions-bar') {
        updateSliderArrows();
      } else if (bar.id === 'notebook-filter-chips') {
        updateNoteChipsArrows();
      } else if (bar.id && bar.classList.contains('sub-nav-bar')) {
        updateSubNavArrows(bar.id);
      }
    }, { passive: true });

    // Pointer Events (Unified Mouse, Touch, Stylus)
    bar.addEventListener('pointerdown', (e) => {
      if (e.pointerType === 'mouse' && e.button !== 0) return;
      isDown = true;
      hasDragged = false;
      capturedPointerId = null;
      startX = e.clientX;
      scrollStart = bar.scrollLeft;
      // Do NOT setPointerCapture on pointerdown so child buttons receive click/tap
    });

    bar.addEventListener('pointermove', (e) => {
      if (!isDown) return;
      const deltaX = e.clientX - startX;
      // Engage dragging only when movement exceeds 8px
      if (!hasDragged && Math.abs(deltaX) > 8) {
        hasDragged = true;
        bar.classList.add('grabbing');
        try {
          bar.setPointerCapture(e.pointerId);
          capturedPointerId = e.pointerId;
        } catch (err) {}
      }
      if (hasDragged) {
        bar.scrollLeft = scrollStart - deltaX;
      }
    });

    const finishPointerDrag = () => {
      if (!isDown) return;
      isDown = false;
      bar.classList.remove('grabbing');
      if (capturedPointerId !== null) {
        try {
          bar.releasePointerCapture(capturedPointerId);
        } catch (err) {}
        capturedPointerId = null;
      }
      if (bar.id === 'quick-actions-bar') {
        updateSliderArrows();
      } else if (bar.id === 'notebook-filter-chips') {
        updateNoteChipsArrows();
      } else if (bar.id && bar.classList.contains('sub-nav-bar')) {
        updateSubNavArrows(bar.id);
      }
      if (hasDragged) {
        setTimeout(() => {
          hasDragged = false;
        }, 50);
      }
    };

    bar.addEventListener('pointerup', finishPointerDrag);
    bar.addEventListener('pointercancel', finishPointerDrag);

    // Suppress child button click only if genuine drag occurred (> 8px)
    bar.addEventListener('click', (e) => {
      if (hasDragged) {
        e.preventDefault();
        e.stopPropagation();
        hasDragged = false;
      }
    }, true);

    // Mouse wheel horizontal translation
    bar.addEventListener('wheel', (e) => {
      const delta = e.deltaX !== 0 ? e.deltaX : e.deltaY;
      if (delta !== 0) {
        e.preventDefault();
        bar.scrollLeft += delta * 0.9;
        if (bar.id === 'quick-actions-bar') {
          updateSliderArrows();
        } else if (bar.id === 'notebook-filter-chips') {
          updateNoteChipsArrows();
        } else if (bar.id && bar.classList.contains('sub-nav-bar')) {
          updateSubNavArrows(bar.id);
        }
      }
    }, { passive: false });
  });

  // Initial arrow update
  setTimeout(() => {
    updateSliderArrows();
    updateNoteChipsArrows();
    ['sub-nav-utilities', 'sub-nav-websec', 'sub-nav-recon'].forEach(updateSubNavArrows);
  }, 200);
  window.addEventListener('resize', () => {
    updateSliderArrows();
    updateNoteChipsArrows();
    ['sub-nav-utilities', 'sub-nav-websec', 'sub-nav-recon'].forEach(updateSubNavArrows);
  });
}

// Update Phone Status Bar Clock
function initClock() {
  const clockEl = document.getElementById('status-time');
  function updateTime() {
    const now = new Date();
    const hours = String(now.getHours()).padStart(2, '0');
    const minutes = String(now.getMinutes()).padStart(2, '0');
    if (clockEl) clockEl.textContent = `${hours}:${minutes}`;
  }
  updateTime();
  setInterval(updateTime, 30000);
}

// Setup Viewport Toggle and Controls
function setupEventListeners() {
  const toggleBtn = document.getElementById('toggle-view-btn');
  if (toggleBtn) {
    toggleBtn.addEventListener('click', () => {
      const body = document.body;
      if (body.classList.contains('mode-phone')) {
        body.classList.remove('mode-phone');
        body.classList.add('mode-desktop');
        toggleBtn.querySelector('span').textContent = 'View: Desktop Expanded';
      } else {
        body.classList.remove('mode-desktop');
        body.classList.add('mode-phone');
        toggleBtn.querySelector('span').textContent = 'View: Phone Mockup';
      }
    });
  }

  const drawerBtn = document.getElementById('toggle-api-drawer-btn');
  if (drawerBtn) drawerBtn.addEventListener('click', toggleApiDrawer);

  const resetBtn = document.getElementById('reset-demo-btn');
  if (resetBtn) {
    resetBtn.addEventListener('click', async () => {
      if (confirm('Reset RedOps database to clean baseline seed data? This will purge all dynamic records.')) {
        try {
          await apiFetch('/system/reset-demo', { method: 'POST' });
          showToast('Database reset complete. Reloading...');
          setTimeout(() => location.reload(), 600);
        } catch (err) {
          alert('Reset failed: ' + err.message);
        }
      }
    });
  }
}

async function handleClearAllData() {
  if (confirm('CRITICAL ACTION: Purge all engagements, targets, findings, notes, and evidence? This will leave the application in an empty, pristine state.')) {
    try {
      await apiFetch('/system/clear-data', { method: 'POST' });
      showToast('All assessment datasets cleared. System in pristine baseline state.');
      setTimeout(() => location.reload(), 600);
    } catch (err) {
      alert('Failed to clear data: ' + err.message);
    }
  }
}

async function handleSeedEvaluationData() {
  if (confirm('Explicitly load the university course evaluation dataset (Acme Corp Annual Security Audit)?')) {
    try {
      await apiFetch('/system/seed-demo', { method: 'POST' });
      showToast('Evaluation dataset loaded successfully.');
      setTimeout(() => location.reload(), 600);
    } catch (err) {
      alert('Failed to load dataset: ' + err.message);
    }
  }
}

function toggleApiDrawer() {
  const drawer = document.getElementById('api-drawer');
  if (drawer) drawer.classList.toggle('open');
}

// Toast Notification (Supports success, error, info)
function showToast(message, type = 'success') {
  const existing = document.querySelector('.toast-notice');
  if (existing) existing.remove();

  const toast = document.createElement('div');
  const icon = type === 'error' ? '✕' : (type === 'info' ? 'ℹ' : '✓');
  toast.className = `toast-notice ${type === 'error' ? 'toast-error' : (type === 'info' ? 'toast-info' : '')}`;
  toast.textContent = `${icon} ${message}`;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2800);
}

// Loading, Empty, and Error UI Component Renderers
function renderLoading(containerId, message = 'Accessing persistent database...') {
  const c = document.getElementById(containerId);
  if (!c) return;
  c.innerHTML = `
    <div class="module-loading-state">
      <div class="cyber-spinner"></div>
      <span>${message}</span>
    </div>
  `;
}

function renderEmpty(containerId, { icon = '🛡️', title = 'No Records Found', desc = '', actionText = null, actionFn = null }) {
  const c = document.getElementById(containerId);
  if (!c) return;
  c.innerHTML = `
    <div class="module-empty-state">
      <div class="empty-state-icon">${icon}</div>
      <div class="empty-state-title">${title}</div>
      ${desc ? `<div class="empty-state-desc">${desc}</div>` : ''}
      ${actionText && actionFn ? `<button class="btn btn-sm btn-primary" onclick="${actionFn}" style="margin-top:6px;">${actionText}</button>` : ''}
    </div>
  `;
}

function renderError(containerId, message = 'Database query failed.', retryFn = null) {
  const c = document.getElementById(containerId);
  if (!c) return;
  c.innerHTML = `
    <div class="module-error-state">
      <div class="empty-state-icon">⚠️</div>
      <div class="error-state-title">Persistent Database Query Error</div>
      <div class="error-state-desc">${message}</div>
      ${retryFn ? `<button class="btn btn-sm btn-outline" onclick="${retryFn}" style="margin-top:8px;">🔄 Retry Query</button>` : ''}
    </div>
  `;
}

async function filterFindingsBySeverity(severity) {
  await switchTab('findings');
  const select = document.getElementById('filter-finding-severity');
  if (select) {
    select.value = severity;
    handleFindingFilterChange();
    showToast(`Filtered findings: ${severity} severity`, 'info');
  }
}

// Wrapper for fetch that logs live HTTP traffic to the simulator console
async function apiFetch(endpoint, options = {}) {
  const url = `${API_BASE}${endpoint}`;
  const method = options.method || 'GET';
  const startTime = Date.now();

  const isFormData = options.body instanceof FormData;
  const headers = {
    ...(isFormData ? {} : { 'Content-Type': 'application/json' }),
    ...(authToken ? { 'Authorization': `Bearer ${authToken}` } : {}),
    ...(options.headers || {})
  };

  try {
    const response = await fetch(url, { ...options, headers });
    const duration = Date.now() - startTime;
    const isJson = response.headers.get('content-type')?.includes('application/json');
    const data = isJson ? await response.json() : await response.text();

    const payloadLog = isFormData ? '[Multipart FormData Upload]' : options.body;
    logApiCall(method, endpoint, response.status, data, duration, payloadLog);
    return data;
  } catch (err) {
    logApiCall(method, endpoint, 'ERR', { error: err.message }, Date.now() - startTime, options.body);
    throw err;
  }
}

function logApiCall(method, endpoint, status, responseData, duration, requestBody) {
  apiLogCount++;
  const counterEl = document.getElementById('api-counter');
  if (counterEl) counterEl.textContent = apiLogCount;

  const container = document.getElementById('api-log-container');
  if (!container) return;

  if (container.querySelector('.api-empty-state')) {
    container.innerHTML = '';
  }

  const entry = document.createElement('div');
  entry.className = 'api-log-entry';

  const statusClass = (status === 200 || status === 201) ? 's200' : 's403';
  const methodClass = method.toLowerCase();

  entry.innerHTML = `
    <div class="api-log-head">
      <span class="api-method ${methodClass}">${method}</span>
      <span class="api-status ${statusClass}">${status}</span>
      <span class="api-endpoint">${endpoint}</span>
      <span style="color: #64748B; margin-left: auto;">${duration}ms</span>
    </div>
    ${requestBody ? `<div class="api-payload" style="border-left: 2px solid var(--cyber-cyan);"><strong>REQ:</strong> ${typeof requestBody === 'string' ? requestBody : JSON.stringify(requestBody)}</div>` : ''}
    <div class="api-payload"><strong>RES:</strong> ${typeof responseData === 'object' ? JSON.stringify(responseData, null, 2) : responseData}</div>
  `;

  container.insertBefore(entry, container.firstChild);
}

// Relative time calculation from ISO/SQLite timestamp
function formatRelativeTime(dateStr) {
  if (!dateStr) return 'Recently';
  const date = new Date(dateStr.includes('T') ? dateStr : dateStr.replace(' ', 'T') + 'Z');
  const now = new Date();
  const diffSec = Math.floor((now - date) / 1000);

  if (isNaN(diffSec) || diffSec < 60) return 'Just now';
  if (diffSec < 3600) return `${Math.floor(diffSec / 60)}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  return `${Math.floor(diffSec / 86400)}d ago`;
}

// Authenticate Operator on startup (Zero hardcoded credentials)
async function authenticateDemoOperator() {
  const storedToken = sessionStorage.getItem('redops_auth_token');
  if (storedToken) {
    authToken = storedToken;
    try {
      const meRes = await apiFetch('/auth/me');
      if (meRes && meRes.status === 'success') {
        await loadAssessments();
        await refreshCurrentAssessmentData();
        return;
      }
    } catch (e) {
      // Stored token expired or invalid
      sessionStorage.removeItem('redops_auth_token');
      authToken = null;
    }
  }

  // No active valid session; prompt operator login
  openModal('modal-operator-login');
}

async function handleOperatorLogin(event) {
  event.preventDefault();
  const usernameInput = document.getElementById('login-username');
  const passwordInput = document.getElementById('login-password');
  const errorEl = document.getElementById('login-error-msg');

  if (errorEl) errorEl.style.display = 'none';

  const username = usernameInput ? usernameInput.value.trim() : '';
  const password = passwordInput ? passwordInput.value : '';

  if (!username || !password) {
    if (errorEl) {
      errorEl.textContent = 'Please enter both username and password.';
      errorEl.style.display = 'block';
    }
    return;
  }

  try {
    const res = await apiFetch('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password })
    });

    if (res.status === 'success' && res.token) {
      authToken = res.token;
      sessionStorage.setItem('redops_auth_token', res.token);
      if (passwordInput) passwordInput.value = '';
      closeModal('modal-operator-login');
      showToast(`Authenticated as ${res.user.username} (${res.user.role})`);
      await loadAssessments();
      await refreshCurrentAssessmentData();
    } else {
      if (errorEl) {
        errorEl.textContent = res.message || 'Invalid operator credentials.';
        errorEl.style.display = 'block';
      }
    }
  } catch (err) {
    if (errorEl) {
      errorEl.textContent = err.message || 'Authentication service unreachable.';
      errorEl.style.display = 'block';
    }
  }
}

function handleOperatorLogout() {
  sessionStorage.removeItem('redops_auth_token');
  authToken = null;
  openModal('modal-operator-login');
  showToast('Session locked. Re-authentication required.');
}

// Handle switching active assessment
async function handleAssessmentSwitch(newId) {
  currentAssessmentId = newId;
  const picker = document.getElementById('assessment-picker');
  if (picker) picker.value = newId;
  await refreshCurrentAssessmentData();
  showToast('Switched active engagement');
}

async function refreshCurrentAssessmentData() {
  await Promise.all([
    loadDashboardMetrics(),
    loadTargets(),
    loadRecon(),
    loadWireless(),
    loadFindings(),
    loadNotebook(),
    loadReport(),
    loadActivities(),
    loadReportHistory()
  ]);
}

// ==============================================================================
// SCREEN NAVIGATION
// ==============================================================================
// NAVIGATION & MODULE ROUTER
// ==============================================================================
async function switchTab(tabId) {
  // Gracefully handle activity tab navigation
  if (tabId === 'activity') {
    scrollToActivityFeed();
    return;
  }

  // Update Screen Views
  document.querySelectorAll('.app-screen').forEach(s => s.classList.remove('active'));
  const targetScreen = document.getElementById(`screen-${tabId}`);
  if (targetScreen) targetScreen.classList.add('active');

  // Update Bottom Nav Tabs
  document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
  let bottomTabKey = tabId;
  if (['assessments', 'targets', 'recon', 'wireless', 'findings', 'notebook', 'reports', 'settings'].includes(tabId)) {
    bottomTabKey = 'assessments';
  }
  const activeBtn = Array.from(document.querySelectorAll('.nav-tab')).find(btn => 
    btn.getAttribute('onclick')?.includes(bottomTabKey)
  );
  if (activeBtn) activeBtn.classList.add('active');

  // Update Sub-nav Tabs (for targets, recon, wireless)
  document.querySelectorAll('.sub-nav-tab').forEach(st => {
    st.classList.toggle('active', st.getAttribute('onclick')?.includes(tabId));
  });

  // Refresh module data when navigating
  if (tabId === 'dashboard') {
    await loadDashboardMetrics();
    await loadActivities();
  }
  else if (tabId === 'assessments') await loadAssessments();
  else if (tabId === 'targets') await loadTargets();
  else if (tabId === 'recon') await loadRecon();
  else if (tabId === 'wireless') await loadWireless();
  else if (tabId === 'findings') await loadFindings();
  else if (tabId === 'notebook') { await loadNotebook(); setTimeout(updateNoteChipsArrows, 150); }
  else if (tabId === 'reports') { await loadReport(); await loadReportHistory(); }
  else if (tabId === 'settings') { await loadSecurityStatus(); }
  else if (tabId === 'utilities-tools') {
    setTimeout(() => updateSubNavArrows('sub-nav-utilities'), 150);
    if (!state.cvssInitialized) {
      recalcCvss();
      state.cvssInitialized = true;
    }
  }
  else if (tabId === 'websec-tools') {
    setTimeout(() => updateSubNavArrows('sub-nav-websec'), 150);
  }
  else if (tabId === 'recon-tools') {
    setTimeout(() => updateSubNavArrows('sub-nav-recon'), 150);
  }
}

function scrollToActivityFeed() {
  switchTab('dashboard');
  setTimeout(() => {
    const feed = document.querySelector('.activity-section');
    if (feed) feed.scrollIntoView({ behavior: 'smooth' });
  }, 120);
}

async function loadSecurityStatus() {
  try {
    const res = await apiFetch('/system/security-status');
    if (res.status === 'success') {
      // Security status loaded and verified from backend
    }
  } catch (err) {
    console.error('Failed to load security status:', err);
  }
}

// ==============================================================================
// 4.1 DASHBOARD CONTROLLER & STATS
// ==============================================================================
async function loadDashboardMetrics() {
  try {
    const res = await apiFetch(`/dashboard/metrics?assessment_id=${currentAssessmentId}`);
    if (res.status === 'success') {
      const m = res.data.metrics;
      document.getElementById('stat-active-assessments').textContent = m.active_assessments;
      document.getElementById('stat-completed-assessments').textContent = `${m.completed_assessments} Completed • ${m.total_engagements} Total`;
      document.getElementById('stat-total-targets').textContent = m.total_targets;
      document.getElementById('stat-inscope-targets').textContent = `${m.in_scope_targets} In-Scope`;
      document.getElementById('stat-total-findings').textContent = m.total_findings;
      document.getElementById('stat-finding-status-sub').textContent = `${m.open_findings} Open • ${m.remediated_findings} Remediated`;
      document.getElementById('stat-critical-findings').textContent = m.critical_findings;

      // Severity Distribution
      const sev = m.severity_distribution;
      document.getElementById('sev-critical').textContent = sev.critical;
      document.getElementById('sev-high').textContent = sev.high;
      document.getElementById('sev-medium').textContent = sev.medium;
      document.getElementById('sev-low').textContent = sev.low;
      document.getElementById('sev-info').textContent = sev.info;

      const totalSev = (sev.critical + sev.high + sev.medium + sev.low + sev.info) || 1;
      document.getElementById('sev-bar-total-label').textContent = `${m.total_findings} Total Findings`;
      document.getElementById('sev-bar-critical').style.width = `${(sev.critical / totalSev) * 100}%`;
      document.getElementById('sev-bar-high').style.width = `${(sev.high / totalSev) * 100}%`;
      document.getElementById('sev-bar-medium').style.width = `${(sev.medium / totalSev) * 100}%`;
      document.getElementById('sev-bar-low').style.width = `${(sev.low / totalSev) * 100}%`;
      document.getElementById('sev-bar-info').style.width = `${(sev.info / totalSev) * 100}%`;
    }
  } catch (err) {
    console.error('Failed to load metrics:', err);
  }
}

// ==============================================================================
// 4.2 ENGAGEMENT MANAGEMENT CONTROLLER
// ==============================================================================
async function loadAssessments() {
  const container = document.getElementById('assessments-list');
  const picker = document.getElementById('assessment-picker');

  renderLoading('assessments-list', 'Querying authorized engagements...');

  try {
    const res = await apiFetch('/assessments');
    if (res.status === 'success') {
      allAssessmentsData = res.data;

      if (allAssessmentsData.length > 0 && !allAssessmentsData.some(a => a.id === currentAssessmentId)) {
        currentAssessmentId = allAssessmentsData[0].id;
      }

      // Update Topbar Picker
      if (picker) {
        picker.innerHTML = '';
        allAssessmentsData.forEach(asm => {
          const opt = document.createElement('option');
          opt.value = asm.id;
          opt.textContent = `${asm.client} - ${asm.name}`;
          if (asm.id === currentAssessmentId) opt.selected = true;
          picker.appendChild(opt);
        });
      }

      renderAssessmentsList(allAssessmentsData);
    } else {
      renderError('assessments-list', res.message || 'Failed to load engagements', 'loadAssessments()');
    }
  } catch (err) {
    console.error('Failed to load assessments:', err);
    renderError('assessments-list', err.message, 'loadAssessments()');
  }
}

function handleAssessmentFilterChange() {
  const query = (document.getElementById('filter-assess-search').value || '').toLowerCase();
  const status = document.getElementById('filter-assess-status').value;
  const type = document.getElementById('filter-assess-type').value;

  const filtered = allAssessmentsData.filter(asm => {
    const matchesSearch = !query ||
      (asm.name && asm.name.toLowerCase().includes(query)) ||
      (asm.client && asm.client.toLowerCase().includes(query)) ||
      (asm.description && asm.description.toLowerCase().includes(query));

    const matchesStatus = status === 'ALL' || asm.status === status;
    const matchesType = type === 'ALL' || asm.assessment_type === type;
    return matchesSearch && matchesStatus && matchesType;
  });

  renderAssessmentsList(filtered);
}

function renderAssessmentsList(list) {
  const container = document.getElementById('assessments-list');
  if (!container) return;
  container.innerHTML = '';

  if (list.length === 0) {
    renderEmpty('assessments-list', {
      icon: '🛡️',
      title: 'No Engagements Found',
      desc: 'No security assessments match your criteria. Create a new authorized engagement to begin.',
      actionText: '+ New Engagement',
      actionFn: "openModal('modal-new-assessment')"
    });
    return;
  }

  list.forEach(asm => {
    const isActive = asm.id === currentAssessmentId;
    const card = document.createElement('div');
    card.className = `item-card ${isActive ? 'highlight-target-item' : ''}`;
    card.innerHTML = `
      <div class="item-header">
        <div>
          <div class="item-title">${asm.name} ${isActive ? '<span style="font-size:10px;color:var(--cyber-cyan);">(ACTIVE)</span>' : ''}</div>
          <div style="font-size: 11px; color: var(--cyber-cyan);">${asm.client}</div>
        </div>
        <span class="item-badge ${asm.status === 'ACTIVE' ? 'badge-in-scope' : 'badge-out-of-scope'}">${asm.status}</span>
      </div>
      <div class="item-desc">${asm.description || 'Authorized security assessment.'}</div>
      ${asm.authorized_scope ? `<div style="font-size: 11px; color: var(--text-secondary); margin-top: 4px;"><strong>Scope:</strong> <code style="color:var(--cyber-cyan);">${asm.authorized_scope}</code></div>` : ''}
      <div class="item-meta">
        <span>📅 ${asm.start_date} to ${asm.end_date}</span>
        <span>📂 ${asm.assessment_type}</span>
      </div>
      <div class="card-action-bar">
        ${!isActive ? `<button class="btn-card-action" onclick="handleAssessmentSwitch('${asm.id}')">Select Active</button>` : ''}
        <button class="btn-card-action" onclick="openEditAssessment('${asm.id}')">✏️ Edit</button>
        <button class="btn-card-action btn-card-delete" onclick="deleteAssessment('${asm.id}')">🗑️ Delete</button>
      </div>
    `;
    container.appendChild(card);
  });
}

async function handleCreateAssessment(e) {
  e.preventDefault();
  const consent = document.getElementById('asm-auth-consent').checked;
  if (!consent) {
    showToast('Mandatory: You must certify authorized permission before creating an engagement.', 'error');
    return;
  }

  const name = document.getElementById('asm-name').value.trim();
  const client = document.getElementById('asm-client').value.trim();
  if (!name || !client) {
    showToast('Please provide both an Engagement Title and Client Organization.', 'error');
    return;
  }

  const submitBtn = e.target.querySelector('button[type="submit"]');
  const originalText = submitBtn ? submitBtn.textContent : '';
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Initializing...'; }

  try {
    const payload = {
      name,
      client,
      start_date: document.getElementById('asm-start').value,
      end_date: document.getElementById('asm-end').value,
      assessment_type: document.getElementById('asm-type').value,
      authorized_scope: document.getElementById('asm-scope').value,
      out_of_scope_assets: document.getElementById('asm-out-of-scope').value,
      objectives: document.getElementById('asm-objectives').value,
      scope_notes: document.getElementById('asm-notes').value,
      is_authorized: consent ? 1 : 0
    };

    const res = await apiFetch('/assessments', {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.status === 'success') {
      closeModal('modal-new-assessment');
      e.target.reset();
      currentAssessmentId = res.data.id;
      await loadAssessments();
      await refreshCurrentAssessmentData();
      showToast('Engagement created and set as active');
    } else {
      showToast(res.message || 'Failed to create engagement', 'error');
    }
  } catch (err) {
    showToast('Failed to create engagement: ' + err.message, 'error');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = originalText; }
  }
}

function openEditAssessment(id) {
  const asm = allAssessmentsData.find(a => a.id === id);
  if (!asm) return;

  document.getElementById('edit-asm-id').value = asm.id;
  document.getElementById('edit-asm-name').value = asm.name || '';
  document.getElementById('edit-asm-client').value = asm.client || '';
  document.getElementById('edit-asm-status').value = asm.status || 'ACTIVE';
  document.getElementById('edit-asm-start').value = asm.start_date || '';
  document.getElementById('edit-asm-end').value = asm.end_date || '';
  document.getElementById('edit-asm-scope').value = asm.authorized_scope || '';
  document.getElementById('edit-asm-out-of-scope').value = asm.out_of_scope_assets || '';
  document.getElementById('edit-asm-objectives').value = asm.objectives || '';

  openModal('modal-edit-assessment');
}

async function handleUpdateAssessment(e) {
  e.preventDefault();
  const id = document.getElementById('edit-asm-id').value;
  const payload = {
    name: document.getElementById('edit-asm-name').value,
    client: document.getElementById('edit-asm-client').value,
    status: document.getElementById('edit-asm-status').value,
    start_date: document.getElementById('edit-asm-start').value,
    end_date: document.getElementById('edit-asm-end').value,
    authorized_scope: document.getElementById('edit-asm-scope').value,
    out_of_scope_assets: document.getElementById('edit-asm-out-of-scope').value,
    objectives: document.getElementById('edit-asm-objectives').value
  };

  const res = await apiFetch(`/assessments/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-edit-assessment');
    await loadAssessments();
    await loadDashboardMetrics();
    showToast('Engagement parameters updated');
  }
}

async function deleteAssessment(id) {
  if (confirm('Delete this entire engagement and all associated targets, findings, and evidence?')) {
    const res = await apiFetch(`/assessments/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      showToast('Engagement deleted');
      await loadAssessments();
      if (allAssessmentsData.length > 0) {
        currentAssessmentId = allAssessmentsData[0].id;
      }
      await refreshCurrentAssessmentData();
    }
  }
}

// ==============================================================================
// 4.3 FINDINGS MANAGEMENT CONTROLLER (CRUD, CVSS, SORT, FILTER, STATUS)
// ==============================================================================
async function loadFindings() {
  renderLoading('findings-list', 'Querying vulnerability repository...');
  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/findings`);
    if (res.status === 'success') {
      allFindingsData = res.data;
      renderFindingsList(allFindingsData);
    } else {
      renderError('findings-list', res.message || 'Failed to load findings', 'loadFindings()');
    }
  } catch (err) {
    console.error('Failed to load findings:', err);
    renderError('findings-list', err.message, 'loadFindings()');
  }
}

function handleFindingFilterChange() {
  const query = (document.getElementById('filter-finding-search').value || '').toLowerCase();
  const severity = document.getElementById('filter-finding-severity').value;
  const status = document.getElementById('filter-finding-status').value;
  const sort = document.getElementById('filter-finding-sort').value;

  let filtered = allFindingsData.filter(f => {
    const matchesSearch = !query ||
      (f.title && f.title.toLowerCase().includes(query)) ||
      (f.affected_asset && f.affected_asset.toLowerCase().includes(query)) ||
      (f.cwe_id && f.cwe_id.toLowerCase().includes(query)) ||
      (f.description && f.description.toLowerCase().includes(query));

    const matchesSev = severity === 'ALL' || f.severity === severity;
    const matchesStatus = status === 'ALL' || f.status === status;
    return matchesSearch && matchesSev && matchesStatus;
  });

  if (sort === 'SEVERITY') {
    const order = { CRITICAL: 1, HIGH: 2, MEDIUM: 3, LOW: 4, INFO: 5 };
    filtered.sort((a, b) => (order[a.severity] || 6) - (order[b.severity] || 6) || (b.cvss_score - a.cvss_score));
  } else if (sort === 'TITLE') {
    filtered.sort((a, b) => a.title.localeCompare(b.title));
  } else {
    filtered.sort((a, b) => new Date(b.created_at) - new Date(a.created_at));
  }

  renderFindingsList(filtered);
}

function renderFindingsList(list) {
  const container = document.getElementById('findings-list');
  if (!container) return;
  container.innerHTML = '';

  if (list.length === 0) {
    renderEmpty('findings-list', {
      icon: '⚠️',
      title: 'No Vulnerabilities Found',
      desc: 'No findings match current search/filter. Record new findings with CVSS classification.',
      actionText: '+ Log Vulnerability',
      actionFn: "openModal('modal-new-finding')"
    });
    return;
  }

  list.forEach(f => {
    const card = document.createElement('div');
    card.className = 'item-card';

    // Attached Evidence files
    let evidenceHtml = '';
    if (f.evidence && f.evidence.length > 0) {
      evidenceHtml = `
        <div class="finding-evidence-tray">
          ${f.evidence.map(ev => `
            <span class="evidence-chip" title="SHA-256: ${ev.sha256_hash}">
              📎 ${ev.original_name} (${(ev.file_size / 1024).toFixed(1)} KB)
              <button type="button" onclick="deleteEvidence('${ev.id}')" style="background:none;border:none;color:var(--sev-critical);cursor:pointer;margin-left:4px;" title="Delete Evidence">✕</button>
            </span>
          `).join('')}
        </div>
      `;
    }

    const sevBadgeClass = (f.severity === 'CRITICAL' || f.severity === 'HIGH') ? 'badge-out-of-scope' : 'badge-in-scope';

    card.innerHTML = `
      <div class="item-header">
        <div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span style="font-family: var(--font-mono); font-size: 11px; color: var(--cyber-cyan); font-weight: bold;">${f.custom_id}</span>
            <span style="font-size: 10.5px; background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px; font-family: var(--font-mono);">CVSS ${f.cvss_score || 'N/A'}</span>
            ${f.cwe_id ? `<span style="font-size: 10px; color: var(--text-muted); font-family: var(--font-mono);">${f.cwe_id}</span>` : ''}
          </div>
          <div class="item-title" style="margin-top: 4px;">${f.title}</div>
        </div>
        <div style="display: flex; flex-direction: column; align-items: flex-end; gap: 4px;">
          <span class="item-badge ${sevBadgeClass}">${f.severity}</span>
          <button class="btn-card-action" onclick="cycleFindingStatus('${f.id}', '${f.status}')" style="font-size:9.5px;padding:2px 6px;">Status: ${f.status}</button>
        </div>
      </div>
      <div class="item-desc">${f.description}</div>
      <div style="font-size: 11px; color: var(--text-secondary); margin-top: 4px;">
        <strong>Asset:</strong> <code style="color:var(--cyber-cyan);">${f.affected_asset}</code>
      </div>
      ${f.technical_details ? `<div style="font-size: 11px; color: var(--text-muted); background: rgba(0,0,0,0.3); padding: 6px; border-radius: 4px; margin-top: 6px; font-family: var(--font-mono);"><strong>PoC:</strong> ${f.technical_details}</div>` : ''}
      ${f.remediation ? `<div style="font-size: 11px; color: var(--cyber-green); background: rgba(0,230,118,0.06); padding: 6px; border-radius: 4px; margin-top: 6px;"><strong>Remediation:</strong> ${f.remediation}</div>` : ''}
      ${evidenceHtml}
      <div class="card-action-bar">
        <button class="btn-card-action btn-card-evidence" onclick="openUploadEvidence('${f.id}', '${f.custom_id} - ${f.title.replace(/'/g, "\\'")}')">📎 Add Evidence</button>
        <button class="btn-card-action" onclick="downloadFindingReport('${f.id}')" title="Download standalone advisory report">📄 Export Report</button>
        <button class="btn-card-action" onclick="openEditFinding('${f.id}')">✏️ Edit</button>
        <button class="btn-card-action btn-card-delete" onclick="deleteFinding('${f.id}')">🗑️ Delete</button>
      </div>
    `;
    container.appendChild(card);
  });
}

async function cycleFindingStatus(id, currentStatus) {
  const nextStatus = currentStatus === 'OPEN' ? 'CONFIRMED' : (currentStatus === 'CONFIRMED' ? 'REMEDIATED' : 'OPEN');
  const res = await apiFetch(`/findings/${id}/status`, {
    method: 'PATCH',
    body: JSON.stringify({ status: nextStatus })
  });

  if (res.status === 'success') {
    await loadFindings();
    await loadDashboardMetrics();
    await loadActivities();
    showToast(`Status changed to ${nextStatus}`);
  } else {
    showToast(res.message || 'Failed to update status', 'error');
  }
}

async function handleCreateFinding(e) {
  e.preventDefault();
  const title = document.getElementById('fnd-title').value.trim();
  const affected_asset = document.getElementById('fnd-asset').value.trim();
  const cvssInput = document.getElementById('fnd-cvss').value;
  const cvss_score = parseFloat(cvssInput || '0.0');

  if (!title || !affected_asset) {
    showToast('Finding Title and Affected Asset are required.', 'error');
    return;
  }

  if (isNaN(cvss_score) || cvss_score < 0 || cvss_score > 10.0) {
    showToast('CVSS Score must be a valid number between 0.0 and 10.0', 'error');
    return;
  }

  const submitBtn = e.target.querySelector('button[type="submit"]');
  const originalText = submitBtn ? submitBtn.textContent : '';
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Recording Finding...'; }

  try {
    const payload = {
      title,
      severity: document.getElementById('fnd-sev').value,
      cvss_score,
      status: document.getElementById('fnd-status').value,
      cwe_id: document.getElementById('fnd-cwe').value.trim(),
      category: document.getElementById('fnd-category').value.trim(),
      affected_asset,
      technical_details: document.getElementById('fnd-tech').value.trim(),
      description: document.getElementById('fnd-desc').value.trim(),
      impact: document.getElementById('fnd-impact').value.trim(),
      remediation: document.getElementById('fnd-remed').value.trim()
    };

    const convertObsId = document.getElementById('fnd-convert-from-obs-id') ? document.getElementById('fnd-convert-from-obs-id').value : '';
    let res;
    if (convertObsId) {
      res = await apiFetch(`/observations/${convertObsId}/convert-to-finding`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
    } else {
      res = await apiFetch(`/assessments/${currentAssessmentId}/findings`, {
        method: 'POST',
        body: JSON.stringify(payload)
      });
    }

    if (res.status === 'success') {
      closeModal('modal-new-finding');
      if (document.getElementById('fnd-convert-from-obs-id')) {
        document.getElementById('fnd-convert-from-obs-id').value = '';
      }
      e.target.reset();
      await loadFindings();
      await loadRecon();
      await loadDashboardMetrics();
      await loadActivities();
      showToast(convertObsId ? 'Observation converted to verified finding' : 'Vulnerability finding recorded successfully');
    } else {
      showToast(res.message || 'Failed to record finding', 'error');
    }
  } catch (err) {
    showToast('Failed to record finding: ' + err.message, 'error');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = originalText; }
  }
}

function switchFindingModalTab(tab, prefix = 'fnd') {
  const panelDesc = document.getElementById(`${prefix}-panel-desc`);
  const panelTech = document.getElementById(`${prefix}-panel-tech`);
  const tabDesc = document.getElementById(`${prefix}-tab-desc`);
  const tabTech = document.getElementById(`${prefix}-tab-tech`);

  if (!panelDesc || !panelTech) return;

  if (tab === 'desc') {
    panelDesc.style.display = 'flex';
    panelTech.style.display = 'none';
    if (tabDesc) tabDesc.classList.add('active');
    if (tabTech) tabTech.classList.remove('active');
  } else {
    panelDesc.style.display = 'none';
    panelTech.style.display = 'flex';
    if (tabDesc) tabDesc.classList.remove('active');
    if (tabTech) tabTech.classList.add('active');
  }
}

function syncDefaultCvss(sevId, cvssId) {
  const sevEl = document.getElementById(sevId);
  const cvssEl = document.getElementById(cvssId);
  if (!sevEl || !cvssEl) return;
  const defaults = {
    CRITICAL: '9.5',
    HIGH: '7.8',
    MEDIUM: '5.3',
    LOW: '2.5',
    INFO: '0.0'
  };
  const val = defaults[sevEl.value];
  if (val !== undefined) {
    cvssEl.value = val;
  }
}

function openEditFinding(findingId) {
  const f = allFindingsData.find(item => item.id === findingId);
  if (!f) return;

  document.getElementById('edit-fnd-id').value = f.id;
  document.getElementById('edit-fnd-title').value = f.title || '';
  document.getElementById('edit-fnd-sev').value = f.severity || 'MEDIUM';
  document.getElementById('edit-fnd-cvss').value = f.cvss_score !== undefined ? f.cvss_score : '5.3';
  document.getElementById('edit-fnd-status').value = f.status || 'OPEN';
  document.getElementById('edit-fnd-cwe').value = f.cwe_id || '';
  document.getElementById('edit-fnd-cat').value = f.category || '';
  document.getElementById('edit-fnd-asset').value = f.affected_asset || '';
  document.getElementById('edit-fnd-tech').value = f.technical_details || '';
  document.getElementById('edit-fnd-desc').value = f.description || '';
  document.getElementById('edit-fnd-remed').value = f.remediation || '';
  const impactEl = document.getElementById('edit-fnd-impact');
  if (impactEl) impactEl.value = f.impact || '';

  switchFindingModalTab('desc', 'edit-fnd');
  openModal('modal-edit-finding');
}

async function handleUpdateFinding(e) {
  e.preventDefault();
  const id = document.getElementById('edit-fnd-id').value;
  const impactEl = document.getElementById('edit-fnd-impact');
  const payload = {
    title: document.getElementById('edit-fnd-title').value,
    severity: document.getElementById('edit-fnd-sev').value,
    cvss_score: parseFloat(document.getElementById('edit-fnd-cvss').value || '0.0'),
    status: document.getElementById('edit-fnd-status').value,
    cwe_id: document.getElementById('edit-fnd-cwe').value,
    category: document.getElementById('edit-fnd-cat').value,
    affected_asset: document.getElementById('edit-fnd-asset').value,
    technical_details: document.getElementById('edit-fnd-tech').value,
    description: document.getElementById('edit-fnd-desc').value,
    impact: impactEl ? impactEl.value : '',
    remediation: document.getElementById('edit-fnd-remed').value
  };

  const res = await apiFetch(`/findings/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-edit-finding');
    await loadFindings();
    await loadDashboardMetrics();
    showToast('Finding updated');
  }
}

async function deleteFinding(id) {
  if (confirm('Delete this vulnerability finding and associated evidence files?')) {
    const res = await apiFetch(`/findings/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadFindings();
      await loadDashboardMetrics();
      await loadActivities();
      showToast('Finding deleted');
    }
  }
}

// ==============================================================================
// 4.4 ASSESSMENT NOTEBOOK CONTROLLER
// ==============================================================================
async function loadNotebook() {
  renderLoading('notebook-list', 'Accessing field notes repository...');
  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/notes`);
    if (res.status === 'success') {
      allNotebookData = res.data;
      handleNoteFilterChange();
    } else {
      renderError('notebook-list', res.message || 'Failed to load field notes', 'loadNotebook()');
    }
  } catch (err) {
    console.error('Failed to load notebook:', err);
    renderError('notebook-list', err.message, 'loadNotebook()');
  }
}

function filterNotebookCategory(cat, btn) {
  currentNotebookCategoryFilter = cat;
  if (btn) {
    document.querySelectorAll('#screen-notebook .filter-chips .chip').forEach(c => c.classList.remove('active'));
    btn.classList.add('active');
  }
  handleNoteFilterChange();
}

function handleNoteFilterChange() {
  const query = (document.getElementById('filter-note-search')?.value || '').toLowerCase().trim();
  const filterCat = (currentNotebookCategoryFilter || 'ALL').toUpperCase().trim();

  const filtered = allNotebookData.filter(n => {
    const matchesSearch = !query ||
      (n.title && n.title.toLowerCase().includes(query)) ||
      (n.content && n.content.toLowerCase().includes(query)) ||
      (n.tags && n.tags.toLowerCase().includes(query)) ||
      (n.category && n.category.toLowerCase().includes(query));

    const noteCat = (n.category || '').toUpperCase().trim();
    const matchesCategory = filterCat === 'ALL' ||
      noteCat === filterCat ||
      (filterCat === 'OBSERVATION' && (noteCat === 'OBSERVATION' || noteCat === 'OBSERVATIONS')) ||
      (filterCat === 'CHECKLIST' && (noteCat === 'CHECKLIST' || noteCat === 'CHECKLISTS')) ||
      (filterCat === 'COMMANDS' && (noteCat === 'COMMAND' || noteCat === 'COMMANDS')) ||
      (filterCat === 'CREDENTIALS' && (noteCat === 'CREDENTIAL' || noteCat === 'CREDENTIALS'));

    return matchesSearch && matchesCategory;
  });

  renderNotebookList(filtered);
}

function openNewNoteWithCategory(cat) {
  openModal('modal-new-note');
  const catSelect = document.getElementById('note-category');
  if (catSelect && cat && cat !== 'ALL') {
    catSelect.value = cat.toUpperCase();
  }
}

function renderNotebookList(list) {
  const container = document.getElementById('notebook-list');
  if (!container) return;
  container.innerHTML = '';

  if (list.length === 0) {
    const catLabel = currentNotebookCategoryFilter !== 'ALL' ? currentNotebookCategoryFilter : '';
    renderEmpty('notebook-list', {
      icon: '📓',
      title: catLabel ? `No ${catLabel} Notes Found` : 'Assessment Notebook Empty',
      desc: catLabel
        ? `No notes currently match category "${catLabel}". You can add one below.`
        : 'No field notes match your search/filter. Record new technical notes, methodologies, or commands.',
      actionText: '+ New Field Note',
      actionFn: `openNewNoteWithCategory('${currentNotebookCategoryFilter || "OBSERVATION"}')`
    });
    return;
  }

  list.forEach(note => {
    const card = document.createElement('div');
    card.className = `note-card ${note.is_pinned ? 'pinned' : ''}`;

    let tagsHtml = '';
    if (note.tags) {
      tagsHtml = `
        <div class="note-tags-row">
          ${note.tags.split(',').map(t => `<span class="note-tag">${t.trim()}</span>`).join('')}
        </div>
      `;
    }

    card.innerHTML = `
      <div class="note-header">
        <div>
          <div style="display:flex;align-items:center;gap:6px;">
            <span class="item-badge badge-in-scope" style="font-size:9.5px;">${note.category}</span>
            ${note.is_pinned ? '<span style="font-size:11px;color:var(--cyber-cyan);">📌 PINNED</span>' : ''}
            ${note.is_favorite ? '<span style="font-size:12px;color:var(--sev-medium);">★</span>' : ''}
          </div>
          <div class="note-title" style="margin-top: 4px;">${note.title}</div>
        </div>
        <div style="display: flex; gap: 4px;">
          <button class="btn-card-action" onclick="togglePinNote('${note.id}')" title="Pin / Unpin">${note.is_pinned ? '📌' : '📍'}</button>
          <button class="btn-card-action" onclick="toggleFavoriteNote('${note.id}')" title="Favorite">${note.is_favorite ? '★' : '☆'}</button>
        </div>
      </div>
      <div class="note-content-box">${note.content}</div>
      ${tagsHtml}
      <div class="card-action-bar">
        <button class="btn-card-action" onclick="copyNoteContent('${note.id}')">📋 Copy Content</button>
        <button class="btn-card-action" onclick="openEditNote('${note.id}')">✏️ Edit</button>
        <button class="btn-card-action btn-card-delete" onclick="deleteNote('${note.id}')">🗑️ Delete</button>
      </div>
    `;
    container.appendChild(card);
  });
}

function copyNoteContent(id) {
  const note = allNotebookData.find(n => n.id === id);
  if (!note) return;
  navigator.clipboard.writeText(note.content).then(() => {
    showToast('Note content copied to clipboard');
  }).catch(() => {
    showToast('Note content copied to clipboard');
  });
}

async function togglePinNote(id) {
  const res = await apiFetch(`/notes/${id}/pin`, { method: 'PATCH' });
  if (res.status === 'success') {
    await loadNotebook();
    showToast(res.is_pinned ? 'Note pinned to top' : 'Note unpinned');
  } else {
    showToast(res.message || 'Failed to update note pin', 'error');
  }
}

async function toggleFavoriteNote(id) {
  const res = await apiFetch(`/notes/${id}/favorite`, { method: 'PATCH' });
  if (res.status === 'success') {
    await loadNotebook();
    showToast(res.is_favorite ? 'Added to favorites' : 'Removed from favorites');
  } else {
    showToast(res.message || 'Failed to update favorite', 'error');
  }
}

async function handleCreateNote(e) {
  e.preventDefault();
  const title = document.getElementById('note-title').value.trim();
  const content = document.getElementById('note-content').value.trim();

  if (!title || !content) {
    showToast('Note Title and Content are required.', 'error');
    return;
  }

  const submitBtn = e.target.querySelector('button[type="submit"]');
  const originalText = submitBtn ? submitBtn.textContent : '';
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Saving Note...'; }

  try {
    const payload = {
      title,
      content,
      category: document.getElementById('note-category').value,
      tags: document.getElementById('note-tags').value.trim(),
      is_pinned: document.getElementById('note-pinned').checked ? 1 : 0
    };

    const res = await apiFetch(`/assessments/${currentAssessmentId}/notes`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.status === 'success') {
      closeModal('modal-new-note');
      e.target.reset();
      await loadNotebook();
      await loadDashboardMetrics();
      await loadActivities();
      showToast('Field note saved to persistent notebook');
    } else {
      showToast(res.message || 'Failed to save note', 'error');
    }
  } catch (err) {
    showToast('Failed to save note: ' + err.message, 'error');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = originalText; }
  }
}

function openEditNote(id) {
  const n = allNotebookData.find(item => item.id === id);
  if (!n) return;

  document.getElementById('edit-note-id').value = n.id;
  document.getElementById('edit-note-title').value = n.title;
  document.getElementById('edit-note-content').value = n.content;
  document.getElementById('edit-note-category').value = n.category;
  document.getElementById('edit-note-tags').value = n.tags || '';

  openModal('modal-edit-note');
}

async function handleUpdateNote(e) {
  e.preventDefault();
  const id = document.getElementById('edit-note-id').value;
  const payload = {
    title: document.getElementById('edit-note-title').value,
    content: document.getElementById('edit-note-content').value,
    category: document.getElementById('edit-note-category').value,
    tags: document.getElementById('edit-note-tags').value
  };

  const res = await apiFetch(`/notes/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-edit-note');
    await loadNotebook();
    showToast('Field note updated');
  }
}

async function deleteNote(id) {
  if (confirm('Delete this field note?')) {
    const res = await apiFetch(`/notes/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadNotebook();
      await loadDashboardMetrics();
      showToast('Note deleted');
    }
  }
}

// ==============================================================================
// 4.5 ACTIVITY FEED CONTROLLER
// ==============================================================================
async function loadActivities() {
  try {
    const res = await apiFetch(`/activities?assessment_id=${currentAssessmentId}`);
    if (res.status === 'success') {
      allActivities = res.data;
      renderActivityFeed();
    }
  } catch (err) {
    console.error('Failed to load activity logs:', err);
  }
}

function filterActivityFeed(category, pillEl) {
  currentActiveActivityFilter = category;
  if (pillEl) {
    document.querySelectorAll('.activity-filter-pill').forEach(p => p.classList.remove('active'));
    pillEl.classList.add('active');
  }
  renderActivityFeed();
}

function getSectionDisplayName(section) {
  switch (section) {
    case 'assessments': return 'Scope & ROE';
    case 'targets': return 'Targets';
    case 'findings': return 'Findings';
    case 'wireless': return 'Wireless';
    case 'recon': return 'Recon';
    case 'notebook': return 'Notebook';
    case 'reports': return 'Reports';
    default: return 'Module';
  }
}

function renderActivityFeed() {
  const container = document.getElementById('activity-feed');
  if (!container) return;
  container.innerHTML = '';

  const filtered = allActivities.filter(act => {
    if (currentActiveActivityFilter === 'ALL') return true;
    return act.type === currentActiveActivityFilter;
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div style="text-align: center; padding: 20px; color: var(--text-muted); font-size: 12px; font-family: var(--font-mono);">
        No activity records for this filter category.
      </div>
    `;
    return;
  }

  filtered.forEach(act => {
    const card = document.createElement('div');
    card.className = 'activity-card';
    card.setAttribute('role', 'button');
    card.setAttribute('tabindex', '0');
    card.title = `Click to navigate to ${getSectionDisplayName(act.target_section)} & view descriptions`;
    card.onclick = (e) => {
      if (e.target.closest('.stop-bubble')) return;
      handleActivityClick(act.id);
    };
    card.onkeydown = (e) => {
      if (e.key === 'Enter' || e.key === ' ') {
        e.preventDefault();
        handleActivityClick(act.id);
      }
    };

    const relTime = formatRelativeTime(act.created_at);
    const sectionName = getSectionDisplayName(act.target_section);

    card.innerHTML = `
      <div class="activity-card-top">
        <div class="activity-card-main">
          <div class="activity-icon-box" style="background: ${act.color || 'rgba(0, 230, 118, 0.2)'};">${act.icon || '🛡️'}</div>
          <div class="activity-details">
            <div class="activity-desc">${act.title}</div>
            <div class="activity-meta-row">
              <span class="badge-roe-status ${act.badge_class || 'badge-roe-auth'}">${act.type}</span>
              <span class="activity-meta">• ${relTime}</span>
            </div>
          </div>
        </div>
        <div class="activity-nav-pill">
          <span>Go to ${sectionName}</span>
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
            <polyline points="9 18 15 12 9 6"></polyline>
          </svg>
        </div>
      </div>
      ${act.description ? `
        <div class="activity-desc-snippet" title="Activity description preview. Click to inspect in ${sectionName}.">
          <span style="color: var(--cyber-cyan); font-weight: 600;">Description:</span> ${act.description}
        </div>
      ` : ''}
      <div class="activity-card-footer">
        <span class="activity-footer-hint">Click card to jump to ${sectionName} & inspect record ➔</span>
      </div>
    `;
    container.appendChild(card);
  });
}

async function handleActivityClick(actId) {
  const act = allActivities.find(a => a.id === actId);
  if (!act) return;

  const targetSection = act.target_section || 'dashboard';
  const sectionName = getSectionDisplayName(targetSection);

  showToast(`Navigating to ${sectionName} — showing descriptions...`);

  // 1. Switch tab and await data fetch and DOM rendering
  await switchTab(targetSection);

  // 2. Render prominent Context Banner with full descriptions
  showActivityContextBanner(act);

  // 3. Highlight and reveal target item with descriptions
  highlightAndRevealTargetItem(act);
}

function showActivityContextBanner(act) {
  const targetScreen = document.getElementById(`screen-${act.target_section}`);
  if (!targetScreen) return;

  const existing = targetScreen.querySelector('.activity-context-banner');
  if (existing) existing.remove();

  const relTime = formatRelativeTime(act.created_at);
  const banner = document.createElement('div');
  banner.className = 'activity-context-banner';
  banner.id = 'active-context-banner';
  banner.innerHTML = `
    <div class="context-banner-header">
      <span class="context-banner-tag">
        <span>${act.icon || '🛡️'}</span>
        <span>NAVIGATED FROM RECENT ENGAGEMENT ACTIVITY • ${act.type} (${relTime})</span>
      </span>
      <button class="btn-icon" onclick="dismissContextBanner()" title="Dismiss Banner" style="color: var(--text-muted); font-size: 16px;">✕</button>
    </div>
    <div class="context-banner-title">${act.title}</div>
    <div class="context-banner-desc-box">
      <div class="desc-box-label">DOCUMENTED ACTIVITY DESCRIPTION:</div>
      <div class="desc-box-content">${act.description || 'Authorized activity executed and documented in database.'}</div>
    </div>
    <div class="context-banner-meta">
      ${act.asset ? `<span><strong>Asset:</strong> <code>${act.asset}</code></span>` : ''}
      ${act.rules_of_engagement ? `<span><strong>ROE:</strong> <span class="text-green">${act.rules_of_engagement}</span></span>` : ''}
      ${act.auth_reference ? `<span><strong>Auth Ref:</strong> <span class="text-cyan">${act.auth_reference}</span></span>` : ''}
      ${act.operator_title ? `<span><strong>Operator:</strong> <span class="text-muted">${act.operator_title}</span></span>` : ''}
    </div>
    <div class="context-banner-btn-row">
      <button class="btn btn-sm btn-outline" type="button" onclick="switchTab('dashboard')">← Return to Dashboard</button>
      <button class="btn btn-sm btn-outline" type="button" onclick="openActivityModal('${act.id}')">Inspect Full ROE Record ℹ️</button>
      <button class="btn btn-sm btn-outline" type="button" onclick="dismissContextBanner()">Dismiss Banner ✕</button>
    </div>
  `;

  const header = targetScreen.querySelector('.screen-header');
  if (header && header.nextSibling) {
    targetScreen.insertBefore(banner, header.nextSibling);
  } else {
    targetScreen.prepend(banner);
  }

  // Scroll to banner / top
  const viewport = document.querySelector('.app-viewport');
  if (viewport) {
    viewport.scrollTo({ top: 0, behavior: 'smooth' });
  }
}

function dismissContextBanner() {
  document.querySelectorAll('.activity-context-banner').forEach(b => b.remove());
  document.querySelectorAll('.highlight-target-item').forEach(c => c.classList.remove('highlight-target-item'));
  document.querySelectorAll('.target-focus-pill').forEach(p => p.remove());
  document.querySelectorAll('.focused-activity-desc-callout').forEach(p => p.remove());
}

function highlightAndRevealTargetItem(act) {
  const activeScreen = document.querySelector('.app-screen.active');
  if (!activeScreen) return;

  // Clear existing highlights and focus pills
  document.querySelectorAll('.highlight-target-item').forEach(c => c.classList.remove('highlight-target-item'));
  document.querySelectorAll('.target-focus-pill').forEach(p => p.remove());
  document.querySelectorAll('.focused-activity-desc-callout').forEach(p => p.remove());

  const cards = activeScreen.querySelectorAll('.item-card, .note-card');
  if (cards.length === 0) return;

  let targetCard = null;
  const kw = (act.target_keyword || '').toLowerCase().trim();
  const asset = (act.asset || '').toLowerCase().trim();
  const titleTokens = (act.title || '').toLowerCase().split(/[\s:,\-_/]+/).filter(t => t.length > 2);

  // Strategy 1: Match target_keyword
  if (kw) {
    for (const card of cards) {
      if (card.textContent.toLowerCase().includes(kw)) {
        targetCard = card;
        break;
      }
    }
  }

  // Strategy 2: Match asset
  if (!targetCard && asset) {
    for (const card of cards) {
      if (card.textContent.toLowerCase().includes(asset)) {
        targetCard = card;
        break;
      }
    }
  }

  // Strategy 3: Match tokens
  if (!targetCard) {
    for (const token of titleTokens) {
      for (const card of cards) {
        if (card.textContent.toLowerCase().includes(token)) {
          targetCard = card;
          break;
        }
      }
      if (targetCard) break;
    }
  }

  // Fallback: first card
  if (!targetCard && cards.length > 0) {
    targetCard = cards[0];
  }

  if (targetCard) {
    targetCard.classList.add('highlight-target-item');

    // Add glowing focus badge
    const focusPill = document.createElement('div');
    focusPill.className = 'target-focus-pill';
    focusPill.innerHTML = `<span>🎯 FOCUSED RECORD • ${act.type} (Referenced in Recent Activity)</span>`;
    targetCard.prepend(focusPill);

    // Ensure description is expanded and visible with zero truncation
    const descEls = targetCard.querySelectorAll('.item-desc, .note-content, .note-content-box');
    descEls.forEach(descEl => {
      descEl.style.webkitLineClamp = 'unset';
      descEl.style.maxHeight = 'none';
      descEl.style.display = 'block';
    });

    // Insert an explicit highlighted callout inside the card showing the activity's description
    if (act.description) {
      const callout = document.createElement('div');
      callout.className = 'focused-activity-desc-callout';
      callout.innerHTML = `
        <span class="desc-badge">RECENT ACTIVITY DESCRIPTION:</span>
        <div style="margin-top: 4px; line-height: 1.45;">${act.description}</div>
      `;
      const descEl = targetCard.querySelector('.item-desc, .note-content-box') || targetCard.querySelector('.item-header');
      if (descEl && descEl.nextSibling) {
        targetCard.insertBefore(callout, descEl.nextSibling);
      } else {
        targetCard.appendChild(callout);
      }
    }

    // Scroll smoothly to target card in .app-viewport
    setTimeout(() => {
      const viewport = document.querySelector('.app-viewport');
      if (viewport) {
        const offset = (targetCard.offsetTop || 0) - 80;
        viewport.scrollTo({ top: Math.max(0, offset), behavior: 'smooth' });
      }
      targetCard.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 120);
  }
}

function openActivityModal(actId) {
  const act = allActivities.find(a => a.id === actId);
  if (!act) return;

  currentModalActivity = act;
  document.getElementById('act-modal-icon').textContent = act.icon || '🛡️';
  document.getElementById('act-modal-title').textContent = act.title;
  document.getElementById('act-modal-type').textContent = act.type;
  document.getElementById('act-modal-time').textContent = formatRelativeTime(act.created_at);
  document.getElementById('act-modal-desc').textContent = act.description;
  document.getElementById('act-modal-auth').textContent = act.auth_reference || 'ROE Verified';
  document.getElementById('act-modal-roe-stmt').textContent = act.rules_of_engagement || 'Strictly Non-Disruptive';
  document.getElementById('act-modal-asset').textContent = act.asset || 'Target Asset';
  document.getElementById('act-modal-operator').textContent = act.operator_title || 'Authorized Operator';

  openModal('modal-activity-detail');
}

function jumpFromActivityModal() {
  closeModal('modal-activity-detail');
  if (currentModalActivity) {
    handleActivityClick(currentModalActivity.id);
  }
}

// ==============================================================================
// 4.6 EVIDENCE MANAGEMENT CONTROLLER
// ==============================================================================
function openUploadEvidence(findingId, findingTitle) {
  document.getElementById('ev-finding-id').value = findingId;
  document.getElementById('ev-finding-title').value = findingTitle;
  document.getElementById('ev-file-input').value = '';
  document.getElementById('ev-desc').value = '';
  openModal('modal-upload-evidence');
}

async function handleUploadEvidence(e) {
  e.preventDefault();
  const findingId = document.getElementById('ev-finding-id').value;
  const fileInput = document.getElementById('ev-file-input');
  const desc = document.getElementById('ev-desc').value;

  if (!fileInput.files || fileInput.files.length === 0) {
    alert('Please select a file to upload.');
    return;
  }

  const formData = new FormData();
  formData.append('evidence_file', fileInput.files[0]);
  if (desc) formData.append('description', desc);

  try {
    const res = await apiFetch(`/findings/${findingId}/evidence`, {
      method: 'POST',
      body: formData
    });

    if (res.status === 'success') {
      closeModal('modal-upload-evidence');
      showToast(`Evidence attached. SHA-256: ${res.data.sha256_hash.slice(0, 12)}...`);
      await loadFindings();
      await loadActivities();
    }
  } catch (err) {
    alert('Failed to upload evidence: ' + err.message);
  }
}

async function deleteEvidence(id) {
  if (confirm('Delete this cryptographic evidence artifact?')) {
    const res = await apiFetch(`/evidence/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadFindings();
      showToast('Evidence deleted');
    }
  }
}

// ==============================================================================
// 4.7 REPORT GENERATION & AUDIT HISTORY (PREVIEW, TXT DOWNLOAD & PDF PRINTING)
// ==============================================================================
let activeModalReportType = 'FULL'; // 'FULL' or 'FINDING'
let activeModalFindingId = null;

// Isolated, Clean Printing Engine for Native PDF Generation
function printHtmlReport(title, htmlContent) {
  const printFrame = document.createElement('iframe');
  printFrame.style.position = 'fixed';
  printFrame.style.right = '0';
  printFrame.style.bottom = '0';
  printFrame.style.width = '0';
  printFrame.style.height = '0';
  printFrame.style.border = '0';
  document.body.appendChild(printFrame);

  const frameDoc = printFrame.contentWindow.document;
  frameDoc.open();
  frameDoc.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>${title}</title>
      <style>
        @page { size: A4 portrait; margin: 18mm 16mm 18mm 16mm; }
        body {
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
          color: #111827;
          background: #ffffff;
          padding: 0;
          margin: 0;
          font-size: 11pt;
          line-height: 1.5;
        }
        .print-header {
          border-bottom: 2px solid #0284c7;
          padding-bottom: 12px;
          margin-bottom: 18px;
        }
        .print-brand {
          font-size: 9pt;
          font-weight: 700;
          letter-spacing: 1.5px;
          color: #0284c7;
          text-transform: uppercase;
        }
        .print-title {
          font-size: 18pt;
          font-weight: 800;
          color: #0f172a;
          margin-top: 4px;
        }
        .print-meta {
          font-size: 9.5pt;
          color: #4b5563;
          margin-top: 6px;
        }
        .print-classification {
          display: inline-block;
          font-size: 8.5pt;
          font-weight: 700;
          color: #b91c1c;
          background: #fef2f2;
          border: 1px solid #fecaca;
          padding: 2px 8px;
          border-radius: 4px;
          margin-top: 8px;
        }
        .print-section {
          margin-bottom: 16px;
          page-break-inside: avoid;
        }
        .print-section-title {
          font-size: 11.5pt;
          font-weight: 700;
          color: #0369a1;
          border-bottom: 1px solid #e5e7eb;
          padding-bottom: 4px;
          margin-bottom: 8px;
          text-transform: uppercase;
          letter-spacing: 0.5px;
        }
        pre, code {
          font-family: "JetBrains Mono", Consolas, "Courier New", monospace;
          font-size: 9pt;
          background: #f8fafc;
          border: 1px solid #e2e8f0;
          border-radius: 4px;
          padding: 8px 10px;
          white-space: pre-wrap;
          word-break: break-all;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin-top: 8px;
          font-size: 9.5pt;
        }
        th, td {
          border: 1px solid #e5e7eb;
          padding: 6px 10px;
          text-align: left;
        }
        th {
          background: #f1f5f9;
          font-weight: 600;
          color: #334155;
        }
        .badge {
          display: inline-block;
          font-size: 8.5pt;
          font-weight: 700;
          padding: 2px 6px;
          border-radius: 3px;
        }
        .badge-critical { background: #fee2e2; color: #991b1b; }
        .badge-high { background: #ffedd5; color: #9a3412; }
        .badge-medium { background: #fef9c3; color: #854d0e; }
        .badge-low { background: #e0f2fe; color: #075985; }
        .badge-info { background: #f3f4f6; color: #374151; }
        .footer {
          margin-top: 24px;
          border-top: 1px solid #e5e7eb;
          padding-top: 8px;
          font-size: 8pt;
          color: #9ca3af;
          text-align: center;
        }
      </style>
    </head>
    <body>
      ${htmlContent}
      <div class="footer">
        Confidential Assessment Documentation • Generated via RedOps Mobile Platform • Cryptographically Logged Audit Record
      </div>
    </body>
    </html>
  `);
  frameDoc.close();

  printFrame.contentWindow.focus();
  setTimeout(() => {
    printFrame.contentWindow.print();
    setTimeout(() => {
      try { document.body.removeChild(printFrame); } catch(e) {}
    }, 2500);
  }, 400);
}

// Full Engagement Report Preview Modal
async function previewFullReport() {
  activeModalReportType = 'FULL';
  activeModalFindingId = null;

  const titleEl = document.getElementById('report-modal-title');
  const bodyEl = document.getElementById('report-modal-body');
  if (!bodyEl) return;

  renderLoading('report-modal-body', 'Compiling executive assessment report...');
  openModal('modal-report-preview');

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/report`);
    if (res.status === 'success') {
      const rep = res.data;
      if (titleEl) titleEl.textContent = `${rep.metadata.report_title} — Full Preview`;

      let html = `
        <div class="report-header">
          <div class="report-title-lg" style="font-size: 14px;">${rep.metadata.report_title}</div>
          <div style="font-size: 11px; color: var(--text-secondary); margin-top: 4px;">
            Target Client: <strong>${rep.metadata.client}</strong> | Period: ${rep.metadata.assessment_period}
          </div>
          <div style="font-size: 10px; color: var(--sev-critical); margin-top: 4px; font-weight: bold; font-family: var(--font-mono);">
            ${rep.metadata.classification}
          </div>
        </div>
      `;

      for (const [sectionTitle, content] of Object.entries(rep.sections)) {
        html += `
          <div class="report-section">
            <div class="report-section-title">${sectionTitle}</div>
            <div>${typeof content === 'string' ? `<p style="margin:0;line-height:1.5;">${content}</p>` : `<pre style="font-family: var(--font-mono); font-size: 10.5px; background: rgba(0,0,0,0.3); padding: 8px; border-radius: 4px; overflow-x: auto; margin:0;">${JSON.stringify(content, null, 2)}</pre>`}</div>
          </div>
        `;
      }

      bodyEl.innerHTML = html;
    } else {
      renderError('report-modal-body', res.message || 'Failed to compile report', 'previewFullReport()');
    }
  } catch (err) {
    console.error('Failed to preview report:', err);
    renderError('report-modal-body', err.message, 'previewFullReport()');
  }
}

// Single Finding Advisory Preview Modal
async function previewFindingReport(findingId) {
  if (!findingId) return;
  activeModalReportType = 'FINDING';
  activeModalFindingId = findingId;

  const titleEl = document.getElementById('report-modal-title');
  const bodyEl = document.getElementById('report-modal-body');
  if (!bodyEl) return;

  renderLoading('report-modal-body', 'Compiling single finding vulnerability advisory...');
  openModal('modal-report-preview');

  try {
    const res = await apiFetch(`/findings/${findingId}/report`);
    if (res.status === 'success') {
      const rep = res.data;
      const f = rep.finding;
      const asm = rep.assessment || {};
      const evList = rep.evidence || [];

      if (titleEl) titleEl.textContent = `[${f.custom_id}] ${f.title} — Advisory Preview`;

      const sevClass = (f.severity === 'CRITICAL' || f.severity === 'HIGH') ? 'badge-out-of-scope' : 'badge-in-scope';

      let evidenceRows = '';
      if (evList.length === 0) {
        evidenceRows = '<tr><td colspan="4" style="text-align:center;color:var(--text-muted);padding:8px;font-size:10.5px;">No cryptographic evidence files attached to this finding.</td></tr>';
      } else {
        evidenceRows = evList.map((ev, idx) => `
          <tr style="border-bottom: 1px solid rgba(255,255,255,0.05);">
            <td style="padding: 6px 8px; font-family: var(--font-mono);">${idx + 1}</td>
            <td style="padding: 6px 8px; font-weight: 500;">📎 ${ev.original_name}</td>
            <td style="padding: 6px 8px; font-family: var(--font-mono); font-size: 9.5px; color: var(--cyber-cyan); word-break: break-all;">${ev.sha256_hash}</td>
            <td style="padding: 6px 8px; font-family: var(--font-mono); font-size: 9.5px; color: var(--text-muted);">${(ev.file_size / 1024).toFixed(1)} KB</td>
          </tr>
        `).join('');
      }

      bodyEl.innerHTML = `
        <div class="report-header" style="border-left: 3px solid var(--sev-${(f.severity || 'low').toLowerCase()}); padding-left: 10px;">
          <div style="display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; flex-wrap: wrap;">
            <div>
              <span style="font-family: var(--font-mono); font-size: 11px; color: var(--cyber-cyan); font-weight: bold;">[${f.custom_id}] TECHNICAL ADVISORY</span>
              <div class="report-title-lg" style="margin-top: 3px; font-size: 14px;">${f.title}</div>
            </div>
            <div style="display: flex; gap: 4px; align-items: center;">
              <span class="item-badge ${sevClass}" style="font-size: 9px; padding: 1px 6px;">${f.severity}</span>
              <span style="font-size: 10px; background: rgba(255,255,255,0.06); padding: 2px 6px; border-radius: 4px; font-family: var(--font-mono);">CVSS ${f.cvss_score || 'N/A'}</span>
            </div>
          </div>
          <div style="font-size: 10.5px; color: var(--text-secondary); margin-top: 6px;">
            Engagement: <strong>${asm.name || 'Authorized Security Assessment'}</strong> | Client: <strong>${asm.client || 'Confidential'}</strong>
          </div>
          <div style="font-size: 10px; color: var(--text-muted); margin-top: 2px; font-family: var(--font-mono);">
            Status: <span style="color:var(--cyber-green);">${f.status}</span> • Category: ${f.category || 'Vulnerability'} • ${f.cwe_id || 'CWE-Unassigned'}
          </div>
        </div>

        <div class="report-section">
          <div class="report-section-title">1. AFFECTED ASSET & TARGET SCOPE</div>
          <div style="font-family: var(--font-mono); font-size: 11px; color: var(--cyber-cyan); background: rgba(0,229,255,0.05); padding: 6px 10px; border-radius: 4px; border: 1px solid rgba(0,229,255,0.2);">
            ${f.affected_asset}
          </div>
        </div>

        <div class="report-section">
          <div class="report-section-title">2. VULNERABILITY OVERVIEW</div>
          <p style="line-height: 1.5; color: var(--text-primary); font-size: 11px; margin: 0;">${f.description || 'No detailed description provided.'}</p>
        </div>

        <div class="report-section">
          <div class="report-section-title">3. TECHNICAL PROOF OF CONCEPT & DETAILS</div>
          <pre style="font-family: var(--font-mono); font-size: 10.5px; background: rgba(0,0,0,0.4); padding: 8px; border-radius: 4px; border: 1px solid var(--border-subtle); overflow-x: auto; white-space: pre-wrap; margin: 0;">${f.technical_details || 'Verified during manual penetration test execution.'}</pre>
        </div>

        <div class="report-section">
          <div class="report-section-title">4. IMPACT ANALYSIS</div>
          <p style="line-height: 1.5; color: var(--text-primary); font-size: 11px; margin: 0;">${f.impact || 'Allows unauthorized exploitation within assessment scope.'}</p>
        </div>

        <div class="report-section">
          <div class="report-section-title">5. ACTIONABLE REMEDIATION GUIDANCE</div>
          <div style="background: rgba(0,230,118,0.06); border: 1px solid rgba(0,230,118,0.25); border-radius: 4px; padding: 8px 10px;">
            <p style="line-height: 1.5; color: var(--cyber-green); font-size: 11px; margin: 0;">${f.remediation || 'Follow vendor security advisories and OWASP remediation guidelines.'}</p>
          </div>
        </div>

        <div class="report-section">
          <div class="report-section-title">6. CRYPTOGRAPHIC EVIDENCE INVENTORY (${evList.length})</div>
          <div style="overflow-x: auto;">
            <table style="width: 100%; border-collapse: collapse; font-size: 10.5px; margin-top: 4px;">
              <thead>
                <tr style="border-bottom: 1px solid var(--border-subtle); text-align: left; color: var(--text-muted); font-family: var(--font-mono); font-size: 9.5px;">
                  <th style="padding: 4px 6px;">#</th>
                  <th style="padding: 4px 6px;">FILENAME</th>
                  <th style="padding: 4px 6px;">SHA-256 HASH</th>
                  <th style="padding: 4px 6px;">SIZE</th>
                </tr>
              </thead>
              <tbody>
                ${evidenceRows}
              </tbody>
            </table>
          </div>
        </div>
      `;
    } else {
      renderError('report-modal-body', res.message || 'Failed to load finding report', `previewFindingReport('${findingId}')`);
    }
  } catch (err) {
    console.error('Failed to preview finding advisory:', err);
    renderError('report-modal-body', err.message, `previewFindingReport('${findingId}')`);
  }
}

// Modal Header Action Handlers
function handleActiveModalDownload() {
  if (activeModalReportType === 'FULL') {
    downloadTxtReport();
  } else if (activeModalFindingId) {
    downloadFindingReport(activeModalFindingId);
  }
}

function handleActiveModalPrint() {
  if (activeModalReportType === 'FULL') {
    printFullReport();
  } else if (activeModalFindingId) {
    printFindingReport(activeModalFindingId);
  }
}

// Native TXT File Download for Full Assessment Report
async function downloadTxtReport() {
  try {
    showToast('Compiling & downloading assessment report...', 'info');
    const res = await fetch(`${API_BASE}/assessments/${currentAssessmentId}/report/download`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const text = await res.text();
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `redops-assessment-report-${currentAssessmentId}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setTimeout(() => loadReportHistory(), 1000);
    showToast('Assessment report downloaded successfully');
  } catch (err) {
    console.error('Failed to download report:', err);
    window.open(`${API_BASE}/assessments/${currentAssessmentId}/report/download`, '_blank');
  }
}

// Native TXT File Download for Individual Finding Advisory
async function downloadFindingReport(findingId) {
  if (!findingId) return;
  try {
    showToast('Generating finding advisory TXT...', 'info');
    const res = await fetch(`${API_BASE}/findings/${findingId}/report/download`);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const text = await res.text();
    const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `redops-finding-${findingId}.txt`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    setTimeout(() => URL.revokeObjectURL(url), 1000);
    setTimeout(() => loadReportHistory(), 1000);
    showToast('Finding advisory downloaded successfully');
  } catch (err) {
    console.error('Failed to download finding advisory:', err);
    window.open(`${API_BASE}/findings/${findingId}/report/download`, '_blank');
  }
}

// Full Assessment Report PDF Generation & Printing
async function printFullReport() {
  showToast('Preparing PDF document for printing...', 'info');
  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/report`);
    if (res.status !== 'success') throw new Error(res.message || 'Failed to load report data');
    const rep = res.data;

    let bodyHtml = `
      <div class="print-header">
        <div class="print-brand">RedOps Mobile — Authorized Security Assessment Report</div>
        <div class="print-title">${rep.metadata.report_title}</div>
        <div class="print-meta">
          Target Client: <strong>${rep.metadata.client}</strong> &nbsp;|&nbsp; Assessment Period: <strong>${rep.metadata.assessment_period}</strong>
        </div>
        <div class="print-classification">${rep.metadata.classification}</div>
      </div>
    `;

    for (const [sectionTitle, content] of Object.entries(rep.sections)) {
      bodyHtml += `
        <div class="print-section">
          <div class="print-section-title">${sectionTitle}</div>
          <div>${typeof content === 'string' ? `<p style="margin:0 0 8px 0;line-height:1.5;">${content}</p>` : `<pre>${JSON.stringify(content, null, 2)}</pre>`}</div>
        </div>
      `;
    }

    printHtmlReport(rep.metadata.report_title, bodyHtml);
  } catch (err) {
    console.error('Failed to generate PDF:', err);
    showToast('Failed to generate PDF: ' + err.message, 'error');
  }
}

// Individual Finding Advisory PDF Generation & Printing
async function printFindingReport(findingId) {
  if (!findingId) return;
  showToast('Preparing finding advisory for printing...', 'info');
  try {
    const res = await apiFetch(`/findings/${findingId}/report`);
    if (res.status !== 'success') throw new Error(res.message || 'Failed to load finding data');
    const rep = res.data;
    const f = rep.finding;
    const asm = rep.assessment || {};
    const evList = rep.evidence || [];

    const sevClass = (f.severity || 'LOW').toLowerCase();

    let evRows = '';
    if (evList.length === 0) {
      evRows = '<tr><td colspan="4" style="text-align:center;color:#6b7280;padding:8px;">No cryptographic evidence files attached.</td></tr>';
    } else {
      evRows = evList.map((ev, idx) => `
        <tr>
          <td>${idx + 1}</td>
          <td><strong>${ev.original_name}</strong></td>
          <td><code>${ev.sha256_hash}</code></td>
          <td>${(ev.file_size / 1024).toFixed(1)} KB</td>
        </tr>
      `).join('');
    }

    const bodyHtml = `
      <div class="print-header">
        <div class="print-brand">RedOps Mobile — Technical Vulnerability Advisory</div>
        <div class="print-title">[${f.custom_id}] ${f.title}</div>
        <div class="print-meta">
          Engagement: <strong>${asm.name || 'Authorized Security Assessment'}</strong> &nbsp;|&nbsp; Client: <strong>${asm.client || 'Confidential'}</strong> &nbsp;|&nbsp; Target: <code>${f.affected_asset}</code>
        </div>
        <div style="margin-top:8px;">
          <span class="badge badge-${sevClass}">${f.severity} (CVSS ${f.cvss_score || 'N/A'})</span>
          &nbsp;<span class="badge badge-info">${f.status}</span>
          ${f.cwe_id ? `&nbsp;<span class="badge badge-info">${f.cwe_id}</span>` : ''}
        </div>
      </div>

      <div class="print-section">
        <div class="print-section-title">1. Affected Asset & Target Scope</div>
        <p style="margin:0 0 8px 0;"><code>${f.affected_asset}</code></p>
      </div>

      <div class="print-section">
        <div class="print-section-title">2. Vulnerability Overview</div>
        <p style="margin:0 0 8px 0;line-height:1.5;">${f.description || 'N/A'}</p>
      </div>

      <div class="print-section">
        <div class="print-section-title">3. Technical Proof of Concept & Execution</div>
        <pre>${f.technical_details || 'Verified during manual penetration test execution.'}</pre>
      </div>

      <div class="print-section">
        <div class="print-section-title">4. Impact Analysis</div>
        <p style="margin:0 0 8px 0;line-height:1.5;">${f.impact || 'Allows unauthorized exploitation within assessment scope.'}</p>
      </div>

      <div class="print-section">
        <div class="print-section-title">5. Actionable Remediation Guidance</div>
        <div style="background:#f0fdf4;border:1px solid #bbf7d0;padding:10px;border-radius:4px;color:#166534;">
          <p style="margin:0;line-height:1.5;">${f.remediation || 'Follow vendor security advisories and OWASP remediation guidelines.'}</p>
        </div>
      </div>

      <div class="print-section">
        <div class="print-section-title">6. Cryptographic Evidence Inventory (${evList.length} artifacts)</div>
        <table>
          <thead>
            <tr>
              <th style="width:5%;">#</th>
              <th style="width:30%;">Filename</th>
              <th style="width:50%;">SHA-256 Hash</th>
              <th style="width:15%;">Size</th>
            </tr>
          </thead>
          <tbody>
            ${evRows}
          </tbody>
        </table>
      </div>
    `;

    printHtmlReport(`Advisory: [${f.custom_id}] ${f.title}`, bodyHtml);
  } catch (err) {
    console.error('Failed to print finding advisory:', err);
    showToast('Failed to print advisory: ' + err.message, 'error');
  }
}

// Populate Finding-Specific Quick Export List
async function loadFindingsReportQuickList() {
  const container = document.getElementById('findings-report-quick-list');
  if (!container) return;

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/findings`);
    if (res.status === 'success') {
      container.innerHTML = '';
      if (res.data.length === 0) {
        container.innerHTML = '<div style="padding:12px;text-align:center;color:var(--text-muted);font-size:11px;">Zero findings documented for this engagement.</div>';
        return;
      }

      res.data.forEach(f => {
        const item = document.createElement('div');
        item.style.padding = '10px';
        item.style.borderBottom = '1px solid rgba(255,255,255,0.05)';
        item.style.display = 'flex';
        item.style.justifyContent = 'space-between';
        item.style.alignItems = 'center';
        item.style.gap = '10px';
        item.style.flexWrap = 'wrap';

        const sevClass = (f.severity === 'CRITICAL' || f.severity === 'HIGH') ? 'badge-out-of-scope' : 'badge-in-scope';

        item.innerHTML = `
          <div style="flex: 1; min-width: 180px;">
            <div style="display: flex; align-items: center; gap: 8px;">
              <span style="font-family: var(--font-mono); font-size: 11px; color: var(--cyber-cyan); font-weight: bold;">${f.custom_id}</span>
              <span class="item-badge ${sevClass}" style="font-size: 9px; padding: 1px 6px;">${f.severity}</span>
              <span style="font-size: 10px; color: var(--text-muted); font-family: var(--font-mono);">CVSS ${f.cvss_score || 'N/A'}</span>
            </div>
            <div style="font-size: 12px; font-weight: 500; color: var(--text-primary); margin-top: 3px;">${f.title}</div>
            <div style="font-size: 10.5px; color: var(--text-muted); font-family: var(--font-mono); margin-top: 2px;">Asset: ${f.affected_asset}</div>
          </div>
          <div style="display: flex; gap: 6px; align-items: center; flex-wrap: wrap;">
            <button class="btn btn-sm btn-outline" style="font-size: 10px; padding: 4px 8px;" onclick="previewFindingReport('${f.id}')" title="Preview single advisory in report viewer">👁️ Preview</button>
            <button class="btn btn-sm btn-outline" style="font-size: 10px; padding: 4px 8px;" onclick="downloadFindingReport('${f.id}')" title="Download standalone TXT advisory">📥 TXT</button>
            <button class="btn btn-sm btn-primary" style="font-size: 10px; padding: 4px 8px;" onclick="printFindingReport('${f.id}')" title="Print/Export finding as PDF">🖨️ PDF</button>
          </div>
        `;
        container.appendChild(item);
      });
    }
  } catch (err) {
    console.error('Failed to load findings quick list:', err);
  }
}

// Load Assessment Report Metadata and Lists
async function loadReport() {
  // Refresh quick list of findings
  loadFindingsReportQuickList();

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/report`);
    if (res.status === 'success') {
      const rep = res.data;

      // Update Full Assessment Report Card headers
      const fullTitle = document.getElementById('full-report-title');
      const fullMeta = document.getElementById('full-report-meta');
      const fullDesc = document.getElementById('full-report-desc');

      if (fullTitle) fullTitle.textContent = rep.metadata.report_title;
      if (fullMeta) fullMeta.textContent = `Client: ${rep.metadata.client} • Period: ${rep.metadata.assessment_period}`;
      if (fullDesc) fullDesc.textContent = `Full 12-section audit documentation • ${rep.metadata.classification}`;
    }
  } catch (err) {
    console.error('Failed to load report summary:', err);
  }
}

async function loadReportHistory() {
  const container = document.getElementById('report-history-list');
  if (!container) return;

  renderLoading('report-history-list', 'Accessing generation audit trail...');

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/report/history`);
    if (res.status === 'success') {
      container.innerHTML = '';
      if (res.data.length === 0) {
        renderEmpty('report-history-list', {
          icon: '📄',
          title: 'Zero Report History',
          desc: 'No reports have been exported for this engagement yet. Generate an executive report above.',
          actionText: '📥 Generate & Download TXT',
          actionFn: "downloadTxtReport()"
        });
        return;
      }

      res.data.forEach(r => {
        const item = document.createElement('div');
        item.style.padding = '10px';
        item.style.borderBottom = '1px solid rgba(255,255,255,0.05)';
        item.style.fontSize = '11px';
        item.style.fontFamily = 'var(--font-mono)';
        item.innerHTML = `
          <div style="display:flex;justify-content:space-between;align-items:center;color:var(--text-primary);flex-wrap:wrap;gap:6px;">
            <span>📄 ${r.report_title}</span>
            <div style="display:flex;align-items:center;gap:8px;">
              <span style="color:var(--cyber-green);">[${r.format}]</span>
              <button onclick="downloadTxtReport()" class="btn btn-sm btn-outline" style="font-size:9.5px;padding:2px 8px;">📥 Download</button>
            </div>
          </div>
          <div style="color:var(--text-muted);margin-top:4px;">
            Size: ${(r.file_size / 1024).toFixed(1)} KB • SHA-256: ${r.sha256_hash ? r.sha256_hash.slice(0, 16) + '...' : 'Verified'} • ${formatRelativeTime(r.created_at)}
          </div>
        `;
        container.appendChild(item);
      });
    } else {
      renderError('report-history-list', res.message || 'Failed to load report history', 'loadReportHistory()');
    }
  } catch (err) {
    console.error('Failed to load report history:', err);
    renderError('report-history-list', err.message, 'loadReportHistory()');
  }
}

// ==============================================================================
// 4.8 SETTINGS, SECURITY, PIN & BACKUP
// ==============================================================================
let isBiometricEnrolled = true;
function toggleBiometrics() {
  isBiometricEnrolled = !isBiometricEnrolled;
  const badge = document.getElementById('settings-bio-badge');
  if (badge) {
    badge.textContent = isBiometricEnrolled ? 'ENROLLED' : 'DISABLED';
    badge.className = isBiometricEnrolled ? 'btn btn-sm btn-outline' : 'btn btn-sm btn-card-delete';
  }
  showToast(isBiometricEnrolled ? 'Biometric authentication enrolled' : 'Biometric authentication disabled', 'info');
}

async function handleVerifyPin(e) {
  e.preventDefault();
  const pinInput = document.getElementById('input-security-pin');
  const pin = pinInput.value.trim();

  if (!pin || pin.length < 4) {
    showToast('PIN must be at least 4 digits.', 'error');
    return;
  }

  const submitBtn = e.target.querySelector('button[type="submit"]');
  const originalText = submitBtn ? submitBtn.textContent : '';
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Verifying...'; }

  try {
    const res = await apiFetch('/system/verify-pin', {
      method: 'POST',
      body: JSON.stringify({ pin })
    });

    if (res.status === 'success') {
      closeModal('modal-verify-pin');
      pinInput.value = '';
      showToast('PIN Verified: Security Clearance Granted');
    } else {
      showToast('Invalid PIN entered: Access Denied', 'error');
    }
  } catch (err) {
    showToast('PIN Verification Failed: Access Denied', 'error');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = originalText; }
  }
}

function exportDatabaseBackup() {
  window.open(`${API_BASE}/system/backup/export`, '_blank');
  showToast('Database backup JSON exported');
}

// ==============================================================================
// TARGETS, RECON, WIRELESS HELPERS
// ==============================================================================
async function loadTargets() {
  const container = document.getElementById('targets-list');
  renderLoading('targets-list', 'Querying scope boundary targets...');

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/targets`);
    if (res.status === 'success') {
      allTargetsData = res.data;
      container.innerHTML = '';
      if (allTargetsData.length === 0) {
        renderEmpty('targets-list', {
          icon: '🎯',
          title: 'No Targets Defined',
          desc: 'No assets configured within engagement scope boundaries. Register authorized domains, IPs, or CIDRs.',
          actionText: '+ Register Target Asset',
          actionFn: "openModal('modal-new-target')"
        });
        return;
      }

      allTargetsData.forEach(tgt => {
        const isInScope = tgt.scope_status === 'IN_SCOPE';
        const card = document.createElement('div');
        card.className = 'item-card';
        card.innerHTML = `
          <div class="item-header">
            <div>
              <div class="item-title">${tgt.domain || tgt.ip_address || tgt.hostname || 'Target'}</div>
              <div style="font-size: 11px; color: var(--text-secondary);">${tgt.url || 'No URL specified'}</div>
            </div>
            <span class="item-badge ${isInScope ? 'badge-in-scope' : 'badge-out-of-scope'}">
              ${isInScope ? 'IN SCOPE' : 'OUT OF SCOPE'}
            </span>
          </div>
          <div class="item-desc">${tgt.description || ''}</div>
          <div class="item-meta">
            <span>IP: ${tgt.ip_address || 'Unresolved'}</span>
            <span>Host: ${tgt.hostname || 'None'}</span>
          </div>
          ${!isInScope ? '<div style="font-size: 10px; color: var(--sev-critical); font-weight: bold; margin-top: 4px;">⚠️ RESTRICTION: Asset is strictly out of scope. Do not probe or assess.</div>' : ''}
          <div class="card-action-bar">
            <button class="btn-card-action" onclick="openEditTarget('${tgt.id}')">✏️ Edit</button>
            <button class="btn-card-action btn-card-delete" onclick="deleteTarget('${tgt.id}')">🗑️ Delete</button>
          </div>
        `;
        container.appendChild(card);
      });
    } else {
      renderError('targets-list', res.message || 'Failed to load targets', 'loadTargets()');
    }
  } catch (err) {
    console.error('Failed to load targets:', err);
    renderError('targets-list', err.message, 'loadTargets()');
  }
}

async function handleCreateTarget(e) {
  e.preventDefault();
  const domain = document.getElementById('tgt-domain').value.trim();
  const ip_address = document.getElementById('tgt-ip').value.trim();
  const url = document.getElementById('tgt-url').value.trim();

  if (!domain && !ip_address && !url) {
    showToast('Please provide at least a Domain, IP Address, or URL.', 'error');
    return;
  }

  if (ip_address) {
    const ipPattern = /^(\d{1,3}\.){3}\d{1,3}(\/\d{1,2})?$|^([a-fA-F0-9:]+:+)+[a-fA-F0-9]+$/;
    if (!ipPattern.test(ip_address)) {
      showToast('Please enter a valid IPv4, IPv6, or CIDR address.', 'error');
      return;
    }
  }

  const submitBtn = e.target.querySelector('button[type="submit"]');
  const originalText = submitBtn ? submitBtn.textContent : '';
  if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Saving Target...'; }

  try {
    const payload = {
      domain,
      ip_address,
      scope_status: document.getElementById('tgt-scope').value,
      url,
      description: document.getElementById('tgt-desc').value.trim()
    };

    const res = await apiFetch(`/assessments/${currentAssessmentId}/targets`, {
      method: 'POST',
      body: JSON.stringify(payload)
    });

    if (res.status === 'success') {
      closeModal('modal-new-target');
      e.target.reset();
      await loadTargets();
      await loadDashboardMetrics();
      await loadActivities();
      showToast('Target added to assessment scope');
    } else {
      showToast(res.message || 'Failed to add target', 'error');
    }
  } catch (err) {
    showToast('Failed to add target: ' + err.message, 'error');
  } finally {
    if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = originalText; }
  }
}

function openEditTarget(tgtId) {
  const tgt = allTargetsData.find(t => t.id === tgtId);
  if (!tgt) return;

  document.getElementById('edit-tgt-id').value = tgt.id;
  document.getElementById('edit-tgt-domain').value = tgt.domain || '';
  document.getElementById('edit-tgt-ip').value = tgt.ip_address || '';
  document.getElementById('edit-tgt-scope').value = tgt.scope_status || 'IN_SCOPE';
  document.getElementById('edit-tgt-url').value = tgt.url || '';
  document.getElementById('edit-tgt-notes').value = tgt.description || '';

  openModal('modal-edit-target');
}

async function handleUpdateTarget(e) {
  e.preventDefault();
  const id = document.getElementById('edit-tgt-id').value;
  const payload = {
    domain: document.getElementById('edit-tgt-domain').value.trim(),
    ip_address: document.getElementById('edit-tgt-ip').value.trim(),
    scope_status: document.getElementById('edit-tgt-scope').value,
    url: document.getElementById('edit-tgt-url').value.trim(),
    description: document.getElementById('edit-tgt-notes').value.trim()
  };

  const res = await apiFetch(`/targets/${id}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-edit-target');
    await loadTargets();
    await loadDashboardMetrics();
    showToast('Target updated');
  } else {
    showToast(res.message || 'Failed to update target', 'error');
  }
}

async function deleteTarget(id) {
  if (confirm('Delete this target asset? This will remove scope boundaries for this entity.')) {
    const res = await apiFetch(`/targets/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadTargets();
      await loadDashboardMetrics();
      await loadActivities();
      showToast('Target deleted');
    } else {
      showToast(res.message || 'Failed to delete target', 'error');
    }
  }
}

// Recon
async function loadRecon() {
  const container = document.getElementById('recon-list');
  renderLoading('recon-list', 'Enumerating reconnaissance records...');

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/recon`);
    if (res.status === 'success') {
      allReconData = res.data;
      renderReconList(allReconData);
    } else {
      renderError('recon-list', res.message || 'Failed to load recon observations', 'loadRecon()');
    }
  } catch (err) {
    console.error('Failed to load recon:', err);
    renderError('recon-list', err.message, 'loadRecon()');
  }
}

function renderReconList(list) {
  const container = document.getElementById('recon-list');
  if (!container) return;
  container.innerHTML = '';
  if (list.length === 0) {
    renderEmpty('recon-list', {
      icon: '🌐',
      title: 'No Tool Observations',
      desc: 'No observations or tool results saved yet. Run any security tool and click "Save to Assessment" to record verified discoveries.',
      actionText: '+ Add Manual Observation',
      actionFn: "openModal('modal-new-recon')"
    });
    return;
  }

  list.forEach(rc => {
    const card = document.createElement('div');
    card.className = 'item-card';

    // Status pill & badge
    const isConverted = rc.status === 'CONVERTED_TO_FINDING' || rc.converted_finding_id;
    let statusPill = '';
    if (isConverted) {
      statusPill = `<span class="item-badge" style="background:rgba(0,230,118,0.15); color:var(--cyber-green); border:1px solid rgba(0,230,118,0.3);">CONVERTED ${rc.finding_custom_id ? '(' + rc.finding_custom_id + ')' : ''}</span>`;
    } else if (rc.status === 'INVESTIGATING') {
      statusPill = `<span class="item-badge" style="background:rgba(255,179,0,0.15); color:var(--sev-medium); border:1px solid rgba(255,179,0,0.3);">INVESTIGATING</span>`;
    } else {
      statusPill = `<span class="item-badge" style="background:rgba(0,229,255,0.12); color:var(--cyber-cyan); border:1px solid rgba(0,229,255,0.25);">INFORMATIONAL</span>`;
    }

    const toolCat = rc.tool_category || rc.observation_type || 'RECONNAISSANCE';
    const displayTitle = rc.title || `${rc.tool_name || rc.observation_type}: ${rc.value}`;
    const createdDate = rc.created_at ? new Date(rc.created_at).toLocaleString() : 'Just now';

    card.innerHTML = `
      <div class="item-header">
        <div>
          <div style="display: flex; align-items: center; gap: 8px;">
            <span class="item-badge badge-in-scope" style="font-size: 9.5px;">${escapeHtml(toolCat)}</span>
            <span style="font-size: 10px; color: var(--text-muted);">${createdDate}</span>
          </div>
          <div class="item-title" style="margin-top: 4px; font-size: 13px;">${escapeHtml(displayTitle)}</div>
        </div>
        <div>${statusPill}</div>
      </div>
      <div style="font-size: 11px; color: var(--text-secondary); margin-top: 4px;">
        <strong>Target:</strong> <code style="color:var(--cyber-cyan);">${escapeHtml(rc.value)}</code>
        ${rc.source_info ? `<span style="margin-left:8px; color:var(--text-muted); font-size:10px;">• ${escapeHtml(rc.source_info)}</span>` : ''}
      </div>
      <div class="item-desc" style="margin-top: 6px;">${escapeHtml(rc.notes || 'Observation recorded from cybersecurity toolkit inspection.')}</div>
      <div class="card-action-bar">
        ${!isConverted ? `<button class="btn-card-action" style="color:var(--cyber-green); border-color:rgba(0,230,118,0.4); font-weight:600;" onclick="openConvertObservation('${rc.id}')">⚡ Convert to Finding</button>` : ''}
        <button class="btn-card-action" onclick="openEditObservation('${rc.id}')">✏️ Edit Notes</button>
        <button class="btn-card-action btn-card-delete" onclick="deleteRecon('${rc.id}')">🗑️ Delete</button>
      </div>
    `;
    container.appendChild(card);
  });
}

function filterRecon(category, btn) {
  document.querySelectorAll('#screen-recon .filter-chips .chip').forEach(c => c.classList.remove('active'));
  btn.classList.add('active');
  if (category === 'ALL') {
    renderReconList(allReconData);
  } else {
    renderReconList(allReconData.filter(r => 
      (r.observation_type && r.observation_type === category) || 
      (r.tool_category && r.tool_category === category)
    ));
  }
}

async function handleCreateRecon(e) {
  e.preventDefault();
  const value = document.getElementById('rc-val').value.trim();
  if (!value) {
    showToast('Observation Value is required.', 'error');
    return;
  }

  const payload = {
    observation_type: document.getElementById('rc-type').value,
    value,
    notes: document.getElementById('rc-notes').value.trim()
  };

  const res = await apiFetch(`/assessments/${currentAssessmentId}/recon`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-new-recon');
    e.target.reset();
    await loadRecon();
    await loadActivities();
    showToast('Recon observation logged');
  } else {
    showToast(res.message || 'Failed to log observation', 'error');
  }
}

function promptSaveToolResult(toolType) {
  // Populate assessment dropdown
  const select = document.getElementById('save-obs-assessment-select');
  if (select && allAssessmentsData) {
    select.innerHTML = allAssessmentsData.map(a => 
      `<option value="${a.id}" ${a.id === currentAssessmentId ? 'selected' : ''}>${escapeHtml(a.name)} (${escapeHtml(a.client)})</option>`
    ).join('');
  }

  let toolName = 'Cybersecurity Toolkit';
  let category = 'RECONNAISSANCE';
  let target = '';
  let title = '';
  let summary = '';
  let sourceInfo = 'Live Inspection at ' + new Date().toLocaleString();
  let rawData = null;

  if (toolType === 'DNS' && toolkitState.lastDns) {
    toolName = 'DNS Records Resolver';
    category = 'DNS';
    target = toolkitState.lastDns.domain;
    title = `DNS Records: ${target}`;
    summary = `${toolkitState.lastDns.records.length} records resolved (${toolkitState.lastDns.records.map(r => r.type).filter((v,i,a)=>a.indexOf(v)===i).join(', ')})`;
    sourceInfo = 'Authoritative DNS Query at ' + new Date(toolkitState.lastDns.queried_at || Date.now()).toLocaleTimeString();
    rawData = toolkitState.lastDns;
  } else if (toolType === 'REVERSE_DNS' && toolkitState.lastRdns) {
    toolName = 'Reverse DNS PTR Lookup';
    category = 'DNS';
    target = toolkitState.lastRdns.ip;
    title = `Reverse DNS: ${target}`;
    summary = `PTR Hostnames: ${(toolkitState.lastRdns.hostnames || []).join(', ') || 'No PTR records'}`;
    sourceInfo = 'in-addr.arpa / ip6.arpa Query';
    rawData = toolkitState.lastRdns;
  } else if (toolType === 'RDAP' && toolkitState.lastRdap) {
    toolName = 'RDAP / WHOIS Query';
    category = 'RECONNAISSANCE';
    target = toolkitState.lastRdap.query;
    title = `RDAP Registration: ${target}`;
    summary = `Registrar: ${toolkitState.lastRdap.registrar || 'Protected'}. Status: ${(toolkitState.lastRdap.status_flags || []).join(' • ') || 'Active'}`;
    sourceInfo = toolkitState.lastRdap.rdap_url || 'ICANN RDAP';
    rawData = toolkitState.lastRdap;
  } else if (toolType === 'IP_INFO' && toolkitState.lastIpInfo) {
    toolName = 'IP Information & Scope';
    category = 'NETWORK';
    target = toolkitState.lastIpInfo.ip;
    title = `IP Scope: ${target}`;
    summary = `Scope: ${toolkitState.lastIpInfo.scope} (${toolkitState.lastIpInfo.is_routable ? 'Public Internet' : 'Non-Routable Private'})`;
    sourceInfo = toolkitState.lastIpInfo.data_provider || 'RIR RDAP Service';
    rawData = toolkitState.lastIpInfo;
  } else if (toolType === 'SUBNET' && toolkitState.lastSubnet) {
    toolName = 'Subnet & CIDR Calculator';
    category = 'NETWORK';
    target = toolkitState.lastSubnet.cidrNotation;
    title = `Subnet Planning: ${target}`;
    summary = `Network: ${toolkitState.lastSubnet.network} • Hosts: ${toolkitState.lastSubnet.usableHosts} usable • Range: ${toolkitState.lastSubnet.usableHostRange}`;
    sourceInfo = 'RFC 791 IPv4 Subnet Parameters';
    rawData = toolkitState.lastSubnet;
  } else if (toolType === 'HEADERS' && toolkitState.lastHeaders) {
    toolName = 'HTTP Security Headers';
    category = 'WEB_SECURITY';
    target = toolkitState.lastHeaders.target_url;
    title = `HTTP Header Audit: ${target}`;
    summary = `Compliance Grade: ${toolkitState.lastHeaders.grade} (${toolkitState.lastHeaders.security_score || 0}%). Server: ${toolkitState.lastHeaders.server || 'Undisclosed'}`;
    sourceInfo = 'HTTP Port 443 Response at ' + new Date().toLocaleTimeString();
    rawData = toolkitState.lastHeaders;
  } else if (toolType === 'SSL' && toolkitState.lastSsl) {
    toolName = 'TLS/SSL Certificate Inspector';
    category = 'WEB_SECURITY';
    target = toolkitState.lastSsl.host;
    title = `TLS Certificate: ${target}`;
    summary = `Issuer: ${toolkitState.lastSsl.issuer?.organization || toolkitState.lastSsl.issuer?.common_name || 'CA'}. Lifespan: ${toolkitState.lastSsl.days_remaining} days remaining`;
    sourceInfo = 'TLS Handshake (' + (toolkitState.lastSsl.protocol_version || 'TLS') + ')';
    rawData = toolkitState.lastSsl;
  } else if (toolType === 'REDIRECTS' && toolkitState.lastRedirects) {
    toolName = 'HTTP Redirect Tracer';
    category = 'WEB_SECURITY';
    target = toolkitState.lastRedirects.initial_url;
    title = `Redirect Trace: ${target}`;
    summary = `${toolkitState.lastRedirects.total_hops} hops &rarr; Destination: ${toolkitState.lastRedirects.final_url}`;
    sourceInfo = 'HTTP 3xx Location Header Chain';
    rawData = toolkitState.lastRedirects;
  }

  document.getElementById('save-obs-tool-name').value = toolName;
  document.getElementById('save-obs-category').value = category;
  document.getElementById('save-obs-target').value = target;
  document.getElementById('save-obs-title').value = title;
  document.getElementById('save-obs-summary').value = summary;
  document.getElementById('save-obs-source-info').value = sourceInfo;
  document.getElementById('save-obs-json-data').value = rawData ? JSON.stringify(rawData) : '';
  document.getElementById('save-obs-notes').value = summary;

  openModal('modal-save-observation');
}

async function handleSaveObservationSubmit(e) {
  e.preventDefault();
  const targetAssessmentId = document.getElementById('save-obs-assessment-select').value;
  const toolName = document.getElementById('save-obs-tool-name').value;
  const category = document.getElementById('save-obs-category').value;
  const target = document.getElementById('save-obs-target').value.trim();
  const title = document.getElementById('save-obs-title').value.trim();
  const summary = document.getElementById('save-obs-summary').value.trim();
  const sourceInfo = document.getElementById('save-obs-source-info').value.trim();
  const notes = document.getElementById('save-obs-notes').value.trim();
  const status = document.getElementById('save-obs-status').value;
  const rawJson = document.getElementById('save-obs-json-data').value;

  let resultData = null;
  if (rawJson) {
    try { resultData = JSON.parse(rawJson); } catch {}
  }

  const payload = {
    tool_name: toolName,
    tool_category: category,
    target,
    title,
    summary,
    source_info: sourceInfo,
    notes,
    status,
    result_data: resultData
  };

  const res = await apiFetch(`/assessments/${targetAssessmentId}/observations`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-save-observation');
    if (targetAssessmentId === currentAssessmentId) {
      await loadRecon();
      await loadActivities();
      await loadDashboardMetrics();
    }
    showToast('Tool observation successfully saved to assessment');
  } else {
    showToast(res.message || 'Failed to save observation', 'error');
  }
}

function openConvertObservation(obsId) {
  const obs = allReconData.find(r => r.id === obsId);
  if (!obs) {
    showToast('Observation not found', 'error');
    return;
  }

  // Pre-fill modal-new-finding
  document.getElementById('fnd-convert-from-obs-id').value = obs.id;
  document.getElementById('fnd-title').value = obs.title || `Security Finding: ${obs.value}`;
  document.getElementById('fnd-asset').value = obs.value;
  document.getElementById('fnd-sev').value = 'MEDIUM';
  syncDefaultCvss('fnd-sev', 'fnd-cvss');
  document.getElementById('fnd-status').value = 'OPEN';
  document.getElementById('fnd-category').value = 'Security Misconfiguration';
  document.getElementById('fnd-cwe').value = 'CWE-693';
  document.getElementById('fnd-desc').value = obs.notes || `Observation recorded from ${obs.tool_name || 'toolkit inspection'}.`;
  document.getElementById('fnd-tech').value = obs.result_data ? `Observation Payload:
${obs.result_data.slice(0, 500)}` : `Observed asset: ${obs.value}`;
  document.getElementById('fnd-impact').value = 'Absence of hardening controls reduces defense-in-depth posture.';
  document.getElementById('fnd-remed').value = 'Implement standard security configurations according to hardening benchmarks.';

  switchFindingModalTab('desc', 'fnd');
  openModal('modal-new-finding');
}

function openEditObservation(obsId) {
  const obs = allReconData.find(r => r.id === obsId);
  if (!obs) return;

  document.getElementById('edit-obs-id').value = obs.id;
  document.getElementById('edit-obs-title').value = obs.title || `${obs.tool_name || obs.observation_type}: ${obs.value}`;
  document.getElementById('edit-obs-target').value = obs.value;
  document.getElementById('edit-obs-status').value = obs.status || 'INFORMATIONAL';
  document.getElementById('edit-obs-notes').value = obs.notes || '';

  openModal('modal-edit-observation');
}

async function handleEditObservationSubmit(e) {
  e.preventDefault();
  const obsId = document.getElementById('edit-obs-id').value;
  const payload = {
    title: document.getElementById('edit-obs-title').value.trim(),
    status: document.getElementById('edit-obs-status').value,
    notes: document.getElementById('edit-obs-notes').value.trim()
  };

  const res = await apiFetch(`/observations/${obsId}`, {
    method: 'PUT',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-edit-observation');
    await loadRecon();
    showToast('Observation updated successfully');
  } else {
    showToast(res.message || 'Failed to update observation', 'error');
  }
}

async function deleteRecon(id) {
  if (confirm('Delete this saved observation record?')) {
    const res = await apiFetch(`/observations/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadRecon();
      await loadActivities();
      showToast('Observation record deleted');
    } else {
      showToast(res.message || 'Failed to delete observation', 'error');
    }
  }
}

function convertCurrentToolResultToFinding(toolType) {
  let asset = '';
  let title = '';
  let desc = '';
  let tech = '';
  let remed = '';
  let cwe = 'CWE-693';
  let cat = 'Security Misconfiguration';

  if (toolType === 'HEADERS' && toolkitState.lastHeaders) {
    const h = toolkitState.lastHeaders;
    asset = h.target_url;
    title = `Missing Defensive Security Headers on ${asset}`;
    const missing = (h.findings || []).filter(f => f.status === 'FAIL').map(f => f.name || f.header).join(', ');
    desc = `Automated HTTP header audit revealed absence of key defense-in-depth headers: ${missing}.`;
    tech = `Target: ${asset}
Grade: ${h.grade} (${h.security_score || 0}%)
Server Banner: ${h.server || 'None'}
Missing Headers: ${missing}`;
    remed = 'Configure reverse proxy to enforce Strict-Transport-Security, Content-Security-Policy, and X-Content-Type-Options.';
  } else if (toolType === 'SSL' && toolkitState.lastSsl) {
    const s = toolkitState.lastSsl;
    asset = s.host;
    title = s.is_expired ? `Expired TLS Certificate on ${asset}` : `Expiring TLS Certificate on ${asset}`;
    desc = `TLS certificate presented by ${asset} is ${s.status.toLowerCase()} (${s.days_remaining} days left).`;
    tech = `Host: ${asset}:${s.port}
Issuer: ${s.issuer?.organization || s.issuer?.common_name || 'Unknown'}
Valid To: ${s.valid_to}
Protocol: ${s.protocol_version}`;
    remed = 'Renew and install valid TLS certificate signed by an authorized Certificate Authority.';
  }

  if (!asset) {
    showToast('No active tool result to convert', 'error');
    return;
  }

  document.getElementById('fnd-convert-from-obs-id').value = '';
  document.getElementById('fnd-title').value = title;
  document.getElementById('fnd-asset').value = asset;
  document.getElementById('fnd-sev').value = 'MEDIUM';
  syncDefaultCvss('fnd-sev', 'fnd-cvss');
  document.getElementById('fnd-status').value = 'OPEN';
  document.getElementById('fnd-category').value = cat;
  document.getElementById('fnd-cwe').value = cwe;
  document.getElementById('fnd-desc').value = desc;
  document.getElementById('fnd-tech').value = tech;
  document.getElementById('fnd-impact').value = 'Absence of cryptographic transport protections allows potential eavesdropping or protocol downgrade attacks.';
  document.getElementById('fnd-remed').value = remed;

  switchFindingModalTab('desc', 'fnd');
  openModal('modal-new-finding');
}

// Wireless
async function loadWireless() {
  const container = document.getElementById('wireless-list');
  if (!container) return;

  renderLoading('wireless-list', 'Querying 802.11 wireless database...');

  try {
    const res = await apiFetch(`/assessments/${currentAssessmentId}/wireless`);
    if (res.status === 'success') {
      container.innerHTML = '';
      if (res.data.length === 0) {
        renderEmpty('wireless-list', {
          icon: '📶',
          title: 'Zero Wireless APs Detected',
          desc: 'No 802.11 access points in database. Initiate a passive RF airwave sweep or register an AP manually.',
          actionText: '📡 Scan Airwaves Now',
          actionFn: "triggerWirelessScan()"
        });
        return;
      }

      res.data.forEach(wf => {
        const pct = Math.min(100, Math.max(10, ((wf.signal_dbm + 100) / 60) * 100));
        const card = document.createElement('div');
        card.className = 'item-card';
        card.innerHTML = `
          <div class="item-header">
            <div>
              <div class="item-title">${wf.ssid || 'Hidden Network'}</div>
              <div style="font-family: var(--font-mono); font-size: 11px; color: var(--text-muted);">${wf.bssid}</div>
            </div>
            <span class="item-badge ${wf.security_capability.includes('WPA3') ? 'badge-in-scope' : 'badge-out-of-scope'}">
              ${wf.security_capability}
            </span>
          </div>
          <div>
            <div style="display: flex; justify-content: space-between; font-size: 10px; color: var(--text-secondary);">
              <span>Signal Strength</span>
              <span>${wf.signal_dbm} dBm (${Math.round(pct)}%)</span>
            </div>
            <div class="wifi-signal-bar-wrap">
              <div class="wifi-signal-bar" style="width: ${pct}%;"></div>
            </div>
          </div>
          <div class="item-desc">${wf.notes || 'Passive 802.11 AP observation recorded.'}</div>
          <div class="item-meta">
            <span>Channel: ${wf.channel || 'Auto'}</span>
            <span>Freq: ${wf.frequency} MHz</span>
            <span>${wf.wifi_standard || '802.11'}</span>
          </div>
          <div class="card-action-bar">
            <button class="btn-card-action btn-card-delete" onclick="deleteWireless('${wf.id}')">🗑️ Delete</button>
          </div>
        `;
        container.appendChild(card);
      });
    } else {
      renderError('wireless-list', res.message || 'Failed to load wireless records', 'loadWireless()');
    }
  } catch (err) {
    console.error('Failed to load wireless:', err);
    renderError('wireless-list', err.message, 'loadWireless()');
  }
}

async function triggerWirelessScan() {
  const btn = document.getElementById('btn-scan-wifi');
  if (btn) {
    btn.textContent = 'Scanning airwaves...';
    btn.disabled = true;
  }

  try {
    await loadWireless();
    showToast('Airwave database refreshed. Physical 802.11 RF radio capture requires native Android client with WifiManager permissions. In web simulator, register authorized APs via "+ AP".', 'info');
  } catch (err) {
    console.error('Airwave refresh failed:', err);
    showToast('Airwave refresh failed: ' + err.message, 'error');
  } finally {
    if (btn) {
      btn.textContent = 'Scan Airwaves';
      btn.disabled = false;
    }
  }
}

async function handleCreateWireless(e) {
  e.preventDefault();
  const ssid = document.getElementById('wf-ssid').value.trim();
  const bssid = document.getElementById('wf-bssid').value.trim();

  if (!bssid) {
    showToast('BSSID MAC address is required.', 'error');
    return;
  }

  const macPattern = /^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$/;
  if (!macPattern.test(bssid)) {
    showToast('BSSID must be a valid MAC address format (e.g. 00:14:22:01:23:45).', 'error');
    return;
  }

  const payload = {
    ssid,
    bssid,
    signal_dbm: parseInt(document.getElementById('wf-signal').value, 10),
    security_capability: document.getElementById('wf-security').value,
    channel: parseInt(document.getElementById('wf-channel').value, 10),
    frequency: parseInt(document.getElementById('wf-channel').value, 10) > 14 ? 5180 : 2412,
    wifi_standard: '802.11ax',
    notes: document.getElementById('wf-notes').value.trim()
  };

  const res = await apiFetch(`/assessments/${currentAssessmentId}/wireless`, {
    method: 'POST',
    body: JSON.stringify(payload)
  });

  if (res.status === 'success') {
    closeModal('modal-new-wireless');
    e.target.reset();
    await loadWireless();
    await loadActivities();
    showToast('Access point registered');
  } else {
    showToast(res.message || 'Failed to register AP', 'error');
  }
}

async function deleteWireless(id) {
  if (confirm('Delete this wireless observation?')) {
    const res = await apiFetch(`/wireless/${id}`, { method: 'DELETE' });
    if (res.status === 'success') {
      await loadWireless();
      showToast('AP observation deleted');
    } else {
      showToast(res.message || 'Failed to delete AP', 'error');
    }
  }
}

// Modal controls
function openModal(id) {
  const m = document.getElementById(id);
  if (m) {
    m.classList.add('open');
    if (id === 'modal-new-finding') {
      switchFindingModalTab('desc', 'fnd');
      syncDefaultCvss('fnd-sev', 'fnd-cvss');
    }
    // Auto-focus first input field in modal if present
    const firstInput = m.querySelector('input:not([type="hidden"]), select, textarea');
    if (firstInput) setTimeout(() => firstInput.focus(), 80);
  }
}

function closeModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.remove('open');
}

// Global modal dismiss listeners (click outside backdrop or press Escape)
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.remove('open');
  }
});

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') {
    document.querySelectorAll('.modal-backdrop.open').forEach(m => m.classList.remove('open'));
  }
});

// ==============================================================================
// CYBERSECURITY TOOLKIT CONTROLLERS & EXECUTORS
// ==============================================================================

// Global Toolkit State
const toolkitState = {
  lastDns: null,
  lastRdns: null,
  lastRdap: null,
  lastSubnet: null,
  lastHeaders: null,
  lastSsl: null,
  lastRedirects: null,
  lastConvert: null,
  lastHashes: null,
  lastIdentify: null,
  cvssMetrics: {
    AV: 'N',
    AC: 'L',
    PR: 'N',
    UI: 'N',
    S: 'U',
    C: 'H',
    I: 'H',
    A: 'H'
  },
  lastCvss: null
};

// Tool Sub-Navigation Switches
function switchReconTool(toolId, btn) {
  document.querySelectorAll('#screen-recon-tools .sub-nav-tab').forEach(b => b.classList.remove('active'));
  if (btn) {
    btn.classList.add('active');
    if (btn.scrollIntoView) {
      btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
    setTimeout(() => updateSubNavArrows('sub-nav-recon'), 260);
  }
  const panels = ['dns', 'reverse', 'rdap', 'ip', 'subnet'];
  panels.forEach(p => {
    const el = document.getElementById(`panel-recon-${p}`);
    if (el) el.style.display = (p === toolId) ? 'flex' : 'none';
  });
}

function switchWebSecTool(toolId, btn) {
  document.querySelectorAll('#screen-websec-tools .sub-nav-tab').forEach(b => b.classList.remove('active'));
  if (btn) {
    btn.classList.add('active');
    if (btn.scrollIntoView) {
      btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
    setTimeout(() => updateSubNavArrows('sub-nav-websec'), 260);
  }
  const panels = ['headers', 'ssl', 'redirects'];
  panels.forEach(p => {
    const el = document.getElementById(`panel-web-${p}`);
    if (el) el.style.display = (p === toolId) ? 'flex' : 'none';
  });
}

function switchUtilTool(toolId, btn) {
  document.querySelectorAll('#screen-utilities-tools .sub-nav-tab').forEach(b => b.classList.remove('active'));
  if (btn) {
    btn.classList.add('active');
    if (btn.scrollIntoView) {
      btn.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
    setTimeout(() => updateSubNavArrows('sub-nav-utilities'), 260);
  }
  const panels = ['hash', 'jwt', 'url', 'subnet', 'convert', 'pwd', 'json', 'cvss', 'identify'];
  panels.forEach(p => {
    const el = document.getElementById(`panel-util-${p}`);
    if (el) el.style.display = (p === toolId) ? 'flex' : 'none';
  });
  if (toolId === 'cvss' && !toolkitState.lastCvss) {
    recalcCvss();
  }
}

function setToolStatus(badgeId, status, text) {
  const badge = document.getElementById(badgeId);
  if (!badge) return;
  badge.className = `tool-status-badge ${status}`;
  badge.textContent = text || status.toUpperCase();
}

function downloadTextFile(text, filename) {
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}

// ------------------------------------------------------------------------------
// 1. PASSIVE RECONNAISSANCE CONTROLLERS
// ------------------------------------------------------------------------------

async function runDnsLookup() {
  const domain = (document.getElementById('input-dns-domain').value || '').trim();
  const authChecked = document.getElementById('auth-check-dns') ? document.getElementById('auth-check-dns').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required to query DNS for target.', 'error');
    return;
  }
  const type = document.getElementById('select-dns-type').value;
  const resultBox = document.getElementById('result-dns-box');
  const outputEl = document.getElementById('output-dns-records');
  const spinner = document.getElementById('btn-dns-spinner');
  const btn = document.getElementById('btn-run-dns');

  if (!domain) {
    showToast('Please enter a target domain name', 'error');
    return;
  }

  setToolStatus('badge-dns-status', 'running', 'RESOLVING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/dns/resolve', {
      method: 'POST',
      body: JSON.stringify({ domain, type, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastDns = res.data;
      setToolStatus('badge-dns-status', 'success', 'SUCCESS');
      resultBox.classList.add('active');
      document.getElementById('label-dns-summary').textContent = 
        `${res.data.records.length} record groups resolved for ${res.data.domain} (${new Date(res.data.queried_at).toLocaleTimeString()})`;

      const allRows = [];
      (res.data.records || []).forEach(group => {
        if (group.status === 'SUCCESS' && group.data) {
          if (group.type === 'A' || group.type === 'AAAA') {
            const list = Array.isArray(group.data) ? group.data : [group.data];
            list.forEach(item => {
              allRows.push({
                type: group.type,
                ttl: item.ttl ? `${item.ttl}s` : '300s',
                value: item.address || item,
                extra: ''
              });
            });
          } else if (group.type === 'MX') {
            const list = Array.isArray(group.data) ? group.data : [group.data];
            list.forEach(item => {
              allRows.push({
                type: 'MX',
                ttl: '300s',
                value: item.exchange || item,
                extra: item.priority !== undefined ? `Priority: ${item.priority}` : ''
              });
            });
          } else if (group.type === 'TXT') {
            const list = Array.isArray(group.data) ? group.data : [group.data];
            list.forEach(item => {
              allRows.push({
                type: 'TXT',
                ttl: '300s',
                value: typeof item === 'string' ? item : JSON.stringify(item),
                extra: ''
              });
            });
          } else if (group.type === 'NS') {
            const list = Array.isArray(group.data) ? group.data : [group.data];
            list.forEach(item => {
              allRows.push({
                type: 'NS',
                ttl: '300s',
                value: item,
                extra: 'Authoritative Nameserver'
              });
            });
          } else if (group.type === 'CNAME') {
            const list = Array.isArray(group.data) ? group.data : [group.data];
            list.forEach(item => {
              allRows.push({
                type: 'CNAME',
                ttl: '300s',
                value: item,
                extra: 'Canonical Alias Target'
              });
            });
          } else if (group.type === 'SOA') {
            const s = group.data;
            allRows.push({
              type: 'SOA',
              ttl: s.minttl ? `${s.minttl}s` : '300s',
              value: s.nsname || 'Primary Nameserver',
              extra: `Hostmaster: ${s.hostmaster || 'N/A'} • Serial: ${s.serial || 'N/A'}`
            });
          }
        } else if (group.status === 'ERROR' || group.status === 'EMPTY') {
          if (type !== 'ALL') {
            allRows.push({
              type: group.type,
              ttl: '-',
              value: `<span style="color:var(--text-muted); font-style:italic;">${escapeHtml(group.message || 'No records found')}</span>`,
              extra: group.status
            });
          }
        }
      });

      if (allRows.length === 0) {
        outputEl.innerHTML = `<div style="padding:10px; color:var(--text-muted); font-family:var(--font-mono); font-size:11px;">No ${type} records found for ${escapeHtml(domain)}.</div>`;
      } else {
        const rowsHtml = allRows.map(r => `
          <tr>
            <td style="color:var(--cyber-cyan); font-weight:700; width:65px;">${escapeHtml(r.type)}</td>
            <td style="color:var(--text-secondary); font-size:10px; width:45px;">${escapeHtml(r.ttl)}</td>
            <td style="word-break:break-all;">
              <strong>${escapeHtml(r.value)}</strong>
              ${r.extra ? `<div style="font-size:10px; color:var(--text-muted); margin-top:2px;">${escapeHtml(r.extra)}</div>` : ''}
            </td>
          </tr>
        `).join('');

        outputEl.innerHTML = `
          <table class="tool-data-table">
            <thead>
              <tr><th>Type</th><th>TTL</th><th>Record Value & Details</th></tr>
            </thead>
            <tbody>${rowsHtml}</tbody>
          </table>
        `;
      }
      showToast(`DNS resolved: ${allRows.length} active records found`);
    } else {
      throw new Error(res.message || 'DNS resolution failed');
    }
  } catch (err) {
    setToolStatus('badge-dns-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyDnsResult() {
  if (!toolkitState.lastDns) return;
  copyToClipboard(JSON.stringify(toolkitState.lastDns, null, 2), 'DNS records copied to clipboard');
}

function exportDnsResult() {
  if (!toolkitState.lastDns) return;
  downloadTextFile(JSON.stringify(toolkitState.lastDns, null, 2), `dns-${toolkitState.lastDns.domain}-${Date.now()}.json`);
}

async function saveDnsToAssessment() {
  if (!toolkitState.lastDns || !toolkitState.lastDns.domain) {
    showToast('No active DNS target to add', 'error');
    return;
  }
  const asmId = currentAssessmentId || (allAssessmentsData && allAssessmentsData[0] ? allAssessmentsData[0].id : 'asm-acme-2026');

  try {
    const res = await apiFetch(`/assessments/${asmId}/targets`, {
      method: 'POST',
      body: JSON.stringify({
        domain: toolkitState.lastDns.domain,
        description: `Target domain resolved via Passive DNS`,
        scope_status: 'IN_SCOPE',
        notes: `Imported from Passive Recon DNS lookup (${(toolkitState.lastDns.records || []).length} record types queried)`
      })
    });
    if (res.status === 'success') {
      showToast(`Target "${toolkitState.lastDns.domain}" added to assessment`);
      await loadTargets();
      await loadDashboardMetrics();
    } else {
      showToast(res.message || 'Failed to add target', 'error');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function runReverseDns() {
  const ip = (document.getElementById('input-rdns-ip').value || '').trim();
  const authChecked = document.getElementById('auth-check-reverse') ? document.getElementById('auth-check-reverse').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required for reverse DNS query.', 'error');
    return;
  }
  const resultBox = document.getElementById('result-rdns-box');
  const outputEl = document.getElementById('output-rdns-records');
  const spinner = document.getElementById('btn-rdns-spinner');
  const btn = document.getElementById('btn-run-rdns');

  if (!ip) {
    showToast('Please enter an IPv4 or IPv6 address', 'error');
    return;
  }

  setToolStatus('badge-rdns-status', 'running', 'QUERYING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/dns/reverse', {
      method: 'POST',
      body: JSON.stringify({ ip, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastRdns = res.data;
      setToolStatus('badge-rdns-status', 'success', 'SUCCESS');
      resultBox.classList.add('active');

      const hostnames = res.data.hostnames || [];
      const ptrDomain = ip.includes(':') 
        ? 'ip6.arpa' 
        : `${ip.split('.').reverse().join('.')}.in-addr.arpa`;

      if (hostnames.length === 0) {
        outputEl.innerHTML = `<div style="padding:10px; color:var(--text-muted); font-family:var(--font-mono); font-size:11px;">No PTR pointer records found for ${escapeHtml(ip)} (Query zone: ${escapeHtml(ptrDomain)}).</div>`;
      } else {
        const rows = hostnames.map(h => `
          <tr>
            <td style="color:var(--cyber-cyan); font-weight:700; width:65px;">PTR</td>
            <td style="color:var(--text-primary); font-weight:600; word-break:break-all;">${escapeHtml(h)}</td>
            <td style="color:var(--text-muted); font-size:10px; word-break:break-all;">${escapeHtml(ptrDomain)}</td>
          </tr>
        `).join('');

        outputEl.innerHTML = `
          <table class="tool-data-table">
            <thead><tr><th>Record</th><th>Hostname</th><th>PTR Query Domain</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        `;
      }
      showToast(`Reverse DNS PTR: ${hostnames.length} hostnames found`);
    } else {
      throw new Error(res.message || 'Reverse DNS lookup failed');
    }
  } catch (err) {
    setToolStatus('badge-rdns-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyRdnsResult() {
  if (!toolkitState.lastRdns) return;
  copyToClipboard(JSON.stringify(toolkitState.lastRdns, null, 2), 'PTR records copied to clipboard');
}

function exportRdnsResult() {
  if (!toolkitState.lastRdns) return;
  downloadTextFile(JSON.stringify(toolkitState.lastRdns, null, 2), `ptr-${toolkitState.lastRdns.ip}-${Date.now()}.json`);
}

async function runRdapLookup() {
  const query = (document.getElementById('input-rdap-query').value || '').trim();
  const authChecked = document.getElementById('auth-check-rdap') ? document.getElementById('auth-check-rdap').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required for RDAP registry query.', 'error');
    return;
  }
  const resultBox = document.getElementById('result-rdap-box');
  const outputEl = document.getElementById('output-rdap-records');
  const spinner = document.getElementById('btn-rdap-spinner');
  const btn = document.getElementById('btn-run-rdap');

  if (!query) {
    showToast('Please enter a target domain or IP', 'error');
    return;
  }

  setToolStatus('badge-rdap-status', 'running', 'QUERYING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/dns/rdap', {
      method: 'POST',
      body: JSON.stringify({ query, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastRdap = res.data;
      setToolStatus('badge-rdap-status', 'success', 'SUCCESS');
      resultBox.classList.add('active');
      const d = res.data;
      const queryType = (d.type || (d.query && d.query.includes('.') ? 'DOMAIN' : 'IP NETWORK')).toUpperCase();
      const regDate = d.registration_date || d.created_date;
      const expDate = d.expiration_date;
      const lastMod = d.last_changed || d.last_changed_date;

      outputEl.innerHTML = `
        <table class="tool-data-table">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">Query Target:</td><td><strong>${escapeHtml(d.query)}</strong> (${escapeHtml(queryType)})</td></tr>
            <tr><td style="color:var(--text-muted);">Registrar / Authority:</td><td><strong style="color:var(--cyber-cyan);">${escapeHtml(d.registrar || 'Protected / Redacted')}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Created:</td><td>${regDate ? new Date(regDate).toLocaleString() : 'N/A'}</td></tr>
            <tr><td style="color:var(--text-muted);">Expires:</td><td>${expDate ? new Date(expDate).toLocaleString() : 'N/A'}</td></tr>
            <tr><td style="color:var(--text-muted);">Last Changed:</td><td>${lastMod ? new Date(lastMod).toLocaleString() : 'N/A'}</td></tr>
            ${d.nameservers && d.nameservers.length ? `<tr><td style="color:var(--text-muted);">Name Servers:</td><td style="font-size:10.5px;">${d.nameservers.map(ns => escapeHtml(ns)).join(', ')}</td></tr>` : ''}
            ${d.status_flags && d.status_flags.length ? `<tr><td style="color:var(--text-muted);">Status Flags:</td><td style="font-size:10px; color:var(--text-secondary);">${d.status_flags.map(s => `<span class="item-badge badge-responsible-pill">${escapeHtml(s)}</span>`).join(' ')}</td></tr>` : ''}
            ${d.network_name ? `<tr><td style="color:var(--text-muted);">Network Name:</td><td>${escapeHtml(d.network_name)}</td></tr>` : ''}
            <tr><td style="color:var(--text-muted);">RDAP Protocol:</td><td><span style="color:var(--cyber-cyan); font-size:10px;">RFC 7480 - RFC 7484 Authoritative Registry</span></td></tr>
          </tbody>
        </table>
      `;
      showToast('RDAP registration query succeeded');
    } else {
      throw new Error(res.message || 'RDAP query failed');
    }
  } catch (err) {
    setToolStatus('badge-rdap-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyRdapResult() {
  if (!toolkitState.lastRdap) return;
  copyToClipboard(JSON.stringify(toolkitState.lastRdap, null, 2), 'RDAP data copied to clipboard');
}

function exportRdapResult() {
  if (!toolkitState.lastRdap) return;
  downloadTextFile(JSON.stringify(toolkitState.lastRdap, null, 2), `rdap-${toolkitState.lastRdap.query}-${Date.now()}.json`);
}

async function runIpInfoLookup() {
  const ip = (document.getElementById('input-ip-target').value || '').trim();
  const authChecked = document.getElementById('auth-check-ip') ? document.getElementById('auth-check-ip').checked : true;
  const resultBox = document.getElementById('result-ip-box');
  const outputEl = document.getElementById('output-ip-records');
  const spinner = document.getElementById('btn-ip-spinner');
  const btn = document.getElementById('btn-run-ip');

  if (!authChecked) {
    showToast('Explicit authorization confirmation required to query IP address.', 'error');
    return;
  }

  if (!ip) {
    showToast('Please enter an IPv4 or IPv6 address', 'error');
    return;
  }

  setToolStatus('badge-ip-status', 'running', 'QUERYING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/dns/ip-info', {
      method: 'POST',
      body: JSON.stringify({ ip, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastIpInfo = res.data;
      setToolStatus('badge-ip-status', 'success', 'SUCCESS');
      resultBox.classList.add('active');
      const d = res.data;

      const scopeBadge = d.is_routable 
        ? '<span class="item-badge badge-in-scope">PUBLIC INTERNET</span>' 
        : '<span class="item-badge badge-responsible-pill">PRIVATE / RESERVED</span>';

      outputEl.innerHTML = `
        <div style="background:rgba(0,229,255,0.06); border:1px solid rgba(0,229,255,0.2); border-radius:var(--radius-sm); padding:8px 12px; margin-bottom:10px; display:flex; justify-content:space-between; align-items:center;">
          <div>
            <div style="font-size:12px; font-weight:700; color:var(--text-primary); font-family:var(--font-mono);">${escapeHtml(d.ip)}</div>
            <div style="font-size:10px; color:var(--text-muted);">${escapeHtml(d.ip_version || 'IPv4')} • ${escapeHtml(d.scope || 'Network Scope')}</div>
          </div>
          <div>${scopeBadge}</div>
        </div>

        <table class="tool-data-table">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">Regional Registry:</td><td><strong style="color:var(--cyber-cyan);">${escapeHtml(d.rir_registry || 'IANA / RIR')}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Organization / Owner:</td><td><strong>${escapeHtml(d.organization || 'Redacted / Administrative')}</strong></td></tr>
            ${d.network_name ? `<tr><td style="color:var(--text-muted);">Network Name:</td><td>${escapeHtml(d.network_name)} ${d.network_handle ? `(${escapeHtml(d.network_handle)})` : ''}</td></tr>` : ''}
            ${d.cidr_range ? `<tr><td style="color:var(--text-muted);">Allocated CIDR:</td><td style="font-family:var(--font-mono); font-size:11px;">${escapeHtml(d.cidr_range)}</td></tr>` : ''}
            ${d.start_address && d.end_address ? `<tr><td style="color:var(--text-muted);">Address Range:</td><td style="font-family:var(--font-mono); font-size:10.5px;">${escapeHtml(d.start_address)} – ${escapeHtml(d.end_address)}</td></tr>` : ''}
            <tr><td style="color:var(--text-muted);">Country / Region:</td><td>${escapeHtml(d.country || 'International / Multi-Regional')}</td></tr>
            <tr><td style="color:var(--text-muted);">Data Provider:</td><td style="font-size:10px; color:var(--text-secondary);">${escapeHtml(d.data_provider || 'Authoritative RIR RDAP')}</td></tr>
          </tbody>
        </table>

        <div style="margin-top:8px; font-size:10px; color:var(--text-muted); font-style:italic;">
          Notice: ${escapeHtml(d.notice || d.limitations || 'IP network registration data reflects administrative allocation, not physical device geolocation.')}
        </div>
      `;

      showToast(`IP ${d.ip} evaluated: ${d.scope || (d.is_routable ? 'Public Internet' : 'Private')}`);
    } else {
      throw new Error(res.message || 'IP information query failed');
    }
  } catch (err) {
    setToolStatus('badge-ip-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyIpInfoResult() {
  if (!toolkitState.lastIpInfo) return;
  copyToClipboard(JSON.stringify(toolkitState.lastIpInfo, null, 2), 'IP registration info copied');
}

function exportIpInfoResult() {
  if (!toolkitState.lastIpInfo) return;
  downloadTextFile(JSON.stringify(toolkitState.lastIpInfo, null, 2), `ipinfo-${toolkitState.lastIpInfo.ip.replace(/[:.]/g, '_')}-${Date.now()}.json`);
}


async function runSubnetCalc() {
  const cidr = (document.getElementById('input-subnet-cidr').value || '').trim();
  const resultBox = document.getElementById('result-subnet-box');
  const outputEl = document.getElementById('output-subnet-records');

  if (!cidr) {
    showToast('Please enter an IPv4 address with CIDR (e.g. 192.168.1.0/24)', 'error');
    return;
  }

  setToolStatus('badge-subnet-status', 'running', 'CALCULATING');

  try {
    const res = await apiFetch('/tools/network/subnet', {
      method: 'POST',
      body: JSON.stringify({ cidr })
    });

    if (res.status === 'success') {
      toolkitState.lastSubnet = res.data;
      setToolStatus('badge-subnet-status', 'success', 'READY');
      resultBox.classList.add('active');
      const d = res.data;

      outputEl.innerHTML = `
        <table class="tool-data-table">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">CIDR Block:</td><td><strong style="color:var(--cyber-cyan); font-size:13px;">${escapeHtml(d.cidrNotation)}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Netmask:</td><td>${d.netmask} &nbsp;<span style="color:var(--text-muted); font-size:10px;">(Wildcard: ${d.wildcard})</span></td></tr>
            <tr><td style="color:var(--text-muted);">Network ID:</td><td><strong>${d.network}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Broadcast IP:</td><td><strong>${d.broadcast}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Usable Host Range:</td><td><strong style="color:var(--cyber-green);">${d.usableHostRange}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Usable Hosts:</td><td><strong>${d.usableHosts}</strong> / ${d.totalHosts} total</td></tr>
            <tr><td style="color:var(--text-muted);">Address Scope:</td><td><span class="item-badge ${d.isRoutable ? 'badge-in-scope' : 'badge-responsible-pill'}">${d.scope}</span> &nbsp;<span style="font-size:10px; color:var(--text-muted);">${d.rfc}</span></td></tr>
            <tr><td style="color:var(--text-muted);">IP Class:</td><td>${d.ipClass}</td></tr>
            <tr><td style="color:var(--text-muted);">Binary Mask:</td><td style="font-size:10px; color:#A7F3D0;">${d.binary.netmask}</td></tr>
            <tr><td style="color:var(--text-muted);">PTR Reverse Zone:</td><td style="font-size:10px; color:var(--text-secondary);">${d.ptrRecord}</td></tr>
          </tbody>
        </table>
      `;
      showToast(`Subnet calculated: ${d.usableHosts} usable hosts`);
    } else {
      throw new Error(res.message || 'Subnet calculation failed');
    }
  } catch (err) {
    setToolStatus('badge-subnet-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  }
}

function copySubnetResult() {
  if (!toolkitState.lastSubnet) return;
  copyToClipboard(JSON.stringify(toolkitState.lastSubnet, null, 2), 'Subnet parameters copied');
}

function exportSubnetResult() {
  if (!toolkitState.lastSubnet) return;
  downloadTextFile(JSON.stringify(toolkitState.lastSubnet, null, 2), `subnet-${toolkitState.lastSubnet.network.replace(/\./g, '_')}-${Date.now()}.json`);
}

// ------------------------------------------------------------------------------
// 2. WEB SECURITY INSPECTION CONTROLLERS
// ------------------------------------------------------------------------------

async function runHttpHeaderScan() {
  const url = (document.getElementById('input-sec-url').value || '').trim();
  const authChecked = document.getElementById('auth-check-headers') ? document.getElementById('auth-check-headers').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required for HTTP security header audit.', 'error');
    return;
  }
  const followRedirects = document.getElementById('check-sec-redirects').checked;
  const resultBox = document.getElementById('result-headers-box');
  const outputEl = document.getElementById('output-headers-records');
  const spinner = document.getElementById('btn-headers-spinner');
  const btn = document.getElementById('btn-run-headers');

  if (!url) {
    showToast('Please enter target URL (e.g. https://example.com)', 'error');
    return;
  }

  setToolStatus('badge-headers-status', 'running', 'AUDITING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/web/headers', {
      method: 'POST',
      body: JSON.stringify({ url, followRedirects, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastHeaders = res.data;
      setToolStatus('badge-headers-status', 'success', 'AUDITED');
      resultBox.classList.add('active');
      const d = res.data;

      // Grade badge styling
      const score = d.security_score !== undefined ? d.security_score : (d.score || 0);
      let gradeColor = 'var(--cyber-green)';
      if (d.grade === 'C' || d.grade === 'D') gradeColor = 'var(--sev-medium)';
      if (d.grade === 'F') gradeColor = 'var(--sev-critical)';

      const findingsList = d.findings || (d.checks ? Object.values(d.checks) : []);
      let checksHtml = findingsList.map(item => {
        const isPass = item.status === 'PASS' || item.present === true;
        const isWarn = item.status === 'WARNING';
        const statusBadge = isPass 
          ? `<span class="item-badge" style="background:var(--cyber-green-dim); color:var(--cyber-green);">PASS</span>`
          : isWarn 
          ? `<span class="item-badge" style="background:rgba(255,179,0,0.15); color:var(--sev-medium);">WARN</span>`
          : `<span class="item-badge" style="background:rgba(255,82,82,0.15); color:var(--sev-critical);">MISSING</span>`;
        return `
          <tr>
            <td style="font-weight:700; color:var(--text-primary); font-size:11px;">${escapeHtml(item.name || item.header)}</td>
            <td>${statusBadge}</td>
            <td style="color:${isPass ? 'var(--cyber-cyan)' : 'var(--text-muted)'}; font-size:10px; word-break:break-all;">
              ${escapeHtml(item.value || item.details || item.remediation || '')}
            </td>
          </tr>
        `;
      }).join('');

      let disclosuresHtml = '';
      if (d.server && d.server !== 'Redacted') {
        disclosuresHtml = `
          <div style="background:rgba(255,179,0,0.1); border:1px solid var(--sev-medium); border-radius:4px; padding:8px 10px; margin-top:8px; font-size:10.5px;">
            <strong style="color:var(--sev-medium);">ℹ️ Server Header:</strong> <span style="color:#fff;">${escapeHtml(d.server)}</span>
          </div>
        `;
      }

      const passedCount = d.summary?.passed !== undefined ? d.summary.passed : (findingsList.filter(f => f.status === 'PASS').length);
      const failedCount = d.summary?.failed !== undefined ? d.summary.failed : (findingsList.filter(f => f.status === 'FAIL').length);

      outputEl.innerHTML = `
        <div style="display:flex; align-items:center; justify-content:space-between; background:var(--surface-2); padding:10px 14px; border-radius:var(--radius-sm); margin-bottom:10px;">
          <div>
            <div style="font-size:10px; color:var(--text-secondary); font-family:var(--font-mono);">SECURITY COMPLIANCE SCORE</div>
            <div style="display:flex; align-items:baseline; gap:8px; margin-top:2px;">
              <span style="font-family:var(--font-mono); font-size:24px; font-weight:800; color:${gradeColor};">${score}%</span>
              <span class="item-badge" style="border:1px solid ${gradeColor}; color:${gradeColor}; font-size:12px; font-weight:800;">GRADE ${d.grade}</span>
            </div>
          </div>
          <div style="text-align:right; font-family:var(--font-mono); font-size:10.5px; color:var(--text-muted);">
            <div>Status: <span style="color:var(--text-primary); font-weight:700;">${d.status_code || 200}</span></div>
            <div>Passed: <span style="color:var(--cyber-green); font-weight:700;">${passedCount}</span> • Failed: <span style="color:var(--sev-critical); font-weight:700;">${failedCount}</span></div>
          </div>
        </div>

        <table class="tool-data-table">
          <thead><tr><th>Header Name</th><th>Status</th><th>Value / Recommendation</th></tr></thead>
          <tbody>${checksHtml}</tbody>
        </table>
        ${disclosuresHtml}
      `;
      showToast(`Header audit complete: Grade ${d.grade} (${score}%)`);
    } else {
      throw new Error(res.message || 'Header scan failed');
    }
  } catch (err) {
    setToolStatus('badge-headers-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyHeadersResult() {
  if (!toolkitState.lastHeaders) return;
  copyToClipboard(JSON.stringify(toolkitState.lastHeaders, null, 2), 'Header audit copied');
}

function exportHeadersResult() {
  if (!toolkitState.lastHeaders) return;
  const hostname = new URL(toolkitState.lastHeaders.target_url).hostname;
  downloadTextFile(JSON.stringify(toolkitState.lastHeaders, null, 2), `headers-${hostname}-${Date.now()}.json`);
}

async function saveHeaderFindingsToAssessment() {
  if (!toolkitState.lastHeaders) {
    showToast('No header audit to log', 'error');
    return;
  }
  const activeAsm = state.activeAssessment || (state.assessments && state.assessments[0]);
  if (!activeAsm) {
    showToast('Please create or select an assessment first', 'error');
    return;
  }

  const findingsList = toolkitState.lastHeaders.findings || [];
  const missing = findingsList.filter(c => c.status === 'FAIL');
  if (missing.length === 0) {
    showToast('No missing security headers detected! Target is compliant.', 'info');
    return;
  }

  const targetUrl = toolkitState.lastHeaders.target_url;
  const missingNames = missing.map(m => m.name || m.header).join(', ');
  const score = toolkitState.lastHeaders.security_score !== undefined ? toolkitState.lastHeaders.security_score : (toolkitState.lastHeaders.score || 0);

  try {
    const res = await apiFetch(`/assessments/${activeAsm.id}/findings`, {
      method: 'POST',
      body: JSON.stringify({
        title: `Missing Defensive HTTP Headers on ${targetUrl}`,
        severity: 'MEDIUM',
        cvss_score: 5.3,
        cvss_vector: 'CVSS:3.1/AV:N/AC:L/PR:N/UI:N/S:U/C:L/I:N/A:N',
        cwe_id: 'CWE-693',
        description: `Automated header inspection identified missing defensive security headers on ${targetUrl}. Missing: ${missingNames}. Grade: ${toolkitState.lastHeaders.grade} (${score}% compliance).`,
        impact: 'Increased vulnerability to clickjacking, cross-site scripting (XSS), MIME-type sniffing, or SSL stripping.',
        remediation_guidance: 'Configure production reverse proxy / web server to inject Strict-Transport-Security, Content-Security-Policy, and X-Content-Type-Options headers.',
        status: 'OPEN',
        affected_asset: targetUrl
      })
    });

    if (res.status === 'success') {
      showToast('Logged finding into Assessment Workspace!');
      await loadFindings();
      await loadDashboardMetrics();
    } else {
      showToast(res.message || 'Failed to save finding', 'error');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function runSslInspect() {
  const host = (document.getElementById('input-ssl-host').value || '').trim();
  const authChecked = document.getElementById('auth-check-ssl') ? document.getElementById('auth-check-ssl').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required for TLS certificate inspection.', 'error');
    return;
  }
  const port = parseInt(document.getElementById('input-ssl-port').value, 10) || 443;
  const resultBox = document.getElementById('result-ssl-box');
  const outputEl = document.getElementById('output-ssl-records');
  const spinner = document.getElementById('btn-ssl-spinner');
  const btn = document.getElementById('btn-run-ssl');

  if (!host) {
    showToast('Please enter target hostname (e.g. example.com)', 'error');
    return;
  }

  setToolStatus('badge-ssl-status', 'running', 'CONNECTING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/web/ssl', {
      method: 'POST',
      body: JSON.stringify({ host, port, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastSsl = res.data;
      setToolStatus('badge-ssl-status', 'success', res.data.is_valid ? 'VALID' : 'EXPIRED');
      resultBox.classList.add('active');
      const d = res.data;

      const sansHtml = (d.subject_alt_names && d.subject_alt_names.length) 
        ? d.subject_alt_names.slice(0, 8).map(s => `<span class="item-badge badge-responsible-pill" style="font-size:9.5px;">${escapeHtml(s)}</span>`).join(' ')
        : 'None specified';

      outputEl.innerHTML = `
        <table class="tool-data-table">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">Subject (CN):</td><td><strong style="color:var(--cyber-cyan); font-size:12px;">${escapeHtml(d.subject.common_name || d.host)}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Issuer:</td><td><strong>${escapeHtml(d.issuer.organization || d.issuer.common_name || 'Unknown')}</strong> &nbsp;<span style="font-size:10px; color:var(--text-muted);">(${escapeHtml(d.issuer.common_name || '')})</span></td></tr>
            <tr><td style="color:var(--text-muted);">Validity:</td><td>${new Date(d.valid_from).toLocaleDateString()} &rarr; <strong>${new Date(d.valid_to).toLocaleDateString()}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Lifespan Remaining:</td><td><strong style="color:${d.days_remaining > 30 ? 'var(--cyber-green)' : 'var(--sev-critical)'};">${d.days_remaining} days</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Protocol & Cipher:</td><td><span class="item-badge badge-in-scope">${d.protocol}</span> &nbsp;<span style="font-size:10px; color:var(--text-secondary);">${d.cipher.name} (${d.cipher.bits}-bit)</span></td></tr>
            <tr><td style="color:var(--text-muted);">SAN Domains:</td><td>${sansHtml} ${d.subject_alt_names && d.subject_alt_names.length > 8 ? `<span style="font-size:9.5px; color:var(--text-muted);">+${d.subject_alt_names.length - 8} more</span>` : ''}</td></tr>
            <tr><td style="color:var(--text-muted);">SHA-256 Fingerprint:</td><td style="font-size:9.5px; color:#A7F3D0; word-break:break-all;">${d.fingerprint_sha256}</td></tr>
          </tbody>
        </table>
      `;
      showToast(`TLS Certificate inspected: ${d.is_valid ? 'Valid' : 'Expired'} (${d.days_remaining} days left)`);
    } else {
      throw new Error(res.message || 'SSL inspection failed');
    }
  } catch (err) {
    setToolStatus('badge-ssl-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copySslResult() {
  if (!toolkitState.lastSsl) return;
  copyToClipboard(JSON.stringify(toolkitState.lastSsl, null, 2), 'TLS certificate info copied');
}

function exportSslResult() {
  if (!toolkitState.lastSsl) return;
  downloadTextFile(JSON.stringify(toolkitState.lastSsl, null, 2), `ssl-${toolkitState.lastSsl.host}-${Date.now()}.json`);
}

async function runRedirectTrace() {
  const url = (document.getElementById('input-redirect-url').value || '').trim();
  const authChecked = document.getElementById('auth-check-redirects') ? document.getElementById('auth-check-redirects').checked : true;
  if (!authChecked) {
    showToast('Explicit authorization confirmation required for redirect tracing.', 'error');
    return;
  }
  const resultBox = document.getElementById('result-redirects-box');
  const outputEl = document.getElementById('output-redirect-hops');
  const spinner = document.getElementById('btn-redirects-spinner');
  const btn = document.getElementById('btn-run-redirects');

  if (!url) {
    showToast('Please enter target URL to trace', 'error');
    return;
  }

  setToolStatus('badge-redirects-status', 'running', 'TRACING');
  if (spinner) spinner.style.display = 'inline-block';
  if (btn) btn.disabled = true;

  try {
    const res = await apiFetch('/tools/web/redirects', {
      method: 'POST',
      body: JSON.stringify({ url, authorized: true })
    });

    if (res.status === 'success') {
      toolkitState.lastRedirects = res.data;
      setToolStatus('badge-redirects-status', 'success', `${res.data.total_hops} HOPS`);
      resultBox.classList.add('active');

      const hopsHtml = res.data.hops.map(h => {
        const isRedirect = h.status_code >= 300 && h.status_code < 400;
        const isSuccess = h.status_code >= 200 && h.status_code < 300;
        let badgeColor = isRedirect ? 'var(--cyber-cyan)' : isSuccess ? 'var(--cyber-green)' : 'var(--sev-critical)';

        return `
          <div class="hop-item">
            <div class="hop-number">${h.hop}</div>
            <div class="hop-details">
              <div class="hop-url">${escapeHtml(h.url)}</div>
              <div class="hop-meta">
                <span class="item-badge" style="border:1px solid ${badgeColor}; color:${badgeColor}; font-size:9.5px;">${h.status_code} ${h.status_text}</span>
                ${h.redirect_to ? `&nbsp; &rarr; &nbsp; <span style="color:var(--text-secondary);">${escapeHtml(h.redirect_to)}</span>` : ''}
              </div>
            </div>
          </div>
        `;
      }).join('');

      outputEl.innerHTML = `
        <div style="font-size:11px; color:var(--text-muted); font-family:var(--font-mono); margin-bottom:8px;">
          Initial: <span style="color:var(--text-primary);">${escapeHtml(res.data.initial_url)}</span> &rarr; Final: <strong style="color:var(--cyber-green);">${escapeHtml(res.data.final_url)}</strong>
        </div>
        <div class="hop-timeline">${hopsHtml}</div>
      `;
      showToast(`Redirect trace complete: ${res.data.total_hops} hops`);
    } else {
      throw new Error(res.message || 'Redirect trace failed');
    }
  } catch (err) {
    setToolStatus('badge-redirects-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  } finally {
    if (spinner) spinner.style.display = 'none';
    if (btn) btn.disabled = false;
  }
}

function copyRedirectsResult() {
  if (!toolkitState.lastRedirects) return;
  copyToClipboard(JSON.stringify(toolkitState.lastRedirects, null, 2), 'Redirect hops copied');
}

function exportRedirectsResult() {
  if (!toolkitState.lastRedirects) return;
  downloadTextFile(JSON.stringify(toolkitState.lastRedirects, null, 2), `redirects-${Date.now()}.json`);
}

// ------------------------------------------------------------------------------
// 3. SECURITY UTILITIES CONTROLLERS
// ------------------------------------------------------------------------------

async function runFormatConversion() {
  const input = document.getElementById('input-convert-text').value;
  const from = document.getElementById('select-convert-from').value;
  const to = document.getElementById('select-convert-to').value;
  const outputEl = document.getElementById('output-convert-text');
  const metaEl = document.getElementById('label-convert-meta');

  setToolStatus('badge-convert-status', 'running', 'CONVERTING');

  try {
    const res = await apiFetch('/tools/crypto/convert', {
      method: 'POST',
      body: JSON.stringify({ input, from, to })
    });

    if (res.status === 'success') {
      toolkitState.lastConvert = res.data;
      setToolStatus('badge-convert-status', 'success', 'READY');
      outputEl.value = res.data.output;
      metaEl.textContent = `Converted: ${res.data.from.toUpperCase()} → ${res.data.to.toUpperCase()} (${res.data.outputLength} chars)`;
    } else {
      throw new Error(res.message || 'Conversion failed');
    }
  } catch (err) {
    setToolStatus('badge-convert-status', 'error', 'FAILED');
    outputEl.value = `Error: ${err.message}`;
    showToast(err.message, 'error');
  }
}

function swapConverterFormats() {
  const fromEl = document.getElementById('select-convert-from');
  const toEl = document.getElementById('select-convert-to');
  const inputEl = document.getElementById('input-convert-text');
  const outputEl = document.getElementById('output-convert-text');

  const oldFrom = fromEl.value;
  fromEl.value = toEl.value;
  toEl.value = oldFrom;

  if (outputEl.value) {
    inputEl.value = outputEl.value;
  }
  runFormatConversion();
}

function copyConvertResult() {
  const text = document.getElementById('output-convert-text').value;
  if (!text) return;
  copyToClipboard(text, 'Converted payload copied to clipboard');
}

function exportConvertResult() {
  const text = document.getElementById('output-convert-text').value;
  if (!text) return;
  downloadTextFile(text, `converted-payload-${Date.now()}.txt`);
}

async function runHashGeneration() {
  const input = document.getElementById('input-hash-text').value;
  const salt = document.getElementById('input-hash-salt').value;
  const hmacKey = document.getElementById('input-hash-hmac').value;
  const resultBox = document.getElementById('result-hash-box');
  const outputEl = document.getElementById('output-hash-records');

  setToolStatus('badge-hash-status', 'running', 'HASHING');

  try {
    const res = await apiFetch('/tools/crypto/hash', {
      method: 'POST',
      body: JSON.stringify({ input, salt, hmacKey })
    });

    if (res.status === 'success') {
      toolkitState.lastHashes = res.data;
      setToolStatus('badge-hash-status', 'success', 'READY');
      resultBox.classList.add('active');

      let rows = res.data.hashes.map(h => `
        <tr>
          <td style="color:var(--cyber-cyan); font-weight:700; width:110px;">${escapeHtml(h.algorithm)}</td>
          <td style="color:var(--text-muted); font-size:10px; width:70px;">${h.bit_length}-bit</td>
          <td style="word-break:break-all; font-family:var(--font-mono); font-size:10.5px; color:#A7F3D0;">
            ${escapeHtml(h.hex)}
          </td>
          <td style="width:40px; text-align:right;">
            <button class="tool-result-btn" onclick="copySingleHash('${h.hex}')" title="Copy ${h.algorithm}">📋</button>
          </td>
        </tr>
      `).join('');

      if (res.data.hmac) {
        rows += `
          <tr style="background:rgba(0,229,255,0.04);">
            <td style="color:var(--cyber-green); font-weight:700;">${escapeHtml(res.data.hmac.algorithm)}</td>
            <td style="color:var(--text-muted); font-size:10px;">HMAC-SHA256</td>
            <td style="word-break:break-all; font-family:var(--font-mono); font-size:10.5px; color:var(--cyber-green);">
              ${escapeHtml(res.data.hmac.hex)}
            </td>
            <td style="text-align:right;">
              <button class="tool-result-btn" onclick="copySingleHash('${res.data.hmac.hex}')" title="Copy HMAC">📋</button>
            </td>
          </tr>
        `;
      }

      outputEl.innerHTML = `
        <table class="tool-data-table">
          <thead><tr><th>Algorithm</th><th>Bits</th><th>Hex Digest</th><th>Copy</th></tr></thead>
          <tbody>${rows}</tbody>
        </table>
      `;
      showToast('Cryptographic hashes generated');
    } else {
      throw new Error(res.message || 'Hash generation failed');
    }
  } catch (err) {
    setToolStatus('badge-hash-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  }
}

function copySingleHash(hex) {
  copyToClipboard(hex, 'Hash digest copied to clipboard');
}

function copyAllHashes() {
  if (!toolkitState.lastHashes) return;
  const lines = toolkitState.lastHashes.hashes.map(h => `${h.algorithm}: ${h.hex}`);
  if (toolkitState.lastHashes.hmac) {
    lines.push(`HMAC-SHA256: ${toolkitState.lastHashes.hmac.hex}`);
  }
  copyToClipboard(lines.join('\n'), 'All hashes copied to clipboard');
}

function exportHashesJson() {
  if (!toolkitState.lastHashes) return;
  downloadTextFile(JSON.stringify(toolkitState.lastHashes, null, 2), `hashes-${Date.now()}.json`);
}

async function runHashIdentification() {
  const hash = (document.getElementById('input-identify-hash').value || '').trim();
  const resultBox = document.getElementById('result-identify-box');
  const outputEl = document.getElementById('output-identify-records');
  const metaEl = document.getElementById('label-identify-meta');

  if (!hash) {
    showToast('Please enter a hash string to identify', 'error');
    return;
  }

  setToolStatus('badge-identify-status', 'running', 'IDENTIFYING');

  try {
    const res = await apiFetch('/tools/crypto/identify', {
      method: 'POST',
      body: JSON.stringify({ hash })
    });

    if (res.status === 'success') {
      toolkitState.lastIdentify = res.data;
      setToolStatus('badge-identify-status', 'success', 'MATCHED');
      resultBox.classList.add('active');
      const d = res.data;

      metaEl.textContent = `Analyzed ${d.char_length} characters (Hex: ${d.is_hex ? 'Yes' : 'No'}) • ${d.candidate_count || 0} Candidates`;

      if (!d.matched || !d.candidates || d.candidates.length === 0) {
        outputEl.innerHTML = `<div style="padding:10px; color:var(--text-muted); font-size:11px;">Unknown hash signature or unsupported encoding format.</div>`;
      } else {
        const rows = d.candidates.map(c => `
          <tr>
            <td style="color:var(--cyber-cyan); font-weight:700;">${escapeHtml(c.algorithm)}</td>
            <td style="color:var(--text-secondary); font-size:10px;">${escapeHtml(c.category)}</td>
            <td>
              ${c.hashcat_mode !== null ? `<span class="item-badge badge-in-scope" style="font-size:9.5px;">mode ${c.hashcat_mode}</span>` : '<span style="color:var(--text-muted); font-size:9.5px;">N/A</span>'}
            </td>
            <td>
              <span class="item-badge ${c.confidence === 'HIGH' ? 'badge-responsible-pill' : 'badge-sev-info'}" style="font-size:9px;">
                ${c.confidence}
              </span>
            </td>
          </tr>
        `).join('');

        outputEl.innerHTML = `
          <table class="tool-data-table">
            <thead><tr><th>Algorithm</th><th>Category</th><th>Hashcat Mode</th><th>Confidence</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        `;
      }
      showToast(`Hash identified: ${d.candidate_count || 0} candidate algorithms`);
    } else {
      throw new Error(res.message || 'Identification failed');
    }
  } catch (err) {
    setToolStatus('badge-identify-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  }
}

function copyIdentifyResult() {
  if (!toolkitState.lastIdentify) return;
  copyToClipboard(JSON.stringify(toolkitState.lastIdentify, null, 2), 'Hash signature candidates copied');
}

function exportIdentifyResult() {
  if (!toolkitState.lastIdentify) return;
  downloadTextFile(JSON.stringify(toolkitState.lastIdentify, null, 2), `hash-identity-${Date.now()}.json`);
}

// ------------------------------------------------------------------------------
// 4. CVSS v3.1 CALCULATOR CONTROLLERS
// ------------------------------------------------------------------------------

function setCvssMetric(metric, val, btn) {
  toolkitState.cvssMetrics[metric] = val;
  const parent = btn.closest('.cvss-btn-row');
  if (parent) {
    parent.querySelectorAll('.cvss-btn-opt').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
  }
  recalcCvss();
}

async function recalcCvss() {
  try {
    const res = await apiFetch('/tools/cvss/calculate', {
      method: 'POST',
      body: JSON.stringify({ metrics: toolkitState.cvssMetrics })
    });

    if (res.status === 'success') {
      toolkitState.lastCvss = res.data;
      const d = res.data;

      // Update vector input
      const vecInput = document.getElementById('input-cvss-vector');
      if (vecInput) vecInput.value = d.vector;

      // Update score display
      const scoreVal = document.getElementById('cvss-score-val');
      const sevPill = document.getElementById('cvss-severity-pill');
      const subEl = document.getElementById('cvss-subscores-text');

      if (scoreVal) {
        scoreVal.textContent = d.baseScore.toFixed(1);
        let sevColor = 'var(--text-muted)';
        if (d.severity === 'Critical') sevColor = 'var(--sev-critical)';
        else if (d.severity === 'High') sevColor = 'var(--sev-high)';
        else if (d.severity === 'Medium') sevColor = 'var(--sev-medium)';
        else if (d.severity === 'Low') sevColor = 'var(--sev-low)';
        scoreVal.style.color = sevColor;
      }

      if (sevPill) {
        sevPill.textContent = d.severity.toUpperCase();
        sevPill.className = `item-badge badge-sev-${d.severity.toLowerCase()}`;
      }

      if (subEl) {
        subEl.textContent = `Exploitability: ${d.exploitabilityScore} • Impact: ${d.impactScore} • ISS: ${d.iss}`;
      }
    }
  } catch (err) {
    console.error('CVSS calc error:', err);
  }
}

async function loadCvssVectorFromInput() {
  const vector = (document.getElementById('input-cvss-vector').value || '').trim();
  if (!vector) return;

  try {
    const res = await apiFetch('/tools/cvss/calculate', {
      method: 'POST',
      body: JSON.stringify({ vector })
    });

    if (res.status === 'success') {
      toolkitState.lastCvss = res.data;
      const metrics = res.data.metrics;
      for (const k in metrics) {
        const val = metrics[k].code;
        toolkitState.cvssMetrics[k] = val;
        const btn = document.querySelector(`.cvss-btn-opt[data-metric="${k}"][data-val="${val}"]`);
        if (btn) {
          const parent = btn.closest('.cvss-btn-row');
          if (parent) {
            parent.querySelectorAll('.cvss-btn-opt').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
          }
        }
      }
      recalcCvss();
      showToast(`Vector parsed: Score ${res.data.baseScore} (${res.data.severity})`);
    } else {
      showToast(res.message || 'Invalid CVSS vector string', 'error');
    }
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function copyCvssVector() {
  if (!toolkitState.lastCvss) return;
  copyToClipboard(toolkitState.lastCvss.vector, 'CVSS v3.1 vector string copied');
}

function useCvssInFindingModal() {
  if (!toolkitState.lastCvss) return;
  openModal('modal-new-finding');

  const cvssInput = document.getElementById('fnd-cvss');
  const sevSelect = document.getElementById('fnd-severity');
  if (cvssInput) cvssInput.value = toolkitState.lastCvss.baseScore;
  if (sevSelect) sevSelect.value = toolkitState.lastCvss.severity.toUpperCase();

  showToast(`Applied CVSS ${toolkitState.lastCvss.baseScore} (${toolkitState.lastCvss.severity}) to New Finding`);
}

// ==============================================================================
// EXTENDED SECURITY UTILITIES CONTROLLERS (7 CORE TOOLS)
// ==============================================================================

// ------------------------------------------------------------------------------
// TOOL 1: Hash Generator (Text & File)
// ------------------------------------------------------------------------------
function switchHashMode(mode) {
  const isText = mode === 'text';
  document.getElementById('btn-hash-mode-text').classList.toggle('active', isText);
  document.getElementById('btn-hash-mode-file').classList.toggle('active', !isText);
  document.getElementById('hash-text-container').style.display = isText ? 'flex' : 'none';
  document.getElementById('hash-file-container').style.display = isText ? 'none' : 'flex';
}

// Drag & drop file listeners
document.addEventListener('DOMContentLoaded', () => {
  const dropZone = document.getElementById('file-drop-zone');
  if (dropZone) {
    dropZone.addEventListener('dragover', (e) => {
      e.preventDefault();
      dropZone.classList.add('dragover');
    });
    dropZone.addEventListener('dragleave', () => {
      dropZone.classList.remove('dragover');
    });
    dropZone.addEventListener('drop', (e) => {
      e.preventDefault();
      dropZone.classList.remove('dragover');
      if (e.dataTransfer.files && e.dataTransfer.files[0]) {
        processFileForHashing(e.dataTransfer.files[0]);
      }
    });
  }
});

function handleFileHashSelect(event) {
  if (event.target.files && event.target.files[0]) {
    processFileForHashing(event.target.files[0]);
  }
}

async function processFileForHashing(file) {
  if (!file) return;

  const resultBox = document.getElementById('result-hash-box');
  const outputEl = document.getElementById('output-hash-records');
  const metaEl = document.getElementById('file-hash-meta');
  const labelMeta = document.getElementById('label-hash-meta');

  setToolStatus('badge-hash-status', 'running', 'HASHING FILE');
  if (metaEl) {
    metaEl.style.display = 'block';
    metaEl.textContent = `Computing hashes for: ${file.name} (${(file.size / 1024).toFixed(1)} KB)...`;
  }

  try {
    const arrayBuffer = await file.arrayBuffer();

    // Use Web Crypto API for ultra-fast local SHA digests
    async function digestBuffer(algo) {
      const hashBuffer = await crypto.subtle.digest(algo, arrayBuffer);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    }

    const sha1Hex = await digestBuffer('SHA-1');
    const sha256Hex = await digestBuffer('SHA-256');
    const sha384Hex = await digestBuffer('SHA-384');
    const sha512Hex = await digestBuffer('SHA-512');

    // Also call backend to get exact MD5 + HMAC verification if connected
    let md5Hex = 'Calculating...';
    try {
      const formData = new FormData();
      formData.append('file', file);
      // Fallback MD5 computation or local slice
      const sampleText = new TextDecoder('utf-8', { fatal: false }).decode(arrayBuffer.slice(0, 1024 * 1024));
      const res = await apiFetch('/tools/crypto/hash', {
        method: 'POST',
        body: JSON.stringify({ input: sampleText })
      });
      if (res.status === 'success') {
        const md5Obj = res.data.hashes.find(h => h.algorithm === 'MD5');
        if (md5Obj) md5Hex = md5Obj.hex;
      }
    } catch {
      md5Hex = 'Client MD5 (Legacy)';
    }

    const fileHashes = [
      { algorithm: 'SHA-256', bits: 256, hex: sha256Hex },
      { algorithm: 'SHA-512', bits: 512, hex: sha512Hex },
      { algorithm: 'SHA-384', bits: 384, hex: sha384Hex },
      { algorithm: 'SHA-1', bits: 160, hex: sha1Hex },
      { algorithm: 'MD5 (Sample)', bits: 128, hex: md5Hex }
    ];

    toolkitState.lastHashes = {
      file_name: file.name,
      file_size_bytes: file.size,
      hashes: fileHashes
    };

    setToolStatus('badge-hash-status', 'success', 'HASHED');
    resultBox.classList.add('active');
    labelMeta.textContent = `File Hashes: ${file.name} (${(file.size / 1024).toFixed(1)} KB)`;

    const rows = fileHashes.map(h => `
      <tr>
        <td style="color:var(--cyber-cyan); font-weight:700; width:110px;">${escapeHtml(h.algorithm)}</td>
        <td style="color:var(--text-muted); font-size:10px; width:70px;">${h.bits}-bit</td>
        <td style="word-break:break-all; font-family:var(--font-mono); font-size:10.5px; color:#A7F3D0;">
          ${escapeHtml(h.hex)}
        </td>
        <td style="width:40px; text-align:right;">
          <button class="tool-result-btn" onclick="copySingleHash('${h.hex}')" title="Copy ${h.algorithm}">📋</button>
        </td>
      </tr>
    `).join('');

    outputEl.innerHTML = `
      <table class="tool-data-table">
        <thead><tr><th>Algorithm</th><th>Bits</th><th>Hex Digest</th><th>Copy</th></tr></thead>
        <tbody>${rows}</tbody>
      </table>
    `;

    showToast(`File hashed: ${file.name}`);
  } catch (err) {
    setToolStatus('badge-hash-status', 'error', 'FAILED');
    showToast(`Failed to hash file: ${err.message}`, 'error');
  }
}

// ------------------------------------------------------------------------------
// TOOL 2: JWT Decoder (100% Client-Side Local Execution)
// ------------------------------------------------------------------------------
function loadSampleJwt() {
  const now = Math.floor(Date.now() / 1000);
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  const payload = btoa(JSON.stringify({
    sub: 'usr-shadow-99',
    name: 'Authorized Security Operator',
    role: 'SECURITY_AUDITOR',
    iss: 'https://auth.redops.local',
    aud: 'https://api.redops.local',
    iat: now - 3600,
    exp: now + 7200,
    scopes: ['assessments:read', 'recon:execute', 'findings:write']
  })).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
  const sig = 's@mpleSignature_dBjftJeZ4CVP_mB92K27uhbUJU1p1r_wW1gFWFOEjXk';

  document.getElementById('input-jwt-token').value = `${header}.${payload}.${sig}`;
  runJwtDecode();
}

function runJwtDecode() {
  const token = (document.getElementById('input-jwt-token').value || '').trim();
  const resultBox = document.getElementById('result-jwt-box');
  const outputEl = document.getElementById('output-jwt-records');

  if (!token) {
    showToast('Please enter or paste a JWT token', 'error');
    return;
  }

  setToolStatus('badge-jwt-status', 'running', 'DECODING');

  try {
    const cleanToken = token.replace(/^Bearer\s+/i, '');
    const parts = cleanToken.split('.');

    if (parts.length !== 3) {
      throw new Error(`Invalid JWT format. Expected 3 dot-separated segments (Header.Payload.Signature), but got ${parts.length}.`);
    }

    function b64UrlDecode(str) {
      let b64 = str.replace(/-/g, '+').replace(/_/g, '/');
      while (b64.length % 4) b64 += '=';
      return decodeURIComponent(escape(atob(b64)));
    }

    const header = JSON.parse(b64UrlDecode(parts[0]));
    const payload = JSON.parse(b64UrlDecode(parts[1]));
    const signature = parts[2];

    toolkitState.lastJwt = { header, payload, signature };

    // Format timestamps
    const now = Math.floor(Date.now() / 1000);
    let expInfo = null;
    let iatInfo = null;

    if (payload.exp) {
      const expDate = new Date(payload.exp * 1000);
      const diffSec = payload.exp - now;
      const isExpired = diffSec < 0;
      const absDiff = Math.abs(diffSec);
      const timeStr = absDiff < 3600 ? `${Math.floor(absDiff / 60)} minutes` : absDiff < 86400 ? `${Math.floor(absDiff / 3600)} hours` : `${Math.floor(absDiff / 86400)} days`;
      expInfo = {
        date: expDate.toLocaleString(),
        isExpired,
        relative: isExpired ? `Expired ${timeStr} ago` : `Expires in ${timeStr}`
      };
    }

    if (payload.iat) {
      const iatDate = new Date(payload.iat * 1000);
      const diffSec = now - payload.iat;
      const timeStr = diffSec < 3600 ? `${Math.floor(diffSec / 60)}m` : `${Math.floor(diffSec / 3600)}h`;
      iatInfo = {
        date: iatDate.toLocaleString(),
        relative: `Issued ${timeStr} ago`
      };
    }

    setToolStatus('badge-jwt-status', 'success', expInfo && expInfo.isExpired ? 'EXPIRED' : 'DECODED');
    resultBox.classList.add('active');

    outputEl.innerHTML = `
      <div style="display:flex; gap:10px; flex-wrap:wrap; margin-bottom:10px;">
        <div class="jwt-section-card" style="flex:1; min-width:220px;">
          <div class="jwt-section-title">
            <span>HEADER (Algorithm & Type)</span>
            <span class="item-badge badge-in-scope" style="font-size:9.5px;">${escapeHtml(header.alg || 'none')}</span>
          </div>
          <pre class="tool-code-output" style="max-height:120px; font-size:10.5px;">${escapeHtml(JSON.stringify(header, null, 2))}</pre>
        </div>

        <div class="jwt-section-card" style="flex:1; min-width:220px;">
          <div class="jwt-section-title">
            <span>TIMESTAMP CLAIMS</span>
            ${expInfo ? `<span class="item-badge ${expInfo.isExpired ? 'badge-sev-critical' : 'badge-responsible-pill'}" style="font-size:9.5px;">${expInfo.relative}</span>` : ''}
          </div>
          <div style="font-family:var(--font-mono); font-size:10.5px; display:flex; flex-direction:column; gap:4px; padding:6px 0;">
            ${expInfo ? `<div><span style="color:var(--text-muted);">Expiration (exp):</span> <strong style="color:${expInfo.isExpired ? 'var(--sev-critical)' : 'var(--cyber-green)'};">${expInfo.date}</strong></div>` : '<div><span style="color:var(--text-muted);">Expiration (exp):</span> None (Never expires)</div>'}
            ${iatInfo ? `<div><span style="color:var(--text-muted);">Issued At (iat):</span> ${iatInfo.date} (${iatInfo.relative})</div>` : ''}
            ${payload.iss ? `<div><span style="color:var(--text-muted);">Issuer (iss):</span> ${escapeHtml(payload.iss)}</div>` : ''}
            ${payload.sub ? `<div><span style="color:var(--text-muted);">Subject (sub):</span> <strong style="color:var(--cyber-cyan);">${escapeHtml(payload.sub)}</strong></div>` : ''}
          </div>
        </div>
      </div>

      <div class="jwt-section-card">
        <div class="jwt-section-title">
          <span>PAYLOAD (Decoded Claims)</span>
          <button class="tool-result-btn" onclick="copyJwtPayload()">📋 Copy JSON</button>
        </div>
        <pre class="tool-code-output" style="max-height:280px; font-size:11px;">${escapeHtml(JSON.stringify(payload, null, 2))}</pre>
      </div>
    `;

    showToast('JWT decoded locally in browser');
  } catch (err) {
    setToolStatus('badge-jwt-status', 'error', 'INVALID');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px; font-family:var(--font-mono); font-size:11px;">Error decoding JWT: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  }
}

function copyJwtPayload() {
  if (!toolkitState.lastJwt || !toolkitState.lastJwt.payload) return;
  copyToClipboard(JSON.stringify(toolkitState.lastJwt.payload, null, 2), 'JWT payload copied to clipboard');
}

function copyAllJwtDecoded() {
  if (!toolkitState.lastJwt) return;
  copyToClipboard(JSON.stringify(toolkitState.lastJwt, null, 2), 'All JWT components copied to clipboard');
}

// ------------------------------------------------------------------------------
// TOOL 3: URL Analyzer
// ------------------------------------------------------------------------------
async function runUrlAnalysis() {
  const url = (document.getElementById('input-url-target').value || '').trim();
  const resultBox = document.getElementById('result-url-box');
  const outputEl = document.getElementById('output-url-records');

  if (!url) {
    showToast('Please enter a target URL to analyze', 'error');
    return;
  }

  setToolStatus('badge-url-status', 'running', 'PARSING');

  try {
    const res = await apiFetch('/tools/web/url-analyze', {
      method: 'POST',
      body: JSON.stringify({ url })
    });

    if (res.status === 'success') {
      toolkitState.lastUrlAnalysis = res.data;
      const d = res.data;

      if (d.is_malformed) {
        setToolStatus('badge-url-status', 'error', 'MALFORMED');
        resultBox.classList.add('active');
        outputEl.innerHTML = `
          <div style="background:rgba(255,82,82,0.1); border:1px solid var(--sev-critical); color:#FFA4A4; padding:12px; border-radius:4px; font-family:var(--font-mono); font-size:11px;">
            <strong>⚠️ Malformed URL Detected:</strong> ${escapeHtml(d.error)}
          </div>
        `;
        showToast('Malformed URL detected', 'error');
        return;
      }

      setToolStatus('badge-url-status', 'success', 'PARSED');
      resultBox.classList.add('active');

      let queryParamsHtml = '<div style="padding:8px; color:var(--text-muted); font-size:10.5px;">No query parameters in URL.</div>';
      if (d.query_params && d.query_params.length > 0) {
        const rows = d.query_params.map(p => `
          <tr>
            <td style="color:var(--cyber-cyan); font-weight:700;">${escapeHtml(p.key)}</td>
            <td style="word-break:break-all;">${escapeHtml(p.value)}</td>
            <td style="color:var(--text-secondary); font-size:10px; word-break:break-all;">${escapeHtml(p.decoded_value)}</td>
          </tr>
        `).join('');

        queryParamsHtml = `
          <table class="tool-data-table">
            <thead><tr><th>Parameter Key</th><th>Raw Value</th><th>Decoded Value</th></tr></thead>
            <tbody>${rows}</tbody>
          </table>
        `;
      }

      let suspiciousHtml = '<div style="font-size:10.5px; color:var(--cyber-green); padding:6px 0;">✓ No suspicious structural anomalies flagged.</div>';
      if (d.suspicious_indicators && d.suspicious_indicators.length > 0) {
        const items = d.suspicious_indicators.map(item => `
          <div style="background:rgba(255,179,0,0.08); border-left:3px solid var(--sev-${item.severity === 'HIGH' ? 'critical' : 'medium'}); padding:6px 10px; margin-bottom:4px; font-size:10.5px; font-family:var(--font-mono);">
            <strong style="color:var(--sev-${item.severity === 'HIGH' ? 'critical' : 'medium'});">${item.type}:</strong> ${escapeHtml(item.message)}
          </div>
        `).join('');

        suspiciousHtml = `
          <div style="margin-top:8px;">
            <div style="font-size:11px; font-weight:700; color:var(--text-primary); margin-bottom:4px;">Heuristic Observations (${d.suspicious_indicators.length}):</div>
            ${items}
          </div>
        `;
      }

      outputEl.innerHTML = `
        <table class="tool-data-table" style="margin-bottom:10px;">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">Scheme / Protocol:</td><td><span class="item-badge ${d.scheme === 'https' ? 'badge-in-scope' : 'badge-sev-medium'}">${d.scheme.toUpperCase()}</span></td></tr>
            <tr><td style="color:var(--text-muted);">Hostname:</td><td><strong style="color:var(--cyber-cyan); font-size:12px;">${escapeHtml(d.hostname)}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Port:</td><td>${d.port}</td></tr>
            <tr><td style="color:var(--text-muted);">Path:</td><td>${escapeHtml(d.path)}</td></tr>
            ${d.fragment ? `<tr><td style="color:var(--text-muted);">Fragment (#):</td><td><span style="color:#A7F3D0;">#${escapeHtml(d.fragment)}</span></td></tr>` : ''}
            ${d.username ? `<tr><td style="color:var(--text-muted);">Credentials:</td><td><span style="color:var(--sev-critical);">User: ${escapeHtml(d.username)} (Embedded auth detected)</span></td></tr>` : ''}
          </tbody>
        </table>

        <div style="margin-top:10px;">
          <div style="font-size:11px; font-weight:700; font-family:var(--font-mono); color:var(--text-primary); margin-bottom:4px;">Query Parameters (${d.query_param_count}):</div>
          ${queryParamsHtml}
        </div>

        ${suspiciousHtml}
      `;

      showToast(`URL parsed: ${d.hostname}`);
    }
  } catch (err) {
    setToolStatus('badge-url-status', 'error', 'FAILED');
    showToast(err.message, 'error');
  }
}

function copyUrlAnalysis() {
  if (!toolkitState.lastUrlAnalysis) return;
  copyToClipboard(JSON.stringify(toolkitState.lastUrlAnalysis, null, 2), 'URL analysis copied');
}

function exportUrlAnalysis() {
  if (!toolkitState.lastUrlAnalysis) return;
  downloadTextFile(JSON.stringify(toolkitState.lastUrlAnalysis, null, 2), `url-analysis-${Date.now()}.json`);
}

// ------------------------------------------------------------------------------
// TOOL 4: Subnet Calculator (in Utilities)
// ------------------------------------------------------------------------------
async function runUtilSubnetCalc() {
  const cidr = (document.getElementById('input-util-subnet-cidr').value || '').trim();
  const resultBox = document.getElementById('result-util-subnet-box');
  const outputEl = document.getElementById('output-util-subnet-records');

  if (!cidr) {
    showToast('Please enter an IPv4 address with CIDR (e.g. 10.0.0.0/16)', 'error');
    return;
  }

  setToolStatus('badge-util-subnet-status', 'running', 'CALCULATING');

  try {
    const res = await apiFetch('/tools/network/subnet', {
      method: 'POST',
      body: JSON.stringify({ cidr })
    });

    if (res.status === 'success') {
      toolkitState.lastUtilSubnet = res.data;
      setToolStatus('badge-util-subnet-status', 'success', 'READY');
      resultBox.classList.add('active');
      const d = res.data;

      outputEl.innerHTML = `
        <table class="tool-data-table">
          <tbody>
            <tr><td style="color:var(--text-muted); width:130px;">CIDR Block:</td><td><strong style="color:var(--cyber-cyan); font-size:13px;">${escapeHtml(d.cidrNotation)}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Netmask:</td><td>${d.netmask} &nbsp;<span style="color:var(--text-muted); font-size:10px;">(Wildcard: ${d.wildcard})</span></td></tr>
            <tr><td style="color:var(--text-muted);">Network Address:</td><td><strong>${d.network}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Broadcast Address:</td><td><strong>${d.broadcast}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Usable Host Range:</td><td><strong style="color:var(--cyber-green);">${d.usableHostRange}</strong></td></tr>
            <tr><td style="color:var(--text-muted);">Usable Hosts:</td><td><strong>${d.usableHosts}</strong> / ${d.totalHosts} total</td></tr>
            <tr><td style="color:var(--text-muted);">Scope & Class:</td><td><span class="item-badge ${d.isRoutable ? 'badge-in-scope' : 'badge-responsible-pill'}">${d.scope}</span> &nbsp;<span style="font-size:10px; color:var(--text-secondary);">${d.ipClass}</span></td></tr>
          </tbody>
        </table>
      `;
      showToast(`Subnet calculated: ${d.usableHosts} usable hosts`);
    } else {
      throw new Error(res.message || 'Subnet calculation failed');
    }
  } catch (err) {
    setToolStatus('badge-util-subnet-status', 'error', 'FAILED');
    resultBox.classList.add('active');
    outputEl.innerHTML = `<div style="color:var(--sev-critical); padding:10px;">Error: ${escapeHtml(err.message)}</div>`;
    showToast(err.message, 'error');
  }
}

function copyUtilSubnetResult() {
  if (!toolkitState.lastUtilSubnet) return;
  copyToClipboard(JSON.stringify(toolkitState.lastUtilSubnet, null, 2), 'Subnet parameters copied');
}

function exportUtilSubnetResult() {
  if (!toolkitState.lastUtilSubnet) return;
  downloadTextFile(JSON.stringify(toolkitState.lastUtilSubnet, null, 2), `subnet-${toolkitState.lastUtilSubnet.network}-${Date.now()}.json`);
}

// ------------------------------------------------------------------------------
// TOOL 6: Password Strength Estimator (100% Client-Side Local Execution)
// ------------------------------------------------------------------------------
function togglePasswordVisibility() {
  const input = document.getElementById('input-pwd-evaluate');
  if (input) {
    input.type = input.type === 'password' ? 'text' : 'password';
  }
}

function evaluatePasswordStrengthLive(pwd) {
  const scoreValEl = document.getElementById('pwd-score-val');
  const strengthPillEl = document.getElementById('pwd-strength-pill');
  const barEl = document.getElementById('pwd-meter-bar');
  const entropyEl = document.getElementById('pwd-entropy-val');
  const lengthEl = document.getElementById('pwd-length-val');
  const crackTimesEl = document.getElementById('pwd-crack-times');
  const suggestionsEl = document.getElementById('pwd-suggestions');

  if (!pwd) {
    if (scoreValEl) scoreValEl.textContent = '0%';
    if (strengthPillEl) {
      strengthPillEl.textContent = 'ENTER PASSWORD';
      strengthPillEl.className = 'item-badge';
    }
    if (barEl) barEl.style.width = '0%';
    if (entropyEl) entropyEl.textContent = '0 bits';
    if (lengthEl) lengthEl.textContent = '0';
    if (crackTimesEl) crackTimesEl.innerHTML = '';
    if (suggestionsEl) suggestionsEl.innerHTML = '';
    document.querySelectorAll('.pwd-pill').forEach(p => p.classList.remove('active'));
    setToolStatus('badge-pwd-status', 'ready', 'READY');
    return;
  }

  const length = pwd.length;
  const hasLower = /[a-z]/.test(pwd);
  const hasUpper = /[A-Z]/.test(pwd);
  const hasDigits = /[0-9]/.test(pwd);
  const hasSymbols = /[^a-zA-Z0-9\s]/.test(pwd);

  // Update pills
  document.getElementById('pill-pwd-len').classList.toggle('active', length >= 14);
  document.getElementById('pill-pwd-lower').classList.toggle('active', hasLower);
  document.getElementById('pill-pwd-upper').classList.toggle('active', hasUpper);
  document.getElementById('pill-pwd-num').classList.toggle('active', hasDigits);
  document.getElementById('pill-pwd-sym').classList.toggle('active', hasSymbols);

  let poolSize = 0;
  if (hasLower) poolSize += 26;
  if (hasUpper) poolSize += 26;
  if (hasDigits) poolSize += 10;
  if (hasSymbols) poolSize += 33;
  if (/\s/.test(pwd)) poolSize += 1;

  const entropyBits = Math.round(length * Math.log2(poolSize || 1) * 10) / 10;

  // Patterns check
  const lowerPwd = pwd.toLowerCase();
  const patterns = [];
  const commonWords = ['password', '123456', 'qwerty', 'admin', 'welcome', 'login', 'shadow'];
  for (const c of commonWords) {
    if (lowerPwd.includes(c)) patterns.push(`Contains dictionary word "${c}"`);
  }
  if (/(.)\1{2,}/.test(pwd)) patterns.push('Contains 3+ repeated characters');

  // Score
  let score = 0;
  if (length >= 8) score += 15;
  if (length >= 12) score += 20;
  if (length >= 16) score += 20;
  if (length >= 20) score += 10;
  if (hasLower && hasUpper) score += 15;
  if (hasDigits) score += 10;
  if (hasSymbols) score += 15;
  if (patterns.length > 0) score = Math.max(10, score - (patterns.length * 20));
  if (length < 8) score = Math.min(score, 25);
  score = Math.min(100, Math.max(5, score));

  let strengthLabel = 'Very Weak';
  let barColor = 'var(--sev-critical)';
  let pillClass = 'badge-sev-critical';

  if (score >= 85) {
    strengthLabel = 'Very Strong';
    barColor = 'var(--cyber-green)';
    pillClass = 'badge-responsible-pill';
  } else if (score >= 65) {
    strengthLabel = 'Strong';
    barColor = 'var(--cyber-cyan)';
    pillClass = 'badge-in-scope';
  } else if (score >= 45) {
    strengthLabel = 'Fair';
    barColor = 'var(--sev-medium)';
    pillClass = 'badge-sev-medium';
  } else if (score >= 25) {
    strengthLabel = 'Weak';
    barColor = 'var(--sev-high)';
    pillClass = 'badge-sev-high';
  }

  setToolStatus('badge-pwd-status', 'success', strengthLabel.toUpperCase());
  if (scoreValEl) {
    scoreValEl.textContent = `${score}%`;
    scoreValEl.style.color = barColor;
  }
  if (strengthPillEl) {
    strengthPillEl.textContent = strengthLabel.toUpperCase();
    strengthPillEl.className = `item-badge ${pillClass}`;
  }
  if (barEl) {
    barEl.style.width = `${score}%`;
    barEl.style.background = barColor;
  }
  if (entropyEl) entropyEl.textContent = `${entropyBits} bits`;
  if (lengthEl) lengthEl.textContent = length;

  // Crack times
  const combinations = Math.pow(poolSize || 1, length);
  function fmtTime(sec) {
    if (sec < 1) return '< 1 second';
    if (sec < 60) return `${Math.round(sec)} seconds`;
    if (sec < 3600) return `${Math.round(sec / 60)} minutes`;
    if (sec < 86400) return `${Math.round(sec / 3600)} hours`;
    if (sec < 31536000) return `${Math.round(sec / 86400)} days`;
    if (sec < 31536000 * 1000) return `${Math.round(sec / 31536000)} years`;
    return `${(sec / 31536000000000).toExponential(1)} million years`;
  }

  if (crackTimesEl) {
    crackTimesEl.innerHTML = `
      Estimated Crack Times: 
      <strong style="color:var(--text-primary);">${fmtTime(combinations / 1.66)}</strong> (Online Throttled) • 
      <strong style="color:${score > 60 ? 'var(--cyber-green)' : 'var(--sev-critical)'};">${fmtTime(combinations / 10000000000)}</strong> (Offline GPU Hash)
    `;
  }

  // Suggestions
  const suggestions = [];
  if (length < 14) suggestions.push('Increase length to 14+ characters for exponential entropy.');
  if (!hasSymbols) suggestions.push('Include special symbols (!@#$%^&*) to defeat dictionaries.');
  if (!hasDigits) suggestions.push('Include numbers.');
  if (patterns.length > 0) suggestions.push('Avoid dictionary words and sequential patterns.');

  if (suggestionsEl) {
    if (suggestions.length === 0) {
      suggestionsEl.innerHTML = '<span style="color:var(--cyber-green);">✓ Excellent password complexity and entropy!</span>';
    } else {
      suggestionsEl.innerHTML = `<span style="color:var(--sev-medium);">💡 Recommendations:</span> ${suggestions.join(' • ')}`;
    }
  }
}

// ------------------------------------------------------------------------------
// TOOL 7: JSON Formatter & Validator
// ------------------------------------------------------------------------------
function formatJsonText(spaces = 2) {
  const editor = document.getElementById('input-json-editor');
  const diagBox = document.getElementById('json-diagnostics-box');
  const raw = editor.value.trim();

  if (!raw) {
    showToast('Please enter JSON text', 'error');
    return;
  }

  try {
    const parsed = JSON.parse(raw);
    editor.value = JSON.stringify(parsed, null, spaces);
    setToolStatus('badge-json-status', 'success', 'VALID JSON');

    diagBox.style.display = 'block';
    diagBox.innerHTML = `
      <div style="background:var(--cyber-green-dim); border:1px solid var(--cyber-green); color:var(--cyber-green); padding:8px 12px; border-radius:4px; font-family:var(--font-mono); font-size:11px; margin-top:8px;">
        ✓ Valid JSON syntax! Pretty-printed with ${spaces} spaces. (${Buffer.byteLength(editor.value, 'utf8')} bytes)
      </div>
    `;
    showToast('JSON formatted successfully');
  } catch (err) {
    handleJsonParseError(raw, err.message);
  }
}

function minifyJsonText() {
  const editor = document.getElementById('input-json-editor');
  const diagBox = document.getElementById('json-diagnostics-box');
  const raw = editor.value.trim();

  if (!raw) {
    showToast('Please enter JSON text', 'error');
    return;
  }

  try {
    const parsed = JSON.parse(raw);
    const minified = JSON.stringify(parsed);
    const oldBytes = Buffer.byteLength(raw, 'utf8');
    const newBytes = Buffer.byteLength(minified, 'utf8');
    editor.value = minified;
    setToolStatus('badge-json-status', 'success', 'MINIFIED');

    diagBox.style.display = 'block';
    diagBox.innerHTML = `
      <div style="background:var(--cyber-green-dim); border:1px solid var(--cyber-green); color:var(--cyber-green); padding:8px 12px; border-radius:4px; font-family:var(--font-mono); font-size:11px; margin-top:8px;">
        ✓ Minified successfully: ${oldBytes} bytes &rarr; <strong>${newBytes} bytes</strong> (${Math.round((1 - (newBytes/oldBytes))*100)}% reduction)
      </div>
    `;
    showToast('JSON minified');
  } catch (err) {
    handleJsonParseError(raw, err.message);
  }
}

function handleJsonParseError(rawJson, errMessage) {
  const diagBox = document.getElementById('json-diagnostics-box');
  setToolStatus('badge-json-status', 'error', 'SYNTAX ERROR');

  const lines = rawJson.split('\n');
  let lineNum = 1;
  let colNum = 1;

  const posMatch = errMessage.match(/position\s+(\d+)/i);
  if (posMatch) {
    const pos = parseInt(posMatch[1], 10);
    let cur = 0;
    for (let i = 0; i < lines.length; i++) {
      if (cur + lines[i].length + 1 > pos) {
        lineNum = i + 1;
        colNum = pos - cur + 1;
        break;
      }
      cur += lines[i].length + 1;
    }
  } else {
    const tokenMatch = errMessage.match(/Unexpected token\s+['"]?([^'",\s]+)['"]?/i);
    if (tokenMatch && tokenMatch[1]) {
      for (let i = 0; i < lines.length; i++) {
        const idx = lines[i].indexOf(tokenMatch[1]);
        if (idx !== -1) {
          lineNum = i + 1;
          colNum = idx + 1;
          break;
        }
      }
    }
  }

  const errLine = lines[lineNum - 1] || '';
  const pointer = ' '.repeat(Math.max(0, colNum - 1)) + '^';

  diagBox.style.display = 'block';
  diagBox.innerHTML = `
    <div class="json-error-box">
      <strong>⚠️ JSON Syntax Error:</strong> ${escapeHtml(errMessage)}
      <div style="margin-top:4px; font-size:10px; color:#FFCDD2;">Error at Line ${lineNum}, Column ${colNum}:</div>
      <div class="json-error-snippet">${escapeHtml(errLine)}\n<span style="color:#FF5252; font-weight:bold;">${pointer}</span></div>
    </div>
  `;
  showToast(`JSON Syntax Error at line ${lineNum}`, 'error');
}

function copyJsonText() {
  const editor = document.getElementById('input-json-editor');
  if (!editor || !editor.value) return;
  copyToClipboard(editor.value, 'JSON copied to clipboard');
}

function clearJsonEditor() {
  const editor = document.getElementById('input-json-editor');
  if (editor) editor.value = '';
  const diag = document.getElementById('json-diagnostics-box');
  if (diag) diag.style.display = 'none';
  setToolStatus('badge-json-status', 'ready', 'READY');
}
