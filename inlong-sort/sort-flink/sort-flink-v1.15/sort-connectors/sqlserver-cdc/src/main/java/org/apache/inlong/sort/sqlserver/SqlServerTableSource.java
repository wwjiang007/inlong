/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.sqlserver;

import org.apache.inlong.sort.base.metric.MetricOption;

import com.ververica.cdc.connectors.sqlserver.table.SqlServerDeserializationConverterFactory;
import com.ververica.cdc.connectors.sqlserver.table.SqlServerReadableMetadata;
import com.ververica.cdc.connectors.sqlserver.table.StartupOptions;
import com.ververica.cdc.debezium.table.MetadataConverter;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.ChangelogMode;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.ScanTableSource;
import org.apache.flink.table.connector.source.SourceFunctionProvider;
import org.apache.flink.table.connector.source.abilities.SupportsReadingMetadata;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.flink.util.Preconditions.checkNotNull;

/**
 * A {@link DynamicTableSource} that describes how to create a SqlServer source from a logical
 * description.
 * <p>
 * Copy from com.ververica:flink-connector-sqlserver-cdc:2.3.0
 */
public class SqlServerTableSource implements ScanTableSource, SupportsReadingMetadata {

    private final ResolvedSchema physicalSchema;
    private final int port;
    private final String hostname;
    private final String database;
    private final String schemaName;
    private final String tableName;
    private final ZoneId serverTimeZone;
    private final String username;
    private final String password;
    private final Properties dbzProperties;
    private final StartupOptions startupOptions;

    // --------------------------------------------------------------------------------------------
    // Mutable attributes
    // --------------------------------------------------------------------------------------------

    /** Data type that describes the final output of the source. */
    protected DataType producedDataType;

    /** Metadata that is appended at the end of a physical source row. */
    protected List<String> metadataKeys;
    private final MetricOption metricOption;

    public SqlServerTableSource(
            ResolvedSchema physicalSchema,
            int port,
            String hostname,
            String database,
            String schemaName,
            String tableName,
            ZoneId serverTimeZone,
            String username,
            String password,
            Properties dbzProperties,
            StartupOptions startupOptions,
            MetricOption metricOption) {
        this.physicalSchema = physicalSchema;
        this.port = port;
        this.hostname = checkNotNull(hostname);
        this.database = checkNotNull(database);
        this.schemaName = checkNotNull(schemaName);
        this.tableName = checkNotNull(tableName);
        this.serverTimeZone = serverTimeZone;
        this.username = checkNotNull(username);
        this.password = checkNotNull(password);
        this.dbzProperties = dbzProperties;
        this.producedDataType = physicalSchema.toPhysicalRowDataType();
        this.metadataKeys = Collections.emptyList();
        this.startupOptions = startupOptions;
        this.metricOption = metricOption;
    }

    @Override
    public ChangelogMode getChangelogMode() {
        return ChangelogMode.newBuilder()
                .addContainedKind(RowKind.INSERT)
                .addContainedKind(RowKind.UPDATE_BEFORE)
                .addContainedKind(RowKind.UPDATE_AFTER)
                .addContainedKind(RowKind.DELETE)
                .build();
    }

    @Override
    public ScanRuntimeProvider getScanRuntimeProvider(ScanContext scanContext) {
        RowType physicalDataType =
                (RowType) physicalSchema.toPhysicalRowDataType().getLogicalType();
        MetadataConverter[] metadataConverters = getMetadataConverters();
        TypeInformation<RowData> typeInfo = scanContext.createTypeInformation(producedDataType);

        DebeziumDeserializationSchema<RowData> deserializer =
                RowDataDebeziumDeserializeSchema.newBuilder()
                        .setPhysicalRowType(physicalDataType)
                        .setMetadataConverters(metadataConverters)
                        .setResultTypeInfo(typeInfo)
                        .setServerTimeZone(serverTimeZone)
                        .setUserDefinedConverterFactory(
                                SqlServerDeserializationConverterFactory.instance())
                        .setMetricOption(metricOption)
                        .build();
        DebeziumSourceFunction<RowData> sourceFunction =
                SqlServerSource.<RowData>builder()
                        .hostname(hostname)
                        .port(port)
                        .database(database)
                        .tableList(schemaName + "." + tableName)
                        .username(username)
                        .password(password)
                        .debeziumProperties(dbzProperties)
                        .startupOptions(startupOptions)
                        .deserializer(deserializer)
                        .metricOption(metricOption)
                        .build();
        return SourceFunctionProvider.of(sourceFunction, false);
    }

    private MetadataConverter[] getMetadataConverters() {
        if (metadataKeys.isEmpty()) {
            return new MetadataConverter[0];
        }

        return metadataKeys.stream()
                .map(
                        key -> Stream.of(SqlServerReadableMetadata.values())
                                .filter(m -> m.getKey().equals(key))
                                .findFirst()
                                .orElseThrow(IllegalStateException::new))
                .map(SqlServerReadableMetadata::getConverter)
                .toArray(MetadataConverter[]::new);
    }

    @Override
    public DynamicTableSource copy() {
        SqlServerTableSource source =
                new SqlServerTableSource(
                        physicalSchema,
                        port,
                        hostname,
                        database,
                        schemaName,
                        tableName,
                        serverTimeZone,
                        username,
                        password,
                        dbzProperties,
                        startupOptions,
                        metricOption);
        source.metadataKeys = metadataKeys;
        source.producedDataType = producedDataType;
        return source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SqlServerTableSource that = (SqlServerTableSource) o;
        return port == that.port
                && Objects.equals(physicalSchema, that.physicalSchema)
                && Objects.equals(hostname, that.hostname)
                && Objects.equals(database, that.database)
                && Objects.equals(schemaName, that.schemaName)
                && Objects.equals(tableName, that.tableName)
                && Objects.equals(serverTimeZone, that.serverTimeZone)
                && Objects.equals(username, that.username)
                && Objects.equals(password, that.password)
                && Objects.equals(dbzProperties, that.dbzProperties)
                && Objects.equals(startupOptions, that.startupOptions)
                && Objects.equals(producedDataType, that.producedDataType)
                && Objects.equals(metadataKeys, that.metadataKeys)
                && Objects.equals(metricOption, that.metricOption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                physicalSchema,
                port,
                hostname,
                database,
                schemaName,
                tableName,
                serverTimeZone,
                username,
                password,
                dbzProperties,
                startupOptions,
                producedDataType,
                metadataKeys,
                metricOption);
    }

    @Override
    public String asSummaryString() {
        return "SqlServer-CDC";
    }

    @Override
    public Map<String, DataType> listReadableMetadata() {
        return Stream.of(SqlServerReadableMetadata.values())
                .collect(
                        Collectors.toMap(
                                SqlServerReadableMetadata::getKey,
                                SqlServerReadableMetadata::getDataType));
    }

    @Override
    public void applyReadableMetadata(List<String> metadataKeys, DataType producedDataType) {
        this.metadataKeys = metadataKeys;
        this.producedDataType = producedDataType;
    }
}
