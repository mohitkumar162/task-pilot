// ===== Config =====
const BASE_URL = '/api';

// ===== State =====
let currentProjects = [];
let currentTasks = [];
let currentDevelopers = [];
let selectedProjectId = null;
let currentPage = 'projects'; // 'projects' | 'dashboard'
let pendingAssign = {}; // taskId -> selected userId (not yet submitted)

// ===== API helper (mirrors the old api.js) =====
async function apiRequest(path, method = 'GET', body = null) {
  const token = sessionStorage.getItem('token');

  const headers = {
    'Content-Type': 'application/json',
  };

  if (token) {
    headers['Authorization'] = 'Bearer ' + token;
  }

  const response = await fetch(BASE_URL + path, {
    method,
    headers,
    body: body ? JSON.stringify(body) : null,
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || 'Request failed');
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

// ===== DOM refs =====
const authScreen = document.getElementById('auth-screen');
const mainScreen = document.getElementById('main-screen');

const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const authTitle = document.getElementById('auth-title');
const switchLink = document.getElementById('switch-link');
const loginError = document.getElementById('login-error');
const registerError = document.getElementById('register-error');

const userInfoText = document.getElementById('user-info-text');
const logoutBtn = document.getElementById('logout-btn');
const navProjects = document.getElementById('nav-projects');
const navDashboard = document.getElementById('nav-dashboard');
const pageProjects = document.getElementById('page-projects');
const pageDashboard = document.getElementById('page-dashboard');

const createProjectForm = document.getElementById('create-project-form');
const projectNameInput = document.getElementById('project-name');
const projectDescInput = document.getElementById('project-description');
const projectListEl = document.getElementById('project-list');
const projectErrorEl = document.getElementById('project-error');
const noProjectsMsg = document.getElementById('no-projects-msg');

const taskBoardContent = document.getElementById('task-board-content');
const dashboardContent = document.getElementById('dashboard-content');

// ===== Auth: login/register form toggle =====
let isRegisterMode = false;

switchLink.addEventListener('click', () => {
  isRegisterMode = !isRegisterMode;
  authTitle.textContent = isRegisterMode ? 'Create Account' : 'Login';
  loginForm.classList.toggle('hidden', isRegisterMode);
  registerForm.classList.toggle('hidden', !isRegisterMode);
  switchLink.textContent = isRegisterMode
    ? 'Already have an account? Login'
    : "Don't have an account? Register";
  loginError.textContent = '';
  registerError.textContent = '';
});

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  loginError.textContent = '';
  const email = document.getElementById('login-email').value;
  const password = document.getElementById('login-password').value;

  try {
    const data = await apiRequest('/auth/login', 'POST', { email, password });
    sessionStorage.setItem('token', data.token);
    sessionStorage.setItem('role', data.role);
    sessionStorage.setItem('name', data.name);
    onLoginSuccess();
  } catch (err) {
    loginError.textContent = 'Login failed. Check your email and password.';
  }
});

registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  registerError.textContent = '';
  const name = document.getElementById('register-name').value;
  const email = document.getElementById('register-email').value;
  const password = document.getElementById('register-password').value;
  const role = document.getElementById('register-role').value;

  try {
    await apiRequest('/auth/register', 'POST', { name, email, password, role });
    // after registering, log the user straight in
    const data = await apiRequest('/auth/login', 'POST', { email, password });
    sessionStorage.setItem('token', data.token);
    sessionStorage.setItem('role', data.role);
    sessionStorage.setItem('name', data.name);
    onLoginSuccess();
  } catch (err) {
    registerError.textContent = 'Registration failed. Email might already be used.';
  }
});

logoutBtn.addEventListener('click', () => {
  sessionStorage.clear();
  selectedProjectId = null;
  showAuthScreen();
});

function onLoginSuccess() {
  showMainScreen();
}

function showAuthScreen() {
  authScreen.classList.remove('hidden');
  mainScreen.classList.add('hidden');
  loginForm.reset();
  registerForm.reset();
}

function showMainScreen() {
  authScreen.classList.add('hidden');
  mainScreen.classList.remove('hidden');

  const role = sessionStorage.getItem('role');
  const name = sessionStorage.getItem('name');
  userInfoText.textContent = `${name} (${role})`;

  // show/hide manager-only controls
  const canManage = role === 'MANAGER';
  createProjectForm.classList.toggle('hidden', !canManage);

  goToProjectsPage();
  loadProjects();
}

