const RECAPTCHA_SCRIPT_ID = 'google-recaptcha-v3-script';
const RECAPTCHA_SITE_KEY = process.env.REACT_APP_RECAPTCHA_SITE_KEY;

let scriptLoadPromise;

const loadRecaptchaScript = () => {
    if (!RECAPTCHA_SITE_KEY) {
        return Promise.reject(new Error('reCAPTCHA site key is not configured.'));
    }

    if (window.grecaptcha) {
        return Promise.resolve(window.grecaptcha);
    }

    if (!scriptLoadPromise) {
        scriptLoadPromise = new Promise((resolve, reject) => {
            const existingScript = document.getElementById(RECAPTCHA_SCRIPT_ID);
            if (existingScript) {
                existingScript.addEventListener('load', () => resolve(window.grecaptcha), { once: true });
                existingScript.addEventListener('error', () => reject(new Error('Failed to load reCAPTCHA script.')), { once: true });
                return;
            }

            const script = document.createElement('script');
            script.id = RECAPTCHA_SCRIPT_ID;
            script.src = `https://www.google.com/recaptcha/api.js?render=${RECAPTCHA_SITE_KEY}`;
            script.async = true;
            script.defer = true;
            script.onload = () => resolve(window.grecaptcha);
            script.onerror = () => reject(new Error('Failed to load reCAPTCHA script.'));
            document.head.appendChild(script);
        });
    }

    return scriptLoadPromise;
};

const executeRecaptcha = async (action) => {
    const grecaptcha = await loadRecaptchaScript();

    return new Promise((resolve, reject) => {
        grecaptcha.ready(() => {
            grecaptcha.execute(RECAPTCHA_SITE_KEY, { action })
                .then(resolve)
                .catch(() => reject(new Error('Failed to generate reCAPTCHA token.')));
        });
    });
};

const recaptchaService = {
    executeRecaptcha,
};

export default recaptchaService;
