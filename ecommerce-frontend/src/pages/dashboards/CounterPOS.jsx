import { useEffect, useMemo, useState } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import {
  Paper,
  Typography,
  Stack,
  Box,
  Table,
  TableHead,
  TableRow,
  TableCell,
  TableBody,
  Chip,
  Alert,
  CircularProgress,
  Button,
  Divider,
  IconButton,
  Snackbar,
  TextField,
  MenuItem,
  Grid,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
} from '@mui/material';
import PointOfSaleIcon from '@mui/icons-material/PointOfSale';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddCircleIcon from '@mui/icons-material/AddCircle';
import RemoveCircleIcon from '@mui/icons-material/RemoveCircle';
import DeleteIcon from '@mui/icons-material/Delete';
import PrintIcon from '@mui/icons-material/Print';
import {
  fetchProducts,
  recordCounterSale,
} from '../../api/endpoints.js';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';

const PAYMENT_OPTIONS = [
  { value: 'CASH', label: 'Cash' },
  { value: 'CREDIT_CARD', label: 'Credit Card' },
  { value: 'DEBIT_CARD', label: 'Debit Card' },
  { value: 'UPI', label: 'UPI' },
];

function fmtMoney(n) {
  const num = Number(n) || 0;
  return num.toFixed(2);
}

