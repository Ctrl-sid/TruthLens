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

      // Check registered local users
      const localUsers = JSON.parse(localStorage.getItem('truthlens_registered_users') || '[]');
      const match = localUsers.find(u => u.username === username && u.password === password);

      if (match) {
        if (match.status === 'BANNED') {
          throw new Error('Your account has been suspended/banned due to code of conduct violations.');
        }
        localStorage.setItem('truthlens_token', match.token);
        localStorage.setItem('truthlens_user', JSON.stringify(match));
        return match;
      }

      throw err.response?.data?.message || new Error('Invalid username or password.');
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
