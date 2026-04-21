import { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Box,
} from '@mui/material';
import { loadUsers } from '../store/slices/usersSlice.js';
import {
  selectActiveUserId,
  setActiveUserId,
} from '../store/slices/sessionSlice.js';

export default function UserSelector() {
  const dispatch = useDispatch();
  const users = useSelector((s) => s.users.items);
  const status = useSelector((s) => s.users.status);
  const activeUserId = useSelector(selectActiveUserId);

  useEffect(() => {
    if (status === 'idle') dispatch(loadUsers());
  }, [status, dispatch]);

  return (
    <Box sx={{ minWidth: 200 }}>
      <FormControl size="small" fullWidth sx={{ bgcolor: 'white', borderRadius: 1 }}>
        <InputLabel id="active-user-label">Active User</InputLabel>
        <Select
          labelId="active-user-label"
          label="Active User"
          value={activeUserId ?? ''}
          onChange={(e) => dispatch(setActiveUserId(e.target.value))}
        >
          <MenuItem value=""><em>None</em></MenuItem>
          {users.map((u) => (
            <MenuItem key={u.userid} value={u.userid}>
              {u.name} (#{u.userid})
            </MenuItem>
          ))}
        </Select>
      </FormControl>
    </Box>
  );
}
