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
  TextField,
  MenuItem,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
} from '@mui/material';
import AssignmentIcon from '@mui/icons-material/Assignment';
import AddIcon from '@mui/icons-material/Add';
import RefreshIcon from '@mui/icons-material/Refresh';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import { createTask, fetchTasks } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const DEPARTMENTS = [
  { value: 'kitchen', label: 'Kitchen' },
  { value: 'bakery', label: 'Bakery (counter)' },
  { value: 'delivery', label: 'Delivery' },
  { value: 'management', label: 'Management' },
];
const PRIORITIES = [
  { value: 'low', label: 'Low' },
  { value: 'normal', label: 'Normal' },
  { value: 'high', label: 'High' },
  { value: 'urgent', label: 'Urgent' },
];
const STATUS_FILTERS = [
  { value: '', label: 'All' },
  { value: 'open', label: 'Open' },
  { value: 'in_progress', label: 'In progress' },
  { value: 'done', label: 'Done' },
  { value: 'cancelled', label: 'Cancelled' },
];

function priorityColor(p) {
  switch (p) {
    case 'urgent': return { bg: '#ffcdd2', fg: '#b71c1c' };
    case 'high':   return { bg: '#ffe0b2', fg: '#e65100' };
    case 'normal': return { bg: '#e8f5e9', fg: '#2e7d32' };
    default:       return { bg: '#eceff1', fg: '#455a64' };
  }
}

function statusColor(s) {
  switch (s) {
    case 'done':        return { bg: '#c8e6c9', fg: '#1b5e20' };
    case 'in_progress': return { bg: '#bbdefb', fg: '#0d47a1' };
    case 'cancelled':   return { bg: '#ffcdd2', fg: '#b71c1c' };
    default:            return { bg: '#fff3e0', fg: '#e65100' };
  }
}

