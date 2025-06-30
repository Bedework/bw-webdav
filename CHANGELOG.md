# Release Notes

This project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased (6.1.0-SNAPSHOT)

## [6.0.0] - 2025-06-30
* First jakarta release

## [5.0.6] - 2025-02-06
* Upgrade library versions
* Move response classes and ToString into bw-base module.
* Pre-jakarta

## [5.0.5] - 2024-10-22
* Upgrade library versions

## [5.0.4] - 2024-09-18
* Upgrade library versions

## [5.0.3] - 2024-09-18
* Upgrade library versions

## [5.0.2] - 2024-09-17
* Upgrade library versions
* Make webdavexception subclass of runtimeexception and tidy up a bit. Should be no noticable changes.

## [5.0.1] - 2023-12-09
* Upgrade library versions
* Add a limit to how long the sync token is valid. Will allow flushing of old tombstoned data
* Encode etag

## [5.0.0] - 2022-02-12
* Use bedework-parent for builds
* Upgrade library versions

## [4.0.13] - 2021-09-11
* Update library versions

## [4.0.12] - 2021-09-05
* Update library versions

## [4.0.11] - 2021-06-07
* Update library versions

## [4.0.10] - 2021-06-02
* Update library versions
* Minor fix to check for null. Resulted in many changes to remove throws clauses from the xml emit utility class methods. This of course resulted in many changes up the call hierarchy.

## [4.0.9] - 2020-03-20
* Update library versions
* Lowercase account unless mixed case environment variable BEDEWORK_MIXEDCASE_ACCOUNTS is set to true
* Fixes to report/propfind - allprops and propname were not being handled correctly.
* Sync spec says "infinite" for depth
* Expect collection when URI ends with "/"
* Provide webdav access to WdSysIntf object. Allows prefixing of uri in error response.
* For propfind on missing resource return a 404 instead of a 207 with 404 inside.
* Return 0 length content with 200 status when no content in resource

## [4.0.8] - 2019-08-26
* Update library versions

## [4.0.7] - 2019-04-15
* Update library versions

## [4.0.6] - 2019-01-07
* Update library versions

## [4.0.5] - 2018-12-13
* Update library versions
* Bulk of these updates is to localize all references to external logging classes to one new module.

## [4.0.4] - 2018-11-28
* Failed previous release.

## [4.0.3] - 2018-11-28
* Update library versions
* [viqeeen] Changes to counter xxe vulnerability

## [4.0.2] - 2018-04-08
* Apparently still tracking changes in subversion to 3.x versions

## [4.0.1] - 2015-10-29
* Fix response to sync-collection - courtesy of Greg Allen.
* Fix handling of response - also noticed by Greg Allen.
* Fix depth handling for webdav sync

## [4.0.0] - 2014-03-20

## Pre 4.0.0
Repository was moved from subversion 2006-08-09 as version 3.0.0. Many changes followed before release 4.0.0 and are available in the git log.