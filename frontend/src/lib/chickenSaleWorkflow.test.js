describe('chicken sale API client', () => {
  test('exposes canonical /api/chicken-sales endpoints', () => {
    const { chickenSaleApi } = require('../api');
    expect(typeof chickenSaleApi.getAll).toBe('function');
    expect(typeof chickenSaleApi.getById).toBe('function');
    expect(typeof chickenSaleApi.create).toBe('function');
    expect(typeof chickenSaleApi.update).toBe('function');
    expect(typeof chickenSaleApi.confirm).toBe('function');
    expect(typeof chickenSaleApi.invoice).toBe('function');
    expect(typeof chickenSaleApi.pay).toBe('function');
    expect(typeof chickenSaleApi.complete).toBe('function');
    expect(typeof chickenSaleApi.cancel).toBe('function');
  });

  test('egg production API methods are gone', () => {
    const { flockApi } = require('../api');
    expect(flockApi.getEggProduction).toBeUndefined();
    expect(flockApi.addEggProduction).toBeUndefined();
  });
});
