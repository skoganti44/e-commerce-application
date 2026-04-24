import { Stack, Typography, Chip, Box } from '@mui/material';

function CustomChip({ label, bg, fg }) {
  return (
    <Chip
      size="small"
      label={label}
      sx={{
        height: 20,
        fontSize: 11,
        fontWeight: 700,
        bgcolor: bg,
        color: fg,
        letterSpacing: 0.3,
      }}
    />
  );
}

export default function KitchenOrderItems({ items = [] }) {
  return (
    <Stack spacing={1}>
      {items.map((it) => {
        const hasCustom =
          it.sweetenerType || it.flourType || it.customization;
        return (
          <Box key={it.itemId}>
            <Typography variant="body2" sx={{ fontWeight: 700 }}>
              {it.quantity} × {it.productName}
            </Typography>
            {hasCustom && (
              <Stack
                direction="row"
                spacing={0.5}
                sx={{ flexWrap: 'wrap', mt: 0.25, rowGap: 0.5 }}
              >
                {it.sweetenerType && (
                  <CustomChip
                    label={`🍯 ${it.sweetenerType}${
                      it.sweetenerPercent ? ` ${it.sweetenerPercent}%` : ''
                    }`}
                    bg="#fff3e0"
                    fg="#bf360c"
                  />
                )}
                {it.flourType && (
                  <CustomChip
                    label={`🌾 ${it.flourType}`}
                    bg="#efebe9"
                    fg="#4e342e"
                  />
                )}
                {it.customization && (
                  <CustomChip
                    label={`📝 ${it.customization}`}
                    bg="#f3e5f5"
                    fg="#4a148c"
                  />
                )}
              </Stack>
            )}
          </Box>
        );
      })}
    </Stack>
  );
}
