-- Tenant T100 / department D10 (subscription SUB-1001, privacy opt-out enabled):
--   ITM-1001..1003  in the October window, returned by the API
--   ITM-1004        in the window but opted out -> excluded by the opt-out filter
--   ITM-1005        outside the window
-- ITM-2001 belongs to another tenant.
DELETE FROM ANALYTICS.ITEMS_VW;

INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-1001', 'Laptop Pro 14',  'Electronics', 1299.99, 10,  'ACTIVE',       TIMESTAMP '2026-10-05 10:15:30', 'T100', 'D10', FALSE);
INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-1002', 'Wireless Mouse', 'Accessories',   25.50, 150, 'ACTIVE',       TIMESTAMP '2026-10-12 08:00:00', 'T100', 'D10', FALSE);
INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-1003', 'USB-C Hub',      'Accessories',   45.00, 0,   'DISCONTINUED', TIMESTAMP '2026-10-20 16:45:10', 'T100', 'D10', FALSE);
INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-1004', 'Privacy Item',   'Electronics',   99.00, 5,   'ACTIVE',       TIMESTAMP '2026-10-21 09:00:00', 'T100', 'D10', TRUE);
INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-1005', 'Old Monitor',    'Electronics',  199.00, 3,   'ACTIVE',       TIMESTAMP '2026-09-15 12:00:00', 'T100', 'D10', FALSE);
INSERT INTO ANALYTICS.ITEMS_VW VALUES ('ITM-2001', 'Other Tenant',   'Electronics',   10.00, 1,   'ACTIVE',       TIMESTAMP '2026-10-10 00:00:00', 'T200', 'D20', FALSE);
