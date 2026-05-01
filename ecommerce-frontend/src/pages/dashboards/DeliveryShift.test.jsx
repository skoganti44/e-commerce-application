import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchDeliveryShiftSummary: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import DeliveryShift from './DeliveryShift.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const driver = {
  userid: 7,
  name: 'Dan',
  roles: ['delivery'],
  departments: ['delivery'],
};

function renderAsDriver() {
  return renderWithProviders(<DeliveryShift />, {
    preloadedState: {
      auth: { currentUser: driver, status: 'idle', error: null },
    },
  });
}

describe('DeliveryShift', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads summary for today by default', async () => {
    api.fetchDeliveryShiftSummary.mockResolvedValue({
      totalTrips: 5,
      delivered: 3,
      failed: 1,
      inFlight: 1,
      codCollected: 250,
      tipsTotal: 30,
      distanceKm: 18.5,
      failuresByReason: { customer_not_home: 1 },
    });
    renderAsDriver();

    await waitFor(() =>
      expect(api.fetchDeliveryShiftSummary).toHaveBeenCalled()
    );
    expect(await screen.findByText('5')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.getByText('$250.00')).toBeInTheDocument();
    expect(screen.getByText('$30.00')).toBeInTheDocument();
    expect(screen.getByText('18.5')).toBeInTheDocument();
    expect(screen.getByTestId('reason-customer_not_home')).toBeInTheDocument();
  });

  it('shows green message when no failures', async () => {
    api.fetchDeliveryShiftSummary.mockResolvedValue({
      totalTrips: 2,
      delivered: 2,
      failed: 0,
      inFlight: 0,
      codCollected: 100,
      tipsTotal: 0,
      distanceKm: 5,
      failuresByReason: {},
    });
    renderAsDriver();
    expect(
      await screen.findByText(/No failed trips in this range/i)
    ).toBeInTheDocument();
  });

  it('refetches when from/to date changed and Apply clicked', async () => {
    api.fetchDeliveryShiftSummary.mockResolvedValue({
      totalTrips: 0,
      delivered: 0,
      failed: 0,
      inFlight: 0,
      codCollected: 0,
      tipsTotal: 0,
      distanceKm: 0,
      failuresByReason: {},
    });
    renderAsDriver();
    await waitFor(() =>
      expect(api.fetchDeliveryShiftSummary).toHaveBeenCalled()
    );
    api.fetchDeliveryShiftSummary.mockClear();

    const fromInput = screen.getByLabelText('shift-from');
    await userEvent.clear(fromInput);
    await userEvent.type(fromInput, '2026-04-01');
    const toInput = screen.getByLabelText('shift-to');
    await userEvent.clear(toInput);
    await userEvent.type(toInput, '2026-04-27');

    await userEvent.click(screen.getByRole('button', { name: /Apply/i }));
    await waitFor(() =>
      expect(api.fetchDeliveryShiftSummary).toHaveBeenLastCalledWith(
        7,
        '2026-04-01',
        '2026-04-27'
      )
    );
  });

  it('shows error when fetch fails', async () => {
    api.fetchDeliveryShiftSummary.mockRejectedValue({
      response: { data: { error: 'bad date' } },
    });
    renderAsDriver();
    expect(await screen.findByText('bad date')).toBeInTheDocument();
  });
});
