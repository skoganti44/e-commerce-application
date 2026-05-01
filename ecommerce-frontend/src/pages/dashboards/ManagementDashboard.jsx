import { useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Box,
  Paper,
  Typography,
  Stack,
  Chip,
  Alert,
  CircularProgress,
  Button,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Snackbar,
} from '@mui/material';
import InventoryIcon from '@mui/icons-material/Inventory2';
import RefreshIcon from '@mui/icons-material/Refresh';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import DashboardIcon from '@mui/icons-material/Dashboard';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import GroupsIcon from '@mui/icons-material/Groups';
import GavelIcon from '@mui/icons-material/Gavel';
import VerifiedIcon from '@mui/icons-material/Verified';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import SavingsIcon from '@mui/icons-material/Savings';
import { Link as RouterLink } from 'react-router-dom';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchSupplyRequests,
  fulfillSupply,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';
import EmployeeQuickTools from './EmployeeQuickTools.jsx';
import MyTasks from './MyTasks.jsx';

const STATUS_META = {
  waiting: { label: 'Waiting', bg: '#fff3e0', fg: '#e65100', rank: 1 },
  urgency: { label: 'Urgency', bg: '#ffebee', fg: '#b71c1c', rank: 0 },
  received: { label: 'Received', bg: '#e8f5e9', fg: '#2e7d32', rank: 2 },
};

function fmt(n) {
  if (n === null || n === undefined) return '0';
  const num = Number(n);
  if (Number.isNaN(num)) return String(n);
  if (Number.isInteger(num)) return String(num);
  return num.toFixed(2).replace(/\.?0+$/, '');
}

