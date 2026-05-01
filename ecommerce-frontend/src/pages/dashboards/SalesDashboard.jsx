import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Stack,
  Box,
  Grid,
  Alert,
  CircularProgress,
  Button,
  TextField,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Divider,
} from '@mui/material';
import TimelineIcon from '@mui/icons-material/Timeline';
import RefreshIcon from '@mui/icons-material/Refresh';
import AssignmentIcon from '@mui/icons-material/Assignment';
import { Link as RouterLink } from 'react-router-dom';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import { fetchSalesAnalytics } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';
import EmployeeQuickTools from './EmployeeQuickTools.jsx';

function fmtMoney(n) {
  const num = Number(n) || 0;
  return num.toFixed(2);
}

function isoDateNDaysAgo(n) {
  const d = new Date();
  d.setDate(d.getDate() - n);
  return d.toISOString().slice(0, 10);
}

function isoToday() {
  return new Date().toISOString().slice(0, 10);
}

function KpiCard({ label, value, accent, testId }) {
  return (
    <Paper
      elevation={0}
      data-testid={testId}
      sx={{
        p: 2,
        borderRadius: 2,
        bgcolor: 'rgba(255,255,255,0.85)',
        border: '1px solid rgba(0,0,0,0.06)',
        height: '100%',
      }}
    >
      <Typography variant="caption" sx={{ fontWeight: 700, color: '#6d4c41', textTransform: 'uppercase' }}>
        {label}
      </Typography>
      <Typography variant="h4" sx={{ fontWeight: 800, color: accent || '#2e7d32' }}>
        {value}
      </Typography>
    </Paper>
  );
}

function BarRow({ label, value, max, color }) {
  const safeMax = Number(max) > 0 ? Number(max) : 1;
  const pct = Math.min(100, Math.max(0, (Number(value) / safeMax) * 100));
  return (
    <Stack direction="row" spacing={1.5} alignItems="center" sx={{ my: 0.75 }}>
      <Box sx={{ width: 140, fontWeight: 700, color: '#4e342e', fontSize: 13 }}>{label}</Box>
      <Box sx={{ flex: 1, bgcolor: 'rgba(0,0,0,0.06)', borderRadius: 1, height: 18, position: 'relative' }}>
        <Box
          data-testid={`bar-${label}`}
          sx={{
            width: `${pct}%`,
            height: '100%',
            bgcolor: color || '#66bb6a',
            borderRadius: 1,
            transition: 'width 0.3s',
          }}
        />
      </Box>
      <Box sx={{ minWidth: 90, textAlign: 'right', fontWeight: 700, color: '#2e7d32' }}>
        ${fmtMoney(value)}
      </Box>
    </Stack>
  );
}

