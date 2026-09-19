import api from './axios';

export { api };

export const authApi = {
  login: (data) => api.post('/api/auth/login', data),
  me: () => api.get('/api/auth/me'),
  changePassword: (data) => api.post('/api/auth/change-password', data),
  logout: () => api.post('/api/auth/logout'),
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

export const chickenReleaseApi = {
  list: (params) => api.get('/api/chicken-releases', { params }),
  getById: (id) => api.get(`/api/chicken-releases/${id}`),
  create: (data) => api.post('/api/chicken-releases', data),
  verify: (id) => api.post(`/api/chicken-releases/${id}/verify`),
  reject: (id, data) => api.post(`/api/chicken-releases/${id}/reject`, data),
  forwardToFarm: (id) => api.post(`/api/chicken-releases/${id}/forward-to-farm`),
  farmApprove: (id) => api.post(`/api/chicken-releases/${id}/farm-approve`),
  farmReject: (id, data) => api.post(`/api/chicken-releases/${id}/farm-reject`, data),
  dispatch: (id, data) => api.post(`/api/chicken-releases/${id}/dispatch`, data),
  marketingReceive: (id) => api.post(`/api/chicken-releases/${id}/marketing-receive`),
  updateLogistics: (id, data) => api.post(`/api/chicken-releases/${id}/logistics`, data),
  markDelivered: (id) => api.post(`/api/chicken-releases/${id}/mark-delivered`),
};

export const internalDeliveryApi = {
  list: (params) => api.get('/api/internal-deliveries', { params }),
  getById: (id) => api.get(`/api/internal-deliveries/${id}`),
  create: (data) => api.post('/api/internal-deliveries', data),
  update: (id, data) => api.put(`/api/internal-deliveries/${id}`, data),
  submit: (id) => api.post(`/api/internal-deliveries/${id}/submit`),
  approve: (id) => api.post(`/api/internal-deliveries/${id}/approve`),
  issue: (id) => api.post(`/api/internal-deliveries/${id}/issue`),
  receive: (id) => api.post(`/api/internal-deliveries/${id}/receive`),
  cancel: (id) => api.post(`/api/internal-deliveries/${id}/cancel`),
};

export const farmDocumentApi = {
  html: (type, id) => api.get(`/api/farm-documents/${type}/${id}`, { responseType: 'text' }),
};

export const dataImportApi = {
  templates: () => api.get('/api/data-import/templates'),
  onboardingSummary: () => api.get('/api/data-import/onboarding-summary'),
  importData: (data) => api.post('/api/data-import', data),
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

export const procurementApi = {
  getDashboard: () => api.get('/api/procurement/dashboard'),
  getReports: () => api.get('/api/procurement/reports'),
  getApAging: () => api.get('/api/procurement/ap/aging'),
  getSupplierStatement: (supplierId) => api.get(`/api/procurement/suppliers/${supplierId}/statement`),

  getRequisitions: (params) => api.get('/api/procurement/requisitions', { params }),
  getRequisition: (id) => api.get(`/api/procurement/requisitions/${id}`),
  createRequisition: (data) => api.post('/api/procurement/requisitions', data),
  updateRequisition: (id, data) => api.put(`/api/procurement/requisitions/${id}`, data),
  submitRequisition: (id) => api.post(`/api/procurement/requisitions/${id}/submit`),
  reviewRequisition: (id) => api.post(`/api/procurement/requisitions/${id}/review`),
  approveRequisition: (id) => api.post(`/api/procurement/requisitions/${id}/approve`),
  rejectRequisition: (id, data) => api.post(`/api/procurement/requisitions/${id}/reject`, data || {}),
  cancelRequisition: (id) => api.post(`/api/procurement/requisitions/${id}/cancel`),
  convertRequisitionToPo: (id, supplierId) => api.post(`/api/procurement/requisitions/${id}/convert-po`, null, { params: { supplierId } }),

  getPurchaseOrders: (params) => api.get('/api/procurement/purchase-orders', { params }),
  getPurchaseOrder: (id) => api.get(`/api/procurement/purchase-orders/${id}`),
  createPurchaseOrder: (data) => api.post('/api/procurement/purchase-orders', data),
  updatePurchaseOrder: (id, data) => api.put(`/api/procurement/purchase-orders/${id}`, data),
  submitPurchaseOrder: (id) => api.post(`/api/procurement/purchase-orders/${id}/submit`),
  approvePurchaseOrder: (id) => api.post(`/api/procurement/purchase-orders/${id}/approve`),
  sendPurchaseOrder: (id) => api.post(`/api/procurement/purchase-orders/${id}/send`),
  cancelPurchaseOrder: (id) => api.post(`/api/procurement/purchase-orders/${id}/cancel`),
  closePurchaseOrder: (id) => api.post(`/api/procurement/purchase-orders/${id}/close`),

  getGoodsReceipts: (params) => api.get('/api/procurement/goods-receipts', { params }),
  getGoodsReceipt: (id) => api.get(`/api/procurement/goods-receipts/${id}`),
  createGoodsReceipt: (data) => api.post('/api/procurement/goods-receipts', data),
  postGoodsReceipt: (id) => api.post(`/api/procurement/goods-receipts/${id}/post`),
  cancelGoodsReceipt: (id) => api.post(`/api/procurement/goods-receipts/${id}/cancel`),

  getSupplierInvoices: (params) => api.get('/api/procurement/supplier-invoices', { params }),
  getSupplierInvoice: (id) => api.get(`/api/procurement/supplier-invoices/${id}`),
  createSupplierInvoice: (data) => api.post('/api/procurement/supplier-invoices', data),
  submitSupplierInvoice: (id) => api.post(`/api/procurement/supplier-invoices/${id}/submit`),
  matchSupplierInvoice: (id) => api.post(`/api/procurement/supplier-invoices/${id}/match`),
  approveSupplierInvoice: (id) => api.post(`/api/procurement/supplier-invoices/${id}/approve`),
  cancelSupplierInvoice: (id) => api.post(`/api/procurement/supplier-invoices/${id}/cancel`),

  getPayments: () => api.get('/api/procurement/supplier-payments'),
  getPayment: (id) => api.get(`/api/procurement/supplier-payments/${id}`),
  createPayment: (data) => api.post('/api/procurement/supplier-payments', data),
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
  getClients: (params) => api.get('/api/crm/clients', { params }),
  getClient: (id) => api.get(`/api/crm/clients/${id}`),
  getCustomer360: (id) => api.get(`/api/crm/clients/${id}/360`),
  createClient: (data) => api.post('/api/crm/clients', data),
  updateClient: (id, data) => api.put(`/api/crm/clients/${id}`, data),
  getClientLeads: () => api.get('/api/crm/client-leads'),

  getLeads: (params) => api.get('/api/crm/leads', { params }),
  getLead: (id) => api.get(`/api/crm/leads/${id}`),
  createLead: (data) => api.post('/api/crm/leads', data),
  updateLead: (id, data) => api.put(`/api/crm/leads/${id}`, data),
  changeLeadStatus: (id, data) => api.patch(`/api/crm/leads/${id}/status`, data),
  convertLead: (id, data) => api.post(`/api/crm/leads/${id}/convert`, data || {}),

  getOpportunities: (params) => api.get('/api/crm/opportunities', { params }),
  getOpportunity: (id) => api.get(`/api/crm/opportunities/${id}`),
  createOpportunity: (data) => api.post('/api/crm/opportunities', data),
  updateOpportunity: (id, data) => api.put(`/api/crm/opportunities/${id}`, data),
  changeOpportunityStage: (id, data) => api.patch(`/api/crm/opportunities/${id}/stage`, data),

  getQuotations: (params) => api.get('/api/crm/quotations', { params }),
  getQuotation: (id) => api.get(`/api/crm/quotations/${id}`),
  createQuotation: (data) => api.post('/api/crm/quotations', data),
  updateQuotation: (id, data) => api.put(`/api/crm/quotations/${id}`, data),
  submitQuotation: (id) => api.post(`/api/crm/quotations/${id}/submit`),
  approveQuotation: (id) => api.post(`/api/crm/quotations/${id}/approve`),
  sendQuotation: (id) => api.post(`/api/crm/quotations/${id}/send`),
  acceptQuotation: (id) => api.post(`/api/crm/quotations/${id}/accept`),
  rejectQuotation: (id) => api.post(`/api/crm/quotations/${id}/reject`),
  cancelQuotation: (id) => api.post(`/api/crm/quotations/${id}/cancel`),

  getOrders: (params) => api.get('/api/crm/orders', { params }),
  getOrder: (id) => api.get(`/api/crm/orders/${id}`),
  createOrder: (data) => api.post('/api/crm/orders', data),
  confirmOrder: (id) => api.post(`/api/crm/orders/${id}/confirm`),
  cancelOrder: (id) => api.post(`/api/crm/orders/${id}/cancel`),
  fulfillOrder: (id, partial = false) => api.post(`/api/crm/orders/${id}/fulfill`, null, { params: { partial } }),
  invoiceOrder: (id) => api.post(`/api/crm/orders/${id}/invoice`),
  recordOrderPayment: (id, data) => api.post(`/api/crm/orders/${id}/payments`, data),
  getOrderPayments: (id) => api.get(`/api/crm/orders/${id}/payments`),
  updateOrderStatus: (id, status) => api.patch(`/api/crm/orders/${id}/status`, null, { params: { status } }),

  getTasks: (params) => api.get('/api/crm/tasks', { params }),
  createTask: (data) => api.post('/api/crm/tasks', data),
  updateTask: (id, data) => api.put(`/api/crm/tasks/${id}`, data),
  getActivities: (params) => api.get('/api/crm/activities', { params }),
  createActivity: (data) => api.post('/api/crm/activities', data),

  getPipeline: () => api.get('/api/crm/pipeline'),
  getDashboard: () => api.get('/api/crm/dashboard'),
  getCommercialReports: () => api.get('/api/crm/reports/commercial'),

  getVisits: (params) => api.get('/api/crm/farm-visits', { params }),
  createVisit: (data) => api.post('/api/crm/farm-visits', data),
  getFollowUps: () => api.get('/api/crm/follow-ups'),
};

export const consultancyApi = {
  getDashboard: () => api.get('/api/consultancy/dashboard'),
  getReports: () => api.get('/api/consultancy/reports'),
  getServices: (params) => api.get('/api/consultancy/services', { params }),
  getService: (id) => api.get(`/api/consultancy/services/${id}`),
  createService: (data) => api.post('/api/consultancy/services', data),
  updateService: (id, data) => api.put(`/api/consultancy/services/${id}`, data),
  getRequests: (params) => api.get('/api/consultancy/requests', { params }),
  getRequest: (id) => api.get(`/api/consultancy/requests/${id}`),
  createRequest: (data) => api.post('/api/consultancy/requests', data),
  updateRequest: (id, data) => api.put(`/api/consultancy/requests/${id}`, data),
  assignRequest: (id, data) => api.post(`/api/consultancy/requests/${id}/assign`, data),
  rejectRequest: (id, data) => api.post(`/api/consultancy/requests/${id}/reject`, data || {}),
  cancelRequest: (id) => api.post(`/api/consultancy/requests/${id}/cancel`),
  convertRequestToProposal: (id, data) => api.post(`/api/consultancy/requests/${id}/convert-proposal`, data || {}),
  convertRequestToEngagement: (id, data) => api.post(`/api/consultancy/requests/${id}/convert-engagement`, data || {}),
  getProposals: (params) => api.get('/api/consultancy/proposals', { params }),
  getProposal: (id) => api.get(`/api/consultancy/proposals/${id}`),
  createProposal: (data) => api.post('/api/consultancy/proposals', data),
  updateProposal: (id, data) => api.put(`/api/consultancy/proposals/${id}`, data),
  sendProposal: (id) => api.post(`/api/consultancy/proposals/${id}/send`),
  acceptProposal: (id) => api.post(`/api/consultancy/proposals/${id}/accept`),
  rejectProposal: (id, data) => api.post(`/api/consultancy/proposals/${id}/reject`, data || {}),
  getEngagements: (params) => api.get('/api/consultancy/engagements', { params }),
  getEngagement: (id) => api.get(`/api/consultancy/engagements/${id}`),
  createEngagement: (data) => api.post('/api/consultancy/engagements', data),
  updateEngagement: (id, data) => api.put(`/api/consultancy/engagements/${id}`, data),
  proposeEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/propose`),
  approveEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/approve`),
  activateEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/activate`),
  holdEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/hold`),
  resumeEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/resume`),
  completeEngagement: (id, data) => api.post(`/api/consultancy/engagements/${id}/complete`, data || {}),
  cancelEngagement: (id) => api.post(`/api/consultancy/engagements/${id}/cancel`),
  assignConsultant: (id, data) => api.post(`/api/consultancy/engagements/${id}/assign`, data),
  createEngagementInvoice: (id) => api.post(`/api/consultancy/engagements/${id}/invoice`),
  recordPayment: (id, data) => api.post(`/api/consultancy/engagements/${id}/payments`, data),
  getActivities: (engagementId) => api.get(`/api/consultancy/engagements/${engagementId}/activities`),
  getActivity: (id) => api.get(`/api/consultancy/activities/${id}`),
  createActivity: (data) => api.post('/api/consultancy/activities', data),
  updateActivity: (id, data) => api.put(`/api/consultancy/activities/${id}`, data),
  startActivity: (id) => api.post(`/api/consultancy/activities/${id}/start`),
  completeActivity: (id, data) => api.post(`/api/consultancy/activities/${id}/complete`, data || {}),
  getDeliverables: (engagementId) => api.get(`/api/consultancy/engagements/${engagementId}/deliverables`),
  getDeliverable: (id) => api.get(`/api/consultancy/deliverables/${id}`),
  createDeliverable: (data) => api.post('/api/consultancy/deliverables', data),
  startDeliverable: (id) => api.post(`/api/consultancy/deliverables/${id}/start`),
  submitDeliverable: (id) => api.post(`/api/consultancy/deliverables/${id}/submit`),
  approveDeliverable: (id) => api.post(`/api/consultancy/deliverables/${id}/approve`),
  rejectDeliverable: (id, data) => api.post(`/api/consultancy/deliverables/${id}/reject`, data || {}),
  billMilestone: (id) => api.post(`/api/consultancy/deliverables/${id}/invoice`),
  getContractByEngagement: (engagementId) => api.get(`/api/consultancy/engagements/${engagementId}/contract`),
  getContract: (id) => api.get(`/api/consultancy/contracts/${id}`),
  createContract: (data) => api.post('/api/consultancy/contracts', data),
  updateContract: (id, data) => api.put(`/api/consultancy/contracts/${id}`, data),
  activateContract: (id) => api.post(`/api/consultancy/contracts/${id}/activate`),
  getClientHistory: (clientId) => api.get(`/api/consultancy/clients/${clientId}/history`),
};

export const hrApi = {
  getDashboard: () => api.get('/api/hr/dashboard'),
  getReports: () => api.get('/api/hr/reports'),

  getDepartments: (params) => api.get('/api/hr/departments', { params }),
  getDepartment: (id) => api.get(`/api/hr/departments/${id}`),
  createDepartment: (data) => api.post('/api/hr/departments', data),
  updateDepartment: (id, data) => api.put(`/api/hr/departments/${id}`, data),

  getPositions: (params) => api.get('/api/hr/positions', { params }),
  getPosition: (id) => api.get(`/api/hr/positions/${id}`),
  createPosition: (data) => api.post('/api/hr/positions', data),
  updatePosition: (id, data) => api.put(`/api/hr/positions/${id}`, data),

  getEmployees: (params) => api.get('/api/hr/employees', { params }),
  getEmployee: (id) => api.get(`/api/hr/employees/${id}`),
  createEmployee: (data) => api.post('/api/hr/employees', data),
  updateEmployee: (id, data) => api.put(`/api/hr/employees/${id}`, data),
  changeEmployeeStatus: (id, data) => api.post(`/api/hr/employees/${id}/status`, data),
  terminateEmployee: (id, data) => api.post(`/api/hr/employees/${id}/terminate`, data),
  getSalaryHistory: (id) => api.get(`/api/hr/employees/${id}/salary`),
  changeSalary: (id, data) => api.post(`/api/hr/employees/${id}/salary`, data),
  getEmployeeLeaveBalances: (id, params) => api.get(`/api/hr/employees/${id}/leave-balances`, { params }),

  getAttendance: (params) => api.get('/api/hr/attendance', { params }),
  getAttendanceRecord: (id) => api.get(`/api/hr/attendance/${id}`),
  recordAttendance: (data) => api.post('/api/hr/attendance', data),
  updateAttendance: (id, data) => api.put(`/api/hr/attendance/${id}`, data),

  getLeaveTypes: (params) => api.get('/api/hr/leave-types', { params }),
  createLeaveType: (data) => api.post('/api/hr/leave-types', data),
  updateLeaveType: (id, data) => api.put(`/api/hr/leave-types/${id}`, data),

  getLeaveRequests: (params) => api.get('/api/hr/leave-requests', { params }),
  getLeaveRequest: (id) => api.get(`/api/hr/leave-requests/${id}`),
  createLeaveRequest: (data) => api.post('/api/hr/leave-requests', data),
  approveLeaveRequest: (id) => api.post(`/api/hr/leave-requests/${id}/approve`),
  rejectLeaveRequest: (id, data) => api.post(`/api/hr/leave-requests/${id}/reject`, data || {}),
  cancelLeaveRequest: (id) => api.post(`/api/hr/leave-requests/${id}/cancel`),

  getDeductionTypes: (params) => api.get('/api/hr/payroll/deduction-types', { params }),
  createDeductionType: (data) => api.post('/api/hr/payroll/deduction-types', data),
  updateDeductionType: (id, data) => api.put(`/api/hr/payroll/deduction-types/${id}`, data),

  getPayrollPeriods: (params) => api.get('/api/hr/payroll/periods', { params }),
  getPayrollPeriod: (id) => api.get(`/api/hr/payroll/periods/${id}`),
  createPayrollPeriod: (data) => api.post('/api/hr/payroll/periods', data),
  calculatePayroll: (id) => api.post(`/api/hr/payroll/periods/${id}/calculate`),
  approvePayroll: (id) => api.post(`/api/hr/payroll/periods/${id}/approve`),
  processPayroll: (id) => api.post(`/api/hr/payroll/periods/${id}/process`),
  closePayroll: (id) => api.post(`/api/hr/payroll/periods/${id}/close`),
  cancelPayrollPeriod: (id) => api.post(`/api/hr/payroll/periods/${id}/cancel`),

  getPayrollRuns: (params) => api.get('/api/hr/payroll/runs', { params }),
  getPayrollRun: (id) => api.get(`/api/hr/payroll/runs/${id}`),
  getPayrollRunItems: (id) => api.get(`/api/hr/payroll/runs/${id}/items`),
  getPayrollRunPayments: (id) => api.get(`/api/hr/payroll/runs/${id}/payments`),
  payPayroll: (id, data) => api.post(`/api/hr/payroll/runs/${id}/payments`, data),
  getPayslip: (id) => api.get(`/api/hr/payroll/payslips/${id}`),

  getMyProfile: () => api.get('/api/hr/me/profile'),
  getMyAttendance: (params) => api.get('/api/hr/me/attendance', { params }),
  getMyLeave: () => api.get('/api/hr/me/leave'),
  submitMyLeave: (data) => api.post('/api/hr/me/leave', data),
  getMyLeaveBalances: (params) => api.get('/api/hr/me/leave-balances', { params }),
  getMyPayslips: () => api.get('/api/hr/me/payslips'),
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
  updateSession: (id, data) => api.put(`/api/training/sessions/${id}`, data),
  getSessionRoster: (id, params) => api.get(`/api/training/sessions/${id}/roster`, { params }),
  getClientHistory: (clientId) => api.get(`/api/training/clients/${clientId}/history`),
  getRegistrations: (params) => api.get('/api/training/registrations', { params }),
  getRegistration: (id) => api.get(`/api/training/registrations/${id}`),
  register: (data) => api.post('/api/training/registrations', data),
  confirmRegistration: (id) => api.post(`/api/training/registrations/${id}/confirm`),
  ensureInvoice: (id) => api.post(`/api/training/registrations/${id}/invoice`),
  cancelRegistration: (id) => api.post(`/api/training/registrations/${id}/cancel`),
  pay: (id, data) => api.post(`/api/training/registrations/${id}/payments`, data),
  recordAttendance: (id, data) => api.post(`/api/training/registrations/${id}/attendance`, data),
  completeRegistration: (id) => api.post(`/api/training/registrations/${id}/complete`),
  issueCertificate: (id, data) => api.post(`/api/training/registrations/${id}/certificate`, data || {}),
  revokeCertificate: (id) => api.post(`/api/training/registrations/${id}/certificate/revoke`),
  getCertificates: () => api.get('/api/training/certificates'),
  verifyCertificate: (code) => api.get(`/api/public/certificates/verify/${code}`),
  verifyCertificateAuthed: (code) => api.get(`/api/training/certificates/${code}/verify`),
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

export const executiveApi = {
  getDashboard: (params) => api.get('/api/executive/dashboard', { params }),
  getFinancialSummary: (params) => api.get('/api/executive/financial-summary', { params }),
  getRevenue: (params) => api.get('/api/executive/revenue', { params }),
  getExpenses: (params) => api.get('/api/executive/expenses', { params }),
  getCashFlow: (params) => api.get('/api/executive/cash-flow', { params }),
  getReceivables: (params) => api.get('/api/executive/receivables', { params }),
  getPayables: (params) => api.get('/api/executive/payables', { params }),
  getSales: (params) => api.get('/api/executive/sales', { params }),
  getFarm: (params) => api.get('/api/executive/farm', { params }),
  getInventory: () => api.get('/api/executive/inventory'),
  getProcurement: () => api.get('/api/executive/procurement'),
  getServices: () => api.get('/api/executive/services'),
  getWorkforce: () => api.get('/api/executive/workforce'),
  getAlerts: (params) => api.get('/api/executive/alerts', { params }),
  getDefinitions: () => api.get('/api/executive/definitions'),
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
  getById: (id) => api.get(`/api/audit-logs/${id}`),
  securityEvents: (params) => api.get('/api/audit-logs/security-events', { params }),
};

export const governanceApi = {
  dashboard: () => api.get('/api/governance/dashboard'),
  controls: () => api.get('/api/governance/controls'),
};

export const feedApi = {
  getRecipes: () => api.get('/api/feed/recipes'),
  createRecipe: (data) => api.post('/api/feed/recipes', data),
  getSilos: () => api.get('/api/feed/silos'),
  logDelivery: (data) => api.post('/api/feed/deliveries', data),
};
