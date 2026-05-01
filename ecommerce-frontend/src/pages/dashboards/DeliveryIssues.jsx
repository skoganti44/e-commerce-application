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
  Snackbar,
} from '@mui/material';
import ReportProblemIcon from '@mui/icons-material/ReportProblem';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchDeliveryIssues,
  logDeliveryIssue,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const ISSUE_TYPES = [
  { value: 'vehicle_breakdown', label: 'Vehicle breakdown' },
  { value: 'traffic_delay', label: 'Traffic / delay' },
  { value: 'accident', label: 'Accident' },
  { value: 'other', label: 'Other' },
];

export default function DeliveryIssues() {
  const user = useSelector(selectCurrentUser);
  const driverId = user?.userid;
  const [issues, setIssues] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const [form, setForm] = useState({
    issueType: 'vehicle_breakdown',
    description: '',
    tripId: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    if (!driverId) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    fetchDeliveryIssues(driverId)
      .then((data) => setIssues(Array.isArray(data) ? data : []))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load issues.')
      )
      .finally(() => setLoading(false));
  }, [driverId]);

  useEffect(() => {
    load();
  }, [load]);

  const submit = async () => {
    if (!form.description.trim()) {
      setForm((s) => ({ ...s, error: 'Description is required' }));
      return;
    }
    setForm((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await logDeliveryIssue({
        driverId,
        issueType: form.issueType,
        description: form.description.trim(),
        tripId: form.tripId ? Number(form.tripId) : null,
      });
      setForm({
        issueType: 'vehicle_breakdown',
        description: '',
        tripId: '',
        submitting: false,
        error: '',
      });
      setToast('Issue logged.');
      load();
    } catch (err) {
      setForm((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not log issue.',
      }));
    }
  };

  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <ReportProblemIcon sx={{ color: '#ef6c00', fontSize: 30 }} />
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
            Delivery Issues
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

        <Stack spacing={2} sx={{ mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 800 }}>
            Log a new issue
          </Typography>
          <Stack direction="row" spacing={2} flexWrap="wrap">
            <TextField
              select
              label="Type"
              value={form.issueType}
              onChange={(e) =>
                setForm((s) => ({ ...s, issueType: e.target.value }))
              }
              sx={{ minWidth: 200 }}
              inputProps={{ 'aria-label': 'issue-type' }}
            >
              {ISSUE_TYPES.map((it) => (
                <MenuItem key={it.value} value={it.value}>
                  {it.label}
                </MenuItem>
              ))}
            </TextField>
            <TextField
              label="Trip # (optional)"
              type="number"
              value={form.tripId}
              onChange={(e) =>
                setForm((s) => ({ ...s, tripId: e.target.value }))
              }
              sx={{ minWidth: 160 }}
              inputProps={{ 'aria-label': 'issue-trip' }}
            />
          </Stack>
          <TextField
            label="What happened?"
            multiline
            minRows={2}
            value={form.description}
            onChange={(e) =>
              setForm((s) => ({ ...s, description: e.target.value }))
            }
            inputProps={{ 'aria-label': 'issue-desc', maxLength: 500 }}
          />
          {form.error && <Alert severity="error">{form.error}</Alert>}
          <Button
            onClick={submit}
            disabled={form.submitting}
            variant="contained"
            sx={{ ...RAINBOW_FILLED_BTN, alignSelf: 'flex-start' }}
          >
            {form.submitting ? 'Saving…' : 'Log issue'}
          </Button>
        </Stack>

        <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
          <Typography variant="h6" sx={{ fontWeight: 800, flexGrow: 1 }}>
            History
          </Typography>
          <Chip
            label={`${issues.length} issue${issues.length === 1 ? '' : 's'}`}
            sx={{ fontWeight: 700, bgcolor: '#fff3e0', color: '#e65100' }}
          />
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

        {!loading && !error && issues.length === 0 && (
          <Alert severity="success">No issues logged yet — drive safe!</Alert>
        )}

        {!loading && !error && issues.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Type</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Description</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Trip</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Reported</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {issues.map((i) => (
                  <TableRow key={i.id} data-testid={`issue-row-${i.id}`}>
                    <TableCell sx={{ fontWeight: 700 }}>#{i.id}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={(i.issueType || '').replace(/_/g, ' ')}
                        sx={{
                          fontWeight: 700,
                          bgcolor: '#fff3e0',
                          color: '#e65100',
                          textTransform: 'uppercase',
                        }}
                      />
                    </TableCell>
                    <TableCell>{i.description}</TableCell>
                    <TableCell>{i.tripId ? `#${i.tripId}` : '—'}</TableCell>
                    <TableCell sx={{ color: '#6d4c41' }}>
                      {i.reportedAt
                        ? i.reportedAt.slice(0, 16).replace('T', ' ')
                        : '—'}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

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
