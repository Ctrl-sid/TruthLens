import api from './api';

export const authService = {
  login: async (username, password) => {
    try {
      const response = await api.post('/auth/login', { username, password });
      if (response.data.token) {
        localStorage.setItem('truthlens_token', response.data.token);
        localStorage.setItem('truthlens_user', JSON.stringify(response.data));
      }
      return response.data;
    } catch (err) {
      // 1. If the server responded with an error (e.g. 403 Forbidden or banned message)
      if (err.response) {
        const errorMsg = err.response.data?.message || err.response.data?.error || '';
        const isBanned = err.response.status === 403 || 
                         errorMsg.toLowerCase().includes('banned') || 
                         errorMsg.toLowerCase().includes('suspended');
        if (isBanned) {
          throw new Error(errorMsg || 'Access Denied: Your account has been suspended/banned due to code of conduct violations.');
        }
        if (err.response.status === 400 || err.response.status === 401) {
          throw new Error(errorMsg || 'Invalid username or password.');
        }
      }

      console.warn('Backend API connection offline, using fallback authentication validator.');

      // Allow admin login in fallback mode
      if (username === 'admin' && password === 'Admin@123') {
        const adminData = {
          token: 'mock-admin-jwt-token-12345',
          tokenType: 'Bearer',
          username: 'admin',
          email: 'admin@truthlens.ai',
          fullName: 'TruthLens Admin Superuser',
          role: 'ROLE_ADMIN',
          status: 'ACTIVE'
        };
        localStorage.setItem('truthlens_token', adminData.token);
        localStorage.setItem('truthlens_user', JSON.stringify(adminData));
        return adminData;
      }

      // Check registered users and admin roster
      const adminUsers = JSON.parse(localStorage.getItem('truthlens_admin_users') || '[]');
      const registeredUsers = JSON.parse(localStorage.getItem('truthlens_registered_users') || '[]');
      const defaultUsers = [
        { id: 1, username: 'admin', email: 'admin@truthlens.ai', fullName: 'TruthLens Admin Superuser', role: 'ROLE_ADMIN', status: 'ACTIVE' },
        { id: 2, username: 'akshay', email: 'akshay@gmail.com', fullName: 'Akshay Prince', role: 'ROLE_USER', status: 'ACTIVE' },
        { id: 3, username: 'ashwin', email: 'ashwin@gmail.com', fullName: 'Ashwin Raj', role: 'ROLE_USER', status: 'BANNED' }
      ];

      // Combine all known users (adminUsers has highest precedence for status modifications)
      const allUsers = [...registeredUsers, ...adminUsers, ...defaultUsers];
      const match = allUsers.find(u => u.username?.toLowerCase() === username?.toLowerCase());

      if (match) {
        if (match.status === 'BANNED') {
          throw new Error('Access Denied: Your account has been suspended/banned due to code of conduct violations.');
        }

        // Validate password or accept demo fallback passwords
        const validPassword = match.password 
          ? (match.password === password) 
          : (password === 'password' || password === 'User@123' || password.length >= 4);

        if (validPassword) {
          const userData = {
            token: match.token || `mock-user-jwt-${Date.now()}`,
            tokenType: 'Bearer',
            username: match.username,
            email: match.email,
            fullName: match.fullName || match.username,
            role: match.role || 'ROLE_USER',
            status: match.status || 'ACTIVE'
          };
          localStorage.setItem('truthlens_token', userData.token);
          localStorage.setItem('truthlens_user', JSON.stringify(userData));
          return userData;
        } else {
          throw new Error('Invalid username or password.');
        }
      }

      throw new Error('Invalid username or password.');
    }
  },

  register: async (username, email, password, fullName) => {
    try {
      const response = await api.post('/auth/register', { username, email, password, fullName });
      if (response.data.token) {
        localStorage.setItem('truthlens_token', response.data.token);
        localStorage.setItem('truthlens_user', JSON.stringify(response.data));
      }
      return response.data;
    } catch (err) {
      console.warn('Backend API connection offline, registering user locally.');
      const newUser = {
        token: `mock-user-jwt-${Date.now()}`,
        tokenType: 'Bearer',
        username,
        email,
        password,
        fullName,
        role: 'ROLE_USER',
        status: 'ACTIVE'
      };

      const localUsers = JSON.parse(localStorage.getItem('truthlens_registered_users') || '[]');
      localUsers.push(newUser);
      localStorage.setItem('truthlens_registered_users', JSON.stringify(localUsers));

      localStorage.setItem('truthlens_token', newUser.token);
      localStorage.setItem('truthlens_user', JSON.stringify(newUser));
      return newUser;
    }
  },

  logout: () => {
    localStorage.removeItem('truthlens_token');
    localStorage.removeItem('truthlens_user');
  },

  getCurrentUser: () => {
    const userStr = localStorage.getItem('truthlens_user');
    return userStr ? JSON.parse(userStr) : null;
  }
};
