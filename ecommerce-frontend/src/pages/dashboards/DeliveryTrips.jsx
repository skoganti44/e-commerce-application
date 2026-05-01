import { useCallback, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Link as RouterLink } from 'react-router-dom';
import {
  Paper,
  Typography,
  Stack,
  Box,
  Alert,
  CircularProgress,
  Button,
  Chip,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  Snackbar,
} from '@mui/material';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import DirectionsRunIcon from '@mui/icons-material/DirectionsRun';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import CancelIcon from '@mui/icons-material/Cancel';
import PhoneIcon from '@mui/icons-material/Phone';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchDeliveryTrips,
  markTripOutForDelivery,
  markTripDelivered,
  markTripFailed,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const FAILURE_REASONS = [
  { value: 'customer_not_home', label: 'Customer not home' },
  { value: 'refused', label: 'Customer refused' },
  { value: 'damaged', label: 'Order damaged' },
  { value: 'wrong_address', label: 'Wrong address' },
  { value: 'other', label: 'Other' },
];

function statusColor(s) {
  switch ((s || '').toLowerCase()) {
    case 'delivered':        return { bg: '#c8e6c9', fg: '#1b5e20' };
    case 'out_for_delivery': return { bg: '#bbdefb', fg: '#0d47a1' };
    case 'picked_up':        return { bg: '#fff3e0', fg: '#e65100' };
    case 'failed':           return { bg: '#ffcdd2', fg: '#b71c1c' };
    default:                 return { bg: '#eceff1', fg: '#455a64' };
  }
}

