import { createTheme } from '@mui/material/styles';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#1976d2' },
    secondary: { main: '#26a69a' },
    background: { default: '#f5f7fa' },
  },
  typography: {
    fontFamily: 'Roboto, Arial, sans-serif',
  },
  shape: { borderRadius: 8 },
});

export default theme;
