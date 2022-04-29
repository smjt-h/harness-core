-- Copyright 2022 Harness Inc. All rights reserved.
-- Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
-- that can be found in the licenses directory at the root of this repository, also available at
-- https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

BEGIN;
 CREATE UNIQUE INDEX accountId_orgId_projecId_serviceId_service_startts ON service_infra_info
 (accountid, orgidentifier, projectidentifier, service_id, service_startts);
COMMIT;
