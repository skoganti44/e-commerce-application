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
  InputAdornment,
  IconButton,
  Divider,
} from '@mui/material';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';
import EmailIcon from '@mui/icons-material/Email';
import LockIcon from '@mui/icons-material/Lock';
import VisibilityIcon from '@mui/icons-material/Visibility';
import VisibilityOffIcon from '@mui/icons-material/VisibilityOff';
import LoginIcon from '@mui/icons-material/Login';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {
  loginUser,
  logout,
  clearAuthError,
  selectCurrentUser,
  hasRole,
  dashboardPathFor,
} from '../store/slices/authSlice.js';
import { setActiveUserId } from '../store/slices/sessionSlice.js';

const VALID_TYPES = ['customer', 'employee'];

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

export default function Login() {
  const dispatch = useDispatch();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();

  const rawType = (searchParams.get('type') || 'customer').toLowerCase();
  const expectedType = VALID_TYPES.includes(rawType) ? rawType : 'customer';
  const isEmployee = expectedType === 'employee';
  const heading = isEmployee ? 'Employee Login' : 'Customer Login';
  const HeadingIcon = isEmployee ? BadgeIcon : PersonIcon;

  const { status, error } = useSelector((s) => s.auth);
  const currentUser = useSelector(selectCurrentUser);

  const [form, setForm] = useState({ email: '', password: '' });
  const [showPw, setShowPw] = useState(false);
  const [roleError, setRoleError] = useState('');

  useEffect(() => {
    dispatch(clearAuthError());
    setRoleError('');
  }, [dispatch, expectedType]);

  useEffect(() => {
    if (currentUser && status === 'succeeded') {
      if (!hasRole(currentUser, expectedType)) {
        setRoleError(
          `This account is not registered as ${expectedType}. Please use the correct login.`
        );
        dispatch(logout());
        dispatch(setActiveUserId(null));
        return;
      }
      navigate(dashboardPathFor(currentUser));
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
    <Box sx={{ display: 'flex', justifyContent: 'center', py: { xs: 2, md: 4 } }}>
      <Paper
        elevation={0}
        sx={{
          p: { xs: 3, md: 4 },
          maxWidth: 460,
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
            <HeadingIcon sx={{ color: '#fff', fontSize: 34 }} />
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
            {heading}
          </Typography>
          <Typography
            sx={{ mt: 0.5, fontWeight: 600, ...RAINBOW_TEXT }}
          >
            Sign in to your {expectedType} account
          </Typography>
        </Box>

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
              InputProps={{
                startAdornment: (
                  <InputAdornment position="start">
                    <EmailIcon sx={{ color: '#ef5350' }} />
                  </InputAdornment>
                ),
              }}
            />
            <TextField
              label="Password"
              type={showPw ? 'text' : 'password'}
              value={form.password}
              onChange={handleChange('password')}
              required
              fullWidth
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
            <Button
              type="submit"
              size="large"
              disabled={status === 'loading'}
              startIcon={<LoginIcon sx={{ color: '#fff' }} />}
              sx={{ ...RAINBOW_FILLED_BTN, py: 1.2 }}
            >
              {status === 'loading' ? 'Signing in…' : 'Sign in'}
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

            {!isEmployee && (
              <Typography
                variant="body2"
                sx={{ textAlign: 'center', mt: 1, fontWeight: 600, ...RAINBOW_TEXT }}
              >
                New here?{' '}
                <Box
                  component={RouterLink}
                  to="/register"
                  sx={{
                    textDecoration: 'underline',
                    textUnderlineOffset: 3,
                    fontWeight: 800,
                    ...RAINBOW_TEXT,
                  }}
                >
                  Create an account
                </Box>
              </Typography>
            )}
          </Stack>
        </Box>
      </Paper>
    </Box>
  );
}
