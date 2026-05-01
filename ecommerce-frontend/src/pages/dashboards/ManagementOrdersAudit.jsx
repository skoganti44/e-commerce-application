import { useCallback, useEffect, useState } from 'react';
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
  Grid,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  TextField,
  MenuItem,
} from '@mui/material';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { fetchManagementOrdersAudit } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function ManagementOrdersAudit() {
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [channel, setChannel] = useState('all');
  const [paymentMethod, setPaymentMethod] = useState('all');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchManagementOrdersAudit({
      from,
      to,
      channel: channel === 'all' ? undefined : channel,
      paymentMethod: paymentMethod === 'all' ? undefined : paymentMethod,
    })
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [from, to, channel, paymentMethod]);

  useEffect(() => {
    load();
  }, [load]);

  const orders = data?.orders || [];
  const byChannel = data?.revenueByChannel || {};
  const byPm = data?.revenueByPaymentMethod || {};

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <ReceiptLongIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Orders Audit
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
            label="From"
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'audit-from' }}
          />
          <TextField
            label="To"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'audit-to' }}
          />
          <TextField
            select
            label="Channel"
            value={channel}
            onChange={(e) => setChannel(e.target.value)}
            sx={{ minWidth: 150 }}
            inputProps={{ 'aria-label': 'audit-channel' }}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="online">Online</MenuItem>
            <MenuItem value="instore">In-store</MenuItem>
          </TextField>
          <TextField
            select
            label="Payment"
            value={paymentMethod}
            onChange={(e) => setPaymentMethod(e.target.value)}
            sx={{ minWidth: 150 }}
            inputProps={{ 'aria-label': 'audit-payment' }}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="cash">Cash</MenuItem>
            <MenuItem value="card">Card</MenuItem>
            <MenuItem value="upi">UPI</MenuItem>
          </TextField>
          <Button
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Apply
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && data && (
          <>
            <Grid container spacing={2} sx={{ mb: 2 }}>
              <Grid item xs={6} sm={3}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#ede7f6', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    Orders
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>
                    {data.count}
                  </Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#c8e6c9', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    Revenue
                  </Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>
                    ${Number(data.totalRevenue || 0).toFixed(2)}
                  </Typography>
                </Paper>
              </Grid>
              <Grid item xs={12} sm={6}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#e3f2fd' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    By payment method
                  </Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 0.5 }}>
                    {Object.entries(byPm).map(([m, v]) => (
                      <Chip
                        key={m}
                        label={`${m} · $${Number(v || 0).toFixed(2)}`}
                        data-testid={`pm-${m}`}
                        sx={{ fontWeight: 700, bgcolor: '#fff', mb: 1 }}
                      />
                    ))}
                  </Stack>
                </Paper>
              </Grid>
              <Grid item xs={12}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#fff3e0' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    By channel
                  </Typography>
                  <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mt: 0.5 }}>
                    {Object.entries(byChannel).map(([c, v]) => (
                      <Chip
                        key={c}
                        label={`${c} · $${Number(v || 0).toFixed(2)}`}
                        data-testid={`ch-${c}`}
                        sx={{ fontWeight: 700, bgcolor: '#fff', mb: 1 }}
                      />
                    ))}
                  </Stack>
                </Paper>
              </Grid>
            </Grid>

            {orders.length === 0 ? (
              <Alert severity="info">No orders match the filter.</Alert>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Channel</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Payment</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Total</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Created</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {orders.map((o) => (
                      <TableRow key={o.orderId} data-testid={`order-row-${o.orderId}`}>
                        <TableCell sx={{ fontWeight: 700 }}>#{o.orderId}</TableCell>
                        <TableCell>{o.customerName || '—'}</TableCell>
                        <TableCell>{o.channel}</TableCell>
                        <TableCell>{o.status}</TableCell>
                        <TableCell>{o.paymentMethod}</TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>
                          ${Number(o.totalAmount || 0).toFixed(2)}
                        </TableCell>
                        <TableCell sx={{ color: '#6d4c41' }}>
                          {o.createdAt ? o.createdAt.slice(0, 16).replace('T', ' ') : '—'}
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}
          </>
        )}
      </Paper>
    </Box>
  );
}
