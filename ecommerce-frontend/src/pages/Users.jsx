import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  Paper,
  Typography,
  Table,
  TableHead,
  TableBody,
  TableRow,
  TableCell,
  Button,
  Box,
  Alert,
  CircularProgress,
} from '@mui/material';
import { loadUsers } from '../store/slices/usersSlice.js';

export default function Users() {
  const dispatch = useDispatch();
  const { items, status, error } = useSelector((s) => s.users);

  useEffect(() => {
    if (status === 'idle') dispatch(loadUsers());
  }, [status, dispatch]);

  return (
    <Paper sx={{ p: 3 }}>
      <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
        <Typography variant="h5" sx={{ flexGrow: 1 }}>
          Users
        </Typography>
        <Button onClick={() => dispatch(loadUsers())} variant="outlined">
          Refresh
        </Button>
      </Box>

      {status === 'loading' && <CircularProgress />}
      {error && <Alert severity="error">{error}</Alert>}

      {items.length > 0 && (
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Created</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {items.map((u) => (
              <TableRow key={u.userid} hover>
                <TableCell>{u.userid}</TableCell>
                <TableCell>{u.name}</TableCell>
                <TableCell>{u.email}</TableCell>
                <TableCell>{u.createdat ?? '—'}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Paper>
  );
}
