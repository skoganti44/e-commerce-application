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
  Select,
} from '@mui/material';
import KitchenIcon from '@mui/icons-material/Kitchen';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import RefreshIcon from '@mui/icons-material/Refresh';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import NoteAddIcon from '@mui/icons-material/NoteAdd';
import EditIcon from '@mui/icons-material/Edit';
import SendIcon from '@mui/icons-material/Send';
import {
  fetchSupplies,
  saveSupply,
  bulkUpdateSupplyStatuses,
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

const CATEGORY_OPTIONS = Object.keys(CATEGORY_META);
const UNIT_OPTIONS = ['kg', 'g', 'l', 'ml', 'pcs', 'box', 'pack'];

const ORDER_STATUSES = ['received', 'waiting', 'urgency'];
const ORDER_STATUS_META = {
  received: { label: 'Received', bg: '#e8f5e9', fg: '#2e7d32' },
  waiting: { label: 'Waiting', bg: '#fff3e0', fg: '#e65100' },
  urgency: { label: 'Urgency', bg: '#ffebee', fg: '#b71c1c' },
};

const EMPTY_FORM = {
  id: null,
  name: '',
  unit: 'kg',
  category: 'other',
  inStock: '0',
  threshold: '0',
  notes: '',
};

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

export default function KitchenSupplies() {
  const [supplies, setSupplies] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');
  const [dialogOpen, setDialogOpen] = useState(false);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [statusDrafts, setStatusDrafts] = useState({});
  const [qtyDrafts, setQtyDrafts] = useState({});
  const [publishing, setPublishing] = useState(false);

  const load = () => {
    setLoading(true);
    setError('');
    fetchSupplies()
      .then((data) => {
        setSupplies(Array.isArray(data) ? data : []);
        setStatusDrafts({});
        setQtyDrafts({});
      })
      .catch((err) => setError(err.message || 'Failed to load supplies.'))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const statusFor = (s) =>
    statusDrafts[s.id] ?? (s.orderStatus || 'waiting').toLowerCase();

  const qtyFor = (s) =>
    qtyDrafts[s.id] ?? (s.requestedQty ? String(fmt(s.requestedQty)) : '0');

  const handleStatusDraft = (id, value) => {
    setStatusDrafts((d) => ({ ...d, [id]: value }));
  };

  const handleQtyDraft = (s, value) => {
    setQtyDrafts((d) => ({ ...d, [s.id]: value }));
    const num = Number(value);
    const currentStatus = (s.orderStatus || 'received').toLowerCase();
    if (num > 0 && currentStatus === 'received' && statusDrafts[s.id] === undefined) {
      setStatusDrafts((d) => ({ ...d, [s.id]: 'waiting' }));
    }
  };

  const pendingChanges = useMemo(() => {
    return supplies.filter((s) => {
      const curStatus = (s.orderStatus || 'received').toLowerCase();
      const draftStatus = statusDrafts[s.id];
      const curQty = Number(s.requestedQty || 0);
      const draftQty = qtyDrafts[s.id];
      const statusChanged = draftStatus !== undefined && draftStatus !== curStatus;
      const qtyChanged =
        draftQty !== undefined &&
        Number.isFinite(Number(draftQty)) &&
        Number(draftQty) !== curQty;
      return statusChanged || qtyChanged;
    });
  }, [supplies, statusDrafts, qtyDrafts]);

  const handlePublish = async () => {
    if (pendingChanges.length === 0) {
      setToast('No changes to publish.');
      return;
    }
    setPublishing(true);
    try {
      const updates = pendingChanges.map((s) => {
        const curStatus = (s.orderStatus || 'received').toLowerCase();
        const status = statusDrafts[s.id] ?? curStatus;
        const qty = qtyDrafts[s.id];
        const out = { id: s.id, orderStatus: status };
        if (qty !== undefined && Number.isFinite(Number(qty))) {
          out.requestedQty = Number(qty);
        }
        return out;
      });
      const saved = await bulkUpdateSupplyStatuses(updates);
      const savedMap = new Map(saved.map((row) => [row.id, row]));

      setSupplies((list) => {
        const next = list
          .map((s) => (savedMap.has(s.id) ? { ...s, ...savedMap.get(s.id) } : s))
          .filter((s) => (s.orderStatus || 'received').toLowerCase() !== 'received');
        return next;
      });
      setStatusDrafts({});
      setQtyDrafts({});
      const receivedCount = saved.filter((r) => r.orderStatus === 'received').length;
      const urgencies = saved.filter((r) => r.orderStatus === 'urgency').length;
      const waitings = saved.filter((r) => r.orderStatus === 'waiting').length;
      const parts = [];
      if (urgencies) parts.push(`${urgencies} urgent`);
      if (waitings) parts.push(`${waitings} waiting`);
      if (receivedCount) parts.push(`${receivedCount} received → pantry`);
      setToast(parts.length ? `Updated: ${parts.join(', ')}.` : 'List updated.');
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Update failed.');
    } finally {
      setPublishing(false);
    }
  };

  const urgencies = useMemo(
    () =>
      supplies.filter(
        (s) => (s.orderStatus || '').toLowerCase() === 'urgency'
      ).length,
    [supplies]
  );
  const waitings = useMemo(
    () =>
      supplies.filter(
        (s) => (s.orderStatus || '').toLowerCase() === 'waiting'
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
    return Array.from(map.entries()).sort((a, b) =>
      a[0].localeCompare(b[0])
    );
  }, [supplies]);

  const openCreate = () => {
    setForm(EMPTY_FORM);
    setDialogOpen(true);
  };

  const openEdit = (s) => {
    setForm({
      id: s.id,
      name: s.name,
      unit: s.unit || 'kg',
      category: s.category || 'other',
      inStock: fmt(s.inStock),
      threshold: fmt(s.threshold),
      notes: s.notes || '',
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    if (!form.name.trim()) {
      setToast('Name is required.');
      return;
    }
    setSaving(true);
    try {
      const payload = {
        id: form.id,
        name: form.name.trim(),
        unit: form.unit,
        category: form.category,
        inStock: Number(form.inStock) || 0,
        threshold: Number(form.threshold) || 0,
        notes: form.notes || null,
      };
      const saved = await saveSupply(payload);
      const isPending =
        (saved.orderStatus || '').toLowerCase() !== 'received';
      setSupplies((list) => {
        const exists = list.some((s) => s.id === saved.id);
        if (!isPending) {
          return list.filter((s) => s.id !== saved.id);
        }
        if (exists) {
          return list.map((s) => (s.id === saved.id ? saved : s));
        }
        return [...list, saved].sort((a, b) => a.name.localeCompare(b.name));
      });
      setToast(`${saved.name} saved.`);
      setDialogOpen(false);
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Save failed.');
    } finally {
      setSaving(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <KitchenIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            Order Supplies
          </Typography>
        </Stack>

        <Typography variant="body2" sx={{ mb: 2, color: '#6d4c41' }}>
          Only items being ordered show here. Items marked{' '}
          <b>received</b> and updated are moved to the In-Stock pantry page.
        </Typography>

        <Stack direction="row" spacing={1.5} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <Chip
            icon={<WarningAmberIcon sx={{ color: '#b71c1c !important' }} />}
            label={`${urgencies} urgency`}
            sx={{ fontWeight: 700, bgcolor: '#ffebee', color: '#b71c1c' }}
          />
          <Chip
            label={`${waitings} waiting`}
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
          <Button
            size="small"
            startIcon={<NoteAddIcon />}
            onClick={openCreate}
            sx={{ ...RAINBOW_FILLED_BTN }}
          >
            Add Supply
          </Button>
          <Button
            size="small"
            startIcon={<SendIcon />}
            onClick={handlePublish}
            disabled={publishing || pendingChanges.length === 0}
            variant="contained"
            sx={{
              fontWeight: 800,
              letterSpacing: 0.5,
              textTransform: 'uppercase',
              bgcolor: pendingChanges.length > 0 ? '#b71c1c' : undefined,
              '&:hover': {
                bgcolor: pendingChanges.length > 0 ? '#9a1414' : undefined,
              },
            }}
          >
            {publishing
              ? 'Sending…'
              : pendingChanges.length > 0
              ? `Update list (${pendingChanges.length})`
              : 'Update list'}
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && supplies.length === 0 && (
          <Alert severity="success">
            No open supply requests. The kitchen pantry is fully stocked — open
            the In-Stock page to request more of any item.
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
                        <TableCell sx={{ fontWeight: 800 }}>Needed</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Delivered</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Order</TableCell>
                        <TableCell sx={{ fontWeight: 800 }}>Edit</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {rows.map((s) => {
                        const draftStatus = statusFor(s);
                        const statusMeta =
                          ORDER_STATUS_META[draftStatus] ||
                          ORDER_STATUS_META.received;
                        const curStatus = (
                          s.orderStatus || 'received'
                        ).toLowerCase();
                        const dirtyStatus = draftStatus !== curStatus;
                        const qty = qtyFor(s);
                        const curQty = Number(s.requestedQty || 0);
                        const dirtyQty =
                          qtyDrafts[s.id] !== undefined &&
                          Number.isFinite(Number(qty)) &&
                          Number(qty) !== curQty;
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
                              <Typography sx={{ fontWeight: 800 }}>
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
                            <TableCell sx={{ minWidth: 140 }}>
                              <TextField
                                type="number"
                                size="small"
                                value={qty}
                                onChange={(e) =>
                                  handleQtyDraft(s, e.target.value)
                                }
                                disabled={publishing}
                                inputProps={{ step: 'any', min: 0 }}
                                sx={{
                                  width: 110,
                                  '& input': {
                                    fontWeight: 700,
                                    textAlign: 'right',
                                  },
                                  outline: dirtyQty
                                    ? '2px solid #b71c1c'
                                    : 'none',
                                  outlineOffset: dirtyQty ? 1 : 0,
                                  borderRadius: 1,
                                }}
                                helperText={s.unit}
                              />
                            </TableCell>
                            <TableCell sx={{ color: '#6d4c41' }}>
                              {Number(s.currentStock) > 0 ? (
                                <Chip
                                  size="small"
                                  label={`${fmt(s.currentStock)} ${s.unit}`}
                                  sx={{
                                    fontWeight: 800,
                                    bgcolor: '#e8f5e9',
                                    color: '#2e7d32',
                                  }}
                                />
                              ) : (
                                <Typography
                                  variant="caption"
                                  sx={{ color: '#999' }}
                                >
                                  —
                                </Typography>
                              )}
                            </TableCell>
                            <TableCell>
                              <Stack spacing={0.25}>
                                <Select
                                  size="small"
                                  value={draftStatus}
                                  onChange={(e) =>
                                    handleStatusDraft(s.id, e.target.value)
                                  }
                                  disabled={publishing}
                                  sx={{
                                    minWidth: 130,
                                    fontWeight: 800,
                                    letterSpacing: 0.3,
                                    bgcolor: statusMeta.bg,
                                    color: statusMeta.fg,
                                    outline: dirtyStatus
                                      ? '2px solid #b71c1c'
                                      : 'none',
                                    outlineOffset: dirtyStatus ? 1 : 0,
                                    '& .MuiSelect-select': { py: 0.75 },
                                  }}
                                >
                                  {ORDER_STATUSES.map((opt) => {
                                    const om = ORDER_STATUS_META[opt];
                                    return (
                                      <MenuItem key={opt} value={opt}>
                                        <Chip
                                          size="small"
                                          label={om.label.toUpperCase()}
                                          sx={{
                                            bgcolor: om.bg,
                                            color: om.fg,
                                            fontWeight: 800,
                                          }}
                                        />
                                      </MenuItem>
                                    );
                                  })}
                                </Select>
                                {s.requestedAt && !dirtyStatus && (
                                  <Typography
                                    variant="caption"
                                    sx={{ color: '#6d4c41' }}
                                  >
                                    since {s.requestedAt.slice(0, 10)}{' '}
                                    {s.requestedAt.slice(11, 16)}
                                  </Typography>
                                )}
                              </Stack>
                            </TableCell>
                            <TableCell>
                              <IconButton
                                size="small"
                                onClick={() => openEdit(s)}
                                disabled={publishing}
                              >
                                <EditIcon fontSize="small" />
                              </IconButton>
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
            to="/dashboard/kitchen/in-stock"
            disableRipple
            sx={RAINBOW_OUTLINE_BTN}
          >
            In-Stock Pantry →
          </Button>
        </Stack>
      </Paper>

      <Dialog
        open={dialogOpen}
        onClose={() => (saving ? null : setDialogOpen(false))}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          {form.id ? 'Edit Supply' : 'Add Supply'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Name"
              value={form.name}
              onChange={(e) => setForm((f) => ({ ...f, name: e.target.value }))}
              fullWidth
              required
              autoFocus
            />
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Category"
                value={form.category}
                onChange={(e) =>
                  setForm((f) => ({ ...f, category: e.target.value }))
                }
                sx={{ flex: 1 }}
              >
                {CATEGORY_OPTIONS.map((c) => (
                  <MenuItem key={c} value={c}>
                    {CATEGORY_META[c].label}
                  </MenuItem>
                ))}
              </TextField>
              <TextField
                select
                label="Unit"
                value={form.unit}
                onChange={(e) =>
                  setForm((f) => ({ ...f, unit: e.target.value }))
                }
                sx={{ width: 140 }}
              >
                {UNIT_OPTIONS.map((u) => (
                  <MenuItem key={u} value={u}>
                    {u}
                  </MenuItem>
                ))}
              </TextField>
            </Stack>
            <Stack direction="row" spacing={2}>
              <TextField
                label="In pantry"
                type="number"
                value={form.inStock}
                onChange={(e) =>
                  setForm((f) => ({ ...f, inStock: e.target.value }))
                }
                sx={{ flex: 1 }}
                inputProps={{ step: 'any', min: 0 }}
              />
              <TextField
                label="Low-stock threshold"
                type="number"
                value={form.threshold}
                onChange={(e) =>
                  setForm((f) => ({ ...f, threshold: e.target.value }))
                }
                sx={{ flex: 1 }}
                inputProps={{ step: 'any', min: 0 }}
                helperText="Alert when pantry is at or below this"
              />
            </Stack>
            <TextField
              label="Notes (supplier, brand, etc.)"
              value={form.notes}
              onChange={(e) =>
                setForm((f) => ({ ...f, notes: e.target.value }))
              }
              multiline
              minRows={2}
              fullWidth
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)} disabled={saving}>
            Cancel
          </Button>
          <Button
            onClick={handleSave}
            disabled={saving}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            {saving ? 'Saving…' : 'Save'}
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