// ===== Nav =====
navProjects.addEventListener('click', goToProjectsPage);
navDashboard.addEventListener('click', goToDashboardPage);

function goToProjectsPage() {
  currentPage = 'projects';
  navProjects.classList.add('active');
  navDashboard.classList.remove('active');
  pageProjects.classList.remove('hidden');
  pageDashboard.classList.add('hidden');
}

function goToDashboardPage() {
  currentPage = 'dashboard';
  navDashboard.classList.add('active');
  navProjects.classList.remove('active');
  pageDashboard.classList.remove('hidden');
  pageProjects.classList.add('hidden');
  loadDashboard();
}

// ===== Projects =====
async function loadProjects() {
  try {
    currentProjects = await apiRequest('/projects');
    renderProjects();
  } catch (err) {
    projectErrorEl.textContent = 'Could not load projects.';
  }
}

function renderProjects() {
  const role = sessionStorage.getItem('role');
  const canManage = role === 'MANAGER';

  projectListEl.innerHTML = '';
  noProjectsMsg.classList.toggle('hidden', currentProjects.length !== 0);

  currentProjects.forEach((project) => {
    const li = document.createElement('li');
    li.className = 'project-item' + (project.id === selectedProjectId ? ' selected' : '');
    li.addEventListener('click', () => {
      selectedProjectId = project.id;
      renderProjects();
      loadTasks();
    });

    const infoDiv = document.createElement('div');
    const strong = document.createElement('strong');
    strong.textContent = project.name;
    const desc = document.createElement('p');
    desc.textContent = project.description || '';
    infoDiv.appendChild(strong);
    infoDiv.appendChild(desc);
    li.appendChild(infoDiv);

    if (canManage) {
      const deleteBtn = document.createElement('button');
      deleteBtn.className = 'danger-btn';
      deleteBtn.textContent = 'Delete';
      deleteBtn.addEventListener('click', (e) => {
        e.stopPropagation();
        handleDeleteProject(project.id);
      });
      li.appendChild(deleteBtn);
    }

    projectListEl.appendChild(li);
  });
}

createProjectForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  projectErrorEl.textContent = '';
  const name = projectNameInput.value;
  const description = projectDescInput.value;

  try {
    await apiRequest('/projects', 'POST', { name, description });
    projectNameInput.value = '';
    projectDescInput.value = '';
    loadProjects();
  } catch (err) {
    projectErrorEl.textContent = 'Could not create project. Only managers can do this.';
  }
});

async function handleDeleteProject(id) {
  try {
    await apiRequest(`/projects/${id}`, 'DELETE');
    if (selectedProjectId === id) {
      selectedProjectId = null;
      renderTaskBoard();
    }
    loadProjects();
  } catch (err) {
    projectErrorEl.textContent = 'Could not delete project.';
  }
}

// ===== Tasks =====
async function loadTasks() {
  if (!selectedProjectId) {
    renderTaskBoard();
    return;
  }
  try {
    const data = await apiRequest('/tasks');
    currentTasks = data.filter((t) => t.project?.id === selectedProjectId);
    const role = sessionStorage.getItem('role');
    if (role === 'MANAGER') {
      await loadDevelopers();
    }
    renderTaskBoard();
  } catch (err) {
    taskBoardContent.innerHTML = `<p class="error-text">Could not load tasks.</p>`;
  }
}

async function loadDevelopers() {
  try {
    currentDevelopers = await apiRequest('/users/developers');
  } catch (err) {
    currentDevelopers = [];
  }
}

