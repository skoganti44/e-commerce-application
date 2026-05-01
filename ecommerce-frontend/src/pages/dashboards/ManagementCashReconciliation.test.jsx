import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchCashReconciliation: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementCashReconciliation from './ManagementCashReconciliation.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 99,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementCashReconciliation />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementCashReconciliation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows balanced state when variance is zero', async () => {
    api.fetchCashReconciliation.mockResolvedValue({
      date: '2026-04-28',
      openingFloat: 500,
      counterCash: 100,
      counterCashCount: 2,
      counterCard: 250,
      counterUpi: 50,
      codCollected: 75,
      expectedCashInDrawer: 675,
      countedCash: 675,
      variance: 0,
      balanced: true,
    });
    renderAsManager();
    await waitFor(() => expect(api.fetchCashReconciliation).toHaveBeenCalled());

    expect(await screen.findByText('$0.00')).toBeInTheDocument();
    expect(screen.getByTestId('cash-pm-card')).toHaveTextContent('Card · $250.00');
    expect(screen.getByTestId('cash-pm-upi')).toHaveTextContent('UPI · $50.00');
  });

  it('shows negative variance when drawer is short', async () => {
    api.fetchCashReconciliation.mockResolvedValue({
      date: '2026-04-28',
      openingFloat: 0,
      counterCash: 100,
      counterCashCount: 1,
      counterCard: 0,
      counterUpi: 0,
      codCollected: 0,
      expectedCashInDrawer: 100,
      countedCash: 80,
      variance: -20,
      balanced: false,
    });
    renderAsManager();
    expect(await screen.findByText('$-20.00')).toBeInTheDocument();
  });

  it('prompts to enter counted cash when omitted', async () => {
    api.fetchCashReconciliation.mockResolvedValue({
      date: '2026-04-28',
      openingFloat: 0,
      counterCash: 0,
      counterCashCount: 0,
      counterCard: 0,
      counterUpi: 0,
      codCollected: 0,
      expectedCashInDrawer: 0,
      countedCash: null,
      variance: null,
      balanced: false,
    });
    renderAsManager();
    expect(await screen.findByText(/Enter counted cash/i)).toBeInTheDocument();
  });

  it('reloads when user clicks Reconcile', async () => {
    api.fetchCashReconciliation.mockResolvedValue({
      date: '2026-04-28',
      openingFloat: 500,
      counterCash: 0,
      counterCashCount: 0,
      counterCard: 0,
      counterUpi: 0,
      codCollected: 0,
      expectedCashInDrawer: 500,
      countedCash: null,
      variance: null,
      balanced: false,
    });
    renderAsManager();
    await waitFor(() => expect(api.fetchCashReconciliation).toHaveBeenCalledTimes(1));
    api.fetchCashReconciliation.mockClear();

    await userEvent.clear(screen.getByLabelText('cash-counted'));
    await userEvent.type(screen.getByLabelText('cash-counted'), '500');
    await userEvent.click(screen.getByRole('button', { name: /Reconcile/i }));

    await waitFor(() =>
      expect(api.fetchCashReconciliation).toHaveBeenLastCalledWith(
        expect.any(String),
        '500',
        '500'
      )
    );
  });

  it('shows backend error', async () => {
    api.fetchCashReconciliation.mockRejectedValue({
      response: { data: { error: 'bad date' } },
    });
    renderAsManager();
    expect(await screen.findByText('bad date')).toBeInTheDocument();
  });
});
