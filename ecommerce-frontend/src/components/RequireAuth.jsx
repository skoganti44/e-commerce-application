import { useSelector } from 'react-redux';
import { Navigate, useLocation } from 'react-router-dom';
import { Alert, Box } from '@mui/material';
import { selectCurrentUser, hasRole } from '../store/slices/authSlice.js';

export default function RequireAuth({ children, role }) {
  const currentUser = useSelector(selectCurrentUser);
  const location = useLocation();

  if (!currentUser) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (role && !hasRole(currentUser, role)) {
    return (
      <Box sx={{ mt: 4 }}>
        <Alert severity="warning">
          This page is restricted to {role}s only.
        </Alert>
      </Box>
    );
  }

  return children;
}
