import { useEffect, useMemo, useState } from 'react';
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
  IconButton,
  Snackbar,
  LinearProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
} from '@mui/material';
import InventoryIcon from '@mui/icons-material/Inventory2';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import RemoveCircleIcon from '@mui/icons-material/RemoveCircle';
import SendIcon from '@mui/icons-material/Send';
import {
  fetchInStockSupplies,
  adjustSupplyStock,
  requestMoreSupply,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const CATEGORY_META = {
  flour: { label: 'Flour', color: '#795548', bg: '#efebe9' },
  sweetener: { label: 'Sweetener', color: '#ef6c00', bg: '#fff3e0' },
  dairy: { label: 'Dairy', color: '#1565c0', bg: '#e3f2fd' },
  egg: { label: 'Egg', color: '#f9a825', bg: '#fffde7' },
  nut_seed: { label: 'Nut / Seed', color: '#6d4c41', bg: '#d7ccc8' },
  flavour: { label: 'Flavour', color: '#6a1b9a', bg: '#f3e5f5' },
  leavening: { label: 'Leavening', color: '#2e7d32', bg: '#e8f5e9' },
  packaging: { label: 'Packaging', color: '#455a64', bg: '#eceff1' },
  cleaning: { label: 'Cleaning', color: '#00838f', bg: '#e0f7fa' },
  other: { label: 'Other', color: '#555', bg: '#f5f5f5' },
};

const URGENCY_OPTIONS = [
  { value: 'waiting', label: 'Normal (waiting)' },
  { value: 'urgency', label: 'Urgent' },
];

function fmt(n) {
  if (n === null || n === undefined) return '0';
  const num = Number(n);
  if (Number.isNaN(num)) return String(n);
  if (Number.isInteger(num)) return String(num);
  return num.toFixed(2).replace(/\.?0+$/, '');
}

function StockBar({ current, threshold }) {
  const t = Number(threshold) || 0;
  const c = Number(current) || 0;
  const cap = Math.max(t * 2, t + 1, c, 1);
  const pct = Math.min(100, (c / cap) * 100);
  let color = 'success';
  if (c <= 0) color = 'error';
  else if (c <= t) color = 'warning';
  return (
    <LinearProgress
      variant="determinate"
      value={pct}
      color={color}
      sx={{ height: 6, borderRadius: 3, mt: 0.5 }}
    />
  );
}

export default function KitchenInStock() {
  const [supplies, setSupplies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [requestDialog, setRequestDialog] = useState({
    open: false,
    supply: null,
    qty: '',
    urgency: 'waiting',
  });
  const [requesting, setRequesting] = useState(false);

  const load = () => {
    setLoading(true);
    setError('');
    fetchInStockSupplies()
      .then((data) => setSupplies(Array.isArray(data) ? data : []))
      .catch((err) => setError(err.message || 'Failed to load pantry.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const handleAdjust = async (supply, delta) => {
    setBusyId(supply.id);
    try {
      const saved = await adjustSupplyStock(supply.id, delta);
      setSupplies((list) => {
        if (Number(saved.inStock) <= 0) {
          return list.filter((s) => s.id !== saved.id);
        }
        return list.map((s) => (s.id === saved.id ? saved : s));
      });
      setToast(
        `${saved.name}: ${delta > 0 ? '+' : ''}${delta} ${saved.unit}`
      );
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Adjust failed.');
    } finally {
      setBusyId(null);
    }
  };

  const openRequest = (supply) => {
    setRequestDialog({
      open: true,
      supply,
      qty: '',
      urgency: 'waiting',
    });
  };

  const closeRequest = () => {
    if (requesting) return;
    setRequestDialog({ open: false, supply: null, qty: '', urgency: 'waiting' });
  };

  const handleRequestMore = async () => {
    const { supply, qty, urgency } = requestDialog;
    if (!supply) return;
    const n = Number(qty);
    if (!Number.isFinite(n) || n <= 0) {
      setToast('Enter a positive quantity.');
      return;
    }
    setRequesting(true);
    try {
      await requestMoreSupply(supply.id, n, urgency);
      setToast(
        `Requested ${fmt(n)} ${supply.unit} of ${supply.name} (${urgency}).`
      );
      setRequestDialog({ open: false, supply: null, qty: '', urgency: 'waiting' });
    } catch (err) {
      setToast(
        err.response?.data?.error || err.message || 'Request failed.'
      );
    } finally {
      setRequesting(false);
    }
  };

  const lowStock = useMemo(
    () =>
      supplies.filter(
        (s) => Number(s.inStock) > 0 && Number(s.inStock) <= Number(s.threshold || 0)
      ).length,
    [supplies]
  );

  const grouped = useMemo(() => {
    const map = new Map();
    for (const s of supplies) {
      const key = s.category || 'other';
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(s);
    }
    return Array.from(map.entries()).sort((a, b) => a[0].localeCompare(b[0]));
  }, [supplies]);

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <InventoryIcon sx={{ color: '#2e7d32', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            In Stock — Pantry
          </Typography>
        </Stack>

        <Typography variant="body2" sx={{ mb: 2, color: '#6d4c41' }}>
          These are the ingredients currently available in the kitchen pantry.
          Use <b>+ / −</b> to record usage as you bake, and tap{' '}
          <b>Request more</b> to send a fresh order to Management.
        </Typography>

        <Stack direction="row" spacing={1.5} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <Chip
            label={`${supplies.length} in pantry`}
            sx={{ fontWeight: 700, bgcolor: '#e8f5e9', color: '#2e7d32' }}
          />
          <Chip
            label={`${lowStock} running low`}
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

        {!loading && !error && supplies.length === 0 && (
          <Alert severity="warning">
            Pantry is empty. Head to Order Supplies to request items.
          </Alert>
        )}

        {!loading && !error && supplies.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            {grouped.map(([category, rows]) => {
              const meta = CATEGORY_META[category] || CATEGORY_META.other;
              return (
                <Box key={category} sx={{ mb: 3 }}>
                  <Stack
                    direction="row"
                    spacing={1}
                    alignItems="center"
                    sx={{ mb: 1 }}
                  >
                    <Typography
                      variant="subtitle1"
                      sx={{
                        fontWeight: 800,
                        textTransform: 'uppercase',
                        letterSpacing: 1,
                        color: meta.color,
                      }}
                    >
                      {meta.label}
                    </Typography>
                    <Chip
                      size="small"
                      label={`${rows.length}`}
                      sx={{
                        fontWeight: 700,
                        bgcolor: meta.bg,
                        color: meta.color,
                      }}
                    />
                  </Stack>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 800 }}>Item</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>In pantry</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Threshold</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Quick adjust</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Order more</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((s) => {
                        const busy = busyId === s.id;
                        const low =
                          Number(s.inStock) <= Number(s.threshold || 0);
                        const pendingQty = Number(s.requestedQty || 0);
                        const pendingStatus = (
                          s.orderStatus || 'received'
                        ).toLowerCase();
                        const hasOpenRequest =
                          pendingQty > 0 && pendingStatus !== 'received';
                        return (
                          <TableRow key={s.id}>
                            <TableCell>
                              <Typography sx={{ fontWeight: 700 }}>
                                {s.name}
                              </Typography>
                              {s.notes && (
                                <Typography
                                  variant="caption"
                                  sx={{ color: '#6d4c41' }}
                                >
                                  📝 {s.notes}
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell sx={{ minWidth: 150 }}>
                              <Typography
                                sx={{
                                  fontWeight: 800,
                                  color: low ? '#e65100' : '#2e7d32',
                                }}
                              >
                                {fmt(s.inStock)} {s.unit}
                              </Typography>
                              <StockBar
                                current={s.inStock}
                                threshold={s.threshold}
                              />
                            </TableCell>
                            <TableCell sx={{ color: '#6d4c41' }}>
                              {fmt(s.threshold)} {s.unit}
                            </TableCell>
                            <TableCell>
                              <Stack direction="row" spacing={0.5}>
                                <IconButton
                                  size="small"
                                  onClick={() => handleAdjust(s, -1)}
                                  disabled={busy || Number(s.inStock) <= 0}
                                >
                                  <RemoveCircleIcon
                                    fontSize="small"
                                    sx={{ color: '#c62828' }}
                                  />
                                </IconButton>
                                <IconButton
                                  size="small"
                                  onClick={() => handleAdjust(s, 1)}
                                  disabled={busy}
                                >
                                  <AddCircleIcon
                                    fontSize="small"
                                    sx={{ color: '#2e7d32' }}
                                  />
                                </IconButton>
                              </Stack>
                            </TableCell>
                            <TableCell>
                              {hasOpenRequest ? (
                                <Chip
                                  size="small"
                                  label={`${pendingStatus.toUpperCase()} · ${fmt(
                                    pendingQty
                                  )} ${s.unit}`}
                                  sx={{
                                    fontWeight: 800,
                                    bgcolor:
                                      pendingStatus === 'urgency'
                                        ? '#ffebee'
                                        : '#fff3e0',
                                    color:
                                      pendingStatus === 'urgency'
                                        ? '#b71c1c'
                                        : '#e65100',
                                  }}
                                />
                              ) : (
                                <Button
                                  size="small"
                                  startIcon={<SendIcon />}
                                  onClick={() => openRequest(s)}
                                  sx={RAINBOW_OUTLINE_BTN}
                                >
                                  Request more
                                </Button>
                              )}
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </Box>
              );
            })}
          </Box>
        )}

        <Divider sx={{ my: 3 }} />
        <Stack direction="row" spacing={2}>
          <Button
            component={RouterLink}
            to="/dashboard/kitchen"
            startIcon={<ArrowBackIcon sx={{ color: '#5d4037' }} />}
            disableRipple
            sx={RAINBOW_OUTLINE_BTN}
          >
            Back to Kitchen
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/kitchen/supplies"
            disableRipple
            sx={RAINBOW_OUTLINE_BTN}
          >
            ← Order Supplies
          </Button>
        </Stack>
      </Paper>

      <Dialog
        open={requestDialog.open}
        onClose={closeRequest}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          Request more {requestDialog.supply?.name || ''}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography variant="body2" sx={{ color: '#6d4c41' }}>
              Currently in pantry:{' '}
              <b>
                {fmt(requestDialog.supply?.inStock)}{' '}
                {requestDialog.supply?.unit}
              </b>
            </Typography>
            <TextField
              label={`Quantity needed (${requestDialog.supply?.unit || ''})`}
              type="number"
              value={requestDialog.qty}
              onChange={(e) =>
                setRequestDialog((d) => ({ ...d, qty: e.target.value }))
              }
              autoFocus
              inputProps={{ step: 'any', min: 0 }}
              fullWidth
            />
            <TextField
              select
              label="Urgency"
              value={requestDialog.urgency}
              onChange={(e) =>
                setRequestDialog((d) => ({ ...d, urgency: e.target.value }))
              }
              fullWidth
            >
              {URGENCY_OPTIONS.map((o) => (
                <MenuItem key={o.value} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </TextField>
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeRequest} disabled={requesting}>
            Cancel
          </Button>
          <Button
            onClick={handleRequestMore}
            disabled={requesting}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            {requesting ? 'Sending…' : 'Send request'}
          </Button>
        </DialogActions>
      </Dialog>

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
