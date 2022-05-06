/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.scim;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.GROUP;
import static io.harness.ng.core.dto.UserGroupDTO.UserGroupDTOBuilder;
import static io.harness.ng.core.user.entities.UserGroup.UserGroupKeys;
import static io.harness.ng.core.utils.UserGroupMapper.toDTO;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserGroup;
import io.harness.ng.core.user.remote.dto.UserMetadataDTO;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.scim.Member;
import io.harness.scim.PatchOperation;
import io.harness.scim.PatchRequest;
import io.harness.scim.ScimGroup;
import io.harness.scim.ScimListResponse;
import io.harness.scim.ScimMultiValuedObject;
import io.harness.scim.service.ScimGroupService;

import com.google.inject.Inject;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@OwnedBy(PL)
public class NGScimGroupServiceImpl implements ScimGroupService {
  @Inject private UserGroupService userGroupService;
  @Inject private NgUserService ngUserService;

  private static final String EXC_MSG_GROUP_DOESNT_EXIST = "Group does not exist";
  private static final Integer MAX_RESULT_COUNT = 20;
  private static final String DISPLAY_NAME = "displayName";
  private static final String REPLACE_OKTA = "replace";
  private static final String REPLACE = "Replace";
  private static final String ADD = "Add";
  private static final String ADD_OKTA = "add";
  private static final String REMOVE = "Remove";
  private static final String REMOVE_OKTA = "remove";

