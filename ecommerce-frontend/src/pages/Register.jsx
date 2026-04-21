import { useState, useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate, Link as RouterLink } from 'react-router-dom';
import {
  Paper,
  Typography,
  TextField,
  Button,
  Stack,
  Alert,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  FormHelperText,
} from '@mui/material';
import {
  registerUser,
  clearAuthError,
  selectCurrentUser,
} from '../store/slices/authSlice.js';

// Password must contain: a letter, a digit, a special character, min 8 chars.
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$/;

export default function Register() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const { status, error } = useSelector((s) => s.auth);
  const currentUser = useSelector(selectCurrentUser);

  const [form, setForm] = useState({
    name: '',
    email: '',
    password: '',
    confirmPassword: '',
    userType: 'customer',
  });
  const [clientError, setClientError] = useState('');

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  useEffect(() => {
    if (currentUser && status === 'succeeded') {
      navigate('/');
    }
  }, [currentUser, status, navigate]);

  const handleChange = (field) => (e) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = (e) => {
    e.preventDefault();
    setClientError('');

    if (!PASSWORD_RE.test(form.password)) {
      setClientError(
        'Password must be at least 8 characters and include a letter, a number, and a special character.'
      );
      return;
    }
    if (form.password !== form.confirmPassword) {
      setClientError('Passwords do not match.');
      return;
    }

    dispatch(
      registerUser({
        name: form.name,
        email: form.email,
        password: form.password,
        userType: form.userType,
      })
    );
  };

  const passwordTouched = form.password.length > 0;
  const passwordValid = !passwordTouched || PASSWORD_RE.test(form.password);
  const confirmMismatch =
    form.confirmPassword.length > 0 && form.password !== form.confirmPassword;

  return (
    <Paper sx={{ p: 4, maxWidth: 520, mx: 'auto' }}>
      <Typography variant="h4" gutterBottom>
        Register
      </Typography>
      <Typography color="text.secondary" paragraph>
        Create a new account.
      </Typography>

      {clientError && (
        <Alert severity="warning" sx={{ mb: 2 }}>
          {clientError}
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
            label="Name"
            value={form.name}
            onChange={handleChange('name')}
            required
            fullWidth
          />
          <TextField
            label="Email"
            type="email"
            value={form.email}
            onChange={handleChange('email')}
            required
            fullWidth
          />
          <FormControl fullWidth required>
            <InputLabel id="user-type-label">Register as</InputLabel>
            <Select
              labelId="user-type-label"
              label="Register as"
              value={form.userType}
              onChange={handleChange('userType')}
            >
              <MenuItem value="customer">Customer</MenuItem>
              <MenuItem value="employee">Employee</MenuItem>
            </Select>
            <FormHelperText>
              Customers can place orders; employees manage the store.
            </FormHelperText>
          </FormControl>
          <TextField
            label="Password"
            type="password"
            value={form.password}
            onChange={handleChange('password')}
            required
            fullWidth
            error={!passwordValid}
            helperText="Min 8 characters, include a letter, a number, and a special character."
          />
          <TextField
            label="Re-enter Password"
            type="password"
            value={form.confirmPassword}
            onChange={handleChange('confirmPassword')}
            required
            fullWidth
            error={confirmMismatch}
            helperText={confirmMismatch ? 'Passwords do not match.' : ' '}
          />
          <Button
            type="submit"
            variant="contained"
            size="large"
            disabled={status === 'loading'}
          >
            {status === 'loading' ? 'Creating…' : 'Create account'}
          </Button>
          <Button component={RouterLink} to="/" variant="text">
            Back to welcome
          </Button>
        </Stack>
      </Box>
    </Paper>
  );
}
