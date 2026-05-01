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
} from '@mui/material';
import DashboardIcon from '@mui/icons-material/Dashboard';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { fetchManagementOps } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function StatTile({ label, value, color }) {
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

export default function ManagementLiveOps() {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchManagementOps()
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load ops view.')
      )
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const kitchen = data?.kitchenQueue || {};
  const dlv = data?.deliveryInFlight || {};
  const breaches = data?.breaches || [];

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <DashboardIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Live Ops
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
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && data && (
          <>
            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Kitchen pipeline
            </Typography>
            <Grid container spacing={1.5} sx={{ mb: 2 }}>
              {Object.entries(kitchen).map(([channel, counts]) => (
                <Grid key={channel} item xs={12} md={6}>
                  <Paper
                    elevation={0}
                    sx={{ p: 2, borderRadius: 3, bgcolor: '#fff8e1' }}
                  >
                    <Typography sx={{ fontWeight: 800, mb: 1, textTransform: 'uppercase' }}>
                      {channel}
                    </Typography>
                    <Stack direction="row" spacing={1} flexWrap="wrap">
                      {Object.entries(counts).map(([status, n]) => (
                        <Chip
                          key={status}
                          label={`${status.replace(/_/g, ' ')} · ${n}`}
                          data-testid={`kitchen-${channel}-${status}`}
                          sx={{
                            fontWeight: 700,
                            bgcolor: '#fff3e0',
                            color: '#e65100',
                            mb: 1,
                          }}
                        />
                      ))}
                    </Stack>
                  </Paper>
                </Grid>
              ))}
            </Grid>

            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Delivery in flight
            </Typography>
            <Grid container spacing={2} sx={{ mb: 2 }}>
              <Grid item xs={4}>
                <StatTile
                  label="Picked up"
                  value={dlv.pickedUp ?? 0}
                  color="#fff3e0"
                />
              </Grid>
              <Grid item xs={4}>
                <StatTile
                  label="Out for delivery"
                  value={dlv.outForDelivery ?? 0}
                  color="#bbdefb"
                />
              </Grid>
              <Grid item xs={4}>
                <StatTile
                  label="Total"
                  value={dlv.total ?? 0}
                  color="#ede7f6"
                />
              </Grid>
            </Grid>

            <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
              <WarningAmberIcon sx={{ color: '#c62828' }} />
              <Typography variant="h6" sx={{ fontWeight: 800, flexGrow: 1 }}>
                SLA breaches ({breaches.length})
              </Typography>
              <Chip
                label={`Kitchen ≥ ${data.kitchenSlaMinutes}m · Delivery ≥ ${data.deliverySlaMinutes}m`}
                sx={{ fontWeight: 700, bgcolor: '#ffebee', color: '#b71c1c' }}
              />
            </Stack>
            {breaches.length === 0 ? (
              <Alert severity="success">
                Nothing running late — everything is within SLA.
              </Alert>
            ) : (
              <Box sx={{ overflowX: 'auto' }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 800 }}>Type</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Order #</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Who / Channel</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Age (min)</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {breaches.map((b, idx) => (
                      <TableRow
                        key={`${b.type}-${b.orderId || b.tripId}-${idx}`}
                        data-testid={`breach-${b.type}-${b.orderId || b.tripId}`}
                      >
                        <TableCell>
                          <Chip
                            size="small"
                            label={b.type}
                            sx={{
                              fontWeight: 700,
                              bgcolor: b.type === 'kitchen' ? '#ffe0b2' : '#bbdefb',
                              color: b.type === 'kitchen' ? '#e65100' : '#0d47a1',
                              textTransform: 'uppercase',
                            }}
                          />
                        </TableCell>
                        <TableCell sx={{ fontWeight: 700 }}>
                          #{b.orderId || '—'}
                        </TableCell>
                        <TableCell>{(b.status || '').replace(/_/g, ' ')}</TableCell>
                        <TableCell>
                          {b.driverName || b.customerName || b.channel || '—'}
                        </TableCell>
                        <TableCell sx={{ fontWeight: 700, color: '#c62828' }}>
                          {b.ageMinutes}
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
