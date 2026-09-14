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
};

export const chickenSaleApi = {
  getAll: (params) => api.get('/api/chicken-sales', { params }),
  getById: (id) => api.get(`/api/chicken-sales/${id}`),
  create: (data) => api.post('/api/chicken-sales', data),
  update: (id, data) => api.put(`/api/chicken-sales/${id}`, data),
  confirm: (id) => api.post(`/api/chicken-sales/${id}/confirm`),
  invoice: (id) => api.post(`/api/chicken-sales/${id}/invoice`),
  pay: (id, data) => api.post(`/api/chicken-sales/${id}/payments`, data),
  complete: (id) => api.post(`/api/chicken-sales/${id}/complete`),
  cancel: (id) => api.post(`/api/chicken-sales/${id}/cancel`),
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
  getCurrentStock: () => api.get('/api/inventory/items').then((res) => {
    const items = normalizeList(res.data);
    return { ...res, data: { data: items } };
  }),
  getLowStock: () => api.get('/api/inventory/low-stock'),
  getExpiryAlerts: () => api.get('/api/inventory/expiring'),
  getMovements: () => api.get('/api/inventory/movements/recent'),
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
  getPrescription: (id) => api.get(`/api/vet/prescriptions/${id}`),
  createPrescription: (data) => api.post('/api/vet/prescriptions', data),
  submitPrescription: (id) => api.post(`/api/vet/prescriptions/${id}/submit`),
  cancelPrescription: (id) => api.post(`/api/vet/prescriptions/${id}/cancel`),
  dispensePrescription: (id, data) => api.patch(`/api/vet/prescriptions/${id}/dispense`, data || {}),
  getDrugUsageReport: (params) => api.get('/api/vet/reports/drug-usage', { params }),
  getFlockEMR: (flockId) => api.get(`/api/vet/flocks/${flockId}/emr`),
  logObservation: (data) => api.post('/api/vet/observations', data),
  createNecropsy: (data) => api.post('/api/vet/necropsies', data),
  setWithdrawal: (flockId, days) => api.post(`/api/vet/flocks/${flockId}/withdrawal`, null, { params: { days } }),
};

export const veterinaryApi = vetApi;

export const pharmacyApi = {
  getDashboard: () => api.get('/api/pharmacy/dashboard'),
  getPharmacyCatalog: () => api.get('/api/inventory/pharmacy-items').then((res) => {
    const items = Array.isArray(res.data) ? res.data : res.data?.data || [];
    return { ...res, data: { data: items } };
  }),
  getCustomers: () => api.get('/api/pharmacy/customers').then((res) => {
    const customers = Array.isArray(res.data) ? res.data : res.data?.data || [];
    return { ...res, data: { data: customers } };
  }),
  createCustomer: (data) => api.post('/api/pharmacy/customers', data),
  getSales: () => api.get('/api/pharmacy/sales'),
  createSale: (data) => api.post('/api/pharmacy/sales', data),
  // Canonical prescription workflow (same entity as /api/vet/prescriptions)
  getPrescriptions: (params) => api.get('/api/pharmacy/prescriptions', { params }),
  getPrescription: (id) => api.get(`/api/pharmacy/prescriptions/${id}`),
  approvePrescription: (id) => api.post(`/api/pharmacy/prescriptions/${id}/approve`),
  rejectPrescription: (id, reason) => api.post(`/api/pharmacy/prescriptions/${id}/reject`, { reason }),
  dispensePrescription: (id, data) => api.post(`/api/pharmacy/prescriptions/${id}/dispense`, data || {}),
  getReceipt: (id) => api.get(`/api/pharmacy/sales/${id}/receipt`),
};

