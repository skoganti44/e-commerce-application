import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Box,
  Button,
  Alert,
  CircularProgress,
  TextField,
  InputAdornment,
  Chip,
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
import RefreshIcon from '@mui/icons-material/Refresh';
import BrushIcon from '@mui/icons-material/Brush';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import GrainIcon from '@mui/icons-material/Grain';
import { loadProducts } from '../store/slices/productsSlice.js';
import { addToCart, clearAddState } from '../store/slices/cartSlice.js';
import { selectCurrentUser } from '../store/slices/authSlice.js';

const SWEETENERS = [
  { code: 'CANE_SUGAR', label: 'Cane Sugar' },
  { code: 'BROWN_SUGAR', label: 'Brown Sugar' },
  { code: 'MAPLE_SYRUP', label: 'Maple Syrup' },
  { code: 'JAGGERY', label: 'Jaggery' },
  { code: 'HONEY', label: 'Honey' },
];

const FLOURS = [
  {
    code: 'FINGER_MILLET',
    label: 'Finger Millet (Ragi)',
    color: 'linear-gradient(135deg,#6d4c41 0%,#a1887f 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1574484184081-afea8a62f9ef?auto=format&fit=crop&w=300&q=80',
  },
  {
    code: 'BAJRA_MILLET',
    label: 'Bajra Millet',
    color: 'linear-gradient(135deg,#8d6e63 0%,#bcaaa4 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=300&q=80',
  },
  {
    code: 'LITTLE_MILLET',
    label: 'Little Millet',
    color: 'linear-gradient(135deg,#c0ca33 0%,#e6ee9c 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1509365465985-25d11c17e812?auto=format&fit=crop&w=300&q=80',
  },
  {
    code: 'SORGHUM',
    label: 'Sorghum (Jowar)',
    color: 'linear-gradient(135deg,#d84315 0%,#ffab91 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1574323347407-f5e1ad6d020b?auto=format&fit=crop&w=300&q=80',
  },
  {
    code: 'WHOLE_WHEAT',
    label: 'Whole Wheat',
    color: 'linear-gradient(135deg,#a1887f 0%,#d7ccc8 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1568254183919-78a4f43a2877?auto=format&fit=crop&w=300&q=80',
  },
  {
    code: 'ALL_PURPOSE',
    label: 'All Purpose (Maida)',
    color: 'linear-gradient(135deg,#eceff1 0%,#ffffff 100%)',
    imageUrl: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=300&q=80',
  },
];

const formatPrice = (price) => {
  if (price == null) return '—';
  const n = Number(price);
  if (Number.isNaN(n)) return price;
  return `₹${n.toFixed(2)}`;
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
  const [imgFailed, setImgFailed] = useState(false);
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
          {flour.imageUrl && !imgFailed ? (
            <Box
              component="img"
              src={flour.imageUrl}
              alt={flour.label}
              onError={() => setImgFailed(true)}
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
  const { catalog, catalogStatus, catalogError } = useSelector(
    (s) => s.products
  );
  const { addStatus, addError, lastAddedAt } = useSelector((s) => s.cart);
  const currentUser = useSelector(selectCurrentUser);

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
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          spacing={2}
          alignItems="center"
        >
          <Typography variant="h4" sx={{ flexGrow: 1 }}>
            Browse Products
          </Typography>
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
            variant="outlined"
            startIcon={<RefreshIcon />}
            onClick={() => dispatch(loadProducts())}
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
                <TableCell sx={{ fontWeight: 600 }}>Image</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Product</TableCell>
                <TableCell sx={{ fontWeight: 600 }}>Description</TableCell>
                <TableCell sx={{ fontWeight: 600 }} align="right">
                  Price
                </TableCell>
                <TableCell sx={{ fontWeight: 600 }} align="center">
                  Availability
                </TableCell>
                <TableCell sx={{ fontWeight: 600 }} align="center">
                  Actions
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
                      <Typography variant="subtitle1" sx={{ fontWeight: 600 }}>
                        {p.name}
                      </Typography>
                      {p.category?.name && (
                        <Chip
                          label={p.category.name}
                          size="small"
                          color="secondary"
                          sx={{ mt: 0.5 }}
                        />
                      )}
                    </TableCell>
                    <TableCell sx={{ maxWidth: 340 }}>
                      <Tooltip title={p.description || ''}>
                        <Typography
                          variant="body2"
                          color="text.secondary"
                          sx={{
                            display: '-webkit-box',
                            WebkitLineClamp: 2,
                            WebkitBoxOrient: 'vertical',
                            overflow: 'hidden',
                          }}
                        >
                          {p.description || '—'}
                        </Typography>
                      </Tooltip>
                    </TableCell>
                    <TableCell align="right">
                      <Typography
                        variant="subtitle1"
                        color="primary"
                        sx={{ fontWeight: 600 }}
                      >
                        {formatPrice(p.price)}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      {inStock ? (
                        <Chip
                          label={`In Stock (${p.stock})`}
                          color="success"
                          size="small"
                          variant="outlined"
                        />
                      ) : (
                        <Chip
                          label="Out of Stock"
                          color="error"
                          size="small"
                          variant="outlined"
                        />
                      )}
                    </TableCell>
                    <TableCell align="center">
                      <Stack
                        direction="row"
                        spacing={1}
                        justifyContent="center"
                      >
                        <Button
                          variant="contained"
                          size="small"
                          startIcon={<AddShoppingCartIcon />}
                          disabled={!inStock || addStatus === 'loading'}
                          onClick={() => handleQuickAdd(p)}
                        >
                          Add to Cart
                        </Button>
                        <Button
                          variant="outlined"
                          size="small"
                          color="secondary"
                          startIcon={<BrushIcon />}
                          disabled={!inStock || addStatus === 'loading'}
                          onClick={() => openCustomDialog(p)}
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

      <Dialog
        open={customDialog.open}
        onClose={closeCustomDialog}
        fullWidth
        maxWidth="md"
      >
        <DialogTitle>
          Customize: {customDialog.product?.name}
        </DialogTitle>
        <DialogContent dividers>
          <Stack spacing={4} sx={{ mt: 1 }}>
            <Box>
              <Typography variant="subtitle2" gutterBottom>
                1. Sweetener
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
              <Typography variant="subtitle2" gutterBottom>
                2. Sweetness level — {customDialog.sweetenerPercent}%
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
              <Typography variant="subtitle2" gutterBottom>
                3. Flour
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
