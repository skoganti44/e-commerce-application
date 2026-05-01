import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchDeliveryOnlineOrders: vi.fn(),
  pickUpDeliveryTrip: vi.fn(),
  updateKitchenOrderStatus: vi.fn(),
  fetchTasks: vi.fn(),
  updateTaskStatus: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import DeliveryDashboard from './DeliveryDashboard.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const driver = {
  userid: 7,
  name: 'Dan',
  roles: ['delivery'],
  departments: ['delivery'],
};

const sampleOrder = {
  orderId: 101,
  customerName: 'Alice',
  totalAmount: 250,
  createdAt: '2026-04-28T10:00:00',
  items: [{ productName: 'Bun', quantity: 2 }],
};

function renderAsDriver() {
  api.fetchTasks.mockResolvedValue([]);
  return renderWithProviders(<DeliveryDashboard />, {
    preloadedState: {
      auth: { currentUser: driver, status: 'idle', error: null },
    },
  });
}

describe('DeliveryDashboard pickup', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows the ready queue and a Pick up button', async () => {
    api.fetchDeliveryOnlineOrders.mockResolvedValue([sampleOrder]);
    renderAsDriver();
    expect(await screen.findByText('Alice')).toBeInTheDocument();
    expect(screen.getByTestId('pickup-101')).toBeInTheDocument();
  });

  it('picks up an order and removes it from the queue', async () => {
    api.fetchDeliveryOnlineOrders.mockResolvedValue([sampleOrder]);
    api.pickUpDeliveryTrip.mockResolvedValue({ id: 500 });
    renderAsDriver();

    await userEvent.click(await screen.findByTestId('pickup-101'));

    await waitFor(() =>
      expect(api.pickUpDeliveryTrip).toHaveBeenCalledWith(101, 7)
    );
    await waitFor(() =>
      expect(screen.queryByText('Alice')).not.toBeInTheDocument()
    );
  });

  it('shows backend error when pickup fails', async () => {
    api.fetchDeliveryOnlineOrders.mockResolvedValue([sampleOrder]);
    api.pickUpDeliveryTrip.mockRejectedValue({
      response: { data: { error: 'kitchen not done' } },
    });
    renderAsDriver();
    await userEvent.click(await screen.findByTestId('pickup-101'));
    expect(await screen.findByText('kitchen not done')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('shows empty state when no orders are ready', async () => {
    api.fetchDeliveryOnlineOrders.mockResolvedValue([]);
    renderAsDriver();
    expect(
      await screen.findByText(/No deliveries waiting/i)
    ).toBeInTheDocument();
  });
});
