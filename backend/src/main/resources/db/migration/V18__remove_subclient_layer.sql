-- ============================================================
-- V18: Remove the Sub-Client layer entirely
--
-- Hierarchy simplified from ClientCompany -> SubClient -> Site
-- to ClientCompany -> Site directly, per business decision. This
-- is an additive forward migration (never edits V11/V17 in place)
-- so it's safe to apply even if those already ran in a real
-- environment.
-- ============================================================

-- Drop the FK + its supporting index, then the column, before dropping the table itself.
ALTER TABLE sites DROP FOREIGN KEY fk_sites_sub_client;
ALTER TABLE sites DROP INDEX idx_sites_sub_client;
ALTER TABLE sites DROP COLUMN sub_client_id;

DROP TABLE sub_clients;

-- Remove the now-meaningless SUBCLIENT_* permissions and their grants.
DELETE rp FROM role_permissions rp
JOIN permissions p ON p.id = rp.permission_id
WHERE p.name LIKE 'SUBCLIENT_%';

DELETE FROM permissions WHERE name LIKE 'SUBCLIENT_%';

-- Cosmetic: the 5 sites seeded in V17 were named as generic buildings under a
-- sub-client (e.g. "Main Building" under sub-client "XYZ IT Park"). Now that a
-- Site belongs directly to the Client Company, rename them to read naturally
-- as the client's own sites.
UPDATE sites SET site_name = 'XYZ IT Park'  WHERE site_code = 'SITE0001';
UPDATE sites SET site_name = 'ABC Mall'     WHERE site_code = 'SITE0002';
UPDATE sites SET site_name = 'DEF Hospital' WHERE site_code = 'SITE0003';
UPDATE sites SET site_name = 'PQR Tower'    WHERE site_code = 'SITE0004';
UPDATE sites SET site_name = 'LMN School'   WHERE site_code = 'SITE0005';
