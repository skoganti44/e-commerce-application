import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchManagementStaffPerformance: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import ManagementStaffPerformance from './ManagementStaffPerformance.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const manager = {
  userid: 1,
  name: 'Maya',
  roles: ['management'],
  departments: ['management'],
};

function renderAsManager() {
  return renderWithProviders(<ManagementStaffPerformance />, {
    preloadedState: {
      auth: { currentUser: manager, status: 'idle', error: null },
    },
  });
}

describe('ManagementStaffPerformance', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders drivers, staff-by-dept, and sales activity', async () => {
    api.fetchManagementStaffPerformance.mockResolvedValue({
      from: '2026-04-28',
      to: '2026-04-28',
      drivers: [
        { userId: 7, name: 'Dan', trips: 3, delivered: 2, failed: 1,
          cod: 250, tips: 30, distanceKm: 12.5 },
      ],
      staffByDepartment: {
        kitchen: [
          { userId: 11, name: 'Karthik', tasksCompleted: 4 },
        ],
        bakery: [
          { userId: 12, name: 'Bella', tasksCompleted: 2 },
        ],
      },
      salesActivity: [
        { userId: 5, name: 'Sara', tasksCreated: 7 },
      ],
    });
    renderAsManager();

    expect(await screen.findByTestId('perf-driver-7')).toBeInTheDocument();
    expect(screen.getByText('Dan')).toBeInTheDocument();
    expect(screen.getByTestId('perf-kitchen-11')).toHaveTextContent('Karthik');
    expect(screen.getByTestId('perf-kitchen-11')).toHaveTextContent('4 done');
    expect(screen.getByTestId('perf-bakery-12')).toHaveTextContent('Bella');
    expect(screen.getByTestId('perf-sales-5')).toHaveTextContent('Sara · 7');
  });

  it('shows empty messages when no data', async () => {
    api.fetchManagementStaffPerformance.mockResolvedValue({
      from: '2026-04-28',
      to: '2026-04-28',
      drivers: [],
      staffByDepartment: {},
      salesActivity: [],
    });
    renderAsManager();
    expect(await screen.findByText(/No driver activity/i)).toBeInTheDocument();
    expect(screen.getByText(/No tasks completed/i)).toBeInTheDocument();
    expect(screen.getByText(/No tasks were created/i)).toBeInTheDocument();
  });

  it('reloads when date range changes', async () => {
    api.fetchManagementStaffPerformance.mockResolvedValue({
      drivers: [],
      staffByDepartment: {},
      salesActivity: [],
    });
    renderAsManager();
    await waitFor(() => expect(api.fetchManagementStaffPerformance).toHaveBeenCalled());
    api.fetchManagementStaffPerformance.mockClear();

    const fromInput = screen.getByLabelText('perf-from');
    await userEvent.clear(fromInput);
    await userEvent.type(fromInput, '2026-04-01');
    const toInput = screen.getByLabelText('perf-to');
    await userEvent.clear(toInput);
    await userEvent.type(toInput, '2026-04-28');
    await userEvent.click(screen.getByRole('button', { name: /Apply/i }));

    await waitFor(() =>
      expect(api.fetchManagementStaffPerformance).toHaveBeenLastCalledWith(
        '2026-04-01', '2026-04-28'
      )
    );
  });

  it('shows backend error', async () => {
    api.fetchManagementStaffPerformance.mockRejectedValue({
      response: { data: { error: 'date oops' } },
    });
    renderAsManager();
    expect(await screen.findByText('date oops')).toBeInTheDocument();
  });
});
