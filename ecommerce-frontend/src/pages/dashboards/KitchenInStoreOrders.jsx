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
  LinearProgress,
  Chip,
  Alert,
  CircularProgress,
  Button,
  Divider,
  Grid,
  Select,
  MenuItem,
  Snackbar,
} from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import InventoryIcon from '@mui/icons-material/Inventory';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import RefreshIcon from '@mui/icons-material/Refresh';
import IconButton from '@mui/material/IconButton';
import {
  fetchKitchenInStoreOrders,
  fetchDailyStock,
  updateKitchenOrderStatus,
  adjustDailyStockPrepared,
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

export default function KitchenInStoreOrders() {
  const [orders, setOrders] = useState([]);
  const [stock, setStock] = useState([]);
  const [drafts, setDrafts] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [savingId, setSavingId] = useState(null);
  const [toast, setToast] = useState('');

  const reload = () => {
    setLoading(true);
    setError('');
    Promise.all([fetchKitchenInStoreOrders(), fetchDailyStock()])
      .then(([os, st]) => {
        setOrders(os || []);
        setStock(st || []);
      })
      .catch((err) =>
        setError(err.message || 'Failed to load kitchen data.')
      )
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    Promise.all([fetchKitchenInStoreOrders(), fetchDailyStock()])
      .then(([os, st]) => {
        if (cancelled) return;
        setOrders(os || []);
        setStock(st || []);
      })
      .catch((err) => {
        if (!cancelled) setError(err.message || 'Failed to load kitchen data.');
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    const poll = setInterval(() => {
      if (cancelled) return;
      Promise.all([fetchKitchenInStoreOrders(), fetchDailyStock()])
        .then(([os, st]) => {
          if (cancelled) return;
          setOrders(os || []);
          setStock(st || []);
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

  const [stockSavingId, setStockSavingId] = useState(null);
  const handleStockAdjust = async (stockId, delta) => {
    setStockSavingId(stockId);
    try {
      const updated = await adjustDailyStockPrepared(stockId, delta);
      setStock((list) =>
        list.map((s) => (s.id === stockId ? updated : s))
      );
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Update failed.');
    } finally {
      setStockSavingId(null);
    }
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
        setToast(`Order #${orderId} marked DONE → handed to bakery counter.`);
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

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <StorefrontIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
              flexGrow: 1,
            }}
          >
            In-Store Orders & Daily Stock
          </Typography>
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

        {!loading && !error && (
          <Grid container spacing={3}>
            <Grid item xs={12} md={7}>
              <Typography
                variant="subtitle1"
                sx={{
                  fontWeight: 800,
                  textTransform: 'uppercase',
                  letterSpacing: 1,
                  mb: 1,
                  ...RAINBOW_TEXT,
                }}
              >
                Walk-in orders to prepare
              </Typography>
              {orders.length === 0 ? (
                <Alert severity="info">No in-store orders right now.</Alert>
              ) : (
                <Box sx={{ overflowX: 'auto' }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
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
                            <TableCell sx={{ fontWeight: 700 }}>
                              #{o.orderId}
                            </TableCell>
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
                            <TableCell>
                              <WaitingTime since={o.createdAt} />
                            </TableCell>
                            <TableCell>
                              <StatusSelect
                                value={draft}
                                disabled={savingId === o.orderId}
                                onChange={(val) =>
                                  handleDraftChange(o.orderId, val)
                                }
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
            </Grid>

            <Grid item xs={12} md={5}>
              <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                <InventoryIcon sx={{ color: '#ab47bc' }} />
                <Typography
                  variant="subtitle1"
                  sx={{
                    fontWeight: 800,
                    textTransform: 'uppercase',
                    letterSpacing: 1,
                    ...RAINBOW_TEXT,
                  }}
                >
                  Today's bake targets
                </Typography>
              </Stack>
              {stock.length === 0 ? (
                <Alert severity="info">
                  No daily targets set for today.
                </Alert>
              ) : (
                <Stack spacing={2}>
                  {stock.map((s) => {
                    const target = s.targetCount || 0;
                    const prepared = s.preparedCount || 0;
                    const pct = target > 0
                      ? Math.min(100, Math.round((prepared / target) * 100))
                      : 0;
                    return (
                      <Box
                        key={s.id}
                        sx={{
                          p: 1.5,
                          borderRadius: 2,
                          border: '1px solid rgba(0,0,0,0.08)',
                          bgcolor: 'rgba(255,255,255,0.6)',
                        }}
                      >
                        <Stack
                          direction="row"
                          justifyContent="space-between"
                          alignItems="center"
                          sx={{ mb: 0.5 }}
                        >
                          <Typography sx={{ fontWeight: 700, color: '#5d4037' }}>
                            {s.productName}
                          </Typography>
                          <Typography
                            variant="caption"
                            sx={{ fontWeight: 700, color: '#6d4c41' }}
                          >
                            {prepared} / {target} · {s.remaining} to go
                          </Typography>
                        </Stack>
                        <LinearProgress
                          variant="determinate"
                          value={pct}
                          sx={{
                            height: 8,
                            borderRadius: 4,
                            bgcolor: '#f5f5f5',
                            '& .MuiLinearProgress-bar': {
                              background:
                                'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
                            },
                          }}
                        />
                        <Stack
                          direction="row"
                          spacing={0.5}
                          alignItems="center"
                          justifyContent="flex-end"
                          sx={{ mt: 1 }}
                        >
                          <Typography variant="caption" sx={{ color: '#6d4c41', mr: 1 }}>
                            Just baked:
                          </Typography>
                          <IconButton
                            size="small"
                            disabled={stockSavingId === s.id || prepared === 0}
                            onClick={() => handleStockAdjust(s.id, -1)}
                            sx={{ bgcolor: '#ffebee', '&:hover': { bgcolor: '#ffcdd2' } }}
                          >
                            <RemoveIcon fontSize="small" />
                          </IconButton>
                          {[1, 6, 12].map((n) => (
                            <Button
                              key={n}
                              size="small"
                              variant="outlined"
                              disabled={stockSavingId === s.id}
                              onClick={() => handleStockAdjust(s.id, n)}
                              sx={{
                                minWidth: 36,
                                fontWeight: 800,
                                borderColor: '#a5d6a7',
                                color: '#2e7d32',
                                '&:hover': {
                                  bgcolor: '#e8f5e9',
                                  borderColor: '#66bb6a',
                                },
                              }}
                            >
                              +{n}
                            </Button>
                          ))}
                          <IconButton
                            size="small"
                            disabled={stockSavingId === s.id}
                            onClick={() => handleStockAdjust(s.id, 1)}
                            sx={{ bgcolor: '#e8f5e9', '&:hover': { bgcolor: '#c8e6c9' } }}
                          >
                            <AddIcon fontSize="small" />
                          </IconButton>
                        </Stack>
                      </Box>
                    );
                  })}
                </Stack>
              )}
            </Grid>
          </Grid>
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
