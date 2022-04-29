/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.service.impl;

import static io.harness.beans.FeatureName.CE_BILLING_DATA_PRE_AGGREGATION;
import static io.harness.rule.OwnerRule.ROHIT;
import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.entities.CEView;
import io.harness.ccm.views.entities.ViewChartType;
import io.harness.ccm.views.entities.ViewCondition;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;
import io.harness.ccm.views.entities.ViewIdCondition;
import io.harness.ccm.views.entities.ViewIdOperator;
import io.harness.ccm.views.entities.ViewQueryParams;
import io.harness.ccm.views.entities.ViewRule;
import io.harness.ccm.views.entities.ViewState;
import io.harness.ccm.views.entities.ViewTimeGranularity;
import io.harness.ccm.views.entities.ViewType;
import io.harness.ccm.views.entities.ViewVisualization;
import io.harness.ccm.views.graphql.QLCESortOrder;
import io.harness.ccm.views.graphql.QLCEViewAggregateOperation;
import io.harness.ccm.views.graphql.QLCEViewAggregation;
import io.harness.ccm.views.graphql.QLCEViewEntityStatsDataPoint;
import io.harness.ccm.views.graphql.QLCEViewFieldInput;
import io.harness.ccm.views.graphql.QLCEViewFilter;
import io.harness.ccm.views.graphql.QLCEViewFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGridData;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;
import io.harness.ccm.views.graphql.QLCEViewMetadataFilter;
import io.harness.ccm.views.graphql.QLCEViewSortCriteria;
import io.harness.ccm.views.graphql.QLCEViewSortType;
import io.harness.ccm.views.graphql.QLCEViewTimeFilter;
import io.harness.ccm.views.graphql.QLCEViewTimeFilterOperator;
import io.harness.ccm.views.graphql.QLCEViewTimeGroupType;
import io.harness.ccm.views.graphql.QLCEViewTimeTruncGroupBy;
import io.harness.ccm.views.graphql.QLCEViewTrendData;
import io.harness.ccm.views.graphql.QLCEViewTrendInfo;
import io.harness.ccm.views.graphql.ViewsMetaDataFields;
import io.harness.ccm.views.graphql.ViewsQueryBuilder;
import io.harness.ccm.views.graphql.ViewsQueryHelper;
import io.harness.ccm.views.service.CEViewService;
import io.harness.ff.FeatureFlagService;
import io.harness.rule.Owner;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValue.Attribute;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.LegacySQLTypeName;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.TableResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

public class ViewsBillingServiceImplTest extends CategoryTest {
  private static final String CLUSTER_ID = "clusterId";
  private static final String CLUSTER_NAME = "clusterName";
  private static final String CLUSTER = "cluster";
  private static final String LABEL_KEY = "labelKey";
  private static final String LABEL_KEY_NAME = "labelKeyName";
  private static final String LABEL_VALUE = "labelValue";
  private static final String WORKLOAD_NAME = "workloadName";
  private static final String NAMESPACE = "namespace";
  private static final String ACCOUNT_ID = "accountId";
  private static final String AWS_USAGE_ACCOUNT_ID = "awsUsageAccountId";
  private static final String CLUSTER_PERSPECTIVE_ID = "clusterPerspectiveId";
  private static final String AWS_PERSPECTIVE_ID = "awsPerspectiveId";
  private static final String StART_TIME_MIN = "startTime_MIN";
  private static final String StART_TIME_MAX = "startTime_MAX";
  private static final String COST = "1000";
  private static final String IDLE_COST = "100";
  private static final String UNALLOCATED_COST = "150";
  private static final String SYSTEM_COST = "50";
  private static final String TOTAL_COUNT = "324";
  @InjectMocks @Spy private ViewsBillingServiceImpl viewsBillingService;
  @Mock private ViewsQueryBuilder viewsQueryBuilder;
  @Mock private ViewsQueryHelper viewsQueryHelper;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private CEViewService viewService;
  @Mock BigQuery bigQuery;
  @Mock TableResult resultSet;
  @Mock FieldValueList row;

  private Schema schema;
  private List<Field> fields;