function renderTaskBoard() {
  const role = sessionStorage.getItem('role');
  const canManage = role === 'MANAGER';

  if (!selectedProjectId) {
    taskBoardContent.innerHTML = `<p>Select a project on the left to see its tasks.</p>`;
    return;
  }

  const columns = ['TODO', 'IN_PROGRESS', 'COMPLETED'];

  let html = '';

  if (canManage) {
    html += `
      <form id="create-task-form" class="inline-form">
        <input type="text" id="task-title" placeholder="New task title" required />
        <button type="submit">Add Task</button>
      </form>
    `;
  }

  html += `<p class="error-text" id="task-error"></p>`;
  html += `<div class="board">`;

  columns.forEach((column) => {
    html += `<div class="board-column"><h3>${column.replace('_', ' ')}</h3>`;

    currentTasks
      .filter((t) => t.status === column)
      .forEach((task) => {
        html += renderTaskCard(task, canManage);
      });

    html += `</div>`;
  });

  html += `</div>`;

  taskBoardContent.innerHTML = html;

  // wire up events after injecting HTML
  if (canManage) {
    document.getElementById('create-task-form').addEventListener('submit', handleCreateTask);
  }

  currentTasks.forEach((task) => {
    const statusSelect = document.getElementById(`status-${task.id}`);
    if (statusSelect) {
      statusSelect.addEventListener('change', (e) => handleStatusChange(task.id, e.target.value));
    }

    const prioritySelect = document.getElementById(`priority-${task.id}`);
    if (prioritySelect) {
      prioritySelect.addEventListener('change', (e) => handlePriorityChange(task.id, e.target.value));
    }

    const assignSelect = document.getElementById(`assign-${task.id}`);
    if (assignSelect) {
      assignSelect.addEventListener('change', (e) => {
        pendingAssign[task.id] = e.target.value;
        const submitBtn = document.getElementById(`assign-submit-${task.id}`);
        if (submitBtn) submitBtn.disabled = !e.target.value;
      });
    }

    const assignSubmitBtn = document.getElementById(`assign-submit-${task.id}`);
    if (assignSubmitBtn) {
      assignSubmitBtn.addEventListener('click', () => handleAssign(task.id));
    }

    const deadlineInput = document.getElementById(`deadline-${task.id}`);
    if (deadlineInput) {
      deadlineInput.addEventListener('change', (e) => handleDeadlineChange(task.id, e.target.value));
    }
  });
}

function renderTaskCard(task, canManage) {
  let html = `<div class="task-card">`;
  html += `<p class="task-title">${escapeHtml(task.title)}</p>`;

  if (canManage) {
    html += `
      <div class="task-meta priority-row">
        <span>Priority:</span>
        <select class="priority-select" id="priority-${task.id}">
          <option value="LOW" ${task.priority === 'LOW' ? 'selected' : ''}>LOW</option>
          <option value="MEDIUM" ${task.priority === 'MEDIUM' ? 'selected' : ''}>MEDIUM</option>
          <option value="HIGH" ${task.priority === 'HIGH' ? 'selected' : ''}>HIGH</option>
        </select>
      </div>
    `;
  } else {
    html += `<p class="task-meta">Priority: ${task.priority}</p>`;
  }

  html += `<p class="task-meta">Assigned: ${task.assignedUser ? escapeHtml(task.assignedUser.name) : 'Unassigned'}</p>`;

  if (canManage) {
    html += `
      <div class="task-meta deadline-row">
        <span>Deadline:</span>
        <input type="date" class="deadline-input" id="deadline-${task.id}" value="${task.deadline || ''}" />
      </div>
    `;
  } else {
    html += `<p class="task-meta">Deadline: ${task.deadline ? escapeHtml(task.deadline) : 'Not set'}</p>`;
  }

  html += `<div class="task-actions">`;
  html += `
    <select id="status-${task.id}">
      <option value="TODO" ${task.status === 'TODO' ? 'selected' : ''}>TODO</option>
      <option value="IN_PROGRESS" ${task.status === 'IN_PROGRESS' ? 'selected' : ''}>IN_PROGRESS</option>
      <option value="COMPLETED" ${task.status === 'COMPLETED' ? 'selected' : ''}>COMPLETED</option>
    </select>
  `;

  if (canManage) {
    const selectedValue = pendingAssign[task.id] !== undefined
      ? pendingAssign[task.id]
      : (task.assignedUser ? task.assignedUser.id : '');

    html += `<select class="assign-select" id="assign-${task.id}">`;
    html += `<option value="" disabled ${!selectedValue ? 'selected' : ''}>Assign to...</option>`;
    currentDevelopers.forEach((dev) => {
      html += `<option value="${dev.id}" ${String(selectedValue) === String(dev.id) ? 'selected' : ''}>${escapeHtml(dev.name)}</option>`;
    });
    html += `</select>`;

    html += `<button type="button" class="assign-submit-btn" id="assign-submit-${task.id}" ${!selectedValue ? 'disabled' : ''}>Submit</button>`;
  }

  html += `</div></div>`;

  return html;
}

