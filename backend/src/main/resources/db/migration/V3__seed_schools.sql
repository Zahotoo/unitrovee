-- V3__seed_schools.sql
-- seed the 13 supported Irish institutions.
-- Schools are reference data, stored in the table rather than hard-coded

INSERT INTO schools (name, short_name, email_domain, city, active, created_at, updated_at)
VALUES
    ('University College Dublin',                       'UCD',  'ucdconnect.ie',         'Dublin',    TRUE, NOW(), NOW()),
    ('Trinity College Dublin',                          'TCD',  'tcd.ie',                'Dublin',    TRUE, NOW(), NOW()),
    ('Dublin City University',                          'DCU',  'mail.dcu.ie',           'Dublin',    TRUE, NOW(), NOW()),
    ('Maynooth University',                             'MU',   'mumail.ie',             'Maynooth',  TRUE, NOW(), NOW()),
    ('University of Galway',                            'UOG',  'universityofgalway.ie', 'Galway',    TRUE, NOW(), NOW()),
    ('University College Cork',                         'UCC',  'umail.ucc.ie',          'Cork',      TRUE, NOW(), NOW()),
    ('University of Limerick',                          'UL',   'studentmail.ul.ie',     'Limerick',  TRUE, NOW(), NOW()),
    ('Technological University Dublin',                 'TUD',  'mytudublin.ie',         'Dublin',    TRUE, NOW(), NOW()),
    ('South East Technological University',             'SETU', 'setu.ie',               'Waterford', TRUE, NOW(), NOW()),
    ('Munster Technological University',                'MTU',  'mymtu.ie',                'Cork',      TRUE, NOW(), NOW()),
    ('Atlantic Technological University',               'ATU',  'atu.ie',                'Galway',    TRUE, NOW(), NOW()),
    ('RCSI University of Medicine and Health Sciences', 'RCSI', 'rcsi.com',              'Dublin',    TRUE, NOW(), NOW()),
    ('Technological University of the Shannon',         'TUS',  'student.tus.ie',        'Limerick',  TRUE, NOW(), NOW());