export default function SalesTasks() {
  const user = useSelector(selectCurrentUser);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('');
  const [openDialog, setOpenDialog] = useState(false);
  const [toast, setToast] = useState('');

  const [form, setForm] = useState({
    assignedToDepartment: 'kitchen',
    title: '',
    description: '',
    priority: 'normal',
    dueDate: '',
    relatedOrderId: '',
  });
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState('');

  const load = useCallback(
    (filter) => {
      if (!user?.userid) return;
      setLoading(true);
      setError('');
      fetchTasks({
        createdByUserId: user.userid,
        status: filter || undefined,
      })
        .then((data) => setTasks(data || []))
        .catch((err) => {
          setError(err.response?.data?.error || err.message || 'Failed to load tasks.');
          setTasks([]);
        })
        .finally(() => setLoading(false));
    },
    [user]
  );

  useEffect(() => {
    load(statusFilter);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.userid]);

  const onApplyFilter = () => load(statusFilter);

  const onSubmit = async () => {
    setFormError('');
    if (!form.title.trim()) {
      setFormError('Title is required.');
      return;
    }
    setSubmitting(true);
    try {
      await createTask({
        createdByUserId: user.userid,
        assignedToDepartment: form.assignedToDepartment,
        title: form.title.trim(),
        description: form.description.trim() || null,
        priority: form.priority,
        dueDate: form.dueDate || null,
        relatedOrderId: form.relatedOrderId
          ? Number(form.relatedOrderId)
          : null,
      });
      setOpenDialog(false);
      setForm({
        assignedToDepartment: 'kitchen',
        title: '',
        description: '',
        priority: 'normal',
        dueDate: '',
        relatedOrderId: '',
      });
      setToast('Task created.');
      load(statusFilter);
    } catch (err) {
      setFormError(err.response?.data?.error || err.message || 'Could not create task.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 1100, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <AssignmentIcon sx={{ color: '#5e35b1', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1, ...RAINBOW_TEXT }}
          >
            Tasks I Created
          </Typography>
        </Stack>
        <Typography sx={{ mb: 2, color: '#6d4c41', fontWeight: 600 }}>
          Hand work to kitchen, bakery counter, delivery or management — and watch it move.
        </Typography>

        <Stack direction="row" spacing={2} alignItems="center" sx={{ mb: 3, flexWrap: 'wrap' }}>
          <Button
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
            sx={RAINBOW_FILLED_BTN}
          >
            New Task
          </Button>
          <TextField
            select
            size="small"
            label="Status"
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            sx={{ minWidth: 160 }}
            inputProps={{ 'aria-label': 'status-filter' }}
          >
            {STATUS_FILTERS.map((s) => (
              <MenuItem key={s.value} value={s.value}>{s.label}</MenuItem>
            ))}
          </TextField>
          <Button onClick={onApplyFilter} disabled={loading} sx={RAINBOW_OUTLINE_BTN}>
            Apply
          </Button>
          <Button
            onClick={() => load(statusFilter)}
            disabled={loading}
            startIcon={<RefreshIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Refresh
          </Button>
          <Button
            component={RouterLink}
            to="/dashboard/sales"
            startIcon={<ArrowBackIcon />}
            sx={RAINBOW_OUTLINE_BTN}
          >
            Back
          </Button>
        </Stack>

        {loading && (
          <Box sx={{ textAlign: 'center', py: 4 }}>
            <CircularProgress />
          </Box>
        )}
        {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}

        {!loading && !error && tasks.length === 0 && (
          <Alert severity="info">No tasks yet for this filter. Use “New Task” to hand work to a teammate.</Alert>
        )}

        {!loading && tasks.length > 0 && (
          <Box sx={{ overflowX: 'auto' }}>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={{ fontWeight: 800 }}>#</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Title</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>For dept</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Priority</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Due</TableCell>
                  <TableCell sx={{ fontWeight: 800 }}>Resolution</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {tasks.map((t) => {
                  const pc = priorityColor(t.priority);
                  const sc = statusColor(t.status);
                  return (
                    <TableRow key={t.id} data-testid={`task-row-${t.id}`}>
                      <TableCell sx={{ fontWeight: 700 }}>#{t.id}</TableCell>
                      <TableCell>
                        <Typography sx={{ fontWeight: 700 }}>{t.title}</Typography>
                        {t.description && (
                          <Typography variant="body2" sx={{ color: '#6d4c41' }}>
                            {t.description}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell sx={{ textTransform: 'capitalize' }}>
                        {t.assignedToDepartment}
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={t.priority}
                          sx={{ fontWeight: 700, bgcolor: pc.bg, color: pc.fg, textTransform: 'uppercase' }}
                        />
                      </TableCell>
                      <TableCell>
                        <Chip
                          size="small"
                          label={t.status.replace('_', ' ')}
                          sx={{ fontWeight: 700, bgcolor: sc.bg, color: sc.fg, textTransform: 'uppercase' }}
                        />
                      </TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>{t.dueDate || '—'}</TableCell>
                      <TableCell sx={{ color: '#6d4c41' }}>
                        {t.completedByName ? `${t.completedByName}: ${t.resolutionNotes || '—'}` : '—'}
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
          </Box>
        )}
      </Paper>

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} fullWidth maxWidth="sm">
        <DialogTitle sx={{ fontWeight: 800 }}>New task</DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <TextField
              select
              label="Hand off to"
              value={form.assignedToDepartment}
              onChange={(e) => setForm({ ...form, assignedToDepartment: e.target.value })}
              inputProps={{ 'aria-label': 'task-department' }}
            >
              {DEPARTMENTS.map((d) => (
                <MenuItem key={d.value} value={d.value}>{d.label}</MenuItem>
              ))}
            </TextField>
            <TextField
              label="Title"
              required
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              inputProps={{ 'aria-label': 'task-title' }}
            />
            <TextField
              label="Description"
              multiline
              minRows={2}
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
              inputProps={{ 'aria-label': 'task-description' }}
            />
            <Stack direction="row" spacing={2}>
              <TextField
                select
                label="Priority"
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value })}
                sx={{ flex: 1 }}
                inputProps={{ 'aria-label': 'task-priority' }}
              >
                {PRIORITIES.map((p) => (
                  <MenuItem key={p.value} value={p.value}>{p.label}</MenuItem>
                ))}
              </TextField>
              <TextField
                label="Due date"
                type="date"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
                InputLabelProps={{ shrink: true }}
                sx={{ flex: 1 }}
                inputProps={{ 'aria-label': 'task-due-date' }}
              />
            </Stack>
            <TextField
              label="Related order # (optional)"
              value={form.relatedOrderId}
              onChange={(e) => setForm({ ...form, relatedOrderId: e.target.value })}
              inputProps={{ 'aria-label': 'task-related-order' }}
            />
            {formError && <Alert severity="error">{formError}</Alert>}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)} disabled={submitting}>Cancel</Button>
          <Button
            onClick={onSubmit}
            disabled={submitting}
            sx={RAINBOW_FILLED_BTN}
          >
            {submitting ? 'Creating…' : 'Create task'}
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
