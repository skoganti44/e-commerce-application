import { createSlice, createAsyncThunk } from '@reduxjs/toolkit';
import * as api from '../../api/endpoints.js';
import { setActiveUserId } from './sessionSlice.js';

const STORAGE_KEY = 'ecommerce.currentUser';

const loadInitialUser = () => {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
};

const persist = (user) => {
  if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
  else localStorage.removeItem(STORAGE_KEY);
};

export const registerUser = createAsyncThunk(
  'auth/register',
  async (payload, { dispatch }) => {
    const user = await api.registerUser(payload);
    dispatch(setActiveUserId(user.userid));
    return user;
  }
);

export const loginUser = createAsyncThunk(
  'auth/login',
  async (payload, { dispatch }) => {
    const user = await api.loginUser(payload);
    dispatch(setActiveUserId(user.userid));
    return user;
  }
);

const authSlice = createSlice({
  name: 'auth',
  initialState: {
    currentUser: loadInitialUser(),
    status: 'idle',
    error: null,
  },
  reducers: {
    logout(state) {
      state.currentUser = null;
      state.status = 'idle';
      state.error = null;
      persist(null);
    },
    clearAuthError(state) {
      state.error = null;
    },
  },
  extraReducers: (builder) => {
    const onPending = (state) => {
      state.status = 'loading';
      state.error = null;
    };
    const onFulfilled = (state, action) => {
      state.status = 'succeeded';
      state.currentUser = action.payload;
      persist(action.payload);
    };
    const onRejected = (state, action) => {
      state.status = 'failed';
      state.error = action.error.message;
    };
    builder
      .addCase(registerUser.pending, onPending)
      .addCase(registerUser.fulfilled, onFulfilled)
      .addCase(registerUser.rejected, onRejected)
      .addCase(loginUser.pending, onPending)
      .addCase(loginUser.fulfilled, onFulfilled)
      .addCase(loginUser.rejected, onRejected);
  },
});

export const { logout, clearAuthError } = authSlice.actions;
export const selectCurrentUser = (state) => state.auth.currentUser;

const normalizeRoles = (user) =>
  (user?.roles || [])
    .map((r) => (r == null ? '' : String(r).trim().toLowerCase()))
    .filter((r) => r.length > 0);

export const isCustomer = (user) => normalizeRoles(user).includes('customer');
export const isEmployee = (user) => {
  const roles = normalizeRoles(user);
  if (roles.length === 0) return false;
  return roles.some((r) => r !== 'customer');
};
export const hasRole = (user, role) => {
  const expected = String(role || '').toLowerCase();
  if (expected === 'customer') return isCustomer(user);
  if (expected === 'employee') return isEmployee(user);
  return isEmployee(user);
};

export default authSlice.reducer;
