ALTER TABLE request ADD COLUMN cohort_name VARCHAR NULL;
ALTER TABLE request DROP cohort_id;
