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
} from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import AccountCircleIcon from '@mui/icons-material/AccountCircle';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import PersonIcon from '@mui/icons-material/Person';
import BadgeIcon from '@mui/icons-material/Badge';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import { selectCurrentUser } from '../store/slices/authSlice.js';

export default function Home() {
  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);
  const currentUser = useSelector(selectCurrentUser);

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
      {!currentUser && (
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
      )}

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
