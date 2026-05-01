import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchManagementOps: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementLiveOps from './ManagementLiveOps.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 1,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementLiveOps />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementLiveOps', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders kitchen counts, delivery in-flight stats, and breaches', async () => {
    api.fetchManagementOps.mockResolvedValue({
      kitchenQueue: {
        online: { pending: 1, preparing: 2, ready: 0, done: 0,
          picked_up: 0, out_for_delivery: 0 },
        instore: { pending: 0, preparing: 0, ready: 0, done: 1,
          picked_up: 0, out_for_delivery: 0 },
      },
      deliveryInFlight: { pickedUp: 1, outForDelivery: 2, total: 3 },
      breaches: [
        { type: 'kitchen', orderId: 50, status: 'preparing', channel: 'online',
          ageMinutes: 45, customerName: 'Alice' },
        { type: 'delivery', orderId: 60, tripId: 11, status: 'out_for_delivery',
          ageMinutes: 75, driverName: 'Dan' },
      ],
      kitchenSlaMinutes: 30,
      deliverySlaMinutes: 60,
      asOf: '2026-04-28T10:00:00',
    });
    renderAsManager();

    await waitFor(() => expect(api.fetchManagementOps).toHaveBeenCalled());
    expect(await screen.findByTestId('kitchen-online-preparing')).toHaveTextContent('preparing · 2');
    expect(screen.getByTestId('kitchen-instore-done')).toHaveTextContent('done · 1');
    expect(screen.getByTestId('breach-kitchen-50')).toBeInTheDocument();
    expect(screen.getByTestId('breach-delivery-60')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Dan')).toBeInTheDocument();
  });

  it('shows green message when no breaches', async () => {
    api.fetchManagementOps.mockResolvedValue({
      kitchenQueue: { online: {}, instore: {} },
      deliveryInFlight: { pickedUp: 0, outForDelivery: 0, total: 0 },
      breaches: [],
      kitchenSlaMinutes: 30,
      deliverySlaMinutes: 60,
    });
    renderAsManager();
    expect(
      await screen.findByText(/Nothing running late/i)
    ).toBeInTheDocument();
  });

  it('shows error when fetch fails', async () => {
    api.fetchManagementOps.mockRejectedValue({
      response: { data: { error: 'oops' } },
    });
    renderAsManager();
    expect(await screen.findByText('oops')).toBeInTheDocument();
  });

  it('refresh button reloads', async () => {
    api.fetchManagementOps.mockResolvedValue({
      kitchenQueue: { online: {}, instore: {} },
      deliveryInFlight: { pickedUp: 0, outForDelivery: 0, total: 0 },
      breaches: [],
      kitchenSlaMinutes: 30,
      deliverySlaMinutes: 60,
    });
    renderAsManager();
    await waitFor(() => expect(api.fetchManagementOps).toHaveBeenCalledTimes(1));
    await userEvent.click(screen.getByRole('button', { name: /Refresh/i }));
    await waitFor(() => expect(api.fetchManagementOps).toHaveBeenCalledTimes(2));
  });
});
