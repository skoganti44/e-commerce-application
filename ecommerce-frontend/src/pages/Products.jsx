import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Paper,
  Typography,
  Box,
  Button,
  Alert,
  CircularProgress,
  TextField,
  InputAdornment,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Avatar,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Snackbar,
  MenuItem,
  Slider,
  Grid,
  Card,
  CardActionArea,
} from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import RefreshIcon from '@mui/icons-material/Refresh';
import BrushIcon from '@mui/icons-material/Brush';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import GrainIcon from '@mui/icons-material/Grain';
import { loadProducts } from '../store/slices/productsSlice.js';
import { addToCart, clearAddState, loadCart } from '../store/slices/cartSlice.js';
import { selectCurrentUser } from '../store/slices/authSlice.js';

const SWEETENERS = [
  { code: 'CANE_SUGAR', label: 'Cane Sugar' },
  { code: 'BROWN_SUGAR', label: 'Brown Sugar' },
  { code: 'MAPLE_SYRUP', label: 'Maple Syrup' },
  { code: 'JAGGERY', label: 'Jaggery' },
  { code: 'HONEY', label: 'Honey' },
];

const SWEETENER_ADDON = {
  CANE_SUGAR: 1,
  BROWN_SUGAR: 1,
  JAGGERY: 2,
  MAPLE_SYRUP: 3,
  HONEY: 3,
};

const FLOUR_ADDON = {
  ALL_PURPOSE: 1,
  WHOLE_WHEAT: 2,
  FINGER_MILLET: 5,
  BAJRA_MILLET: 5,
  LITTLE_MILLET: 5,
  SORGHUM: 5,
};

// To use the exact stock images requested, download them and save to
// ecommerce-frontend/public/flours/<code>.jpg — the tile will prefer /flours/<code>.jpg
// over the Unsplash fallback when present.
const FLOURS = [
  {
    // Requested: https://stock.adobe.com/search?k=%22finger+millet%22&asset_id=1869735377
    code: 'FINGER_MILLET',
    label: 'Finger Millet (Ragi)',
    color: 'linear-gradient(135deg,#6d4c41 0%,#a1887f 100%)',
    imageUrl: '/flours/finger-millet.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1574484184081-afea8a62f9ef?auto=format&fit=crop&w=300&q=80',
  },
  {
    // Requested: https://www.vecteezy.com/photo/2859494-close-up-of-pearl-millet-bajra-with-wooden-spoon
    code: 'BAJRA_MILLET',
    label: 'Bajra Millet',
    color: 'linear-gradient(135deg,#8d6e63 0%,#bcaaa4 100%)',
    imageUrl: '/flours/bajra-millet.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=300&q=80',
  },
  {
    // Requested: https://www.vecteezy.com/photo/47150227-millet-spike-with-shallow-depth-of-field-selective-focus
    code: 'LITTLE_MILLET',
    label: 'Little Millet',
    color: 'linear-gradient(135deg,#c0ca33 0%,#e6ee9c 100%)',
    imageUrl: '/flours/little-millet.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1509365465985-25d11c17e812?auto=format&fit=crop&w=300&q=80',
  },
  {
    // Requested: https://stock.adobe.com/search?k=grain+sorghum&asset_id=1890558905
    code: 'SORGHUM',
    label: 'Sorghum (Jowar)',
    color: 'linear-gradient(135deg,#d84315 0%,#ffab91 100%)',
    imageUrl: '/flours/sorghum.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&w=300&q=80',
  },
  {
    // Requested: https://stock.adobe.com/search?k=whole+grain&asset_id=244167973
    code: 'WHOLE_WHEAT',
    label: 'Whole Wheat',
    color: 'linear-gradient(135deg,#a1887f 0%,#d7ccc8 100%)',
    imageUrl: '/flours/whole-wheat.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&w=300&q=80',
  },
  {
    // Requested: https://stock.adobe.com/search?k=%22all-purpose+flour%22&asset_id=1905281613
    code: 'ALL_PURPOSE',
    label: 'All Purpose (Maida)',
    color: 'linear-gradient(135deg,#eceff1 0%,#ffffff 100%)',
    imageUrl: '/flours/all-purpose.jpg',
    fallbackUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=300&q=80',
  },
];

