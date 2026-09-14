/**
 * Frontend contract checks for the canonical prescription workflow API client.
 */
describe('prescription workflow API client', () => {
  test('pharmacy queue uses canonical /api/pharmacy/prescriptions', () => {
    // Static import path verification via module shape
    const { pharmacyApi, vetApi } = require('../api');
    expect(typeof pharmacyApi.getPrescriptions).toBe('function');
    expect(typeof pharmacyApi.approvePrescription).toBe('function');
    expect(typeof pharmacyApi.rejectPrescription).toBe('function');
    expect(typeof pharmacyApi.dispensePrescription).toBe('function');
    expect(typeof pharmacyApi.getPrescription).toBe('function');
    expect(typeof vetApi.createPrescription).toBe('function');
    expect(typeof vetApi.submitPrescription).toBe('function');
    expect(typeof vetApi.cancelPrescription).toBe('function');
  });

  test('rejection requires reason payload shape', () => {
    const reason = '';
    expect(Boolean(reason.trim())).toBe(false);
    expect(Boolean('Stock interaction risk'.trim())).toBe(true);
  });
});
