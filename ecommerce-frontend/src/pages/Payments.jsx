import { useEffect, useState } from 'react';
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
  Divider,
} from '@mui/material';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import {
  loadPayments,
  clearPayments,
  setIncludeAll,
} from '../store/slices/paymentsSlice.js';
import { selectActiveUserId } from '../store/slices/sessionSlice.js';

const RAINBOW_TEXT = {
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  backgroundClip: 'text',
};

const RAINBOW_BTN = {
  bgcolor: 'transparent',
  boxShadow: 'none',
  fontWeight: 700,
  letterSpacing: 1,
  textTransform: 'uppercase',
  border: '1.5px solid',
  borderColor: 'rgba(0,0,0,0.12)',
  ...RAINBOW_TEXT,
  '&:hover': {
    bgcolor: 'transparent',
    borderColor: 'rgba(0,0,0,0.25)',
    filter: 'brightness(1.15)',
  },
  '&.Mui-disabled': {
    opacity: 0.45,
    WebkitTextFillColor: 'transparent',
  },
};

const RAINBOW_CHIP = {
  bgcolor: 'transparent',
  borderColor: 'rgba(0,0,0,0.18)',
  fontWeight: 700,
  letterSpacing: 0.5,
  '& .MuiChip-label': { ...RAINBOW_TEXT },
};

const HEADER_SX = {
  fontWeight: 800,
  textTransform: 'uppercase',
  letterSpacing: 1,
  ...RAINBOW_TEXT,
};

const statusColor = (s) => {
  const v = (s || '').toUpperCase();
  if (v === 'SUCCESS') return 'success';
  if (v === 'FAILED') return 'error';
  if (v === 'PENDING') return 'warning';
  return 'default';
};

const LAST_CHECKOUT_KEY = 'dhati.lastCheckout';

export default function Payments() {
  const dispatch = useDispatch();
  const userId = useSelector(selectActiveUserId);
  const { items, status, error, includeAll } = useSelector((s) => s.payments);

  const [lastCheckout, setLastCheckout] = useState(null);

  useEffect(() => {
    if (userId) dispatch(loadPayments({ userid: userId, includeAll }));
    else dispatch(clearPayments());
  }, [userId, includeAll, dispatch]);

  useEffect(() => {
    try {
      const raw = sessionStorage.getItem(LAST_CHECKOUT_KEY);
      if (raw) {
        setLastCheckout(JSON.parse(raw));
        sessionStorage.removeItem(LAST_CHECKOUT_KEY);
      }
    } catch {
      /* ignore */
    }
  }, []);

  if (!userId) {
    return (
      <Alert severity="info">
        <Typography sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
          Select an active user from the top-right to view payments.
        </Typography>
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      {lastCheckout && (
        <Paper
          sx={{
            p: 3,
            borderLeft: 6,
            borderColor: 'success.main',
          }}
        >
          <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
            <ReceiptLongIcon sx={{ color: '#66bb6a' }} />
            <Typography variant="h6" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
              Order Placed ✓
            </Typography>
          </Stack>
          <Stack direction="row" spacing={3} flexWrap="wrap" useFlexGap>
            <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
              <b>Order ID:</b> {lastCheckout.orderId}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
              <b>Payment ID:</b> {lastCheckout.paymentId}
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
              <b>Method:</b> {lastCheckout.paymentMethod}
            </Typography>
            <Typography
              variant="body2"
              component="span"
              sx={{ fontWeight: 700, ...RAINBOW_TEXT }}
            >
              <b>Status:</b>{' '}
              <Chip
                size="small"
                label={lastCheckout.paymentStatus}
                color={statusColor(lastCheckout.paymentStatus)}
              />
            </Typography>
            <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
              <b>Total:</b> ${Number(lastCheckout.totalAmount).toFixed(2)}
            </Typography>
          </Stack>
        </Paper>
      )}

      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2, gap: 2, flexWrap: 'wrap' }}>
          <Typography variant="h5" sx={{ flexGrow: 1, ...HEADER_SX, fontSize: '1.5rem' }}>
            ORDERS
          </Typography>
          <FormControlLabel
            control={
              <Switch
                checked={includeAll}
                onChange={(e) => dispatch(setIncludeAll(e.target.checked))}
              />
            }
            label={
              <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                Include all (show failed even when success exists)
              </Typography>
            }
          />
          <Button
            onClick={() =>
              dispatch(loadPayments({ userid: userId, includeAll }))
            }
            disableRipple
            sx={RAINBOW_BTN}
          >
            Refresh
          </Button>
        </Box>

        <Divider sx={{ mb: 2 }} />

        {status === 'loading' && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}

        {items.length === 0 && status === 'succeeded' && (
          <Typography sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
            No payments found.
          </Typography>
        )}

        {items.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={HEADER_SX}>ID</TableCell>
                <TableCell sx={HEADER_SX}>Order</TableCell>
                <TableCell align="right" sx={HEADER_SX}>Amount</TableCell>
                <TableCell sx={HEADER_SX}>Method</TableCell>
                <TableCell sx={HEADER_SX}>Status</TableCell>
                <TableCell sx={HEADER_SX}>Date</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((p) => (
                <TableRow key={p.id}>
                  <TableCell sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.id}
                  </TableCell>
                  <TableCell sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.order?.id ?? '—'}
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.amount != null ? `$${Number(p.amount).toFixed(2)}` : '—'}
                  </TableCell>
                  <TableCell sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.method ?? '—'}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={p.status ?? '—'}
                      color={statusColor(p.status)}
                    />
                  </TableCell>
                  <TableCell sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.paymentDate ?? p.createdAt ?? '—'}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>
    </Stack>
  );
}