  private static QLCEViewFieldInput clusterId;
  private static QLCEViewFieldInput labelKey;
  private static QLCEViewFieldInput labelValue;
  private static String cloudProviderTable = "project.dataset.table";
  private int count = 0;
  private static final int limit = 2;
  private static final long ONE_DAY_IN_MILLIS = 86400000L;
  private long currentTime;
  private long startTime;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doCallRealMethod().when(viewsQueryBuilder).getAliasFromField(any());
    doCallRealMethod()
        .when(viewsQueryBuilder)
        .getFilterValuesQuery(any(), any(), any(), anyString(), anyInt(), anyInt());
    doCallRealMethod().when(viewsQueryBuilder).getQuery(any(), any(), any(), any(), any(), any(), anyString());
    doCallRealMethod()
        .when(viewsQueryBuilder)
        .getQuery(any(), any(), any(), any(), any(), any(), anyString(), anyInt());
    doCallRealMethod().when(viewsQueryBuilder).getTotalCountQuery(any(), any(), any(), any(), anyString());
    doReturn(resultSet).when(bigQuery).query(any());
    doCallRealMethod().when(viewsQueryHelper).buildQueryParams(anyString(), anyBoolean());
    doCallRealMethod().when(viewsQueryHelper).buildQueryParams(anyString(), anyBoolean(), anyBoolean());
    doCallRealMethod()
        .when(viewsQueryHelper)
        .buildQueryParams(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean());
    doCallRealMethod()
        .when(viewsQueryHelper)
        .buildQueryParams(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), anyInt());
    doCallRealMethod().when(viewsQueryBuilder).getViewFieldInput(any());
    doCallRealMethod().when(viewsQueryBuilder).mapConditionToFilter(any());

    clusterId = QLCEViewFieldInput.builder()
                    .fieldId(CLUSTER_ID)
                    .fieldName(CLUSTER)
                    .identifier(ViewFieldIdentifier.CLUSTER)
                    .identifierName(ViewFieldIdentifier.CLUSTER.getDisplayName())
                    .build();

    labelKey = QLCEViewFieldInput.builder()
                   .fieldId(ViewsMetaDataFields.LABEL_KEY.getFieldName())
                   .identifier(ViewFieldIdentifier.LABEL)
                   .identifierName(ViewFieldIdentifier.LABEL.getDisplayName())
                   .build();
    doReturn(Collections.singletonList(LABEL_KEY))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(labelKey));

    labelValue = QLCEViewFieldInput.builder()
                     .fieldId(ViewsMetaDataFields.LABEL_VALUE.getFieldName())
                     .fieldName(LABEL_KEY_NAME)
                     .identifier(ViewFieldIdentifier.LABEL)
                     .identifierName(ViewFieldIdentifier.LABEL.getDisplayName())
                     .build();
    doReturn(Collections.singletonList(LABEL_VALUE))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(labelValue));

    currentTime = System.currentTimeMillis();
    startTime = currentTime - 7 * ONE_DAY_IN_MILLIS;

    when(featureFlagService.isEnabled(CE_BILLING_DATA_PRE_AGGREGATION, ACCOUNT_ID)).thenReturn(true);
    when(row.get(CLUSTER_ID)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, CLUSTER));
    when(row.get(CLUSTER_NAME)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, CLUSTER));
    when(row.get(WORKLOAD_NAME)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, WORKLOAD_NAME));
    when(row.get(NAMESPACE)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, NAMESPACE));
    when(row.get(AWS_USAGE_ACCOUNT_ID)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, AWS_USAGE_ACCOUNT_ID));
    when(row.get("cost")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, COST));
    when(row.get("billingamount")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, COST));
    when(row.get("actualidlecost")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, IDLE_COST));
    when(row.get("unallocatedcost")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, UNALLOCATED_COST));
    when(row.get("systemcost")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, SYSTEM_COST));
    when(row.get("memoryactualidlecost")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, IDLE_COST));
    when(row.get("totalCount")).thenReturn(FieldValue.of(Attribute.PRIMITIVE, TOTAL_COUNT));
    when(row.get(StART_TIME_MIN)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, String.valueOf(startTime)));
    when(row.get(StART_TIME_MAX)).thenReturn(FieldValue.of(Attribute.PRIMITIVE, String.valueOf(currentTime)));

    when(resultSet.iterateAll()).thenReturn(new Iterable<FieldValueList>() {
      @NotNull
      @Override
      public Iterator<FieldValueList> iterator() {
        return new Iterator<FieldValueList>() {
          @Override
          public boolean hasNext() {
            if (count < limit) {
              count++;
              return true;
            } else {
              count = 0;
              return false;
            }
          }

          @Override
          public FieldValueList next() {
            return row;
          }
        };
      }
    });

    when(viewService.get(CLUSTER_PERSPECTIVE_ID))
        .thenReturn(getMockPerspective(CLUSTER_NAME, "Cluster Name", ViewFieldIdentifier.CLUSTER));
    when(viewService.get(AWS_PERSPECTIVE_ID))
        .thenReturn(getMockPerspective(AWS_USAGE_ACCOUNT_ID, "Account", ViewFieldIdentifier.AWS));
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStats() {
    doReturn(Collections.singletonList(CLUSTER))
        .when(viewsBillingService)
        .convertToFilterValuesData(resultSet, Collections.singletonList(clusterId));
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(clusterId).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsQuery() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(clusterId).values(new String[] {CLUSTER}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsLabelKey() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(labelKey).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(LABEL_KEY);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void getFilterValueStatsLabelValue() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(labelValue).values(new String[] {""}).build())
                    .build());
    List<String> filterValueStats =
        viewsBillingService.getFilterValueStats(bigQuery, filters, cloudProviderTable, 10, 0);
    assertThat(filterValueStats.get(0)).isEqualTo(LABEL_VALUE);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void getFilterValueStatsNg() {
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(QLCEViewFilterWrapper.builder()
                    .idFilter(QLCEViewFilter.builder().field(clusterId).values(new String[] {CLUSTER}).build())
                    .build());
    List<String> filterValueStats = viewsBillingService.getFilterValueStatsNg(
        bigQuery, filters, cloudProviderTable, 10, 0, getMockViewQueryParams(false));
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);

    // Cluster table query
    filterValueStats = viewsBillingService.getFilterValueStatsNg(
        bigQuery, filters, cloudProviderTable, 10, 0, getMockViewQueryParams(true));
    assertThat(filterValueStats.get(0)).isEqualTo(CLUSTER);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveGrid() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(CLUSTER_NAME, "Cluster Name", ViewFieldIdentifier.CLUSTER));

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective grid query
    List<QLCEViewEntityStatsDataPoint> data = viewsBillingService.getEntityStatsDataPoints(
        bigQuery, filters, groupBy, aggregations, sortCriteria, cloudProviderTable, 100, 0);

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.get(0).getName()).isEqualTo(CLUSTER);
    assertThat(data.get(0).getCost()).isEqualTo(Double.valueOf(COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveGridNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("billingamount", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("actualidlecost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("memoryactualidlecost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    fields.add(Field.newBuilder(NAMESPACE, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("actualidlecost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("memoryactualidlecost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(NAMESPACE, "Namespace Id", ViewFieldIdentifier.CLUSTER));

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective grid query
    QLCEViewGridData data = viewsBillingService.getEntityStatsDataPointsNg(bigQuery, filters, groupBy, aggregations,
        sortCriteria, cloudProviderTable, 100, 0, getMockViewQueryParams(false));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getClusterData().getClusterName()).isEqualTo(CLUSTER);
    assertThat(data.getData().get(0).getClusterData().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(data.getData().get(0).getCost()).isEqualTo(Double.valueOf(COST));
    assertThat(data.getData().get(0).getClusterData().getIdleCost()).isEqualTo(Double.valueOf(IDLE_COST));
    assertThat(data.getData().get(0).getClusterData().getMemoryActualIdleCost()).isEqualTo(Double.valueOf(IDLE_COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCloudPerspectiveGridNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(AWS_USAGE_ACCOUNT_ID, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(AWS_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(AWS_USAGE_ACCOUNT_ID, "Account", ViewFieldIdentifier.AWS));

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective grid query
    QLCEViewGridData data = viewsBillingService.getEntityStatsDataPointsNg(bigQuery, filters, groupBy, aggregations,
        sortCriteria, cloudProviderTable, 100, 0, getMockViewQueryParams(false));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getName()).isEqualTo(AWS_USAGE_ACCOUNT_ID);
    assertThat(data.getData().get(0).getCost()).isEqualTo(Double.valueOf(COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveGridNgWithoutViewMetadata() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("billingamount", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("actualidlecost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("memoryactualidlecost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(WORKLOAD_NAME, LegacySQLTypeName.STRING).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    fields.add(Field.newBuilder(NAMESPACE, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveFilter(NAMESPACE, "Namespace", ViewFieldIdentifier.CLUSTER, new String[] {NAMESPACE}));
    filters.add(
        getPerspectiveFilter(CLUSTER_NAME, "Cluster Name", ViewFieldIdentifier.CLUSTER, new String[] {CLUSTER}));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("actualidlecost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("memoryactualidlecost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(WORKLOAD_NAME, "Workload Id", ViewFieldIdentifier.CLUSTER));

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective grid query
    QLCEViewGridData data = viewsBillingService.getEntityStatsDataPointsNg(bigQuery, filters, groupBy, aggregations,
        sortCriteria, cloudProviderTable, 100, 0, getMockViewQueryParams(true));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getData().get(0).getClusterData().getClusterName()).isEqualTo(CLUSTER);
    assertThat(data.getData().get(0).getClusterData().getNamespace()).isEqualTo(NAMESPACE);
    assertThat(data.getData().get(0).getClusterData().getWorkloadName()).isEqualTo(WORKLOAD_NAME);
    assertThat(data.getData().get(0).getCost()).isEqualTo(Double.valueOf(COST));
    assertThat(data.getData().get(0).getClusterData().getIdleCost()).isEqualTo(Double.valueOf(IDLE_COST));
    assertThat(data.getData().get(0).getClusterData().getMemoryActualIdleCost()).isEqualTo(Double.valueOf(IDLE_COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveChart() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(CLUSTER_NAME, "Cluster Name", ViewFieldIdentifier.CLUSTER));
    groupBy.add(getTimeGroupBy());

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective chart query
    TableResult data = viewsBillingService.getTimeSeriesStats(
        bigQuery, filters, groupBy, aggregations, sortCriteria, cloudProviderTable);

    // Assertions on result
    assertThat(data).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveChartNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(CLUSTER_NAME, "Cluster Name", ViewFieldIdentifier.CLUSTER));
    groupBy.add(getTimeGroupBy());

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective chart query
    TableResult data = viewsBillingService.getTimeSeriesStatsNg(bigQuery, filters, groupBy, aggregations, sortCriteria,
        cloudProviderTable, false, 100, getMockViewQueryParams(false, true));

    // Assertions on result
    assertThat(data).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCloudPerspectiveChartNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(AWS_USAGE_ACCOUNT_ID, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(AWS_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(AWS_USAGE_ACCOUNT_ID, "Account", ViewFieldIdentifier.AWS));
    groupBy.add(getTimeGroupBy());

    List<QLCEViewSortCriteria> sortCriteria = Collections.singletonList(getSortCriteria());

    // Perspective chart query
    TableResult data = viewsBillingService.getTimeSeriesStatsNg(bigQuery, filters, groupBy, aggregations, sortCriteria,
        cloudProviderTable, false, 100, getMockViewQueryParams(false, true));

    // Assertions on result
    assertThat(data).isNotNull();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveSummaryCard() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(StART_TIME_MIN, LegacySQLTypeName.NUMERIC).build());
    fields.add(Field.newBuilder(StART_TIME_MAX, LegacySQLTypeName.NUMERIC).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    // Perspective SummaryCard query
    QLCEViewTrendInfo data = viewsBillingService.getTrendStatsData(bigQuery, filters, aggregations, cloudProviderTable);

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getValue()).isEqualTo(Double.valueOf(COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveSummaryCardNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("actualidlecost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("unallocatedcost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder("systemcost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(StART_TIME_MIN, LegacySQLTypeName.NUMERIC).build());
    fields.add(Field.newBuilder(StART_TIME_MAX, LegacySQLTypeName.NUMERIC).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("actualidlecost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("unallocatedcost", QLCEViewAggregateOperation.SUM));
    aggregations.add(getAggregation("systemcost", QLCEViewAggregateOperation.SUM));

    // Perspective SummaryCard query
    QLCEViewTrendData data = viewsBillingService.getTrendStatsDataNg(
        bigQuery, filters, aggregations, cloudProviderTable, getMockViewQueryParams(false));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getTotalCost().getValue()).isEqualTo(Double.valueOf(COST));
    assertThat(data.getIdleCost().getValue()).isEqualTo(Double.valueOf(IDLE_COST));
    assertThat(data.getUnallocatedCost().getValue()).isEqualTo(Double.valueOf(UNALLOCATED_COST));
    assertThat(data.getSystemCost().getValue()).isEqualTo(Double.valueOf(SYSTEM_COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCloudPerspectiveSummaryCardNg() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("cost", LegacySQLTypeName.FLOAT).build());
    fields.add(Field.newBuilder(StART_TIME_MIN, LegacySQLTypeName.TIMESTAMP).build());
    fields.add(Field.newBuilder(StART_TIME_MAX, LegacySQLTypeName.TIMESTAMP).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(AWS_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewAggregation> aggregations = new ArrayList<>();
    aggregations.add(getAggregation("cost", QLCEViewAggregateOperation.SUM));

    // Perspective SummaryCard query
    QLCEViewTrendData data = viewsBillingService.getTrendStatsDataNg(
        bigQuery, filters, aggregations, cloudProviderTable, getMockViewQueryParams(false));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data.getTotalCost().getValue()).isEqualTo(Double.valueOf(COST));
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testClusterPerspectiveTotalCount() {
    // Mock fields returned by result set
    fields = new ArrayList<>();
    fields.add(Field.newBuilder("totalCount", LegacySQLTypeName.NUMERIC).build());
    fields.add(Field.newBuilder(CLUSTER_NAME, LegacySQLTypeName.STRING).build());
    fields.add(Field.newBuilder(NAMESPACE, LegacySQLTypeName.STRING).build());
    schema = Schema.of(fields);
    when(resultSet.getSchema()).thenReturn(schema);

    // Build query parameters
    List<QLCEViewFilterWrapper> filters = new ArrayList<>();
    filters.add(getPerspectiveMetadataFilter(CLUSTER_PERSPECTIVE_ID));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.AFTER, startTime));
    filters.add(getPerspectiveTimeFilter(QLCEViewTimeFilterOperator.BEFORE, currentTime));

    List<QLCEViewGroupBy> groupBy = new ArrayList<>();
    groupBy.add(getEntityGroupBy(NAMESPACE, "Namespace Id", ViewFieldIdentifier.CLUSTER));

    // Total count query
    Integer data = viewsBillingService.getTotalCountForQuery(
        bigQuery, filters, groupBy, cloudProviderTable, getMockViewQueryParamsForTotalCount(false));

    // Assertions on result
    assertThat(data).isNotNull();
    assertThat(data).isEqualTo(Integer.valueOf(TOTAL_COUNT));
  }

  // Methods to build aggregations
  private QLCEViewAggregation getAggregation(String columnName, QLCEViewAggregateOperation operation) {
    return QLCEViewAggregation.builder().columnName(columnName).operationType(operation).build();
  }

  // Methods to build group by
  private QLCEViewGroupBy getEntityGroupBy(String fieldId, String fieldName, ViewFieldIdentifier identifier) {
    return QLCEViewGroupBy.builder()
        .entityGroupBy(QLCEViewFieldInput.builder()
                           .fieldId(fieldId)
                           .fieldName(fieldName)
                           .identifier(identifier)
                           .identifierName(identifier.getDisplayName())
                           .build())
        .build();
  }

  private QLCEViewGroupBy getTimeGroupBy() {
    return QLCEViewGroupBy.builder()
        .timeTruncGroupBy(QLCEViewTimeTruncGroupBy.builder().resolution(QLCEViewTimeGroupType.DAY).build())
        .build();
  }

  // Methods to build filters
  private QLCEViewFilterWrapper getPerspectiveMetadataFilter(String perspectiveId) {
    return QLCEViewFilterWrapper.builder()
        .viewMetadataFilter(QLCEViewMetadataFilter.builder().viewId(perspectiveId).isPreview(false).build())
        .build();
  }

  private QLCEViewFilterWrapper getPerspectiveTimeFilter(QLCEViewTimeFilterOperator operator, long value) {
    QLCEViewFieldInput field = QLCEViewFieldInput.builder()
                                   .fieldId("startTime")
                                   .fieldName("startTime")
                                   .identifier(ViewFieldIdentifier.COMMON)
                                   .identifierName(ViewFieldIdentifier.COMMON.getDisplayName())
                                   .build();
    return QLCEViewFilterWrapper.builder()
        .timeFilter(QLCEViewTimeFilter.builder().field(field).operator(operator).value(value).build())
        .build();
  }

  private QLCEViewFilterWrapper getPerspectiveFilter(
      String fieldId, String fieldName, ViewFieldIdentifier identifier, String[] values) {
    QLCEViewFieldInput field = QLCEViewFieldInput.builder()
                                   .fieldId(fieldId)
                                   .fieldName(fieldName)
                                   .identifier(identifier)
                                   .identifierName(identifier.getDisplayName())
                                   .build();
    return QLCEViewFilterWrapper.builder()
        .idFilter(QLCEViewFilter.builder().field(field).values(values).operator(QLCEViewFilterOperator.IN).build())
        .build();
  }

  // Method to get sort
  private QLCEViewSortCriteria getSortCriteria() {
    return QLCEViewSortCriteria.builder().sortOrder(QLCESortOrder.DESCENDING).sortType(QLCEViewSortType.COST).build();
  }

  // Methods to get mock data
  private CEView getMockPerspective(String fieldId, String fieldName, ViewFieldIdentifier identifier) {
    ViewCondition condition = ViewIdCondition.builder()
                                  .viewField(ViewField.builder()
                                                 .fieldId(fieldId)
                                                 .fieldName(fieldName)
                                                 .identifier(identifier)
                                                 .identifierName(identifier.getDisplayName())
                                                 .build())
                                  .viewOperator(ViewIdOperator.NOT_NULL)
                                  .values(Collections.singletonList(""))
                                  .build();
    ViewRule rule = ViewRule.builder().viewConditions(Collections.singletonList(condition)).build();

    return CEView.builder()
        .accountId(ACCOUNT_ID)
        .name("Mock Perspective")
        .viewVersion("v1")
        .viewType(ViewType.DEFAULT)
        .viewState(ViewState.COMPLETED)
        .viewRules(Collections.singletonList(rule))
        .viewVisualization(ViewVisualization.builder()
                               .granularity(ViewTimeGranularity.DAY)
                               .chartType(ViewChartType.STACKED_TIME_SERIES)
                               .groupBy(ViewField.builder()
                                            .fieldId(fieldId)
                                            .fieldName(fieldName)
                                            .identifier(identifier)
                                            .identifierName(identifier.getDisplayName())
                                            .build())
                               .build())
        .dataSources(Collections.singletonList(identifier))
        .build();
  }

  private ViewQueryParams getMockViewQueryParams(boolean isClusterQuery) {
    return ViewQueryParams.builder().accountId(ACCOUNT_ID).isClusterQuery(isClusterQuery).timeOffsetInDays(0).build();
  }

  private ViewQueryParams getMockViewQueryParamsForTotalCount(boolean isClusterQuery) {
    return ViewQueryParams.builder()
        .accountId(ACCOUNT_ID)
        .isClusterQuery(isClusterQuery)
        .isTotalCountQuery(true)
        .timeOffsetInDays(0)
        .build();
  }

  private ViewQueryParams getMockViewQueryParams(boolean isClusterQuery, boolean isTimeTruncGroupByRequired) {
    return ViewQueryParams.builder()
        .accountId(ACCOUNT_ID)
        .isClusterQuery(isClusterQuery)
        .isTimeTruncGroupByRequired(isTimeTruncGroupByRequired)
        .timeOffsetInDays(0)
        .build();
  }
}
