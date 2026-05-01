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
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { fetchManagementDeliveriesAudit } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function ManagementDeliveriesAudit() {
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [driverId, setDriverId] = useState('');
  const [status, setStatus] = useState('all');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchManagementDeliveriesAudit({
      from,
      to,
      driverId: driverId ? Number(driverId) : undefined,
      status: status === 'all' ? undefined : status,
    })
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [from, to, driverId, status]);

  useEffect(() => {
    load();
  }, [load]);

  const trips = data?.trips || [];

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
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
            Deliveries Audit
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
            inputProps={{ 'aria-label': 'dlv-from' }}
          />
          <TextField
            label="To"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'dlv-to' }}
          />
          <TextField
            label="Driver # (optional)"
            type="number"
            value={driverId}
            onChange={(e) => setDriverId(e.target.value)}
            sx={{ minWidth: 150 }}
            inputProps={{ 'aria-label': 'dlv-driver' }}
          />
          <TextField
            select
            label="Status"
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            sx={{ minWidth: 180 }}
            inputProps={{ 'aria-label': 'dlv-status' }}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="picked_up">Picked up</MenuItem>
            <MenuItem value="out_for_delivery">Out for delivery</MenuItem>
            <MenuItem value="delivered">Delivered</MenuItem>
            <MenuItem value="failed">Failed</MenuItem>
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
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>Total</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>{data.count}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#c8e6c9', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>Delivered</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>{data.delivered}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#ffcdd2', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>Failed</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>{data.failed}</Typography>
                </Paper>
              </Grid>
              <Grid item xs={6} sm={3}>
                <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#fff9c4', textAlign: 'center' }}>
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>COD + Tips</Typography>
                  <Typography variant="h5" sx={{ fontWeight: 800 }}>
                    ${(Number(data.codCollected || 0) + Number(data.tipsTotal || 0)).toFixed(2)}
                  </Typography>
                </Paper>
              </Grid>
            </Grid>

            {trips.length === 0 ? (
              <Alert severity="info">No trips match the filter.</Alert>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 800 }}>Trip</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Order</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Driver</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Customer</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>COD</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Tip</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Reason</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {trips.map((t) => (
                      <TableRow key={t.id} data-testid={`audit-trip-${t.id}`}>
                        <TableCell sx={{ fontWeight: 700 }}>#{t.id}</TableCell>
                        <TableCell>#{t.orderId}</TableCell>
                        <TableCell>{t.driverName || '—'}</TableCell>
                        <TableCell>{t.customerName || '—'}</TableCell>
                        <TableCell>
                          <Chip
                            size="small"
                            label={(t.status || '').replace(/_/g, ' ')}
                            sx={{
                              fontWeight: 700,
                              bgcolor: t.status === 'delivered' ? '#c8e6c9'
                                : t.status === 'failed' ? '#ffcdd2'
                                : '#fff3e0',
                              textTransform: 'uppercase',
                            }}
                          />
                        </TableCell>
                        <TableCell>
                          {t.codAmount ? `$${Number(t.codAmount).toFixed(2)}` : '—'}
                        </TableCell>
                        <TableCell>
                          {t.tipAmount ? `$${Number(t.tipAmount).toFixed(2)}` : '—'}
                        </TableCell>
                        <TableCell>{t.failureReason || '—'}</TableCell>
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
