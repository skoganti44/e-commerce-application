import { configureStore } from '@reduxjs/toolkit';
import { Provider } from 'react-redux';
import { MemoryRouter } from 'react-router-dom';
import { render } from '@testing-library/react';

import authReducer from '../store/slices/authSlice.js';
import sessionReducer from '../store/slices/sessionSlice.js';

export function makeStore(preloaded = {}) {
  return configureStore({
    reducer: {
      auth: authReducer,
      session: sessionReducer,
    },
    preloadedState: preloaded,
  });
}

export function renderWithProviders(
  ui,
  { preloadedState, store = makeStore(preloadedState), route = '/' } = {}
) {
  const Wrapper = ({ children }) => (
    <Provider store={store}>
      <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
    </Provider>
  );
  return { store, ...render(ui, { wrapper: Wrapper }) };
}
