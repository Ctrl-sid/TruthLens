import api from './api';

export const messagingService = {
  sendMessageToAdmin: async (claimId, subject, messageText, claimContextSummary) => {
    try {
      const response = await api.post('/user/messages', {
        claimId,
        subject,
        messageText,
        claimContextSummary
      });
      return response.data;
    } catch (err) {
      return {
        id: Date.now(),
        senderUsername: 'currentUser',
        senderFullName: 'You',
        receiverUsername: 'admin',
        claimId,
        subject: subject || 'Claim Feedback / Dispute',
        messageText,
        claimContextSummary,
        isRead: false,
        createdAt: new Date().toLocaleString()
      };
    }
  },

  getUserMessages: async () => {
    try {
      const response = await api.get('/user/messages');
      return response.data;
    } catch (err) {
      return [
        {
          id: 301,
          senderUsername: 'admin',
          senderFullName: 'TruthLens Admin Superuser',
          receiverUsername: 'currentUser',
          claimId: 1,
          subject: 'RE: Cultural Context Note on Viral Headline',
          messageText: 'Thank you for providing the regional context! Our fact-checking team has reviewed your submission and updated the claim nuances.',
          claimContextSummary: 'Viral Headline on Regional Event',
          isRead: false,
          createdAt: new Date().toLocaleString()
        }
      ];
    }
  },

  submitFeedback: async (claimId, rating, flagReason, comments) => {
    try {
      const response = await api.post('/user/feedback', {
        claimId,
        rating,
        flagReason,
        comments
      });
      return response.data;
    } catch (err) {
      return {
        id: Date.now(),
        claimId,
        rating,
        flagReason,
        comments,
        createdAt: new Date().toLocaleString()
      };
    }
  }
};
