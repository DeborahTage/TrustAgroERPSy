import api from './axios';

test('API client uses the current origin so production requests go through the same-origin proxy', () => {
  expect(api.defaults.baseURL).toBe('');
});
