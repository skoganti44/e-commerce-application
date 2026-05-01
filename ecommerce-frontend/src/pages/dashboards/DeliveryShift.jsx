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
  Grid,
  TextField,
  Chip,
} from '@mui/material';
import AssessmentIcon from '@mui/icons-material/Assessment';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import { fetchDeliveryShiftSummary } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function StatCard({ label, value, color }) {
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
      <Typography variant="h5" sx={{ fontWeight: 800, mt: 0.5 }}>
        {value}
      </Typography>
    </Paper>
  );
}

export default function DeliveryShift() {
  const user = useSelector(selectCurrentUser);
  const driverId = user?.userid;
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    if (!driverId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    fetchDeliveryShiftSummary(driverId, from, to)
      .then((data) => setSummary(data))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load summary.')
      )
      .finally(() => setLoading(false));
  }, [driverId, from, to]);

  useEffect(() => {
    load();
  }, [load]);

  const reasons = (summary && summary.failuresByReason) || {};

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <AssessmentIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Shift Summary
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

        <Stack direction="row" spacing={2} sx={{ mb: 3, flexWrap: 'wrap' }}>
          <TextField
            label="From"
            type="date"
            value={from}
            onChange={(e) => setFrom(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'shift-from' }}
          />
          <TextField
            label="To"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'shift-to' }}
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

        {!loading && !error && summary && (
          <>
            <Grid container spacing={2}>
              <Grid item xs={6} sm={3}>
                <StatCard
                  label="Total trips"
                  value={summary.totalTrips}
                  color="#ede7f6"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatCard
                  label="Delivered"
                  value={summary.delivered}
                  color="#c8e6c9"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatCard
                  label="Failed"
                  value={summary.failed}
                  color="#ffcdd2"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatCard
                  label="In flight"
                  value={summary.inFlight}
                  color="#bbdefb"
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <StatCard
                  label="COD collected"
                  value={`$${Number(summary.codCollected || 0).toFixed(2)}`}
                  color="#fff9c4"
                />
              </Grid>
              <Grid item xs={6} sm={4}>
                <StatCard
                  label="Tips"
                  value={`$${Number(summary.tipsTotal || 0).toFixed(2)}`}
                  color="#fff9c4"
                />
              </Grid>
              <Grid item xs={12} sm={4}>
                <StatCard
                  label="Distance (km)"
                  value={Number(summary.distanceKm || 0).toFixed(1)}
                  color="#e3f2fd"
                />
              </Grid>
            </Grid>

            <Box sx={{ mt: 3 }}>
              <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
                Failures by reason
              </Typography>
              {Object.keys(reasons).length === 0 ? (
                <Alert severity="success">
                  No failed trips in this range — well done.
                </Alert>
              ) : (
                <Stack direction="row" spacing={1} flexWrap="wrap">
                  {Object.entries(reasons).map(([reason, n]) => (
                    <Chip
                      key={reason}
                      label={`${reason.replace(/_/g, ' ')} · ${n}`}
                      data-testid={`reason-${reason}`}
                      sx={{
                        fontWeight: 700,
                        bgcolor: '#fff3e0',
                        color: '#e65100',
                        textTransform: 'capitalize',
                        mb: 1,
                      }}
                    />
                  ))}
                </Stack>
              )}
            </Box>
          </>
        )}
      </Paper>
    </Box>
  );
}
