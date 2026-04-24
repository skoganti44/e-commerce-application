import { Link as RouterLink } from 'react-router-dom';
import { Paper, Typography, Button, Stack, Box } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import {
  RAINBOW_TEXT,
  RAINBOW_OUTLINE_BTN,
  GLASS_PAPER,
} from './dashboardStyles.js';
import EmployeeQuickTools from './EmployeeQuickTools.jsx';

export default function DashboardStub({
  title,
  description,
  backTo = '/',
  showQuickTools = false,
}) {
  return (
    <Box sx={{ maxWidth: 900, mx: 'auto' }}>
      <Paper elevation={0} sx={{ ...GLASS_PAPER }}>
        <Stack spacing={2}>
          <Typography
            variant="h4"
            sx={{
              fontWeight: 800,
              textTransform: 'uppercase',
              letterSpacing: 1,
              ...RAINBOW_TEXT,
            }}
          >
            {title}
          </Typography>
          <Typography variant="body1" sx={{ fontWeight: 600, ...RAINBOW_TEXT }}>
            {description}
          </Typography>
          <Box
            sx={{
              border: '1px dashed rgba(0,0,0,0.15)',
              borderRadius: 2,
              p: 3,
              textAlign: 'center',
              color: '#6d4c41',
              fontWeight: 600,
            }}
          >
            This page is a placeholder. Content will be wired to backend data
            next.
          </Box>
          <Button
            component={RouterLink}
            to={backTo}
            startIcon={<ArrowBackIcon sx={{ color: '#5d4037' }} />}
            disableRipple
            sx={RAINBOW_OUTLINE_BTN}
          >
            Back
          </Button>
        </Stack>
      </Paper>
      {showQuickTools && <EmployeeQuickTools />}
    </Box>
  );
}
