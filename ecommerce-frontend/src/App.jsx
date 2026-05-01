import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './components/Layout.jsx';
import RequireAuth from './components/RequireAuth.jsx';
import Home from './pages/Home.jsx';
import Register from './pages/Register.jsx';
import Login from './pages/Login.jsx';
import Products from './pages/Products.jsx';
import ManageProducts from './pages/ManageProducts.jsx';
import AddProducts from './pages/AddProducts.jsx';
import Cart from './pages/Cart.jsx';
import Checkout from './pages/Checkout.jsx';
import Payments from './pages/Payments.jsx';
import KitchenDashboard from './pages/dashboards/KitchenDashboard.jsx';
import KitchenOnlineOrders from './pages/dashboards/KitchenOnlineOrders.jsx';
import KitchenInStoreOrders from './pages/dashboards/KitchenInStoreOrders.jsx';
import KitchenSupplies from './pages/dashboards/KitchenSupplies.jsx';
import KitchenInStock from './pages/dashboards/KitchenInStock.jsx';
import BakeryDashboard from './pages/dashboards/BakeryDashboard.jsx';
import CounterPOS from './pages/dashboards/CounterPOS.jsx';
import SalesDashboard from './pages/dashboards/SalesDashboard.jsx';
import SalesTasks from './pages/dashboards/SalesTasks.jsx';
import DeliveryDashboard from './pages/dashboards/DeliveryDashboard.jsx';
import DeliveryTrips from './pages/dashboards/DeliveryTrips.jsx';
import DeliveryIssues from './pages/dashboards/DeliveryIssues.jsx';
import DeliveryShift from './pages/dashboards/DeliveryShift.jsx';
import ManagementDashboard from './pages/dashboards/ManagementDashboard.jsx';
import ManagementLiveOps from './pages/dashboards/ManagementLiveOps.jsx';
import ManagementOrdersAudit from './pages/dashboards/ManagementOrdersAudit.jsx';
import ManagementDeliveriesAudit from './pages/dashboards/ManagementDeliveriesAudit.jsx';
import ManagementDayPnl from './pages/dashboards/ManagementDayPnl.jsx';
import ManagementStaffPerformance from './pages/dashboards/ManagementStaffPerformance.jsx';
import ManagementRefunds from './pages/dashboards/ManagementRefunds.jsx';
import ManagementApprovals from './pages/dashboards/ManagementApprovals.jsx';
import ManagementDiscounts from './pages/dashboards/ManagementDiscounts.jsx';
import ManagementCashReconciliation from './pages/dashboards/ManagementCashReconciliation.jsx';

export default function App() {
  return (
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/register" element={<Register />} />
        <Route path="/login" element={<Login />} />
        <Route
          path="/products"
          element={
            <RequireAuth>
              <Products />
            </RequireAuth>
          }
        />
        <Route
          path="/cart"
          element={
            <RequireAuth>
              <Cart />
            </RequireAuth>
          }
        />
        <Route
          path="/checkout"
          element={
            <RequireAuth>
              <Checkout />
            </RequireAuth>
          }
        />
        <Route
          path="/payments"
          element={
            <RequireAuth>
              <Payments />
            </RequireAuth>
          }
        />
        <Route
          path="/manage-products"
          element={
            <RequireAuth role="employee">
              <ManageProducts />
            </RequireAuth>
          }
        />
        <Route
          path="/add-products"
          element={
            <RequireAuth role="employee">
              <AddProducts />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/kitchen"
          element={
            <RequireAuth role="employee">
              <KitchenDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/kitchen/online-orders"
          element={
            <RequireAuth role="employee">
              <KitchenOnlineOrders />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/kitchen/instore-orders"
          element={
            <RequireAuth role="employee">
              <KitchenInStoreOrders />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/kitchen/supplies"
          element={
            <RequireAuth role="employee">
              <KitchenSupplies />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/kitchen/in-stock"
          element={
            <RequireAuth role="employee">
              <KitchenInStock />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/bakery"
          element={
            <RequireAuth role="employee">
              <BakeryDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/bakery/pos"
          element={
            <RequireAuth role="employee">
              <CounterPOS />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/sales"
          element={
            <RequireAuth role="employee">
              <SalesDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/sales/tasks"
          element={
            <RequireAuth role="employee">
              <SalesTasks />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/delivery"
          element={
            <RequireAuth role="employee">
              <DeliveryDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/delivery/trips"
          element={
            <RequireAuth role="employee">
              <DeliveryTrips />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/delivery/issues"
          element={
            <RequireAuth role="employee">
              <DeliveryIssues />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/delivery/shift"
          element={
            <RequireAuth role="employee">
              <DeliveryShift />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management"
          element={
            <RequireAuth role="employee">
              <ManagementDashboard />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/ops"
          element={
            <RequireAuth role="employee">
              <ManagementLiveOps />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/orders-audit"
          element={
            <RequireAuth role="employee">
              <ManagementOrdersAudit />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/deliveries-audit"
          element={
            <RequireAuth role="employee">
              <ManagementDeliveriesAudit />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/day-pnl"
          element={
            <RequireAuth role="employee">
              <ManagementDayPnl />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/staff-performance"
          element={
            <RequireAuth role="employee">
              <ManagementStaffPerformance />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/refunds"
          element={
            <RequireAuth role="employee">
              <ManagementRefunds />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/approvals"
          element={
            <RequireAuth role="employee">
              <ManagementApprovals />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/discounts"
          element={
            <RequireAuth role="employee">
              <ManagementDiscounts />
            </RequireAuth>
          }
        />
        <Route
          path="/dashboard/management/cash-reconciliation"
          element={
            <RequireAuth role="employee">
              <ManagementCashReconciliation />
            </RequireAuth>
          }
        />
        <Route path="*" element={<Navigate to="/" replace />} />
      </Route>
    </Routes>
  );
}
