import { useEffect, useState } from 'react';
import { Chip } from '@mui/material';
import AccessTimeIcon from '@mui/icons-material/AccessTime';

function minutesSince(iso) {
  if (!iso) return null;
  const t = new Date(iso).getTime();
  if (Number.isNaN(t)) return null;
  return Math.max(0, Math.floor((Date.now() - t) / 60000));
}

function styleFor(mins) {
  if (mins == null) return { bg: '#f5f5f5', fg: '#555' };
  if (mins >= 20) return { bg: '#ffebee', fg: '#c62828' };
  if (mins >= 10) return { bg: '#fff3e0', fg: '#ef6c00' };
  return { bg: '#e8f5e9', fg: '#2e7d32' };
}

function format(mins) {
  if (mins == null) return '—';
  if (mins < 60) return `${mins}m`;
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${h}h ${m}m`;
}

export default function WaitingTime({ since }) {
  const [mins, setMins] = useState(() => minutesSince(since));
  useEffect(() => {
    setMins(minutesSince(since));
    const id = setInterval(() => setMins(minutesSince(since)), 30000);
    return () => clearInterval(id);
  }, [since]);
  const style = styleFor(mins);
  return (
    <Chip
      size="small"
      icon={<AccessTimeIcon sx={{ color: `${style.fg} !important`, fontSize: 16 }} />}
      label={format(mins)}
      sx={{
        bgcolor: style.bg,
        color: style.fg,
        fontWeight: 800,
        letterSpacing: 0.3,
      }}
    />
  );
}
