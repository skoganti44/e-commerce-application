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
  Divider,
  Card,
  CardContent,
  CardMedia,
} from '@mui/material';
import {
  createProduct,
  clearRecentlyCreated,
} from '../store/slices/productsSlice.js';
import { selectCurrentUser } from '../store/slices/authSlice.js';

const emptyItem = {
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
  const { recentlyCreated, createStatus, createError } = useSelector(
    (s) => s.products
  );

  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [item, setItem] = useState(emptyItem);

  const handleField = (field) => (e) =>
    setItem((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!userId) return;
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
          },
        ],
      })
    );
  };

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Typography variant="h5" gutterBottom>
          Add Items to Sell
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Only employees can add items to the catalogue.
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
                label="Product name"
                value={name}
                onChange={(e) => setName(e.target.value)}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Description"
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
          </Grid>
          <Box sx={{ mt: 2, display: 'flex', gap: 1 }}>
            <Button
              type="submit"
              variant="contained"
              disabled={!userId || createStatus === 'loading'}
            >
              {createStatus === 'loading' ? 'Saving…' : 'Save product'}
            </Button>
            <Button
              onClick={() => dispatch(clearRecentlyCreated())}
              variant="text"
            >
              Clear results
            </Button>
          </Box>
          {createError && (
            <Alert severity="error" sx={{ mt: 2 }}>
              {createError}
            </Alert>
          )}
        </Box>
      </Paper>

      {recentlyCreated.length > 0 && (
        <Paper sx={{ p: 3 }}>
          <Typography variant="h6" gutterBottom>
            Recently created ({recentlyCreated.length})
          </Typography>
          <Divider sx={{ mb: 2 }} />
          <Grid container spacing={2}>
            {recentlyCreated.map((p) => (
              <Grid item xs={12} sm={6} md={4} key={p.id}>
                <Card>
                  {p.imageUrl && (
                    <CardMedia
                      component="img"
                      height="140"
                      image={p.imageUrl}
                      alt={p.name}
                    />
                  )}
                  <CardContent>
                    <Typography variant="subtitle1">{p.name}</Typography>
                    <Typography variant="body2" color="text.secondary">
                      {p.description}
                    </Typography>
                    <Typography variant="body2" sx={{ mt: 1 }}>
                      Price: {p.price} · Stock: {p.stock}
                    </Typography>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Paper>
      )}
    </Stack>
  );
}
