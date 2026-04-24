import { useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Stack,
  Box,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Alert,
  CircularProgress,
  Button,
  Divider,
  Snackbar,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import RefreshIcon from '@mui/icons-material/Refresh';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import UndoIcon from '@mui/icons-material/Undo';
import {
  fetchDeliveryOnlineOrders,
  updateKitchenOrderStatus,
} from '../../api/endpoints.js';
import KitchenOrderItems from './KitchenOrderItems.jsx';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';
import EmployeeQuickTools from './EmployeeQuickTools.jsx';

export default function DeliveryDashboard() {
  const user = useSelector(selectCurrentUser);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [sendingId, setSendingId] = useState(null);
  const [toast, setToast] = useState('');

  const sendBack = async (orderId) => {
    setSendingId(orderId);
    try {
      await updateKitchenOrderStatus(orderId, 'ready');
      setOrders((list) => list.filter((o) => o.orderId !== orderId));
      setToast(`Order #${orderId} sent back to kitchen.`);
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Send-back failed.');
    } finally {
      setSendingId(null);
    }
  };

  const load = () => {
    setLoading(true);
    setError('');
    fetchDeliveryOnlineOrders()
      .then((data) => setOrders(data || []))
      .catch((err) =>
        setError(err.message || 'Failed to load delivery queue.')
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchDeliveryOnlineOrders()
      .then((data) => {
        if (!cancelled) setOrders(data || []);
      })
      .catch((err) => {
        if (!cancelled)
          setError(err.message || 'Failed to load delivery queue.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const totalItems = orders.reduce(
    (sum, o) =>
      sum + (o.items?.reduce((s, i) => s + (i.quantity || 0), 0) || 0),
    0
  );

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <LocalShippingIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            Welcome to Delivery, {user?.name || 'Driver'}
          </Typography>
        </Stack>
        <Typography sx={{ mb: 2, color: '#6d4c41', fontWeight: 600 }}>
          Online orders the kitchen has marked DONE — ready to go out.
        </Typography>

        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <Chip
            label={`${orders.length} order${orders.length === 1 ? '' : 's'} ready`}
            sx={{ fontWeight: 700, bgcolor: '#ede7f6', color: '#4527a0' }}
          />
          <Chip
            label={`${totalItems} item${totalItems === 1 ? '' : 's'} to deliver`}
            sx={{ fontWeight: 700, bgcolor: '#e3f2fd', color: '#1565c0' }}
          />
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && orders.length === 0 && (
          <Alert severity="info">
            No deliveries waiting. Once the kitchen marks an online order DONE,
            it'll show up here.
          </Alert>
        )}

        {!loading && !error && orders.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Items</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Total</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Placed</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((o) => (
                  <TableRow key={o.orderId}>
                    <TableCell sx={{ fontWeight: 700 }}>#{o.orderId}</TableCell>
                    <TableCell>{o.customerName}</TableCell>
                    <TableCell>
                      <KitchenOrderItems items={o.items} />
                    </TableCell>
                    <TableCell sx={{ fontWeight: 700, color: '#2e7d32' }}>
                      ${Number(o.totalAmount || 0).toFixed(2)}
                    </TableCell>
                    <TableCell sx={{ color: '#6d4c41' }}>
                      {o.createdAt
                        ? o.createdAt.slice(0, 16).replace('T', ' ')
                        : '—'}
                    </TableCell>
                    <TableCell>
                      <Button
                        size="small"
                        startIcon={<UndoIcon />}
                        disabled={sendingId === o.orderId}
                        onClick={() => sendBack(o.orderId)}
                        sx={{
                          fontWeight: 700,
                          color: '#c62828',
                          textTransform: 'none',
                        }}
                      >
                        {sendingId === o.orderId ? 'Sending…' : 'Send back'}
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}

        <Divider sx={{ my: 3 }} />
      </Paper>
      <EmployeeQuickTools />
      <Snackbar
        open={Boolean(toast)}
        autoHideDuration={2500}
        onClose={() => setToast('')}
        message={toast}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      />
    </Box>
  );
}
