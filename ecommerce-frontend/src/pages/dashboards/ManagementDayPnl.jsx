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
  TextField,
} from '@mui/material';
import AccountBalanceIcon from '@mui/icons-material/AccountBalance';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { fetchManagementDayPnl } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function StatTile({ label, value, color, big }) {
  return (
    <Paper
      elevation={0}
      sx={{
        p: 2,
        borderRadius: 3,
        textAlign: 'center',
        bgcolor: color || '#ede7f6',
      }}
    >
      <Typography variant="caption" sx={{ color: '#455a64', fontWeight: 700 }}>
        {label}
      </Typography>
      <Typography
        variant={big ? 'h4' : 'h5'}
        sx={{ fontWeight: 800, mt: 0.5 }}
      >
        {value}
      </Typography>
    </Paper>
  );
}

export default function ManagementDayPnl() {
  const [date, setDate] = useState(todayIso());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchManagementDayPnl(date)
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [date]);

  useEffect(() => {
    load();
  }, [load]);

  const byPm = data?.revenueByPaymentMethod || {};

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <AccountBalanceIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Day P&amp;L
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

        <Stack direction="row" spacing={2} sx={{ mb: 3 }}>
          <TextField
            label="Date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'pnl-date' }}
          />
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
              <Grid item xs={12} sm={6}>
                <StatTile
                  big
                  label="Total revenue"
                  value={`$${Number(data.totalRevenue || 0).toFixed(2)}`}
                  color="#c8e6c9"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label="Orders"
                  value={data.orderCount}
                  color="#ede7f6"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  big
                  label="Net inflow"
                  value={`$${Number(data.net || 0).toFixed(2)}`}
                  color="#fff9c4"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label="Online revenue"
                  value={`$${Number(data.onlineRevenue || 0).toFixed(2)}`}
                  color="#bbdefb"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label="Counter revenue"
                  value={`$${Number(data.counterRevenue || 0).toFixed(2)}`}
                  color="#fff3e0"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label="COD collected"
                  value={`$${Number(data.codCollected || 0).toFixed(2)}`}
                  color="#fff9c4"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label="Tips"
                  value={`$${Number(data.tipsCollected || 0).toFixed(2)}`}
                  color="#fff9c4"
                />
              </Grid>
            </Grid>

            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Revenue by payment method
            </Typography>
            <Stack direction="row" spacing={1} flexWrap="wrap" sx={{ mb: 3 }}>
              {Object.entries(byPm).map(([m, v]) => (
                <Chip
                  key={m}
                  label={`${m} · $${Number(v || 0).toFixed(2)}`}
                  data-testid={`pnl-pm-${m}`}
                  sx={{
                    fontWeight: 700,
                    bgcolor: '#e3f2fd',
                    color: '#1565c0',
                    mb: 1,
                  }}
                />
              ))}
            </Stack>

            <Alert severity="info" sx={{ mt: 2 }}>
              Supplier spend and refunds are tracked in upcoming releases —
              shown as $0 today.
            </Alert>
          </>
        )}
      </Paper>
    </Box>
  );
}
