import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadOrders = createAsyncThunk(
  'orders/load',
  async (userid) => await api.fetchOrders(userid)
);

const ordersSlice = createSlice({
  name: 'orders',
  initialState: { orders: [], items: [], status: 'idle', error: null },
  reducers: {
    clearOrders(state) {
      state.orders = [];
      state.items = [];
      state.status = 'idle';
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadOrders.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loadOrders.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.orders = action.payload?.orders ?? [];
        state.items = action.payload?.items ?? [];
      })
      .addCase(loadOrders.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      });
  },
});

export const { clearOrders } = ordersSlice.actions;
export default ordersSlice.reducer;
