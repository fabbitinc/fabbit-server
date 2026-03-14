CREATE TABLE part_revision_workflow_policies (
    id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    policy_key VARCHAR(50) NOT NULL,
    mode VARCHAR(50) NOT NULL,
    CONSTRAINT part_revision_workflow_policies_pkey PRIMARY KEY (id),
    CONSTRAINT uq_part_revision_workflow_policies_policy_key UNIQUE (policy_key),
    CONSTRAINT ck_part_revision_workflow_policies_policy_key_default CHECK (policy_key = 'DEFAULT'),
    CONSTRAINT ck_part_revision_workflow_policies_mode CHECK (mode IN ('DIRECT', 'CHANGE_REQUEST_REQUIRED'))
);

INSERT INTO part_revision_workflow_policies (
    id,
    created_at,
    updated_at,
    policy_key,
    mode
) VALUES (
    '01959554-5305-79e7-8a36-8bf3c5a65ca7',
    NOW(),
    NOW(),
    'DEFAULT',
    'DIRECT'
);
