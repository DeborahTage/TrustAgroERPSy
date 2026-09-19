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
import CRMDashboard from './pages/crm/CRMDashboard';
import CrmLeads from './pages/crm/CrmLeads';
import CrmLeadDetail from './pages/crm/CrmLeadDetail';
import CrmOpportunities from './pages/crm/CrmOpportunities';
import CrmOpportunityDetail from './pages/crm/CrmOpportunityDetail';
import CrmQuotations from './pages/crm/CrmQuotations';
import CrmQuotationDetail from './pages/crm/CrmQuotationDetail';
import CrmOrders from './pages/crm/CrmOrders';
import CrmOrderDetail from './pages/crm/CrmOrderDetail';
import CrmCustomer360 from './pages/crm/CrmCustomer360';
import CrmTasks from './pages/crm/CrmTasks';
import CrmReports from './pages/crm/CrmReports';
import CrmPipeline from './pages/crm/CrmPipeline';
import CrmClientForm from './pages/crm/CrmClientForm';
import FarmVisitForm from './pages/crm/FarmVisitForm';
import MarketingPage from './pages/marketing/MarketingPage';
import BarnDashboard from './pages/farm/BarnDashboard';
import ChickenSales from './pages/farm/ChickenSales';
import ChickenSaleForm from './pages/farm/ChickenSaleForm';
import ChickenSaleDetail from './pages/farm/ChickenSaleDetail';
import { PaymentVerificationQueue, FarmReleaseApprovalQueue, MarketingDeliveryQueue } from './pages/farm/ChickenReleaseQueues';
import InternalDeliveryPage from './pages/farm/InternalDeliveryPage';
import DataOnboardingPage from './pages/admin/DataOnboardingPage';
import SiloDashboard from './pages/feed/SiloDashboard';
import FeedRecipeForm from './pages/feed/FeedRecipeForm';
import InventoryItemList from './pages/inventory/InventoryItemList';
import InventoryItemForm from './pages/inventory/InventoryItemForm';
import StockInForm from './pages/inventory/StockInForm';
import StockOutForm from './pages/inventory/StockOutForm';
import ExpiryTracking from './pages/inventory/ExpiryTracking';
import SupplierPage from './pages/inventory/SupplierPage';
import ScanStation from './pages/inventory/ScanStation';
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
import GovernanceDashboard from './pages/governance/GovernanceDashboard';
import TrainingDashboard from './pages/training/TrainingDashboard';
import TrainingPrograms from './pages/training/TrainingPrograms';
import TrainingProgramDetail from './pages/training/TrainingProgramDetail';
import TrainingSessions from './pages/training/TrainingSessions';
import TrainingSessionDetail from './pages/training/TrainingSessionDetail';
import TrainingRegistrations from './pages/training/TrainingRegistrations';
import TrainingRegistrationDetail from './pages/training/TrainingRegistrationDetail';
import TrainingCertificates from './pages/training/TrainingCertificates';
import ConsultancyDashboard from './pages/consultancy/ConsultancyDashboard';
import ConsultancyServices from './pages/consultancy/ConsultancyServices';
import ConsultancyRequests from './pages/consultancy/ConsultancyRequests';
import ConsultancyProposals from './pages/consultancy/ConsultancyProposals';
import ConsultancyEngagements from './pages/consultancy/ConsultancyEngagements';
import ConsultancyEngagementDetail from './pages/consultancy/ConsultancyEngagementDetail';
import ConsultancyActivities from './pages/consultancy/ConsultancyActivities';
import ConsultancyContracts from './pages/consultancy/ConsultancyContracts';
import ConsultancyReports from './pages/consultancy/ConsultancyReports';
import HrDashboard from './pages/hr/HrDashboard';
import HrEmployees from './pages/hr/HrEmployees';
import HrEmployeeDetail from './pages/hr/HrEmployeeDetail';
import HrDepartments from './pages/hr/HrDepartments';
import HrPositions from './pages/hr/HrPositions';
import HrAttendance from './pages/hr/HrAttendance';
import HrLeaveRequests from './pages/hr/HrLeaveRequests';
import HrLeaveTypes from './pages/hr/HrLeaveTypes';
import HrPayroll from './pages/hr/HrPayroll';
import HrPayrollRunDetail from './pages/hr/HrPayrollRunDetail';
import HrPayslips from './pages/hr/HrPayslips';
import HrReports from './pages/hr/HrReports';
import HrMySelfService from './pages/hr/HrMySelfService';
import ProcurementDashboard from './pages/procurement/ProcurementDashboard';
import ProcurementSuppliers from './pages/procurement/ProcurementSuppliers';
import ProcurementSupplierDetail from './pages/procurement/ProcurementSupplierDetail';
import ProcurementRequisitions from './pages/procurement/ProcurementRequisitions';
import ProcurementRequisitionDetail from './pages/procurement/ProcurementRequisitionDetail';
import ProcurementPurchaseOrders from './pages/procurement/ProcurementPurchaseOrders';
import ProcurementPurchaseOrderDetail from './pages/procurement/ProcurementPurchaseOrderDetail';
import ProcurementGoodsReceipts from './pages/procurement/ProcurementGoodsReceipts';
import ProcurementSupplierInvoices from './pages/procurement/ProcurementSupplierInvoices';
import ProcurementAccountsPayable from './pages/procurement/ProcurementAccountsPayable';
import ProcurementReports from './pages/procurement/ProcurementReports';

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
                <Route path="executive" element={<Dashboard />} />

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
                <Route path="farm/chicken-sales/new" element={<ActionRoute action="create" resource="chickenSales"><ChickenSaleForm /></ActionRoute>} />
                <Route path="farm/chicken-sales/:id/edit" element={<ActionRoute action="edit" resource="chickenSales"><ChickenSaleForm /></ActionRoute>} />
                <Route path="farm/chicken-sales/:id" element={<ModuleRoute module="chickenSales"><ChickenSaleDetail /></ModuleRoute>} />
                <Route path="farm/payment-verification" element={<PermissionRoute permission={PERMISSIONS.VERIFY_CHICKEN_SALES}><PaymentVerificationQueue /></PermissionRoute>} />
                <Route path="farm/release-approvals" element={<PermissionRoute permission={PERMISSIONS.EDIT_FARMS}><FarmReleaseApprovalQueue /></PermissionRoute>} />
                <Route path="farm/internal-deliveries" element={<PermissionRoute permission={PERMISSIONS.VIEW_FARMS}><InternalDeliveryPage /></PermissionRoute>} />
                <Route path="marketing/chicken-deliveries" element={<ModuleRoute module="chickenSales"><MarketingDeliveryQueue /></ModuleRoute>} />
                <Route path="admin/data-onboarding" element={<PermissionRoute permission={PERMISSIONS.CREATE_FARMS}><DataOnboardingPage /></PermissionRoute>} />

                <Route path="feed/silos" element={<ModuleRoute module="farms"><SiloDashboard /></ModuleRoute>} />
                <Route path="feed/recipes" element={<ModuleRoute module="farms"><FeedRecipeForm /></ModuleRoute>} />

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

                <Route path="crm" element={<ModuleRoute module="crm"><CRMDashboard /></ModuleRoute>} />
                <Route path="crm/leads" element={<ModuleRoute module="crm"><CrmLeads /></ModuleRoute>} />
                <Route path="crm/leads/:id" element={<ModuleRoute module="crm"><CrmLeadDetail /></ModuleRoute>} />
                <Route path="crm/opportunities" element={<ModuleRoute module="crm"><CrmOpportunities /></ModuleRoute>} />
                <Route path="crm/opportunities/:id" element={<ModuleRoute module="crm"><CrmOpportunityDetail /></ModuleRoute>} />
                <Route path="crm/quotations" element={<ModuleRoute module="crm"><CrmQuotations /></ModuleRoute>} />
                <Route path="crm/quotations/:id" element={<ModuleRoute module="crm"><CrmQuotationDetail /></ModuleRoute>} />
                <Route path="crm/orders" element={<ModuleRoute module="crm"><CrmOrders /></ModuleRoute>} />
                <Route path="crm/orders/:id" element={<ModuleRoute module="crm"><CrmOrderDetail /></ModuleRoute>} />
                <Route path="crm/clients" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/clients/new" element={<PermissionRoute permission={PERMISSIONS.MANAGE_CRM}><CrmClientForm /></PermissionRoute>} />
                <Route path="crm/clients/:id/edit" element={<PermissionRoute permission={PERMISSIONS.EDIT_CRM}><CrmClientForm /></PermissionRoute>} />
                <Route path="crm/clients/:id" element={<ModuleRoute module="crm"><CrmCustomer360 /></ModuleRoute>} />
                <Route path="crm/pipeline" element={<ModuleRoute module="crm"><CrmPipeline /></ModuleRoute>} />
                <Route path="crm/follow-ups" element={<ModuleRoute module="crm"><CrmPage /></ModuleRoute>} />
                <Route path="crm/tasks" element={<ModuleRoute module="crm"><CrmTasks /></ModuleRoute>} />
                <Route path="crm/activities" element={<ModuleRoute module="crm"><CrmTasks /></ModuleRoute>} />
                <Route path="crm/reports" element={<PermissionRoute permission={PERMISSIONS.CRM_REPORT}><CrmReports /></PermissionRoute>} />
                <Route path="crm/visits/new" element={<ActionRoute action="create" resource="farmVisits"><FarmVisitForm /></ActionRoute>} />

                <Route path="marketing" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/campaigns" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/leads" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/email" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/events" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/segments" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />
                <Route path="marketing/reports" element={<ModuleRoute module="marketing"><MarketingPage /></ModuleRoute>} />

                <Route path="consultancy" element={<ModuleRoute module="consultancy"><ConsultancyDashboard /></ModuleRoute>} />
                <Route path="consultancy/services" element={<ModuleRoute module="consultancy"><ConsultancyServices /></ModuleRoute>} />
                <Route path="consultancy/requests" element={<ModuleRoute module="consultancy"><ConsultancyRequests /></ModuleRoute>} />
                <Route path="consultancy/proposals" element={<ModuleRoute module="consultancy"><ConsultancyProposals /></ModuleRoute>} />
                <Route path="consultancy/engagements" element={<ModuleRoute module="consultancy"><ConsultancyEngagements /></ModuleRoute>} />
                <Route path="consultancy/engagements/:id" element={<ModuleRoute module="consultancy"><ConsultancyEngagementDetail /></ModuleRoute>} />
                <Route path="consultancy/activities" element={<ModuleRoute module="consultancy"><ConsultancyActivities /></ModuleRoute>} />
                <Route path="consultancy/contracts" element={<ModuleRoute module="consultancy"><ConsultancyContracts /></ModuleRoute>} />
                <Route path="consultancy/reports" element={<ModuleRoute module="consultancy"><ConsultancyReports /></ModuleRoute>} />

                <Route path="training" element={<ModuleRoute module="training"><TrainingDashboard /></ModuleRoute>} />
                <Route path="training/programs" element={<ModuleRoute module="training"><TrainingPrograms /></ModuleRoute>} />
                <Route path="training/programs/:id" element={<ModuleRoute module="training"><TrainingProgramDetail /></ModuleRoute>} />
                <Route path="training/sessions" element={<ModuleRoute module="training"><TrainingSessions /></ModuleRoute>} />
                <Route path="training/sessions/:id" element={<ModuleRoute module="training"><TrainingSessionDetail /></ModuleRoute>} />
                <Route path="training/students" element={<ModuleRoute module="training"><TrainingRegistrations /></ModuleRoute>} />
                <Route path="training/registrations" element={<ModuleRoute module="training"><TrainingRegistrations /></ModuleRoute>} />
                <Route path="training/registrations/:id" element={<ModuleRoute module="training"><TrainingRegistrationDetail /></ModuleRoute>} />
                <Route path="training/certificates" element={<ModuleRoute module="training"><TrainingCertificates /></ModuleRoute>} />
                <Route path="hr" element={<ModuleRoute module="hr"><HrDashboard /></ModuleRoute>} />
                <Route path="hr/employees" element={<ModuleRoute module="hr"><HrEmployees /></ModuleRoute>} />
                <Route path="hr/employees/:id" element={<ModuleRoute module="hr"><HrEmployeeDetail /></ModuleRoute>} />
                <Route path="hr/departments" element={<ModuleRoute module="hr"><HrDepartments /></ModuleRoute>} />
                <Route path="hr/positions" element={<ModuleRoute module="hr"><HrPositions /></ModuleRoute>} />
                <Route path="hr/attendance" element={<ModuleRoute module="hr"><HrAttendance /></ModuleRoute>} />
                <Route path="hr/leave" element={<ModuleRoute module="hr"><HrLeaveRequests /></ModuleRoute>} />
                <Route path="hr/leave-types" element={<ModuleRoute module="hr"><HrLeaveTypes /></ModuleRoute>} />
                <Route path="hr/payroll" element={<ModuleRoute module="hr"><HrPayroll /></ModuleRoute>} />
                <Route path="hr/payroll/runs/:id" element={<ModuleRoute module="hr"><HrPayrollRunDetail /></ModuleRoute>} />
                <Route path="hr/payslips" element={<ModuleRoute module="hr"><HrPayslips /></ModuleRoute>} />
                <Route path="hr/reports" element={<ModuleRoute module="hr"><HrReports /></ModuleRoute>} />
                <Route path="hr/me" element={<PermissionRoute permission={PERMISSIONS.HR_SELF}><HrMySelfService /></PermissionRoute>} />

                <Route path="procurement" element={<ModuleRoute module="procurement"><ProcurementDashboard /></ModuleRoute>} />
                <Route path="procurement/suppliers" element={<ModuleRoute module="procurement"><ProcurementSuppliers /></ModuleRoute>} />
                <Route path="procurement/suppliers/:id" element={<ModuleRoute module="procurement"><ProcurementSupplierDetail /></ModuleRoute>} />
                <Route path="procurement/requisitions" element={<ModuleRoute module="procurement"><ProcurementRequisitions /></ModuleRoute>} />
                <Route path="procurement/requisitions/:id" element={<ModuleRoute module="procurement"><ProcurementRequisitionDetail /></ModuleRoute>} />
                <Route path="procurement/purchase-orders" element={<ModuleRoute module="procurement"><ProcurementPurchaseOrders /></ModuleRoute>} />
                <Route path="procurement/purchase-orders/:id" element={<ModuleRoute module="procurement"><ProcurementPurchaseOrderDetail /></ModuleRoute>} />
                <Route path="procurement/goods-receipts" element={<ModuleRoute module="procurement"><ProcurementGoodsReceipts /></ModuleRoute>} />
                <Route path="procurement/supplier-invoices" element={<ModuleRoute module="procurement"><ProcurementSupplierInvoices /></ModuleRoute>} />
                <Route path="procurement/ap" element={<ModuleRoute module="procurement"><ProcurementAccountsPayable /></ModuleRoute>} />
                <Route path="procurement/reports" element={<ModuleRoute module="procurement"><ProcurementReports /></ModuleRoute>} />

                <Route path="reports" element={<ModuleRoute module="reports"><Reports /></ModuleRoute>} />
                <Route path="governance" element={<ModuleRoute module="governance"><GovernanceDashboard /></ModuleRoute>} />
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
