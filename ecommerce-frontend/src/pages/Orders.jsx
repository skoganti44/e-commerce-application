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
} from '@mui/material';
import { loadOrders, clearOrders } from '../store/slices/ordersSlice.js';
import { selectActiveUserId } from '../store/slices/sessionSlice.js';

export default function Orders() {
  const dispatch = useDispatch();
  const userId = useSelector(selectActiveUserId);
  const { orders, items, status, error } = useSelector((s) => s.orders);

  useEffect(() => {
    if (userId) dispatch(loadOrders(userId));
    else dispatch(clearOrders());
  }, [userId, dispatch]);

  if (!userId) {
    return (
      <Alert severity="info">
        Select an active user from the top-right to view their orders.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h5" sx={{ flexGrow: 1 }}>
            Orders for user #{userId}
          </Typography>
          <Button
            onClick={() => dispatch(loadOrders(userId))}
            variant="outlined"
          >
            Refresh
          </Button>
        </Box>

        {status === 'loading' && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}

        {orders.length === 0 && status === 'succeeded' && (
          <Typography color="text.secondary">No orders yet.</Typography>
        )}

        {orders.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Order ID</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">Total</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {orders.map((o) => (
                <TableRow key={o.id}>
                  <TableCell>{o.id}</TableCell>
                  <TableCell>{o.status ?? '—'}</TableCell>
                  <TableCell align="right">{o.totalAmount ?? '—'}</TableCell>
                  <TableCell>{o.createdAt ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      {items.length > 0 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Order items ({items.length})
          </Typography>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Item ID</TableCell>
                <TableCell>Order ID</TableCell>
                <TableCell>Product</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell align="right">Price</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((it) => (
                <TableRow key={it.id}>
                  <TableCell>{it.id}</TableCell>
                  <TableCell>{it.order?.id ?? '—'}</TableCell>
                  <TableCell>{it.product?.name ?? '—'}</TableCell>
                  <TableCell align="right">{it.quantity}</TableCell>
                  <TableCell align="right">{it.price ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </Paper>
      )}
    </Stack>
  );
}
