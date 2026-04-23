import { useEffect, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  TextField,
  Button,
  Box,
  Grid,
  Alert,
  Stack,
  Divider,
  Chip,
  FormGroup,
  FormControlLabel,
  Checkbox,
  InputAdornment,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  CircularProgress,
} from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import AddBoxIcon from '@mui/icons-material/AddBox';
import GrassIcon from '@mui/icons-material/Grass';
import RefreshIcon from '@mui/icons-material/Refresh';
import {
  createProduct,
  clearRecentlyCreated,
  loadProducts,
} from '../store/slices/productsSlice.js';
import { selectCurrentUser } from '../store/slices/authSlice.js';

const RAINBOW_TEXT = {
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  backgroundClip: 'text',
};

const RAINBOW_FILLED_BTN = {
  fontWeight: 700,
  letterSpacing: 1,
  textTransform: 'uppercase',
  color: '#fff',
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
  boxShadow: 'none',
  '&:hover': {
    background:
      'linear-gradient(90deg,#e53935 0%,#fb8c00 50%,#43a047 100%)',
  },
  '&.Mui-disabled': {
    color: 'rgba(255,255,255,0.75)',
    background:
      'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
    opacity: 0.55,
  },
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
};

const HEADING_SX = {
  fontWeight: 800,
  textTransform: 'uppercase',
  letterSpacing: 1,
  ...RAINBOW_TEXT,
};

const RAINBOW_CHIP = {
  bgcolor: 'transparent',
  borderColor: 'rgba(0,0,0,0.18)',
  fontWeight: 700,
  letterSpacing: 0.5,
  '& .MuiChip-label': { ...RAINBOW_TEXT },
};

const FLOUR_OPTIONS = [
  { code: 'FINGER_MILLET', label: 'Finger Millet' },
  { code: 'BAJRA_MILLET', label: 'Bajra Millet' },
  { code: 'LITTLE_MILLET', label: 'Little Millet' },
  { code: 'SORGHUM', label: 'Sorghum' },
  { code: 'WHOLE_WHEAT', label: 'Whole Wheat' },
  { code: 'ALL_PURPOSE', label: 'All Purpose' },
];

const SWEETENER_OPTIONS = [
  { code: 'CANE_SUGAR', label: 'Cane Sugar' },
  { code: 'BROWN_SUGAR', label: 'Brown Sugar' },
  { code: 'JAGGERY', label: 'Jaggery' },
  { code: 'MAPLE_SYRUP', label: 'Maple Syrup' },
  { code: 'HONEY', label: 'Honey' },
];

const prettyCode = (c) =>
  !c
    ? ''
    : c.toString().replace(/_/g, ' ').toLowerCase()
        .replace(/\b\w/g, (l) => l.toUpperCase());

const initialItem = {
  price: '',
  stock: '',
  imageUrl: '',
  categoryName: '',
  type: '',
};

