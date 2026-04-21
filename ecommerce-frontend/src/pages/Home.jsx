import { useState } from 'react';
import {
  Box,
  Button,
  Stack,
  Typography,
  Menu,
  MenuItem,
  ListItemIcon,
  Divider,
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';
import StorefrontIcon from '@mui/icons-material/Storefront';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';

// Drop your own picture at ecommerce-frontend/public/welcome-bg.jpg to override.
const REMOTE_BG =
  'https://images.unsplash.com/photo-1558961363-fa8fdf82db35?auto=format&fit=crop&w=1920&q=80';

export default function Home() {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  return (
    <Box
      sx={{
        position: 'relative',
        minHeight: 'calc(100vh - 140px)',
        borderRadius: 2,
        overflow: 'hidden',
        background: `linear-gradient(
            135deg,
            rgba(62,39,35,0.55) 0%,
            rgba(109,76,65,0.35) 50%,
            rgba(255,224,178,0.35) 100%
          ),
          url('/welcome-bg.jpg'),
          url('${REMOTE_BG}')`,
        backgroundSize: 'cover',
        backgroundPosition: 'center',
        boxShadow: 3,
      }}
    >
      <Box sx={{ position: 'absolute', top: 20, right: 24, zIndex: 2 }}>
        <Button
          onClick={(e) => setAnchorEl(e.currentTarget)}
          variant="contained"
          startIcon={<AccountCircleIcon />}
          endIcon={<KeyboardArrowDownIcon />}
          sx={{
            bgcolor: 'rgba(255,255,255,0.95)',
            color: '#3e2723',
            fontWeight: 600,
            letterSpacing: 0.5,
            boxShadow: 3,
            px: 2.5,
            py: 1,
            '&:hover': { bgcolor: 'white' },
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
              <PersonIcon fontSize="small" color="secondary" />
            </ListItemIcon>
            <Box>
              <Typography variant="body1">Customer Login</Typography>
              <Typography variant="caption" color="text.secondary">
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
              <Typography variant="body1">Employee Login</Typography>
              <Typography variant="caption" color="text.secondary">
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
              <PersonAddIcon fontSize="small" color="primary" />
            </ListItemIcon>
            <Box>
              <Typography variant="body1" color="primary" fontWeight={600}>
                Create New Account
              </Typography>
              <Typography variant="caption" color="text.secondary">
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
          justifyContent: 'center',
          minHeight: 'calc(100vh - 140px)',
          textAlign: 'center',
          px: 3,
        }}
      >
        <Typography
          variant="overline"
          sx={{
            color: 'white',
            letterSpacing: 4,
            fontWeight: 600,
            mb: 1,
            textShadow: '1px 1px 3px rgba(0,0,0,0.4)',
          }}
        >
          Artisan Bakery · Since Today
        </Typography>
        <Typography
          variant="h1"
          sx={{
            fontSize: { xs: '3rem', md: '5rem' },
            fontWeight: 800,
            letterSpacing: 4,
            color: 'white',
            textShadow: '2px 3px 10px rgba(0,0,0,0.5)',
            lineHeight: 1.1,
          }}
        >
          WELCOME TO DHATI
        </Typography>
        <Typography
          variant="h5"
          sx={{
            mt: 3,
            fontStyle: 'italic',
            color: 'white',
            textShadow: '1px 1px 4px rgba(0,0,0,0.5)',
            maxWidth: 640,
          }}
        >
          Special in Millets and normal bake items
        </Typography>

        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} sx={{ mt: 5 }}>
          <Button
            component={RouterLink}
            to="/products"
            variant="contained"
            size="large"
            startIcon={<StorefrontIcon />}
            sx={{
              px: 4,
              py: 1.5,
              fontSize: '1rem',
              fontWeight: 600,
              letterSpacing: 1,
              bgcolor: '#d84315',
              '&:hover': { bgcolor: '#bf360c' },
              boxShadow: 4,
            }}
          >
            Browse Our Products
          </Button>
          <Button
            component={RouterLink}
            to="/register"
            variant="outlined"
            size="large"
            sx={{
              px: 4,
              py: 1.5,
              fontSize: '1rem',
              fontWeight: 600,
              letterSpacing: 1,
              color: 'white',
              borderColor: 'white',
              borderWidth: 2,
              '&:hover': {
                borderWidth: 2,
                borderColor: 'white',
                bgcolor: 'rgba(255,255,255,0.1)',
              },
            }}
          >
            Join Us
          </Button>
        </Stack>
      </Box>
    </Box>
  );
}
