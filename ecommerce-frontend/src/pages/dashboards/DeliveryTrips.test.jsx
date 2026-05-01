import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchDeliveryTrips: vi.fn(),
  markTripOutForDelivery: vi.fn(),
  markTripDelivered: vi.fn(),
  markTripFailed: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import DeliveryTrips from './DeliveryTrips.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const driver = {
  userid: 7,
  name: 'Dan',
  roles: ['delivery'],
  departments: ['delivery'],
};

const tripPickedUp = {
  id: 10,
  status: 'picked_up',
  orderId: 101,
  customerName: 'Alice',
  customerPhone: '5551234',
  shippingAddress: '123 Main St',
  otpCode: null,
};
const tripOut = {
  id: 11,
  status: 'out_for_delivery',
  orderId: 102,
  customerName: 'Bob',
  customerPhone: null,
  shippingAddress: '99 Oak',
  otpCode: '4321',
};

function renderAsDriver() {
  return renderWithProviders(<DeliveryTrips />, {
    preloadedState: {
      auth: { currentUser: driver, status: 'idle', error: null },
    },
  });
}

describe('DeliveryTrips', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads active trips for the driver on mount', async () => {
    api.fetchDeliveryTrips.mockResolvedValue([tripPickedUp, tripOut]);
    renderAsDriver();
    await waitFor(() =>
      expect(api.fetchDeliveryTrips).toHaveBeenCalledWith(7, 'active')
    );
    expect(await screen.findByText('Alice')).toBeInTheDocument();
    expect(screen.getByText('Bob')).toBeInTheDocument();
    expect(screen.getByText('4321')).toBeInTheDocument();
  });

  it('shows empty state when no trips', async () => {
    api.fetchDeliveryTrips.mockResolvedValue([]);
    renderAsDriver();
    expect(await screen.findByText(/No trips to show/i)).toBeInTheDocument();
  });

  it('shows error when fetch fails', async () => {
    api.fetchDeliveryTrips.mockRejectedValue({
      response: { data: { error: 'oops' } },
    });
    renderAsDriver();
    expect(await screen.findByText('oops')).toBeInTheDocument();
  });

  it('shows tel: link when customer phone is present', async () => {
    api.fetchDeliveryTrips.mockResolvedValue([tripPickedUp]);
    renderAsDriver();
    const callLink = await screen.findByTestId('trip-call-10');
    expect(callLink).toHaveAttribute('href', 'tel:5551234');
  });

  it('marks trip out for delivery (positive)', async () => {
    api.fetchDeliveryTrips
      .mockResolvedValueOnce([tripPickedUp])
      .mockResolvedValueOnce([{ ...tripPickedUp, status: 'out_for_delivery', otpCode: '1111' }]);
    api.markTripOutForDelivery.mockResolvedValue({});
    renderAsDriver();

    const out = await screen.findByTestId('trip-out-10');
    await userEvent.click(out);

    await waitFor(() =>
      expect(api.markTripOutForDelivery).toHaveBeenCalledWith(10, 7)
    );
  });

  it('marks delivered with OTP and COD', async () => {
    api.fetchDeliveryTrips.mockResolvedValueOnce([tripOut]);
    api.markTripDelivered.mockResolvedValue({});
    renderAsDriver();

    const btn = await screen.findByTestId('trip-deliver-11');
    await userEvent.click(btn);

    await userEvent.type(screen.getByLabelText('deliver-otp'), '4321');
    await userEvent.type(screen.getByLabelText('deliver-cod'), '250');
    await userEvent.type(screen.getByLabelText('deliver-tip'), '20');
    await userEvent.type(screen.getByLabelText('deliver-distance'), '3.5');
    await userEvent.click(screen.getByRole('button', { name: /Mark delivered/i }));

    await waitFor(() =>
      expect(api.markTripDelivered).toHaveBeenCalledWith(
        11,
        expect.objectContaining({
          driverId: 7,
          otp: '4321',
          codAmount: 250,
          tipAmount: 20,
          distanceKm: 3.5,
        })
      )
    );
  });

  it('shows backend error on deliver if OTP wrong', async () => {
    api.fetchDeliveryTrips.mockResolvedValueOnce([tripOut]);
    api.markTripDelivered.mockRejectedValue({
      response: { data: { error: 'OTP does not match' } },
    });
    renderAsDriver();

    await userEvent.click(await screen.findByTestId('trip-deliver-11'));
    await userEvent.type(screen.getByLabelText('deliver-otp'), '0000');
    await userEvent.click(screen.getByRole('button', { name: /Mark delivered/i }));

    expect(await screen.findByText('OTP does not match')).toBeInTheDocument();
  });

  it('marks trip failed with reason and notes', async () => {
    api.fetchDeliveryTrips.mockResolvedValueOnce([tripOut]);
    api.markTripFailed.mockResolvedValue({});
    renderAsDriver();

    await userEvent.click(await screen.findByTestId('trip-fail-11'));
    await userEvent.type(screen.getByLabelText('fail-notes'), 'no answer');
    await userEvent.click(screen.getByRole('button', { name: /Mark failed/i }));

    await waitFor(() =>
      expect(api.markTripFailed).toHaveBeenCalledWith(
        11,
        expect.objectContaining({
          driverId: 7,
          reason: 'customer_not_home',
          notes: 'no answer',
        })
      )
    );
  });

  it('shows backend error on fail submission', async () => {
    api.fetchDeliveryTrips.mockResolvedValueOnce([tripOut]);
    api.markTripFailed.mockRejectedValue({
      response: { data: { error: 'already delivered' } },
    });
    renderAsDriver();
    await userEvent.click(await screen.findByTestId('trip-fail-11'));
    await userEvent.click(screen.getByRole('button', { name: /Mark failed/i }));
    expect(await screen.findByText('already delivered')).toBeInTheDocument();
  });

  it('filter dropdown reloads with selected status', async () => {
    api.fetchDeliveryTrips.mockResolvedValue([]);
    renderAsDriver();
    await waitFor(() =>
      expect(api.fetchDeliveryTrips).toHaveBeenCalledWith(7, 'active')
    );

    await userEvent.click(screen.getByLabelText('trip-status-filter'));
    const delivered = await screen.findByRole('option', { name: 'Delivered' });
    await userEvent.click(delivered);

    await waitFor(() =>
      expect(api.fetchDeliveryTrips).toHaveBeenLastCalledWith(7, 'delivered')
    );
  });
});
