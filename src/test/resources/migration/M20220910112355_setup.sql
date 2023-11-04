CREATE TABLE something (
	id uuid NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	name varchar not null,
	value varchar null,
	enabled bool NOT NULL DEFAULT true,
	CONSTRAINT something_pk PRIMARY KEY (id)
);

CREATE TABLE something_else (
	id uuid NOT NULL,
	created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
	name varchar not null,
	something_id uuid not NULL,
	CONSTRAINT something_else_pk PRIMARY KEY (id),
	CONSTRAINT something_else_something_id_fk FOREIGN KEY (something_id) REFERENCES something(id) ON DELETE CASCADE
);
