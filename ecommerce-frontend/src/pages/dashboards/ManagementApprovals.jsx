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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
} from '@mui/material';
import VerifiedIcon from '@mui/icons-material/Verified';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import FlagIcon from '@mui/icons-material/Flag';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchOrdersPendingApproval,
  flagOrderForApproval,
  decideOrderApproval,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

export default function ManagementApprovals() {
  const user = useSelector(selectCurrentUser);
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const [flag, setFlag] = useState({
    open: false,
    orderId: '',
    notes: '',
    submitting: false,
    error: '',
  });

  const [decide, setDecide] = useState({
    open: false,
    order: null,
    nextStatus: 'approved',
    notes: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchOrdersPendingApproval()
      .then((d) => setOrders(Array.isArray(d) ? d : []))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const submitFlag = async () => {
    if (!flag.orderId) {
      setFlag((s) => ({ ...s, error: 'Order # is required' }));
      return;
    }
    setFlag((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await flagOrderForApproval(Number(flag.orderId), flag.notes.trim());
      setFlag({
        open: false,
        orderId: '',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Order #${flag.orderId} flagged for approval.`);
      load();
    } catch (err) {
      setFlag((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not flag.',
      }));
    }
  };

  const openDecide = (order, nextStatus) => {
    setDecide({
      open: true,
      order,
      nextStatus,
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitDecide = async () => {
    const { order, nextStatus, notes } = decide;
    if (!order) return;
    setDecide((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await decideOrderApproval(order.orderId, {
        managerUserId: user?.userid,
        decision: nextStatus,
        notes: notes.trim() || null,
      });
      setDecide({
        open: false,
        order: null,
        nextStatus: 'approved',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Order #${order.orderId} ${nextStatus}.`);
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
          <VerifiedIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Order Approvals
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
          <Button
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Button
            startIcon={<FlagIcon />}
            onClick={() => setFlag((s) => ({ ...s, open: true, error: '' }))}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            Flag corporate order
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && orders.length === 0 && (
          <Alert severity="success">No orders awaiting sign-off.</Alert>
        )}

        {!loading && !error && orders.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Channel</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Total</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Notes</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {orders.map((o) => (
                  <TableRow key={o.orderId} data-testid={`approval-row-${o.orderId}`}>
                    <TableCell sx={{ fontWeight: 700 }}>#{o.orderId}</TableCell>
                    <TableCell>{o.customerName || '—'}</TableCell>
                    <TableCell>{o.channel}</TableCell>
                    <TableCell sx={{ fontWeight: 700 }}>
                      ${Number(o.totalAmount || 0).toFixed(2)}
                    </TableCell>
                    <TableCell sx={{ maxWidth: 280 }}>
                      <Typography variant="body2">{o.approvalNotes || '—'}</Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={o.approvalStatus || 'pending'}
                        sx={{
                          fontWeight: 700,
                          bgcolor: '#fff3e0',
                          color: '#e65100',
                          textTransform: 'uppercase',
                        }}
                      />
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={0.5}>
                        <Button
                          size="small"
                          startIcon={<CheckCircleIcon />}
                          onClick={() => openDecide(o, 'approved')}
                          data-testid={`approval-approve-${o.orderId}`}
                          sx={{ fontWeight: 700, color: '#2e7d32' }}
                        >
                          Approve
                        </Button>
                        <Button
                          size="small"
                          startIcon={<CancelIcon />}
                          onClick={() => openDecide(o, 'rejected')}
                          data-testid={`approval-reject-${o.orderId}`}
                          sx={{ fontWeight: 700, color: '#c62828' }}
                        >
                          Reject
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

      <Dialog
        open={flag.open}
        onClose={() => !flag.submitting && setFlag((s) => ({ ...s, open: false }))}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>Flag order for approval</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Order #"
              type="number"
              value={flag.orderId}
              onChange={(e) => setFlag((s) => ({ ...s, orderId: e.target.value }))}
              inputProps={{ 'aria-label': 'flag-order' }}
            />
            <TextField
              label="Notes for the manager"
              multiline
              minRows={2}
              value={flag.notes}
              onChange={(e) => setFlag((s) => ({ ...s, notes: e.target.value }))}
              inputProps={{ 'aria-label': 'flag-notes' }}
            />
            {flag.error && <Alert severity="error">{flag.error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setFlag((s) => ({ ...s, open: false }))}
            disabled={flag.submitting}
          >
            Cancel
          </Button>
          <Button
            onClick={submitFlag}
            disabled={flag.submitting}
            variant="contained"
            sx={{ fontWeight: 700 }}
          >
            {flag.submitting ? 'Saving…' : 'Flag'}
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
          {decide.nextStatus === 'approved' ? 'Approve' : 'Reject'} order #{decide.order?.orderId}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography sx={{ fontWeight: 600 }}>
              {decide.order?.customerName || '—'} · ${Number(decide.order?.totalAmount || 0).toFixed(2)}
            </Typography>
            <TextField
              label="Decision notes"
              multiline
              minRows={2}
              value={decide.notes}
              onChange={(e) => setDecide((s) => ({ ...s, notes: e.target.value }))}
              inputProps={{ 'aria-label': 'approval-decide-notes' }}
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
