import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchSalesAnalytics: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import SalesDashboard from './SalesDashboard.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const sampleAnalytics = {
  from: '2026-04-01',
  to: '2026-04-03',
  totalRevenue: 400,
  orderCount: 3,
  avgOrderValue: 133.33,
  revenueByChannel: { online: 340, instore: 60 },
  revenueByPaymentMethod: { CREDIT_CARD: 100, CASH: 50 },
  ordersByStatus: { CONFIRMED: 2, CANCELLED: 1 },
  topProducts: [
    { productId: 2, name: 'Cake',   quantitySold: 2, revenue: 200 },
    { productId: 3, name: 'Bread',  quantitySold: 3, revenue: 150 },
    { productId: 1, name: 'Cookie', quantitySold: 5, revenue: 50  },
  ],
  dailyTrend: [
    { date: '2026-04-01', revenue: 80, orderCount: 1 },
    { date: '2026-04-02', revenue:  0, orderCount: 0 },
    { date: '2026-04-03', revenue: 20, orderCount: 1 },
  ],
};

describe('SalesDashboard', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders KPI cards from analytics data', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    renderWithProviders(<SalesDashboard />);

    expect(await screen.findByTestId('kpi-revenue')).toHaveTextContent('$400.00');
    expect(screen.getByTestId('kpi-orders')).toHaveTextContent('3');
    expect(screen.getByTestId('kpi-aov')).toHaveTextContent('$133.33');
  });

  it('renders top products sorted as returned', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    const rows = screen.getAllByRole('row');
    // first row is header; subsequent rows in order
    expect(rows[1]).toHaveTextContent('Cake');
    expect(rows[2]).toHaveTextContent('Bread');
    expect(rows[3]).toHaveTextContent('Cookie');
  });

  it('renders channel and payment-method bars from data', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    expect(screen.getByTestId('bar-online')).toBeInTheDocument();
    expect(screen.getByTestId('bar-instore')).toBeInTheDocument();
    expect(screen.getByTestId('bar-CREDIT_CARD')).toBeInTheDocument();
    expect(screen.getByTestId('bar-CASH')).toBeInTheDocument();
  });

  it('renders one trend bar per day in the dailyTrend list', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    expect(screen.getByTestId('trend-2026-04-01')).toBeInTheDocument();
    expect(screen.getByTestId('trend-2026-04-02')).toBeInTheDocument();
    expect(screen.getByTestId('trend-2026-04-03')).toBeInTheDocument();
  });

  it('renders ordersByStatus chips', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    renderWithProviders(<SalesDashboard />);

    expect(await screen.findByText('CONFIRMED: 2')).toBeInTheDocument();
    expect(screen.getByText('CANCELLED: 1')).toBeInTheDocument();
  });

  it('shows empty-state messages when there is no channel/payment/product data', async () => {
    api.fetchSalesAnalytics.mockResolvedValue({
      from: '2026-04-01',
      to: '2026-04-03',
      totalRevenue: 0,
      orderCount: 0,
      avgOrderValue: 0,
      revenueByChannel: {},
      revenueByPaymentMethod: {},
      ordersByStatus: {},
      topProducts: [],
      dailyTrend: [
        { date: '2026-04-01', revenue: 0, orderCount: 0 },
        { date: '2026-04-02', revenue: 0, orderCount: 0 },
        { date: '2026-04-03', revenue: 0, orderCount: 0 },
      ],
    });
    renderWithProviders(<SalesDashboard />);

    expect(await screen.findByTestId('no-channel-data')).toBeInTheDocument();
    expect(screen.getByTestId('no-payment-data')).toBeInTheDocument();
    expect(screen.getByText(/no products sold/i)).toBeInTheDocument();
    expect(screen.getByText(/no orders/i)).toBeInTheDocument();
  });

  it('calls the api with from and to when Apply is clicked', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    const user = userEvent.setup();
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    api.fetchSalesAnalytics.mockClear();

    const fromInput = screen.getByLabelText('from-date');
    const toInput = screen.getByLabelText('to-date');
    await user.clear(fromInput);
    await user.type(fromInput, '2026-04-01');
    await user.clear(toInput);
    await user.type(toInput, '2026-04-05');
    await user.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() =>
      expect(api.fetchSalesAnalytics).toHaveBeenCalledWith('2026-04-01', '2026-04-05')
    );
  });

  it('blocks apply and shows an error when from > to', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    const user = userEvent.setup();
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    api.fetchSalesAnalytics.mockClear();

    const fromInput = screen.getByLabelText('from-date');
    const toInput = screen.getByLabelText('to-date');
    await user.clear(fromInput);
    await user.type(fromInput, '2026-04-10');
    await user.clear(toInput);
    await user.type(toInput, '2026-04-01');
    await user.click(screen.getByRole('button', { name: /apply/i }));

    expect(screen.getByText(/'from' date must be on or before 'to' date/i)).toBeInTheDocument();
    expect(api.fetchSalesAnalytics).not.toHaveBeenCalled();
  });

  it('shows backend error message when api rejects with a structured error', async () => {
    api.fetchSalesAnalytics.mockRejectedValue({
      response: { data: { error: 'Invalid from date' } },
    });
    renderWithProviders(<SalesDashboard />);

    expect(await screen.findByText(/invalid from date/i)).toBeInTheDocument();
  });

  it('shows generic error when api rejects without response body', async () => {
    api.fetchSalesAnalytics.mockRejectedValue(new Error('network down'));
    renderWithProviders(<SalesDashboard />);

    expect(await screen.findByText(/network down/i)).toBeInTheDocument();
  });

  it('Refresh re-calls the api with current dates', async () => {
    api.fetchSalesAnalytics.mockResolvedValue(sampleAnalytics);
    const user = userEvent.setup();
    renderWithProviders(<SalesDashboard />);

    await screen.findByText('Cake');
    expect(api.fetchSalesAnalytics).toHaveBeenCalledTimes(1);

    await user.click(screen.getByRole('button', { name: /refresh/i }));
    await waitFor(() => expect(api.fetchSalesAnalytics).toHaveBeenCalledTimes(2));
  });
});
