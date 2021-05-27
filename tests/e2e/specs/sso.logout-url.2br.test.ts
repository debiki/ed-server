import * as u from '../utils/utils';
import constructSsoLoginTest from './sso-login-member-impl.2browsers.test';


constructSsoLoginTest(`sso.logout-url.2br  TyTE2ESSOLGOURL2`, {
    loginRequired: false,
    // This server and page don't exist; the browser will show an error. Fine.
    ssoLogoutUrl: `http://localhost:8080/${u.ssoLogoutRedirPageSlug}`,
    approvalRequired: false  });
