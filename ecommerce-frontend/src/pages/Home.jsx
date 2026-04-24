import { useState } from 'react';
import { useSelector } from 'react-redux';
import {
  Box,
  Button,
  Typography,
  Menu,
  MenuItem,
  ListItemIcon,
  Divider,
  Paper,
  Stack,
  keyframes,
} from '@mui/material';
import { Link as RouterLink, useNavigate } from 'react-router-dom';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import StorefrontIcon from '@mui/icons-material/Storefront';
import GrassIcon from '@mui/icons-material/Grass';
import FavoriteIcon from '@mui/icons-material/Favorite';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import DashboardIcon from '@mui/icons-material/Dashboard';
import {
  selectCurrentUser,
  isEmployee,
  dashboardPathFor,
} from '../store/slices/authSlice.js';

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
};

const scrollLeftToRight = keyframes`
  0%   { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
`;

const features = [
  {
    Icon: GrassIcon,
    color: '#66bb6a',
    title: 'Millet Specials',
    desc: 'Wholesome, nutty, guilt-free',
  },
  {
    Icon: FavoriteIcon,
    color: '#ef5350',
    title: 'Handmade Daily',
    desc: 'Small batches, baked fresh',
  },
  {
    Icon: LocalShippingIcon,
    color: '#29b6f6',
    title: 'Quick Delivery',
    desc: 'Straight to your door',
  },
];