const formatPrice = (price) => {
  if (price == null) return '—';
  const n = Number(price);
  if (Number.isNaN(n)) return price;
  return `$${n.toFixed(2)}`;
};

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

const INITIAL_CUSTOM = {
  open: false,
  product: null,
  sweetenerType: 'CANE_SUGAR',
  sweetenerPercent: 50,
  flourType: 'WHOLE_WHEAT',
  quantity: 1,
};

function FlourTile({ flour, selected, onSelect }) {
  const sources = [flour.imageUrl, flour.fallbackUrl].filter(Boolean);
  const [srcIndex, setSrcIndex] = useState(0);
  const currentSrc = sources[srcIndex];
  return (
    <Card
      variant="outlined"
      sx={{
        borderColor: selected ? 'primary.main' : 'divider',
        borderWidth: selected ? 2 : 1,
        position: 'relative',
      }}
    >
      <CardActionArea onClick={() => onSelect(flour.code)}>
        <Box
          sx={{
            height: 100,
            background: flour.color,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            position: 'relative',
            overflow: 'hidden',
          }}
        >
          {currentSrc ? (
            <Box
              component="img"
              src={currentSrc}
              alt={flour.label}
              onError={() => setSrcIndex((i) => i + 1)}
              sx={{
                width: '100%',
                height: '100%',
                objectFit: 'cover',
              }}
            />
          ) : (
            <GrainIcon sx={{ fontSize: 48, color: 'rgba(255,255,255,0.9)' }} />
          )}
          {selected && (
            <CheckCircleIcon
              sx={{
                position: 'absolute',
                top: 6,
                right: 6,
                color: 'primary.main',
                bgcolor: 'white',
                borderRadius: '50%',
              }}
            />
          )}
        </Box>
        <Box sx={{ p: 1, textAlign: 'center' }}>
          <Typography variant="body2" sx={{ fontWeight: selected ? 600 : 400 }}>
            {flour.label}
          </Typography>
        </Box>
      </CardActionArea>
    </Card>
  );
}

