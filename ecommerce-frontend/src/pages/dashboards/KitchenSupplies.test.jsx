import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchSupplies: vi.fn(),
  saveSupply: vi.fn(),
  bulkUpdateSupplyStatuses: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import KitchenSupplies from './KitchenSupplies.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const flour = {
  id: 1,
  name: 'Wheat Flour',
  unit: 'kg',
  category: 'flour',
  inStock: 2,
  threshold: 5,
  currentStock: 0,
  requestedQty: 0,
  orderStatus: 'received',
  notes: null,
  requestedAt: null,
};

const sugar = {
  id: 2,
  name: 'Sugar',
  unit: 'kg',
  category: 'sweetener',
  inStock: 3,
  threshold: 4,
  currentStock: 0,
  requestedQty: 10,
  orderStatus: 'waiting',
  notes: 'brown preferred',
  requestedAt: '2026-04-20T09:15:00',
};

describe('KitchenSupplies', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders supplies grouped by category after loading', async () => {
    api.fetchSupplies.mockResolvedValue([flour, sugar]);
    renderWithProviders(<KitchenSupplies />);

    expect(await screen.findByText('Wheat Flour')).toBeInTheDocument();
    expect(screen.getByText('Sugar')).toBeInTheDocument();
    expect(screen.getByText(/brown preferred/i)).toBeInTheDocument();
  });

  it('shows the empty-state alert when there are no open requests', async () => {
    api.fetchSupplies.mockResolvedValue([]);
    renderWithProviders(<KitchenSupplies />);

    expect(
      await screen.findByText(/no open supply requests/i)
    ).toBeInTheDocument();
  });

  it('surfaces a load error when the api rejects', async () => {
    api.fetchSupplies.mockRejectedValue(new Error('boom'));
    renderWithProviders(<KitchenSupplies />);

    expect(await screen.findByText('boom')).toBeInTheDocument();
  });

  it('keeps the publish button disabled until a draft change is made', async () => {
    api.fetchSupplies.mockResolvedValue([sugar]);
    renderWithProviders(<KitchenSupplies />);

    await screen.findByText('Sugar');
    const updateBtn = screen.getByRole('button', { name: /update list/i });
    expect(updateBtn).toBeDisabled();
  });

  it('enables publish after editing the needed quantity and sends the update', async () => {
    api.fetchSupplies.mockResolvedValue([sugar]);
    api.bulkUpdateSupplyStatuses.mockResolvedValue([
      { ...sugar, requestedQty: 25, orderStatus: 'waiting' },
    ]);
    const user = userEvent.setup();
    renderWithProviders(<KitchenSupplies />);

    await screen.findByText('Sugar');
    const qtyInput = screen
      .getByRole('row', { name: /sugar/i })
      .querySelector('input[type="number"]');
    await user.clear(qtyInput);
    await user.type(qtyInput, '25');

    const updateBtn = screen.getByRole('button', { name: /update list/i });
    await waitFor(() => expect(updateBtn).not.toBeDisabled());
    await user.click(updateBtn);

    await waitFor(() =>
      expect(api.bulkUpdateSupplyStatuses).toHaveBeenCalledTimes(1)
    );
    const [updates] = api.bulkUpdateSupplyStatuses.mock.calls[0];
    expect(updates).toEqual([
      expect.objectContaining({ id: 2, requestedQty: 25 }),
    ]);
  });

  it('rejects saving a new supply with an empty name', async () => {
    api.fetchSupplies.mockResolvedValue([]);
    const user = userEvent.setup();
    renderWithProviders(<KitchenSupplies />);

    await screen.findByText(/no open supply requests/i);
    await user.click(screen.getByRole('button', { name: /add supply/i }));

    const dialog = await screen.findByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: /^save$/i }));

    expect(api.saveSupply).not.toHaveBeenCalled();
    expect(await screen.findByText(/name is required/i)).toBeInTheDocument();
  });
});