export default function DeliveryTrips() {
  const user = useSelector(selectCurrentUser);
  const driverId = user?.userid;
  const [trips, setTrips] = useState([]);
  const [filter, setFilter] = useState('active');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [toast, setToast] = useState('');

  const [deliverDialog, setDeliverDialog] = useState({
    open: false,
    trip: null,
    otp: '',
    photoUrl: '',
    codAmount: '',
    tipAmount: '',
    distanceKm: '',
    notes: '',
    submitting: false,
    error: '',
  });

  const [failDialog, setFailDialog] = useState({
    open: false,
    trip: null,
    reason: 'customer_not_home',
    notes: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    if (!driverId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    fetchDeliveryTrips(driverId, filter)
      .then((data) => setTrips(Array.isArray(data) ? data : []))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load trips.')
      )
      .finally(() => setLoading(false));
  }, [driverId, filter]);

  useEffect(() => {
    load();
  }, [load]);

  const onMarkOut = async (trip) => {
    setBusyId(trip.id);
    try {
      await markTripOutForDelivery(trip.id, driverId);
      setToast(`Trip #${trip.id} is now OUT FOR DELIVERY.`);
      load();
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Update failed.');
    } finally {
      setBusyId(null);
    }
  };

  const openDeliver = (trip) => {
    setDeliverDialog({
      open: true,
      trip,
      otp: '',
      photoUrl: '',
      codAmount: trip.codAmount ? String(trip.codAmount) : '',
      tipAmount: '',
      distanceKm: '',
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitDeliver = async () => {
    const d = deliverDialog;
    if (!d.trip) return;
    setDeliverDialog((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await markTripDelivered(d.trip.id, {
        driverId,
        otp: d.otp.trim() || null,
        photoUrl: d.photoUrl.trim() || null,
        codAmount: d.codAmount ? Number(d.codAmount) : null,
        tipAmount: d.tipAmount ? Number(d.tipAmount) : null,
        distanceKm: d.distanceKm ? Number(d.distanceKm) : null,
        notes: d.notes.trim() || null,
      });
      setDeliverDialog({
        open: false,
        trip: null,
        otp: '',
        photoUrl: '',
        codAmount: '',
        tipAmount: '',
        distanceKm: '',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Trip #${d.trip.id} delivered.`);
      load();
    } catch (err) {
      setDeliverDialog((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Delivery failed.',
      }));
    }
  };

  const openFail = (trip) => {
    setFailDialog({
      open: true,
      trip,
      reason: 'customer_not_home',
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitFail = async () => {
    const f = failDialog;
    if (!f.trip) return;
    setFailDialog((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await markTripFailed(f.trip.id, {
        driverId,
        reason: f.reason,
        notes: f.notes.trim() || null,
      });
      setFailDialog({
        open: false,
        trip: null,
        reason: 'customer_not_home',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Trip #${f.trip.id} marked failed.`);
      load();
    } catch (err) {
      setFailDialog((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not mark failed.',
      }));
    }
  };

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <LocalShippingIcon sx={{ color: '#ef5350', fontSize: 30 }} />
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
            My Trips
          </Typography>
          <Button
            component={RouterLink}
            to="/dashboard/delivery"
            size="small"
            startIcon={<ArrowBackIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Back
          </Button>
        </Stack>

        <Stack direction="row" spacing={1} sx={{ mb: 2, flexWrap: 'wrap' }}>
          <TextField
            select
            size="small"
            label="Show"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            sx={{ minWidth: 170 }}
            inputProps={{ 'aria-label': 'trip-status-filter' }}
          >
            <MenuItem value="active">Active (picked up + out)</MenuItem>
            <MenuItem value="picked_up">Picked up</MenuItem>
            <MenuItem value="out_for_delivery">Out for delivery</MenuItem>
            <MenuItem value="delivered">Delivered</MenuItem>
            <MenuItem value="failed">Failed</MenuItem>
          </TextField>
          <Button
            size="small"
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Chip
            label={`${trips.length} trip${trips.length === 1 ? '' : 's'}`}
            sx={{ fontWeight: 700, bgcolor: '#ede7f6', color: '#4527a0' }}
          />
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={30} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && trips.length === 0 && (
          <Alert severity="info">
            No trips to show. Pick one up from the queue on the Delivery
            dashboard.
          </Alert>
        )}

        {!loading && !error && trips.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>Trip</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Order</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Address</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>OTP</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {trips.map((t) => {
                  const sc = statusColor(t.status);
                  const busy = busyId === t.id;
                  return (
                    <TableRow key={t.id} data-testid={`trip-row-${t.id}`}>
                      <TableCell sx={{ fontWeight: 700 }}>#{t.id}</TableCell>
                      <TableCell>#{t.orderId}</TableCell>
                      <TableCell>{t.customerName || '—'}</TableCell>
                      <TableCell sx={{ maxWidth: 220 }}>
                        <Typography variant="body2">
                          {t.shippingAddress || '—'}
                        </Typography>
                        {t.customerPhone && (
                          <Stack
                            direction="row"
                            spacing={0.5}
                            alignItems="center"
                            sx={{ color: '#1565c0', mt: 0.25 }}
                          >
                            <PhoneIcon sx={{ fontSize: 14 }} />
                            <Typography
                              component="a"
                              href={`tel:${t.customerPhone}`}
                              variant="caption"
                              sx={{ color: '#1565c0', textDecoration: 'none' }}
                              data-testid={`trip-call-${t.id}`}
                            >
                              {t.customerPhone}
                            </Typography>
                          </Stack>
                        )}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={(t.status || '').replace(/_/g, ' ')}
                          sx={{
                            fontWeight: 700,
                            bgcolor: sc.bg,
                            color: sc.fg,
                            textTransform: 'uppercase',
                          }}
                        />
                      </TableCell>
                      <TableCell sx={{ fontWeight: 700, color: '#5e35b1' }}>
                        {t.otpCode || '—'}
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={1} flexWrap="wrap">
                          {t.status === 'picked_up' && (
                            <Button
                              size="small"
                              startIcon={<DirectionsRunIcon />}
                              disabled={busy}
                              onClick={() => onMarkOut(t)}
                              data-testid={`trip-out-${t.id}`}
                              sx={{ fontWeight: 700, color: '#0d47a1' }}
                            >
                              Out for delivery
                            </Button>
                          )}
                          {t.status === 'out_for_delivery' && (
                            <Button
                              size="small"
                              startIcon={<DoneAllIcon />}
                              disabled={busy}
                              onClick={() => openDeliver(t)}
                              data-testid={`trip-deliver-${t.id}`}
                              sx={{ fontWeight: 700, color: '#2e7d32' }}
                            >
                              Delivered
                            </Button>
                          )}
                          {(t.status === 'picked_up' ||
                            t.status === 'out_for_delivery') && (
                            <Button
                              size="small"
                              startIcon={<CancelIcon />}
                              disabled={busy}
                              onClick={() => openFail(t)}
                              data-testid={`trip-fail-${t.id}`}
                              sx={{ fontWeight: 700, color: '#c62828' }}
                            >
                              Fail
                            </Button>
                          )}
                        </Stack>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

      <Dialog
        open={deliverDialog.open}
        onClose={() =>
          !deliverDialog.submitting &&
          setDeliverDialog((s) => ({ ...s, open: false }))
        }
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          Deliver trip #{deliverDialog.trip?.id}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Alert severity="info">
              Enter the OTP from the customer OR a photo proof URL.
            </Alert>
            <TextField
              label="Customer OTP"
              value={deliverDialog.otp}
              onChange={(e) =>
                setDeliverDialog((s) => ({ ...s, otp: e.target.value }))
              }
              inputProps={{ 'aria-label': 'deliver-otp' }}
            />
            <TextField
              label="Photo proof URL"
              value={deliverDialog.photoUrl}
              onChange={(e) =>
                setDeliverDialog((s) => ({ ...s, photoUrl: e.target.value }))
              }
              inputProps={{ 'aria-label': 'deliver-photo' }}
            />
            <Stack direction="row" spacing={1}>
              <TextField
                label="COD collected ($)"
                type="number"
                value={deliverDialog.codAmount}
                onChange={(e) =>
                  setDeliverDialog((s) => ({ ...s, codAmount: e.target.value }))
                }
                inputProps={{ 'aria-label': 'deliver-cod' }}
              />
              <TextField
                label="Tip ($)"
                type="number"
                value={deliverDialog.tipAmount}
                onChange={(e) =>
                  setDeliverDialog((s) => ({ ...s, tipAmount: e.target.value }))
                }
                inputProps={{ 'aria-label': 'deliver-tip' }}
              />
              <TextField
                label="Distance (km)"
                type="number"
                value={deliverDialog.distanceKm}
                onChange={(e) =>
                  setDeliverDialog((s) => ({
                    ...s,
                    distanceKm: e.target.value,
                  }))
                }
                inputProps={{ 'aria-label': 'deliver-distance' }}
              />
            </Stack>
            <TextField
              label="Notes (optional)"
              multiline
              minRows={2}
              value={deliverDialog.notes}
              onChange={(e) =>
                setDeliverDialog((s) => ({ ...s, notes: e.target.value }))
              }
              inputProps={{ 'aria-label': 'deliver-notes' }}
            />
            {deliverDialog.error && (
              <Alert severity="error">{deliverDialog.error}</Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setDeliverDialog((s) => ({ ...s, open: false }))}
            disabled={deliverDialog.submitting}
          >
            Back
          </Button>
          <Button
            onClick={submitDeliver}
            disabled={deliverDialog.submitting}
            variant="contained"
            color="success"
            sx={{ fontWeight: 700 }}
          >
            {deliverDialog.submitting ? 'Saving…' : 'Mark delivered'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={failDialog.open}
        onClose={() =>
          !failDialog.submitting &&
          setFailDialog((s) => ({ ...s, open: false }))
        }
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          Mark trip #{failDialog.trip?.id} as failed
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label="Reason"
              value={failDialog.reason}
              onChange={(e) =>
                setFailDialog((s) => ({ ...s, reason: e.target.value }))
              }
              inputProps={{ 'aria-label': 'fail-reason' }}
            >
              {FAILURE_REASONS.map((r) => (
                <MenuItem key={r.value} value={r.value}>
                  {r.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Notes"
              multiline
              minRows={2}
              value={failDialog.notes}
              onChange={(e) =>
                setFailDialog((s) => ({ ...s, notes: e.target.value }))
              }
              inputProps={{ 'aria-label': 'fail-notes' }}
            />
            {failDialog.error && (
              <Alert severity="error">{failDialog.error}</Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setFailDialog((s) => ({ ...s, open: false }))}
            disabled={failDialog.submitting}
          >
            Back
          </Button>
          <Button
            onClick={submitFail}
            disabled={failDialog.submitting}
            variant="contained"
            color="error"
            sx={{ fontWeight: 700 }}
          >
            {failDialog.submitting ? 'Saving…' : 'Mark failed'}
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