async function handleCreateTask(e) {
  e.preventDefault();
  const taskErrorEl = document.getElementById('task-error');
  taskErrorEl.textContent = '';
  const title = document.getElementById('task-title').value;

  if (!selectedProjectId) {
    taskErrorEl.textContent = 'Select a project first.';
    return;
  }

  try {
    await apiRequest(`/tasks?projectId=${selectedProjectId}`, 'POST', { title });
    loadTasks();
  } catch (err) {
    taskErrorEl.textContent = 'Could not create task. Only managers can do this.';
  }
}

async function handleStatusChange(taskId, newStatus) {
  try {
    await apiRequest(`/tasks/${taskId}/status`, 'PUT', { status: newStatus });
    loadTasks();
  } catch (err) {
    const taskErrorEl = document.getElementById('task-error');
    if (taskErrorEl) taskErrorEl.textContent = 'Could not update status.';
  }
}

async function handlePriorityChange(taskId, newPriority) {
  try {
    await apiRequest(`/tasks/${taskId}/priority`, 'PUT', { priority: newPriority });
    loadTasks();
  } catch (err) {
    const taskErrorEl = document.getElementById('task-error');
    if (taskErrorEl) taskErrorEl.textContent = 'Could not update priority.';
  }
}

async function handleDeadlineChange(taskId, newDeadline) {
  if (!newDeadline) return;
  try {
    await apiRequest(`/tasks/${taskId}/deadline`, 'PUT', { deadline: newDeadline });
    loadTasks();
  } catch (err) {
    const taskErrorEl = document.getElementById('task-error');
    if (taskErrorEl) taskErrorEl.textContent = 'Could not update deadline.';
  }
}

async function handleAssign(taskId) {
  const userId = pendingAssign[taskId];
  if (!userId) return;
  try {
    await apiRequest(`/tasks/${taskId}/assign`, 'PUT', { userId: Number(userId) });
    delete pendingAssign[taskId];
    loadTasks();
  } catch (err) {
    const taskErrorEl = document.getElementById('task-error');
    if (taskErrorEl) taskErrorEl.textContent = 'Could not assign task.';
  }
}

// ===== Dashboard =====
async function loadDashboard() {
  dashboardContent.innerHTML = `<p>Loading dashboard...</p>`;
  try {
    const summary = await apiRequest('/dashboard');
    renderDashboard(summary);
  } catch (err) {
    dashboardContent.innerHTML = `<p class="error-text">Could not load dashboard.</p>`;
  }
}

function renderDashboard(summary) {
  const tasksByDev = summary.tasksByDeveloper || {};

  let html = `
    <div class="stats-grid">
      <div class="stat-card">
        <p class="stat-number">${summary.totalTasks ?? 0}</p>
        <p class="stat-label">Total Tasks</p>
      </div>
      <div class="stat-card">
        <p class="stat-number">${summary.completedTasks ?? 0}</p>
        <p class="stat-label">Completed</p>
      </div>
      <div class="stat-card">
        <p class="stat-number">${summary.inProgressTasks ?? 0}</p>
        <p class="stat-label">In Progress</p>
      </div>
      <div class="stat-card">
        <p class="stat-number">${summary.pendingTasks ?? 0}</p>
        <p class="stat-label">Pending</p>
      </div>
    </div>

    <h3>Tasks per Developer</h3>
    <div class="dev-detail-list">
  `;

  const devNames = Object.keys(tasksByDev);

  devNames.forEach((devName) => {
    const tasks = tasksByDev[devName];
    html += `<div class="dev-detail-card">`;
    html += `<p class="dev-detail-name">${escapeHtml(devName)} — ${tasks.length} task(s)</p>`;
    html += `<ul class="dev-task-list">`;
    tasks.forEach((task) => {
      html += `<li><span class="dev-task-title">${escapeHtml(task.title)}</span>`;
      if (task.project) {
        html += `<span class="dev-task-project"> (${escapeHtml(task.project)})</span>`;
      }
      html += `<span class="dev-task-tags"><span class="tag">${task.status}</span><span class="tag">${task.priority}</span></span></li>`;
    });
    html += `</ul></div>`;
  });

  html += `</div>`;

  if (devNames.length === 0) {
    html += `<p>No tasks assigned yet.</p>`;
  }

  dashboardContent.innerHTML = html;
}

// ===== Utility =====
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

// ===== Init =====
(function init() {
  const token = sessionStorage.getItem('token');
  if (token) {
    showMainScreen();
  } else {
    showAuthScreen();
  }
})();
