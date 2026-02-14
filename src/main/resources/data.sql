-- Inicjalizacja danych testowych
-- Hasła są przechowywane w czystym tekście (tylko do celów demonstracyjnych!)

-- Użytkownicy testowi (MERGE aby uniknąć duplikatów)
MERGE INTO app_user (id, username, password, role, full_name) KEY(id) VALUES 
(1, 'admin', 'admin123', 'ADMINISTRATOR', 'Jan Kowalski'),
(2, 'logistyk', 'logistyk123', 'LOGISTICIAN', 'Anna Nowak');

-- Przykładowe ciężarówki (MERGE aby uniknąć duplikatów)
MERGE INTO truck (id, brand, registration_number, status) KEY(id) VALUES 
(1, 'Volvo FH16', 'WW12345', 'ACTIVE'),
(2, 'Scania R450', 'KR98765', 'ACTIVE'),
(3, 'Mercedes Actros', 'GD54321', 'MAINTENANCE');

-- Przykładowe dokumenty (MERGE aby uniknąć duplikatów)
MERGE INTO document (id, truck_id, document_type, expiry_date, description) KEY(id) VALUES 
(1, 1, 'INSURANCE', '2024-12-31', 'Ubezpieczenie OC/AC'),
(2, 1, 'TECHNICAL_INSPECTION', '2024-06-30', 'Przegląd techniczny'),
(3, 2, 'INSURANCE', '2024-11-15', 'Ubezpieczenie OC/AC'),
(4, 3, 'TECHNICAL_INSPECTION', '2024-05-20', 'Przegląd techniczny');
