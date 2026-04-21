import { createSlice } from '@reduxjs/toolkit';

const STORAGE_KEY = 'ecommerce.activeUserId';

const loadInitialUserId = () => {
  const raw = localStorage.getItem(STORAGE_KEY);
  const n = raw ? Number(raw) : NaN;
  return Number.isFinite(n) && n > 0 ? n : null;
};

const sessionSlice = createSlice({
  name: 'session',
  initialState: { activeUserId: loadInitialUserId() },
  reducers: {
    setActiveUserId(state, action) {
      const id = Number(action.payload);
      state.activeUserId = Number.isFinite(id) && id > 0 ? id : null;
      if (state.activeUserId) {
        localStorage.setItem(STORAGE_KEY, String(state.activeUserId));
      } else {
        localStorage.removeItem(STORAGE_KEY);
      }
    },
  },
});

export const { setActiveUserId } = sessionSlice.actions;
export const selectActiveUserId = (state) => state.session.activeUserId;
export default sessionSlice.reducer;
