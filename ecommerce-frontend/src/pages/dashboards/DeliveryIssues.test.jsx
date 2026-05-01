import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchDeliveryIssues: vi.fn(),
  logDeliveryIssue: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import DeliveryIssues from './DeliveryIssues.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const driver = {
  userid: 7,
  name: 'Dan',
  roles: ['delivery'],
  departments: ['delivery'],
};

function renderAsDriver() {
  return renderWithProviders(<DeliveryIssues />, {
    preloadedState: {
      auth: { currentUser: driver, status: 'idle', error: null },
    },
  });
}

describe('DeliveryIssues', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads issues for the driver', async () => {
    api.fetchDeliveryIssues.mockResolvedValue([
      {
        id: 1,
        issueType: 'vehicle_breakdown',
        description: 'flat tire',
        tripId: null,
        reportedAt: '2026-04-28T10:00:00',
      },
    ]);
    renderAsDriver();
    await waitFor(() =>
      expect(api.fetchDeliveryIssues).toHaveBeenCalledWith(7)
    );
    expect(await screen.findByText('flat tire')).toBeInTheDocument();
  });

  it('shows empty state when none', async () => {
    api.fetchDeliveryIssues.mockResolvedValue([]);
    renderAsDriver();
    expect(await screen.findByText(/No issues logged yet/i)).toBeInTheDocument();
  });

  it('shows backend error when load fails', async () => {
    api.fetchDeliveryIssues.mockRejectedValue({
      response: { data: { error: 'boom' } },
    });
    renderAsDriver();
    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('logs a new issue with optional trip link', async () => {
    api.fetchDeliveryIssues.mockResolvedValue([]);
    api.logDeliveryIssue.mockResolvedValue({ id: 5 });
    renderAsDriver();

    await screen.findByLabelText('issue-desc');
    await userEvent.type(
      screen.getByLabelText('issue-desc'),
      'tire blew out near sector 5'
    );
    await userEvent.type(screen.getByLabelText('issue-trip'), '12');
    await userEvent.click(screen.getByRole('button', { name: /Log issue/i }));

    await waitFor(() =>
      expect(api.logDeliveryIssue).toHaveBeenCalledWith({
        driverId: 7,
        issueType: 'vehicle_breakdown',
        description: 'tire blew out near sector 5',
        tripId: 12,
      })
    );
  });

  it('rejects blank description client-side', async () => {
    api.fetchDeliveryIssues.mockResolvedValue([]);
    renderAsDriver();
    await screen.findByLabelText('issue-desc');
    await userEvent.click(screen.getByRole('button', { name: /Log issue/i }));
    expect(
      await screen.findByText(/Description is required/i)
    ).toBeInTheDocument();
    expect(api.logDeliveryIssue).not.toHaveBeenCalled();
  });

  it('shows backend error when log fails', async () => {
    api.fetchDeliveryIssues.mockResolvedValue([]);
    api.logDeliveryIssue.mockRejectedValue({
      response: { data: { error: 'too long' } },
    });
    renderAsDriver();
    await screen.findByLabelText('issue-desc');
    await userEvent.type(screen.getByLabelText('issue-desc'), 'x');
    await userEvent.click(screen.getByRole('button', { name: /Log issue/i }));
    expect(await screen.findByText('too long')).toBeInTheDocument();
  });
});
