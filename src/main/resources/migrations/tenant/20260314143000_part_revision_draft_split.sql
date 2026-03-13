update part_revisions
set revision_code = null
where status in ('DRAFT', 'IN_REVIEW');

alter table part_revisions
    alter column revision_code drop not null;
