/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.eraro.ErrorCode.SERVICENOW_ERROR;
import static io.harness.exception.WingsException.USER;
import static io.harness.network.Http.getOkHttpClientBuilder;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.servicenow.ServiceNowConnectorDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.NestedExceptionUtils;
import io.harness.exception.ServiceNowException;
import io.harness.exception.WingsException;
import io.harness.network.Http;
import io.harness.security.encryption.SecretDecryptionService;
import io.harness.servicenow.ServiceNowFieldAllowedValueNG;
import io.harness.servicenow.ServiceNowFieldNG;
import io.harness.servicenow.ServiceNowFieldNG.ServiceNowFieldNGBuilder;
import io.harness.servicenow.ServiceNowFieldSchemaNG;
import io.harness.servicenow.ServiceNowFieldTypeNG;
import io.harness.servicenow.ServiceNowFieldValueNG;
import io.harness.servicenow.ServiceNowTemplate;
import io.harness.servicenow.ServiceNowTicketNG;
import io.harness.servicenow.ServiceNowUtils;
import io.harness.utils.FieldWithPlainTextOrSecretValueHelper;

import software.wings.helpers.ext.servicenow.ServiceNowRestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@OwnedBy(CDC)
@Slf4j
@Singleton
public class ServiceNowTaskNgHelper {
  public static final String INVALID_SERVICE_NOW_CREDENTIALS = "Invalid ServiceNow credentials";
  public static final String NOT_FOUND = "404 Not found";
  private final SecretDecryptionService secretDecryptionService;
  static final long TIME_OUT = 60;

  @Inject
  public ServiceNowTaskNgHelper(SecretDecryptionService secretDecryptionService) {
    this.secretDecryptionService = secretDecryptionService;
  }

  public ServiceNowTaskNGResponse getServiceNowResponse(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    decryptRequestDTOs(serviceNowTaskNGParameters);
    switch (serviceNowTaskNGParameters.getAction()) {
      case VALIDATE_CREDENTIALS:
        return validateCredentials(serviceNowTaskNGParameters);
      case GET_TICKET_CREATE_METADATA:
        return getIssueCreateMetaData(serviceNowTaskNGParameters);
      case GET_TICKET:
        return getTicket(serviceNowTaskNGParameters);
      case CREATE_TICKET:
        return createTicket(serviceNowTaskNGParameters);
      case UPDATE_TICKET:
        return updateTicket(serviceNowTaskNGParameters);
      case GET_METADATA:
        return getMetadata(serviceNowTaskNGParameters);
      case GET_TEMPLATE:
        return getTemplateList(serviceNowTaskNGParameters);
      default:
        throw new InvalidRequestException(
            String.format("Invalid servicenow task action: %s", serviceNowTaskNGParameters.getAction()));
    }
  }

  private ServiceNowTaskNGResponse createTicket(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    if (!serviceNowTaskNGParameters.isUseServiceNowTemplate()) {
      return createTicketWithoutTemplate(serviceNowTaskNGParameters);
    } else {
      return createTicketUsingServiceNowTemplate(serviceNowTaskNGParameters);
    }
  }

