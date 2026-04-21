import { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  useNavigate,
  useSearchParams,
  Link as RouterLink,
} from 'react-router-dom';
import {
  Paper,
  Typography,
  TextField,
  Button,
  Stack,
  Alert,
  Box,
} from '@mui/material';
import {
  loginUser,
  logout,
  clearAuthError,
  selectCurrentUser,
} from '../store/slices/authSlice.js';
import { setActiveUserId } from '../store/slices/sessionSlice.js';

const VALID_TYPES = ['customer', 'employee'];

export default function Login() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const rawType = (searchParams.get('type') || 'customer').toLowerCase();
  const expectedType = VALID_TYPES.includes(rawType) ? rawType : 'customer';
  const heading = expectedType === 'employee' ? 'Employee Login' : 'Customer Login';

  const { status, error } = useSelector((s) => s.auth);
  const currentUser = useSelector(selectCurrentUser);

  const [form, setForm] = useState({ email: '', password: '' });
  const [roleError, setRoleError] = useState('');

  useEffect(() => {
    dispatch(clearAuthError());
    setRoleError('');
  }, [dispatch, expectedType]);

  useEffect(() => {
    if (currentUser && status === 'succeeded') {
      const roles = (currentUser.roles || []).map((r) => r.toLowerCase());
      if (!roles.includes(expectedType)) {
        setRoleError(
          `This account is not registered as ${expectedType}. Please use the correct login.`
        );
        dispatch(logout());
        dispatch(setActiveUserId(null));
        return;
      }
      navigate('/');
    }
  }, [currentUser, status, expectedType, navigate, dispatch]);

  const handleChange = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    setRoleError('');
    dispatch(loginUser(form));
  };

  return (
    <Paper sx={{ p: 4, maxWidth: 480, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        {heading}
      </Typography>
      <Typography color="text.secondary" paragraph>
        Sign in to your {expectedType} account.
      </Typography>

      {roleError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {roleError}
        </Alert>
      )}
      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Box component="form" onSubmit={handleSubmit} noValidate>
        <Stack spacing={2}>
          <TextField
            label="Email"
            type="email"
            value={form.email}
            onChange={handleChange('email')}
            required
            fullWidth
          />
          <TextField
            label="Password"
            type="password"
            value={form.password}
            onChange={handleChange('password')}
            required
            fullWidth
          />
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={status === 'loading'}
          >
            {status === 'loading' ? 'Signing in…' : 'Sign in'}
          </Button>
          <Button component={RouterLink} to="/" variant="text">
            Back to welcome
          </Button>
        </Stack>
      </Box>
    </Paper>
  );
}
