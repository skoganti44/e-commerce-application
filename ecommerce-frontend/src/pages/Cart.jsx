import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
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
  Chip,
  IconButton,
  Tooltip,
} from '@mui/material';
import ShoppingCartCheckoutIcon from '@mui/icons-material/ShoppingCartCheckout';
import AddIcon from '@mui/icons-material/Add';
import RemoveIcon from '@mui/icons-material/Remove';
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline';
import {
  loadCart,
  clearCart,
  updateCartItemQty,
  removeCartItem,
} from '../store/slices/cartSlice.js';
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

const formatPrice = (v) => {
  if (v == null) return '—';
  const n = Number(v);
  if (Number.isNaN(n)) return v;
  return `$${n.toFixed(2)}`;
};

const prettyCode = (c) =>
  !c ? '' : c.toString().replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (l) => l.toUpperCase());

export default function Cart() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const userId = useSelector(selectActiveUserId);
  const { items, itemTotals, totals, status, error } = useSelector(
    (s) => s.cart
  );

  useEffect(() => {
    if (userId) dispatch(loadCart(userId));
    else dispatch(clearCart());
  }, [userId, dispatch]);

  const incQty = (it) =>
    dispatch(
      updateCartItemQty({
        userid: userId,
        cartItemId: it.id,
        quantity: (it.quantity || 0) + 1,
      }),
    );

  const decQty = (it) => {
    const next = (it.quantity || 0) - 1;
    if (next <= 0) {
      dispatch(removeCartItem({ userid: userId, cartItemId: it.id }));
    } else {
      dispatch(
        updateCartItemQty({
          userid: userId,
          cartItemId: it.id,
          quantity: next,
        }),
      );
    }
  };

  const removeItem = (it) =>
    dispatch(removeCartItem({ userid: userId, cartItemId: it.id }));

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
        <Box sx={{ display: 'flex', alignItems: 'center' }}>
          <Typography
            variant="h5"
            sx={{
              flexGrow: 1,
              fontWeight: 800,
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            Your Cart
          </Typography>
          <Button
            onClick={() => dispatch(loadCart(userId))}
            disableRipple
            sx={RAINBOW_BTN}
          >
            Refresh
          </Button>
        </Box>

        {status === 'loading' && <CircularProgress sx={{ mt: 2 }} />}
        {error && <Alert severity="error" sx={{ mt: 2 }}>{error}</Alert>}
      </Paper>

      <Paper sx={{ p: 3 }}>
        <Typography
          variant="h6"
          gutterBottom
          sx={{ fontWeight: 800, letterSpacing: 1, ...RAINBOW_TEXT }}
        >
          Items ({items.length})
        </Typography>
        {items.length === 0 ? (
          <Typography color="text.secondary">No items in cart.</Typography>
        ) : (
          <>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell sx={HEADER_SX}>PRODUCT</TableCell>
                  <TableCell sx={HEADER_SX}>CUSTOMIZATION</TableCell>
                  <TableCell align="right" sx={HEADER_SX}>UNIT PRICE</TableCell>
                  <TableCell align="center" sx={HEADER_SX}>QTY</TableCell>
                  <TableCell align="right" sx={HEADER_SX}>LINE TOTAL</TableCell>
                  <TableCell align="center" sx={HEADER_SX}>REMOVE</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {items.map((it) => {
                  const pricing = itemTotals?.[it.id] || {};
                  const base = Number(pricing.basePrice ?? it.product?.price ?? 0);
                  const sAdd = Number(pricing.sweetenerAddon ?? 0);
                  const fAdd = Number(pricing.flourAddon ?? 0);
                  const unit = Number(pricing.unitPrice ?? base + sAdd + fAdd);
                  const line = Number(pricing.lineTotal ?? unit * (it.quantity || 0));
                  const stockCap = it.product?.stock ?? Infinity;
                  return (
                    <TableRow key={it.id}>
                      <TableCell>
                        <Typography
                          variant="body2"
                          sx={{ fontWeight: 700, ...RAINBOW_TEXT }}
                        >
                          {it.product?.name ?? '—'}
                        </Typography>
                      </TableCell>
                      <TableCell>
                        <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                          {it.sweetenerType && (
                            <Chip
                              size="small"
                              variant="outlined"
                              label={`${prettyCode(it.sweetenerType)}${
                                it.sweetenerPercent != null ? ` ${it.sweetenerPercent}%` : ''
                              }`}
                              sx={RAINBOW_CHIP}
                            />
                          )}
                          {it.flourType && (
                            <Chip
                              size="small"
                              variant="outlined"
                              label={prettyCode(it.flourType)}
                              sx={RAINBOW_CHIP}
                            />
                          )}
                          {!it.sweetenerType && !it.flourType && (
                            <Typography variant="caption" sx={RAINBOW_TEXT}>
                              —
                            </Typography>
                          )}
                        </Stack>
                      </TableCell>
                      <TableCell align="right">
                        <Typography
                          variant="body2"
                          sx={{ fontWeight: 700, ...RAINBOW_TEXT }}
                        >
                          {formatPrice(unit)}
                        </Typography>
                        {(sAdd > 0 || fAdd > 0) && (
                          <Typography variant="caption" sx={RAINBOW_TEXT}>
                            base {formatPrice(base)}
                            {sAdd > 0 ? ` + swt ${formatPrice(sAdd)}` : ''}
                            {fAdd > 0 ? ` + flr ${formatPrice(fAdd)}` : ''}
                          </Typography>
                        )}
                      </TableCell>
                      <TableCell align="center">
                        <Stack direction="row" spacing={0.5} alignItems="center" justifyContent="center">
                          <Tooltip title={(it.quantity || 0) <= 1 ? 'Remove item' : 'Decrease'}>
                            <IconButton size="small" onClick={() => decQty(it)}>
                              <RemoveIcon fontSize="small" sx={{ color: '#d84315' }} />
                            </IconButton>
                          </Tooltip>
                          <Typography
                            sx={{
                              minWidth: 28,
                              textAlign: 'center',
                              fontWeight: 800,
                              ...RAINBOW_TEXT,
                            }}
                          >
                            {it.quantity}
                          </Typography>
                          <Tooltip title={(it.quantity || 0) >= stockCap ? 'Stock limit' : 'Add more'}>
                            <span>
                              <IconButton
                                size="small"
                                onClick={() => incQty(it)}
                                disabled={(it.quantity || 0) >= stockCap}
                              >
                                <AddIcon fontSize="small" sx={{ color: '#2e7d32' }} />
                              </IconButton>
                            </span>
                          </Tooltip>
                        </Stack>
                      </TableCell>
                      <TableCell align="right">
                        <Typography sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                          {formatPrice(line)}
                        </Typography>
                      </TableCell>
                      <TableCell align="center">
                        <Tooltip title="Remove from cart">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => removeItem(it)}
                          >
                            <DeleteOutlineIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    </TableRow>
                  );
                })}
              </TableBody>
            </Table>
            <Box
              sx={{
                mt: 2,
                p: 2,
                borderRadius: 2,
                bgcolor: 'grey.50',
                border: 1,
                borderColor: 'divider',
                display: 'flex',
                flexWrap: 'wrap',
                alignItems: 'center',
                gap: 2,
              }}
            >
              <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                Items: {totals?.itemCount ?? items.length} · Qty:{' '}
                {totals?.totalQuantity ?? items.reduce((s, i) => s + (i.quantity || 0), 0)}
              </Typography>
              <Box sx={{ flexGrow: 1 }} />
              <Typography
                variant="h6"
                sx={{ fontWeight: 800, letterSpacing: 0.5, ...RAINBOW_TEXT }}
              >
                Subtotal: {formatPrice(totals?.subtotal ?? 0)}
              </Typography>
              <Button
                disableRipple
                onClick={() => navigate('/products')}
                sx={RAINBOW_BTN}
              >
                Continue Shopping
              </Button>
              <Button
                variant="contained"
                size="large"
                disabled={items.length === 0}
                onClick={() => navigate('/checkout')}
                startIcon={<ShoppingCartCheckoutIcon />}
                sx={{
                  fontWeight: 700,
                  letterSpacing: 1,
                  px: 3,
                  background:
                    'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
                  '&:hover': {
                    background:
                      'linear-gradient(90deg,#e53935 0%,#fb8c00 50%,#43a047 100%)',
                  },
                }}
              >
                Proceed to Checkout
              </Button>
            </Box>
          </>
        )}
      </Paper>
    </Stack>
  );
}
