import { useSelector } from 'react-redux';
import DashboardStub from './DashboardStub.jsx';
import { selectCurrentUser } from '../../store/slices/authSlice.js';

export default function SalesDashboard() {
  const user = useSelector(selectCurrentUser);
  return (
    <DashboardStub
      title={`Welcome to Sales, ${user?.name || 'Teammate'}`}
      description="Sales department dashboard. Daily sales, promotions, and customer-facing tools will live here."
      showQuickTools
    />
  );
}
