CREATE TABLE test_table
(
    i int,
    t text
);

INSERT INTO test_table
VALUES (1, 'one'), (2, 'two'), (3, 'three');

CREATE FUNCTION test_add_func(a int, b int) returns int
    language plpgsql
as
'
    BEGIN
        RETURN a + b;
    end;
';

CREATE FUNCTION test_long_process_with_notices(seconds int) returns int
language plpgsql
as '
BEGIN
    FOR counter IN 1..seconds loop
        PERFORM pg_sleep(1);
        RAISE NOTICE ''% seconds passed...'', counter;
    end loop;
    RAISE NOTICE ''Finished!'';
    RETURN seconds;
end;
'