export default function SalesDashboard() {
  const user = useSelector(selectCurrentUser);
  const [from, setFrom] = useState(isoDateNDaysAgo(29));
  const [to, setTo] = useState(isoToday());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(
    (fromArg, toArg) => {
      setLoading(true);
      setError('');
      fetchSalesAnalytics(fromArg, toArg)
        .then((d) => setData(d))
        .catch((err) => {
          const msg =
            err.response?.data?.error || err.message || 'Failed to load sales analytics.';
          setError(msg);
          setData(null);
        })
        .finally(() => setLoading(false));
    },
    []
  );

  useEffect(() => {
    load(from, to);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onApply = () => {
    if (from && to && from > to) {
      setError("'From' date must be on or before 'To' date.");
      return;
    }
    load(from, to);
  };

  const channelEntries = useMemo(
    () => Object.entries(data?.revenueByChannel || {}),
    [data]
  );
  const paymentEntries = useMemo(
    () => Object.entries(data?.revenueByPaymentMethod || {}),
    [data]
  );
  const statusEntries = useMemo(
    () => Object.entries(data?.ordersByStatus || {}),
    [data]
  );
  const channelMax = useMemo(
    () => channelEntries.reduce((m, [, v]) => Math.max(m, Number(v) || 0), 0),
    [channelEntries]
  );
  const paymentMax = useMemo(
    () => paymentEntries.reduce((m, [, v]) => Math.max(m, Number(v) || 0), 0),
    [paymentEntries]
  );
  const trendMax = useMemo(
    () =>
      (data?.dailyTrend || []).reduce(
        (m, row) => Math.max(m, Number(row.revenue) || 0),
        0
      ),
    [data]
  );

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <TimelineIcon sx={{ color: '#5e35b1', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1, ...RAINBOW_TEXT }}
          >
            Welcome to Sales, {user?.name || 'Teammate'}
          </Typography>
        </Stack>
        <Typography sx={{ mb: 2, color: '#6d4c41', fontWeight: 600 }}>
          Daily revenue, top products, channel and payment-method splits.
        </Typography>

        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3, flexWrap: 'wrap' }}>
          <TextField
            label="From"
            type="date"
            size="small"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'from-date' }}
          />
          <TextField
            label="To"
            type="date"
            size="small"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'to-date' }}
          />
          <Button onClick={onApply} disabled={loading} sx={RAINBOW_FILLED_BTN}>
            Apply
          </Button>
          <Button
            onClick={() => load(from, to)}
            disabled={loading}
            startIcon={<RefreshIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/sales/tasks"
            startIcon={<AssignmentIcon />}
            sx={RAINBOW_FILLED_BTN}
          >
            My Tasks
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        {!loading && data && (
          <>
            <Grid container spacing={2} sx={{ mb: 3 }}>
              <Grid item xs={12} sm={4}>
                <KpiCard
                  label="Total revenue"
                  value={`$${fmtMoney(data.totalRevenue)}`}
                  accent="#2e7d32"
                  testId="kpi-revenue"
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <KpiCard
                  label="Orders"
                  value={data.orderCount}
                  accent="#1565c0"
                  testId="kpi-orders"
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <KpiCard
                  label="Avg order value"
                  value={`$${fmtMoney(data.avgOrderValue)}`}
                  accent="#6a1b9a"
                  testId="kpi-aov"
                />
              </Grid>
            </Grid>

            <Grid container spacing={3} sx={{ mb: 3 }}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, mb: 1, color: '#4e342e' }}>
                  Revenue by channel
                </Typography>
                {channelEntries.length === 0 && (
                  <Alert severity="info" data-testid="no-channel-data">
                    No channel data for this range.
                  </Alert>
                )}
                {channelEntries.map(([k, v]) => (
                  <BarRow key={k} label={k} value={v} max={channelMax} color="#42a5f5" />
                ))}
              </Grid>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, mb: 1, color: '#4e342e' }}>
                  Revenue by payment method
                </Typography>
                {paymentEntries.length === 0 && (
                  <Alert severity="info" data-testid="no-payment-data">
                    No payment data for this range.
                  </Alert>
                )}
                {paymentEntries.map(([k, v]) => (
                  <BarRow key={k} label={k} value={v} max={paymentMax} color="#ab47bc" />
                ))}
              </Grid>
            </Grid>

            <Divider sx={{ my: 2 }} />

            <Grid container spacing={3}>
              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, mb: 1, color: '#4e342e' }}>
                  Top products
                </Typography>
                {(!data.topProducts || data.topProducts.length === 0) && (
                  <Alert severity="info">No products sold in this range.</Alert>
                )}
                {data.topProducts && data.topProducts.length > 0 && (
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell sx={{ fontWeight: 800 }}>Product</TableCell>
                        <TableCell sx={{ fontWeight: 800 }} align="right">Qty</TableCell>
                        <TableCell sx={{ fontWeight: 800 }} align="right">Revenue</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {data.topProducts.map((p) => (
                        <TableRow key={p.productId}>
                          <TableCell sx={{ fontWeight: 600 }}>{p.name}</TableCell>
                          <TableCell align="right">{p.quantitySold}</TableCell>
                          <TableCell align="right" sx={{ color: '#2e7d32', fontWeight: 700 }}>
                            ${fmtMoney(p.revenue)}
                          </TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                )}
              </Grid>

              <Grid item xs={12} md={6}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, mb: 1, color: '#4e342e' }}>
                  Orders by status
                </Typography>
                <Stack direction="row" spacing={1} sx={{ flexWrap: 'wrap' }}>
                  {statusEntries.length === 0 && (
                    <Chip label="No orders" sx={{ fontWeight: 700 }} />
                  )}
                  {statusEntries.map(([k, v]) => (
                    <Chip
                      key={k}
                      label={`${k}: ${v}`}
                      sx={{
                        fontWeight: 700,
                        bgcolor: k === 'CONFIRMED' ? '#e8f5e9' : '#ffebee',
                        color: k === 'CONFIRMED' ? '#2e7d32' : '#c62828',
                      }}
                    />
                  ))}
                </Stack>
              </Grid>
            </Grid>

            <Divider sx={{ my: 3 }} />

            <Typography variant="subtitle1" sx={{ fontWeight: 800, mb: 1, color: '#4e342e' }}>
              Daily trend
            </Typography>
            <Stack direction="row" spacing={0.5} alignItems="flex-end" sx={{ height: 120, overflowX: 'auto' }}>
              {(data.dailyTrend || []).map((row) => {
                const pct = trendMax > 0 ? (Number(row.revenue) / trendMax) * 100 : 0;
                return (
                  <Box
                    key={row.date}
                    data-testid={`trend-${row.date}`}
                    title={`${row.date}: $${fmtMoney(row.revenue)} (${row.orderCount})`}
                    sx={{
                      width: 22,
                      minWidth: 22,
                      height: `${Math.max(pct, 2)}%`,
                      bgcolor: pct > 0 ? '#66bb6a' : 'rgba(0,0,0,0.08)',
                      borderRadius: '4px 4px 0 0',
                    }}
                  />
                );
              })}
            </Stack>
            <Typography variant="caption" sx={{ color: '#6d4c41', fontWeight: 600 }}>
              Hover a bar to see date / revenue / orders.
            </Typography>
          </>
        )}
      </Paper>
      <EmployeeQuickTools />
    </Box>
  );
}
