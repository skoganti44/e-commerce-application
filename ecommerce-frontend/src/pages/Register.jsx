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
  InputAdornment,
  IconButton,
  Divider,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import PersonIcon from '@mui/icons-material/Person';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import BadgeIcon from '@mui/icons-material/Badge';
import ApartmentIcon from '@mui/icons-material/Apartment';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {
  registerUser,
  clearAuthError,
  selectCurrentUser,
  dashboardPathFor,
} from '../store/slices/authSlice.js';

// Password must contain: a letter, a digit, a special character, min 8 chars.
const PASSWORD_RE = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$/;

const RAINBOW_TEXT = {
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  backgroundClip: 'text',
};

const RAINBOW_FILLED_BTN = {
  fontWeight: 700,
  letterSpacing: 1,
  textTransform: 'uppercase',
  color: '#fff',
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
  boxShadow: 'none',
  '&:hover': {
    background:
      'linear-gradient(90deg,#e53935 0%,#fb8c00 50%,#43a047 100%)',
  },
  '&.Mui-disabled': {
    color: 'rgba(255,255,255,0.75)',
    background:
      'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
    opacity: 0.55,
  },
};

const RAINBOW_BTN = {
  bgcolor: 'transparent',
  boxShadow: 'none',
  fontWeight: 700,
  letterSpacing: 1,
  textTransform: 'uppercase',
  border: '1.5px solid',
  borderColor: 'rgba(0,0,0,0.12)',
  ...RAINBOW_TEXT,
  '&:hover': {
    bgcolor: 'transparent',
    borderColor: 'rgba(0,0,0,0.25)',
    filter: 'brightness(1.15)',
  },
};

