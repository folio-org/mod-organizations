## 2.0.0 - Unreleased

## 1.9.0 - Released (Quesnelia R1 2024)

The primary focus of this release was to update rmb, vertx and improving fields of tables

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.8.0...v1.9.0)

### Stories
* [MODORG-61](https://folio-org.atlassian.net/browse/MODORG-61) - mod-organizations: Upgrade RAML Module Builder
* [MODORG-60](https://folio-org.atlassian.net/browse/MODORG-60) - Support searching by bankAccountNumber
* [MODORG-59](https://folio-org.atlassian.net/browse/MODORG-59) - BE - Introduce 'isDonor' indicator for organizations
* [MODORG-56](https://folio-org.atlassian.net/browse/MODORG-56) - Update RMB and vertx to the latest version
* [MODORG-54](https://folio-org.atlassian.net/browse/MODORG-54) - BE - Banking information fields edit and delete

### Dependencies
* Bump `raml` from `35.1.1` to `35.2.0`
* Bump `vertx` from `4.3.4` to `4.5.4`

## 1.8.0 - Released (Poppy R2 2023)

The primary focus of this release was to update ram-util and java version to 17

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.7.0...v1.8.0)

### Stories
* [FOLIO-3678](https://issues.folio.org/browse/FOLIO-3678) - Use GitHub Workflows api-lint and api-schema-lint and api-doc
* [MODORG-51](https://issues.folio.org/browse/MODORG-51) -  Update mod-organization md required interface section
* [MODORG-49](https://issues.folio.org/browse/MODORG-49) - Updated to java-17
* [MODORG-46](https://issues.folio.org/browse/MODORG-46) - Update dependent raml-util

### Dependencies
* Bump `java version` from `11` to `17`

## 1.7.0 - Released (Orchid R1 2023)
The primary focus of this release was to replace old HTTP Clients with Vertx WebClient and improvement Logging

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.6.0...v1.7.0)

### Stories
* [MODORG-45](https://issues.folio.org/browse/MODORG-45) - Logging improvement - Configuration
* [MODORG-42](https://issues.folio.org/browse/MODORG-42) - Replace old HTTP Clients with Vertx WebClient
* [MODORG-37](https://issues.folio.org/browse/MODORG-37) - Logging improvement

## 1.6.0 - Released
The primary focus of this release was to update RMB up to 35.0.1

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.5.0...v1.6.0)

### Stories
* [MODORG-43](https://issues.folio.org/browse/MODORG-43) Upgrade RAML Module Builder v35.0.1


## 1.5.0 - Released
The primary focus of this release was to Upgrade RMB up to 34.1.0

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.4.0...v1.5.0)

### Stories
* [MODORG-41](https://issues.folio.org/browse/MODORG-41) Upgrade RAML Module Builder v34.1.0

## 1.4.0 - Released
The primary focus of this release is to apply account number uniqueness

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.3.0...v1.4.0)

### Stories
* [MODORG-19](https://issues.folio.org/browse/MODORG-19) "Account number" must be unique for organization


## 1.3.0 - Released
The primary focus of this release was to Upgrade RMB up to 33.0.0

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.2.0...v1.3.0)

### Stories
* [MODORG-26](https://issues.folio.org/browse/MODORG-26) mod-organizations: Update RMB v33.0.0
* [MODORG-22](https://issues.folio.org/browse/MODORG-22) Add personal data disclosure form	Andrei Makaranka

## 1.2.0 - Released
The primary focus of this release was to Upgrade RMB up to 32.1.0

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.1.1...v1.2.0)

### Stories
* [MODORG-21](https://issues.folio.org/browse/MODORG-21) mod-organizations: Update RMB

## 1.1.1 - Released
The focus of this release was to fix logging issues

[Full Changelog](https://github.com/folio-org/mod-organizations/compare/v1.1.0...v1.1.1)

### Bug Fixes
* [MODORG-17](https://issues.folio.org/browse/MODORG-17) No logging in honeysuckle version


## 1.1.0 - Released
The primary focus of this release was to Upgrade RMB and JDK 11 versions

### Stories
* [MODORG-15](https://issues.folio.org/browse/MODORG-15) mod-organizations: Update RMB
* [MODORG-12](https://issues.folio.org/browse/MODORG-12) Migrate mod-organizations to JDK 11
 
## 1.0.0 - Released
The primary focus of this release was to add initial acquisition units support for organizations API

### Stories
* [MODORG-9](https://issues.folio.org/browse/MODORG-9) Update to RMB v30
* [MODORG-5](https://issues.folio.org/browse/MODORG-5) Create business layer proxy API for organization operations
* [MODORG-4](https://issues.folio.org/browse/MODORG-4) Restrict updates of Organization records based upon acquisitions unit
* [MODORG-3](https://issues.folio.org/browse/MODORG-3) Restrict search/view of Organization records based upon acquisitions unit
* [MODORG-1](https://issues.folio.org/browse/MODORG-1) Project setup: mod-organizations

### Bug Fixes
* [MODORG-11](https://issues.folio.org/browse/MODORG-11) Check restrictions for non-acq-unit members
