import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchProducts: vi.fn(),
  recordCounterSale: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import CounterPOS from './CounterPOS.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const cookie = { id: 1, name: 'Cookie', price: 5 };
const cake = { id: 2, name: 'Cake', price: 100 };

describe('CounterPOS', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders product tiles after load', async () => {
    api.fetchProducts.mockResolvedValue([cookie, cake]);
    renderWithProviders(<CounterPOS />);

    expect(await screen.findByRole('button', { name: /cookie/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /cake/i })).toBeInTheDocument();
  });

  it('shows products error when load fails', async () => {
    api.fetchProducts.mockRejectedValue(new Error('network down'));
    renderWithProviders(<CounterPOS />);

    expect(await screen.findByText(/network down/i)).toBeInTheDocument();
  });

  it('disables Charge button until items are added', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    renderWithProviders(<CounterPOS />);

    await screen.findByRole('button', { name: /cookie/i });
    expect(screen.getByRole('button', { name: /charge/i })).toBeDisabled();
  });

  it('adds a product, increments quantity on a second tap, and totals correctly', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    const tile = await screen.findByRole('button', { name: /cookie/i });
    await user.click(tile);
    await user.click(tile);

    expect(screen.getByText('2')).toBeInTheDocument();
    expect(
      screen.getByRole('button', { name: /charge \$10\.00/i })
    ).toBeInTheDocument();
  });

  it('blocks charge for cash sale when cash is short', async () => {
    api.fetchProducts.mockResolvedValue([cake]);
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cake/i }));
    const cashField = screen.getByLabelText(/cash given/i);
    await user.type(cashField, '50');

    expect(
      screen.getByRole('button', { name: /charge \$100\.00/i })
    ).toBeDisabled();
    expect(screen.getByText(/short by \$50\.00/i)).toBeInTheDocument();
  });

  it('shows the change due when cash given exceeds total', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cookie/i }));
    await user.type(screen.getByLabelText(/cash given/i), '20');

    expect(screen.getByText(/change: \$15\.00/i)).toBeInTheDocument();
  });

  it('charges a cash sale, calls the api, and shows the receipt', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    api.recordCounterSale.mockResolvedValue({
      orderId: 42,
      paymentId: 7,
      totalAmount: 5,
      status: 'CONFIRMED',
      paymentMethod: 'CASH',
      paymentStatus: 'SUCCESS',
      cashGiven: 10,
      changeDue: 5,
      channel: 'instore',
    });
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cookie/i }));
    await user.type(screen.getByLabelText(/cash given/i), '10');
    await user.type(screen.getByLabelText(/customer name/i), 'Anita');
    await user.click(screen.getByRole('button', { name: /charge \$5\.00/i }));

    await waitFor(() =>
      expect(api.recordCounterSale).toHaveBeenCalledTimes(1)
    );
    const [payload] = api.recordCounterSale.mock.calls[0];
    expect(payload).toMatchObject({
      items: [{ productId: 1, quantity: 1 }],
      paymentMethod: 'CASH',
      cashGiven: 10,
      customerName: 'Anita',
    });

    const dialog = await screen.findByRole('dialog');
    expect(within(dialog).getByText(/order #42/i)).toBeInTheDocument();
    expect(within(dialog).getByText(/customer: anita/i)).toBeInTheDocument();
  });

  it('charges a card sale without requiring cash given', async () => {
    api.fetchProducts.mockResolvedValue([cake]);
    api.recordCounterSale.mockResolvedValue({
      orderId: 5,
      paymentId: 6,
      totalAmount: 100,
      paymentMethod: 'CREDIT_CARD',
      paymentStatus: 'SUCCESS',
      changeDue: 0,
      channel: 'instore',
    });
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cake/i }));
    await user.click(screen.getByLabelText(/payment/i));
    await user.click(await screen.findByRole('option', { name: /credit card/i }));

    expect(screen.queryByLabelText(/cash given/i)).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /charge \$100\.00/i }));

    await waitFor(() =>
      expect(api.recordCounterSale).toHaveBeenCalledTimes(1)
    );
    const [payload] = api.recordCounterSale.mock.calls[0];
    expect(payload.paymentMethod).toBe('CREDIT_CARD');
    expect(payload).not.toHaveProperty('cashGiven');
  });

  it('surfaces api error message on charge failure', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    api.recordCounterSale.mockRejectedValue({
      response: { data: { error: 'card declined' } },
    });
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cookie/i }));
    await user.click(screen.getByLabelText(/payment/i));
    await user.click(await screen.findByRole('option', { name: /credit card/i }));
    await user.click(screen.getByRole('button', { name: /charge \$5\.00/i }));

    expect(await screen.findByText(/card declined/i)).toBeInTheDocument();
  });

  it('Clear button empties the current sale', async () => {
    api.fetchProducts.mockResolvedValue([cookie]);
    const user = userEvent.setup();
    renderWithProviders(<CounterPOS />);

    await user.click(await screen.findByRole('button', { name: /cookie/i }));
    expect(screen.getByText('1')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: /clear/i }));
    expect(screen.queryByText('1')).not.toBeInTheDocument();
    expect(screen.getByText(/no items yet/i)).toBeInTheDocument();
  });
});
