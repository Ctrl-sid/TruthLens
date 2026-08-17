import api from './api';

const DEFAULT_USERS = [
  { id: 1, username: 'admin', email: 'admin@truthlens.ai', fullName: 'TruthLens Admin Superuser', role: 'ROLE_ADMIN', status: 'ACTIVE', createdAt: '2026-08-15 17:27' },
  { id: 2, username: 'akshay', email: 'akshay@gmail.com', fullName: 'Akshay Prince', role: 'ROLE_USER', status: 'ACTIVE', createdAt: '2026-08-15 18:40' },
  { id: 3, username: 'ashwin', email: 'ashwin@gmail.com', fullName: 'Ashwin Raj', role: 'ROLE_USER', status: 'BANNED', createdAt: '2026-08-15 18:41' }
];

function getStoredUsers() {
  const localList = localStorage.getItem('truthlens_admin_users');
  if (localList) {
    try {
      const parsed = JSON.parse(localList);
      if (Array.isArray(parsed) && parsed.length > 0) {
        // Filter out legacy mock data if present
        const hasLegacy = parsed.some(u => u.username === 'john_doe' || u.username === 'sarah_m' || u.username === 'spammer_99');
        if (!hasLegacy) {
          return parsed;
        }
      }
    } catch (e) {
      // fallback
    }
  }
  localStorage.setItem('truthlens_admin_users', JSON.stringify(DEFAULT_USERS));
  return DEFAULT_USERS;
}

function saveStoredUsers(users) {
  localStorage.setItem('truthlens_admin_users', JSON.stringify(users));
}

export const adminService = {
  getUsers: async () => {
    try {
      const response = await api.get('/admin/users');
      if (response.data && Array.isArray(response.data)) {
        saveStoredUsers(response.data);
        return response.data;
      }
    } catch (err) {
      console.warn('Backend API offline or unauthorized, returning database-aligned fallback list');
    }
    return getStoredUsers();
  },

  updateUserStatus: async (userId, status, reason) => {
    const users = getStoredUsers();
    const user = users.find(u => u.id === userId);
    if (user) {
      user.status = status;
      saveStoredUsers(users);
    }

    try {
      const response = await api.put(`/admin/users/${userId}/status`, { status, reason });
      return response.data;
    } catch (err) {
      return user;
    }
  },

  deleteUser: async (userId) => {
    const users = getStoredUsers().filter(u => u.id !== userId);
    saveStoredUsers(users);

    try {
      await api.delete(`/admin/users/${userId}`);
    } catch (err) {
      console.log('Deleted mock user', userId);
    }
  },

  getAdminInbox: async () => {
    try {
      const response = await api.get('/admin/messages');
      return response.data;
    } catch (err) {
      return getFallbackAdminInbox();
    }
  },

  replyToUser: async (recipientUsername, claimId, subject, messageText, claimContextSummary) => {
    try {
      const response = await api.post('/admin/messages/reply', {
        recipientUsername,
        claimId,
        subject,
        messageText,
        claimContextSummary
      });
      return response.data;
    } catch (err) {
      return {
        id: Date.now(),
        senderUsername: 'admin',
        senderFullName: 'TruthLens Admin Superuser',
        receiverUsername: recipientUsername,
        claimId,
        subject: subject || 'RE: Admin Response',
        messageText,
        claimContextSummary,
        isRead: false,
        createdAt: new Date().toLocaleString()
      };
    }
  },

  getAllClaimFeedback: async () => {
    try {
      const response = await api.get('/admin/feedback');
      return response.data;
    } catch (err) {
      return getFallbackFeedback();
    }
  }
};

function getFallbackAdminInbox() {
  return [
    {
      id: 101,
      senderUsername: 'john_doe',
      senderFullName: 'John Doe',
      receiverUsername: 'admin',
      claimId: 1,
      subject: 'Cultural Context Note on Viral Headline',
      messageText: 'Hello Admin, I wanted to point out that the regional phrasing used in this claim has a specific local cultural context that makes it sound exaggerated, but the event itself actually happened in local news archives.',
      claimContextSummary: 'Viral Headline on Regional Event',
      isRead: false,
      createdAt: '2026-08-06 08:30'
    },
    {
      id: 102,
      senderUsername: 'sarah_m',
      senderFullName: 'Sarah Miller',
      receiverUsername: 'admin',
      claimId: 2,
      subject: 'Disputing Fact-Check Verdict Score',
      messageText: 'I am sharing peer-reviewed research links showing that the health study referenced is genuine and was published last month.',
      claimContextSummary: 'Health Study Discovery Claim',
      isRead: true,
      createdAt: '2026-08-05 16:45'
    }
  ];
}

function getFallbackFeedback() {
  return [
    { id: 201, username: 'john_doe', claimId: 1, rating: 4, flagReason: 'CULTURALLY_INAPPROPRIATE', comments: 'Regional phrasing misclassified by sentiment analyzer.', createdAt: '2026-08-06 08:30' },
    { id: 202, username: 'sarah_m', claimId: 2, rating: 2, flagReason: 'INACCURATE_FACT', comments: 'Missing recent peer-reviewed publication data.', createdAt: '2026-08-05 16:45' }
  ];
}
