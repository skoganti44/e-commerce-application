import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Alert,
  CircularProgress,
  Button,
  Box,
  Stack,
  FormControlLabel,
  Switch,
  Chip,
} from '@mui/material';
import {
  loadPayments,
  clearPayments,
  setIncludeAll,
} from '../store/slices/paymentsSlice.js';
import { selectActiveUserId } from '../store/slices/sessionSlice.js';

const statusColor = (s) => {
  const v = (s || '').toUpperCase();
  if (v === 'SUCCESS') return 'success';
  if (v === 'FAILED') return 'error';
  if (v === 'PENDING') return 'warning';
  return 'default';
};

export default function Payments() {
  const dispatch = useDispatch();
  const userId = useSelector(selectActiveUserId);
  const { items, status, error, includeAll } = useSelector((s) => s.payments);

  useEffect(() => {
    if (userId) dispatch(loadPayments({ userid: userId, includeAll }));
    else dispatch(clearPayments());
  }, [userId, includeAll, dispatch]);

  if (!userId) {
    return (
      <Alert severity="info">
        Select an active user from the top-right to view payments.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 2 }}>
          <Typography variant="h5" sx={{ flexGrow: 1 }}>
            Payments for user #{userId}
          </Typography>
          <FormControlLabel
            control={
              <Switch
                checked={includeAll}
                onChange={(e) => dispatch(setIncludeAll(e.target.checked))}
              />
            }
            label="Include all (show failed even when success exists)"
          />
          <Button
            onClick={() =>
              dispatch(loadPayments({ userid: userId, includeAll }))
            }
            variant="outlined"
          >
            Refresh
          </Button>
        </Box>

        {status === 'loading' && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}

        {items.length === 0 && status === 'succeeded' && (
          <Typography color="text.secondary">No payments found.</Typography>
        )}

        {items.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Order</TableCell>
                <TableCell align="right">Amount</TableCell>
                <TableCell>Method</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((p) => (
                <TableRow key={p.id}>
                  <TableCell>{p.id}</TableCell>
                  <TableCell>{p.order?.id ?? '—'}</TableCell>
                  <TableCell align="right">{p.amount ?? '—'}</TableCell>
                  <TableCell>{p.method ?? '—'}</TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={p.status ?? '—'}
                      color={statusColor(p.status)}
                    />
                  </TableCell>
                  <TableCell>{p.paymentDate ?? p.createdAt ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>
    </Stack>
  );
}