  @Override
  public ScimListResponse<ScimGroup> searchGroup(String filter, String accountId, Integer count, Integer startIndex) {
    startIndex = startIndex == null ? 0 : startIndex;
    count = count == null ? MAX_RESULT_COUNT : count;
    ScimListResponse<ScimGroup> searchGroupResponse = new ScimListResponse<>();
    log.info("NGSCIM: Searching groups in account {} with filter: {}", accountId, filter);
    String searchQuery = null;

    if (StringUtils.isNotEmpty(filter)) {
      try {
        filter = URLDecoder.decode(filter, "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        searchQuery = operand.substring(1, operand.length() - 1);
      } catch (Exception ex) {
        log.error("NGSCIM: Failed to process for account {} group search query: {} ", accountId, filter, ex);
      }
    }

    List<ScimGroup> groupList = new ArrayList<>();

    try {
      groupList = searchUserGroupByGroupName(accountId, searchQuery, count, startIndex);
      groupList.forEach(searchGroupResponse::resource);
    } catch (WingsException ex) {
      log.info("NGSCIM: Search in account {} for group , query: {}", accountId, searchQuery, ex);
    }

    searchGroupResponse.startIndex(startIndex);
    searchGroupResponse.itemsPerPage(count);
    searchGroupResponse.totalResults(groupList.size());
    return searchGroupResponse;
  }

  private List<ScimGroup> searchUserGroupByGroupName(
      String accountId, String searchQuery, Integer count, Integer startIndex) {
    List<UserGroup> userGroupList;
    List<ScimGroup> scimGroupList = new ArrayList<>();

    if (StringUtils.isNotEmpty(searchQuery)) {
      userGroupList = userGroupService.list(
          Criteria.where(UserGroupKeys.accountIdentifier).is(accountId).and(UserGroupKeys.name).is(searchQuery));
    } else {
      return scimGroupList;
    }
    if (isNotEmpty(userGroupList)) {
      for (UserGroup userGroup : userGroupList.subList(startIndex, startIndex + count)) {
        scimGroupList.add(buildGroupResponse(userGroup));
      }
    }
    return scimGroupList;
  }

  private ScimGroup buildGroupResponse(UserGroup userGroup) {
    ScimGroup scimGroup = new ScimGroup();
    if (userGroup != null) {
      scimGroup.setId(userGroup.getIdentifier());
      scimGroup.setDisplayName(userGroup.getName());
      List<Member> memberList = new ArrayList<>();

      Scope scope = Scope.builder()
                        .accountIdentifier(userGroup.getAccountIdentifier())
                        .orgIdentifier(userGroup.getOrgIdentifier())
                        .projectIdentifier(userGroup.getProjectIdentifier())
                        .build();

      List<UserMetadataDTO> members = userGroupService.getUsersInUserGroup(scope, userGroup.getIdentifier());

      if (isNotEmpty(members)) {
        members.forEach(member -> {
          Member memberTemp = new Member();
          memberTemp.setValue(member.getUuid());
          memberTemp.setDisplay(member.getEmail());
          memberList.add(memberTemp);
        });
      }
      scimGroup.setMembers(memberList);
    }
    return scimGroup;
  }

  @Override
  public Response updateGroup(String groupId, String accountId, ScimGroup scimGroup) {
    log.info("NGSCIM: Update group call with accountId: {}, groupIdentifier {}, group resource:{}", accountId, groupId,
        scimGroup);
    List<UserGroup> existingUserGroupList = userGroupService.list(Criteria.where(UserGroupKeys.identifier)
                                                                      .is(groupId)
                                                                      .and(UserGroupKeys.accountIdentifier)
                                                                      .is(accountId)
                                                                      .and(UserGroupKeys.externallyManaged)
                                                                      .is(true));
    if (!isNotEmpty(existingUserGroupList)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    for (UserGroup existingUserGroup : existingUserGroupList) {
      UserGroupDTO userGroupDTO = toDTO(existingUserGroup);
      userGroupDTO.setName(scimGroup.getDisplayName());
      userGroupDTO.setUsers(fetchMembersOfUserGroup(scimGroup));
      userGroupService.update(userGroupDTO);
    }
    log.info("NGSCIM: Update group call successful accountId {}, groupId  {}, group resource: {}", accountId, groupId,
        scimGroup);
    return Response.status(Response.Status.OK).entity(scimGroup).build();
  }

  @Override
  public void deleteGroup(String groupId, String accountId) {
    List<UserGroup> userGroupList = userGroupService.list(Criteria.where(UserGroupKeys.identifier)
                                                              .is(groupId)
                                                              .and(UserGroupKeys.accountIdentifier)
                                                              .is(accountId)
                                                              .and(UserGroupKeys.externallyManaged)
                                                              .is(true));
    if (!isNotEmpty(userGroupList)) {
      throw new UnauthorizedException(EXC_MSG_GROUP_DOESNT_EXIST, GROUP);
    }
    for (UserGroup userGroupToBeDeleted : userGroupList) {
      Scope scope = Scope.builder()
                        .accountIdentifier(userGroupToBeDeleted.getAccountIdentifier())
                        .orgIdentifier(userGroupToBeDeleted.getOrgIdentifier())
                        .projectIdentifier(userGroupToBeDeleted.getProjectIdentifier())
                        .build();
      userGroupService.delete(scope, userGroupToBeDeleted.getIdentifier());
      log.info("NGSCIM: Deleted from account {}, group {} and scope {}", accountId,
          userGroupToBeDeleted.getIdentifier(), scope);
    }
  }

  private String processReplaceOperationOnGroup(String groupId, String accountId, PatchOperation patchOperation) {
    if (!DISPLAY_NAME.equals(patchOperation.getPath())) {
      log.error(
          "NGSCIM: Expected replace operation only on the displayName. Received it on path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      // no operation needed. Pass
    } else {
      try {
        return patchOperation.getValue(String.class);
      } catch (Exception ex) {
        log.error("NGSCIM: Failed to process the operation: {}, for accountId: {}, for GroupId {}",
            patchOperation.toString(), accountId, groupId, ex);
      }
    }
    throw new InvalidRequestException("Failed to update group name");
  }

  private String processOktaReplaceOperationOnGroup(String groupId, String accountId, PatchOperation patchOperation) {
    try {
      if (patchOperation.getValue(ScimMultiValuedObject.class) != null) {
        return patchOperation.getValue(ScimMultiValuedObject.class).getDisplayName();
      }
    } catch (Exception ex) {
      log.error("NGSCIM: Failed to process the REPLACE_OKTA operation: {}, for accountId: {}, for GroupId {}",
          patchOperation.toString(), accountId, groupId, ex);
    }
    throw new InvalidRequestException("Failed to update group name");
  }

  @Override
  public Response updateGroup(String groupId, String accountId, PatchRequest patchRequest) {
    List<UserGroup> existingUserGroupList = userGroupService.list(Criteria.where(UserGroupKeys.identifier)
                                                                      .is(groupId)
                                                                      .and(UserGroupKeys.accountIdentifier)
                                                                      .is(accountId)
                                                                      .and(UserGroupKeys.externallyManaged)
                                                                      .is(true));

    if (!isNotEmpty(existingUserGroupList)) {
      return Response.status(Response.Status.NOT_FOUND).build();
    }

    UserGroup existingGroup = existingUserGroupList.get(0);

    List<String> existingMemberIds =
        isNotEmpty(existingGroup.getUsers()) ? existingGroup.getUsers() : new ArrayList<>();
    Set<String> newMemberIds = new HashSet<>(existingMemberIds);
    String newGroupName = null;

    for (PatchOperation patchOperation : patchRequest.getOperations()) {
      Set<String> userIdsFromOperation = getUserIdsFromOperation(patchOperation, accountId, groupId);
      switch (patchOperation.getOpType()) {
        case REPLACE: {
          newGroupName = processReplaceOperationOnGroup(groupId, accountId, patchOperation);
          break;
        }
        case REPLACE_OKTA: {
          newGroupName = processOktaReplaceOperationOnGroup(groupId, accountId, patchOperation);
          break;
        }
        case ADD:
        case ADD_OKTA: {
          updateNewMemberIds(userIdsFromOperation, newMemberIds);
          break;
        }
        case REMOVE:
        case REMOVE_OKTA: {
          newMemberIds.removeAll(userIdsFromOperation);
          break;
        }
        default: {
          log.error("NGSCIM: Received unexpected PATCH operation: {}", patchOperation);
          break;
        }
      }
    }
    List<String> finalMemberIds = new ArrayList<>(newMemberIds);

    for (UserGroup userGroup : existingUserGroupList) {
      UserGroupDTO finalUserGroupDTO = toDTO(userGroup);
      boolean updateGroup = false;

      if (StringUtils.isNotEmpty(newGroupName)) {
        finalUserGroupDTO.setName(newGroupName);
        updateGroup = true;
      }
      if (!existingMemberIds.equals(finalMemberIds)) {
        finalUserGroupDTO.setUsers(finalMemberIds);
        updateGroup = true;
      }
      if (updateGroup) {
        finalUserGroupDTO.setExternallyManaged(true);
        userGroupService.update(finalUserGroupDTO);
      }
    }
    return Response.status(Response.Status.NO_CONTENT).build();
  }

  private void updateNewMemberIds(Set<String> userIdsFromOperation, Set<String> newMemberIds) {
    for (String userId : userIdsFromOperation) {
      Optional<UserInfo> userInfoOptional = ngUserService.getUserById(userId);
      if (userInfoOptional.isPresent()) {
        newMemberIds.add(userId);
      } else {
        log.info("SCIM: No user exists in the db with the id {}", userId);
      }
    }
  }

  private Set<String> getUserIdsFromOperation(PatchOperation patchOperation, String accountId, String groupId) {
    if (patchOperation.getPath() != null && patchOperation.getPath().contains("members[")) {
      try {
        Set<String> userIds = new HashSet<>();
        String filter = URLDecoder.decode(patchOperation.getPath(), "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        userIds.add(operand.substring(1, operand.length() - 2));
        return userIds;
      } catch (Exception ex) {
        log.error("SCIM: Not able to decode path. Received it in path: {}, for accountId: {}, for GroupId {}",
            patchOperation.getPath(), accountId, groupId, ex);
      }
    }

    if (!"members".equals(patchOperation.getPath())) {
      log.error(
          "SCIM: Expect operation only on the members. Received it in path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      return Collections.emptySet();
    }

    try {
      List<? extends ScimMultiValuedObject> operations = patchOperation.getValues(ScimMultiValuedObject.class);
      if (!isEmpty(operations)) {
        return operations.stream().map(operation -> (String) operation.getValue()).collect(Collectors.toSet());
      }
      log.error("NGSCIM: Operations received is null. Skipping remove operation processing for groupId: {}", groupId);
    } catch (Exception ex) {
      log.error("NGSCIM: Failed to process the operation: {}, for accountId: {}, for GroupId {}", patchOperation,
          accountId, groupId, ex);
    }

    return Collections.emptySet();
  }

  @Override
  public ScimGroup getGroup(String groupId, String accountId) {
    List<UserGroup> userGroupList = userGroupService.list(
        Criteria.where(UserGroupKeys.identifier).is(groupId).and(UserGroupKeys.accountIdentifier).is(accountId));
    if (!isNotEmpty(userGroupList)) {
      throw new UnauthorizedException(EXC_MSG_GROUP_DOESNT_EXIST, GROUP);
    }
    ScimGroup scimGroup = buildGroupResponse(userGroupList.get(0));
    log.info("NGSCIM: Response for accountId {} to get group {} with call: {}", accountId, groupId, scimGroup);
    return scimGroup;
  }

  @Override
  public ScimGroup createGroup(ScimGroup groupQuery, String accountId) {
    log.info("NGSCIM: Creating group in account {} where name {} with call: {}", accountId, groupQuery.getDisplayName(),
        groupQuery);
    String userGroupIdentifier = isNotEmpty(groupQuery.getDisplayName())
        ? groupQuery.getDisplayName().replaceAll("\\.", "_")
        : groupQuery.getDisplayName();
    UserGroupDTOBuilder userGroupDTOBuilder = UserGroupDTO.builder()
                                                  .name(groupQuery.getDisplayName())
                                                  .users(fetchMembersOfUserGroup(groupQuery))
                                                  .accountIdentifier(accountId)
                                                  .identifier(userGroupIdentifier)
                                                  .externallyManaged(true);
    UserGroup userGroupCreated = null;

    if (StringUtils.isNotEmpty(groupQuery.getHarnessScopes())) {
      String[] scopes = groupQuery.getHarnessScopes().split(",");
      for (String scimScope : scopes) {
        String[] identifiers = scimScope.split(":");
        if (identifiers.length == 2) {
          userGroupDTOBuilder.orgIdentifier(identifiers[1]);
        }
        if (identifiers.length == 3) {
          userGroupDTOBuilder.orgIdentifier(identifiers[1]).projectIdentifier(identifiers[2]);
        }
        if (identifiers.length > 3) {
          log.info("NGSCIM: Skipping group creation unidentified scope");
          continue;
        }
        userGroupCreated = userGroupService.create(userGroupDTOBuilder.build());
      }
    } else {
      userGroupCreated = userGroupService.create(userGroupDTOBuilder.build());
    }

    ScimGroup scimGroup = buildGroupResponse(userGroupCreated);
    log.info("NGSCIM: Response for accountId {} to create group with call: {}", accountId, scimGroup);
    return scimGroup;
  }

  private List<String> fetchMembersOfUserGroup(ScimGroup scimGroup) {
    List<String> newMemberIds = new ArrayList<>();
    if (isNotEmpty(scimGroup.getMembers())) {
      scimGroup.getMembers().forEach(member -> {
        if (!newMemberIds.contains(member.getValue())) {
          Optional<UserInfo> userInfoOptional = ngUserService.getUserById(member.getValue());
          if (userInfoOptional.isPresent()) {
            newMemberIds.add(member.getValue());
          } else {
            log.info("NGSCIM: No user exists with the id {}", member.getValue());
          }
        }
      });
    }
    return newMemberIds;
  }
}
