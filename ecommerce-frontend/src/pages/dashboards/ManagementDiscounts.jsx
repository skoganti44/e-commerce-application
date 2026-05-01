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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
} from '@mui/material';
import LocalOfferIcon from '@mui/icons-material/LocalOffer';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import AddIcon from '@mui/icons-material/Add';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  fetchDiscountCampaigns,
  proposeDiscountCampaign,
  decideDiscountCampaign,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

function todayIso() {
  return new Date().toISOString().slice(0, 10);
}

function statusColor(s) {
  switch ((s || '').toLowerCase()) {
    case 'approved': return { bg: '#c8e6c9', fg: '#1b5e20' };
    case 'rejected': return { bg: '#ffcdd2', fg: '#b71c1c' };
    default:         return { bg: '#fff3e0', fg: '#e65100' };
  }
}

export default function ManagementDiscounts() {
  const user = useSelector(selectCurrentUser);
  const [filter, setFilter] = useState('pending');
  const [campaigns, setCampaigns] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [toast, setToast] = useState('');

  const [propose, setPropose] = useState({
    open: false,
    name: '',
    categoryFilter: '',
    discountPercent: '',
    startsOn: todayIso(),
    endsOn: todayIso(),
    submitting: false,
    error: '',
  });

  const [decide, setDecide] = useState({
    open: false,
    campaign: null,
    nextStatus: 'approved',
    notes: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    setLoading(true);
    setError('');
    fetchDiscountCampaigns(filter)
      .then((d) => setCampaigns(Array.isArray(d) ? d : []))
      .catch((err) =>
        setError(err.response?.data?.error || err.message || 'Failed to load.')
      )
      .finally(() => setLoading(false));
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  const submitPropose = async () => {
    if (!propose.name.trim()) {
      setPropose((s) => ({ ...s, error: 'Name is required' }));
      return;
    }
    if (!propose.discountPercent) {
      setPropose((s) => ({ ...s, error: 'Discount % is required' }));
      return;
    }
    setPropose((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await proposeDiscountCampaign({
        proposedByUserId: user?.userid,
        name: propose.name.trim(),
        categoryFilter: propose.categoryFilter.trim() || null,
        discountPercent: Number(propose.discountPercent),
        startsOn: propose.startsOn || null,
        endsOn: propose.endsOn || null,
      });
      setPropose({
        open: false,
        name: '',
        categoryFilter: '',
        discountPercent: '',
        startsOn: todayIso(),
        endsOn: todayIso(),
        submitting: false,
        error: '',
      });
      setToast('Campaign proposed.');
      load();
    } catch (err) {
      setPropose((s) => ({
        ...s,
        submitting: false,
        error: err.response?.data?.error || err.message || 'Could not propose.',
      }));
    }
  };

  const openDecide = (campaign, nextStatus) => {
    setDecide({
      open: true,
      campaign,
      nextStatus,
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitDecide = async () => {
    const { campaign, nextStatus, notes } = decide;
    if (!campaign) return;
    setDecide((s) => ({ ...s, submitting: true, error: '' }));
    try {
      await decideDiscountCampaign(campaign.id, {
        managerUserId: user?.userid,
        decision: nextStatus,
        notes: notes.trim() || null,
      });
      setDecide({
        open: false,
        campaign: null,
        nextStatus: 'approved',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(`Campaign #${campaign.id} ${nextStatus}.`);
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
          <LocalOfferIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
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
            Discount Campaigns
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
            select
            label="Status"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            sx={{ minWidth: 160 }}
            inputProps={{ 'aria-label': 'discount-filter' }}
          >
            <MenuItem value="all">All</MenuItem>
            <MenuItem value="pending">Pending</MenuItem>
            <MenuItem value="approved">Approved</MenuItem>
            <MenuItem value="rejected">Rejected</MenuItem>
          </TextField>
          <Button
            startIcon={<RefreshIcon />}
            onClick={load}
            disabled={loading}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Button
            startIcon={<AddIcon />}
            onClick={() => setPropose((s) => ({ ...s, open: true, error: '' }))}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            Propose campaign
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 3 }}>
            <CircularProgress size={28} />
          </Box>
        )}
        {error && <Alert severity="error">{error}</Alert>}

        {!loading && !error && campaigns.length === 0 && (
          <Alert severity="info">No campaigns in this view.</Alert>
        )}

        {!loading && !error && campaigns.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Name</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Category</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>%</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>From</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>To</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Proposed by</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {campaigns.map((c) => {
                  const sc = statusColor(c.status);
                  return (
                    <TableRow key={c.id} data-testid={`discount-row-${c.id}`}>
                      <TableCell sx={{ fontWeight: 700 }}>#{c.id}</TableCell>
                      <TableCell>{c.name}</TableCell>
                      <TableCell>{c.categoryFilter || '—'}</TableCell>
                      <TableCell sx={{ fontWeight: 700, color: '#2e7d32' }}>
                        {Number(c.discountPercent || 0)}%
                      </TableCell>
                      <TableCell>{c.startsOn || '—'}</TableCell>
                      <TableCell>{c.endsOn || '—'}</TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={c.status}
                          sx={{
                            fontWeight: 700,
                            bgcolor: sc.bg,
                            color: sc.fg,
                            textTransform: 'uppercase',
                          }}
                        />
                      </TableCell>
                      <TableCell>{c.proposedByName || '—'}</TableCell>
                      <TableCell>
                        {c.status === 'pending' ? (
                          <Stack direction="row" spacing={0.5}>
                            <Button
                              size="small"
                              startIcon={<CheckCircleIcon />}
                              onClick={() => openDecide(c, 'approved')}
                              data-testid={`discount-approve-${c.id}`}
                              sx={{ fontWeight: 700, color: '#2e7d32' }}
                            >
                              Approve
                            </Button>
                            <Button
                              size="small"
                              startIcon={<CancelIcon />}
                              onClick={() => openDecide(c, 'rejected')}
                              data-testid={`discount-reject-${c.id}`}
                              sx={{ fontWeight: 700, color: '#c62828' }}
                            >
                              Reject
                            </Button>
                          </Stack>
                        ) : (
                          <Typography variant="caption" sx={{ color: '#6d4c41' }}>
                            by {c.decidedByName || '—'}
                          </Typography>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

      <Dialog
        open={propose.open}
        onClose={() => !propose.submitting && setPropose((s) => ({ ...s, open: false }))}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>Propose discount campaign</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              label="Campaign name"
              value={propose.name}
              onChange={(e) => setPropose((s) => ({ ...s, name: e.target.value }))}
              inputProps={{ 'aria-label': 'campaign-name' }}
            />
            <TextField
              label="Category filter (optional)"
              value={propose.categoryFilter}
              onChange={(e) => setPropose((s) => ({ ...s, categoryFilter: e.target.value }))}
              inputProps={{ 'aria-label': 'campaign-category' }}
            />
            <TextField
              label="Discount %"
              type="number"
              value={propose.discountPercent}
              onChange={(e) => setPropose((s) => ({ ...s, discountPercent: e.target.value }))}
              inputProps={{ 'aria-label': 'campaign-percent' }}
            />
            <Stack direction="row" spacing={1}>
              <TextField
                label="From"
                type="date"
                value={propose.startsOn}
                onChange={(e) => setPropose((s) => ({ ...s, startsOn: e.target.value }))}
                InputLabelProps={{ shrink: true }}
                inputProps={{ 'aria-label': 'campaign-from' }}
              />
              <TextField
                label="To"
                type="date"
                value={propose.endsOn}
                onChange={(e) => setPropose((s) => ({ ...s, endsOn: e.target.value }))}
                InputLabelProps={{ shrink: true }}
                inputProps={{ 'aria-label': 'campaign-to' }}
              />
            </Stack>
            {propose.error && <Alert severity="error">{propose.error}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => setPropose((s) => ({ ...s, open: false }))}
            disabled={propose.submitting}
          >
            Cancel
          </Button>
          <Button
            onClick={submitPropose}
            disabled={propose.submitting}
            variant="contained"
            sx={{ fontWeight: 700 }}
          >
            {propose.submitting ? 'Saving…' : 'Submit'}
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
          {decide.nextStatus === 'approved' ? 'Approve' : 'Reject'} #{decide.campaign?.id}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography sx={{ fontWeight: 600 }}>
              {decide.campaign?.name} · {Number(decide.campaign?.discountPercent || 0)}%
            </Typography>
            <TextField
              label="Decision notes"
              multiline
              minRows={2}
              value={decide.notes}
              onChange={(e) => setDecide((s) => ({ ...s, notes: e.target.value }))}
              inputProps={{ 'aria-label': 'discount-decide-notes' }}
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
