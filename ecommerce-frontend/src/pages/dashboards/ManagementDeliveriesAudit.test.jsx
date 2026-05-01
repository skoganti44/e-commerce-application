import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchManagementDeliveriesAudit: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementDeliveriesAudit from './ManagementDeliveriesAudit.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 1,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementDeliveriesAudit />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

const sample = {
  from: '2026-04-28',
  to: '2026-04-28',
  count: 2,
  delivered: 1,
  failed: 1,
  inFlight: 0,
  codCollected: 250,
  tipsTotal: 25,
  trips: [
    {
      id: 10,
      orderId: 100,
      driverName: 'Dan',
      customerName: 'Alice',
      status: 'delivered',
      codAmount: 250,
      tipAmount: 25,
      failureReason: null,
    },
    {
      id: 11,
      orderId: 101,
      driverName: 'Dan',
      customerName: 'Bob',
      status: 'failed',
      codAmount: null,
      tipAmount: null,
      failureReason: 'customer_not_home',
    },
  ],
};

describe('ManagementDeliveriesAudit', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads trips, summary, and shows failure reason', async () => {
    api.fetchManagementDeliveriesAudit.mockResolvedValue(sample);
    renderAsManager();
    expect(await screen.findByTestId('audit-trip-10')).toBeInTheDocument();
    expect(screen.getByTestId('audit-trip-11')).toBeInTheDocument();
    expect(screen.getByText('customer_not_home')).toBeInTheDocument();
  });

  it('reloads with driver + status filter applied', async () => {
    api.fetchManagementDeliveriesAudit.mockResolvedValue(sample);
    renderAsManager();
    await waitFor(() =>
      expect(api.fetchManagementDeliveriesAudit).toHaveBeenCalled()
    );
    api.fetchManagementDeliveriesAudit.mockClear();

    await userEvent.type(screen.getByLabelText('dlv-driver'), '7');
    await userEvent.click(screen.getByLabelText('dlv-status'));
    await userEvent.click(await screen.findByRole('option', { name: 'Failed' }));
    await userEvent.click(screen.getByRole('button', { name: /Apply/i }));

    await waitFor(() =>
      expect(api.fetchManagementDeliveriesAudit).toHaveBeenLastCalledWith(
        expect.objectContaining({ driverId: 7, status: 'failed' })
      )
    );
  });

  it('shows empty state', async () => {
    api.fetchManagementDeliveriesAudit.mockResolvedValue({
      ...sample,
      count: 0,
      trips: [],
    });
    renderAsManager();
    expect(await screen.findByText(/No trips match the filter/i)).toBeInTheDocument();
  });

  it('shows backend error', async () => {
    api.fetchManagementDeliveriesAudit.mockRejectedValue({
      response: { data: { error: 'date error' } },
    });
    renderAsManager();
    expect(await screen.findByText('date error')).toBeInTheDocument();
  });
});
