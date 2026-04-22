import { useEffect, useMemo, useState } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { useNavigate } from 'react-router-dom';
import {
  Paper,
  Typography,
  Box,
  Button,
  Stack,
  RadioGroup,
  Radio,
  FormControlLabel,
  FormControl,
  FormLabel,
  TextField,
  Grid,
  Alert,
  Snackbar,
  Divider,
  InputAdornment,
  Chip,
  MenuItem,
  Autocomplete,
} from '@mui/material';
import CreditCardIcon from '@mui/icons-material/CreditCard';
import PaymentIcon from '@mui/icons-material/Payment';
import RedeemIcon from '@mui/icons-material/Redeem';
import LocalShippingIcon from '@mui/icons-material/LocalShipping';
import LockIcon from '@mui/icons-material/Lock';
import HomeIcon from '@mui/icons-material/Home';
import { loadCart } from '../store/slices/cartSlice.js';
import { loadPayments } from '../store/slices/paymentsSlice.js';
import { selectActiveUserId } from '../store/slices/sessionSlice.js';
import * as api from '../api/endpoints.js';

const RAINBOW_TEXT = {
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  backgroundClip: 'text',
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
  '&.Mui-disabled': {
    opacity: 0.45,
    WebkitTextFillColor: 'transparent',
  },
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

const RAINBOW_CHIP = {
  bgcolor: 'transparent',
  borderColor: 'rgba(0,0,0,0.18)',
  fontWeight: 700,
  letterSpacing: 0.5,
  '& .MuiChip-label': { ...RAINBOW_TEXT },
};

const HEADING_SX = {
  fontWeight: 800,
  textTransform: 'uppercase',
  letterSpacing: 1,
  ...RAINBOW_TEXT,
};

const METHODS = [
  { value: 'DEBIT_CARD', label: 'Debit Card', Icon: PaymentIcon },
  { value: 'CREDIT_CARD', label: 'Credit Card', Icon: CreditCardIcon },
  { value: 'GIFT_CARD', label: 'Gift Card', Icon: RedeemIcon },
  { value: 'COD', label: 'Cash on Delivery', Icon: LocalShippingIcon },
];

const US_STATES = [
  'Alabama', 'Alaska', 'Arizona', 'Arkansas', 'California', 'Colorado',
  'Connecticut', 'Delaware', 'District of Columbia', 'Florida', 'Georgia',
  'Hawaii', 'Idaho', 'Illinois', 'Indiana', 'Iowa', 'Kansas', 'Kentucky',
  'Louisiana', 'Maine', 'Maryland', 'Massachusetts', 'Michigan', 'Minnesota',
  'Mississippi', 'Missouri', 'Montana', 'Nebraska', 'Nevada', 'New Hampshire',
  'New Jersey', 'New Mexico', 'New York', 'North Carolina', 'North Dakota',
  'Ohio', 'Oklahoma', 'Oregon', 'Pennsylvania', 'Rhode Island',
  'South Carolina', 'South Dakota', 'Tennessee', 'Texas', 'Utah', 'Vermont',
  'Virginia', 'Washington', 'West Virginia', 'Wisconsin', 'Wyoming',
];

const CITIES_BY_STATE = {
  'Alabama': ['Birmingham', 'Montgomery', 'Mobile', 'Huntsville', 'Tuscaloosa'],
  'Alaska': ['Anchorage', 'Fairbanks', 'Juneau', 'Sitka', 'Ketchikan'],
  'Arizona': ['Phoenix', 'Tucson', 'Mesa', 'Chandler', 'Scottsdale', 'Glendale'],
  'Arkansas': ['Little Rock', 'Fort Smith', 'Fayetteville', 'Springdale', 'Jonesboro'],
  'California': ['Los Angeles', 'San Diego', 'San Jose', 'San Francisco', 'Fresno', 'Sacramento', 'Long Beach', 'Oakland', 'Bakersfield', 'Anaheim'],
  'Colorado': ['Denver', 'Colorado Springs', 'Aurora', 'Fort Collins', 'Lakewood', 'Boulder'],
  'Connecticut': ['Bridgeport', 'New Haven', 'Hartford', 'Stamford', 'Waterbury'],
  'Delaware': ['Wilmington', 'Dover', 'Newark', 'Middletown', 'Smyrna'],
  'District of Columbia': ['Washington'],
  'Florida': ['Jacksonville', 'Miami', 'Tampa', 'Orlando', 'St. Petersburg', 'Hialeah', 'Tallahassee', 'Fort Lauderdale'],
  'Georgia': ['Atlanta', 'Augusta', 'Columbus', 'Savannah', 'Athens', 'Macon'],
  'Hawaii': ['Honolulu', 'Hilo', 'Kailua', 'Kapolei', 'Kaneohe'],
  'Idaho': ['Boise', 'Meridian', 'Nampa', 'Idaho Falls', 'Pocatello'],
  'Illinois': ['Chicago', 'Aurora', 'Naperville', 'Joliet', 'Rockford', 'Springfield'],
  'Indiana': ['Indianapolis', 'Fort Wayne', 'Evansville', 'South Bend', 'Carmel'],
  'Iowa': ['Des Moines', 'Cedar Rapids', 'Davenport', 'Sioux City', 'Iowa City'],
  'Kansas': ['Wichita', 'Overland Park', 'Kansas City', 'Olathe', 'Topeka'],
  'Kentucky': ['Louisville', 'Lexington', 'Bowling Green', 'Owensboro', 'Covington'],
  'Louisiana': ['New Orleans', 'Baton Rouge', 'Shreveport', 'Lafayette', 'Lake Charles'],
  'Maine': ['Portland', 'Lewiston', 'Bangor', 'South Portland', 'Auburn'],
  'Maryland': ['Baltimore', 'Frederick', 'Rockville', 'Gaithersburg', 'Bowie', 'Annapolis'],
  'Massachusetts': ['Boston', 'Worcester', 'Springfield', 'Cambridge', 'Lowell', 'Brockton'],
  'Michigan': ['Detroit', 'Grand Rapids', 'Warren', 'Sterling Heights', 'Ann Arbor', 'Lansing'],
  'Minnesota': ['Minneapolis', 'Saint Paul', 'Rochester', 'Duluth', 'Bloomington'],
  'Mississippi': ['Jackson', 'Gulfport', 'Southaven', 'Hattiesburg', 'Biloxi'],
  'Missouri': ['Kansas City', 'Saint Louis', 'Springfield', 'Columbia', 'Independence'],
  'Montana': ['Billings', 'Missoula', 'Great Falls', 'Bozeman', 'Helena'],
  'Nebraska': ['Omaha', 'Lincoln', 'Bellevue', 'Grand Island', 'Kearney'],
  'Nevada': ['Las Vegas', 'Henderson', 'Reno', 'North Las Vegas', 'Sparks', 'Carson City'],
  'New Hampshire': ['Manchester', 'Nashua', 'Concord', 'Dover', 'Rochester'],
  'New Jersey': ['Newark', 'Jersey City', 'Paterson', 'Elizabeth', 'Edison', 'Trenton'],
  'New Mexico': ['Albuquerque', 'Las Cruces', 'Rio Rancho', 'Santa Fe', 'Roswell'],
  'New York': ['New York City', 'Buffalo', 'Rochester', 'Yonkers', 'Syracuse', 'Albany'],
  'North Carolina': ['Charlotte', 'Raleigh', 'Greensboro', 'Durham', 'Winston-Salem', 'Fayetteville'],
  'North Dakota': ['Fargo', 'Bismarck', 'Grand Forks', 'Minot', 'West Fargo'],
  'Ohio': ['Columbus', 'Cleveland', 'Cincinnati', 'Toledo', 'Akron', 'Dayton'],
  'Oklahoma': ['Oklahoma City', 'Tulsa', 'Norman', 'Broken Arrow', 'Edmond'],
  'Oregon': ['Portland', 'Salem', 'Eugene', 'Gresham', 'Hillsboro', 'Bend'],
  'Pennsylvania': ['Philadelphia', 'Pittsburgh', 'Allentown', 'Erie', 'Reading', 'Harrisburg'],
  'Rhode Island': ['Providence', 'Warwick', 'Cranston', 'Pawtucket', 'East Providence'],
  'South Carolina': ['Columbia', 'Charleston', 'North Charleston', 'Mount Pleasant', 'Rock Hill', 'Greenville'],
  'South Dakota': ['Sioux Falls', 'Rapid City', 'Aberdeen', 'Brookings', 'Watertown', 'Pierre'],
  'Tennessee': ['Nashville', 'Memphis', 'Knoxville', 'Chattanooga', 'Clarksville', 'Murfreesboro'],
  'Texas': ['Houston', 'San Antonio', 'Dallas', 'Austin', 'Fort Worth', 'El Paso', 'Arlington', 'Plano'],
  'Utah': ['Salt Lake City', 'West Valley City', 'Provo', 'West Jordan', 'Orem', 'Sandy'],
  'Vermont': ['Burlington', 'Essex', 'South Burlington', 'Colchester', 'Rutland', 'Montpelier'],
  'Virginia': ['Virginia Beach', 'Norfolk', 'Chesapeake', 'Richmond', 'Newport News', 'Alexandria', 'Arlington'],
  'Washington': ['Seattle', 'Spokane', 'Tacoma', 'Vancouver', 'Bellevue', 'Kent', 'Olympia'],
  'West Virginia': ['Charleston', 'Huntington', 'Morgantown', 'Parkersburg', 'Wheeling'],
  'Wisconsin': ['Milwaukee', 'Madison', 'Green Bay', 'Kenosha', 'Racine', 'Appleton'],
  'Wyoming': ['Cheyenne', 'Casper', 'Laramie', 'Gillette', 'Rock Springs'],
};

const ADDRESS_STORAGE_KEY = 'dhati.shippingAddress';

const emptyAddress = () => ({
  fullName: '',
  phone: '',
  line1: '',
  line2: '',
  landmark: '',
  city: '',
  state: '',
  pincode: '',
  country: 'United States',
  instructions: '',
  addressType: 'HOME',
});

const formatPrice = (v) => {
  if (v == null) return '—';
  const n = Number(v);
  if (Number.isNaN(n)) return v;
  return `$${n.toFixed(2)}`;
};

const formatCardNumber = (raw) => {
  const digits = raw.replace(/\D/g, '').slice(0, 16);
  return digits.replace(/(\d{4})(?=\d)/g, '$1 ');
};

const formatExpiry = (raw) => {
  const digits = raw.replace(/\D/g, '').slice(0, 4);
  if (digits.length < 3) return digits;
  return `${digits.slice(0, 2)}/${digits.slice(2)}`;
};

const detectBrand = (cardNum) => {
  const d = cardNum.replace(/\s/g, '');
  if (/^4/.test(d)) return 'Visa';
  if (/^(5[1-5]|2[2-7])/.test(d)) return 'Mastercard';
  if (/^3[47]/.test(d)) return 'Amex';
  if (/^6/.test(d)) return 'RuPay';
  return null;
};

export default function Checkout() {
  const navigate = useNavigate();
  const dispatch = useDispatch();
  const userId = useSelector(selectActiveUserId);
  const { items, totals } = useSelector((s) => s.cart);

  const [address, setAddress] = useState(() => {
    try {
      const saved = localStorage.getItem(ADDRESS_STORAGE_KEY);
      if (saved) return { ...emptyAddress(), ...JSON.parse(saved) };
    } catch {
      /* ignore */
    }
    return emptyAddress();
  });
  const [addressConfirmed, setAddressConfirmed] = useState(false);

  const [method, setMethod] = useState('DEBIT_CARD');
  const [card, setCard] = useState({
    number: '',
    name: '',
    expiry: '',
    cvv: '',
  });
  const [gift, setGift] = useState({ code: '', pin: '' });

  const [errors, setErrors] = useState({});
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const [placing, setPlacing] = useState(false);

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    api
      .fetchShippingAddress(userId)
      .then((data) => {
        if (cancelled) return;
        if (data && data.fullName) {
          setAddress((a) => ({ ...a, ...data }));
        }
      })
      .catch(() => {
        /* ignore */
      });
    return () => {
      cancelled = true;
    };
  }, [userId]);

  const subtotal = Number(totals?.subtotal ?? 0);
  const isCard = method === 'DEBIT_CARD' || method === 'CREDIT_CARD';
  const brand = useMemo(() => (isCard ? detectBrand(card.number) : null), [isCard, card.number]);
  const cityOptions = CITIES_BY_STATE[address.state] || [];

  const setAddressField = (key, value) => {
    setAddress((a) => ({ ...a, [key]: value }));
    if (errors[key]) setErrors((e) => ({ ...e, [key]: undefined }));
  };

  const validateAddress = () => {
    const e = {};
    if (!address.fullName.trim()) e.fullName = 'Full name is required';
    if (!/^\d{10}$/.test(address.phone)) e.phone = 'Enter a 10-digit phone number';
    if (!address.line1.trim()) e.line1 = 'Address line 1 is required';
    if (!address.city.trim()) e.city = 'City is required';
    if (!address.state) e.state = 'Select a state';
    if (!/^\d{5}(-\d{4})?$/.test(address.pincode)) e.pincode = 'Enter a valid ZIP (5 digits or ZIP+4)';
    return e;
  };

  const validatePayment = () => {
    const e = {};
    if (isCard) {
      const digits = card.number.replace(/\s/g, '');
      if (digits.length < 12 || digits.length > 19) e.number = 'Enter a valid card number';
      if (!card.name.trim()) e.name = 'Cardholder name is required';
      if (!/^\d{2}\/\d{2}$/.test(card.expiry)) e.expiry = 'Use MM/YY';
      else {
        const [mm, yy] = card.expiry.split('/').map(Number);
        const now = new Date();
        const year2 = now.getFullYear() % 100;
        const month = now.getMonth() + 1;
        if (mm < 1 || mm > 12) e.expiry = 'Invalid month';
        else if (yy < year2 || (yy === year2 && mm < month)) e.expiry = 'Card is expired';
      }
      if (!/^\d{3,4}$/.test(card.cvv)) e.cvv = 'CVV must be 3-4 digits';
    }
    if (method === 'GIFT_CARD') {
      if (!/^[A-Za-z0-9-]{8,}$/.test(gift.code.trim())) e.code = 'Enter a valid gift card code';
      if (!/^\d{4,6}$/.test(gift.pin)) e.pin = 'PIN must be 4-6 digits';
    }
    return e;
  };

  const confirmAddress = () => {
    const e = validateAddress();
    setErrors(e);
    if (Object.keys(e).length > 0) {
      setSnack({
        open: true,
        message: 'Please fix the highlighted address fields.',
        severity: 'error',
      });
      return;
    }
    try {
      localStorage.setItem(ADDRESS_STORAGE_KEY, JSON.stringify(address));
    } catch {
      /* ignore */
    }
    setAddressConfirmed(true);
    setSnack({
      open: true,
      message: 'Address saved. Now choose your payment method.',
      severity: 'success',
    });
  };

  const placeOrder = async () => {
    if (items.length === 0) {
      setSnack({ open: true, message: 'Your cart is empty.', severity: 'warning' });
      return;
    }
    const addrErrors = validateAddress();
    const payErrors = validatePayment();
    const combined = { ...addrErrors, ...payErrors };
    setErrors(combined);
    if (Object.keys(combined).length > 0) {
      if (Object.keys(addrErrors).length > 0) setAddressConfirmed(false);
      setSnack({
        open: true,
        message: 'Please fix the highlighted fields.',
        severity: 'error',
      });
      return;
    }

    setPlacing(true);
    const digits = card.number.replace(/\s/g, '');
    const cardLast4 = isCard ? digits.slice(-4) : null;
    try {
      const result = await api.checkout({
        userid: userId,
        paymentMethod: method,
        address,
        cardLast4,
      });
      try {
        sessionStorage.setItem('dhati.lastCheckout', JSON.stringify(result));
      } catch {
        /* ignore */
      }
      dispatch(loadCart(userId));
      dispatch(loadPayments({ userid: userId, includeAll: false }));
      setSnack({
        open: true,
        message: `Order #${result.orderId} placed (${result.paymentStatus}). Total $${Number(result.totalAmount).toFixed(2)}.`,
        severity: 'success',
      });
      setTimeout(() => navigate('/payments'), 1200);
    } catch (err) {
      const msg = err?.response?.data?.error || err.message || 'Failed to place order';
      setSnack({ open: true, message: msg, severity: 'error' });
    } finally {
      setPlacing(false);
    }
  };

  if (items.length === 0) {
    return (
      <Paper sx={{ p: 4, textAlign: 'center' }}>
        <Typography
          variant="h6"
          gutterBottom
          sx={{ fontWeight: 800, ...RAINBOW_TEXT }}
        >
          Your cart is empty
        </Typography>
        <Typography sx={{ mb: 2, fontWeight: 600, ...RAINBOW_TEXT }}>
          Add products to your cart before checking out.
        </Typography>
        <Button onClick={() => navigate('/products')} sx={RAINBOW_FILLED_BTN}>
          Browse Products
        </Button>
      </Paper>
    );
  }

  return (
    <Stack spacing={3}>
      <Paper sx={{ p: 3 }}>
        <Typography
          variant="h4"
          sx={{
            textAlign: 'center',
            fontWeight: 800,
            letterSpacing: 2,
            mb: 1,
            background:
              'linear-gradient(90deg,#ef5350 0%,#ffb300 25%,#66bb6a 50%,#29b6f6 75%,#ab47bc 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
            backgroundClip: 'text',
          }}
        >
          Checkout
        </Typography>
        <Typography
          variant="body2"
          sx={{ textAlign: 'center', fontWeight: 600, ...RAINBOW_TEXT }}
        >
          <LockIcon sx={{ fontSize: 14, verticalAlign: 'middle', mr: 0.5, color: '#d84315' }} />
          Demo only — no real card data is processed.
        </Typography>
      </Paper>

      <Grid container spacing={3}>
        <Grid item xs={12} md={7}>
          <Stack spacing={3}>
            <Paper sx={{ p: 3 }}>
              <Stack direction="row" spacing={1.5} alignItems="center" sx={{ mb: 2 }}>
                <LocalShippingIcon sx={{ color: '#29b6f6' }} />
                <Typography variant="h6" sx={{ flexGrow: 1, ...HEADING_SX }}>
                  1. Shipping Address
                </Typography>
                {addressConfirmed && (
                  <Chip label="Saved" variant="outlined" size="small" sx={RAINBOW_CHIP} />
                )}
              </Stack>

              {!addressConfirmed ? (
                <>
                  <Alert severity="info" sx={{ mb: 2 }}>
                    Please provide the delivery details so we know where to send your package.
                  </Alert>
                  <Grid container spacing={2}>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Full name"
                        value={address.fullName}
                        onChange={(e) => setAddressField('fullName', e.target.value)}
                        error={Boolean(errors.fullName)}
                        helperText={errors.fullName}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Phone number"
                        value={address.phone}
                        onChange={(e) =>
                          setAddressField('phone', e.target.value.replace(/\D/g, '').slice(0, 10))
                        }
                        inputProps={{ inputMode: 'numeric' }}
                        error={Boolean(errors.phone)}
                        helperText={errors.phone}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Address line 1 (House no, Building, Street)"
                        value={address.line1}
                        onChange={(e) => setAddressField('line1', e.target.value)}
                        error={Boolean(errors.line1)}
                        helperText={errors.line1}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Address line 2 (Apt, Suite) — optional"
                        value={address.line2}
                        onChange={(e) => setAddressField('line2', e.target.value)}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        label="Landmark — optional"
                        value={address.landmark}
                        onChange={(e) => setAddressField('landmark', e.target.value)}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        select
                        label="State"
                        value={address.state}
                        onChange={(e) => {
                          const nextState = e.target.value;
                          setAddress((a) => ({
                            ...a,
                            state: nextState,
                            city:
                              a.city && CITIES_BY_STATE[nextState]?.includes(a.city)
                                ? a.city
                                : '',
                          }));
                          if (errors.state) setErrors((err) => ({ ...err, state: undefined }));
                        }}
                        error={Boolean(errors.state)}
                        helperText={errors.state}
                        fullWidth
                      >
                        {US_STATES.map((s) => (
                          <MenuItem key={s} value={s}>
                            {s}
                          </MenuItem>
                        ))}
                      </TextField>
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <Autocomplete
                        freeSolo
                        options={cityOptions}
                        value={address.city}
                        onChange={(_, newValue) => setAddressField('city', newValue || '')}
                        onInputChange={(_, newInputValue) =>
                          setAddressField('city', newInputValue || '')
                        }
                        disabled={!address.state}
                        renderInput={(params) => (
                          <TextField
                            {...params}
                            label="City"
                            placeholder={
                              address.state ? 'Select or type a city' : 'Select a state first'
                            }
                            error={Boolean(errors.city)}
                            helperText={
                              errors.city ||
                              (address.state && cityOptions.length > 0
                                ? `Suggestions for ${address.state}`
                                : '')
                            }
                            fullWidth
                          />
                        )}
                      />
                    </Grid>
                    <Grid item xs={6} sm={3}>
                      <TextField
                        label="ZIP code"
                        value={address.pincode}
                        onChange={(e) => {
                          const digits = e.target.value.replace(/\D/g, '').slice(0, 9);
                          const formatted =
                            digits.length > 5
                              ? `${digits.slice(0, 5)}-${digits.slice(5)}`
                              : digits;
                          setAddressField('pincode', formatted);
                        }}
                        inputProps={{ inputMode: 'numeric' }}
                        placeholder="12345 or 12345-6789"
                        error={Boolean(errors.pincode)}
                        helperText={errors.pincode}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={6} sm={3}>
                      <TextField
                        label="Country"
                        value={address.country}
                        onChange={(e) => setAddressField('country', e.target.value)}
                        fullWidth
                      />
                    </Grid>
                    <Grid item xs={12} sm={6}>
                      <TextField
                        select
                        label="Address type"
                        value={address.addressType}
                        onChange={(e) => setAddressField('addressType', e.target.value)}
                        fullWidth
                      >
                        <MenuItem value="HOME">Home</MenuItem>
                        <MenuItem value="WORK">Work</MenuItem>
                        <MenuItem value="OTHER">Other</MenuItem>
                      </TextField>
                    </Grid>
                    <Grid item xs={12}>
                      <TextField
                        label="Delivery instructions — optional"
                        value={address.instructions}
                        onChange={(e) => setAddressField('instructions', e.target.value)}
                        placeholder="e.g., Leave at the door, ring twice"
                        multiline
                        minRows={2}
                        fullWidth
                      />
                    </Grid>
                  </Grid>
                  <Box sx={{ mt: 2, display: 'flex', justifyContent: 'flex-end' }}>
                    <Button
                      startIcon={<HomeIcon sx={{ color: '#fff' }} />}
                      onClick={confirmAddress}
                      sx={RAINBOW_FILLED_BTN}
                    >
                      Save Address &amp; Continue
                    </Button>
                  </Box>
                </>
              ) : (
                <Box
                  sx={{
                    p: 2,
                    borderRadius: 2,
                    bgcolor: 'grey.50',
                    border: 1,
                    borderColor: 'divider',
                    display: 'flex',
                    gap: 2,
                    alignItems: 'flex-start',
                  }}
                >
                  <HomeIcon sx={{ mt: 0.5, color: '#66bb6a' }} />
                  <Box sx={{ flexGrow: 1 }}>
                    <Typography sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                      {address.fullName}{' '}
                      <Chip
                        size="small"
                        label={address.addressType}
                        sx={{ ml: 1, ...RAINBOW_CHIP }}
                        variant="outlined"
                      />
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                      {[address.line1, address.line2, address.landmark]
                        .filter(Boolean)
                        .join(', ')}
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                      {address.city}, {address.state} {address.pincode}, {address.country}
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                      Phone: {address.phone}
                    </Typography>
                    {address.instructions && (
                      <Typography variant="caption" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                        Note: {address.instructions}
                      </Typography>
                    )}
                  </Box>
                  <Button
                    size="small"
                    onClick={() => setAddressConfirmed(false)}
                    disableRipple
                    sx={RAINBOW_BTN}
                  >
                    Change
                  </Button>
                </Box>
              )}
            </Paper>

            <Paper sx={{ p: 3, opacity: addressConfirmed ? 1 : 0.55, pointerEvents: addressConfirmed ? 'auto' : 'none' }}>
              <Typography variant="h6" sx={{ mb: 2, ...HEADING_SX }}>
                2. Payment Method
              </Typography>
              {!addressConfirmed && (
                <Alert severity="info" sx={{ mb: 2 }}>
                  Save your shipping address first to unlock payment.
                </Alert>
              )}
              <FormControl fullWidth>
                <FormLabel sx={{ mb: 1, '&.Mui-focused': { ...RAINBOW_TEXT }, ...HEADING_SX }}>
                  Select payment method
                </FormLabel>
                <RadioGroup
                  value={method}
                  onChange={(e) => {
                    setMethod(e.target.value);
                    setErrors({});
                  }}
                >
                  <Grid container spacing={1}>
                    {METHODS.map((m) => (
                      <Grid item xs={12} sm={6} key={m.value}>
                        <Paper
                          variant="outlined"
                          sx={{
                            p: 1.5,
                            borderColor: method === m.value ? 'primary.main' : 'divider',
                            borderWidth: method === m.value ? 2 : 1,
                            cursor: 'pointer',
                          }}
                          onClick={() => {
                            setMethod(m.value);
                            setErrors({});
                          }}
                        >
                          <FormControlLabel
                            value={m.value}
                            control={<Radio />}
                            label={
                              <Stack direction="row" spacing={1} alignItems="center">
                                <m.Icon sx={{ color: method === m.value ? '#d84315' : '#9e9e9e' }} />
                                <Typography sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                                  {m.label}
                                </Typography>
                              </Stack>
                            }
                            sx={{ m: 0, width: '100%' }}
                          />
                        </Paper>
                      </Grid>
                    ))}
                  </Grid>
                </RadioGroup>
              </FormControl>

              <Divider sx={{ my: 3 }} />

              {isCard && (
                <Stack spacing={2}>
                  <Typography variant="subtitle1" sx={HEADING_SX}>
                    Enter card details
                  </Typography>
                  <TextField
                    label="Card number"
                    value={card.number}
                    onChange={(e) =>
                      setCard((c) => ({ ...c, number: formatCardNumber(e.target.value) }))
                    }
                    placeholder="0000 0000 0000 0000"
                    inputProps={{ inputMode: 'numeric', autoComplete: 'cc-number' }}
                    error={Boolean(errors.number)}
                    helperText={errors.number}
                    fullWidth
                    InputProps={{
                      endAdornment: brand ? (
                        <InputAdornment position="end">
                          <Chip size="small" label={brand} variant="outlined" sx={RAINBOW_CHIP} />
                        </InputAdornment>
                      ) : null,
                    }}
                  />
                  <TextField
                    label="Name on card"
                    value={card.name}
                    onChange={(e) => setCard((c) => ({ ...c, name: e.target.value }))}
                    inputProps={{ autoComplete: 'cc-name' }}
                    error={Boolean(errors.name)}
                    helperText={errors.name}
                    fullWidth
                  />
                  <Stack direction="row" spacing={2}>
                    <TextField
                      label="Expiry (MM/YY)"
                      value={card.expiry}
                      onChange={(e) =>
                        setCard((c) => ({ ...c, expiry: formatExpiry(e.target.value) }))
                      }
                      placeholder="MM/YY"
                      inputProps={{ inputMode: 'numeric', autoComplete: 'cc-exp' }}
                      error={Boolean(errors.expiry)}
                      helperText={errors.expiry}
                      sx={{ flex: 1 }}
                    />
                    <TextField
                      label="CVV"
                      type="password"
                      value={card.cvv}
                      onChange={(e) =>
                        setCard((c) => ({ ...c, cvv: e.target.value.replace(/\D/g, '').slice(0, 4) }))
                      }
                      inputProps={{ inputMode: 'numeric', autoComplete: 'cc-csc' }}
                      error={Boolean(errors.cvv)}
                      helperText={errors.cvv}
                      sx={{ flex: 1 }}
                    />
                  </Stack>
                </Stack>
              )}

              {method === 'GIFT_CARD' && (
                <Stack spacing={2}>
                  <Typography variant="subtitle1" sx={HEADING_SX}>
                    Enter gift card details
                  </Typography>
                  <TextField
                    label="Gift card code"
                    value={gift.code}
                    onChange={(e) => setGift((g) => ({ ...g, code: e.target.value.toUpperCase() }))}
                    placeholder="ABCD-1234-EFGH"
                    error={Boolean(errors.code)}
                    helperText={errors.code}
                    fullWidth
                  />
                  <TextField
                    label="PIN"
                    type="password"
                    value={gift.pin}
                    onChange={(e) =>
                      setGift((g) => ({ ...g, pin: e.target.value.replace(/\D/g, '').slice(0, 6) }))
                    }
                    inputProps={{ inputMode: 'numeric' }}
                    error={Boolean(errors.pin)}
                    helperText={errors.pin}
                    sx={{ maxWidth: 220 }}
                  />
                </Stack>
              )}

              {method === 'COD' && (
                <Alert severity="info">
                  You'll pay {formatPrice(subtotal)} in cash when your order arrives at the address above.
                </Alert>
              )}
            </Paper>
          </Stack>
        </Grid>

        <Grid item xs={12} md={5}>
          <Paper sx={{ p: 3, position: 'sticky', top: 80 }}>
            <Typography variant="subtitle1" sx={{ mb: 2, ...HEADING_SX }}>
              Order summary
            </Typography>
            <OrderSummaryLines />
            <Divider sx={{ my: 2 }} />
            <Stack direction="row" justifyContent="space-between" sx={{ mb: 2 }}>
              <Typography variant="h6" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                Total
              </Typography>
              <Typography variant="h6" sx={{ fontWeight: 800, ...RAINBOW_TEXT }}>
                {formatPrice(subtotal)}
              </Typography>
            </Stack>
            <Stack direction="row" spacing={1}>
              <Button
                onClick={() => navigate('/cart')}
                disableRipple
                fullWidth
                sx={RAINBOW_BTN}
              >
                Back to Cart
              </Button>
              <Button
                onClick={placeOrder}
                disabled={placing || !addressConfirmed}
                fullWidth
                startIcon={<LockIcon sx={{ color: '#fff' }} />}
                sx={RAINBOW_FILLED_BTN}
              >
                {placing ? 'Placing…' : `Pay ${formatPrice(subtotal)}`}
              </Button>
            </Stack>
            {!addressConfirmed && (
              <Typography
                variant="caption"
                sx={{ mt: 1, display: 'block', fontWeight: 600, ...RAINBOW_TEXT }}
              >
                Save your shipping address to place the order.
              </Typography>
            )}
          </Paper>
        </Grid>
      </Grid>

      <Snackbar
        open={snack.open}
        autoHideDuration={3000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snack.severity} sx={{ width: '100%' }}>
          {snack.message}
        </Alert>
      </Snackbar>
    </Stack>
  );
}

function OrderSummaryLines() {
  const { items, itemTotals } = useSelector((s) => s.cart);
  return (
    <Stack spacing={0.8}>
      {items.map((it) => {
        const pricing = itemTotals?.[it.id] || {};
        const line = Number(
          pricing.lineTotal ??
            (Number(it.product?.price ?? 0) * (it.quantity || 0))
        );
        return (
          <Stack
            key={it.id}
            direction="row"
            justifyContent="space-between"
            alignItems="flex-start"
          >
            <Box>
              <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
                {it.product?.name} × {it.quantity}
              </Typography>
              {(it.sweetenerType || it.flourType) && (
                <Typography variant="caption" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
                  {[
                    it.sweetenerType && it.sweetenerType.replace(/_/g, ' ').toLowerCase(),
                    it.flourType && it.flourType.replace(/_/g, ' ').toLowerCase(),
                  ]
                    .filter(Boolean)
                    .join(' · ')}
                </Typography>
              )}
            </Box>
            <Typography variant="body2" sx={{ fontWeight: 700, ...RAINBOW_TEXT }}>
              {formatPrice(line)}
            </Typography>
          </Stack>
        );
      })}
    </Stack>
  );
}
