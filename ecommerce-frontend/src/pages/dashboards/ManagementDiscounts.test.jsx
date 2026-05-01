import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchDiscountCampaigns: vi.fn(),
  proposeDiscountCampaign: vi.fn(),
  decideDiscountCampaign: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementDiscounts from './ManagementDiscounts.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 99,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

const pendingCampaign = {
  id: 50,
  name: '20% off Cakes',
  categoryFilter: 'Cakes',
  discountPercent: 20,
  startsOn: '2026-04-28',
  endsOn: '2026-04-30',
  status: 'pending',
  proposedByName: 'Sara',
  decidedByName: null,
};

function renderAsManager() {
  return renderWithProviders(<ManagementDiscounts />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementDiscounts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads pending campaigns by default', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([pendingCampaign]);
    renderAsManager();
    await waitFor(() =>
      expect(api.fetchDiscountCampaigns).toHaveBeenCalledWith('pending')
    );
    expect(await screen.findByTestId('discount-row-50')).toBeInTheDocument();
    expect(screen.getByText('20% off Cakes')).toBeInTheDocument();
  });

  it('shows empty state', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([]);
    renderAsManager();
    expect(await screen.findByText(/No campaigns/i)).toBeInTheDocument();
  });

  it('shows backend error', async () => {
    api.fetchDiscountCampaigns.mockRejectedValue({
      response: { data: { error: 'oops' } },
    });
    renderAsManager();
    expect(await screen.findByText('oops')).toBeInTheDocument();
  });

  it('approves a pending campaign', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([pendingCampaign]);
    api.decideDiscountCampaign.mockResolvedValue({});
    renderAsManager();

    await userEvent.click(await screen.findByTestId('discount-approve-50'));
    await userEvent.type(screen.getByLabelText('discount-decide-notes'), 'go');
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));

    await waitFor(() =>
      expect(api.decideDiscountCampaign).toHaveBeenCalledWith(
        50,
        expect.objectContaining({
          managerUserId: 99,
          decision: 'approved',
          notes: 'go',
        })
      )
    );
  });

  it('rejects a campaign and surfaces backend error', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([pendingCampaign]);
    api.decideDiscountCampaign.mockRejectedValue({
      response: { data: { error: 'already decided' } },
    });
    renderAsManager();
    await userEvent.click(await screen.findByTestId('discount-reject-50'));
    await userEvent.click(screen.getByRole('button', { name: 'Reject' }));
    expect(await screen.findByText('already decided')).toBeInTheDocument();
  });

  it('proposes a new campaign', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([]);
    api.proposeDiscountCampaign.mockResolvedValue({ id: 7 });
    renderAsManager();

    await screen.findByText(/No campaigns/i);
    await userEvent.click(screen.getByRole('button', { name: /Propose campaign/i }));

    await userEvent.type(screen.getByLabelText('campaign-name'), 'Diwali special');
    await userEvent.type(screen.getByLabelText('campaign-percent'), '15');
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));

    await waitFor(() =>
      expect(api.proposeDiscountCampaign).toHaveBeenCalledWith(
        expect.objectContaining({
          name: 'Diwali special',
          discountPercent: 15,
          proposedByUserId: 99,
        })
      )
    );
  });

  it('blocks propose when name is empty', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([]);
    renderAsManager();
    await screen.findByText(/No campaigns/i);
    await userEvent.click(screen.getByRole('button', { name: /Propose campaign/i }));
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(await screen.findByText(/Name is required/i)).toBeInTheDocument();
    expect(api.proposeDiscountCampaign).not.toHaveBeenCalled();
  });

  it('blocks propose when percent is empty', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([]);
    renderAsManager();
    await screen.findByText(/No campaigns/i);
    await userEvent.click(screen.getByRole('button', { name: /Propose campaign/i }));
    await userEvent.type(screen.getByLabelText('campaign-name'), 'X');
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(await screen.findByText(/Discount % is required/i)).toBeInTheDocument();
  });

  it('shows backend error on propose failure', async () => {
    api.fetchDiscountCampaigns.mockResolvedValue([]);
    api.proposeDiscountCampaign.mockRejectedValue({
      response: { data: { error: 'invalid percent' } },
    });
    renderAsManager();
    await screen.findByText(/No campaigns/i);
    await userEvent.click(screen.getByRole('button', { name: /Propose campaign/i }));
    await userEvent.type(screen.getByLabelText('campaign-name'), 'X');
    await userEvent.type(screen.getByLabelText('campaign-percent'), '5');
    await userEvent.click(screen.getByRole('button', { name: /Submit/i }));
    expect(await screen.findByText('invalid percent')).toBeInTheDocument();
  });
});
