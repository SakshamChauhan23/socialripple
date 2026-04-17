import { useTheme, useMediaQuery } from '@mui/material';

const useScreenSize = () => {
    const theme = useTheme();
    const isMobileScreen = useMediaQuery(theme.breakpoints.down('sm'));
    const isTabletScreen = useMediaQuery(theme.breakpoints.between('sm', 'md'));
    const isDesktopScreen = useMediaQuery(theme.breakpoints.up('md'));

    return { isMobileScreen, isTabletScreen, isDesktopScreen };
};

export default useScreenSize;
