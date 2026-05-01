import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchManagementOrdersAudit: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementOrdersAudit from './ManagementOrdersAudit.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 1,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementOrdersAudit />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

const sampleResponse = {
  from: '2026-04-28',
  to: '2026-04-28',
  count: 2,
  totalRevenue: 350,
  revenueByChannel: { online: 100, instore: 250 },
  revenueByPaymentMethod: { upi: 100, cash: 250 },
  countByPaymentMethod: { upi: 1, cash: 1 },
  orders: [
    {
      orderId: 1,
      customerName: 'Alice',
      channel: 'online',
      status: 'delivered',
      kitchenStatus: 'delivered',
      paymentMethod: 'upi',
      paymentStatus: 'captured',
      totalAmount: 100,
      createdAt: '2026-04-28T10:00:00',
    },
    {
      orderId: 2,
      customerName: 'Bob',
      channel: 'instore',
      status: 'completed',
      kitchenStatus: null,
      paymentMethod: 'cash',
      paymentStatus: 'captured',
      totalAmount: 250,
      createdAt: '2026-04-28T11:00:00',
    },
  ],
};

describe('ManagementOrdersAudit', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads orders + summary tiles + payment chips on mount', async () => {
    api.fetchManagementOrdersAudit.mockResolvedValue(sampleResponse);
    renderAsManager();
    await waitFor(() => expect(api.fetchManagementOrdersAudit).toHaveBeenCalled());
    expect(await screen.findByTestId('order-row-1')).toBeInTheDocument();
    expect(screen.getByTestId('order-row-2')).toBeInTheDocument();
    expect(screen.getByTestId('pm-cash')).toHaveTextContent('cash · $250.00');
    expect(screen.getByTestId('pm-upi')).toHaveTextContent('upi · $100.00');
    expect(screen.getByTestId('ch-online')).toHaveTextContent('online · $100.00');
  });

  it('reloads with channel + payment filter on Apply', async () => {
    api.fetchManagementOrdersAudit.mockResolvedValue(sampleResponse);
    renderAsManager();
    await waitFor(() => expect(api.fetchManagementOrdersAudit).toHaveBeenCalledTimes(1));
    api.fetchManagementOrdersAudit.mockClear();

    await userEvent.click(screen.getByLabelText('audit-channel'));
    await userEvent.click(await screen.findByRole('option', { name: 'Online' }));
    await userEvent.click(screen.getByLabelText('audit-payment'));
    await userEvent.click(await screen.findByRole('option', { name: 'Cash' }));
    await userEvent.click(screen.getByRole('button', { name: /Apply/i }));

    await waitFor(() =>
      expect(api.fetchManagementOrdersAudit).toHaveBeenLastCalledWith(
        expect.objectContaining({ channel: 'online', paymentMethod: 'cash' })
      )
    );
  });

  it('shows empty state when no orders', async () => {
    api.fetchManagementOrdersAudit.mockResolvedValue({
      ...sampleResponse,
      count: 0,
      orders: [],
    });
    renderAsManager();
    expect(await screen.findByText(/No orders match the filter/i)).toBeInTheDocument();
  });

  it('shows error when load fails', async () => {
    api.fetchManagementOrdersAudit.mockRejectedValue({
      response: { data: { error: 'bad date' } },
    });
    renderAsManager();
    expect(await screen.findByText('bad date')).toBeInTheDocument();
  });
});