export default function ManagementDashboard() {
  const user = useSelector(selectCurrentUser);
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [toast, setToast] = useState('');

  const load = () => {
    setLoading(true);
    setError('');
    fetchSupplyRequests()
      .then((data) => setRequests(Array.isArray(data) ? data : []))
      .catch((err) =>
        setError(err.message || 'Failed to load supply requests.')
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const sorted = useMemo(() => {
    return [...requests].sort((a, b) => {
      const ra =
        STATUS_META[(a.orderStatus || '').toLowerCase()]?.rank ?? 99;
      const rb =
        STATUS_META[(b.orderStatus || '').toLowerCase()]?.rank ?? 99;
      if (ra !== rb) return ra - rb;
      return (a.requestedAt || '').localeCompare(b.requestedAt || '');
    });
  }, [requests]);

  const urgent = requests.filter(
    (r) => (r.orderStatus || '').toLowerCase() === 'urgency'
  ).length;
  const waiting = requests.filter(
    (r) => (r.orderStatus || '').toLowerCase() === 'waiting'
  ).length;

  const handleFulfill = async (supply) => {
    const suggested =
      supply.threshold && Number(supply.threshold) > 0
        ? String(Number(supply.threshold) * 2)
        : '1';
    const raw = window.prompt(
      `How much ${supply.unit} of "${supply.name}" was delivered?`,
      suggested
    );
    if (raw === null) return;
    const qty = Number(raw);
    if (!Number.isFinite(qty) || qty <= 0) {
      setToast('Enter a positive number.');
      return;
    }
    const note = window.prompt(
      'Supplier / invoice note (optional):',
      supply.notes || ''
    );
    setBusyId(supply.id);
    try {
      await fulfillSupply(supply.id, qty, note || null);
      setRequests((list) => list.filter((r) => r.id !== supply.id));
      setToast(
        `${supply.name}: +${fmt(qty)} ${supply.unit} received, marked fulfilled.`
      );
    } catch (err) {
      setToast(
        err.response?.data?.error || err.message || 'Fulfilment failed.'
      );
    } finally {
      setBusyId(null);
    }
  };

  return (
    <Box>
      <Paper
        elevation={0}
        sx={{
          borderRadius: 4,
          mb: 4,
          textAlign: 'center',
          p: { xs: 3, md: 5 },
          bgcolor: 'rgba(255,255,255,0.78)',
          backdropFilter: 'blur(8px)',
          border: '1px solid rgba(0,0,0,0.06)',
          boxShadow: '0 10px 30px rgba(60,30,10,0.12)',
        }}
      >
        <Typography
          variant="overline"
          sx={{ letterSpacing: 2, fontWeight: 700, ...RAINBOW_TEXT }}
        >
          Management
        </Typography>
        <Typography
          variant="h3"
          sx={{
            fontWeight: 800,
            letterSpacing: 1,
            mt: 0.5,
            ...RAINBOW_TEXT,
          }}
        >
          Welcome Manager, {user?.name || ''}
        </Typography>
        <Stack
          direction="row"
          spacing={1}
          justifyContent="center"
          flexWrap="wrap"
          sx={{ mt: 2 }}
        >
          <Button
            component={RouterLink}
            to="/dashboard/management/ops"
            startIcon={<DashboardIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Live Ops
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/orders-audit"
            startIcon={<ReceiptLongIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Orders audit
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/deliveries-audit"
            startIcon={<LocalShippingIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Deliveries audit
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/day-pnl"
            startIcon={<AccountBalanceIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Day P&amp;L
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/staff-performance"
            startIcon={<GroupsIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Staff performance
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/refunds"
            startIcon={<GavelIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refunds
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/approvals"
            startIcon={<VerifiedIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Approvals
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/discounts"
            startIcon={<LocalOfferIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Discounts
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/management/cash-reconciliation"
            startIcon={<SavingsIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Cash recon
          </Button>
        </Stack>
      </Paper>

      <Paper elevation={0} sx={{ ...GLASS_PAPER, mb: 3 }}>
        <Stack
          direction="row"
          spacing={1.5}
          alignItems="center"
          sx={{ mb: 2, flexWrap: 'wrap' }}
        >
          <InventoryIcon sx={{ color: '#ef5350', fontSize: 30 }} />
          <Typography
            variant="h6"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
              flexGrow: 1,
            }}
          >
            Supply Requests from Kitchen
          </Typography>
          <Chip
            icon={<WarningAmberIcon sx={{ color: '#b71c1c !important' }} />}
            label={`${urgent} urgent`}
            sx={{ fontWeight: 700, bgcolor: '#ffebee', color: '#b71c1c' }}
          />
          <Chip
            label={`${waiting} waiting`}
            sx={{ fontWeight: 700, bgcolor: '#fff3e0', color: '#e65100' }}
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

        {!loading && !error && requests.length === 0 && (
          <Alert severity="success">
            No pending supply requests. Kitchen is fully stocked.
          </Alert>
        )}

        {!loading && !error && requests.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>Item</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Category</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Team</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>In stock</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Threshold</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Requested</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Urgency</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Fulfill</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {sorted.map((r) => {
                  const statusKey = (r.orderStatus || '').toLowerCase();
                  const meta = STATUS_META[statusKey] || STATUS_META.waiting;
                  return (
                    <TableRow key={r.id}>
                      <TableCell>
                        <Typography sx={{ fontWeight: 700 }}>
                          {r.name}
                        </Typography>
                        {r.notes && (
                          <Typography
                            variant="caption"
                            sx={{ color: '#6d4c41' }}
                          >
                            📝 {r.notes}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>
                        {r.category}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          data-testid={`supply-team-${r.id}`}
                          label={(r.requestedByTeam || 'kitchen').toUpperCase()}
                          sx={{
                            fontWeight: 700,
                            bgcolor: '#e3f2fd',
                            color: '#1565c0',
                          }}
                        />
                      </TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>
                        {fmt(r.currentStock)} {r.unit}
                      </TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>
                        {fmt(r.threshold)} {r.unit}
                      </TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>
                        {r.requestedAt
                          ? `${r.requestedAt.slice(0, 10)} ${r.requestedAt.slice(11, 16)}`
                          : '—'}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={meta.label.toUpperCase()}
                          sx={{
                            bgcolor: meta.bg,
                            color: meta.fg,
                            fontWeight: 800,
                            letterSpacing: 0.5,
                          }}
                        />
                      </TableCell>
                      <TableCell>
                        <Button
                          size="small"
                          variant="contained"
                          color="success"
                          startIcon={<CheckCircleIcon />}
                          disabled={busyId === r.id}
                          onClick={() => handleFulfill(r)}
                          sx={{
                            fontWeight: 700,
                            textTransform: 'uppercase',
                          }}
                        >
                          {busyId === r.id ? 'Saving…' : 'Mark received'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

      <MyTasks department="management" />

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
