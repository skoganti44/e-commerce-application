import { configureStore } from '@reduxjs/toolkit';
import sessionReducer from './slices/sessionSlice.js';
import authReducer from './slices/authSlice.js';
import usersReducer from './slices/usersSlice.js';
import productsReducer from './slices/productsSlice.js';
import cartReducer from './slices/cartSlice.js';
import paymentsReducer from './slices/paymentsSlice.js';

export const store = configureStore({
  reducer: {
    session: sessionReducer,
    auth: authReducer,
    users: usersReducer,
    products: productsReducer,
    cart: cartReducer,
    payments: paymentsReducer,
  },
});
