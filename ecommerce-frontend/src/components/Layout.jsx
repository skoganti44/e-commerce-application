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
import ShoppingCartIcon from '@mui/icons-material/ShoppingCart';
import LogoutIcon from '@mui/icons-material/Logout';
import { logout, selectCurrentUser } from '../store/slices/authSlice.js';
import { setActiveUserId } from '../store/slices/sessionSlice.js';

const navLinks = [
  { to: '/', label: 'Home' },
  { to: '/products', label: 'Products', requiresAuth: true },
  { to: '/cart', label: 'Cart', requiresAuth: true },
  { to: '/orders', label: 'Orders', requiresAuth: true },
  { to: '/payments', label: 'Payments', requiresAuth: true },
  {
    to: '/manage-products',
    label: 'Add items',
    requiresAuth: true,
    requiresRole: 'employee',
    hint: 'Only employees can add items to sell',
  },
];

export default function Layout() {
  const dispatch = useDispatch();
  const currentUser = useSelector(selectCurrentUser);

  const handleLogout = () => {
    dispatch(logout());
    dispatch(setActiveUserId(null));
  };

  return (
    <Box sx={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <AppBar position="sticky" elevation={1}>
        <Toolbar>
          <ShoppingCartIcon sx={{ mr: 1 }} />
          <Typography variant="h6" sx={{ flexShrink: 0, mr: 3 }}>
            E-Commerce
          </Typography>
          <Box sx={{ flexGrow: 1, display: 'flex', gap: 1 }}>
            {navLinks
              .filter((link) => {
                if (link.requiresAuth && !currentUser) return false;
                if (link.requiresRole) {
                  const userRoles = (currentUser?.roles || []).map((r) =>
                    r.toLowerCase()
                  );
                  if (!userRoles.includes(link.requiresRole.toLowerCase()))
                    return false;
                }
                return true;
              })
              .map((link) => {
                const button = (
                  <Button
                    key={link.to}
                    component={NavLink}
                    to={link.to}
                    end={link.to === '/'}
                    color="inherit"
                    sx={{
                      '&.active': {
                        backgroundColor: 'rgba(255,255,255,0.15)',
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
                  color="secondary"
                  size="small"
                  sx={{ color: 'white' }}
                />
                <Button
                  color="inherit"
                  onClick={handleLogout}
                  startIcon={<LogoutIcon />}
                  size="small"
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
