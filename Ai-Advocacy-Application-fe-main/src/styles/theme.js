import { createTheme } from '@mui/material/styles';

const theme = createTheme({
    components: {
        MuiTextField: {
            styleOverrides: {
                root: {
                    '& .MuiInputBase-root': {
                        backgroundColor: '#F7F6FF',
                        borderRadius: 5,
                        height: '30px',
                        marginTop: '10px',
                        fontSize: '10px',
                        fontWeight: '400',
                    },
                },
            },
        },
    },
});

export default theme;
