import api from './axios';

export { api };

export const authApi = {
  login: (data) => api.post('/api/auth/login', data),
  me: () => api.get('/api/auth/me'),
};

export const userApi = {
  getAll: () => api.get('/api/users'),
  getById: (id) => api.get(`/api/users/${id}`),
  create: (data) => api.post('/api/users', data),
  update: (id, data) => api.put(`/api/users/${id}`, data),
  updateStatus: (id, status) => api.patch(`/api/users/${id}/status`, { status }),
};

export const farmApi = {
  getAll: () => api.get('/api/farms'),
  getById: (id) => api.get(`/api/farms/${id}`),
  create: (data) => api.post('/api/farms', data),
  update: (id, data) => api.put(`/api/farms/${id}`, data),
  updateStatus: (id, status) => api.patch(`/api/farms/${id}/status`, { status }),
  getBiosecurityLogs: (id) => api.get(`/api/farms/${id}/biosecurity`),
  logBiosecurity: (id, data) => api.post(`/api/farms/${id}/biosecurity`, data),
  getBarns: () => api.get('/api/farms/barns'),
};

export const flockApi = {
  getAll: () => api.get('/api/flocks'),
  getById: (id) => api.get(`/api/flocks/${id}`),
  create: (data) => api.post('/api/flocks', data),
  update: (id, data) => api.put(`/api/flocks/${id}`, data),
  close: (id) => api.patch(`/api/flocks/${id}/close`),
  getGrowth: (id) => api.get(`/api/flocks/${id}/growth`),
  addGrowth: (id, data) => api.post(`/api/flocks/${id}/growth`, data),
  getEggProduction: (id) => api.get(`/api/flocks/${id}/eggs`),
  addEggProduction: (id, data) => api.post(`/api/flocks/${id}/eggs`, data),
  getFCR: (id) => api.get(`/api/flocks/${id}/fcr`),
};

export const dailyRecordApi = {
  getAll: (params) => api.get('/api/daily-farm-records', { params }),
  getById: (id) => api.get(`/api/daily-farm-records/${id}`),
  create: (data) => api.post('/api/daily-farm-records', data),
  update: (id, data) => api.put(`/api/daily-farm-records/${id}`, data),
};

const normalizeList = (payload) => (Array.isArray(payload) ? payload : payload?.data || []);

export const inventoryApi = {
  getItems: () => api.get('/api/inventory/items').then((res) => {
    const items = normalizeList(res.data);
    return { ...res, data: { data: items } };
  }),
  getItem: (id) => api.get(`/api/inventory/items/${id}`),
  createItem: (data) => api.post('/api/inventory/items', data),
  updateItem: (id, data) => api.put(`/api/inventory/items/${id}`, data),
  stockIn: (data) => api.post('/api/inventory/stock-in', data),
  stockOut: (data) => api.post('/api/inventory/stock-out', data),
  getCurrentStock: () => api.get('/api/inventory/current-stock'),
  getLowStock: () => api.get('/api/inventory/low-stock'),
  getExpiryAlerts: () => api.get('/api/inventory/expiry-alerts'),
  getMovements: () => api.get('/api/inventory/movements'),
  getValuationReport: () => api.get('/api/inventory/reports/valuation'),
  getDashboardStats: () => api.get('/api/inventory/stats'),
};

export const requisitionApi = {
  getAll: () => api.get('/api/inventory/requisitions'),
  create: (data) => api.post('/api/inventory/requisitions', data),
  approve: (id) => api.put(`/api/inventory/requisitions/${id}/approve`),
  issue: (id) => api.put(`/api/inventory/requisitions/${id}/issue`),
};

export const supplierApi = {
  getAll: () => api.get('/api/inventory/suppliers'),
  getById: (id) => api.get(`/api/inventory/suppliers/${id}`),
  create: (data) => api.post('/api/inventory/suppliers', data),
  update: (id, data) => api.put(`/api/inventory/suppliers/${id}`, data),
  delete: (id) => api.delete(`/api/inventory/suppliers/${id}`),
};

