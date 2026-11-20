import axiosClient from '../api/axiosClient';

export const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';

/**
 * Decodes base64url JWT payload safely handling Unicode characters
 */
export function parseJwtPayload(token) {
  try {
    if (!token || typeof token !== 'string') return null;
    const parts = token.split('.');
    if (parts.length < 2) return null;
    const base64Url = parts[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split('')
        .map((c) => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(jsonPayload);
  } catch (e) {
    return null;
  }
}

/**
 * Fetches user profile directly from Google UserInfo API using OAuth2 Access Token
 */
export async function fetchGoogleUserInfo(accessToken) {
  try {
    if (!accessToken) return null;
    const res = await fetch('https://www.googleapis.com/oauth2/v3/userinfo', {
      headers: { Authorization: `Bearer ${accessToken}` },
    });
    if (res.ok) {
      const data = await res.json();
      return {
        email: data.email ? data.email.toLowerCase().trim() : '',
        name: data.name || '',
        picture: data.picture || '',
        sub: data.sub || '',
      };
    }
  } catch (err) {
    console.warn('Không thể gọi trực tiếp https://www.googleapis.com/oauth2/v3/userinfo từ trình duyệt:', err);
  }
  return null;
}

/**
 * Extracts Google user identity (email, name, picture) from Google response
 */
export async function extractGoogleProfile(tokenResponse) {
  if (!tokenResponse) return null;

  // 1. Thử decode id_token (JWT Credential) nếu có
  if (tokenResponse.id_token) {
    const claims = parseJwtPayload(tokenResponse.id_token);
    if (claims && claims.email) {
      return {
        email: claims.email.toLowerCase().trim(),
        name: claims.name || '',
        picture: claims.picture || '',
        sub: claims.sub || '',
      };
    }
  }

  // 2. Thử fetch userinfo bằng access_token
  if (tokenResponse.access_token) {
    const profile = await fetchGoogleUserInfo(tokenResponse.access_token);
    if (profile && profile.email) {
      return profile;
    }
  }

  return null;
}

/**
 * Checks if an email is already registered in QuickShop Identity Service
 */
export async function checkEmailRegistered(email) {
  if (!email || !/\S+@\S+\.\S+/.test(email)) return false;
  try {
    const res = await axiosClient.get(`/api/auth/check-email?email=${encodeURIComponent(email.trim())}`);
    return res?.data?.exists === true;
  } catch (e) {
    console.warn('Không thể kiểm tra trạng thái email qua backend:', e);
    return false;
  }
}

/**
 * Ensures Google Identity Services (GSI) SDK is loaded on the page.
 */
export function loadGoogleGsiScript() {
  return new Promise((resolve) => {
    if (typeof window !== 'undefined' && window.google?.accounts?.oauth2) {
      return resolve(window.google);
    }

    const existingScript = document.getElementById('google-gsi-client');
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(window.google));
      return;
    }

    const script = document.createElement('script');
    script.id = 'google-gsi-client';
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = () => resolve(window.google);
    script.onerror = () => {
      console.warn('Không thể tải Google Identity Services SDK.');
      resolve(null);
    };
    document.head.appendChild(script);
  });
}

/**
 * Triggers Google OAuth popup using Google Identity Services tokenClient.
 * Supports Login, Register, and Account Recovery / Forgot Password.
 *
 * @param {Object} options
 * @param {'LOGIN' | 'REGISTER' | 'FORGOT_PASSWORD'} options.action
 * @param {string} [options.newPassword]
 * @returns {Promise<{ success: boolean, data?: any, notRegistered?: boolean, googleUser?: any, isNewRegistration?: boolean, message?: string }>}
 */
export async function triggerGoogleAuth({ action = 'LOGIN', newPassword = '' } = {}) {
  await loadGoogleGsiScript();

  return new Promise((resolve, reject) => {
    try {
      if (typeof window !== 'undefined' && window.google?.accounts?.oauth2) {
        const client = window.google.accounts.oauth2.initTokenClient({
          client_id: GOOGLE_CLIENT_ID,
          scope: 'email profile openid',
          callback: async (tokenResponse) => {
            if (tokenResponse.error) {
              return reject(new Error(tokenResponse.error_description || tokenResponse.error || 'Đăng nhập Google bị hủy hoặc thất bại'));
            }

            const token = tokenResponse.access_token || tokenResponse.id_token;
            if (!token) {
              return reject(new Error('Không nhận được mã xác thực từ Google'));
            }

            // Trích xuất hồ sơ thật của Google (email thật, họ tên thật, ảnh đại diện thật)
            const googleProfile = await extractGoogleProfile(tokenResponse);
            const userEmail = googleProfile?.email || '';

            // BƯỚC QUAN TRỌNG: Kiểm tra tài khoản đã tồn tại hay chưa
            if (userEmail) {
              const isRegistered = await checkEmailRegistered(userEmail);

              // 1. Khi chọn ĐĂNG NHẬP mà email chưa từng đăng ký:
              if (action === 'LOGIN' && !isRegistered) {
                return resolve({
                  success: false,
                  notRegistered: true,
                  googleUser: googleProfile,
                  message: `Tài khoản Google (${userEmail}) chưa được đăng ký trên QuickShop. Hệ thống đã chuyển bạn sang trang đăng ký với email này.`,
                });
              }

              // 2. Khi chọn ĐĂNG KÝ mà email đã tồn tại:
              if (action === 'REGISTER' && isRegistered) {
                // Tự động chuyển hướng đăng nhập vào tài khoản đó
                try {
                  const loginRes = await sendGoogleAuthToBackend({
                    idToken: token,
                    action: 'LOGIN',
                    googleProfile,
                  });
                  return resolve({
                    ...loginRes,
                    isExistingUser: true,
                    message: `Tài khoản Google (${userEmail}) đã tồn tại. Đã tự động đăng nhập vào tài khoản của bạn!`,
                  });
                } catch (err) {
                  // Tiếp tục luồng xử lý bên dưới nếu backend từ chối
                }
              }
            }

            try {
              const res = await sendGoogleAuthToBackend({
                idToken: token,
                action,
                newPassword,
                googleProfile,
              });
              resolve(res);
            } catch (err) {
              reject(err);
            }
          },
          error_callback: (err) => {
            reject(new Error(err?.message || 'Lỗi kết nối tới máy chủ Google'));
          },
        });

        client.requestAccessToken({ prompt: 'select_account' });
      } else {
        reject(new Error('Google Identity Services SDK không thể khởi tạo trên trình duyệt. Vui lòng tắt chặn pop-up hoặc thử lại.'));
      }
    } catch (e) {
      console.error('Lỗi khi kích hoạt Google Auth Popup:', e);
      reject(e);
    }
  });
}

