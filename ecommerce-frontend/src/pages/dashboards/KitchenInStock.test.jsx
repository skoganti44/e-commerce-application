import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchInStockSupplies: vi.fn(),
  adjustSupplyStock: vi.fn(),
  requestMoreSupply: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import KitchenInStock from './KitchenInStock.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const milk = {
  id: 10,
  name: 'Milk',
  unit: 'l',
  category: 'dairy',
  inStock: 6,
  threshold: 2,
  currentStock: 0,
  requestedQty: 0,
  orderStatus: 'received',
  notes: null,
};

const lowButter = {
  id: 11,
  name: 'Butter',
  unit: 'kg',
  category: 'dairy',
  inStock: 1,
  threshold: 3,
  currentStock: 0,
  requestedQty: 0,
  orderStatus: 'received',
  notes: null,
};

describe('KitchenInStock', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('lists items and flags low-stock count in the header', async () => {
    api.fetchInStockSupplies.mockResolvedValue([milk, lowButter]);
    renderWithProviders(<KitchenInStock />);

    expect(await screen.findByText('Milk')).toBeInTheDocument();
    expect(screen.getByText('Butter')).toBeInTheDocument();
    expect(screen.getByText(/2 in pantry/i)).toBeInTheDocument();
    expect(screen.getByText(/1 running low/i)).toBeInTheDocument();
  });

  it('shows empty-pantry alert when the api returns nothing', async () => {
    api.fetchInStockSupplies.mockResolvedValue([]);
    renderWithProviders(<KitchenInStock />);

    expect(
      await screen.findByText(/pantry is empty/i)
    ).toBeInTheDocument();
  });

  it('calls adjustSupplyStock with delta +1 when the add button is clicked', async () => {
    api.fetchInStockSupplies.mockResolvedValue([milk]);
    api.adjustSupplyStock.mockResolvedValue({ ...milk, inStock: 7 });
    const user = userEvent.setup();
    renderWithProviders(<KitchenInStock />);

    await screen.findByText('Milk');
    const row = screen.getByRole('row', { name: /milk/i });
    const [minusBtn, plusBtn] = within(row).getAllByRole('button');
    await user.click(plusBtn);

    await waitFor(() =>
      expect(api.adjustSupplyStock).toHaveBeenCalledWith(10, 1)
    );
    // sanity: the minus button exists and is the opposite direction
    expect(minusBtn).toBeInTheDocument();
  });

  it('blocks submitting a request for zero quantity', async () => {
    api.fetchInStockSupplies.mockResolvedValue([milk]);
    const user = userEvent.setup();
    renderWithProviders(<KitchenInStock />);

    await screen.findByText('Milk');
    await user.click(screen.getByRole('button', { name: /request more/i }));

    const dialog = await screen.findByRole('dialog');
    await user.click(
      within(dialog).getByRole('button', { name: /send request/i })
    );

    expect(api.requestMoreSupply).not.toHaveBeenCalled();
    expect(
      await screen.findByText(/enter a positive quantity/i)
    ).toBeInTheDocument();
  });

  it('sends a valid request with the chosen quantity and urgency', async () => {
    api.fetchInStockSupplies.mockResolvedValue([milk]);
    api.requestMoreSupply.mockResolvedValue({});
    const user = userEvent.setup();
    renderWithProviders(<KitchenInStock />);

    await screen.findByText('Milk');
    await user.click(screen.getByRole('button', { name: /request more/i }));

    const dialog = await screen.findByRole('dialog');
    const qtyField = within(dialog).getByLabelText(/quantity needed/i);
    await user.type(qtyField, '4');
    await user.click(
      within(dialog).getByRole('button', { name: /send request/i })
    );

    await waitFor(() =>
      expect(api.requestMoreSupply).toHaveBeenCalledWith(10, 4, 'waiting')
    );
  });
});
