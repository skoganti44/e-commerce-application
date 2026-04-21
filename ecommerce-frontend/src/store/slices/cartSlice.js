import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadCart = createAsyncThunk(
  'cart/load',
  async (userid) => await api.fetchCart(userid)
);

export const addToCart = createAsyncThunk(
  'cart/add',
  async ({ userid, productId, quantity, customization }, { rejectWithValue }) => {
    try {
      return await api.addToCart({ userid, productId, quantity, customization });
    } catch (err) {
      const msg = err?.response?.data?.error || err.message || 'Failed to add to cart';
      return rejectWithValue(msg);
    }
  }
);

const cartSlice = createSlice({
  name: 'cart',
  initialState: {
    cart: [],
    items: [],
    status: 'idle',
    error: null,
    addStatus: 'idle',
    addError: null,
    lastAddedAt: null,
  },
  reducers: {
    clearCart(state) {
      state.cart = [];
      state.items = [];
      state.status = 'idle';
      state.error = null;
    },
    clearAddState(state) {
      state.addStatus = 'idle';
      state.addError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadCart.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loadCart.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.cart = action.payload?.cart ?? [];
        state.items = action.payload?.items ?? [];
      })
      .addCase(loadCart.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      })
      .addCase(addToCart.pending, (state) => {
        state.addStatus = 'loading';
        state.addError = null;
      })
      .addCase(addToCart.fulfilled, (state, action) => {
        state.addStatus = 'succeeded';
        state.cart = action.payload?.cart ?? state.cart;
        state.items = action.payload?.items ?? state.items;
        state.lastAddedAt = Date.now();
      })
      .addCase(addToCart.rejected, (state, action) => {
        state.addStatus = 'failed';
        state.addError = action.payload || action.error.message;
      });
  },
});

export const { clearCart, clearAddState } = cartSlice.actions;
export default cartSlice.reducer;