const DEPARTMENTS = [
  { value: 'bakery', label: 'Bakery' },
  { value: 'sales', label: 'Sales' },
  { value: 'kitchen', label: 'Kitchen' },
  { value: 'delivery', label: 'Delivery' },
  { value: 'management', label: 'Management' },
];

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
    department: '',
  });
  const [clientError, setClientError] = useState('');
  const [showPw, setShowPw] = useState(false);
  const [showPw2, setShowPw2] = useState(false);

  useEffect(() => {
    dispatch(clearAuthError());
  }, [dispatch]);

  useEffect(() => {
    if (currentUser && status === 'succeeded') {
      navigate(dashboardPathFor(currentUser));
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
    if (form.userType === 'employee' && !form.department) {
      setClientError('Please select a department.');
      return;
    }

    dispatch(
      registerUser({
        name: form.name,
        email: form.email,
        password: form.password,
        userType: form.userType,
        department: form.userType === 'employee' ? form.department : undefined,
      })
    );
  };

  const passwordTouched = form.password.length > 0;
  const passwordValid = !passwordTouched || PASSWORD_RE.test(form.password);
  const confirmMismatch =
    form.confirmPassword.length > 0 && form.password !== form.confirmPassword;

  return (
    <Box sx={{ display: 'flex', justifyContent: 'center', py: { xs: 2, md: 4 } }}>
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          maxWidth: 520,
          width: '100%',
          borderRadius: 4,
          bgcolor: 'rgba(255,255,255,0.82)',
          backdropFilter: 'blur(10px)',
          border: '1px solid rgba(0,0,0,0.06)',
          boxShadow: '0 8px 32px rgba(60,30,10,0.08)',
        }}
      >
        <Box sx={{ textAlign: 'center', mb: 3 }}>
          <Box
            sx={{
              width: 64,
              height: 64,
              borderRadius: '50%',
              display: 'grid',
              placeItems: 'center',
              mx: 'auto',
              background:
                'linear-gradient(135deg,#ef5350 0%,#ffb300 35%,#66bb6a 65%,#29b6f6 100%)',
              boxShadow: '0 6px 18px rgba(0,0,0,0.12)',
            }}
          >
            <PersonAddIcon sx={{ color: '#fff', fontSize: 34 }} />
          </Box>
          <Typography
            variant="h4"
            sx={{
              mt: 2,
              fontWeight: 800,
              letterSpacing: 1,
              textTransform: 'uppercase',
              ...RAINBOW_TEXT,
            }}
          >
            Create Account
          </Typography>
          <Typography sx={{ mt: 0.5, fontWeight: 600, ...RAINBOW_TEXT }}>
            Join Dhati Bake today
          </Typography>
        </Box>

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
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <PersonIcon sx={{ color: '#66bb6a' }} />
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              label="Email"
              type="email"
              value={form.email}
              onChange={handleChange('email')}
              required
              fullWidth
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon sx={{ color: '#ef5350' }} />
                  </InputAdornment>
                ),
              }}
            />
            <FormControl fullWidth required>
              <InputLabel id="user-type-label">Register as</InputLabel>
              <Select
                labelId="user-type-label"
                label="Register as"
                value={form.userType}
                onChange={handleChange('userType')}
                startAdornment={
                  <InputAdornment position="start">
                    <BadgeIcon sx={{ color: '#ffb300' }} />
                  </InputAdornment>
                }
              >
                <MenuItem value="customer">Customer</MenuItem>
                <MenuItem value="employee">Employee</MenuItem>
              </Select>
              <FormHelperText>
                Customers can place orders; employees manage the store.
              </FormHelperText>
            </FormControl>

            {form.userType === 'employee' && (
              <FormControl fullWidth required>
                <InputLabel id="department-label">Department</InputLabel>
                <Select
                  labelId="department-label"
                  label="Department"
                  value={form.department}
                  onChange={handleChange('department')}
                  startAdornment={
                    <InputAdornment position="start">
                      <ApartmentIcon sx={{ color: '#29b6f6' }} />
                    </InputAdornment>
                  }
                >
                  {DEPARTMENTS.map((d) => (
                    <MenuItem key={d.value} value={d.value}>
                      {d.label}
                    </MenuItem>
                  ))}
                </Select>
                <FormHelperText>
                  Which team will this employee work in?
                </FormHelperText>
              </FormControl>
            )}

            <TextField
              label="Password"
              type={showPw ? 'text' : 'password'}
              value={form.password}
              onChange={handleChange('password')}
              required
              fullWidth
              error={!passwordValid}
              helperText="Min 8 characters, include a letter, a number, and a special character."
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon sx={{ color: '#ab47bc' }} />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      size="small"
                      onClick={() => setShowPw((v) => !v)}
                      edge="end"
                    >
                      {showPw ? (
                        <VisibilityOffIcon fontSize="small" sx={{ color: '#6d4c41' }} />
                      ) : (
                        <VisibilityIcon fontSize="small" sx={{ color: '#6d4c41' }} />
                      )}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              label="Re-enter Password"
              type={showPw2 ? 'text' : 'password'}
              value={form.confirmPassword}
              onChange={handleChange('confirmPassword')}
              required
              fullWidth
              error={confirmMismatch}
              helperText={confirmMismatch ? 'Passwords do not match.' : ' '}
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <LockIcon sx={{ color: '#ab47bc' }} />
                  </InputAdornment>
                ),
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      size="small"
                      onClick={() => setShowPw2((v) => !v)}
                      edge="end"
                    >
                      {showPw2 ? (
                        <VisibilityOffIcon fontSize="small" sx={{ color: '#6d4c41' }} />
                      ) : (
                        <VisibilityIcon fontSize="small" sx={{ color: '#6d4c41' }} />
                      )}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />
            <Button
              type="submit"
              size="large"
              disabled={status === 'loading'}
              startIcon={<PersonAddIcon sx={{ color: '#fff' }} />}
              sx={{ ...RAINBOW_FILLED_BTN, py: 1.2 }}
            >
              {status === 'loading' ? 'Creating…' : 'Create account'}
            </Button>

            <Divider sx={{ my: 0.5 }} />

            <Button
              component={RouterLink}
              to="/"
              disableRipple
              startIcon={<ArrowBackIcon sx={{ color: '#5d4037' }} />}
              sx={RAINBOW_BTN}
            >
              Back to welcome
            </Button>

            <Typography
              variant="body2"
              sx={{ textAlign: 'center', mt: 1, fontWeight: 600, ...RAINBOW_TEXT }}
            >
              Already have an account?{' '}
              <Box
                component={RouterLink}
                to="/login?type=customer"
                sx={{
                  textDecoration: 'underline',
                  textUnderlineOffset: 3,
                  fontWeight: 800,
                  ...RAINBOW_TEXT,
                }}
              >
                Sign in
              </Box>
            </Typography>
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
