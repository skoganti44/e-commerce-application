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
import { loadCart, clearCart } from '../store/slices/cartSlice.js';
import { selectActiveUserId } from '../store/slices/sessionSlice.js';

export default function Cart() {
  const dispatch = useDispatch();
  const userId = useSelector(selectActiveUserId);
  const { cart, items, status, error } = useSelector((s) => s.cart);

  useEffect(() => {
    if (userId) dispatch(loadCart(userId));
    else dispatch(clearCart());
  }, [userId, dispatch]);

  if (!userId) {
    return (
      <Alert severity="info">
        Select an active user from the top-right to view their cart.
      </Alert>
    );
  }

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Typography variant="h5" sx={{ flexGrow: 1 }}>
            Cart for user #{userId}
          </Typography>
          <Button onClick={() => dispatch(loadCart(userId))} variant="outlined">
            Refresh
          </Button>
        </Box>

        {status === 'loading' && <CircularProgress />}
        {error && <Alert severity="error">{error}</Alert>}

        {cart.length > 0 && (
          <Table size="small" sx={{ mb: 3 }}>
            <TableHead>
              <TableRow>
                <TableCell>Cart ID</TableCell>
                <TableCell>User ID</TableCell>
                <TableCell>Created</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {cart.map((c) => (
                <TableRow key={c.id ?? c.cartid}>
                  <TableCell>{c.id ?? c.cartid}</TableCell>
                  <TableCell>{c.user?.userid ?? userId}</TableCell>
                  <TableCell>{c.createdAt ?? c.createdat ?? '—'}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        )}
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography variant="h6" gutterBottom>
          Items ({items.length})
        </Typography>
        {items.length === 0 ? (
          <Typography color="text.secondary">No items in cart.</Typography>
        ) : (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Item ID</TableCell>
                <TableCell>Product</TableCell>
                <TableCell align="right">Quantity</TableCell>
                <TableCell align="right">Price</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {items.map((it) => (
                <TableRow key={it.id}>
                  <TableCell>{it.id}</TableCell>
                  <TableCell>{it.product?.name ?? '—'}</TableCell>
                  <TableCell align="right">{it.quantity}</TableCell>
                  <TableCell align="right">
                    {it.product?.price ?? '—'}
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
