-- ============================================================
-- V125: Accounts still using a documented default password must change it
--
-- V4 creates `super_admin` and V17 creates the sample company's `client_admin`, both with the password
-- `admin123` - which is written in those files, in the README, and so known to anyone who reads the project.
-- V4 never set must_change_password, so nothing ever forced that to change.
--
-- This turns on the application's EXISTING "must change password at next login" behaviour, but ONLY for an
-- account whose stored password is still EXACTLY the default one (the hash is compared, not the username
-- alone). An account whose password has already been changed has a different hash and is not touched. No
-- flow changes: it is the same forced-change screen users already get after an admin-issued password.
-- ============================================================
UPDATE users SET must_change_password = TRUE
WHERE username = 'super_admin' AND password = '$2b$12$Vg5B2vmiI7t1Mr31z1DrhufpruExhhDE3GZCvORNyh0IZmxfeWIvS';

UPDATE users SET must_change_password = TRUE
WHERE username = 'client_admin' AND password = '$2b$12$Vg5B2vmiI7t1Mr31z1DrhufpruExhhDE3GZCvORNyh0IZmxfeWIvS';