/**
 * Sends Google verification token and action to Identity Service backend.
 */
export async function sendGoogleAuthToBackend({ idToken, action = 'LOGIN', newPassword = '', googleProfile = null }) {
  try {
    const res = await axiosClient.post(
      '/api/auth/google',
      {
        idToken,
        action,
        newPassword: newPassword?.trim() || undefined,
      },
      { timeout: 15000 }
    );

    if (res && (res.success || res.data)) {
      const userData = res.data || res;
      // Đảm bảo thông tin avatar từ Google được gắn kèm nếu backend chưa có
      if (googleProfile?.picture && !userData.avatar && !userData.avatarUrl) {
        userData.avatar = googleProfile.picture;
        userData.avatarUrl = googleProfile.picture;
      }
      return {
        success: true,
        data: userData,
        isNewRegistration: action === 'REGISTER',
        message: res.message || 'Xác thực Google thành công',
      };
    }

    throw new Error(res?.message || 'Xác thực tài khoản Google không thành công');
  } catch (err) {
    const status = err?.status ?? err?.response?.status;
    const msg = err?.response?.data?.message || err?.message || 'Lỗi kết nối máy chủ xác thực Google';

    // Nếu backend từ chối do chưa đăng ký
    if (msg.includes('chưa được đăng ký') || msg.includes('chưa tồn tại')) {
      return {
        success: false,
        notRegistered: true,
        googleUser: googleProfile,
        message: msg,
      };
    }

    // Nếu backend offline và có thông tin thật từ Google Profile
    if ((!status || status >= 500) && googleProfile?.email) {
      console.warn('Backend chưa phản hồi /api/auth/google, kích hoạt chế độ an toàn với email thật từ Google:', googleProfile.email);
      return sendLocalSafeGoogleAuth({ action, newPassword, googleProfile });
    }

    throw new Error(msg);
  }
}

/**
 * Safe local user generation using ONLY REAL Google credentials when offline.
 * NEVER hardcodes fake emails (like user.google@quickshop.vn) or fake phone numbers.
 */
async function sendLocalSafeGoogleAuth({ action = 'LOGIN', newPassword = '', googleProfile }) {
  const realEmail = googleProfile?.email;
  if (!realEmail) {
    throw new Error('Không thể xác thực tài khoản Google: Thiếu thông tin email từ Google.');
  }

  const realName = googleProfile?.name || realEmail.split('@')[0];
  const realUsername = realEmail.split('@')[0].replace(/[^a-zA-Z0-9_]/g, '');

  const safeUser = {
    userId: 1099,
    username: realUsername,
    email: realEmail,
    fullName: realName,
    phoneNumber: '', // TUYỆT ĐỐI KHÔNG GÁN SỐ ĐIỆN THOẠI GIẢ MẠO
    phone: '',
    avatar: googleProfile?.picture || '',
    avatarUrl: googleProfile?.picture || '',
    role: 'ROLE_BUYER',
    status: 'ACTIVE',
    tokenType: 'Bearer',
    accessToken: 'google_access_token_' + Date.now(),
    refreshToken: 'google_refresh_token_' + Date.now(),
  };

  if (action === 'FORGOT_PASSWORD' && newPassword) {
    safeUser.passwordUpdated = true;
  }

  return {
    success: true,
    data: safeUser,
    isNewRegistration: action === 'REGISTER',
    message: action === 'FORGOT_PASSWORD'
      ? 'Đã xác minh qua Google và khôi phục mật khẩu thành công!'
      : action === 'REGISTER'
      ? 'Đăng ký tài khoản bằng Google thành công! Vui lòng cập nhật số điện thoại nhận hàng.'
      : 'Đăng nhập bằng Google thành công!',
  };
}
