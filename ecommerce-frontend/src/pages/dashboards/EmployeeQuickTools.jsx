import { useNavigate } from 'react-router-dom';
import {
  Paper,
  Stack,
  Typography,
  Button,
  Divider,
  Box,
} from '@mui/material';
import StorefrontIcon from '@mui/icons-material/Storefront';
import ReceiptLongIcon from '@mui/icons-material/ReceiptLong';
import InventoryIcon from '@mui/icons-material/Inventory';
import AddShoppingCartIcon from '@mui/icons-material/AddShoppingCart';
import { RAINBOW_TEXT, GLASS_PAPER } from './dashboardStyles.js';

const TOOLS = [
  {
    label: 'Shop',
    hint: 'Browse products as a shopper',
    to: '/products',
    icon: StorefrontIcon,
    color: '#66bb6a',
  },
  {
    label: 'Orders',
    hint: 'Your order history & payments',
    to: '/payments',
    icon: ReceiptLongIcon,
    color: '#29b6f6',
  },
  {
    label: 'Manage Items',
    hint: 'Add items to the store catalog',
    to: '/manage-products',
    icon: InventoryIcon,
    color: '#ab47bc',
  },
  {
    label: 'Quick Add Product',
    hint: 'Add a single product fast',
    to: '/add-products',
    icon: AddShoppingCartIcon,
    color: '#ef5350',
  },
];

export default function EmployeeQuickTools() {
  const navigate = useNavigate();

  return (
    <Paper elevation={0} sx={{ ...GLASS_PAPER, mt: 4 }}>
      <Stack
        direction={{ xs: 'column', sm: 'row' }}
        alignItems={{ xs: 'flex-start', sm: 'center' }}
        spacing={1}
        sx={{ mb: 2 }}
      >
        <Typography
          variant="overline"
          sx={{ letterSpacing: 2, fontWeight: 800, ...RAINBOW_TEXT }}
        >
          More Tools
        </Typography>
        <Divider flexItem sx={{ display: { xs: 'none', sm: 'block' }, flex: 1 }} />
        <Typography
          variant="caption"
          sx={{ fontWeight: 600, color: '#8d6e63' }}
        >
          Secondary actions — not your main duties
        </Typography>
      </Stack>

      <Box
        sx={{
          display: 'grid',
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(2, 1fr)',
            md: 'repeat(4, 1fr)',
          },
          gap: 1.5,
        }}
      >
        {TOOLS.map(({ label, hint, to, icon: Icon, color }) => (
          <Button
            key={to}
            onClick={() => navigate(to)}
            disableRipple
            sx={{
              justifyContent: 'flex-start',
              textAlign: 'left',
              px: 2,
              py: 1.5,
              borderRadius: 2,
              border: '1px solid rgba(0,0,0,0.08)',
              bgcolor: 'rgba(255,255,255,0.6)',
              '&:hover': {
                bgcolor: 'rgba(255,255,255,0.85)',
                borderColor: 'rgba(0,0,0,0.18)',
              },
            }}
          >
            <Stack direction="row" spacing={1.5} alignItems="center">
              <Box
                sx={{
                  width: 38,
                  height: 38,
                  borderRadius: '50%',
                  display: 'grid',
                  placeItems: 'center',
                  bgcolor: `${color}22`,
                  flexShrink: 0,
                }}
              >
                <Icon sx={{ color, fontSize: 22 }} />
              </Box>
              <Box sx={{ minWidth: 0 }}>
                <Typography
                  sx={{
                    fontWeight: 800,
                    letterSpacing: 0.5,
                    textTransform: 'uppercase',
                    fontSize: '0.78rem',
                    ...RAINBOW_TEXT,
                  }}
                >
                  {label}
                </Typography>
                <Typography
                  variant="caption"
                  sx={{ color: '#5d4037', fontWeight: 600 }}
                >
                  {hint}
                </Typography>
              </Box>
            </Stack>
          </Button>
        ))}
      </Box>
    </Paper>
  );
}
