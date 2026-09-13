import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastContainer } from 'react-toastify';
import 'react-toastify/dist/ReactToastify.css';

import { AuthProvider } from './context/AuthContext';
import { LanguageProvider } from './context/LanguageContext';
import { MasterDataProvider } from './context/MasterDataContext';
import ProtectedRoute from './components/common/ProtectedRoute';
import MainLayout from './layouts/MainLayout';
import { PERMISSIONS } from './lib/permissions';

import Login from './pages/auth/Login';
import Dashboard from './pages/dashboard/Dashboard';
import UserList from './pages/users/UserList';
import UserForm from './pages/users/UserForm';
import FarmList from './pages/farm/FarmList';
import FarmForm from './pages/farm/FarmForm';
import FlockList from './pages/farm/FlockList';
import FlockForm from './pages/farm/FlockForm';
import DailyFarmRecordList from './pages/farm/DailyFarmRecordList';
import DailyFarmRecordForm from './pages/farm/DailyFarmRecordForm';
import CrmPage from './pages/crm/CrmPage';
import CrmLeads from './pages/crm/CrmLeads';
import CrmPipeline from './pages/crm/CrmPipeline';
import CrmClientForm from './pages/crm/CrmClientForm';
import FarmVisitForm from './pages/crm/FarmVisitForm';
import MarketingPage from './pages/marketing/MarketingPage';
import BarnDashboard from './pages/farm/BarnDashboard';
import ChickenSales from './pages/farm/ChickenSales';
import SiloDashboard from './pages/feed/SiloDashboard';
import FeedRecipeForm from './pages/feed/FeedRecipeForm';
import OrderDashboard from './pages/crm/OrderDashboard';
import InventoryItemList from './pages/inventory/InventoryItemList';
import InventoryItemForm from './pages/inventory/InventoryItemForm';
import StockInForm from './pages/inventory/StockInForm';
import StockOutForm from './pages/inventory/StockOutForm';
import ExpiryTracking from './pages/inventory/ExpiryTracking';
import SupplierPage from './pages/inventory/SupplierPage';
import InventoryOverview from './pages/inventory/InventoryOverview';
import ScanStation from './pages/inventory/ScanStation';
import StockIn from './pages/inventory/StockIn';
import StockOut from './pages/inventory/StockOut';
import PurchaseOrders from './pages/inventory/PurchaseOrders';
import CycleCount from './pages/inventory/CycleCount';
import Forecasting from './pages/inventory/Forecasting';
import InventoryReports from './pages/inventory/InventoryReports';
import InventoryDashboard from './pages/inventory/InventoryDashboard';
import RequisitionCenter from './pages/inventory/RequisitionCenter';
import VeterinaryPage from './pages/veterinary/VeterinaryPage';
import VaccinationForm from './pages/veterinary/VaccinationForm';
import HealthIssueReportForm from './pages/veterinary/HealthIssueReportForm';
import DiseaseCaseForm from './pages/veterinary/DiseaseCaseForm';
import TreatmentForm from './pages/veterinary/TreatmentForm';
import PrescriptionForm from './pages/veterinary/PrescriptionForm';
import DrugUsageReport from './pages/veterinary/DrugUsageReport';
import BiosecurityForm from './pages/veterinary/BiosecurityForm';
import VeterinaryDashboard from './pages/veterinary/VeterinaryDashboard';
import VaccinationCalendar from './pages/veterinary/VaccinationCalendar';
import FlockEMR from './pages/veterinary/FlockEMR';
import NecropsyTerminal from './pages/veterinary/NecropsyTerminal';
import PharmacyOverview from './pages/pharmacy/Overview';
import ProcessPrescription from './pages/pharmacy/ProcessPrescription';
import POSTerminal from './pages/pharmacy/POSTerminal';
import PharmacyCustomers from './pages/pharmacy/Customers';
import PharmacyReports from './pages/pharmacy/Reports';
import FinanceDashboard from './pages/finance/FinanceDashboard';
import ChartOfAccounts from './pages/finance/ChartOfAccounts';
import InvoiceCenter from './pages/finance/InvoiceCenter';
import WalletManagement from './pages/finance/WalletManagement';
import CashReconciliation from './pages/finance/CashReconciliation';
import ReportCenter from './pages/finance/ReportCenter';
import FlockProfitability from './pages/finance/FlockProfitability';
import TransactionForm from './pages/finance/TransactionForm';
import Reports from './pages/reports/Reports';
import Settings from './pages/settings/Settings';
import TrainingPage from './pages/training/TrainingPage';

