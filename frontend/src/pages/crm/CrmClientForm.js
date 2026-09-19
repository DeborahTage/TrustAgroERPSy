import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { crmApi, userApi, chickenSaleApi } from '../../api';
import { Card, Form, Button, Row, Col, Alert } from 'react-bootstrap';
import { toast } from 'react-toastify';
import { useLanguage } from '../../context/LanguageContext';

const CrmClientForm = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const { t } = useLanguage();
  const isEdit = Boolean(id);
  const [form, setForm] = useState({ clientName: '', phone: '', location: '', farmType: '', farmSize: '', numberOfChickens: '', status: 'LEAD', assignedExtensionWorkerId: '' });
  const [workers, setWorkers] = useState([]);
  const [sales, setSales] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    userApi.getAll().then((r) => setWorkers(r.data.data?.filter((u) => u.role === 'EXTENSION_WORKER') || []));
    if (isEdit) {
      crmApi.getClient(id).then((r) => {
        const c = r.data.data;
        setForm({ clientName: c.clientName, phone: c.phone || '', location: c.location || '', farmType: c.farmType || '', farmSize: c.farmSize || '', numberOfChickens: c.numberOfChickens || '', status: c.status, assignedExtensionWorkerId: c.assignedExtensionWorkerId || '' });
      });
      chickenSaleApi.getAll({ customerId: id }).then((r) => setSales(r.data.data || [])).catch(() => setSales([]));
    }
  }, [id, isEdit]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = { ...form, numberOfChickens: form.numberOfChickens ? Number(form.numberOfChickens) : null, assignedExtensionWorkerId: form.assignedExtensionWorkerId ? Number(form.assignedExtensionWorkerId) : null };
      if (isEdit) {
        await crmApi.updateClient(id, payload);
        toast.success(t('crmClientForm.successUpdate'));
      } else {
        await crmApi.createClient(payload);
        toast.success(t('crmClientForm.successCreate'));
      }
      navigate('/crm');
    } catch (err) {
      setError(err.response?.data?.message || t('crmClientForm.error'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div>
      <h5 className="fw-bold mb-3">{isEdit ? t('crmClientForm.editTitle') : t('crmClientForm.addTitle')}</h5>
      <Card className="border-0 shadow-sm" style={{ maxWidth: 600 }}>
        <Card.Body className="p-4">
          {error && <Alert variant="danger" className="py-2 small">{error}</Alert>}
          <Form onSubmit={handleSubmit}>
            <Row className="g-3">
              <Col xs={12}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.clientName')}</Form.Label><Form.Control value={form.clientName} onChange={(e) => setForm({ ...form, clientName: e.target.value })} required /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.phone')}</Form.Label><Form.Control value={form.phone} onChange={(e) => setForm({ ...form, phone: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.location')}</Form.Label><Form.Control value={form.location} onChange={(e) => setForm({ ...form, location: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.farmType')}</Form.Label><Form.Control value={form.farmType} onChange={(e) => setForm({ ...form, farmType: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.farmSize')}</Form.Label><Form.Control value={form.farmSize} onChange={(e) => setForm({ ...form, farmSize: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.numberOfChickens')}</Form.Label><Form.Control type="number" min="0" value={form.numberOfChickens} onChange={(e) => setForm({ ...form, numberOfChickens: e.target.value })} /></Form.Group></Col>
              <Col xs={6}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.status')}</Form.Label><Form.Select value={form.status} onChange={(e) => setForm({ ...form, status: e.target.value })}>{['LEAD', 'ACTIVE', 'INACTIVE', 'LOST'].map((s) => <option key={s} value={s}>{t(`status.${s}`)}</option>)}</Form.Select></Form.Group></Col>
              <Col xs={12}><Form.Group><Form.Label className="small fw-semibold">{t('crmClientForm.extensionWorker')}</Form.Label><Form.Select value={form.assignedExtensionWorkerId} onChange={(e) => setForm({ ...form, assignedExtensionWorkerId: e.target.value })}><option value="">{t('forms.none')}</option>{workers.map((w) => <option key={w.id} value={w.id}>{w.fullName}</option>)}</Form.Select></Form.Group></Col>
            </Row>
            <div className="d-flex gap-2 mt-4">
              <Button type="submit" variant="success" disabled={loading}>{loading ? t('forms.saving') : t('forms.save')}</Button>
              <Button variant="outline-secondary" onClick={() => navigate('/crm')}>{t('forms.cancel')}</Button>
            </div>
          </Form>
        </Card.Body>
      </Card>
      {isEdit && (
        <Card className="border-0 shadow-sm mt-4" style={{ maxWidth: 800 }}>
          <Card.Body className="p-4">
            <h6 className="fw-bold mb-3">Chicken purchases</h6>
            {sales.length === 0 && <p className="text-muted small mb-0">No chicken sales for this customer yet.</p>}
            {sales.map((s) => (
              <div key={s.id} className="d-flex justify-content-between border-bottom py-2 small">
                <span>{s.saleNumber} · {s.status} · {s.flockBatchCode}</span>
                <span>{s.totalAmount} ETB outstanding {s.outstandingAmount}</span>
              </div>
            ))}
          </Card.Body>
        </Card>
      )}
    </div>
  );
};

export default CrmClientForm;