export const vetApi = {
  getDashboardStats: () => api.get('/api/vet/dashboard-stats'),
  getVaccinations: () => api.get('/api/vet/vaccinations'),
  createVaccination: (data) => api.post('/api/vet/vaccinations', data),
  completeVaccination: (id) => api.patch(`/api/vet/vaccinations/${id}/complete`),
  getHealthReports: () => api.get('/api/vet/health-reports'),
  createHealthReport: (data) => api.post('/api/vet/health-reports', data),
  reviewHealthReport: (id, data) => api.patch(`/api/vet/health-reports/${id}/review`, data),
  getDiseaseCases: () => api.get('/api/vet/disease-cases'),
  getActiveDiseaseCases: () => api.get('/api/vet/disease-cases/active'),
  createDiseaseCase: (data) => api.post('/api/vet/disease-cases', data),
  updateDiseaseCase: (id, data) => api.put(`/api/vet/disease-cases/${id}`, data),
  getTreatments: () => api.get('/api/vet/treatments'),
  createTreatment: (data) => api.post('/api/vet/treatments', data),
  getPrescriptions: () => api.get('/api/vet/prescriptions'),
  createPrescription: (data) => api.post('/api/vet/prescriptions', data),
  dispensePrescription: (id) => api.patch(`/api/vet/prescriptions/${id}/dispense`),
  getDrugUsageReport: (params) => api.get('/api/vet/reports/drug-usage', { params }),
  getFlockEMR: (flockId) => api.get(`/api/vet/flocks/${flockId}/emr`),
  logObservation: (data) => api.post('/api/vet/observations', data),
  createNecropsy: (data) => api.post('/api/vet/necropsies', data),
  setWithdrawal: (flockId, days) => api.post(`/api/vet/flocks/${flockId}/withdrawal`, null, { params: { days } }),
};

export const veterinaryApi = vetApi;

export const pharmacyApi = {
  getDashboard: () => api.get('/api/pharmacy/dashboard'),
  getCustomers: () => api.get('/api/pharmacy/customers'),
  createCustomer: (data) => api.post('/api/pharmacy/customers', data),
  getSales: () => api.get('/api/pharmacy/sales'),
  createSale: (data) => api.post('/api/pharmacy/sales', data),
  getPrescriptions: () => api.get('/api/vet/prescriptions'),
  dispensePrescription: (id) => api.patch(`/api/vet/prescriptions/${id}/dispense`),
  getReceipt: (id) => api.get(`/api/pharmacy/sales/${id}/receipt`),
};

export const financeApi = {
  getDashboard: () => api.get('/api/finance/dashboard'),
  getTransactions: () => api.get('/api/finance/transactions'),
  getReports: (from, to) => api.get(`/api/finance/reports/profit-loss?from=${from}&to=${to}`),
  getBalanceSheet: () => api.get('/api/finance/reports/balance-sheet'),
  getReconciliations: () => api.get('/api/finance/reconciliations'),
  reconcileCash: (data) => api.post('/api/finance/reconciliation', data),
  createPOSInvoice: (data) => api.post('/api/finance/invoices/pos', data),
  postTransaction: (data) => api.post('/api/finance/transactions', data),
  getAccounts: () => api.get('/api/finance/accounts'),
  getAuditTrail: () => api.get('/api/finance/audit-trail'),
};




export const crmApi = {
  getClients: () => api.get('/api/crm/clients'),
  getClient: (id) => api.get(`/api/crm/clients/${id}`),
  createClient: (data) => api.post('/api/crm/clients', data),
  updateClient: (id, data) => api.put(`/api/crm/clients/${id}`, data),
  getVisits: (params) => api.get('/api/crm/farm-visits', { params }),
  createVisit: (data) => api.post('/api/crm/farm-visits', data),
  getFollowUps: () => api.get('/api/crm/follow-ups'),
  getOrders: () => api.get('/api/crm/orders'),
};

export const notificationApi = {
  getAll: () => api.get('/api/notifications'),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
};

export const dashboardApi = {
  admin: () => api.get('/api/dashboard/admin'),
  farmManager: () => api.get('/api/dashboard/farm-manager'),
  store: () => api.get('/api/dashboard/store'),
  vet: () => api.get('/api/dashboard/vet'),
  pharmacy: () => api.get('/api/dashboard/pharmacy'),
  finance: () => api.get('/api/dashboard/finance'),
};

export const masterDataApi = {
  getByCategory: (category, activeOnly = true) => api.get('/api/master-data', { params: { category, activeOnly } }),
  create: (data) => api.post('/api/master-data', data),
  update: (id, data) => api.put(`/api/master-data/${id}`, data),
  toggleStatus: (id) => api.patch(`/api/master-data/${id}/toggle`),
  delete: (id) => api.delete(`/api/master-data/${id}`),
};

export const auditApi = {
  getAll: (params) => api.get('/api/audit-logs', { params }),
};

export const feedApi = {
  getRecipes: () => api.get('/api/feed/recipes'),
  createRecipe: (data) => api.post('/api/feed/recipes', data),
  getSilos: () => api.get('/api/feed/silos'),
  logDelivery: (data) => api.post('/api/feed/deliveries', data),
};
