-- V6: Seed Core CRM Clients for Trust Agro ERP

INSERT INTO crm_clients (
    customer_code, client_name, contact_person, phone, email,
    tax_identifier, customer_type, region, city, location, address,
    farm_type, farm_size, number_of_chickens, status, notes,
    created_at, updated_at
) VALUES
('CUST-000001', 'Bishoftu Commercial Layer Farm', 'Ato Dawit Bekele', '+251 91 123 4567', 'dawit.bishoftu@trustagro.internal', '0012345678', 'FARM', 'Oromia', 'Bishoftu', 'Bishoftu South', 'Expressway Exit 2', 'Layer', '2 Hectares', 8000, 'ACTIVE', 'Major regular buyer of Layer Feed and Vaccines', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CUST-000002', 'Hawassa Agro Poultry Producer', 'W/ro Selamawit Hailu', '+251 92 234 5678', 'selam.hawassa@trustagro.internal', '0023456789', 'FARM', 'Sidama', 'Hawassa', 'Tabor Subcity', 'Lake Hawassa Road', 'Broiler', '1.5 Hectares', 5000, 'ACTIVE', 'Key commercial partner for Pullet Feed and antibiotics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CUST-000003', 'Dukem Modern Broiler Farm', 'Ato Tariku Kebede', '+251 93 345 6789', 'tariku.dukem@trustagro.internal', '0034567890', 'FARM', 'Oromia', 'Dukem', 'Industrial Zone B', 'Warehouse Road Plot 14', 'Broiler', '3 Hectares', 12000, 'ACTIVE', 'High volume buyer of Finisher feed and veterinary supplies', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CUST-000004', 'Addis Central Poultry Cooperative', 'Ato Tesfaye Mengistu', '+251 94 456 7890', 'tesfaye.coop@trustagro.internal', '0045678901', 'ORGANIZATION', 'Addis Ababa', 'Akaki Kality', 'Kality Center', 'Office 301', 'Mixed Commercial', '5 Hectares', 15000, 'ACTIVE', 'Regional cooperative sourcing feeds and vaccines', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('CUST-000005', 'Adama Pullet & Layer Farm', 'Ato Yared Assefa', '+251 95 567 8901', 'yared.adama@trustagro.internal', '0056789012', 'FARM', 'Oromia', 'Adama', 'Wonji Road', 'Farm Complex 5', 'Layer', '1 Hectare', 4000, 'ACTIVE', 'Purchases Layer and Pullet feed bi-weekly', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
