import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../api/endpoints.js', () => ({
  loginUser: vi.fn(),
  registerUser: vi.fn(),
}));

import * as api from '../api/endpoints.js';
import Login from './Login.jsx';
import { renderWithProviders } from '../test/renderWithProviders.jsx';

describe('Login', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders customer heading by default', () => {
    renderWithProviders(<Login />);
    expect(
      screen.getByRole('heading', { name: /customer login/i })
    ).toBeInTheDocument();
    expect(
      screen.getByText(/sign in to your customer account/i)
    ).toBeInTheDocument();
    expect(
      screen.getByRole('link', { name: /create an account/i })
    ).toBeInTheDocument();
  });

  it('renders employee heading when type=employee and hides register link', () => {
    renderWithProviders(<Login />, { route: '/login?type=employee' });
    expect(
      screen.getByRole('heading', { name: /employee login/i })
    ).toBeInTheDocument();
    expect(
      screen.queryByRole('link', { name: /create an account/i })
    ).not.toBeInTheDocument();
  });

  it('submits credentials and calls the loginUser api', async () => {
    api.loginUser.mockResolvedValue({
      userid: 7,
      name: 'Jane',
      roles: ['customer'],
      departments: [],
    });
    const user = userEvent.setup();
    renderWithProviders(<Login />);

    await user.type(screen.getByLabelText(/email/i), 'jane@example.com');
    await user.type(screen.getByLabelText(/password/i), 'pw12345');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(api.loginUser).toHaveBeenCalledTimes(1));
    expect(api.loginUser).toHaveBeenCalledWith({
      email: 'jane@example.com',
      password: 'pw12345',
    });
  });

  it('shows the api error when login fails', async () => {
    api.loginUser.mockRejectedValue(new Error('Invalid credentials'));
    const user = userEvent.setup();
    renderWithProviders(<Login />);

    await user.type(screen.getByLabelText(/email/i), 'wrong@example.com');
    await user.type(screen.getByLabelText(/password/i), 'badpw');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(await screen.findByText(/invalid credentials/i)).toBeInTheDocument();
  });

  it('rejects a customer account trying to use employee login', async () => {
    api.loginUser.mockResolvedValue({
      userid: 3,
      name: 'Cathy',
      roles: ['customer'],
      departments: [],
    });
    const user = userEvent.setup();
    renderWithProviders(<Login />, { route: '/login?type=employee' });

    await user.type(screen.getByLabelText(/email/i), 'cathy@example.com');
    await user.type(screen.getByLabelText(/password/i), 'pw');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    expect(
      await screen.findByText(/not registered as employee/i)
    ).toBeInTheDocument();
  });

  it('accepts a non-customer role as employee (role rule)', async () => {
    api.loginUser.mockResolvedValue({
      userid: 11,
      name: 'Ken',
      roles: ['baker'],
      departments: ['kitchen'],
    });
    const user = userEvent.setup();
    renderWithProviders(<Login />, { route: '/login?type=employee' });

    await user.type(screen.getByLabelText(/email/i), 'ken@example.com');
    await user.type(screen.getByLabelText(/password/i), 'pw');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => expect(api.loginUser).toHaveBeenCalled());
    expect(
      screen.queryByText(/not registered as employee/i)
    ).not.toBeInTheDocument();
  });
});
