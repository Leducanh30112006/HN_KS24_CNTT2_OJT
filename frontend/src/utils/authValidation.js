/**
 * QuickShop Authentication Validation Utilities
 * Cung cấp quy tắc kiểm thực chuẩn cho Đăng ký & Đăng nhập người dùng
 */

// Regex kiểm tra định dạng email chuẩn RFC 5322 cơ bản
export const EMAIL_REGEX = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;

// Regex kiểm tra định dạng số điện thoại Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09 hoặc +84)
export const VN_PHONE_REGEX = /^(0|\+84)[3|5|7|8|9][0-9]{8}$/;

// Regex kiểm tra username (3 - 30 ký tự, chữ cái, số, _, -, ., không có khoảng trắng)
export const USERNAME_REGEX = /^[a-zA-Z0-9_.-]{3,30}$/;

/**
 * Kiểm thực dữ liệu form Đăng ký người dùng
 * @param {Object} data 
 * @returns {{ isValid: boolean, errors: Object, firstError: string }}
 */
export function validateRegisterInput(data = {}) {
  const {
    fullName = '',
    username = '',
    email = '',
    phoneNumber = '',
    password = '',
    confirmPassword = '',
    agreeTerms = true
  } = data;

  const errors = {};

  // 1. Kiểm tra Họ và tên
  const trimmedName = fullName.trim();
  if (!trimmedName) {
    errors.fullName = 'Vui lòng nhập họ và tên của bạn.';
  } else if (trimmedName.length < 2) {
    errors.fullName = 'Họ và tên phải có tối thiểu 2 ký tự.';
  } else if (trimmedName.length > 50) {
    errors.fullName = 'Họ và tên không được vượt quá 50 ký tự.';
  }

  // 2. Kiểm tra Tên đăng nhập (Username)
  const trimmedUser = username.trim();
  if (!trimmedUser) {
    errors.username = 'Vui lòng nhập tên đăng nhập.';
  } else if (trimmedUser.length < 3) {
    errors.username = 'Tên đăng nhập phải có ít nhất 3 ký tự.';
  } else if (trimmedUser.length > 30) {
    errors.username = 'Tên đăng nhập không được dài quá 30 ký tự.';
  } else if (/\s/.test(trimmedUser)) {
    errors.username = 'Tên đăng nhập không được chứa khoảng cách / dấu cách.';
  } else if (!USERNAME_REGEX.test(trimmedUser)) {
    errors.username = 'Tên đăng nhập chỉ gồm chữ cái không dấu, số, dấu chấm (.), gạch nối (-) hoặc gạch dưới (_).';
  }

  // 3. Kiểm tra Email
  const trimmedEmail = email.trim();
  if (!trimmedEmail) {
    errors.email = 'Vui lòng nhập địa chỉ email.';
  } else if (!EMAIL_REGEX.test(trimmedEmail)) {
    errors.email = 'Địa chỉ email không đúng định dạng (Ví dụ: user@example.com).';
  }

  // 4. Kiểm tra Số điện thoại (Tùy chọn, nếu nhập phải chuẩn định dạng 10 số VN)
  const trimmedPhone = phoneNumber.trim();
  if (trimmedPhone) {
    if (!VN_PHONE_REGEX.test(trimmedPhone)) {
      errors.phoneNumber = 'Số điện thoại không đúng định dạng Việt Nam (10 số, bắt đầu bằng 03, 05, 07, 08, 09 hoặc +84).';
    }
  }

  // 5. Kiểm tra Mật khẩu
  if (!password) {
    errors.password = 'Vui lòng nhập mật khẩu đăng nhập.';
  } else if (password.length < 6) {
    errors.password = 'Mật khẩu phải có độ dài tối thiểu từ 6 ký tự.';
  } else if (password.length > 50) {
    errors.password = 'Mật khẩu không được dài quá 50 ký tự.';
  }

  // 6. Kiểm tra Xác nhận mật khẩu
  if (confirmPassword !== undefined) {
    if (!confirmPassword) {
      errors.confirmPassword = 'Vui lòng nhập lại mật khẩu để xác nhận.';
    } else if (confirmPassword !== password) {
      errors.confirmPassword = 'Mật khẩu xác nhận không khớp với mật khẩu đã nhập.';
    }
  }

  // 7. Kiểm tra đồng ý Điều khoản
  if (agreeTerms === false) {
    errors.agreeTerms = 'Bạn cần đồng ý với Quy chế hoạt động & Điều khoản dịch vụ của QuickShop để đăng ký.';
  }

  const errorKeys = Object.keys(errors);
  const isValid = errorKeys.length === 0;
  const firstError = isValid ? '' : errors[errorKeys[0]];

  return { isValid, errors, firstError };
}

/**
 * Kiểm thực dữ liệu form Đăng nhập
 * @param {Object} data 
 * @returns {{ isValid: boolean, errors: Object, firstError: string }}
 */
export function validateLoginInput(data = {}) {
  const { usernameOrEmail = '', password = '' } = data;
  const errors = {};

  const trimmed = usernameOrEmail.trim();
  if (!trimmed) {
    errors.usernameOrEmail = 'Vui lòng nhập tên đăng nhập hoặc địa chỉ email.';
  }

  if (!password) {
    errors.password = 'Vui lòng nhập mật khẩu đăng nhập.';
  }

  const errorKeys = Object.keys(errors);
  const isValid = errorKeys.length === 0;
  const firstError = isValid ? '' : errors[errorKeys[0]];

  return { isValid, errors, firstError };
}
