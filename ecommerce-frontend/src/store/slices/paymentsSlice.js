import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadPayments = createAsyncThunk(
  'payments/load',
  async ({ userid, includeAll = false }) =>
    await api.fetchPayments(userid, includeAll)
);

const paymentsSlice = createSlice({
  name: 'payments',
  initialState: { items: [], status: 'idle', error: null, includeAll: false },
  reducers: {
    clearPayments(state) {
      state.items = [];
      state.status = 'idle';
      state.error = null;
    },
    setIncludeAll(state, action) {
      state.includeAll = Boolean(action.payload);
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadPayments.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loadPayments.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload;
      })
      .addCase(loadPayments.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      });
  },
});

export const { clearPayments, setIncludeAll } = paymentsSlice.actions;
export default paymentsSlice.reducer;
