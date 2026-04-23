import { NavLink, Outlet } from 'react-router-dom';
import { useDispatch, useSelector } from 'react-redux';
import {
  AppBar,
  Toolbar,
  Typography,
  Container,
  Box,
  Button,
  Chip,
  Stack,
  Tooltip,
} from '@mui/material';
import BakeryDiningIcon from '@mui/icons-material/BakeryDining';
import LogoutIcon from '@mui/icons-material/Logout';
import { logout, selectCurrentUser, hasRole } from '../store/slices/authSlice.js';
import { setActiveUserId } from '../store/slices/sessionSlice.js';

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/products', label: 'Products', requiresAuth: true },
  { to: '/payments', label: 'Orders', requiresAuth: true },
  {
    to: '/manage-products',
    label: 'Add items',
    requiresAuth: true,
    requiresRole: 'employee',
    hint: 'Only employees can add items to sell',
  },
  {
    to: '/add-products',
    label: 'Add Products',
    requiresAuth: true,
    requiresRole: 'employee',
    hint: 'Quick-add a product with name, price, description and image',
  },
];

const AFTER_LOGIN_BG = '/afterlogin-bake.png';

export default function Layout() {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);

  const handleLogout = () => {
    dispatch(logout());
    dispatch(setActiveUserId(null));
  };

  const loggedIn = Boolean(currentUser);

  return (
    <Box
      sx={{
        minHeight: '100vh',
        display: 'flex',
        flexDirection: 'column',
        background: loggedIn
          ? `linear-gradient(
                180deg,
                rgba(255,255,255,0.82) 0%,
                rgba(255,255,255,0.72) 100%
              ),
              url('${AFTER_LOGIN_BG}')`
          : undefined,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundAttachment: 'fixed',
      }}
    >
      <AppBar
        position="sticky"
        elevation={0}
        sx={{
          bgcolor: 'transparent',
          backgroundImage: 'none',
          color: '#3e2723',
          borderBottom: 1,
          borderColor: 'rgba(0,0,0,0.08)',
          backdropFilter: 'blur(6px)',
        }}
      >
        <Toolbar>
          <BakeryDiningIcon sx={{ mr: 1, color: '#d84315' }} />
          <Typography
            variant="h6"
            sx={{
              flexShrink: 0,
              mr: 3,
              fontFamily: "'Pacifico', 'Dancing Script', cursive",
              fontWeight: 400,
              letterSpacing: 0.5,
              background:
                'linear-gradient(90deg,#d84315 0%,#ff8a65 50%,#5d4037 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
            }}
          >
            Dhati Bake
          </Typography>
          <Box sx={{ flexGrow: 1, display: 'flex', gap: 1 }}>
            {navLinks
              .filter((link) => {
                if (link.requiresAuth && !currentUser) return false;
                if (link.requiresRole && !hasRole(currentUser, link.requiresRole))
                  return false;
                return true;
              })
              .map((link) => {
                const button = (
                  <Button
                    key={link.to}
                    component={NavLink}
                    to={link.to}
                    end={link.to === '/'}
                    disableRipple
                    sx={{
                      bgcolor: 'transparent',
                      boxShadow: 'none',
                      fontWeight: 700,
                      letterSpacing: 1,
                      textTransform: 'uppercase',
                      background:
                        'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      backgroundClip: 'text',
                      '&:hover': {
                        bgcolor: 'transparent',
                        filter: 'brightness(1.15)',
                      },
                      '&.active': {
                        textDecoration: 'underline',
                        textUnderlineOffset: 4,
                        textDecorationThickness: 2,
                        textDecorationColor: '#d84315',
                      },
                    }}
                  >
                    {link.label}
                  </Button>
                );
                return link.hint ? (
                  <Tooltip key={link.to} title={link.hint}>
                    {button}
                  </Tooltip>
                ) : (
                  button
                );
              })}
          </Box>
          <Stack direction="row" spacing={1} alignItems="center">
            {currentUser && (
              <>
                <Chip
                  label={currentUser.name}
                  size="small"
                  variant="outlined"
                  sx={{
                    bgcolor: 'transparent',
                    borderColor: 'rgba(0,0,0,0.18)',
                    fontWeight: 800,
                    letterSpacing: 0.5,
                    '& .MuiChip-label': {
                      background:
                        'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
                      WebkitBackgroundClip: 'text',
                      WebkitTextFillColor: 'transparent',
                      backgroundClip: 'text',
                    },
                  }}
                />
                <Button
                  onClick={handleLogout}
                  startIcon={<LogoutIcon sx={{ color: '#d84315' }} />}
                  size="small"
                  disableRipple
                  sx={{
                    bgcolor: 'transparent',
                    boxShadow: 'none',
                    fontWeight: 700,
                    letterSpacing: 1,
                    textTransform: 'uppercase',
                    background:
                      'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
                    WebkitBackgroundClip: 'text',
                    WebkitTextFillColor: 'transparent',
                    backgroundClip: 'text',
                    '&:hover': {
                      bgcolor: 'transparent',
                      filter: 'brightness(1.15)',
                    },
                  }}
                >
                  Logout
                </Button>
              </>
            )}
          </Stack>
        </Toolbar>
      </AppBar>
      <Container maxWidth="lg" sx={{ py: 4, flexGrow: 1 }}>
        <Outlet />
      </Container>
    </Box>
  );
}
