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
  TextField,
  MenuItem,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
} from '@mui/material';
import GavelIcon from '@mui/icons-material/Gavel';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import AddIcon from '@mui/icons-material/Add';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchRefundRequests,
  raiseRefundRequest,
  decideRefundRequest,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const TYPES = [
  { value: 'refund', label: 'Refund' },
  { value: 'cancellation', label: 'Cancellation' },
  { value: 'damage_writeoff', label: 'Damage write-off' },
];

function statusColor(s) {
  switch ((s || '').toLowerCase()) {
    case 'approved': return { bg: '#c8e6c9', fg: '#1b5e20' };
    case 'rejected': return { bg: '#ffcdd2', fg: '#b71c1c' };
    default:         return { bg: '#fff3e0', fg: '#e65100' };
  }
}

export default function ManagementRefunds() {
  const user = useSelector(selectCurrentUser);
  const [filter, setFilter] = useState('pending');
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const [raise, setRaise] = useState({
    open: false,
    orderId: '',
    requestType: 'refund',
    reason: '',
    amount: '',
    submitting: false,
    error: '',
  });

  const [decide, setDecide] = useState({
    open: false,
    request: null,
    nextStatus: 'approved',
    notes: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchRefundRequests(filter)
      .then((d) => setRequests(Array.isArray(d) ? d : []))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  const submitRaise = async () => {
    if (!raise.orderId) {
      setRaise((s) => ({ ...s, error: 'Order # is required' }));
      return;
    }
    if (!raise.reason.trim()) {
      setRaise((s) => ({ ...s, error: 'Reason is required' }));
      return;
    }
    setRaise((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await raiseRefundRequest({
        orderId: Number(raise.orderId),
        raisedByUserId: user?.userid,
        requestType: raise.requestType,
        reason: raise.reason.trim(),
        amount: raise.amount ? Number(raise.amount) : 0,
      });
      setRaise({
        open: false,
        orderId: '',
        requestType: 'refund',
        reason: '',
        amount: '',
        submitting: false,
        error: '',
      });
      setToast('Refund request raised.');
      load();
    } catch (err) {
      setRaise((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not raise.',
      }));
    }
  };

  const openDecide = (request, nextStatus) => {
    setDecide({
      open: true,
      request,
      nextStatus,
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitDecide = async () => {
    const { request, nextStatus, notes } = decide;
    if (!request) return;
    setDecide((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await decideRefundRequest(request.id, {
        managerUserId: user?.userid,
        decision: nextStatus,
        notes: notes.trim() || null,
      });
      setDecide({
        open: false,
        request: null,
        nextStatus: 'approved',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Request #${request.id} ${nextStatus}.`);
      load();
    } catch (err) {
      setDecide((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not save.',
      }));
    }
  };

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <GavelIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Refunds &amp; Cancellations
          </Typography>
          <Button
            component={RouterLink}
            to="/dashboard/management"
            size="small"
            startIcon={<ArrowBackIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Back
          </Button>
        </Stack>

        <Stack direction="row" spacing={2} sx={{ mb: 3, flexWrap: 'wrap' }}>
          <TextField
            select
            label="Status"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            sx={{ minWidth: 160 }}
            inputProps={{ 'aria-label': 'refund-filter' }}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="pending">Pending</MenuItem>
            <MenuItem value="approved">Approved</MenuItem>
            <MenuItem value="rejected">Rejected</MenuItem>
          </TextField>
          <Button
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Button
            startIcon={<AddIcon />}
            onClick={() => setRaise((s) => ({ ...s, open: true, error: '' }))}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            Raise request
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && requests.length === 0 && (
          <Alert severity="info">No requests in this view.</Alert>
        )}

        {!loading && !error && requests.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Order</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Reason</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Amount</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Raised by</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {requests.map((r) => {
                  const sc = statusColor(r.status);
                  return (
                    <TableRow key={r.id} data-testid={`refund-row-${r.id}`}>
                      <TableCell sx={{ fontWeight: 700 }}>#{r.id}</TableCell>
                      <TableCell>#{r.orderId}</TableCell>
                      <TableCell>{r.customerName || '—'}</TableCell>
                      <TableCell>{(r.requestType || '').replace(/_/g, ' ')}</TableCell>
                      <TableCell sx={{ maxWidth: 260 }}>
                        <Typography variant="body2">{r.reason}</Typography>
                      </TableCell>
                      <TableCell sx={{ fontWeight: 700 }}>
                        ${Number(r.amount || 0).toFixed(2)}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={r.status}
                          sx={{
                            fontWeight: 700,
                            bgcolor: sc.bg,
                            color: sc.fg,
                            textTransform: 'uppercase',
                          }}
                        />
                      </TableCell>
                      <TableCell>{r.raisedByName}</TableCell>
                      <TableCell>
                        {r.status === 'pending' ? (
                          <Stack direction="row" spacing={0.5}>
                            <Button
                              size="small"
                              startIcon={<CheckCircleIcon />}
                              onClick={() => openDecide(r, 'approved')}
                              data-testid={`refund-approve-${r.id}`}
                              sx={{ fontWeight: 700, color: '#2e7d32' }}
                            >
                              Approve
                            </Button>
                            <Button
                              size="small"
                              startIcon={<CancelIcon />}
                              onClick={() => openDecide(r, 'rejected')}
                              data-testid={`refund-reject-${r.id}`}
                              sx={{ fontWeight: 700, color: '#c62828' }}
                            >
                              Reject
                            </Button>
                          </Stack>
                        ) : (
                          <Typography variant="caption" sx={{ color: '#6d4c41' }}>
                            by {r.decidedByName || '—'}
                          </Typography>
                        )}
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
        open={raise.open}
        onClose={() => !raise.submitting && setRaise((s) => ({ ...s, open: false }))}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>Raise refund / cancellation</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Order #"
              type="number"
              value={raise.orderId}
              onChange={(e) => setRaise((s) => ({ ...s, orderId: e.target.value }))}
              inputProps={{ 'aria-label': 'raise-order' }}
            />
            <TextField
              select
              label="Type"
              value={raise.requestType}
              onChange={(e) => setRaise((s) => ({ ...s, requestType: e.target.value }))}
              inputProps={{ 'aria-label': 'raise-type' }}
            >
              {TYPES.map((t) => (
                <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Reason"
              multiline
              minRows={2}
              value={raise.reason}
              onChange={(e) => setRaise((s) => ({ ...s, reason: e.target.value }))}
              inputProps={{ 'aria-label': 'raise-reason', maxLength: 500 }}
            />
            <TextField
              label="Amount ($)"
              type="number"
              value={raise.amount}
              onChange={(e) => setRaise((s) => ({ ...s, amount: e.target.value }))}
              inputProps={{ 'aria-label': 'raise-amount' }}
            />
            {raise.error && <Alert severity="error">{raise.error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setRaise((s) => ({ ...s, open: false }))}
            disabled={raise.submitting}
          >
            Cancel
          </Button>
          <Button
            onClick={submitRaise}
            disabled={raise.submitting}
            variant="contained"
            sx={{ fontWeight: 700 }}
          >
            {raise.submitting ? 'Saving…' : 'Submit'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={decide.open}
        onClose={() => !decide.submitting && setDecide((s) => ({ ...s, open: false }))}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          {decide.nextStatus === 'approved' ? 'Approve' : 'Reject'} request #{decide.request?.id}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography sx={{ fontWeight: 600 }}>
              {decide.request?.requestType} · ${Number(decide.request?.amount || 0).toFixed(2)} · {decide.request?.reason}
            </Typography>
            <TextField
              label="Decision notes (optional)"
              multiline
              minRows={2}
              value={decide.notes}
              onChange={(e) => setDecide((s) => ({ ...s, notes: e.target.value }))}
              inputProps={{ 'aria-label': 'decide-notes' }}
            />
            {decide.error && <Alert severity="error">{decide.error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setDecide((s) => ({ ...s, open: false }))}
            disabled={decide.submitting}
          >
            Back
          </Button>
          <Button
            onClick={submitDecide}
            disabled={decide.submitting}
            variant="contained"
            color={decide.nextStatus === 'approved' ? 'success' : 'error'}
            sx={{ fontWeight: 700 }}
          >
            {decide.submitting ? 'Saving…' : (decide.nextStatus === 'approved' ? 'Approve' : 'Reject')}
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
