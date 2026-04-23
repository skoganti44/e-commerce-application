import { useState } from 'react';
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
  InputAdornment,
} from '@mui/material';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import ImageIcon from '@mui/icons-material/Image';
import {
  createProduct,
  clearRecentlyCreated,
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

const initialForm = {
  name: '',
  price: '',
  description: '',
  imageUrl: '',
};

export default function AddProducts() {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);
  const userId = currentUser?.userid;
  const { createStatus, createError, recentlyCreated } = useSelector(
    (s) => s.products,
  );

  const [form, setForm] = useState(initialForm);
  const [submitted, setSubmitted] = useState(false);

  const update = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!userId) return;
    setSubmitted(true);
    dispatch(
      createProduct({
        userId,
        name: form.name,
        description: form.description,
        items: [
          {
            price: Number(form.price),
            stock: 1,
            imageUrl: form.imageUrl,
            category: { categoryName: 'General', type: '' },
            supportedFlours: [],
            supportedSweeteners: [],
          },
        ],
      }),
    ).then((res) => {
      if (res.meta.requestStatus === 'fulfilled') {
        setForm(initialForm);
      }
    });
  };

  return (
    <Paper
      elevation={0}
      sx={{
        p: { xs: 3, md: 4 },
        borderRadius: 3,
        bgcolor: 'rgba(255,255,255,0.82)',
        backdropFilter: 'blur(8px)',
        border: '1px solid rgba(0,0,0,0.06)',
        maxWidth: 780,
        mx: 'auto',
      }}
    >
      <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 1 }}>
        <AddShoppingCartIcon sx={{ color: '#ef5350', fontSize: 34 }} />
        <Typography variant="h5" sx={HEADING_SX}>
          Add Products
        </Typography>
      </Stack>
      <Typography variant="body2" sx={{ mb: 3, fontWeight: 600, ...RAINBOW_TEXT }}>
        Fill in the product details below.
      </Typography>

      {!userId && (
        <Alert severity="info" sx={{ mb: 2 }}>
          You must be logged in as an employee to add products.
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Grid container spacing={2}>
          <Grid item xs={12}>
            <TextField
              label="Product name"
              value={form.name}
              onChange={update('name')}
              fullWidth
              required
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Price"
              type="number"
              value={form.price}
              onChange={update('price')}
              fullWidth
              required
              inputProps={{ min: 0, step: '0.01' }}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">$</InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12} md={6}>
            <TextField
              label="Product image URL"
              value={form.imageUrl}
              onChange={update('imageUrl')}
              fullWidth
              placeholder="https://example.com/image.jpg"
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <ImageIcon sx={{ color: '#29b6f6' }} />
                  </InputAdornment>
                ),
              }}
            />
          </Grid>
          <Grid item xs={12}>
            <TextField
              label="Description"
              value={form.description}
              onChange={update('description')}
              fullWidth
              multiline
              minRows={3}
            />
          </Grid>
        </Grid>

        {form.imageUrl && (
          <Box
            sx={{
              mt: 2,
              p: 1,
              border: '1px dashed rgba(0,0,0,0.15)',
              borderRadius: 2,
              display: 'inline-block',
            }}
          >
            <img
              src={form.imageUrl}
              alt="Preview"
              style={{
                maxWidth: 200,
                maxHeight: 160,
                display: 'block',
                borderRadius: 4,
              }}
              onError={(e) => {
                e.currentTarget.style.display = 'none';
              }}
            />
          </Box>
        )}

        <Box sx={{ mt: 3, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Button
            type="submit"
            startIcon={<AddShoppingCartIcon sx={{ color: '#fff' }} />}
            disabled={!userId || createStatus === 'loading'}
            sx={{ ...RAINBOW_FILLED_BTN, px: 3, py: 1.2 }}
          >
            {createStatus === 'loading' ? 'Saving…' : 'Add Product'}
          </Button>
          <Button
            onClick={() => {
              setForm(initialForm);
              setSubmitted(false);
              dispatch(clearRecentlyCreated());
            }}
            disableRipple
            sx={RAINBOW_BTN}
          >
            Reset
          </Button>
        </Box>

        {createError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {createError}
          </Alert>
        )}
        {submitted && createStatus === 'succeeded' && recentlyCreated.length > 0 && (
          <Alert severity="success" sx={{ mt: 2 }}>
            Product “{recentlyCreated[0].name}” added successfully.
          </Alert>
        )}
      </Box>
    </Paper>
  );
}