export default function Home() {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  const currentUser = useSelector(selectCurrentUser);
  const navigate = useNavigate();

  if (currentUser) {
    const displayName = currentUser.name || '';
    const employee = isEmployee(currentUser);
    return (
      <Stack spacing={3}>
        <Box
          sx={{
            position: 'relative',
            overflow: 'hidden',
            whiteSpace: 'nowrap',
            py: 0.9,
            bgcolor: 'rgba(255,255,255,0.65)',
            backdropFilter: 'blur(6px)',
            border: '1px solid rgba(0,0,0,0.06)',
            borderRadius: 999,
          }}
        >
          <Typography
            component="div"
            sx={{
              display: 'inline-block',
              fontFamily: "'Pacifico', 'Dancing Script', cursive",
              fontSize: { xs: '1rem', md: '1.15rem' },
              fontWeight: 400,
              letterSpacing: 2,
              ...RAINBOW_TEXT,
              animation: `${scrollLeftToRight} 16s linear infinite`,
              willChange: 'transform',
              px: 2,
            }}
          >
            ✦ WELCOME {displayName ? displayName.toUpperCase() : ''} TO DHATI BAKE ✦ FRESH BAKES, MADE WITH LOVE ✦
          </Typography>
        </Box>

        <Paper
          elevation={0}
          sx={{
            position: 'relative',
            borderRadius: 4,
            px: { xs: 3, md: 6 },
            py: { xs: 5, md: 8 },
            textAlign: 'center',
            bgcolor: 'rgba(255,255,255,0.7)',
            backdropFilter: 'blur(10px)',
            border: '1px solid rgba(0,0,0,0.06)',
            boxShadow: '0 8px 32px rgba(60,30,10,0.08)',
          }}
        >
          <Typography
            sx={{
              fontFamily: "'Dancing Script', cursive",
              fontSize: { xs: '1.3rem', md: '1.5rem' },
              fontWeight: 700,
              color: '#6d4c41',
              mb: 1,
            }}
          >
            Hello,
          </Typography>
          <Typography
            sx={{
              fontFamily: "'Pacifico', 'Dancing Script', cursive",
              fontSize: { xs: '2.6rem', md: '4rem' },
              fontWeight: 400,
              lineHeight: 1.1,
              letterSpacing: 0.5,
              ...RAINBOW_TEXT,
            }}
          >
            {displayName || 'Friend'}
          </Typography>
          <Typography
            sx={{
              mt: 2,
              fontFamily: "'Dancing Script', cursive",
              fontSize: { xs: '1.1rem', md: '1.4rem' },
              fontWeight: 700,
              color: '#5d4037',
              maxWidth: 560,
              mx: 'auto',
            }}
          >
            Sweet things baked with care — cakes, cookies, breads and more.
            Take a look and let the cravings decide.
          </Typography>

          {employee ? (
            <Button
              onClick={() => navigate(dashboardPathFor(currentUser))}
              startIcon={<DashboardIcon sx={{ color: '#fff' }} />}
              sx={{ ...RAINBOW_FILLED_BTN, mt: 4, px: 4, py: 1.3 }}
            >
              Go to Dashboard
            </Button>
          ) : (
            <Button
              onClick={() => navigate('/products')}
              startIcon={<StorefrontIcon sx={{ color: '#fff' }} />}
              sx={{ ...RAINBOW_FILLED_BTN, mt: 4, px: 4, py: 1.3 }}
            >
              Start Shopping
            </Button>
          )}
        </Paper>

        <Box
          sx={{
            display: 'grid',
            gridTemplateColumns: { xs: '1fr', sm: 'repeat(3, 1fr)' },
            gap: 2,
          }}
        >
          {features.map(({ Icon, color, title, desc }) => (
            <Paper
              key={title}
              elevation={0}
              sx={{
                p: 2.5,
                borderRadius: 2,
                border: '1px solid rgba(0,0,0,0.06)',
                bgcolor: 'rgba(255,255,255,0.78)',
                backdropFilter: 'blur(4px)',
                display: 'flex',
                alignItems: 'center',
                gap: 1.5,
              }}
            >
              <Box
                sx={{
                  width: 44,
                  height: 44,
                  borderRadius: '50%',
                  display: 'grid',
                  placeItems: 'center',
                  bgcolor: `${color}22`,
                }}
              >
                <Icon sx={{ color, fontSize: 24 }} />
              </Box>
              <Box>
                <Typography
                  sx={{
                    fontWeight: 800,
                    letterSpacing: 0.8,
                    textTransform: 'uppercase',
                    fontSize: '0.82rem',
                    ...RAINBOW_TEXT,
                  }}
                >
                  {title}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ color: '#5d4037', fontWeight: 600 }}
                >
                  {desc}
                </Typography>
              </Box>
            </Paper>
          ))}
        </Box>
      </Stack>
    );
  }

  return (
    <Box
      sx={{
        position: 'relative',
        minHeight: 'calc(100vh - 140px)',
        borderRadius: 2,
        overflow: 'hidden',
        backgroundImage: "url('/welcome-bake.png')",
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        backgroundRepeat: 'no-repeat',
        boxShadow: 3,
      }}
    >
      <Box sx={{ position: 'absolute', top: 20, right: 24, zIndex: 2 }}>
        <Button
          onClick={(e) => setAnchorEl(e.currentTarget)}
          startIcon={<AccountCircleIcon sx={{ color: '#d84315' }} />}
          endIcon={<KeyboardArrowDownIcon sx={{ color: '#5d4037' }} />}
          disableRipple
          sx={{
            bgcolor: 'transparent',
            boxShadow: 'none',
            border: '1.5px solid',
            borderColor: 'rgba(0,0,0,0.15)',
            backdropFilter: 'blur(4px)',
            fontWeight: 800,
            letterSpacing: 1,
            textTransform: 'uppercase',
            px: 2.5,
            py: 1,
            ...RAINBOW_TEXT,
            '&:hover': {
              bgcolor: 'rgba(255,255,255,0.25)',
              borderColor: 'rgba(0,0,0,0.3)',
              filter: 'brightness(1.15)',
            },
          }}
        >
          Sign In
        </Button>
        <Menu
          anchorEl={anchorEl}
          open={open}
          onClose={() => setAnchorEl(null)}
          anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
          transformOrigin={{ vertical: 'top', horizontal: 'right' }}
          PaperProps={{ sx: { minWidth: 240, mt: 0.5, borderRadius: 2 } }}
        >
          <MenuItem
            component={RouterLink}
            to="/login?type=customer"
            onClick={() => setAnchorEl(null)}
          >
            <ListItemIcon>
              <PersonIcon fontSize="small" sx={{ color: '#ab47bc' }} />
            </ListItemIcon>
            <Box>
              <Typography variant="body1" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                Customer Login
              </Typography>
              <Typography variant="caption" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                Browse and buy products
              </Typography>
            </Box>
          </MenuItem>
          <MenuItem
            component={RouterLink}
            to="/login?type=employee"
            onClick={() => setAnchorEl(null)}
          >
            <ListItemIcon>
              <BadgeIcon fontSize="small" sx={{ color: '#6d4c41' }} />
            </ListItemIcon>
            <Box>
              <Typography variant="body1" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                Employee Login
              </Typography>
              <Typography variant="caption" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                Manage store and inventory
              </Typography>
            </Box>
          </MenuItem>
          <Divider />
          <MenuItem
            component={RouterLink}
            to="/register"
            onClick={() => setAnchorEl(null)}
          >
            <ListItemIcon>
              <PersonAddIcon fontSize="small" sx={{ color: '#66bb6a' }} />
            </ListItemIcon>
            <Box>
              <Typography variant="body1" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                Create New Account
              </Typography>
              <Typography variant="caption" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                New here? Sign up in a minute
              </Typography>
            </Box>
          </MenuItem>
        </Menu>
      </Box>

      <Box
        sx={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'flex-end',
          minHeight: 'calc(100vh - 140px)',
          textAlign: 'center',
          px: 3,
          pb: 6,
        }}
      >
        <Box
          sx={{
            display: 'inline-block',
            px: 4,
            py: 2.5,
            borderRadius: 3,
            bgcolor: 'rgba(255,255,255,0.78)',
            backdropFilter: 'blur(4px)',
            boxShadow: 4,
          }}
        >
          <Typography
            variant="h1"
            sx={{
              fontFamily: "'Pacifico', 'Dancing Script', cursive",
              fontSize: { xs: '2.6rem', md: '4.2rem' },
              fontWeight: 400,
              letterSpacing: 1,
              lineHeight: 1.1,
              background:
                'linear-gradient(90deg,#d84315 0%,#ff8a65 35%,#8d6e63 70%,#5d4037 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent',
              backgroundClip: 'text',
              textShadow: '1px 1px 2px rgba(255,255,255,0.3)',
            }}
          >
            Welcome to Dhati Bake
          </Typography>
          <Typography
            variant="h6"
            sx={{
              mt: 1,
              fontFamily: "'Dancing Script', cursive",
              fontWeight: 700,
              color: '#5d4037',
            }}
          >
            Special in Millets and normal bake items
          </Typography>
        </Box>
      </Box>
    </Box>
  );
}
