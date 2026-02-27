-- Инициализация тестовых данных
-- Пароли хранятся в открытом виде (только для демонстрационных целей!)

-- Тестовые пользователи (MERGE для избежания дубликатов)
MERGE INTO app_user (id, username, password, role, full_name) KEY(id) VALUES 
(1, 'admin', 'admin123', 'ADMINISTRATOR', 'Иван Петров'),
(2, 'logistyk', 'logistyk123', 'LOGISTICIAN', 'Анна Сидорова');

-- Примеры грузовиков (MERGE для избежания дубликатов)
MERGE INTO truck (id, brand, registration_number, registration_country, status) KEY(id) VALUES 
(1, 'Volvo FH16', 'WW12345', 'Польша', 'Доступна'),
(2, 'Scania R450', 'KR98765', 'Польша', 'Доступна'),
(3, 'Mercedes Actros', 'GD54321', 'Чехия', 'на ремонте');

-- Примеры прицепов (MERGE для избежания дубликатов)
MERGE INTO trailer (id, registration_number, brand, registration_country, status, current_location) KEY(id) VALUES 
(1, 'PO1234T', 'Schmitz Cargobull', 'Польша', 'Доступен', 'Варшава'),
(2, 'BY5678P', 'Krone', 'Беларусь', 'Доступен', 'Минск'),
(3, 'CZ9012R', 'Wielton', 'Чехия', 'на ремонте', 'Прага');

-- Примеры заказчиков (MERGE для избежания дубликатов)
MERGE INTO customer (id, name) KEY(id) VALUES 
(1, 'ООО ТрансЛогистик'),
(2, 'ИП Иванов'),
(3, 'АО ГрузоПеревозки');

-- Примеры документов (MERGE для избежания дубликатов)
MERGE INTO document (id, truck_id, document_type, expiry_date, description) KEY(id) VALUES 
(1, 1, 'INSURANCE', '2024-12-31', 'Страхование ОСАГО/КАСКО'),
(2, 1, 'TECHNICAL_INSPECTION', '2024-06-30', 'Технический осмотр'),
(3, 2, 'INSURANCE', '2024-11-15', 'Страхование ОСАГО/КАСКО'),
(4, 3, 'TECHNICAL_INSPECTION', '2024-05-20', 'Технический осмотр');
