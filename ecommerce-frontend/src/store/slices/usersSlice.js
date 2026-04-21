import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';

export const loadUsers = createAsyncThunk('users/load', async () => {
  return await api.fetchUsers();
});

const usersSlice = createSlice({
  name: 'users',
  initialState: { items: [], status: 'idle', error: null },
  reducers: {},
  extraReducers: (builder) => {
    builder
      .addCase(loadUsers.pending, (state) => {
        state.status = 'loading';
        state.error = null;
      })
      .addCase(loadUsers.fulfilled, (state, action) => {
        state.status = 'succeeded';
        state.items = action.payload;
      })
      .addCase(loadUsers.rejected, (state, action) => {
        state.status = 'failed';
        state.error = action.error.message;
      });
  },
});

export default usersSlice.reducer;