const ModuleRoute = ({ module, children }) => (
  <ProtectedRoute module={module}>{children}</ProtectedRoute>
);

const PermissionRoute = ({ permission, children }) => (
  <ProtectedRoute permission={permission}>{children}</ProtectedRoute>
);

const ActionRoute = ({ action, resource, children }) => (
  <ProtectedRoute action={action} resource={resource}>{children}</ProtectedRoute>
);

function App() {
  return (
    <LanguageProvider>
      <AuthProvider>
        <MasterDataProvider>
          <BrowserRouter>
            <Routes>
              <Route path="/login" element={<Login />} />
              <Route path="/" element={<ProtectedRoute><MainLayout /></ProtectedRoute>}>
                <Route index element={<Navigate to="/dashboard" replace />} />
                <Route path="dashboard" element={<Dashboard />} />

                <Route path="users" element={<ModuleRoute module="users"><UserList /></ModuleRoute>} />
                <Route path="users/new" element={<PermissionRoute permission={PERMISSIONS.CREATE_USERS}><UserForm /></PermissionRoute>} />
                <Route path="users/:id/edit" element={<PermissionRoute permission={PERMISSIONS.EDIT_USERS}><UserForm /></PermissionRoute>} />

                <Route path="farms" element={<ModuleRoute module="farms"><FarmList /></ModuleRoute>} />
                <Route path="farms/new" element={<ActionRoute action="create" resource="farms"><FarmForm /></ActionRoute>} />
                <Route path="farms/:id/edit" element={<ActionRoute action="edit" resource="farms"><FarmForm /></ActionRoute>} />
                <Route path="farms/barn-dashboard" element={<PermissionRoute permission={PERMISSIONS.VIEW_FARMS}><BarnDashboard /></PermissionRoute>} />

                <Route path="flocks" element={<ModuleRoute module="flocks"><FlockList /></ModuleRoute>} />
                <Route path="flocks/new" element={<ActionRoute action="create" resource="flocks"><FlockForm /></ActionRoute>} />
                <Route path="flocks/:id/edit" element={<ActionRoute action="edit" resource="flocks"><FlockForm /></ActionRoute>} />

                <Route path="farm/chicken-sales" element={<ModuleRoute module="chickenSales"><ChickenSales /></ModuleRoute>} />

                <Route path="feed/silos" element={<ModuleRoute module="farms"><SiloDashboard /></ModuleRoute>} />
                <Route path="feed/recipes" element={<ModuleRoute module="farms"><FeedRecipeForm /></ModuleRoute>} />

                <Route path="crm/orders" element={<ModuleRoute module="crm"><OrderDashboard /></ModuleRoute>} />

                <Route path="daily-records" element={<ModuleRoute module="dailyRecords"><DailyFarmRecordList /></ModuleRoute>} />
                <Route path="daily-records/new" element={<ActionRoute action="create" resource="dailyRecords"><DailyFarmRecordForm /></ActionRoute>} />
                <Route path="daily-records/:id/edit" element={<ActionRoute action="edit" resource="dailyRecords"><DailyFarmRecordForm /></ActionRoute>} />

                <Route path="inventory" element={<ModuleRoute module="inventory"><InventoryDashboard /></ModuleRoute>} />
                <Route path="inventory/overview" element={<ModuleRoute module="inventory"><InventoryDashboard /></ModuleRoute>} />
                <Route path="inventory/dashboard" element={<ModuleRoute module="inventory"><InventoryDashboard /></ModuleRoute>} />
                <Route path="inventory/scan" element={<ActionRoute action="stockIn" resource="inventoryItems"><ScanStation /></ActionRoute>} />
                <Route path="inventory/stock-in" element={<ActionRoute action="stockIn" resource="inventoryItems"><StockInForm /></ActionRoute>} />
                <Route path="inventory/stock-out" element={<ActionRoute action="stockOut" resource="inventoryItems"><StockOutForm /></ActionRoute>} />
                <Route path="inventory/po" element={<ModuleRoute module="inventory"><PurchaseOrders /></ModuleRoute>} />
                <Route path="inventory/count" element={<ActionRoute action="stockIn" resource="inventoryItems"><CycleCount /></ActionRoute>} />
                <Route path="inventory/forecast" element={<ModuleRoute module="inventory"><Forecasting /></ModuleRoute>} />
                <Route path="inventory/suppliers" element={<ModuleRoute module="inventory"><SupplierPage /></ModuleRoute>} />
                <Route path="inventory/reports" element={<PermissionRoute permission={PERMISSIONS.VIEW_INVENTORY_REPORTS}><InventoryReports /></PermissionRoute>} />
                <Route path="inventory/requisitions" element={<ModuleRoute module="inventory"><RequisitionCenter /></ModuleRoute>} />
                <Route path="inventory/items" element={<ModuleRoute module="inventory"><InventoryItemList /></ModuleRoute>} />
                <Route path="inventory/items/new" element={<ActionRoute action="create" resource="inventoryItems"><InventoryItemForm /></ActionRoute>} />
                <Route path="inventory/items/:id/edit" element={<ActionRoute action="edit" resource="inventoryItems"><InventoryItemForm /></ActionRoute>} />
                <Route path="inventory/expiry" element={<PermissionRoute permission={PERMISSIONS.VIEW_EXPIRY_MONITORING}><ExpiryTracking /></PermissionRoute>} />

                <Route path="veterinary" element={<ModuleRoute module="veterinary"><VeterinaryPage /></ModuleRoute>} />
                <Route path="veterinary/vaccinations/new" element={<PermissionRoute permission={PERMISSIONS.MANAGE_VACCINATIONS}><VaccinationForm /></PermissionRoute>} />
                <Route path="veterinary/health-reports/new" element={<PermissionRoute permission={PERMISSIONS.CREATE_DISEASE_CASES}><HealthIssueReportForm /></PermissionRoute>} />
                <Route path="veterinary/disease-cases/new" element={<ActionRoute action="create" resource="diseaseCases"><DiseaseCaseForm /></ActionRoute>} />
                <Route path="veterinary/treatments/new" element={<ActionRoute action="create" resource="treatments"><TreatmentForm /></ActionRoute>} />
                <Route path="veterinary/prescriptions/new" element={<ActionRoute action="create" resource="prescriptions"><PrescriptionForm /></ActionRoute>} />
                <Route path="veterinary/reports" element={<ModuleRoute module="veterinary"><DrugUsageReport /></ModuleRoute>} />
                <Route path="veterinary/biosecurity/new" element={<PermissionRoute permission={PERMISSIONS.CREATE_DISEASE_CASES}><BiosecurityForm /></PermissionRoute>} />

                {/* Poultry Health EMR Routes */}
                <Route path="vet/flocks" element={<ModuleRoute module="veterinary"><FlockList /></ModuleRoute>} />
                <Route path="vet" element={<ModuleRoute module="veterinary"><VeterinaryDashboard /></ModuleRoute>} />
                <Route path="vet/vaccinations" element={<ModuleRoute module="veterinary"><VaccinationCalendar /></ModuleRoute>} />
                <Route path="vet/flock-emr/:id" element={<ModuleRoute module="veterinary"><FlockEMR /></ModuleRoute>} />
                <Route path="vet/necropsy" element={<ModuleRoute module="veterinary"><NecropsyTerminal /></ModuleRoute>} />

                <Route path="pharmacy" element={<ModuleRoute module="pharmacy"><PharmacyOverview /></ModuleRoute>} />
                <Route path="pharmacy/overview" element={<ModuleRoute module="pharmacy"><PharmacyOverview /></ModuleRoute>} />
                <Route path="pharmacy/process-rx" element={<ActionRoute action="create" resource="pharmacySales"><ProcessPrescription /></ActionRoute>} />
                <Route path="pharmacy/pos" element={<ActionRoute action="create" resource="pharmacySales"><POSTerminal /></ActionRoute>} />
                <Route path="pharmacy/customers" element={<ModuleRoute module="pharmacy"><PharmacyCustomers /></ModuleRoute>} />
                <Route path="pharmacy/reports" element={<ModuleRoute module="pharmacy"><PharmacyReports /></ModuleRoute>} />

                <Route path="finance" element={<ModuleRoute module="finance"><FinanceDashboard /></ModuleRoute>} />
                <Route path="finance/dashboard" element={<ModuleRoute module="finance"><FinanceDashboard /></ModuleRoute>} />
                <Route path="finance/coa" element={<ModuleRoute module="finance"><ChartOfAccounts /></ModuleRoute>} />
                <Route path="finance/invoices" element={<ModuleRoute module="finance"><InvoiceCenter /></ModuleRoute>} />
                <Route path="finance/wallets" element={<ModuleRoute module="finance"><WalletManagement /></ModuleRoute>} />
                <Route path="finance/reconciliation" element={<ModuleRoute module="finance"><CashReconciliation /></ModuleRoute>} />
                <Route path="finance/reports" element={<ModuleRoute module="finance"><ReportCenter /></ModuleRoute>} />
                <Route path="finance/flock-cogs" element={<ModuleRoute module="finance"><FlockProfitability /></ModuleRoute>} />
                <Route path="finance/new" element={<ProtectedRoute permissions={[PERMISSIONS.CREATE_INCOME, PERMISSIONS.CREATE_EXPENSES]}><TransactionForm /></ProtectedRoute>} />

                <Route path="crm" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/leads" element={<ModuleRoute module="crm"><CrmLeads /></ModuleRoute>} />
                <Route path="crm/clients" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/pipeline" element={<ModuleRoute module="crm"><CrmPipeline /></ModuleRoute>} />
                <Route path="crm/follow-ups" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/tasks" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/reports" element={<ModuleRoute module="crm"><OrderDashboard /></ModuleRoute>} />
                <Route path="crm/clients/new" element={<ActionRoute action="create" resource="customers"><CrmClientForm /></ActionRoute>} />
                <Route path="crm/clients/:id/edit" element={<PermissionRoute permission={PERMISSIONS.MANAGE_CLIENTS}><CrmClientForm /></PermissionRoute>} />
                <Route path="crm/visits/new" element={<ActionRoute action="create" resource="farmVisits"><FarmVisitForm /></ActionRoute>} />

                <Route path="marketing" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/campaigns" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/leads" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/email" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/events" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/segments" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/reports" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />

                <Route path="consultancy" element={<ModuleRoute module="consultancy"><CrmPage /></ModuleRoute>} />

                <Route path="training" element={<ModuleRoute module="training"><TrainingPage /></ModuleRoute>} />
                <Route path="training/sessions" element={<ModuleRoute module="training"><TrainingPage /></ModuleRoute>} />
                <Route path="training/students" element={<ModuleRoute module="training"><TrainingPage /></ModuleRoute>} />
                <Route path="training/certificates" element={<ModuleRoute module="training"><TrainingPage /></ModuleRoute>} />
                <Route path="reports" element={<ModuleRoute module="reports"><Reports /></ModuleRoute>} />
                <Route path="settings" element={<PermissionRoute permission={PERMISSIONS.VIEW_SETTINGS}><Settings /></PermissionRoute>} />
              </Route>
              <Route path="*" element={<Navigate to="/dashboard" replace />} />
            </Routes>
          </BrowserRouter>
          <ToastContainer position="top-right" autoClose={3000} hideProgressBar={false} />
        </MasterDataProvider>
      </AuthProvider>
    </LanguageProvider>
  );
}

export default App;