  private ServiceNowTaskNGResponse createTicketWithoutTemplate(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    Map<String, String> body = new HashMap<>();
    // todo: process servicenow fields before client uses these
    serviceNowTaskNGParameters.getFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key, value);
      }
    });

    final Call<JsonNode> request = serviceNowRestClient.createTicket(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(), "all", "number,sys_id", body);
    Response<JsonNode> response = null;

    try {
      log.info("Body of the create issue request made to the ServiceNow server: {}", body);
      response = request.execute();
      log.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to create ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String ticketNumber = responseObj.get("number").get("display_value").asText();
      String ticketSysId = responseObj.get("sys_id").get("display_value").asText();
      log.info("sys_id of created ticket: {}", ticketSysId);
      String ticketUrlFromTicketId = ServiceNowUtils.prepareTicketUrlFromTicketNumber(
          serviceNowConnectorDTO.getServiceNowUrl(), ticketNumber, serviceNowTaskNGParameters.getTicketType());
      // todo: add field details from ticket?
      return ServiceNowTaskNGResponse.builder()
          .ticket(ServiceNowTicketNG.builder().url(ticketUrlFromTicketId).number(ticketNumber).build())
          .build();
    } catch (Exception e) {
      log.error("Failed to create ServiceNow ticket ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse createTicketUsingServiceNowTemplate(
      ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    if (serviceNowTaskNGParameters.getTemplateName() == null) {
      throw new ServiceNowException("templateName can not be empty", SERVICENOW_ERROR, USER);
    }
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.createUsingTemplate(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(), serviceNowTaskNGParameters.getTemplateName());
    Response<JsonNode> response = null;

    try {
      log.info("createUsingTemplate called for ticketType: {}, templateName: {}",
          serviceNowTaskNGParameters.getTicketType(), serviceNowTaskNGParameters.getTemplateName());
      response = request.execute();
      log.info("Response received for createUsingTemplate: {}", response);
      handleResponse(response, "Failed to create ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String ticketNumber = responseObj.get("record_number").asText();
      String ticketSysId = responseObj.get("record_sys_id").asText();
      log.info("sys_id of created ticket: {}", ticketSysId);
      // todo: use ticketLink provided by ServiceNow?
      String ticketUrlFromTicketId = ServiceNowUtils.prepareTicketUrlFromTicketNumber(
          serviceNowConnectorDTO.getServiceNowUrl(), ticketNumber, serviceNowTaskNGParameters.getTicketType());
      // todo: add field details from ticket?
      return ServiceNowTaskNGResponse.builder()
          .ticket(ServiceNowTicketNG.builder().url(ticketUrlFromTicketId).number(ticketNumber).build())
          .build();
    } catch (Exception e) {
      log.error("Failed to create ServiceNow ticket ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse updateTicket(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    if (!serviceNowTaskNGParameters.isUseServiceNowTemplate()) {
      return updateTicketWithoutTemplate(serviceNowTaskNGParameters);
    } else {
      return updateTicketUsingServiceNowTemplate(serviceNowTaskNGParameters);
    }
  }

  private ServiceNowTaskNGResponse updateTicketUsingServiceNowTemplate(
      ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    if (serviceNowTaskNGParameters.getTemplateName() == null) {
      throw new ServiceNowException("templateName can not be empty", SERVICENOW_ERROR, USER);
    }
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.updateUsingTemplate(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(), serviceNowTaskNGParameters.getTemplateName(),
        serviceNowTaskNGParameters.getTicketNumber());
    Response<JsonNode> response = null;

    try {
      log.info("updateUsingTemplate called for ticketType: {}, templateName: {}",
          serviceNowTaskNGParameters.getTicketType(), serviceNowTaskNGParameters.getTemplateName());
      response = request.execute();
      log.info("Response received for updateUsingTemplate: {}", response);
      handleResponse(response, "Failed to update ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String ticketNumber = responseObj.get("record_number").asText();
      log.info("Ticket Number of updated ticket: {}", ticketNumber);
      // todo: use ticketLink provided by ServiceNow?
      String ticketUrlFromTicketNumber = ServiceNowUtils.prepareTicketUrlFromTicketNumber(
          serviceNowConnectorDTO.getServiceNowUrl(), ticketNumber, serviceNowTaskNGParameters.getTicketType());
      return ServiceNowTaskNGResponse.builder()
          .ticket(ServiceNowTicketNG.builder().url(ticketUrlFromTicketNumber).number(ticketNumber).build())
          .build();
    } catch (Exception e) {
      log.error("Failed to create ServiceNow ticket ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private String getIssueIdFromIssueNumber(ServiceNowTaskNGParameters parameters) {
    String query = "number=" + parameters.getTicketNumber();
    ServiceNowConnectorDTO serviceNowConnectorDTO = parameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());

    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.getIssue(
        Credentials.basic(userName, password), parameters.getTicketType().toString().toLowerCase(), query, "all");
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      log.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to fetch ticketId : " + parameters.getTicketNumber() + " from serviceNow");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        if (responseObj.size() > 1) {
          String errorMsg =
              "Multiple issues found for " + parameters.getTicketNumber() + "Please enter unique issueNumber";
          throw new ServiceNowException(errorMsg, SERVICENOW_ERROR, USER);
        }
        JsonNode issueObj = responseObj.get(0);
        if (issueObj != null) {
          return issueObj.get("sys_id").get("display_value").asText();
        } else {
          String errorMsg = "Error in fetching issue " + parameters.getTicketNumber() + " .Issue does not exist";
          throw new ServiceNowException(errorMsg, SERVICENOW_ERROR, USER);
        }
      } else {
        throw new ServiceNowException(
            "Failed to fetch issueNumber " + parameters.getTicketNumber() + "response: " + response, SERVICENOW_ERROR,
            USER);
      }
    } catch (WingsException e) {
      throw e;
    } catch (Exception e) {
      String errorMsg = "Error in fetching issueNumber " + parameters.getTicketNumber();
      throw new ServiceNowException(errorMsg + ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse updateTicketWithoutTemplate(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());
    String ticketId = null;
    if (serviceNowTaskNGParameters.getTicketNumber() != null) {
      ticketId = getIssueIdFromIssueNumber(serviceNowTaskNGParameters);
    }
    Map<String, String> body = new HashMap<>();
    // todo: process servicenow fields before client uses these
    serviceNowTaskNGParameters.getFields().forEach((key, value) -> {
      if (EmptyPredicate.isNotEmpty(value)) {
        body.put(key, value);
      }
    });

    final Call<JsonNode> request = serviceNowRestClient.updateTicket(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(), ticketId, "all", "number,sys_id", body);
    Response<JsonNode> response = null;

    try {
      log.info("Body of the create issue request made to the ServiceNow server: {}", body);
      response = request.execute();
      log.info("Response received from serviceNow: {}", response);
      handleResponse(response, "Failed to update ServiceNow ticket");
      JsonNode responseObj = response.body().get("result");
      String ticketNumber = responseObj.get("number").get("display_value").asText();
      String ticketSysId = responseObj.get("sys_id").get("display_value").asText();
      log.info("sys_id of created ticket: {}", ticketSysId);
      String ticketUrlFromTicketId = ServiceNowUtils.prepareTicketUrlFromTicketNumber(
          serviceNowConnectorDTO.getServiceNowUrl(), ticketNumber, serviceNowTaskNGParameters.getTicketType());
      // todo: add field details from ticket?
      return ServiceNowTaskNGResponse.builder()
          .ticket(ServiceNowTicketNG.builder().url(ticketUrlFromTicketId).number(ticketNumber).build())
          .build();
    } catch (Exception e) {
      log.error("Failed to create ServiceNow ticket ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse getTemplateList(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.getTemplateList(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(), serviceNowTaskNGParameters.getTemplateListLimit(),
        serviceNowTaskNGParameters.getTemplateListOffset(), serviceNowTaskNGParameters.getTemplateName());
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to get ServiceNow templates");
      JsonNode responseObj = response.body().get("result");
      if (responseObj != null && responseObj.get("queryResponse") != null) {
        JsonNode templateList = responseObj.get("queryResponse");
        List<ServiceNowTemplate> templateResponse = new ArrayList<>();
        for (JsonNode template : templateList) {
          String[] fieldList = template.get("readableValue").asText().split(".and.");
          String templateName = template.get("name").asText();
          Map<String, ServiceNowFieldValueNG> parsedFields = new HashMap<>();
          for (String field : fieldList) {
            String[] keyValue = field.split("=");
            // what about other types of fields
            if (keyValue.length == 2) {
              parsedFields.put(
                  keyValue[0].trim(), ServiceNowFieldValueNG.builder().displayValue(keyValue[1].trim()).build());
            }
          }
          templateResponse.add(ServiceNowTemplate.builder().name(templateName).fields(parsedFields).build());
        }
        return ServiceNowTaskNGResponse.builder().serviceNowTemplateList(templateResponse).build();
      } else {
        throw new ServiceNowException("Failed to fetch fields for ticket type "
                + serviceNowTaskNGParameters.getTicketType() + " response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (Exception e) {
      log.error("Failed to get serviceNow fields ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse getTicket(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.getIssue(Credentials.basic(userName, password),
        serviceNowTaskNGParameters.getTicketType().toLowerCase(),
        "number=" + serviceNowTaskNGParameters.getTicketNumber(), "all");
    Response<JsonNode> response = null;

    try {
      response = request.execute();
      handleResponse(response, "Failed to get serviceNow ticket");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        Map<String, ServiceNowFieldValueNG> fieldValues = new HashMap<>();
        for (JsonNode fieldsObj : responseObj) {
          fieldsObj.fields().forEachRemaining(fieldObj
              -> fieldValues.put(fieldObj.getKey(),
                  ServiceNowFieldValueNG.builder()
                      .value(fieldObj.getValue().get("value").textValue())
                      .displayValue(fieldObj.getValue().get("display_value").textValue())
                      .build()));
        }
        return ServiceNowTaskNGResponse.builder()
            .ticket(ServiceNowTicketNG.builder()
                        .number(serviceNowTaskNGParameters.getTicketNumber())
                        .fields(fieldValues)
                        .url(ServiceNowUtils.prepareTicketUrlFromTicketNumber(serviceNowConnectorDTO.getServiceNowUrl(),
                            serviceNowTaskNGParameters.getTicketNumber(), serviceNowTaskNGParameters.getTicketType()))
                        .build())
            .build();
      } else {
        throw new ServiceNowException("Failed to fetch ticket for ticket type "
                + serviceNowTaskNGParameters.getTicketType() + " response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (Exception e) {
      log.error("Failed to get serviceNow ticket ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private String getUserName(ServiceNowConnectorDTO serviceNowConnectorDTO) {
    return FieldWithPlainTextOrSecretValueHelper.getSecretAsStringFromPlainTextOrSecretRef(
        serviceNowConnectorDTO.getUsername(), serviceNowConnectorDTO.getUsernameRef());
  }

  private ServiceNowTaskNGResponse getIssueCreateMetaData(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.getAdditionalFields(
        Credentials.basic(userName, password), serviceNowTaskNGParameters.getTicketType().toLowerCase());
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to get serviceNow fields");
      JsonNode responseObj = response.body().get("result");
      if (responseObj.isArray()) {
        List<ServiceNowFieldNG> fields = new ArrayList<>();
        for (JsonNode fieldObj : responseObj) {
          fields.add(ServiceNowFieldNG.builder()
                         .name(fieldObj.get("label").textValue())
                         .key(fieldObj.get("name").textValue())
                         .build());
        }
        return ServiceNowTaskNGResponse.builder().serviceNowFieldNGList(fields).build();
      } else {
        throw new ServiceNowException("Failed to fetch fields for ticket type "
                + serviceNowTaskNGParameters.getTicketType() + " response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (Exception e) {
      log.error("Failed to get serviceNow fields ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private ServiceNowTaskNGResponse getMetadata(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(serviceNowConnectorDTO.getServiceNowUrl());

    final Call<JsonNode> request = serviceNowRestClient.getMetadata(
        Credentials.basic(userName, password), serviceNowTaskNGParameters.getTicketType().toLowerCase());
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to get serviceNow fields");
      JsonNode responseObj = response.body().get("result");
      if (responseObj != null && responseObj.get("columns") != null) {
        JsonNode columns = responseObj.get("columns");
        List<ServiceNowFieldNG> fields = new ArrayList<>();
        for (JsonNode fieldObj : columns) {
          List<ServiceNowFieldAllowedValueNG> allowedValues = buildAllowedValues(fieldObj.get("choices"));

          JsonNode fieldType = fieldObj.get("type");

          ServiceNowFieldNGBuilder fieldBuilder = ServiceNowFieldNG.builder()
                                                      .name(fieldObj.get("label").textValue())
                                                      .key(fieldObj.get("name").textValue())
                                                      .allowedValues(allowedValues);

          if (!allowedValues.isEmpty()) {
            fieldBuilder.schema(ServiceNowFieldSchemaNG.builder()
                                    .type(ServiceNowFieldTypeNG.OPTION)
                                    .typeStr("array")
                                    .array(true)
                                    .build());
          }

          fields.add(fieldBuilder.build());
        }
        return ServiceNowTaskNGResponse.builder().serviceNowFieldNGList(fields).build();
      } else {
        throw new ServiceNowException("Failed to fetch fields for ticket type "
                + serviceNowTaskNGParameters.getTicketType() + " response: " + response,
            SERVICENOW_ERROR, USER);
      }
    } catch (Exception e) {
      log.error("Failed to get serviceNow fields ");
      throw new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e);
    }
  }

  private List<ServiceNowFieldAllowedValueNG> buildAllowedValues(JsonNode choices) {
    ArrayList<ServiceNowFieldAllowedValueNG> allowedValues = new ArrayList<>();
    if (choices == null || !choices.isArray()) {
      return allowedValues;
    }
    if (choices.isArray()) {
      for (final JsonNode objNode : choices) {
        allowedValues.add(ServiceNowFieldAllowedValueNG.builder()
                              .id(objNode.get("value").textValue())
                              .name(objNode.get("label").textValue())
                              .build());
      }
    }
    return allowedValues;
  }

  private ServiceNowTaskNGResponse validateCredentials(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    ServiceNowConnectorDTO serviceNowConnectorDTO = serviceNowTaskNGParameters.getServiceNowConnectorDTO();
    String url = serviceNowConnectorDTO.getServiceNowUrl();
    String userName = getUserName(serviceNowConnectorDTO);
    String password = new String(serviceNowConnectorDTO.getPasswordRef().getDecryptedValue());
    ServiceNowRestClient serviceNowRestClient = getServiceNowRestClient(url);
    final Call<JsonNode> request = serviceNowRestClient.validateConnection(Credentials.basic(userName, password));
    Response<JsonNode> response = null;
    try {
      response = request.execute();
      handleResponse(response, "Failed to validate ServiceNow credentials");
      return ServiceNowTaskNGResponse.builder().build();
    } catch (ServiceNowException se) {
      log.error("Failed to authenticate to servicenow: {}", se.getMessage());
      if (se.getMessage().equals(INVALID_SERVICE_NOW_CREDENTIALS)) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the ServiceNow credentials are correct and you have necessary permissions to access the incident table",
            "The credentials provided are invalid or the user does not have necessary permissions to access incident table",
            new ServiceNowException(se.getMessage(), SERVICENOW_ERROR, USER));
      } else if (se.getMessage().equals(NOT_FOUND)) {
        throw NestedExceptionUtils.hintWithExplanationException(
            "Check if the ServiceNow url is correct and accessible from delegate",
            "Not able to access the given ServiceNow url", new ServiceNowException(NOT_FOUND, SERVICENOW_ERROR, USER));
      } else {
        throw wrapInNestedException(se);
      }
    } catch (Exception e) {
      log.error("Failed to authenticate to servicenow. ");
      throw wrapInNestedException(new ServiceNowException(ExceptionUtils.getMessage(e), SERVICENOW_ERROR, USER, e));
    }
  }

  private ServiceNowRestClient getServiceNowRestClient(String url) {
    Retrofit retrofit = new Retrofit.Builder()
                            .client(getHttpClient(url))
                            .baseUrl(url)
                            .addConverterFactory(JacksonConverterFactory.create())
                            .build();
    return retrofit.create(ServiceNowRestClient.class);
  }

  @NotNull
  private OkHttpClient getHttpClient(String url) {
    return getOkHttpClientBuilder()
        .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
        .readTimeout(TIME_OUT, TimeUnit.SECONDS)
        .proxy(Http.checkAndGetNonProxyIfApplicable(url))
        .build();
  }

  private void decryptRequestDTOs(ServiceNowTaskNGParameters serviceNowTaskNGParameters) {
    secretDecryptionService.decrypt(
        serviceNowTaskNGParameters.getServiceNowConnectorDTO(), serviceNowTaskNGParameters.getEncryptionDetails());
  }

  public static void handleResponse(Response<?> response, String message) throws IOException {
    if (response.isSuccessful()) {
      return;
    }
    if (response.code() == 401) {
      throw new ServiceNowException(INVALID_SERVICE_NOW_CREDENTIALS, SERVICENOW_ERROR, USER);
    }
    if (response.code() == 404) {
      throw new ServiceNowException(NOT_FOUND, SERVICENOW_ERROR, USER);
    }
    if (response.errorBody() == null) {
      throw new ServiceNowException(message + " : " + response.message(), SERVICENOW_ERROR, USER);
    }
    throw new ServiceNowException(response.errorBody().string(), SERVICENOW_ERROR, USER);
  }

  @NotNull
  private static WingsException wrapInNestedException(ServiceNowException e) {
    return NestedExceptionUtils.hintWithExplanationException(
        "Check if the ServiceNow url and credentials are correct and accessible from delegate",
        "Not able to access the given ServiceNow url with the credentials", e);
  }
}
