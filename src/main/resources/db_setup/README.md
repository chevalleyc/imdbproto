# Notes on Setting Up a Live Quadstore DB

## IMPORTANT
Make sure you run this to create a new DB with distribution
DO NOT USE WITH AN EXISTING DB

## Sequence

1. Create DB on the target host

```shell
psql -h <host> -U postgres -w < db-setup.sql
```

F.e. the following is a successfull local DB creation:

```shell
christian@christian-GL552VW:~/IdeaProjects/imdbproto/src/main/resources/db_setup$ psql -h localhost -U postgres -w < db-setup.sql 
CREATE DATABASE
GRANT
```

2. Perform the migration to create the tables
   NB. you should run the migration outside of the standard application startup. This is required
   since there are a number of post migration operations to be performed.

cd to the location where flyway is installed and run it with:

```shell
./flyway migrate
```
Flyway has to be configured to perform the migration on the *target host*. See README-FLYWAY.md for
more on configuring this processor.

See https://flywaydb.org/documentation/usage/commandline/