export default function ManageProducts() {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);
  const userId = currentUser?.userid;
  const {
    recentlyCreated,
    createStatus,
    createError,
    catalog,
    catalogStatus,
    catalogError,
  } = useSelector((s) => s.products);

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [item, setItem] = useState(initialItem);
  const [flours, setFlours] = useState([]);
  const [sweeteners, setSweeteners] = useState([]);
  const [submitted, setSubmitted] = useState(false);

  useEffect(() => {
    dispatch(loadProducts());
  }, [dispatch]);

  useEffect(() => {
    if (submitted && createStatus === 'succeeded') {
      setName('');
      setDescription('');
      setItem(initialItem);
      setFlours([]);
      setSweeteners([]);
      setSubmitted(false);
      dispatch(loadProducts());
    }
  }, [submitted, createStatus, dispatch]);

  const handleField = (field) => (e) =>
    setItem((prev) => ({ ...prev, [field]: e.target.value }));

  const toggle = (list, setList) => (code) => () => {
    setList(list.includes(code) ? list.filter((c) => c !== code) : [...list, code]);
  };

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!userId) return;
    setSubmitted(true);
    dispatch(
      createProduct({
        userId,
        name,
        description,
        items: [
          {
            price: Number(item.price),
            stock: Number(item.stock),
            imageUrl: item.imageUrl,
            category: { categoryName: item.categoryName, type: item.type },
            supportedFlours: flours,
            supportedSweeteners: sweeteners,
          },
        ],
      }),
    );
  };

  return (
    <Stack spacing={3}>
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          borderRadius: 3,
          bgcolor: 'rgba(255,255,255,0.82)',
          backdropFilter: 'blur(8px)',
          border: '1px solid rgba(0,0,0,0.06)',
        }}
      >
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <AddBoxIcon sx={{ color: '#ef5350', fontSize: 34 }} />
          <Typography variant="h5" sx={HEADING_SX}>
            Add Product to Sell
          </Typography>
        </Stack>
        <Typography variant="body2" sx={{ mb: 2, fontWeight: 600, ...RAINBOW_TEXT }}>
          Fill in the item details, choose the flour & sweetener options you can
          bake with, and save.
        </Typography>

        {!userId && (
          <Alert severity="info" sx={{ mb: 2 }}>
            You must be logged in as an employee to add products.
          </Alert>
        )}

        <Box component="form" onSubmit={handleSubmit} noValidate>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Item name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Item description"
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Price"
                type="number"
                value={item.price}
                onChange={handleField('price')}
                fullWidth
                required
                InputProps={{
                  startAdornment: (
                    <InputAdornment position="start">$</InputAdornment>
                  ),
                }}
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Stock"
                type="number"
                value={item.stock}
                onChange={handleField('stock')}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={4}>
              <TextField
                label="Image URL"
                value={item.imageUrl}
                onChange={handleField('imageUrl')}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Category name"
                value={item.categoryName}
                onChange={handleField('categoryName')}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Category type"
                value={item.type}
                onChange={handleField('type')}
                fullWidth
              />
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper
                variant="outlined"
                sx={{ p: 2, borderColor: 'rgba(0,0,0,0.1)' }}
              >
                <Stack direction="row" spacing={1} alignItems="center" sx={{ mb: 1 }}>
                  <GrassIcon sx={{ color: '#66bb6a' }} />
                  <Typography variant="subtitle2" sx={HEADING_SX}>
                    Flour types you can bake with
                  </Typography>
                </Stack>
                <FormGroup>
                  {FLOUR_OPTIONS.map((o) => (
                    <FormControlLabel
                      key={o.code}
                      control={
                        <Checkbox
                          checked={flours.includes(o.code)}
                          onChange={toggle(flours, setFlours)(o.code)}
                        />
                      }
                      label={
                        <Typography sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                          {o.label}
                        </Typography>
                      }
                    />
                  ))}
                </FormGroup>
              </Paper>
            </Grid>

            <Grid item xs={12} md={6}>
              <Paper
                variant="outlined"
                sx={{ p: 2, borderColor: 'rgba(0,0,0,0.1)' }}
              >
                <Typography
                  variant="subtitle2"
                  sx={{ mb: 1, ...HEADING_SX }}
                >
                  Sweetener types
                </Typography>
                <FormGroup>
                  {SWEETENER_OPTIONS.map((o) => (
                    <FormControlLabel
                      key={o.code}
                      control={
                        <Checkbox
                          checked={sweeteners.includes(o.code)}
                          onChange={toggle(sweeteners, setSweeteners)(o.code)}
                        />
                      }
                      label={
                        <Typography sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                          {o.label}
                        </Typography>
                      }
                    />
                  ))}
                </FormGroup>
              </Paper>
            </Grid>
          </Grid>

          <Box sx={{ mt: 3, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            <Button
              type="submit"
              startIcon={<AddBoxIcon sx={{ color: '#fff' }} />}
              disabled={!userId || createStatus === 'loading'}
              sx={{ ...RAINBOW_FILLED_BTN, px: 3, py: 1.2 }}
            >
              {createStatus === 'loading' ? 'Saving…' : 'Save product'}
            </Button>
            <Button
              onClick={() => dispatch(clearRecentlyCreated())}
              disableRipple
              sx={RAINBOW_BTN}
            >
              Clear results
            </Button>
          </Box>
          {createError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {createError}
            </Alert>
          )}
          {createStatus === 'succeeded' && recentlyCreated.length > 0 && (
            <Alert severity="success" sx={{ mt: 2 }}>
              Saved {recentlyCreated.length} product
              {recentlyCreated.length === 1 ? '' : 's'} successfully.
            </Alert>
          )}
        </Box>
      </Paper>

      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          borderRadius: 3,
          bgcolor: 'rgba(255,255,255,0.82)',
          backdropFilter: 'blur(8px)',
          border: '1px solid rgba(0,0,0,0.06)',
        }}
      >
        <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
          <StorefrontIcon sx={{ color: '#29b6f6', fontSize: 30 }} />
          <Typography variant="h6" sx={{ flexGrow: 1, ...HEADING_SX }}>
            All Products
          </Typography>
          <Button
            startIcon={<RefreshIcon sx={{ color: '#d84315' }} />}
            onClick={() => dispatch(loadProducts())}
            disableRipple
            sx={RAINBOW_BTN}
          >
            Refresh
          </Button>
        </Stack>
        <Divider sx={{ mb: 2 }} />

        {catalogStatus === 'loading' && <CircularProgress size={28} />}
        {catalogError && <Alert severity="error">{catalogError}</Alert>}

        {catalogStatus === 'succeeded' && catalog.length === 0 && (
          <Typography sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
            No products yet — add your first one above.
          </Typography>
        )}

        {catalog.length > 0 && (
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell sx={HEADING_SX}>Name</TableCell>
                <TableCell sx={HEADING_SX}>Description</TableCell>
                <TableCell align="right" sx={HEADING_SX}>Price</TableCell>
                <TableCell align="right" sx={HEADING_SX}>Stock</TableCell>
                <TableCell sx={HEADING_SX}>Flours</TableCell>
                <TableCell sx={HEADING_SX}>Sweeteners</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {catalog.map((p) => (
                <TableRow key={p.id}>
                  <TableCell sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                    {p.name}
                  </TableCell>
                  <TableCell sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                    {p.description || '—'}
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.price != null ? `$${Number(p.price).toFixed(2)}` : '—'}
                  </TableCell>
                  <TableCell align="right" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                    {p.stock ?? '—'}
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                      {(p.supportedFlours || []).length === 0 ? (
                        <Typography variant="caption" sx={RAINBOW_TEXT}>—</Typography>
                      ) : (
                        p.supportedFlours.map((f) => (
                          <Chip
                            key={f}
                            label={prettyCode(f)}
                            size="small"
                            variant="outlined"
                            sx={RAINBOW_CHIP}
                          />
                        ))
                      )}
                    </Stack>
                  </TableCell>
                  <TableCell>
                    <Stack direction="row" spacing={0.5} flexWrap="wrap" useFlexGap>
                      {(p.supportedSweeteners || []).length === 0 ? (
                        <Typography variant="caption" sx={RAINBOW_TEXT}>—</Typography>
                      ) : (
                        p.supportedSweeteners.map((s) => (
                          <Chip
                            key={s}
                            label={prettyCode(s)}
                            size="small"
                            variant="outlined"
                            sx={RAINBOW_CHIP}
                          />
                        ))
                      )}
                    </Stack>
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
