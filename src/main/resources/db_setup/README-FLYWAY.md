# Configuring and Running Flyway Locally
This section gives some details on how to configure flyway to use on the command line (CLI).

The objective is to be able to perform the DB migration without needing to run the migration from the application while
keeping the schema history synchronized.

## Flyway Installation

Download the installer from https://flywaydb.org/documentation/usage/commandline/ and follow
the installation instruction depending on your platform O/S.

## Flyway Configuration
It is assumed that IMPersistence GitHub repository is cloned or at least, the migration directory
is copied locally.

In particular, the migration is relatively located at

`database/src/main/resources/db/migration`

This path will be used for the flyway configuration.

In flyway installation directory, edit `conf/flyway.conf` with the following parameters

```properties
# JDBC url to use to connect to the database
# Examples
# --------
# Most drivers are included out of the box.
#...
flyway.url=jdbc:postgresql://localhost:5432/quadstore

# Fully qualified classname of the JDBC driver (autodetected by default based on flyway.url)
flyway.driver=org.postgresql.Driver

# User to use to connect to the database. Flyway will prompt you to enter it if not specified, and if the JDBC
# connection is not using a password-less method of authentication.
flyway.user=postgres

# Password to use to connect to the database. Flyway will prompt you to enter it if not specified, and if the JDBC
# connection is not using a password-less method of authentication.
flyway.password=postgres

# The default schema managed by Flyway. This schema name is case-sensitive. If not specified, but flyway.schemas
# is, Flyway uses the first schema in that list. If that is also not specified, Flyway uses the default schema for the
# database connection.
# Consequences:
# - This schema will be the one containing the schema history table.
# - This schema will be the default for the database connection (provided the database supports this concept).
flyway.defaultSchema=quadstore

# Comma-separated list of the schemas managed by Flyway. These schema names are case-sensitive. If not specified, Flyway uses
# the default schema for the database connection. If flyway.defaultSchema is not specified, then the first of
# this list also acts as the default schema.
# Consequences:
# - Flyway will automatically attempt to create all these schemas, unless they already exist.
# - The schemas will be cleaned in the order of this list.
# - If Flyway created them, the schemas themselves will be dropped when cleaning.
flyway.schemas=quastore

#
# Locations starting with filesystem: point to a directory on the filesystem, may only
# contain SQL migrations and are only scanned recursively down non-hidden directories.
# Locations starting with s3: point to a bucket in AWS S3, may only contain SQL migrations, and are scanned
# recursively. They are in the format s3:<bucket>(/optionalfolder/subfolder)
# Locations starting with gcs: point to a bucket in Google Cloud Storage, may only contain SQL migrations, and are scanned
# recursively. They are in the format gcs:<bucket>(/optionalfolder/subfolder)
# Wildcards can be used to reduce duplication of location paths. (e.g. filesystem:migrations/*/oracle) Supported wildcards:
# ** : Matches any 0 or more directories
# *  : Matches any 0 or more non-separator characters
# ?  : Matches any 1 non-separator character
#
flyway.locations=filesystem:/IMPersistence/database/src/main/resources/db/migration
```
## Performing the Migration
At this stage, cd into the flyway installation directory (f.e. `cd ~/flyway-8.4.4`) and run it as follows:

```shell
./flyway migrate
```

F.e. a successful migration will result in: (Linux terminal)

```shell
christian@christian-GL552VW:/mnt/A0E245CAE245A4FE/devel/flyway-8.4.4$ ./flyway migrate
A new version of Flyway is available
Upgrade to Flyway 9.11.0: https://rd.gt/2X0gakb
Flyway Community Edition 8.4.4 by Redgate
Database: jdbc:postgresql://localhost:5432/quadstore (PostgreSQL 12.9)
Executing SQL callback: beforeValidate - 
Successfully validated 3 migrations (execution time 00:00.023s)
Executing SQL callback: beforeMigrate - 
WARNING: DB: schema "quadstore" already exists, skipping (SQL State: 42P06 - Error Code: 0)
Current version of schema "quadstore": null
Migrating schema "quadstore" to version "1 - empty"
Migrating schema "quadstore" to version "2 - quadstore"
WARNING: DB: extension "uuid-ossp" already exists, skipping (SQL State: 42710 - Error Code: 0)
Successfully applied 2 migrations to schema "quadstore", now at version v2 (execution time 00:00.165s)

```

NB. Depending on your environment, you may have to comment out the following line from `beforeMigrate.sql` since
roles are global in the runtime environment.

```sql
CREATE ROLE quadsmgr WITH LOGIN PASSWORD 'quadsmgr';
```