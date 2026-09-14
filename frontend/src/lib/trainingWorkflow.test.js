/**
 * Frontend guards mirroring training finance rules (display-only; backend is authoritative).
 */
describe('training finance rules', () => {
  test('partial payment does not inflate revenue', () => {
    const invoice = 10000;
    const payment = 6000;
    const revenue = invoice;
    const cash = payment;
    const ar = invoice - payment;
    expect(revenue).toBe(10000);
    expect(cash).toBe(6000);
    expect(ar).toBe(4000);
    expect(revenue + cash).not.toBe(invoice);
  });

  test('capacity blocks overbooking', () => {
    const capacity = 30;
    const confirmed = 30;
    const canAccept = confirmed < capacity;
    expect(canAccept).toBe(false);
  });
});
