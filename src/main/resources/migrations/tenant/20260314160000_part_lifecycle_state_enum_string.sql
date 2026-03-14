UPDATE parts
SET lifecycle_state = UPPER(REPLACE(REPLACE(TRIM(lifecycle_state), '-', '_'), ' ', '_'))
WHERE lifecycle_state IS NOT NULL;

UPDATE part_revisions
SET lifecycle_state = UPPER(REPLACE(REPLACE(TRIM(lifecycle_state), '-', '_'), ' ', '_'))
WHERE lifecycle_state IS NOT NULL;