export default function Products() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { catalog, catalogStatus, catalogError } = useSelector(
    (s) => s.products
  );
  const { addStatus, addError, lastAddedAt, totals } = useSelector((s) => s.cart);
  const currentUser = useSelector(selectCurrentUser);
  const cartCount = totals?.totalQuantity ?? 0;

  const [search, setSearch] = useState('');
  const [customDialog, setCustomDialog] = useState(INITIAL_CUSTOM);
  const [snack, setSnack] = useState({
    open: false,
    message: '',
    severity: 'success',
  });

  useEffect(() => {
    if (catalogStatus === 'idle') dispatch(loadProducts());
  }, [catalogStatus, dispatch]);

  useEffect(() => {
    if (currentUser?.userid) dispatch(loadCart(currentUser.userid));
  }, [currentUser?.userid, dispatch]);

  useEffect(() => {
    if (addStatus === 'succeeded' && lastAddedAt) {
      setSnack({ open: true, message: 'Added to cart', severity: 'success' });
      dispatch(clearAddState());
    } else if (addStatus === 'failed' && addError) {
      setSnack({ open: true, message: addError, severity: 'error' });
      dispatch(clearAddState());
    }
  }, [addStatus, addError, lastAddedAt, dispatch]);

  const filtered = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return catalog;
    return catalog.filter(
      (p) =>
        (p.name && p.name.toLowerCase().includes(q)) ||
        (p.description && p.description.toLowerCase().includes(q)) ||
        (p.category?.name && p.category.name.toLowerCase().includes(q))
    );
  }, [catalog, search]);

  const userid = currentUser?.userid;

  const handleQuickAdd = (product) => {
    if (!userid) {
      setSnack({
        open: true,
        message: 'Please log in to add items to your cart.',
        severity: 'warning',
      });
      return;
    }
    dispatch(
      addToCart({
        userid,
        productId: product.id,
        quantity: 1,
        sweetenerType: null,
        sweetenerPercent: null,
        flourType: null,
      })
    );
  };

  const openCustomDialog = (product) => {
    if (!userid) {
      setSnack({
        open: true,
        message: 'Please log in to customize items.',
        severity: 'warning',
      });
      return;
    }
    setCustomDialog({ ...INITIAL_CUSTOM, open: true, product });
  };

  const closeCustomDialog = () => setCustomDialog(INITIAL_CUSTOM);

  const submitCustom = () => {
    const { product, sweetenerType, sweetenerPercent, flourType, quantity } =
      customDialog;
    if (!product) return;
    dispatch(
      addToCart({
        userid,
        productId: product.id,
        quantity: Math.max(1, Number(quantity) || 1),
        sweetenerType,
        sweetenerPercent: Number(sweetenerPercent),
        flourType,
      })
    );
    closeCustomDialog();
  };

  return (
    <Box>
      <Paper sx={{ p: 3, mb: 3 }}>
        <Typography
          variant="h3"
          sx={{
            textAlign: 'center',
            fontWeight: 800,
            letterSpacing: 3,
            mb: 2,
            background:
              'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          Browse Products
        </Typography>
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          alignItems="center"
          justifyContent="center"
        >
          <TextField
            placeholder="Search products…"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            size="small"
            sx={{ minWidth: 260 }}
            InputProps={{
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            }}
          />
          <Button
            disableRipple
            startIcon={<RefreshIcon sx={{ color: '#29b6f6' }} />}
            onClick={() => dispatch(loadProducts())}
            sx={RAINBOW_BTN}
          >
            Refresh
          </Button>
        </Stack>
      </Paper>

      {catalogStatus === 'loading' && (
        <Box sx={{ display: 'flex', justifyContent: 'center', p: 4 }}>
          <CircularProgress />
        </Box>
      )}

      {catalogError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {catalogError}
        </Alert>
      )}

      {catalogStatus === 'succeeded' && filtered.length === 0 && (
        <Paper sx={{ p: 4, textAlign: 'center' }}>
          <Typography color="text.secondary">
            {catalog.length === 0
              ? 'No products yet. Employees can add products from the "Manage Products" page.'
              : 'No products match your search.'}
          </Typography>
        </Paper>
      )}

      {catalogStatus === 'succeeded' && filtered.length > 0 && (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow sx={{ bgcolor: 'grey.100' }}>
                <TableCell sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1.5, ...RAINBOW_TEXT }}>
                  IMAGE
                </TableCell>
                <TableCell sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1.5, ...RAINBOW_TEXT }}>
                  PRODUCT
                </TableCell>
                <TableCell sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1.5, ...RAINBOW_TEXT }}>
                  DESCRIPTION
                </TableCell>
                <TableCell align="right" sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1.5, ...RAINBOW_TEXT }}>
                  PRICE
                </TableCell>
                <TableCell align="center" sx={{ fontWeight: 800, textTransform: 'uppercase', letterSpacing: 1.5, ...RAINBOW_TEXT }}>
                  ACTIONS
                </TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filtered.map((p) => {
                const inStock = (p.stock ?? 0) > 0;
                return (
                  <TableRow key={p.id} hover>
                    <TableCell>
                      {p.imageUrl ? (
                        <Avatar
                          src={p.imageUrl}
                          alt={p.name}
                          variant="rounded"
                          sx={{ width: 72, height: 72 }}
                        />
                      ) : (
                        <Avatar
                          variant="rounded"
                          sx={{ width: 72, height: 72, bgcolor: 'grey.200' }}
                        >
                          <Typography variant="caption" color="text.secondary">
                            No image
                          </Typography>
                        </Avatar>
                      )}
                    </TableCell>
                    <TableCell>
                      <Typography variant="subtitle1" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                        {p.name}
                      </Typography>
                    </TableCell>
                    <TableCell sx={{ maxWidth: 340 }}>
                      <Tooltip title={p.description || ''}>
                        <Typography
                          variant="body2"
                          sx={{
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                            fontWeight: 600,
                            fontStyle: 'italic',
                            ...RAINBOW_TEXT,
                          }}
                        >
                          {p.description || '—'}
                        </Typography>
                      </Tooltip>
                    </TableCell>
                    <TableCell align="right">
                      <Typography
                        variant="subtitle1"
                        sx={{ fontWeight: 800, ...RAINBOW_TEXT }}
                      >
                        {formatPrice(p.price)}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Stack
                        direction="row"
                        spacing={1}
                        justifyContent="center"
                      >
                        <Button
                          size="small"
                          disableRipple
                          startIcon={<AddShoppingCartIcon sx={{ color: '#ef5350' }} />}
                          disabled={!inStock || addStatus === 'loading'}
                          onClick={() => handleQuickAdd(p)}
                          sx={RAINBOW_BTN}
                        >
                          Add to Cart
                        </Button>
                        <Button
                          size="small"
                          disableRipple
                          startIcon={<BrushIcon sx={{ color: '#ab47bc' }} />}
                          disabled={!inStock || addStatus === 'loading'}
                          onClick={() => openCustomDialog(p)}
                          sx={RAINBOW_BTN}
                        >
                          Custom
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                );
              })}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {catalogStatus === 'succeeded' && filtered.length > 0 && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4, mb: 2 }}>
          <Button
            variant="contained"
            size="large"
            startIcon={<ShoppingCartIcon />}
            onClick={() => navigate('/cart')}
            disabled={cartCount === 0}
            sx={{
              fontWeight: 700,
              letterSpacing: 1,
              px: 5,
              py: 1.5,
              fontSize: '1rem',
              background: 'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
              '&:hover': {
                background:
                  'linear-gradient(90deg,#e53935 0%,#fb8c00 50%,#43a047 100%)',
              },
            }}
          >
            View Cart{cartCount > 0 ? ` (${cartCount})` : ''}
          </Button>
        </Box>
      )}

      <Dialog
        open={customDialog.open}
        onClose={closeCustomDialog}
        fullWidth
        maxWidth="md"
      >
        <DialogTitle
          sx={{
            textAlign: 'center',
            fontWeight: 800,
            letterSpacing: 2,
            background:
              'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          Customize: {customDialog.product?.name}
        </DialogTitle>
        <DialogContent dividers>
          <Stack spacing={4} sx={{ mt: 1 }}>
            <Box>
              <Typography
                variant="subtitle2"
                gutterBottom
                sx={{ textTransform: 'uppercase', letterSpacing: 1, fontWeight: 700 }}
              >
                1. SWEETENER
              </Typography>
              <TextField
                select
                fullWidth
                size="small"
                value={customDialog.sweetenerType}
                onChange={(e) =>
                  setCustomDialog((d) => ({
                    ...d,
                    sweetenerType: e.target.value,
                  }))
                }
              >
                {SWEETENERS.map((s) => (
                  <MenuItem key={s.code} value={s.code}>
                    {s.label}
                  </MenuItem>
                ))}
              </TextField>
            </Box>

            <Box>
              <Typography
                variant="subtitle2"
                gutterBottom
                sx={{ textTransform: 'uppercase', letterSpacing: 1, fontWeight: 700 }}
              >
                2. SWEETNESS LEVEL — {customDialog.sweetenerPercent}%
              </Typography>
              <Slider
                value={Number(customDialog.sweetenerPercent)}
                onChange={(_, value) =>
                  setCustomDialog((d) => ({ ...d, sweetenerPercent: value }))
                }
                valueLabelDisplay="auto"
                step={5}
                marks={[
                  { value: 0, label: '0%' },
                  { value: 25, label: 'Light' },
                  { value: 50, label: 'Normal' },
                  { value: 75, label: 'Sweet' },
                  { value: 100, label: 'Extra' },
                ]}
                min={0}
                max={100}
              />
            </Box>

            <Box>
              <Typography
                variant="subtitle2"
                gutterBottom
                sx={{ textTransform: 'uppercase', letterSpacing: 1, fontWeight: 700 }}
              >
                3. FLOUR
              </Typography>
              <Grid container spacing={2}>
                {FLOURS.map((f) => (
                  <Grid item xs={6} sm={4} md={4} key={f.code}>
                    <FlourTile
                      flour={f}
                      selected={customDialog.flourType === f.code}
                      onSelect={(code) =>
                        setCustomDialog((d) => ({ ...d, flourType: code }))
                      }
                    />
                  </Grid>
                ))}
              </Grid>
            </Box>

            <Box>
              <Typography variant="subtitle2" gutterBottom>
                Quantity
              </Typography>
              <TextField
                type="number"
                size="small"
                value={customDialog.quantity}
                onChange={(e) =>
                  setCustomDialog((d) => ({ ...d, quantity: e.target.value }))
                }
                inputProps={{
                  min: 1,
                  max: customDialog.product?.stock ?? 1,
                }}
                sx={{ width: 160 }}
              />
            </Box>

            {(() => {
              const base = Number(customDialog.product?.price) || 0;
              const sAdd = SWEETENER_ADDON[customDialog.sweetenerType] || 0;
              const fAdd = FLOUR_ADDON[customDialog.flourType] || 0;
              const qty = Math.max(1, Number(customDialog.quantity) || 1);
              const unit = base + sAdd + fAdd;
              const total = unit * qty;
              const sLabel =
                SWEETENERS.find((s) => s.code === customDialog.sweetenerType)?.label || '';
              const fLabel =
                FLOURS.find((f) => f.code === customDialog.flourType)?.label || '';
              return (
                <Box
                  sx={{
                    p: 2,
                    borderRadius: 2,
                    bgcolor: 'grey.50',
                    border: 1,
                    borderColor: 'divider',
                  }}
                >
                  <Typography
                    variant="subtitle2"
                    gutterBottom
                    sx={{ textTransform: 'uppercase', letterSpacing: 1, fontWeight: 700 }}
                  >
                    PRICE
                  </Typography>
                  <Stack spacing={0.5}>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Base price</Typography>
                      <Typography variant="body2">{formatPrice(base)}</Typography>
                    </Stack>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Sweetener ({sLabel})</Typography>
                      <Typography variant="body2">+{formatPrice(sAdd)}</Typography>
                    </Stack>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Flour ({fLabel})</Typography>
                      <Typography variant="body2">+{formatPrice(fAdd)}</Typography>
                    </Stack>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Unit price</Typography>
                      <Typography variant="body2">{formatPrice(unit)}</Typography>
                    </Stack>
                    <Stack direction="row" justifyContent="space-between">
                      <Typography variant="body2">Quantity</Typography>
                      <Typography variant="body2">× {qty}</Typography>
                    </Stack>
                    <Stack
                      direction="row"
                      justifyContent="space-between"
                      sx={{ pt: 1, borderTop: 1, borderColor: 'divider' }}
                    >
                      <Typography variant="subtitle1" sx={{ fontWeight: 700 }}>
                        Total
                      </Typography>
                      <Typography
                        variant="subtitle1"
                        color="primary"
                        sx={{ fontWeight: 700 }}
                      >
                        {formatPrice(total)}
                      </Typography>
                    </Stack>
                  </Stack>
                </Box>
              );
            })()}
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button onClick={closeCustomDialog}>Cancel</Button>
          <Button variant="contained" onClick={submitCustom}>
            Add Customized to Cart
          </Button>
        </DialogActions>
      </Dialog>

      <Snackbar
        open={snack.open}
        autoHideDuration={3500}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          severity={snack.severity}
          onClose={() => setSnack((s) => ({ ...s, open: false }))}
          sx={{ width: '100%' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
