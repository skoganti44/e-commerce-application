import { useCallback, useEffect, useMemo, useState } from 'react';
import { useSelector } from 'react-redux';
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
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Snackbar,
} from '@mui/material';
import AssignmentIndIcon from '@mui/icons-material/AssignmentInd';
import RefreshIcon from '@mui/icons-material/Refresh';
import PlayArrowIcon from '@mui/icons-material/PlayArrow';
import DoneAllIcon from '@mui/icons-material/DoneAll';
import CancelIcon from '@mui/icons-material/Cancel';
import {
  selectCurrentUser,
  getPrimaryDepartment,
} from '../../store/slices/authSlice.js';
import { fetchTasks, updateTaskStatus } from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

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

const PRIORITY_RANK = { urgent: 0, high: 1, normal: 2, low: 3 };

export default function MyTasks({ department: deptOverride }) {
  const user = useSelector(selectCurrentUser);
  const dept = deptOverride || getPrimaryDepartment(user);
  const [tasks, setTasks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [busyId, setBusyId] = useState(null);
  const [toast, setToast] = useState('');

  const [resolveDialog, setResolveDialog] = useState({
    open: false,
    task: null,
    nextStatus: 'done',
    notes: '',
    submitting: false,
    error: '',
  });

  const load = useCallback(() => {
    if (!dept) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError('');
    fetchTasks({ department: dept })
      .then((data) => setTasks(Array.isArray(data) ? data : []))
      .catch((err) => {
        setError(
          err.response?.data?.error || err.message || 'Failed to load tasks.'
        );
        setTasks([]);
      })
      .finally(() => setLoading(false));
  }, [dept]);

  useEffect(() => {
    load();
  }, [load]);

  const openTasks = useMemo(
    () =>
      tasks
        .filter((t) => t.status === 'open' || t.status === 'in_progress')
        .sort((a, b) => {
          const pa = PRIORITY_RANK[a.priority] ?? 99;
          const pb = PRIORITY_RANK[b.priority] ?? 99;
          if (pa !== pb) return pa - pb;
          return (a.dueDate || 'zzzz').localeCompare(b.dueDate || 'zzzz');
        }),
    [tasks]
  );

  const onStart = async (task) => {
    setBusyId(task.id);
    try {
      await updateTaskStatus(task.id, {
        status: 'in_progress',
        actingUserId: user?.userid,
      });
      setToast(`Task #${task.id} started.`);
      load();
    } catch (err) {
      setToast(
        err.response?.data?.error || err.message || 'Could not start task.'
      );
    } finally {
      setBusyId(null);
    }
  };

  const openResolve = (task, nextStatus) => {
    setResolveDialog({
      open: true,
      task,
      nextStatus,
      notes: '',
      submitting: false,
      error: '',
    });
  };

  const submitResolve = async () => {
    const { task, nextStatus, notes } = resolveDialog;
    if (!task) return;
    setResolveDialog((d) => ({ ...d, submitting: true, error: '' }));
    try {
      await updateTaskStatus(task.id, {
        status: nextStatus,
        actingUserId: user?.userid,
        resolutionNotes: notes.trim() || null,
      });
      setResolveDialog({
        open: false,
        task: null,
        nextStatus: 'done',
        notes: '',
        submitting: false,
        error: '',
      });
      setToast(
        `Task #${task.id} ${nextStatus === 'done' ? 'completed' : 'cancelled'}.`
      );
      load();
    } catch (err) {
      setResolveDialog((d) => ({
        ...d,
        submitting: false,
        error:
          err.response?.data?.error || err.message || 'Could not save status.',
      }));
    }
  };

  if (!dept) {
    return null;
  }

  return (
    <Paper elevation={0} sx={{ ...GLASS_PAPER, mt: 3 }}>
      <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
        <AssignmentIndIcon sx={{ color: '#5e35b1', fontSize: 30 }} />
        <Typography
          variant="h6"
          sx={{
            fontWeight: 800,
            textTransform: 'uppercase',
            letterSpacing: 1,
            ...RAINBOW_TEXT,
            flexGrow: 1,
          }}
        >
          Tasks for my team ({dept})
        </Typography>
        <Chip
          label={`${openTasks.length} open`}
          sx={{ fontWeight: 700, bgcolor: '#ede7f6', color: '#4527a0' }}
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

      {!loading && !error && openTasks.length === 0 && (
        <Alert severity="success">
          Nothing pending — sales hasn't sent anything for {dept} right now.
        </Alert>
      )}

      {!loading && !error && openTasks.length > 0 && (
        <Box sx={{ overflowX: 'auto' }}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={{ fontWeight: 800 }}>#</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>Title</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>From</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>Priority</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>Status</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>Due</TableCell>
                <TableCell sx={{ fontWeight: 800 }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {openTasks.map((t) => {
                const pc = priorityColor(t.priority);
                const sc = statusColor(t.status);
                const busy = busyId === t.id;
                return (
                  <TableRow
                    key={t.id}
                    data-testid={`mytask-row-${t.id}`}
                  >
                    <TableCell sx={{ fontWeight: 700 }}>#{t.id}</TableCell>
                    <TableCell>
                      <Typography sx={{ fontWeight: 700 }}>{t.title}</Typography>
                      {t.description && (
                        <Typography variant="body2" sx={{ color: '#6d4c41' }}>
                          {t.description}
                        </Typography>
                      )}
                      {t.relatedOrderId && (
                        <Typography variant="caption" sx={{ color: '#1565c0' }}>
                          Related order: #{t.relatedOrderId}
                        </Typography>
                      )}
                    </TableCell>
                    <TableCell sx={{ color: '#6d4c41' }}>
                      {t.createdByName || '—'}
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={t.priority}
                        sx={{
                          fontWeight: 700,
                          bgcolor: pc.bg,
                          color: pc.fg,
                          textTransform: 'uppercase',
                        }}
                      />
                    </TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={(t.status || '').replace('_', ' ')}
                        sx={{
                          fontWeight: 700,
                          bgcolor: sc.bg,
                          color: sc.fg,
                          textTransform: 'uppercase',
                        }}
                      />
                    </TableCell>
                    <TableCell sx={{ color: '#6d4c41' }}>
                      {t.dueDate || '—'}
                    </TableCell>
                    <TableCell>
                      <Stack direction="row" spacing={1}>
                        {t.status === 'open' && (
                          <Button
                            size="small"
                            startIcon={<PlayArrowIcon />}
                            disabled={busy}
                            onClick={() => onStart(t)}
                            data-testid={`mytask-start-${t.id}`}
                            sx={{ fontWeight: 700, color: '#1565c0' }}
                          >
                            Start
                          </Button>
                        )}
                        <Button
                          size="small"
                          startIcon={<DoneAllIcon />}
                          disabled={busy}
                          onClick={() => openResolve(t, 'done')}
                          data-testid={`mytask-done-${t.id}`}
                          sx={{ fontWeight: 700, color: '#2e7d32' }}
                        >
                          Complete
                        </Button>
                        <Button
                          size="small"
                          startIcon={<CancelIcon />}
                          disabled={busy}
                          onClick={() => openResolve(t, 'cancelled')}
                          data-testid={`mytask-cancel-${t.id}`}
                          sx={{ fontWeight: 700, color: '#c62828' }}
                        >
                          Cancel
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </Box>
      )}

      <Dialog
        open={resolveDialog.open}
        onClose={() =>
          !resolveDialog.submitting &&
          setResolveDialog((d) => ({ ...d, open: false }))
        }
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          {resolveDialog.nextStatus === 'done'
            ? `Complete task #${resolveDialog.task?.id}`
            : `Cancel task #${resolveDialog.task?.id}`}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2} sx={{ mt: 1 }}>
            <Typography sx={{ fontWeight: 600 }}>
              {resolveDialog.task?.title}
            </Typography>
            <TextField
              label="Resolution notes (optional)"
              multiline
              minRows={2}
              value={resolveDialog.notes}
              onChange={(e) =>
                setResolveDialog((d) => ({ ...d, notes: e.target.value }))
              }
              inputProps={{ 'aria-label': 'mytask-notes' }}
            />
            {resolveDialog.error && (
              <Alert severity="error">{resolveDialog.error}</Alert>
            )}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() =>
              setResolveDialog((d) => ({ ...d, open: false }))
            }
            disabled={resolveDialog.submitting}
          >
            Back
          </Button>
          <Button
            onClick={submitResolve}
            disabled={resolveDialog.submitting}
            variant="contained"
            color={resolveDialog.nextStatus === 'done' ? 'success' : 'error'}
            sx={{ fontWeight: 700 }}
          >
            {resolveDialog.submitting
              ? 'Saving…'
              : resolveDialog.nextStatus === 'done'
              ? 'Complete'
              : 'Cancel task'}
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
    </Paper>
  );
}
