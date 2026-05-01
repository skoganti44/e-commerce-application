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
} from '@mui/material';
import GroupsIcon from '@mui/icons-material/Groups';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { fetchManagementStaffPerformance } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

export default function ManagementStaffPerformance() {
  const [from, setFrom] = useState(todayIso());
  const [to, setTo] = useState(todayIso());
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchManagementStaffPerformance(from, to)
      .then((d) => setData(d))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [from, to]);

  useEffect(() => {
    load();
  }, [load]);

  const drivers = data?.drivers || [];
  const byDept = data?.staffByDepartment || {};
  const sales = data?.salesActivity || [];

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <GroupsIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Staff Performance
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
            inputProps={{ 'aria-label': 'perf-from' }}
          />
          <TextField
            label="To"
            type="date"
            value={to}
            onChange={(e) => setTo(e.target.value)}
            InputLabelProps={{ shrink: true }}
            inputProps={{ 'aria-label': 'perf-to' }}
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
            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Drivers
            </Typography>
            {drivers.length === 0 ? (
              <Alert severity="info" sx={{ mb: 3 }}>
                No driver activity in this range.
              </Alert>
            ) : (
              <Box sx={{ overflowX: 'auto', mb: 3 }}>
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 800 }}>Driver</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Trips</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Delivered</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Failed</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>COD</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Tips</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Distance</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {drivers.map((d) => (
                      <TableRow key={d.userId} data-testid={`perf-driver-${d.userId}`}>
                        <TableCell sx={{ fontWeight: 700 }}>{d.name}</TableCell>
                        <TableCell>{d.trips}</TableCell>
                        <TableCell sx={{ color: '#2e7d32', fontWeight: 700 }}>
                          {d.delivered}
                        </TableCell>
                        <TableCell sx={{ color: '#c62828', fontWeight: 700 }}>
                          {d.failed}
                        </TableCell>
                        <TableCell>${Number(d.cod || 0).toFixed(2)}</TableCell>
                        <TableCell>${Number(d.tips || 0).toFixed(2)}</TableCell>
                        <TableCell>{Number(d.distanceKm || 0).toFixed(1)} km</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </Box>
            )}

            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Tasks completed by team
            </Typography>
            {Object.keys(byDept).length === 0 ? (
              <Alert severity="info" sx={{ mb: 3 }}>
                No tasks completed in this range.
              </Alert>
            ) : (
              <Grid container spacing={2} sx={{ mb: 3 }}>
                {Object.entries(byDept).map(([dept, rows]) => (
                  <Grid key={dept} item xs={12} md={6}>
                    <Paper sx={{ p: 2, borderRadius: 3, bgcolor: '#fff8e1' }}>
                      <Typography sx={{ fontWeight: 800, textTransform: 'uppercase', mb: 1 }}>
                        {dept}
                      </Typography>
                      <Stack spacing={0.75}>
                        {rows.map((r) => (
                          <Stack
                            key={r.userId}
                            direction="row"
                            justifyContent="space-between"
                            data-testid={`perf-${dept}-${r.userId}`}
                          >
                            <Typography>{r.name}</Typography>
                            <Chip
                              size="small"
                              label={`${r.tasksCompleted} done`}
                              sx={{ fontWeight: 700, bgcolor: '#c8e6c9', color: '#1b5e20' }}
                            />
                          </Stack>
                        ))}
                      </Stack>
                    </Paper>
                  </Grid>
                ))}
              </Grid>
            )}

            <Typography variant="h6" sx={{ fontWeight: 800, mb: 1 }}>
              Sales activity (tasks created)
            </Typography>
            {sales.length === 0 ? (
              <Alert severity="info">No tasks were created in this range.</Alert>
            ) : (
              <Stack direction="row" spacing={1} flexWrap="wrap">
                {sales.map((s) => (
                  <Chip
                    key={s.userId}
                    label={`${s.name} · ${s.tasksCreated}`}
                    data-testid={`perf-sales-${s.userId}`}
                    sx={{ fontWeight: 700, bgcolor: '#e3f2fd', color: '#1565c0', mb: 1 }}
                  />
                ))}
              </Stack>
            )}
          </>
        )}
      </Paper>
    </Box>
  );
}
