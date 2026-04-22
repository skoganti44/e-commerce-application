import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadCart = createAsyncThunk(
  'cart/load',
  async (userid) => await api.fetchCart(userid)
);

export const addToCart = createAsyncThunk(
  'cart/add',
  async (payload, { rejectWithValue }) => {
    try {
      return await api.addToCart(payload);
    } catch (err) {
      const msg = err?.response?.data?.error || err.message || 'Failed to add to cart';
      return rejectWithValue(msg);
    }
  }
);

export const updateCartItemQty = createAsyncThunk(
  'cart/updateQty',
  async ({ userid, cartItemId, quantity }, { rejectWithValue }) => {
    try {
      return await api.updateCartItem(userid, cartItemId, quantity);
    } catch (err) {
      const msg = err?.response?.data?.error || err.message || 'Failed to update cart';
      return rejectWithValue(msg);
    }
  }
);

export const removeCartItem = createAsyncThunk(
  'cart/remove',
  async ({ userid, cartItemId }, { rejectWithValue }) => {
    try {
      return await api.removeCartItem(userid, cartItemId);
    } catch (err) {
      const msg = err?.response?.data?.error || err.message || 'Failed to remove item';
      return rejectWithValue(msg);
    }
  }
);

const cartSlice = createSlice({
  name: 'cart',
  initialState: {
    cart: [],
    items: [],
    itemTotals: {},
    totals: { subtotal: 0, itemCount: 0, totalQuantity: 0 },
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
      state.itemTotals = {};
      state.totals = { subtotal: 0, itemCount: 0, totalQuantity: 0 };
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
        state.itemTotals = action.payload?.itemTotals ?? {};
        state.totals = action.payload?.totals ?? {
          subtotal: 0,
          itemCount: 0,
          totalQuantity: 0,
        };
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
        state.itemTotals = action.payload?.itemTotals ?? state.itemTotals;
        state.totals = action.payload?.totals ?? state.totals;
        state.lastAddedAt = Date.now();
      })
      .addCase(addToCart.rejected, (state, action) => {
        state.addStatus = 'failed';
        state.addError = action.payload || action.error.message;
      })
      .addCase(updateCartItemQty.fulfilled, (state, action) => {
        state.cart = action.payload?.cart ?? state.cart;
        state.items = action.payload?.items ?? state.items;
        state.itemTotals = action.payload?.itemTotals ?? state.itemTotals;
        state.totals = action.payload?.totals ?? state.totals;
      })
      .addCase(updateCartItemQty.rejected, (state, action) => {
        state.addError = action.payload || action.error.message;
      })
      .addCase(removeCartItem.fulfilled, (state, action) => {
        state.cart = action.payload?.cart ?? state.cart;
        state.items = action.payload?.items ?? state.items;
        state.itemTotals = action.payload?.itemTotals ?? state.itemTotals;
        state.totals = action.payload?.totals ?? state.totals;
      })
      .addCase(removeCartItem.rejected, (state, action) => {
        state.addError = action.payload || action.error.message;
      });
  },
});

export const { clearCart, clearAddState } = cartSlice.actions;
export default cartSlice.reducer;