export const financeApi = {
  getDashboard: () => api.get('/api/finance/dashboard'),
  getTransactions: () => api.get('/api/finance/transactions'),
  getReports: (from, to) => api.get(`/api/finance/reports/profit-loss?from=${from}&to=${to}`),
  getBalanceSheet: () => api.get('/api/finance/reports/balance-sheet'),
  getCashFlow: (from, to) => api.get(`/api/finance/reports/cash-flow?from=${from}&to=${to}`),
  getReconciliations: () => api.get('/api/finance/reconciliations'),
  reconcileCash: (data) => api.post('/api/finance/reconciliation', data),
  createPOSInvoice: (data) => api.post('/api/finance/invoices/pos', data),
  postTransaction: (data) => api.post('/api/finance/transactions', data),
  getAccounts: () => api.get('/api/finance/accounts'),
  getAuditTrail: () => api.get('/api/finance/audit-trail'),
  getInvoices: () => api.get('/api/finance/invoices'),
  getReceivables: () => api.get('/api/finance/receivables'),
  getExpectedCash: () => api.get('/api/finance/cash/expected'),
  getWallets: () => api.get('/api/finance/wallets'),
  getWalletSummary: () => api.get('/api/finance/wallets/summary'),
  depositWallet: (data) => api.post('/api/finance/wallets/deposit', data),
  getFlockProfitLoss: (flockId) => api.get(`/api/finance/flocks/${flockId}/profit-loss`),
};




export const crmApi = {
  getClients: () => api.get('/api/crm/clients'),
  getClient: (id) => api.get(`/api/crm/clients/${id}`),
  createClient: (data) => api.post('/api/crm/clients', data),
  updateClient: (id, data) => api.put(`/api/crm/clients/${id}`, data),
  getLeads: () => api.get('/api/crm/leads'),
  getPipeline: () => api.get('/api/crm/pipeline'),
  getDashboard: () => api.get('/api/crm/dashboard'),
  getVisits: (params) => api.get('/api/crm/farm-visits', { params }),
  createVisit: (data) => api.post('/api/crm/farm-visits', data),
  getFollowUps: () => api.get('/api/crm/follow-ups'),
  getOrders: () => api.get('/api/crm/orders'),
};

export const trainingApi = {
  getDashboard: () => api.get('/api/training/dashboard'),
  getReports: () => api.get('/api/training/reports'),
  getPrograms: () => api.get('/api/training/programs'),
  getProgram: (id) => api.get(`/api/training/programs/${id}`),
  createProgram: (data) => api.post('/api/training/programs', data),
  updateProgram: (id, data) => api.put(`/api/training/programs/${id}`, data),
  publishProgram: (id) => api.post(`/api/training/programs/${id}/publish`),
  openRegistration: (id) => api.post(`/api/training/programs/${id}/open-registration`),
  closeRegistration: (id) => api.post(`/api/training/programs/${id}/close-registration`),
  startProgram: (id) => api.post(`/api/training/programs/${id}/start`),
  completeProgram: (id) => api.post(`/api/training/programs/${id}/complete`),
  cancelProgram: (id) => api.post(`/api/training/programs/${id}/cancel`),
  getSessions: (params) => api.get('/api/training/sessions', { params }),
  getSession: (id) => api.get(`/api/training/sessions/${id}`),
  createSession: (data) => api.post('/api/training/sessions', data),
  getRegistrations: (params) => api.get('/api/training/registrations', { params }),
  getRegistration: (id) => api.get(`/api/training/registrations/${id}`),
  register: (data) => api.post('/api/training/registrations', data),
  confirmRegistration: (id) => api.post(`/api/training/registrations/${id}/confirm`),
  cancelRegistration: (id) => api.post(`/api/training/registrations/${id}/cancel`),
  pay: (id, data) => api.post(`/api/training/registrations/${id}/payments`, data),
  recordAttendance: (id, data) => api.post(`/api/training/registrations/${id}/attendance`, data),
  completeRegistration: (id) => api.post(`/api/training/registrations/${id}/complete`),
  issueCertificate: (id, data) => api.post(`/api/training/registrations/${id}/certificate`, data || {}),
  revokeCertificate: (id) => api.post(`/api/training/registrations/${id}/certificate/revoke`),
  getCertificates: () => api.get('/api/training/certificates'),
  verifyCertificate: (code) => api.get(`/api/public/certificates/verify/${code}`),
};

export const marketingApi = {
  getDashboard: () => api.get('/api/marketing/dashboard'),
  getCampaigns: () => api.get('/api/marketing/campaigns'),
  createCampaign: (data) => api.post('/api/marketing/campaigns', data),
  getLeads: () => api.get('/api/marketing/leads'),
  createLead: (data) => api.post('/api/marketing/leads', data),
  convertLead: (id) => api.post(`/api/marketing/leads/${id}/convert`),
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
  crm: () => api.get('/api/dashboard/crm'),
  marketing: () => api.get('/api/dashboard/marketing'),
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
