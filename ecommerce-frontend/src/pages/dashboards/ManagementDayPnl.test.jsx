import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchManagementDayPnl: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementDayPnl from './ManagementDayPnl.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 1,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementDayPnl />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementDayPnl', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows revenue tiles + payment method chips', async () => {
    api.fetchManagementDayPnl.mockResolvedValue({
      date: '2026-04-28',
      orderCount: 5,
      onlineRevenue: 200,
      counterRevenue: 300,
      totalRevenue: 500,
      revenueByPaymentMethod: { cash: 300, upi: 100, card: 100 },
      codCollected: 200,
      tipsCollected: 30,
      grossInflow: 530,
      net: 530,
    });
    renderAsManager();

    expect(await screen.findByText('$500.00')).toBeInTheDocument();
    expect(screen.getByText('$530.00')).toBeInTheDocument();
    expect(screen.getByTestId('pnl-pm-cash')).toHaveTextContent('cash · $300.00');
    expect(screen.getByTestId('pnl-pm-upi')).toHaveTextContent('upi · $100.00');
  });

  it('reloads when a different date is applied', async () => {
    api.fetchManagementDayPnl.mockResolvedValue({
      date: '2026-04-28',
      orderCount: 0,
      onlineRevenue: 0,
      counterRevenue: 0,
      totalRevenue: 0,
      revenueByPaymentMethod: {},
      codCollected: 0,
      tipsCollected: 0,
      grossInflow: 0,
      net: 0,
    });
    renderAsManager();
    await waitFor(() => expect(api.fetchManagementDayPnl).toHaveBeenCalled());
    api.fetchManagementDayPnl.mockClear();

    const dateInput = screen.getByLabelText('pnl-date');
    await userEvent.clear(dateInput);
    await userEvent.type(dateInput, '2026-04-27');
    await userEvent.click(screen.getByRole('button', { name: /Apply/i }));

    await waitFor(() =>
      expect(api.fetchManagementDayPnl).toHaveBeenLastCalledWith('2026-04-27')
    );
  });

  it('shows backend error', async () => {
    api.fetchManagementDayPnl.mockRejectedValue({
      response: { data: { error: 'bad date' } },
    });
    renderAsManager();
    expect(await screen.findByText('bad date')).toBeInTheDocument();
  });
});