export default function CounterPOS() {
  const [products, setProducts] = useState([]);
  const [loadingProducts, setLoadingProducts] = useState(true);
  const [productsError, setProductsError] = useState('');
  const [search, setSearch] = useState('');

  const [lines, setLines] = useState([]);
  const [paymentMethod, setPaymentMethod] = useState('CASH');
  const [cashGiven, setCashGiven] = useState('');
  const [customerName, setCustomerName] = useState('');
  const [customerNotes, setCustomerNotes] = useState('');

  const [submitting, setSubmitting] = useState(false);
  const [toast, setToast] = useState('');
  const [receipt, setReceipt] = useState(null);

  const loadProducts = () => {
    setLoadingProducts(true);
    setProductsError('');
    fetchProducts()
      .then((data) => setProducts(Array.isArray(data) ? data : []))
      .catch((err) =>
        setProductsError(err.message || 'Failed to load products.')
      )
      .finally(() => setLoadingProducts(false));
  };

  useEffect(() => {
    loadProducts();
  }, []);

  const filteredProducts = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return products;
    return products.filter((p) => p.name?.toLowerCase().includes(q));
  }, [products, search]);

  const total = useMemo(
    () =>
      lines.reduce(
        (sum, l) => sum + Number(l.price || 0) * Number(l.quantity || 0),
        0
      ),
    [lines]
  );

  const changeDue = useMemo(() => {
    if (paymentMethod !== 'CASH') return 0;
    const cash = Number(cashGiven);
    if (!Number.isFinite(cash)) return 0;
    return Math.max(0, cash - total);
  }, [cashGiven, total, paymentMethod]);

  const cashShortage = useMemo(() => {
    if (paymentMethod !== 'CASH') return 0;
    const cash = Number(cashGiven);
    if (!Number.isFinite(cash)) return 0;
    return Math.max(0, total - cash);
  }, [cashGiven, total, paymentMethod]);

  const addProduct = (product) => {
    setLines((prev) => {
      const idx = prev.findIndex((l) => l.productId === product.id);
      if (idx >= 0) {
        const next = [...prev];
        next[idx] = { ...next[idx], quantity: next[idx].quantity + 1 };
        return next;
      }
      return [
        ...prev,
        {
          productId: product.id,
          name: product.name,
          price: Number(product.price || 0),
          quantity: 1,
        },
      ];
    });
  };

  const adjustQty = (productId, delta) => {
    setLines((prev) =>
      prev
        .map((l) =>
          l.productId === productId
            ? { ...l, quantity: l.quantity + delta }
            : l
        )
        .filter((l) => l.quantity > 0)
    );
  };

  const removeLine = (productId) => {
    setLines((prev) => prev.filter((l) => l.productId !== productId));
  };

  const clearSale = () => {
    setLines([]);
    setPaymentMethod('CASH');
    setCashGiven('');
    setCustomerName('');
    setCustomerNotes('');
  };

  const canCharge =
    lines.length > 0 &&
    !submitting &&
    (paymentMethod !== 'CASH' || Number(cashGiven) >= total);

  const handleCharge = async () => {
    setSubmitting(true);
    try {
      const payload = {
        items: lines.map((l) => ({
          productId: l.productId,
          quantity: l.quantity,
        })),
        paymentMethod,
        customerName: customerName.trim() || undefined,
        customerNotes: customerNotes.trim() || undefined,
      };
      if (paymentMethod === 'CASH') {
        payload.cashGiven = Number(cashGiven);
      }
      const result = await recordCounterSale(payload);
      setReceipt({
        ...result,
        lines: lines.map((l) => ({ ...l })),
        timestamp: new Date().toISOString(),
        customerName: customerName.trim(),
      });
      setToast(`Sale #${result.orderId} recorded.`);
      clearSale();
    } catch (err) {
      setToast(err.response?.data?.error || err.message || 'Sale failed.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 1200, mx: 'auto' }}>
      <Paper elevation={0} sx={GLASS_PAPER}>
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
          <PointOfSaleIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography
            variant="h5"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            Counter — Walk-in POS
          </Typography>
        </Stack>

        <Typography variant="body2" sx={{ mb: 2, color: '#6d4c41' }}>
          Tap a product to add to the current sale. Choose payment, charge, hand
          the receipt to the customer.
        </Typography>

        <Grid container spacing={3}>
          <Grid item xs={12} md={7}>
            <Stack direction="row" spacing={1.5} sx={{ mb: 1.5 }}>
              <TextField
                size="small"
                placeholder="Search products"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                fullWidth
              />
              <Button
                size="small"
                startIcon={<RefreshIcon />}
                onClick={loadProducts}
                disabled={loadingProducts}
                sx={RAINBOW_OUTLINE_BTN}
              >
                Refresh
              </Button>
            </Stack>
            {loadingProducts && (
              <Box sx={{ textAlign: 'center', py: 4 }}>
                <CircularProgress />
              </Box>
            )}
            {productsError && <Alert severity="error">{productsError}</Alert>}
            {!loadingProducts && !productsError && (
              <Box
                sx={{
                  display: 'grid',
                  gridTemplateColumns: 'repeat(auto-fill, minmax(160px, 1fr))',
                  gap: 1.5,
                  maxHeight: 520,
                  overflowY: 'auto',
                  pr: 0.5,
                }}
              >
                {filteredProducts.map((p) => (
                  <Button
                    key={p.id}
                    onClick={() => addProduct(p)}
                    sx={{
                      p: 1.5,
                      bgcolor: '#fff',
                      border: '1px solid rgba(0,0,0,0.08)',
                      borderRadius: 2,
                      textAlign: 'left',
                      flexDirection: 'column',
                      alignItems: 'flex-start',
                      textTransform: 'none',
                      '&:hover': { bgcolor: '#fff8e1' },
                    }}
                  >
                    <Typography sx={{ fontWeight: 700, color: '#3e2723' }}>
                      {p.name}
                    </Typography>
                    <Typography
                      variant="body2"
                      sx={{ color: '#2e7d32', fontWeight: 700 }}
                    >
                      ${fmtMoney(p.price)}
                    </Typography>
                  </Button>
                ))}
                {filteredProducts.length === 0 && (
                  <Alert severity="info" sx={{ gridColumn: '1 / -1' }}>
                    No products match.
                  </Alert>
                )}
              </Box>
            )}
          </Grid>

          <Grid item xs={12} md={5}>
            <Paper
              variant="outlined"
              sx={{
                p: 2,
                borderRadius: 3,
                bgcolor: 'rgba(255,255,255,0.85)',
              }}
            >
              <Typography
                variant="subtitle1"
                sx={{ fontWeight: 800, mb: 1, ...RAINBOW_TEXT }}
              >
                Current sale
              </Typography>
              {lines.length === 0 ? (
                <Alert severity="info">No items yet.</Alert>
              ) : (
                <Table size="small">
                  <TableHead>
                    <TableRow>
                      <TableCell sx={{ fontWeight: 800 }}>Item</TableCell>
                      <TableCell sx={{ fontWeight: 800 }}>Qty</TableCell>
                      <TableCell sx={{ fontWeight: 800 }} align="right">
                        Line
                      </TableCell>
                      <TableCell />
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {lines.map((l) => (
                      <TableRow key={l.productId}>
                        <TableCell>
                          <Typography sx={{ fontWeight: 700 }}>
                            {l.name}
                          </Typography>
                          <Typography
                            variant="caption"
                            sx={{ color: '#6d4c41' }}
                          >
                            ${fmtMoney(l.price)} ea
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Stack
                            direction="row"
                            alignItems="center"
                            spacing={0.25}
                          >
                            <IconButton
                              size="small"
                              aria-label={`decrease ${l.name}`}
                              onClick={() => adjustQty(l.productId, -1)}
                            >
                              <RemoveCircleIcon
                                fontSize="small"
                                sx={{ color: '#c62828' }}
                              />
                            </IconButton>
                            <Typography sx={{ fontWeight: 700, minWidth: 22, textAlign: 'center' }}>
                              {l.quantity}
                            </Typography>
                            <IconButton
                              size="small"
                              aria-label={`increase ${l.name}`}
                              onClick={() => adjustQty(l.productId, 1)}
                            >
                              <AddCircleIcon
                                fontSize="small"
                                sx={{ color: '#2e7d32' }}
                              />
                            </IconButton>
                          </Stack>
                        </TableCell>
                        <TableCell align="right" sx={{ fontWeight: 700 }}>
                          ${fmtMoney(l.price * l.quantity)}
                        </TableCell>
                        <TableCell>
                          <IconButton
                            size="small"
                            aria-label={`remove ${l.name}`}
                            onClick={() => removeLine(l.productId)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              )}

              <Divider sx={{ my: 1.5 }} />
              <Stack
                direction="row"
                justifyContent="space-between"
                sx={{ mb: 1 }}
              >
                <Typography sx={{ fontWeight: 800 }}>Total</Typography>
                <Typography sx={{ fontWeight: 800, color: '#2e7d32' }}>
                  ${fmtMoney(total)}
                </Typography>
              </Stack>

              <Stack spacing={1.5}>
                <TextField
                  select
                  size="small"
                  label="Payment"
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value)}
                >
                  {PAYMENT_OPTIONS.map((o) => (
                    <MenuItem key={o.value} value={o.value}>
                      {o.label}
                    </MenuItem>
                  ))}
                </TextField>
                {paymentMethod === 'CASH' && (
                  <>
                    <TextField
                      size="small"
                      type="number"
                      label="Cash given"
                      value={cashGiven}
                      onChange={(e) => setCashGiven(e.target.value)}
                      inputProps={{ step: 'any', min: 0 }}
                      error={cashShortage > 0}
                      helperText={
                        cashShortage > 0
                          ? `Short by $${fmtMoney(cashShortage)}`
                          : changeDue > 0
                          ? `Change: $${fmtMoney(changeDue)}`
                          : ' '
                      }
                    />
                  </>
                )}
                <TextField
                  size="small"
                  label="Customer name (optional)"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                />
                <TextField
                  size="small"
                  label="Notes (optional)"
                  value={customerNotes}
                  onChange={(e) => setCustomerNotes(e.target.value)}
                  multiline
                  minRows={1}
                />
                <Stack direction="row" spacing={1.5}>
                  <Button
                    onClick={clearSale}
                    disabled={lines.length === 0 || submitting}
                    sx={RAINBOW_OUTLINE_BTN}
                  >
                    Clear
                  </Button>
                  <Button
                    onClick={handleCharge}
                    disabled={!canCharge}
                    variant="contained"
                    sx={{ ...RAINBOW_FILLED_BTN, flex: 1 }}
                  >
                    {submitting
                      ? 'Charging…'
                      : `Charge $${fmtMoney(total)}`}
                  </Button>
                </Stack>
              </Stack>
            </Paper>
          </Grid>
        </Grid>

        <Divider sx={{ my: 3 }} />
        <Button
          component={RouterLink}
          to="/dashboard/bakery"
          startIcon={<ArrowBackIcon sx={{ color: '#5d4037' }} />}
          disableRipple
          sx={RAINBOW_OUTLINE_BTN}
        >
          Back to Bakery
        </Button>
      </Paper>

      <Dialog
        open={Boolean(receipt)}
        onClose={() => setReceipt(null)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ fontWeight: 800 }}>
          Receipt — Order #{receipt?.orderId}
        </DialogTitle>
        <DialogContent dividers>
          <Box sx={{ fontFamily: 'monospace', whiteSpace: 'pre-wrap' }}>
            <Typography sx={{ fontWeight: 800, mb: 1 }}>Dhati Bake</Typography>
            <Typography variant="body2">
              {receipt?.timestamp?.replace('T', ' ').slice(0, 16)}
            </Typography>
            {receipt?.customerName && (
              <Typography variant="body2">
                Customer: {receipt.customerName}
              </Typography>
            )}
            <Divider sx={{ my: 1 }} />
            {receipt?.lines.map((l) => (
              <Stack
                key={l.productId}
                direction="row"
                justifyContent="space-between"
              >
                <Typography variant="body2">
                  {l.quantity} × {l.name}
                </Typography>
                <Typography variant="body2">
                  ${fmtMoney(l.price * l.quantity)}
                </Typography>
              </Stack>
            ))}
            <Divider sx={{ my: 1 }} />
            <Stack direction="row" justifyContent="space-between">
              <Typography sx={{ fontWeight: 800 }}>Total</Typography>
              <Typography sx={{ fontWeight: 800 }}>
                ${fmtMoney(receipt?.totalAmount)}
              </Typography>
            </Stack>
            <Stack direction="row" justifyContent="space-between">
              <Typography variant="body2">
                Paid ({receipt?.paymentMethod})
              </Typography>
              <Typography variant="body2">
                ${fmtMoney(receipt?.totalAmount)}
              </Typography>
            </Stack>
            {Number(receipt?.changeDue) > 0 && (
              <Stack direction="row" justifyContent="space-between">
                <Typography variant="body2">Change</Typography>
                <Typography variant="body2">
                  ${fmtMoney(receipt?.changeDue)}
                </Typography>
              </Stack>
            )}
            <Divider sx={{ my: 1 }} />
            <Chip
              size="small"
              label="Goes to kitchen as walk-in order"
              sx={{ fontWeight: 700, bgcolor: '#fff3e0', color: '#e65100' }}
            />
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setReceipt(null)}>Close</Button>
          <Button
            onClick={() => window.print()}
            startIcon={<PrintIcon />}
            variant="contained"
            sx={RAINBOW_FILLED_BTN}
          >
            Print
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
