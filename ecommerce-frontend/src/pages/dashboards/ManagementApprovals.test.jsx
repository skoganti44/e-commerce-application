import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchOrdersPendingApproval: vi.fn(),
  flagOrderForApproval: vi.fn(),
  decideOrderApproval: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementApprovals from './ManagementApprovals.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 99,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

const pendingOrder = {
  orderId: 200,
  customerName: 'Acme Corp',
  channel: 'online',
  totalAmount: 5000,
  approvalStatus: 'pending',
  approvalNotes: '5000 cookies for ABC',
};

function renderAsManager() {
  return renderWithProviders(<ManagementApprovals />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementApprovals', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads orders pending approval on mount', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([pendingOrder]);
    renderAsManager();
    await waitFor(() => expect(api.fetchOrdersPendingApproval).toHaveBeenCalled());
    expect(await screen.findByTestId('approval-row-200')).toBeInTheDocument();
    expect(screen.getByText('Acme Corp')).toBeInTheDocument();
    expect(screen.getByText('5000 cookies for ABC')).toBeInTheDocument();
  });

  it('shows green message when nothing pending', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([]);
    renderAsManager();
    expect(
      await screen.findByText(/No orders awaiting sign-off/i)
    ).toBeInTheDocument();
  });

  it('shows backend error', async () => {
    api.fetchOrdersPendingApproval.mockRejectedValue({
      response: { data: { error: 'oops' } },
    });
    renderAsManager();
    expect(await screen.findByText('oops')).toBeInTheDocument();
  });

  it('approves an order with notes', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([pendingOrder]);
    api.decideOrderApproval.mockResolvedValue({});
    renderAsManager();

    await userEvent.click(await screen.findByTestId('approval-approve-200'));
    await userEvent.type(screen.getByLabelText('approval-decide-notes'), 'go ahead');
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() =>
      expect(api.decideOrderApproval).toHaveBeenCalledWith(
        200,
        expect.objectContaining({
          managerUserId: 99,
          decision: 'approved',
          notes: 'go ahead',
        })
      )
    );
  });

  it('rejects an order and surfaces backend error', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([pendingOrder]);
    api.decideOrderApproval.mockRejectedValue({
      response: { data: { error: 'already decided' } },
    });
    renderAsManager();
    await userEvent.click(await screen.findByTestId('approval-reject-200'));
    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(await screen.findByText('already decided')).toBeInTheDocument();
  });

  it('flags a new order for approval', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([]);
    api.flagOrderForApproval.mockResolvedValue({});
    renderAsManager();

    await screen.findByText(/No orders/i);
    await userEvent.click(screen.getByRole('button', { name: /Flag corporate order/i }));
    await userEvent.type(screen.getByLabelText('flag-order'), '301');
    await userEvent.type(screen.getByLabelText('flag-notes'), 'large catering');
    await userEvent.click(screen.getByRole('button', { name: 'Flag' }));

    await waitFor(() =>
      expect(api.flagOrderForApproval).toHaveBeenCalledWith(301, 'large catering')
    );
  });

  it('blocks flag when order # missing', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([]);
    renderAsManager();
    await screen.findByText(/No orders/i);
    await userEvent.click(screen.getByRole('button', { name: /Flag corporate order/i }));
    await userEvent.click(screen.getByRole('button', { name: 'Flag' }));
    expect(await screen.findByText(/Order # is required/i)).toBeInTheDocument();
    expect(api.flagOrderForApproval).not.toHaveBeenCalled();
  });

  it('shows backend error on flag failure', async () => {
    api.fetchOrdersPendingApproval.mockResolvedValue([]);
    api.flagOrderForApproval.mockRejectedValue({
      response: { data: { error: 'order not found' } },
    });
    renderAsManager();
    await screen.findByText(/No orders/i);
    await userEvent.click(screen.getByRole('button', { name: /Flag corporate order/i }));
    await userEvent.type(screen.getByLabelText('flag-order'), '999');
    await userEvent.click(screen.getByRole('button', { name: 'Flag' }));
    expect(await screen.findByText('order not found')).toBeInTheDocument();
  });
});
