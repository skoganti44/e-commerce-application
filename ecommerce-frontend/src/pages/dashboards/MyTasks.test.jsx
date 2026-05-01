import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

vi.mock('../../api/endpoints.js', () => ({
  fetchTasks: vi.fn(),
  updateTaskStatus: vi.fn(),
}));

import * as api from '../../api/endpoints.js';
import MyTasks from './MyTasks.jsx';
import { renderWithProviders } from '../../test/renderWithProviders.jsx';

const kitchenUser = {
  userid: 11,
  name: 'Chef',
  roles: ['kitchen'],
  departments: ['kitchen'],
};

const tasks = [
  {
    id: 1,
    title: 'Bake brownies',
    description: '12 pieces',
    assignedToDepartment: 'kitchen',
    priority: 'urgent',
    status: 'open',
    dueDate: '2026-04-30',
    createdByName: 'Sara',
    relatedOrderId: 99,
  },
  {
    id: 2,
    title: 'Prep dough',
    description: null,
    assignedToDepartment: 'kitchen',
    priority: 'normal',
    status: 'in_progress',
    dueDate: null,
    createdByName: 'Sara',
    relatedOrderId: null,
  },
  {
    id: 3,
    title: 'Old finished thing',
    description: null,
    assignedToDepartment: 'kitchen',
    priority: 'low',
    status: 'done',
    dueDate: null,
    createdByName: 'Sara',
    relatedOrderId: null,
  },
];

function renderAsKitchen(props = {}) {
  return renderWithProviders(<MyTasks {...props} />, {
    preloadedState: {
      auth: { currentUser: kitchenUser, status: 'idle', error: null },
    },
  });
}

describe('MyTasks', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("loads tasks for the current user's primary department", async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    renderAsKitchen();

    await waitFor(() =>
      expect(api.fetchTasks).toHaveBeenCalledWith({ department: 'kitchen' })
    );
    expect(await screen.findByText('Bake brownies')).toBeInTheDocument();
    expect(screen.getByText('Prep dough')).toBeInTheDocument();
  });

  it('hides done and cancelled tasks', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    renderAsKitchen();

    await screen.findByText('Bake brownies');
    expect(screen.queryByText('Old finished thing')).not.toBeInTheDocument();
  });

  it('shows the success empty-state when nothing is assigned', async () => {
    api.fetchTasks.mockResolvedValue([]);
    renderAsKitchen();

    expect(
      await screen.findByText(/sales hasn't sent anything for kitchen/i)
    ).toBeInTheDocument();
  });

  it('shows error when load fails', async () => {
    api.fetchTasks.mockRejectedValue({
      response: { data: { error: 'load broke' } },
    });
    renderAsKitchen();

    expect(await screen.findByText(/load broke/i)).toBeInTheDocument();
  });

  it('starts an open task by calling updateTaskStatus with in_progress', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    api.updateTaskStatus.mockResolvedValue({ id: 1, status: 'in_progress' });
    const user = userEvent.setup();
    renderAsKitchen();

    await screen.findByText('Bake brownies');
    await user.click(screen.getByTestId('mytask-start-1'));

    await waitFor(() =>
      expect(api.updateTaskStatus).toHaveBeenCalledWith(1, {
        status: 'in_progress',
        actingUserId: 11,
      })
    );
  });

  it('completes a task with optional resolution notes', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    api.updateTaskStatus.mockResolvedValue({ id: 2, status: 'done' });
    const user = userEvent.setup();
    renderAsKitchen();

    await screen.findByText('Prep dough');
    await user.click(screen.getByTestId('mytask-done-2'));

    const notes = await screen.findByLabelText('mytask-notes');
    await user.type(notes, 'all baked, smells great');
    await user.click(screen.getByRole('button', { name: /^complete$/i }));

    await waitFor(() =>
      expect(api.updateTaskStatus).toHaveBeenCalledWith(2, {
        status: 'done',
        actingUserId: 11,
        resolutionNotes: 'all baked, smells great',
      })
    );
  });

  it('cancels a task with the cancel dialog', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    api.updateTaskStatus.mockResolvedValue({ id: 1, status: 'cancelled' });
    const user = userEvent.setup();
    renderAsKitchen();

    await screen.findByText('Bake brownies');
    await user.click(screen.getByTestId('mytask-cancel-1'));
    await user.click(screen.getByRole('button', { name: /cancel task/i }));

    await waitFor(() =>
      expect(api.updateTaskStatus).toHaveBeenCalledWith(1, {
        status: 'cancelled',
        actingUserId: 11,
        resolutionNotes: null,
      })
    );
  });

  it('shows backend error inside the dialog when status update fails', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    api.updateTaskStatus.mockRejectedValue({
      response: { data: { error: 'already done' } },
    });
    const user = userEvent.setup();
    renderAsKitchen();

    await screen.findByText('Prep dough');
    await user.click(screen.getByTestId('mytask-done-2'));
    await user.click(screen.getByRole('button', { name: /^complete$/i }));

    expect(await screen.findByText(/already done/i)).toBeInTheDocument();
  });

  it('orders tasks by priority — urgent first', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    renderAsKitchen();

    await screen.findByText('Bake brownies');
    const rows = screen.getAllByRole('row');
    // header + 2 active tasks
    expect(rows).toHaveLength(3);
    expect(rows[1]).toHaveTextContent('Bake brownies');
    expect(rows[2]).toHaveTextContent('Prep dough');
  });

  it('does not render at all when user has no primary department', async () => {
    api.fetchTasks.mockResolvedValue([]);
    const { container } = renderWithProviders(<MyTasks />, {
      preloadedState: {
        auth: {
          currentUser: { userid: 5, name: 'Nope', roles: [], departments: [] },
          status: 'idle',
          error: null,
        },
      },
    });

    expect(container.firstChild).toBeNull();
    expect(api.fetchTasks).not.toHaveBeenCalled();
  });

  it('honors a department override prop over the user dept', async () => {
    api.fetchTasks.mockResolvedValue([]);
    renderAsKitchen({ department: 'bakery' });

    await waitFor(() =>
      expect(api.fetchTasks).toHaveBeenCalledWith({ department: 'bakery' })
    );
    expect(
      await screen.findByText(/sales hasn't sent anything for bakery/i)
    ).toBeInTheDocument();
  });

  it('shows the related order id under the title when present', async () => {
    api.fetchTasks.mockResolvedValue(tasks);
    renderAsKitchen();

    const row = await screen.findByTestId('mytask-row-1');
    expect(within(row).getByText(/related order: #99/i)).toBeInTheDocument();
  });
});
