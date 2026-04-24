import { useEffect, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
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
  Select,
  MenuItem,
  Snackbar,
} from '@mui/material';
import RestaurantMenuIcon from '@mui/icons-material/RestaurantMenu';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import RefreshIcon from '@mui/icons-material/Refresh';
import {
  fetchKitchenOnlineOrders,
  updateKitchenOrderStatus,
} from '../../api/endpoints.js';
import KitchenOrderItems from './KitchenOrderItems.jsx';
import WaitingTime from './WaitingTime.jsx';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const STATUS_COLORS = {
  pending: { bg: '#fff3e0', fg: '#e65100' },
  baking: { bg: '#e3f2fd', fg: '#1565c0' },
  ready: { bg: '#e8f5e9', fg: '#2e7d32' },
  done: { bg: '#ede7f6', fg: '#4527a0' },
  cancelled: { bg: '#ffebee', fg: '#b71c1c' },
};

const STATUS_OPTIONS = ['pending', 'baking', 'ready', 'done', 'cancelled'];

function StatusSelect({ value, onChange, disabled }) {
  const key = (value || 'pending').toLowerCase();
  const style = STATUS_COLORS[key] || { bg: '#f5f5f5', fg: '#555' };
  return (
    <Select
      size="small"
      value={key}
      disabled={disabled}
      onChange={(e) => onChange(e.target.value)}
      sx={{
        minWidth: 130,
        fontWeight: 800,
        letterSpacing: 0.5,
        bgcolor: style.bg,
        color: style.fg,
        '& .MuiSelect-select': { py: 0.75 },
      }}
    >
      {STATUS_OPTIONS.map((opt) => {
        const s = STATUS_COLORS[opt];
        return (
          <MenuItem key={opt} value={opt}>
            <Chip
              size="small"
              label={opt.toUpperCase()}
              sx={{
                bgcolor: s.bg,
                color: s.fg,
                fontWeight: 800,
                letterSpacing: 0.5,
              }}
            />
          </MenuItem>
        );
      })}
    </Select>
  );
}

export default function KitchenOnlineOrders() {
  const [orders, setOrders] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [savingId, setSavingId] = useState(null);
  const [toast, setToast] = useState('');

  const reload = () => {
    setLoading(true);
    setError('');
    fetchKitchenOnlineOrders()
      .then((data) => setOrders(data || []))
      .catch((err) => setError(err.message || 'Failed to load orders.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    fetchKitchenOnlineOrders()
      .then((data) => {
        if (!cancelled) setOrders(data || []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load orders.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    const poll = setInterval(() => {
      if (cancelled) return;
      fetchKitchenOnlineOrders()
        .then((data) => {
          if (!cancelled) setOrders(data || []);
        })
        .catch(() => {});
    }, 30000);
    return () => {
      cancelled = true;
      clearInterval(poll);
    };
  }, []);

  const draftFor = (o) =>
    drafts[o.orderId] ?? (o.kitchenStatus || 'pending').toLowerCase();

  const handleDraftChange = (orderId, newStatus) => {
    setDrafts((d) => ({ ...d, [orderId]: newStatus }));
  };

  const handleUpdate = async (orderId) => {
    const draft = drafts[orderId];
    if (!draft) return;
    let reason = null;
    if (draft === 'cancelled') {
      reason = window.prompt(
        `Cancel order #${orderId}? Enter reason (e.g. "out of millet flour"):`
      );
      if (reason === null) return;
      if (!reason.trim()) {
        setToast('Cancellation reason is required.');
        return;
      }
    }
    setSavingId(orderId);
    try {
      await updateKitchenOrderStatus(orderId, draft, reason);
      if (draft === 'done') {
        setOrders((list) => list.filter((o) => o.orderId !== orderId));
        setToast(`Order #${orderId} marked DONE → handed to delivery team.`);
      } else if (draft === 'cancelled') {
        setOrders((list) => list.filter((o) => o.orderId !== orderId));
        setToast(`Order #${orderId} cancelled: ${reason}`);
      } else {
        setOrders((list) =>
          list.map((o) =>
            o.orderId === orderId ? { ...o, kitchenStatus: draft } : o
          )
        );
        setToast(`Order #${orderId} → ${draft.toUpperCase()}`);
      }
      setDrafts((d) => {
        const copy = { ...d };
        delete copy[orderId];
        return copy;
      });
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Update failed.');
    } finally {
      setSavingId(null);
    }
  };

  const totalItems = orders.reduce(
    (sum, o) => sum + (o.items?.reduce((s, i) => s + (i.quantity || 0), 0) || 0),
    0
  );

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <RestaurantMenuIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            Online Orders to Bake
          </Typography>
        </Stack>

        <Stack direction="row" spacing={2} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <Chip
            label={`${orders.length} open order${orders.length === 1 ? '' : 's'}`}
            sx={{ fontWeight: 700, bgcolor: '#fff3e0', color: '#e65100' }}
          />
          <Chip
            label={`${totalItems} item${totalItems === 1 ? '' : 's'} to prepare`}
            sx={{ fontWeight: 700, bgcolor: '#e3f2fd', color: '#1565c0' }}
          />
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={reload}
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
          <Alert severity="info">No online orders waiting right now.</Alert>
        )}

        {!loading && !error && orders.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Items</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Waiting</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((o) => {
                  const current = (o.kitchenStatus || 'pending').toLowerCase();
                  const draft = draftFor(o);
                  const dirty = draft !== current;
                  return (
                    <TableRow key={o.orderId}>
                      <TableCell sx={{ fontWeight: 700 }}>#{o.orderId}</TableCell>
                      <TableCell>{o.customerName}</TableCell>
                      <TableCell>
                        <KitchenOrderItems items={o.items} />
                        {o.customerNotes && (
                          <Alert
                            severity="warning"
                            icon={false}
                            sx={{
                              mt: 1,
                              py: 0,
                              fontSize: 12,
                              fontWeight: 700,
                              '& .MuiAlert-message': { py: 0.5 },
                            }}
                          >
                            📝 {o.customerNotes}
                          </Alert>
                        )}
                      </TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>
                        <Stack spacing={0.25}>
                          <WaitingTime since={o.createdAt} />
                          <Typography variant="caption" sx={{ color: '#8d6e63' }}>
                            {o.createdAt
                              ? o.createdAt.slice(11, 16)
                              : '—'}
                          </Typography>
                        </Stack>
                      </TableCell>
                      <TableCell>
                        <StatusSelect
                          value={draft}
                          disabled={savingId === o.orderId}
                          onChange={(val) => handleDraftChange(o.orderId, val)}
                        />
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="contained"
                          size="small"
                          disabled={!dirty || savingId === o.orderId}
                          onClick={() => handleUpdate(o.orderId)}
                          sx={{
                            fontWeight: 800,
                            letterSpacing: 0.5,
                            textTransform: 'uppercase',
                          }}
                        >
                          {savingId === o.orderId ? 'Saving…' : 'Update'}
                        </Button>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        )}

        <Divider sx={{ my: 3 }} />
        <Button
          component={RouterLink}
          to="/dashboard/kitchen"
          startIcon={<ArrowBackIcon sx={{ color: '#5d4037' }} />}
          disableRipple
          sx={RAINBOW_OUTLINE_BTN}
        >
          Back to Kitchen
        </Button>
      </Paper>
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
