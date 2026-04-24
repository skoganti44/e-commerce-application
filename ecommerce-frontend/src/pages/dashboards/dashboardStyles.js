export const RAINBOW_TEXT = {
  background:
    'linear-gradient(90deg,#ef5350 0%,#ffb300 20%,#66bb6a 45%,#29b6f6 70%,#ab47bc 100%)',
  WebkitBackgroundClip: 'text',
  WebkitTextFillColor: 'transparent',
  backgroundClip: 'text',
};

export const RAINBOW_FILLED_BTN = {
  fontWeight: 700,
  letterSpacing: 1,
  textTransform: 'uppercase',
  color: '#fff',
  background: 'linear-gradient(90deg,#ef5350 0%,#ffb300 50%,#66bb6a 100%)',
  boxShadow: 'none',
  '&:hover': {
    background: 'linear-gradient(90deg,#e53935 0%,#fb8c00 50%,#43a047 100%)',
  },
};

export const RAINBOW_OUTLINE_BTN = {
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
};

export const GLASS_PAPER = {
  p: { xs: 3, md: 4 },
  borderRadius: 3,
  bgcolor: 'rgba(255,255,255,0.82)',
  backdropFilter: 'blur(8px)',
  border: '1px solid rgba(0,0,0,0.06)',
};
