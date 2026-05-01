import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import {
  Box,
  Paper,
  Typography,
  Button,
  Stack,
  Grid,
} from '@mui/material';
import RestaurantMenuIcon from '@mui/icons-material/RestaurantMenu';
import StorefrontIcon from '@mui/icons-material/Storefront';
import KitchenIcon from '@mui/icons-material/Kitchen';
import InventoryIcon from '@mui/icons-material/Inventory2';
import { selectCurrentUser } from '../../store/slices/authSlice.js';
import {
  RAINBOW_TEXT,
  RAINBOW_FILLED_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';
import EmployeeQuickTools from './EmployeeQuickTools.jsx';
import MyTasks from './MyTasks.jsx';

const CARDS = [
  {
    title: 'Online Orders',
    subtitle: 'See orders placed online and get baking.',
    icon: RestaurantMenuIcon,
    to: '/dashboard/kitchen/online-orders',
  },
  {
    title: 'In-Store Orders & Stock',
    subtitle:
      'Walk-in orders plus how many cookies and cakes to prep today.',
    icon: StorefrontIcon,
    to: '/dashboard/kitchen/instore-orders',
  },
  {
    title: 'Order Supplies',
    subtitle:
      'Request appliances, flour, sweeteners, baking utensils & more.',
    icon: KitchenIcon,
    to: '/dashboard/kitchen/supplies',
  },
  {
    title: 'In Stock',
    subtitle:
      'See what is on the pantry shelf right now and request refills.',
    icon: InventoryIcon,
    to: '/dashboard/kitchen/in-stock',
  },
];

export default function KitchenDashboard() {
  const navigate = useNavigate();
  const user = useSelector(selectCurrentUser);
  const name = user?.name || 'Chef';

  return (
    <Box>
      <Paper
        elevation={0}
        sx={{
          borderRadius: 4,
          mb: 4,
          textAlign: 'center',
          p: { xs: 3, md: 5 },
          bgcolor: 'rgba(255,255,255,0.78)',
          backdropFilter: 'blur(8px)',
          border: '1px solid rgba(0,0,0,0.06)',
          boxShadow: '0 10px 30px rgba(60,30,10,0.12)',
        }}
      >
        <Typography
          variant="overline"
          sx={{ letterSpacing: 2, fontWeight: 700, ...RAINBOW_TEXT }}
        >
          Kitchen
        </Typography>
        <Typography
          variant="h3"
          sx={{
            fontWeight: 800,
            letterSpacing: 1,
            mt: 0.5,
            ...RAINBOW_TEXT,
          }}
        >
          Welcome to the Kitchen, {name}
        </Typography>
        <Typography
          variant="h6"
          sx={{ mt: 1, fontWeight: 600, ...RAINBOW_TEXT }}
        >
          Let's bake something wonderful today.
        </Typography>
      </Paper>

      <Grid container spacing={3}>
        {CARDS.map((card) => {
          const Icon = card.icon;
          return (
            <Grid item xs={12} sm={6} md={3} key={card.to}>
              <Paper elevation={0} sx={{ ...GLASS_PAPER, height: '100%' }}>
                <Stack spacing={2} sx={{ height: '100%' }}>
                  <Stack direction="row" spacing={1.5} alignItems="center">
                    <Icon sx={{ color: '#ef5350', fontSize: 34 }} />
                    <Typography
                      variant="h6"
                      sx={{
                        fontWeight: 800,
                        textTransform: 'uppercase',
                        letterSpacing: 1,
                        ...RAINBOW_TEXT,
                      }}
                    >
                      {card.title}
                    </Typography>
                  </Stack>
                  <Typography
                    variant="body2"
                    sx={{ fontWeight: 600, ...RAINBOW_TEXT, flexGrow: 1 }}
                  >
                    {card.subtitle}
                  </Typography>
                  <Button
                    onClick={() => navigate(card.to)}
                    sx={{ ...RAINBOW_FILLED_BTN, py: 1.2 }}
                  >
                    Open
                  </Button>
                </Stack>
              </Paper>
            </Grid>
          );
        })}
      </Grid>

      <MyTasks department="kitchen" />
      <EmployeeQuickTools />
    </Box>
  );
}
