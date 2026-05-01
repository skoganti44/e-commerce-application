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
  Grid,
  TextField,
  Chip,
} from '@mui/material';
import SavingsIcon from '@mui/icons-material/Savings';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { fetchCashReconciliation } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

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

export default function ManagementCashReconciliation() {
  const [date, setDate] = useState(todayIso());
  const [openingFloat, setOpeningFloat] = useState('500');
  const [countedCash, setCountedCash] = useState('');
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchCashReconciliation(date, openingFloat, countedCash)
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [date, openingFloat, countedCash]);

  useEffect(() => {
    load();
  }, [load]);

  const variance = data?.variance;
  const balanced = data?.balanced === true;

  return (
    <Box sx={{ maxWidth: 1000, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <SavingsIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Cash Reconciliation
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
            label="Date"
            type="date"
            value={date}
            onChange={(e) => setDate(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'cash-date' }}
          />
          <TextField
            label="Opening float"
            type="number"
            value={openingFloat}
            onChange={(e) => setOpeningFloat(e.target.value)}
            inputProps={{ 'aria-label': 'cash-opening' }}
          />
          <TextField
            label="Counted cash in drawer"
            type="number"
            value={countedCash}
            onChange={(e) => setCountedCash(e.target.value)}
            inputProps={{ 'aria-label': 'cash-counted' }}
          />
          <Button
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Reconcile
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
                <StatTile
                  label="Opening float"
                  value={`$${Number(data.openingFloat || 0).toFixed(2)}`}
                  color="#ede7f6"
                />
              </Grid>
              <Grid item xs={6} sm={3}>
                <StatTile
                  label={`Counter cash (${data.counterCashCount})`}
                  value={`$${Number(data.counterCash || 0).toFixed(2)}`}
                  color="#fff9c4"
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
                  label="Expected drawer"
                  value={`$${Number(data.expectedCashInDrawer || 0).toFixed(2)}`}
                  color="#bbdefb"
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <StatTile
                  label="Counted cash"
                  value={
                    data.countedCash !== null && data.countedCash !== undefined
                      ? `$${Number(data.countedCash).toFixed(2)}`
                      : '—'
                  }
                  color={data.countedCash === null ? '#eceff1' : '#fff'}
                />
              </Grid>
              <Grid item xs={12} sm={6}>
                <Paper
                  elevation={0}
                  sx={{
                    p: 2,
                    borderRadius: 3,
                    textAlign: 'center',
                    bgcolor: balanced
                      ? '#c8e6c9'
                      : variance == null
                      ? '#eceff1'
                      : '#ffcdd2',
                  }}
                >
                  <Typography variant="caption" sx={{ fontWeight: 700 }}>
                    Variance
                  </Typography>
                  <Stack
                    direction="row"
                    spacing={1}
                    justifyContent="center"
                    alignItems="center"
                    sx={{ mt: 0.5 }}
                  >
                    {balanced ? (
                      <CheckCircleIcon sx={{ color: '#1b5e20' }} />
                    ) : variance == null ? null : (
                      <WarningAmberIcon sx={{ color: '#b71c1c' }} />
                    )}
                    <Typography variant="h5" sx={{ fontWeight: 800 }}>
                      {variance == null
                        ? 'Enter counted cash'
                        : `$${Number(variance).toFixed(2)}`}
                    </Typography>
                  </Stack>
                </Paper>
              </Grid>
            </Grid>

            <Stack direction="row" spacing={1} flexWrap="wrap">
              <Chip
                label={`Card · $${Number(data.counterCard || 0).toFixed(2)}`}
                data-testid="cash-pm-card"
                sx={{ fontWeight: 700, bgcolor: '#e3f2fd', color: '#1565c0', mb: 1 }}
              />
              <Chip
                label={`UPI · $${Number(data.counterUpi || 0).toFixed(2)}`}
                data-testid="cash-pm-upi"
                sx={{ fontWeight: 700, bgcolor: '#e3f2fd', color: '#1565c0', mb: 1 }}
              />
            </Stack>
          </>
        )}
      </Paper>
    </Box>
  );
}
