import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchTasks: vi.fn(),
  createTask: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import SalesTasks from './SalesTasks.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const salesUser = {
  userid: 42,
  name: 'Sara',
  roles: ['sales'],
  departments: ['sales'],
};

const sampleTasks = [
  {
    id: 1,
    title: 'Bake 12 brownies',
    description: 'For the office order',
    assignedToDepartment: 'kitchen',
    priority: 'high',
    status: 'open',
    dueDate: '2026-04-30',
    createdByName: 'Sara',
    relatedOrderId: 100,
    completedByName: null,
    resolutionNotes: null,
  },
  {
    id: 2,
    title: 'Pack catering',
    description: null,
    assignedToDepartment: 'bakery',
    priority: 'normal',
    status: 'done',
    dueDate: null,
    createdByName: 'Sara',
    relatedOrderId: null,
    completedByName: 'Anna',
    resolutionNotes: 'packed and labelled',
  },
];

function renderAsSales() {
  return renderWithProviders(<SalesTasks />, {
    preloadedState: {
      auth: { currentUser: salesUser, status: 'idle', error: null },
    },
  });
}

describe('SalesTasks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('loads tasks for the current sales user on mount', async () => {
    api.fetchTasks.mockResolvedValue(sampleTasks);
    renderAsSales();

    await waitFor(() =>
      expect(api.fetchTasks).toHaveBeenCalledWith({
        createdByUserId: 42,
        status: undefined,
      })
    );
    expect(await screen.findByText('Bake 12 brownies')).toBeInTheDocument();
    expect(screen.getByTestId('task-row-1')).toBeInTheDocument();
    expect(screen.getByTestId('task-row-2')).toBeInTheDocument();
  });

  it('shows an empty-state message when there are no tasks', async () => {
    api.fetchTasks.mockResolvedValue([]);
    renderAsSales();

    expect(
      await screen.findByText(/no tasks yet for this filter/i)
    ).toBeInTheDocument();
  });

  it('shows a backend-supplied error when load fails', async () => {
    api.fetchTasks.mockRejectedValue({
      response: { data: { error: 'kaboom' } },
    });
    renderAsSales();

    expect(await screen.findByText(/kaboom/i)).toBeInTheDocument();
  });

  it('reloads with the chosen status when Apply is clicked', async () => {
    api.fetchTasks.mockResolvedValue(sampleTasks);
    const user = userEvent.setup();
    renderAsSales();

    await screen.findByText('Bake 12 brownies');
    api.fetchTasks.mockClear();

    await user.click(screen.getByLabelText('status-filter'));
    await user.click(await screen.findByRole('option', { name: 'Open' }));
    await user.click(screen.getByRole('button', { name: /apply/i }));

    await waitFor(() =>
      expect(api.fetchTasks).toHaveBeenCalledWith({
        createdByUserId: 42,
        status: 'open',
      })
    );
  });

  it('creates a task with the form data and reloads', async () => {
    api.fetchTasks.mockResolvedValue(sampleTasks);
    api.createTask.mockResolvedValue({ id: 3 });
    const user = userEvent.setup();
    renderAsSales();

    await screen.findByText('Bake 12 brownies');
    await user.click(screen.getByRole('button', { name: /new task/i }));

    await user.type(screen.getByLabelText('task-title'), 'New cake order');
    await user.type(screen.getByLabelText('task-description'), '2 layers');
    await user.type(screen.getByLabelText('task-related-order'), '55');

    api.fetchTasks.mockClear();
    await user.click(screen.getByRole('button', { name: /create task/i }));

    await waitFor(() => expect(api.createTask).toHaveBeenCalledTimes(1));
    expect(api.createTask).toHaveBeenCalledWith(
      expect.objectContaining({
        createdByUserId: 42,
        assignedToDepartment: 'kitchen',
        title: 'New cake order',
        description: '2 layers',
        priority: 'normal',
        relatedOrderId: 55,
      })
    );
    await waitFor(() => expect(api.fetchTasks).toHaveBeenCalled());
    expect(await screen.findByText(/task created/i)).toBeInTheDocument();
  });

  it('blocks submission and shows an inline error when title is blank', async () => {
    api.fetchTasks.mockResolvedValue([]);
    const user = userEvent.setup();
    renderAsSales();

    await screen.findByText(/no tasks yet for this filter/i);
    await user.click(screen.getByRole('button', { name: /new task/i }));
    await user.click(screen.getByRole('button', { name: /create task/i }));

    expect(
      await screen.findByText(/title is required/i)
    ).toBeInTheDocument();
    expect(api.createTask).not.toHaveBeenCalled();
  });

  it('shows a backend error when create fails', async () => {
    api.fetchTasks.mockResolvedValue([]);
    api.createTask.mockRejectedValue({
      response: { data: { error: 'related order not found' } },
    });
    const user = userEvent.setup();
    renderAsSales();

    await screen.findByText(/no tasks yet for this filter/i);
    await user.click(screen.getByRole('button', { name: /new task/i }));
    await user.type(screen.getByLabelText('task-title'), 'Send to ghost order');
    await user.click(screen.getByRole('button', { name: /create task/i }));

    expect(
      await screen.findByText(/related order not found/i)
    ).toBeInTheDocument();
  });

  it('renders status and priority chips for each task row', async () => {
    api.fetchTasks.mockResolvedValue(sampleTasks);
    renderAsSales();

    const row = await screen.findByTestId('task-row-1');
    expect(within(row).getByText(/high/i)).toBeInTheDocument();
    expect(within(row).getByText(/open/i)).toBeInTheDocument();
  });
});
