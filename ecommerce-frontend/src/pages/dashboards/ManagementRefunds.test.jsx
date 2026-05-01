import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchRefundRequests: vi.fn(),
  raiseRefundRequest: vi.fn(),
  decideRefundRequest: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementRefunds from './ManagementRefunds.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 99,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

const pending = {
  id: 1,
  orderId: 100,
  customerName: 'Asha',
  requestType: 'refund',
  reason: 'Cake arrived damaged',
  amount: 500,
  status: 'pending',
  raisedByName: 'Asha',
  decidedByName: null,
};

function renderAsManager() {
  return renderWithProviders(<ManagementRefunds />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementRefunds', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads pending refund requests by default', async () => {
    api.fetchRefundRequests.mockResolvedValue([pending]);
    renderAsManager();
    await waitFor(() =>
      expect(api.fetchRefundRequests).toHaveBeenCalledWith('pending')
    );
    expect(await screen.findByTestId('refund-row-1')).toBeInTheDocument();
    expect(screen.getByText('Cake arrived damaged')).toBeInTheDocument();
  });

  it('shows empty state when no requests', async () => {
    api.fetchRefundRequests.mockResolvedValue([]);
    renderAsManager();
    expect(await screen.findByText(/No requests in this view/i)).toBeInTheDocument();
  });

  it('shows backend error', async () => {
    api.fetchRefundRequests.mockRejectedValue({
      response: { data: { error: 'oops' } },
    });
    renderAsManager();
    expect(await screen.findByText('oops')).toBeInTheDocument();
  });

  it('approves a pending request with notes', async () => {
    api.fetchRefundRequests.mockResolvedValue([pending]);
    api.decideRefundRequest.mockResolvedValue({});
    renderAsManager();

    await userEvent.click(await screen.findByTestId('refund-approve-1'));
    await userEvent.type(screen.getByLabelText('decide-notes'), 'ok');
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() =>
      expect(api.decideRefundRequest).toHaveBeenCalledWith(
        1,
        expect.objectContaining({
          managerUserId: 99,
          decision: 'approved',
          notes: 'ok',
        })
      )
    );
  });

  it('rejects a request and surfaces backend error', async () => {
    api.fetchRefundRequests.mockResolvedValue([pending]);
    api.decideRefundRequest.mockRejectedValue({
      response: { data: { error: 'already decided' } },
    });
    renderAsManager();
    await userEvent.click(await screen.findByTestId('refund-reject-1'));
    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(await screen.findByText('already decided')).toBeInTheDocument();
  });

  it('raises a new refund request', async () => {
    api.fetchRefundRequests.mockResolvedValue([]);
    api.raiseRefundRequest.mockResolvedValue({ id: 5 });
    renderAsManager();

    await screen.findByText(/No requests/i);
    await userEvent.click(screen.getByRole('button', { name: /Raise request/i }));

    await userEvent.type(screen.getByLabelText('raise-order'), '101');
    await userEvent.type(screen.getByLabelText('raise-reason'), 'wrong delivery');
    await userEvent.type(screen.getByLabelText('raise-amount'), '250');
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));

    await waitFor(() =>
      expect(api.raiseRefundRequest).toHaveBeenCalledWith({
        orderId: 101,
        raisedByUserId: 99,
        requestType: 'refund',
        reason: 'wrong delivery',
        amount: 250,
      })
    );
  });

  it('blocks raise with empty order #', async () => {
    api.fetchRefundRequests.mockResolvedValue([]);
    renderAsManager();
    await screen.findByText(/No requests/i);
    await userEvent.click(screen.getByRole('button', { name: /Raise request/i }));
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(await screen.findByText(/Order # is required/i)).toBeInTheDocument();
    expect(api.raiseRefundRequest).not.toHaveBeenCalled();
  });

  it('blocks raise with empty reason', async () => {
    api.fetchRefundRequests.mockResolvedValue([]);
    renderAsManager();
    await screen.findByText(/No requests/i);
    await userEvent.click(screen.getByRole('button', { name: /Raise request/i }));
    await userEvent.type(screen.getByLabelText('raise-order'), '101');
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(await screen.findByText(/Reason is required/i)).toBeInTheDocument();
  });

  it('reloads when filter changes', async () => {
    api.fetchRefundRequests.mockResolvedValue([]);
    renderAsManager();
    await waitFor(() => expect(api.fetchRefundRequests).toHaveBeenCalledTimes(1));
    api.fetchRefundRequests.mockClear();

    await userEvent.click(screen.getByLabelText('refund-filter'));
    await userEvent.click(await screen.findByRole('option', { name: 'Approved' }));
    await waitFor(() =>
      expect(api.fetchRefundRequests).toHaveBeenLastCalledWith('approved')
    );
  });
});
