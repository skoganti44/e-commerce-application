import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadProducts = createAsyncThunk(
  'products/load',
  async () => await api.fetchProducts()
);

export const createProduct = createAsyncThunk(
  'products/create',
  async (payload) => await api.saveProduct(payload)
);

const initialState = {
  catalog: [],
  catalogStatus: 'idle',
  catalogError: null,
  recentlyCreated: [],
  createStatus: 'idle',
  createError: null,
};

const productsSlice = createSlice({
  name: 'products',
  initialState,
  reducers: {
    clearRecentlyCreated(state) {
      state.recentlyCreated = [];
      state.createStatus = 'idle';
      state.createError = null;
    },
  },
  extraReducers: (builder) => {
    builder
      .addCase(loadProducts.pending, (state) => {
        state.catalogStatus = 'loading';
        state.catalogError = null;
      })
      .addCase(loadProducts.fulfilled, (state, action) => {
        state.catalogStatus = 'succeeded';
        state.catalog = action.payload;
      })
      .addCase(loadProducts.rejected, (state, action) => {
        state.catalogStatus = 'failed';
        state.catalogError = action.error.message;
      })
      .addCase(createProduct.pending, (state) => {
        state.createStatus = 'loading';
        state.createError = null;
      })
      .addCase(createProduct.fulfilled, (state, action) => {
        state.createStatus = 'succeeded';
        state.recentlyCreated = [...action.payload, ...state.recentlyCreated];
      })
      .addCase(createProduct.rejected, (state, action) => {
        state.createStatus = 'failed';
        state.createError = action.error.message;
      });
  },
});

export const { clearRecentlyCreated } = productsSlice.actions;
export default productsSlice.reducer;
